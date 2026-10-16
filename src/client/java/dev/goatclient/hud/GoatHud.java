package dev.goatclient.hud;

import dev.goatclient.GoatClient;
import dev.goatclient.config.ProfileSettings;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.time.LocalTime;

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
        if (profile.showDirection) {
            String[] directions = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
            int direction = Math.floorMod((int) Math.floor((client.player.getYRot() + 22.5F) / 45.0F), 8);
            y = line(graphics, "DIRECTION  " + directions[direction], y);
        }
        if (profile.showClock) y = line(graphics, "TIME  " + LocalTime.now().withNano(0), y);
        if (profile.showSprintStatus) y = line(graphics, "SPRINT  " + (client.player.isSprinting() ? "ON" : "OFF"), y);
        if (profile.showKeystrokes) {
            y += 4;
            key(graphics, client.options.keyUp, 30, y, 20);
            key(graphics, client.options.keyLeft, 8, y + 22, 20);
            key(graphics, client.options.keyDown, 30, y + 22, 20);
            key(graphics, client.options.keyRight, 52, y + 22, 20);
            key(graphics, client.options.keyJump, 8, y + 44, 64);
        }
        if (profile.cosmeticsEnabled && profile.petEnabled) pet(graphics, client);
        String emote = EmoteOverlay.current();
        if (!emote.isEmpty()) {
            int center = client.getWindow().getGuiScaledWidth() / 2;
            int bottom = client.getWindow().getGuiScaledHeight() - 70;
            int width = client.font.width(emote) + 22;
            graphics.fill(center - width / 2, bottom - 4, center + width / 2, bottom + 18, 0xE912191F);
            graphics.fill(center - width / 2, bottom - 4, center + width / 2, bottom - 2, ACCENT);
            graphics.centeredText(client.font, emote, center, bottom + 2, 0xFFF0F4F8);
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

    private static void pet(GuiGraphicsExtractor graphics, Minecraft client) {
        int bob = (int) Math.round(Math.sin(System.currentTimeMillis() / 240.0) * 2);
        int x = client.getWindow().getGuiScaledWidth() - 43;
        int y = client.getWindow().getGuiScaledHeight() - 57 + bob;
        int cream = 0xFFFFF4E8;
        int shadow = 0xFFBFC8D0;
        int dark = 0xFF17202A;
        int horn = 0xFFD9A15D;
        graphics.fill(x + 8, y + 12, x + 27, y + 26, 0xB912191F);
        graphics.fill(x + 10, y + 14, x + 23, y + 23, cream);
        graphics.fill(x + 19, y + 7, x + 31, y + 19, cream);
        graphics.fill(x + 21, y + 3, x + 24, y + 8, horn);
        graphics.fill(x + 27, y + 3, x + 30, y + 8, horn);
        graphics.fill(x + 22, y + 11, x + 24, y + 13, dark);
        graphics.fill(x + 28, y + 11, x + 30, y + 13, dark);
        graphics.fill(x + 21, y + 23, x + 24, y + 28, shadow);
        graphics.fill(x + 12, y + 23, x + 15, y + 28, shadow);
        graphics.fill(x + 2, y + 29, x + 42, y + 42, 0xD912191F);
        graphics.centeredText(client.font, "CABRITO", x + 22, y + 32, ACCENT);
    }
}
