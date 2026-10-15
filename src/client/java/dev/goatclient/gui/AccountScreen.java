package dev.goatclient.gui;

import dev.goatclient.GoatClient;
import dev.goatclient.auth.AccountManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public final class AccountScreen extends Screen {
    private final Screen parent;
    private String nickname;
    private String message = "Perfil offline: o nick não é verificado pela Microsoft.";
    private boolean error;
    private EditBox input;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public AccountScreen(Screen parent) {
        super(Component.literal("Conta offline / GoatClient"));
        this.parent = parent;
        var settings = GoatClient.authSettings();
        nickname = settings.offlineName == null ? "" : settings.offlineName;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(520, width - 20);
        panelHeight = Math.min(230, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int contentWidth = panelWidth - 24;

        input = new EditBox(font, left + 12, top + 100, contentWidth, 22, Component.literal("Nick offline"));
        input.setMaxLength(16);
        input.setValue(nickname);
        input.setHint(Component.literal("SeuNick (3 a 16 caracteres)"));
        input.setResponder(value -> {
            nickname = value;
            if (error) {
                error = false;
                message = "Perfil offline: o nick não é verificado pela Microsoft.";
            }
        });
        addRenderableWidget(input);

        var login = new GoatButton(left + 12, top + 132, contentWidth, 24, "Usar perfil offline", button -> {
            try {
                AccountManager.useOffline(nickname);
                GoatClient.authSettings().offlineName = nickname.strip();
                GoatClient.save();
                minecraft.gui.setScreen(new GoatScreen(new TitleScreen()));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                error = true;
                message = exception.getMessage();
            }
        }, () -> false);
        login.active = AccountManager.canSwitch(minecraft);
        addRenderableWidget(login);

        addRenderableWidget(new GoatButton(left + panelWidth - 78, top + panelHeight - 28, 66, 20,
                "Voltar", button -> onClose(), () -> false));
    }

    @Override
    protected void setInitialFocus() {
        if (input != null) setInitialFocus(input);
        else super.setInitialFocus();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xF0091018, 0xF013201E);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF121A23);
        graphics.outline(left, top, panelWidth, panelHeight, 0xFF35443F);
        graphics.fill(left, top, left + 3, top + panelHeight, 0xFF8BE9A4);
        GoatBrand.header(graphics, font, left, top, panelWidth, "GOAT / CONTA OFFLINE",
                "Atual: " + minecraft.getUser().getName() + " / Offline");
        graphics.text(font, "Nick offline", left + 12, top + 84, 0xFFF0F4F8, false);
        graphics.textWithWordWrap(font, Component.literal(message), left + 12, top + 166,
                panelWidth - 24, error ? 0xFFFFB36B : 0xFF99A7B5, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
