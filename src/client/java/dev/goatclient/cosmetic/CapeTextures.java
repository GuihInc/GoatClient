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
    private static Map<CapeStyle, ClientAsset.Texture> wingTextures = Map.of();

    private CapeTextures() {}

    public static void register(Minecraft client) {
        Map<CapeStyle, ClientAsset.Texture> registered = new EnumMap<>(CapeStyle.class);
        Map<CapeStyle, ClientAsset.Texture> registeredWings = new EnumMap<>(CapeStyle.class);
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

            NativeImage wingImage = wings(style);
            Identifier wingTexture = GoatClient.id("wings/" + style.id());
            client.getTextureManager().register(wingTexture,
                    new DynamicTexture(() -> "GoatClient wings " + style.id(), wingImage));
            registeredWings.put(style, new ClientAsset.ResourceTexture(wingTexture, wingTexture));
        }
        textures = Map.copyOf(registered);
        wingTextures = Map.copyOf(registeredWings);
    }

    public static PlayerSkin apply(PlayerSkin original) {
        ProfileSettings profile = GoatClient.profile();
        if (!profile.cosmeticsEnabled) return original;
        ClientAsset.Texture cape = textures.get(profile.capeStyle());
        ClientAsset.Texture wings = wingTextures.get(profile.capeStyle() == CapeStyle.NONE
                ? CapeStyle.GOAT : profile.capeStyle());
        return new PlayerSkin(original.body(), cape == null ? original.cape() : cape,
                profile.wingsEnabled && wings != null ? wings : original.elytra(), original.model(), original.secure());
    }

    private static NativeImage wings(CapeStyle style) {
        NativeImage image = new NativeImage(64, 32, true);
        image.fillRect(0, 0, 64, 32, 0x00000000);
        int accent = style.accent();
        int shade = (accent & 0xFF000000) | (((accent >>> 16) & 0xFF) * 3 / 4 << 16)
                | (((accent >>> 8) & 0xFF) * 3 / 4 << 8) | ((accent & 0xFF) * 3 / 4);
        for (int y = 0; y < 16; y++) {
            int span = 5 + y;
            for (int x = 0; x < span; x++) {
                int color = x % 4 == 0 ? shade : accent;
                image.setPixel(x, y, color);
                image.setPixel(22 + x, y, color);
            }
        }
        return image;
    }
}
