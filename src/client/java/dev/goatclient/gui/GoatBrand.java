package dev.goatclient.gui;

import dev.goatclient.GoatClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Shared branding using the original PNG supplied by the client owner. */
public final class GoatBrand {
    private static final Identifier LOGO = GoatClient.id("textures/gui/logo.png");

    private GoatBrand() {}

    public static void logo(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.blit(LOGO, x, y, x + size, y + size, 0.0F, 1.0F, 0.0F, 1.0F);
    }

    public static void header(GuiGraphicsExtractor graphics, Font font, int left, int top,
                              int width, String title, String subtitle) {
        logo(graphics, left + 12, top + 8, 36);
        graphics.text(font, title, left + 56, top + 14, 0xFF8BE9A4, false);
        graphics.text(font, font.plainSubstrByWidth(subtitle, width - 72),
                left + 56, top + 30, 0xFF99A7B5, false);
    }
}
