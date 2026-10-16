package dev.goatclient.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.goatclient.GoatClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Simple low-poly 3D companions anchored to the local avatar's shoulder. */
public final class ShoulderPetLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public ShoulderPetLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float limbAngle, float limbDistance) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || state.id != client.player.getId() || state.isInvisible) return;
        var profile = GoatClient.profile();
        if (!profile.cosmeticsEnabled || !profile.petEnabled) return;

        poses.pushPose();
        // Render layers start at the avatar's feet, while model-space Y points down.
        // Zero therefore places the companion beside the upper torso instead of above the head.
        poses.translate(0.25F, state.isCrouching ? 0.22F : 0.0F, -0.02F);
        // Point the companion's face outward, toward the third-person camera behind the player.
        poses.rotateDegrees(Axis.YP, 180.0F);
        poses.translate(0, (float) Math.sin(state.ageInTicks * 0.12F) * 0.015F, 0);
        PetStyle pet = profile.petStyle();
        collector.submitCustomGeometry(poses, RenderTypes.debugQuads(),
                (pose, vertices) -> renderPet(pose, vertices, pet));
        poses.popPose();
    }

    private static void renderPet(PoseStack.Pose pose, VertexConsumer out, PetStyle pet) {
        int body = pet.body();
        int accent = pet.accent();
        int dark = pet.dark();
        int shadow = shade(body, 0.72F);

        switch (pet) {
            case GOAT -> {
                cube(pose, out, -0.16F, 0.02F, -0.13F, 0.16F, 0.22F, 0.13F, body);
                cube(pose, out, -0.11F, -0.12F, -0.22F, 0.12F, 0.08F, 0.02F, body);
                cube(pose, out, -0.10F, 0.01F, -0.28F, -0.055F, 0.055F, -0.20F, dark);
                cube(pose, out, 0.055F, 0.01F, -0.28F, 0.10F, 0.055F, -0.20F, dark);
                cube(pose, out, -0.09F, -0.24F, -0.13F, -0.045F, -0.11F, -0.06F, accent);
                cube(pose, out, 0.055F, -0.24F, -0.13F, 0.10F, -0.11F, -0.06F, accent);
                cube(pose, out, -0.12F, 0.19F, -0.10F, -0.065F, 0.30F, -0.02F, shadow);
                cube(pose, out, 0.065F, 0.19F, -0.10F, 0.12F, 0.30F, -0.02F, shadow);
                legs(pose, out, dark);
            }
            case FOX -> {
                cube(pose, out, -0.17F, 0.02F, -0.10F, 0.17F, 0.22F, 0.13F, body);
                cube(pose, out, -0.12F, -0.13F, -0.24F, 0.12F, 0.08F, -0.02F, body);
                cube(pose, out, -0.08F, -0.01F, -0.32F, 0.08F, 0.07F, -0.21F, accent);
                cube(pose, out, -0.12F, -0.25F, 0.06F, 0.12F, -0.07F, 0.34F, body);
                cube(pose, out, -0.10F, -0.27F, 0.27F, 0.10F, -0.09F, 0.38F, accent);
                cube(pose, out, -0.12F, -0.25F, -0.12F, -0.055F, -0.10F, -0.04F, accent);
                cube(pose, out, 0.055F, -0.25F, -0.12F, 0.12F, -0.10F, -0.04F, accent);
                legs(pose, out, dark);
            }
            case BEE -> {
                cube(pose, out, -0.16F, 0.00F, -0.13F, 0.16F, 0.22F, 0.13F, body);
                cube(pose, out, -0.16F, 0.07F, -0.14F, 0.16F, 0.12F, 0.14F, dark);
                cube(pose, out, -0.13F, -0.12F, -0.22F, 0.13F, 0.08F, 0.01F, body);
                cube(pose, out, -0.10F, -0.02F, -0.29F, -0.05F, 0.035F, -0.21F, dark);
                cube(pose, out, 0.05F, -0.02F, -0.29F, 0.10F, 0.035F, -0.21F, dark);
                cube(pose, out, -0.29F, -0.04F, -0.01F, -0.17F, 0.01F, 0.12F, accent);
                cube(pose, out, 0.17F, -0.04F, -0.01F, 0.29F, 0.01F, 0.12F, accent);
                legs(pose, out, dark);
            }
            case SLIME -> {
                cube(pose, out, -0.18F, -0.12F, -0.15F, 0.18F, 0.22F, 0.15F, body);
                cube(pose, out, -0.12F, -0.02F, -0.205F, -0.04F, 0.08F, -0.145F, dark);
                cube(pose, out, 0.04F, -0.02F, -0.205F, 0.12F, 0.08F, -0.145F, dark);
                cube(pose, out, -0.06F, 0.10F, -0.19F, 0.06F, 0.14F, -0.14F, accent);
                cube(pose, out, -0.13F, 0.20F, -0.10F, -0.02F, 0.24F, 0.00F, accent);
                cube(pose, out, 0.02F, 0.20F, -0.10F, 0.13F, 0.24F, 0.00F, accent);
            }
            case GHOST -> {
                cube(pose, out, -0.16F, -0.12F, -0.13F, 0.16F, 0.12F, 0.13F, body);
                cube(pose, out, -0.12F, -0.24F, -0.10F, 0.12F, -0.05F, 0.10F, body);
                cube(pose, out, -0.13F, 0.10F, -0.12F, -0.05F, 0.22F, 0.04F, accent);
                cube(pose, out, 0.05F, 0.10F, -0.12F, 0.13F, 0.22F, 0.04F, accent);
                cube(pose, out, -0.11F, -0.04F, -0.19F, -0.045F, 0.035F, -0.13F, dark);
                cube(pose, out, 0.045F, -0.04F, -0.19F, 0.11F, 0.035F, -0.13F, dark);
                cube(pose, out, -0.15F, 0.12F, -0.10F, -0.06F, 0.24F, -0.01F, body);
                cube(pose, out, 0.06F, 0.12F, -0.10F, 0.15F, 0.24F, -0.01F, body);
            }
        }
    }

    private static void legs(PoseStack.Pose pose, VertexConsumer out, int color) {
        cube(pose, out, -0.12F, 0.18F, -0.08F, -0.06F, 0.29F, -0.01F, color);
        cube(pose, out, 0.06F, 0.18F, -0.08F, 0.12F, 0.29F, -0.01F, color);
        cube(pose, out, -0.12F, 0.18F, 0.06F, -0.06F, 0.29F, 0.13F, color);
        cube(pose, out, 0.06F, 0.18F, 0.06F, 0.12F, 0.29F, 0.13F, color);
    }

    private static void cube(PoseStack.Pose pose, VertexConsumer out,
                             float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        quad(pose, out, color, 0, 0, -1, x0,y0,z0, x1,y0,z0, x1,y1,z0, x0,y1,z0);
        quad(pose, out, shade(color, 0.78F), 0, 0, 1, x1,y0,z1, x0,y0,z1, x0,y1,z1, x1,y1,z1);
        quad(pose, out, shade(color, 0.88F), -1, 0, 0, x0,y0,z1, x0,y0,z0, x0,y1,z0, x0,y1,z1);
        quad(pose, out, color, 1, 0, 0, x1,y0,z0, x1,y0,z1, x1,y1,z1, x1,y1,z0);
        quad(pose, out, shade(color, 1.08F), 0, -1, 0, x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1);
        quad(pose, out, shade(color, 0.68F), 0, 1, 0, x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer out, int color,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(pose, out, color, nx, ny, nz, ax, ay, az);
        vertex(pose, out, color, nx, ny, nz, bx, by, bz);
        vertex(pose, out, color, nx, ny, nz, cx, cy, cz);
        vertex(pose, out, color, nx, ny, nz, dx, dy, dz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer out, int color,
                               float nx, float ny, float nz, float x, float y, float z) {
        out.addVertex(pose, x, y, z).setColor(color).setNormal(pose, nx, ny, nz);
    }

    private static int shade(int color, float amount) {
        int r = Math.min(255, (int) (((color >>> 16) & 0xFF) * amount));
        int g = Math.min(255, (int) (((color >>> 8) & 0xFF) * amount));
        int b = Math.min(255, (int) ((color & 0xFF) * amount));
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
