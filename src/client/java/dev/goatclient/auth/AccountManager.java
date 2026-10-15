package dev.goatclient.auth;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import com.mojang.authlib.services.ProfileResult;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;

public final class AccountManager {
    public enum Mode { LAUNCHER, MICROSOFT, OFFLINE }
    private static ClientSession launcher;
    private static Mode mode = Mode.LAUNCHER;

    private AccountManager() {}

    public static void initialize(Minecraft client) {
        launcher = ((SessionAccess) client).goatclient$captureSession();
    }

    public static boolean canSwitch(Minecraft client) {
        return client.level == null && client.getConnection() == null && !client.hasSingleplayerServer();
    }

    public static Mode mode() { return mode; }

    public static String launcherName() {
        return launcher == null ? "Launcher" : launcher.user().getName();
    }

    public static void useLauncher() {
        if (launcher == null) throw new IllegalStateException("A conta do launcher ainda não está disponível.");
        apply(launcher, Mode.LAUNCHER);
    }

    public static void useOffline(String input) {
        String name = OfflineAccount.validateName(input);
        User user = new User(name, OfflineAccount.uuid(name), "0", Optional.empty(), Optional.empty());
        apply(new ClientSession(user, CompletableFuture.completedFuture(null), UserApiService.OFFLINE,
                CompletableFuture.completedFuture(UserApiService.OFFLINE_PROPERTIES),
                ProfileKeyPairManager.EMPTY_KEY_MANAGER, true), Mode.OFFLINE);
    }

    /** Called on the login worker, so network and permission checks cannot freeze the UI. */
    public static ClientSession prepareMicrosoft(MicrosoftAccount account) throws IOException {
        if (!account.expiresAt().isAfter(Instant.now())) throw new IOException("A sessão expirou. Faça login novamente.");
        Minecraft client = Minecraft.getInstance();
        var discovery = MinecraftServicesDiscoveryService.create(client.getProxy());
        UserApiService api = discovery.createUserApiService(account.accessToken());
        UserApiService.UserProperties properties;
        try {
            properties = api.fetchProperties();
        } catch (com.mojang.authlib.exceptions.AuthenticationException exception) {
            throw new IOException("Não foi possível verificar as permissões da conta Minecraft.");
        }
        ProfileResult profile = client.services().sessionService().fetchProfile(account.uuid(), true);
        if (profile == null || !profile.profile().id().equals(account.uuid())) {
            throw new IOException("Não foi possível carregar o perfil autenticado. Tente novamente.");
        }
        User user = new User(profile.profile().name(), account.uuid(), account.accessToken(),
                account.xuid().isBlank() ? Optional.empty() : Optional.of(account.xuid()), Optional.of(account.clientId()));
        return new ClientSession(user, CompletableFuture.completedFuture(profile), api,
                CompletableFuture.completedFuture(properties),
                ProfileKeyPairManager.create(api, user, client.gameDirectory.toPath()), false);
    }

    public static void useMicrosoft(ClientSession session) {
        apply(session, Mode.MICROSOFT);
    }

    private static void apply(ClientSession session, Mode target) {
        Minecraft client = Minecraft.getInstance();
        if (!client.isSameThread() || !canSwitch(client)) {
            throw new IllegalStateException("Saia do mundo ou servidor antes de trocar de conta.");
        }
        ((SessionAccess) client).goatclient$applySession(session);
        mode = target;
        client.updateTitle();
    }
}
