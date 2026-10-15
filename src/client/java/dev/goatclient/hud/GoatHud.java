package dev.goatclient.hud;

import dev.goatclient.GoatClient;
import dev.goatclient.config.ProfileSettings;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class GoatHud {
    private static final int ACCENT = 0xFF8BE9A4;

    private GoatHud() {}

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.hud.isHidden() || client.debugEntries.isOverlayVisible()) return;
        ProfileSettings profile = GoatClient.profile();
        int y = 8;
        if (profile.showFps) y = line(graphics, "GOAT  /  " + client.getFps() + " FPS", y);
        if (profile.showCoordinates && !client.showOnlyReducedInfo()) {
            var pos = client.player.blockPosition();
            y = line(graphics, "XYZ  " + pos.getX() + "  " + pos.getY() + "  " + pos.getZ(), y);
        }
        if (profile.showKeystrokes) {
            y += 4;
            key(graphics, client.options.keyUp, 30, y, 20);
            key(graphics, client.options.keyLeft, 8, y + 22, 20);
            key(graphics, client.options.keyDown, 30, y + 22, 20);
            key(graphics, client.options.keyRight, 52, y + 22, 20);
            key(graphics, client.options.keyJump, 8, y + 44, 64);
        }
    }

    private static int line(GuiGraphicsExtractor graphics, String text, int y) {
        var font = Minecraft.getInstance().font;
        graphics.fill(8, y, 20 + font.width(text), y + 18, 0xD912191F);
        graphics.fill(8, y, 10, y + 18, ACCENT);
        graphics.text(font, text, 15, y + 5, 0xFFF0F4F8, false);
        return y + 21;
    }

    private static void key(GuiGraphicsExtractor graphics, KeyMapping mapping, int x, int y, int width) {
        var font = Minecraft.getInstance().font;
        graphics.fill(x, y, x + width, y + 20, mapping.isDown() ? ACCENT : 0xD912191F);
        String label = font.plainSubstrByWidth(mapping.getTranslatedKeyMessage().getString(), width - 4);
        graphics.centeredText(font, label, x + width / 2, y + 6, mapping.isDown() ? 0xFF122119 : 0xFFF0F4F8);
    }
}
