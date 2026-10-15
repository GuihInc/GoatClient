package dev.goatclient.cosmetic;

import com.mojang.blaze3d.platform.NativeImage;
import dev.goatclient.GoatClient;
import dev.goatclient.config.ProfileSettings;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

public final class CapeTextures {
    private static Map<CapeStyle, ClientAsset.Texture> textures = Map.of();

    private CapeTextures() {}

    public static void register(Minecraft client) {
        Map<CapeStyle, ClientAsset.Texture> registered = new EnumMap<>(CapeStyle.class);
        for (CapeStyle style : CapeStyle.values()) {
            if (style == CapeStyle.NONE) continue;
            NativeImage image = new NativeImage(64, 32, true);
            image.fillRect(0, 0, 64, 32, style.pixel(1, 1));
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 10; x++) {
                    image.setPixel(1 + x, 1 + y, style.pixel(x, y));
                    image.setPixel(12 + x, 1 + y, style.pixel(9 - x, y));
                }
            }
            Identifier texture = GoatClient.id("capes/" + style.id());
            client.getTextureManager().register(texture, new DynamicTexture(() -> "GoatClient " + style.id(), image));
            registered.put(style, new ClientAsset.ResourceTexture(texture, texture));
        }
        textures = Map.copyOf(registered);
    }

    public static PlayerSkin apply(PlayerSkin original) {
        ProfileSettings profile = GoatClient.profile();
        ClientAsset.Texture cape = textures.get(profile.capeStyle());
        if (!profile.cosmeticsEnabled || cape == null) return original;
        return new PlayerSkin(original.body(), cape, original.elytra(), original.model(), original.secure());
    }
}
