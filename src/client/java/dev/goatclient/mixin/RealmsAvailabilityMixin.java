package dev.goatclient.mixin;

import com.mojang.realmsclient.RealmsAvailability;
import dev.goatclient.auth.SessionAccess;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RealmsAvailability.class)
public abstract class RealmsAvailabilityMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void goatclient$skipOfflineRealms(CallbackInfoReturnable<CompletableFuture<RealmsAvailability.Result>> callback) {
        if (((SessionAccess) Minecraft.getInstance()).goatclient$isOffline()) {
            callback.setReturnValue(CompletableFuture.completedFuture(
                    new RealmsAvailability.Result(RealmsAvailability.Type.AUTHENTICATION_ERROR)));
        }
    }
}
