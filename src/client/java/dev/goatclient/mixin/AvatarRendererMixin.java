package dev.goatclient.mixin;

import dev.goatclient.cosmetic.ShoulderPetLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void goatclient$addShoulderPetLayer(EntityRendererProvider.Context context, boolean slim,
                                                CallbackInfo callback) {
        LivingEntityRenderer<?, ?, ?> renderer = (LivingEntityRenderer<?, ?, ?>) (Object) this;
        ((LivingEntityRendererAccessor) renderer).goatclient$addLayer(
                new ShoulderPetLayer((RenderLayerParent) renderer));
    }
}
