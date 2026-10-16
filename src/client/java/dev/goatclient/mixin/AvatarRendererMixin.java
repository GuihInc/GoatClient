package dev.goatclient.mixin;

import dev.goatclient.cosmetic.ShoulderPetLayer;
import dev.goatclient.GoatClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
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

    @Inject(method = "submitNameDisplay", at = @At("HEAD"))
    private void goatclient$submitLocalNick(AvatarRenderState state, PoseStack poses,
                                            SubmitNodeCollector collector, CameraRenderState camera,
                                            CallbackInfo callback) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || state.id != client.player.getId() || state.isInvisible) return;

        state.nameTagAttachment = new Vec3(0.0, state.boundingBoxHeight + 0.5, 0.0);
        Component name = Component.literal(GoatClient.displayNickname())
                .withStyle(style -> style.withColor(ChatFormatting.WHITE).withFont(FontDescription.DEFAULT));
        if (GoatClient.profile().showGoatSymbol) {
            var logo = Component.literal("\uE000")
                    .withStyle(style -> style.withFont(new FontDescription.Resource(GoatClient.id("default"))));
            Component gap = Component.literal(" ").withStyle(style -> style.withFont(FontDescription.DEFAULT));
            state.nameTag = logo.append(gap).append(name);
        } else {
            state.nameTag = name;
        }
    }
}
