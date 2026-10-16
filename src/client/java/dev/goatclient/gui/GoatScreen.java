package dev.goatclient.gui;

import dev.goatclient.GoatClient;
import dev.goatclient.auth.AccountManager;
import dev.goatclient.config.ProfileSettings;
import dev.goatclient.cosmetic.CapeStyle;
import dev.goatclient.hud.EmoteOverlay;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

public final class GoatScreen extends Screen {
    private static final int TEXT = 0xFFF0F4F8;
    private static final int MUTED = 0xFF99A7B5;
    private static final int GREEN = 0xFF8BE9A4;
    private final Screen parent;
    private final Supplier<PlayerSkin> skinLookup;
    private boolean hudTab;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int sidebar;
    private int contentX;
    private int contentWidth;
    private int previewX;

    public GoatScreen(Screen parent) {
        super(Component.literal("GoatClient"));
        this.parent = parent;
        skinLookup = minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), true);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(720, width - 20);
        panelHeight = Math.min(390, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        sidebar = Math.max(96, Math.min(140, panelWidth / 4));
        contentX = left + sidebar + 12;
        contentWidth = panelWidth - sidebar - 24;
        previewX = 0;
        if (!hudTab && panelWidth >= 600) {
            contentWidth -= 144;
            previewX = contentX + contentWidth + 12;
            PlayerSkinWidget skin = new PlayerSkinWidget(120, panelHeight - 154,
                    minecraft.getEntityModels(), skinLookup);
            skin.setX(previewX);
            skin.setY(top + 84);
            addRenderableWidget(skin);
        }
        addRenderableWidget(new GoatButton(left + 10, top + 64, sidebar - 20, 24,
                "Cosméticos", button -> switchTab(false), () -> !hudTab));
        addRenderableWidget(new GoatButton(left + 10, top + 94, sidebar - 20, 24,
                "Mods", button -> switchTab(true), () -> hudTab));
        var accounts = new GoatButton(left + 10, top + 124, sidebar - 20, 24,
                "Contas", button -> minecraft.gui.setScreen(new AccountScreen(this)), () -> false);
        accounts.active = AccountManager.canSwitch(minecraft);
        accounts.setTooltip(Tooltip.create(Component.literal(accounts.active
                ? "Perfil offline do GoatClient."
                : "Saia do mundo ou servidor para trocar de conta.")));
        addRenderableWidget(accounts);
        addRenderableWidget(new GoatButton(left + panelWidth - 78, top + panelHeight - 28,
                66, 20, "Voltar", button -> onClose(), () -> false));

        if (hudTab) initHud();
        else initWardrobe();
    }

    private void switchTab(boolean hud) {
        hudTab = hud;
        rebuildWidgets();
    }

    private void initWardrobe() {
        ProfileSettings profile = GoatClient.profile();
        int cardWidth = (contentWidth - 8) / 2;
        int cardHeight = 48;
        int index = 0;
        for (CapeStyle cape : CapeStyle.values()) {
            int x = contentX + (index % 2) * (cardWidth + 8);
            int y = top + 82 + (index / 2) * (cardHeight + 6);
            var card = new GoatButton(x, y, cardWidth, cardHeight, cape.title(), button -> {
                profile.cape = cape.id();
                GoatClient.save();
            }, () -> profile.capeStyle() == cape, cape);
            card.setTooltip(Tooltip.create(Component.literal(cape == CapeStyle.NONE
                    ? "Usar a capa original da sua conta, se houver."
                    : "Equipar " + cape.title() + " neste dispositivo. Use F5 para ver no jogo.")));
            addRenderableWidget(card);
            index++;
        }
        toggle(contentX, top + 194, contentWidth, "Cosméticos",
                () -> profile.cosmeticsEnabled, () -> profile.cosmeticsEnabled = !profile.cosmeticsEnabled);
        toggle(contentX, top + 222, contentWidth, "Asas (visíveis usando elytra)",
                () -> profile.wingsEnabled, () -> profile.wingsEnabled = !profile.wingsEnabled);
        toggle(contentX, top + 250, contentWidth, "Pet cabrito no HUD",
                () -> profile.petEnabled, () -> profile.petEnabled = !profile.petEnabled);
        int emoteWidth = (contentWidth - 16) / 3;
        emote(contentX, top + 292, emoteWidth, "GG", "GG!");
        emote(contentX + emoteWidth + 8, top + 292, emoteWidth, "Coração", "<3");
        emote(contentX + (emoteWidth + 8) * 2, top + 292, emoteWidth, "Aceno", "o/");
    }

    private void initHud() {
        ProfileSettings profile = GoatClient.profile();
        int gap = 8;
        int columnWidth = (contentWidth - gap) / 2;
        module(contentX, top + 85, columnWidth, "FPS", () -> profile.showFps,
                () -> profile.showFps = !profile.showFps);
        module(contentX + columnWidth + gap, top + 85, columnWidth, "Coordenadas XYZ", () -> profile.showCoordinates,
                () -> profile.showCoordinates = !profile.showCoordinates);
        module(contentX, top + 117, columnWidth, "Teclas de movimento", () -> profile.showKeystrokes,
                () -> profile.showKeystrokes = !profile.showKeystrokes);
        module(contentX + columnWidth + gap, top + 117, columnWidth, "Direção", () -> profile.showDirection,
                () -> profile.showDirection = !profile.showDirection);
        module(contentX, top + 149, columnWidth, "Relógio", () -> profile.showClock,
                () -> profile.showClock = !profile.showClock);
        module(contentX + columnWidth + gap, top + 149, columnWidth, "Status de corrida", () -> profile.showSprintStatus,
                () -> profile.showSprintStatus = !profile.showSprintStatus);
    }

    private void module(int x, int y, int width, String name, BooleanSupplier enabled, Runnable action) {
        toggle(x, y, width, name, enabled, action);
    }

    private void emote(int x, int y, int width, String label, String message) {
        addRenderableWidget(new GoatButton(x, y, width, 24, label,
                button -> EmoteOverlay.show(message), () -> false));
    }

    private void toggle(int x, int y, int width, String name, BooleanSupplier value, Runnable action) {
        addRenderableWidget(new GoatButton(x, y, width, 24, toggleLabel(name, value.getAsBoolean()), button -> {
            action.run();
            button.setMessage(Component.literal(toggleLabel(name, value.getAsBoolean())));
            GoatClient.save();
        }, value));
    }

    private static String toggleLabel(String name, boolean enabled) {
        return name + ": " + (enabled ? "ON" : "OFF");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xF0091018, 0xF013201E);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF121A23);
        graphics.outline(left, top, panelWidth, panelHeight, 0xFF35443F);
        graphics.fill(left, top, left + 3, top + panelHeight, GREEN);
        graphics.fill(left + sidebar, top + 54, left + sidebar + 1, top + panelHeight - 36, 0xFF2C3943);
        graphics.fill(left + 12, top + 52, left + panelWidth - 12, top + 53, 0xFF2C3943);
        GoatBrand.header(graphics, font, left, top, panelWidth, "GOAT", "CLIENT / 26.3");
        String name = minecraft.getUser().getName();
        String greeting = font.plainSubstrByWidth(name, panelWidth - 170);
        graphics.text(font, greeting, left + panelWidth - 16 - font.width(greeting), top + 14, TEXT, false);
        String status = GoatClient.preferencesSaved() ? "Salvo neste dispositivo" : "Falha ao salvar: veja o log";
        status = font.plainSubstrByWidth(status, panelWidth - 170);
        graphics.text(font, status, left + panelWidth - 16 - font.width(status), top + 30,
                GoatClient.preferencesSaved() ? MUTED : 0xFFFFB36B, false);

        graphics.text(font, "SEU PERFIL", left + 12, top + 164, MUTED, false);
        graphics.text(font, font.plainSubstrByWidth(name, sidebar - 24), left + 12, top + 179, TEXT, false);
        if (panelHeight >= 270) graphics.text(font, minecraft.getUser().getProfileId().toString().substring(0, 8),
                left + 12, top + 194, MUTED, false);
        graphics.text(font, hudTab ? "Mods do HUD" : "Guarda-roupa e emotes", contentX, top + 64, TEXT, false);
        if (previewX != 0) {
            graphics.fill(previewX, top + 82, previewX + 120, top + panelHeight - 46, 0xFF0D151D);
            graphics.centeredText(font, "Sua skin", previewX + 60, top + 65, MUTED);
            graphics.centeredText(font, "Arraste para girar", previewX + 60, top + panelHeight - 55, MUTED);
        }
        String footer = hudTab ? "Módulos locais • configurações por jogador" : "Cosméticos locais • asas aparecem com elytra";
        graphics.text(font, font.plainSubstrByWidth(footer, panelWidth - 100),
                left + 12, top + panelHeight - 22, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
