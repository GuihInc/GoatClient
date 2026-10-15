package dev.goatclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Logo entry point on Minecraft's title and pause screens. */
public final class GoatMenuButton extends Button {
    public GoatMenuButton(Screen parent) {
        super(6, 6, 72, 24, Component.literal("GOAT"),
                button -> Minecraft.getInstance().gui.setScreen(new GoatScreen(parent)), DEFAULT_NARRATION);
        setTooltip(Tooltip.create(Component.literal("Cosméticos e HUD do GoatClient")));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(getX(), getY(), getRight(), getBottom(), isHoveredOrFocused() ? 0xFF293440 : 0xFF1B242E);
        graphics.outline(getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused() ? 0xFF8BE9A4 : 0xFF34404C);
        GoatBrand.logo(graphics, getX() + 4, getY() + 3, 18);
        graphics.text(Minecraft.getInstance().font, getMessage(), getX() + 30, getY() + 8, 0xFF8BE9A4, false);
    }
}
