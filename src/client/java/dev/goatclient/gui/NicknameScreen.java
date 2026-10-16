package dev.goatclient.gui;

import dev.goatclient.GoatClient;
import dev.goatclient.config.ProfileSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Local-only nameplate editor; it never changes the authenticated game profile. */
public final class NicknameScreen extends Screen {
    private final Screen parent;
    private String nickname;
    private boolean showSymbol;
    private String message = "Apenas visual: o servidor e os outros jogadores não recebem este nick.";
    private boolean error;
    private EditBox input;
    private GoatButton symbolButton;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public NicknameScreen(Screen parent) {
        super(Component.literal("GoatClient / Nick"));
        this.parent = parent;
        ProfileSettings profile = GoatClient.profile();
        nickname = profile.displayNickname == null ? "" : profile.displayNickname;
        showSymbol = profile.showGoatSymbol;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(520, width - 20);
        panelHeight = Math.min(250, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int contentWidth = panelWidth - 24;

        input = new EditBox(font, left + 12, top + 100, contentWidth, 22, Component.literal("Nick local"));
        input.setMaxLength(16);
        input.setValue(nickname);
        input.setHint(Component.literal("Nick (3 a 16: letras, números e _ )"));
        input.setResponder(value -> nickname = value);
        addRenderableWidget(input);

        symbolButton = new GoatButton(left + 12, top + 132, contentWidth, 24, symbolLabel(), button -> {
            showSymbol = !showSymbol;
            button.setMessage(Component.literal(symbolLabel()));
        }, () -> showSymbol);
        addRenderableWidget(symbolButton);
        addRenderableWidget(new GoatButton(left + 12, top + 164, contentWidth / 2 - 4, 24,
                "Salvar nick", button -> save(), () -> false));
        addRenderableWidget(new GoatButton(left + 16 + contentWidth / 2, top + 164, contentWidth / 2 - 4, 24,
                "Usar nick da conta", button -> {
                    nickname = "";
                    input.setValue("");
                    save();
                }, () -> false));
        addRenderableWidget(new GoatButton(left + panelWidth - 78, top + panelHeight - 28, 66, 20,
                "Voltar", button -> onClose(), () -> false));
    }

    private String symbolLabel() {
        return "Símbolo [GC]: " + (showSymbol ? "ON" : "OFF");
    }

    private void save() {
        String value = nickname == null ? "" : nickname.strip();
        if (!value.isEmpty() && !value.matches("[A-Za-z0-9_]{3,16}")) {
            error = true;
            message = "Use de 3 a 16 letras, números ou _; ou deixe vazio para usar o nick da conta.";
            return;
        }
        ProfileSettings profile = GoatClient.profile();
        profile.displayNickname = value;
        profile.showGoatSymbol = showSymbol;
        GoatClient.save();
        error = false;
        message = "Salvo neste dispositivo. Só aparece sobre seu personagem neste client.";
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
        GoatBrand.header(graphics, font, left, top, panelWidth, "GOAT / NICK",
                "F5  /  editor visual local");
        graphics.text(font, "Nick exibido sobre você em terceira pessoa", left + 12, top + 84,
                0xFFF0F4F8, false);
        graphics.textWithWordWrap(font, Component.literal(message), left + 12, top + 198,
                panelWidth - 24, error ? 0xFFFFB36B : 0xFF99A7B5, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
