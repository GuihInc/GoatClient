package dev.goatclient.mixin;

import dev.goatclient.cosmetic.CapeTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class LocalPlayerSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void goatclient$applyLocalCape(CallbackInfoReturnable<PlayerSkin> callback) {
        if (Minecraft.getInstance().player == (Object) this) {
            callback.setReturnValue(CapeTextures.apply(callback.getReturnValue()));
        }
    }
}
