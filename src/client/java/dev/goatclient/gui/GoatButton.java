package dev.goatclient.gui;

import dev.goatclient.cosmetic.CapeStyle;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class GoatButton extends Button {
    private final BooleanSupplier selected;
    private final CapeStyle cape;

    GoatButton(int x, int y, int width, int height, String text, OnPress action, BooleanSupplier selected) {
        this(x, y, width, height, text, action, selected, null);
    }

    GoatButton(int x, int y, int width, int height, String text, OnPress action,
               BooleanSupplier selected, CapeStyle cape) {
        super(x, y, width, height, Component.literal(text), action, DEFAULT_NARRATION);
        this.selected = selected;
        this.cape = cape;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int accent = cape == null ? 0xFF8BE9A4 : cape.accent();
        boolean chosen = selected.getAsBoolean();
        graphics.fill(getX(), getY(), getRight(), getBottom(),
                chosen ? 0xFF23382E : isHoveredOrFocused() ? 0xFF293440 : 0xFF1B242E);
        graphics.outline(getX(), getY(), getWidth(), getHeight(),
                chosen || isHoveredOrFocused() ? accent : 0xFF34404C);
        var font = Minecraft.getInstance().font;
        String label = font.plainSubstrByWidth(getMessage().getString(), getWidth() - 12);
        if (cape != null && getHeight() >= 66) {
            int scale = getHeight() >= 90 ? 3 : 2;
            int left = getX() + (getWidth() - 10 * scale) / 2;
            int top = getY() + 7;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 10; x++) {
                    graphics.fill(left + x * scale, top + y * scale,
                            left + (x + 1) * scale, top + (y + 1) * scale, cape.pixel(x, y));
                }
            }
            graphics.centeredText(font, label, getX() + getWidth() / 2, getBottom() - 14, 0xFFF0F4F8);
        } else {
            graphics.centeredText(font, label, getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, chosen ? accent : 0xFFF0F4F8);
        }
    }
}
