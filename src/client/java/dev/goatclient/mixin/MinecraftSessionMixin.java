package dev.goatclient.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import com.mojang.authlib.services.ProfileResult;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import dev.goatclient.auth.AccountManager;
import dev.goatclient.auth.ClientSession;
import dev.goatclient.auth.SessionAccess;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftSessionMixin implements SessionAccess {
    @Shadow @Final @Mutable private User user;
    @Shadow @Final @Mutable private CompletableFuture<ProfileResult> profileFuture;
    @Shadow @Final @Mutable private UserApiService userApiService;
    @Shadow @Final @Mutable private CompletableFuture<UserApiService.UserProperties> userPropertiesFuture;
    @Shadow @Final @Mutable private ProfileKeyPairManager profileKeyPairManager;
    @Shadow @Final @Mutable private PlayerSocialManager playerSocialManager;
    @Shadow @Final @Mutable private RemoteFriendListUpdateHandler remoteFriendListUpdateHandler;
    @Shadow @Final @Mutable private ClientTelemetryManager telemetryManager;
    @Shadow @Final @Mutable private RealmsDataFetcher realmsDataFetcher;
    @Shadow private ReportingContext reportingContext;
    @Unique private boolean goatclient$offline;

    @Override
    public ClientSession goatclient$captureSession() {
        return new ClientSession(user, profileFuture, userApiService, userPropertiesFuture, profileKeyPairManager,
                goatclient$isOffline());
    }

    @Override
    public boolean goatclient$isOffline() {
        return goatclient$offline || userApiService == UserApiService.OFFLINE;
    }

    @Override
    public void goatclient$applySession(ClientSession session) {
        Minecraft client = (Minecraft) (Object) this;
        if (!client.isSameThread() || !AccountManager.canSwitch(client)) {
            throw new IllegalStateException("A sessão só pode ser trocada no menu principal.");
        }
        ClientSession previous = goatclient$captureSession();
        var oldFriends = remoteFriendListUpdateHandler;
        var oldSocial = playerSocialManager;
        var oldTelemetry = telemetryManager;
        var oldRealms = realmsDataFetcher;
        RemoteFriendListUpdateHandler newFriends = null;
        ClientTelemetryManager newTelemetry = null;
        oldFriends.stop();
        try {
            goatclient$setCredentials(session);
            var discovery = session.offline() ? MinecraftServicesDiscoveryService.createOffline(client.getProxy())
                    : MinecraftServicesDiscoveryService.create(client.getProxy());
            var friends = discovery.createFriendsService(user.getAccessToken());
            newFriends = new RemoteFriendListUpdateHandler(friends, client);
            var newSocial = new PlayerSocialManager(client, userApiService, friends, newFriends);
            var newReporting = ReportingContext.create(ReportEnvironment.local(), userApiService);
            newTelemetry = new ClientTelemetryManager(client, userApiService, user);
            RealmsClientAccessor.goatclient$resetClient(null);
            RealmsAvailabilityAccessor.goatclient$resetAvailability(null);
            if (!session.offline()) realmsDataFetcher = new RealmsDataFetcher(RealmsClient.getOrCreate(client));
            remoteFriendListUpdateHandler = newFriends;
            playerSocialManager = newSocial;
            reportingContext = newReporting;
            telemetryManager = newTelemetry;
        } catch (RuntimeException exception) {
            goatclient$setCredentials(previous);
            realmsDataFetcher = oldRealms;
            RealmsClientAccessor.goatclient$resetClient(null);
            RealmsAvailabilityAccessor.goatclient$resetAvailability(null);
            if (newFriends != null) newFriends.close();
            if (newTelemetry != null) newTelemetry.close();
            if (oldSocial.isFriendListEnabled()) oldFriends.start();
            throw new IllegalStateException("Não foi possível trocar de conta. A sessão anterior foi mantida.");
        }
        oldFriends.close();
        oldTelemetry.close();
        if (playerSocialManager.isFriendListEnabled()) remoteFriendListUpdateHandler.start();
    }

    @Unique
    private void goatclient$setCredentials(ClientSession session) {
        user = session.user();
        profileFuture = session.profile();
        userApiService = session.api();
        userPropertiesFuture = session.properties();
        profileKeyPairManager = session.keys();
        goatclient$offline = session.offline();
    }

    @Inject(method = {"allowsRealms", "friendsEnabled", "allowFriendRequests"}, at = @At("HEAD"), cancellable = true)
    private void goatclient$offlineOnlineFeatures(CallbackInfoReturnable<Boolean> callback) {
        if (goatclient$offline) callback.setReturnValue(false);
    }
}
