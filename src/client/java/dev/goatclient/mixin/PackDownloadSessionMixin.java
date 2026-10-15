package dev.goatclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Resource-pack requests must use the active nickname/UUID after an account switch. */
@Mixin(targets = "net.minecraft.client.resources.server.DownloadedPackSource$4")
public abstract class PackDownloadSessionMixin {
    @ModifyExpressionValue(method = "createDownloadHeaders", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/resources/server/DownloadedPackSource$4;val$user:Lnet/minecraft/client/User;"))
    private User goatclient$currentDownloadUser(User original) {
        return Minecraft.getInstance().getUser();
    }
}
