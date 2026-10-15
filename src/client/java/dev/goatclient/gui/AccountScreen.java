package dev.goatclient.gui;

import com.mojang.blaze3d.Blaze3D;
import dev.goatclient.GoatClient;
import dev.goatclient.auth.AccountManager;
import dev.goatclient.auth.MicrosoftAuth;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public final class AccountScreen extends Screen {
    private enum Page { LAUNCHER, MICROSOFT, OFFLINE }
    private final Screen parent;
    private Page page = Page.LAUNCHER;
    private String nickname;
    private String applicationId;
    private String message = "Use a sessão já autenticada pelo seu launcher.";
    private boolean error;
    private boolean busy;
    private Thread loginTask;
    private int attempt;
    private MicrosoftAuth.DeviceCode deviceCode;
    private EditBox input;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public AccountScreen(Screen parent) {
        super(Component.literal("Contas / GoatClient"));
        this.parent = parent;
        var settings = GoatClient.authSettings();
        nickname = settings.offlineName == null ? "" : settings.offlineName;
        applicationId = settings.microsoftClientId == null ? "" : settings.microsoftClientId;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(560, width - 20);
        panelHeight = Math.min(280, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int tabWidth = (panelWidth - 32) / 3;
        Page[] pages = Page.values();
        String[] labels = {"Launcher", "Microsoft", "Offline"};
        for (int index = 0; index < pages.length; index++) {
            Page choice = pages[index];
            addRenderableWidget(new GoatButton(left + 12 + index * (tabWidth + 4), top + 52,
                    tabWidth, 22, labels[index], button -> switchPage(choice), () -> page == choice));
        }
        addRenderableWidget(new GoatButton(left + panelWidth - 78, top + panelHeight - 28, 66, 20,
                "Voltar", button -> onClose(), () -> false));
        int contentWidth = panelWidth - 24;
        input = null;
        if (page == Page.LAUNCHER) {
            var use = new GoatButton(left + 12, top + 100, contentWidth, 24, "Usar conta do launcher", button -> {
                try {
                    AccountManager.useLauncher();
                    finishSwitch();
                } catch (IllegalStateException exception) {
                    fail(exception.getMessage());
                }
            }, () -> AccountManager.mode() == AccountManager.Mode.LAUNCHER);
            use.active = AccountManager.canSwitch(minecraft);
            addRenderableWidget(use);
        } else if (page == Page.OFFLINE) {
            input = new EditBox(font, left + 12, top + 100, contentWidth, 22, Component.literal("Nick offline"));
            input.setMaxLength(16);
            input.setValue(nickname);
            input.setHint(Component.literal("SeuNick (3 a 16 caracteres)"));
            input.setResponder(value -> {
                nickname = value;
                if (error) {
                    error = false;
                    message = "Para mundos locais e servidores offline. O nick não é verificado pela Microsoft.";
                }
            });
            addRenderableWidget(input);
            var login = new GoatButton(left + 12, top + 128, contentWidth, 24, "Entrar offline", button -> {
                try {
                    AccountManager.useOffline(nickname);
                    GoatClient.authSettings().offlineName = nickname.strip();
                    GoatClient.save();
                    finishSwitch();
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    fail(exception.getMessage());
                }
            }, () -> false);
            login.active = AccountManager.canSwitch(minecraft);
            addRenderableWidget(login);
        } else if (deviceCode == null) {
            input = new EditBox(font, left + 12, top + 100, contentWidth, 22, Component.literal("Application (client) ID"));
            input.setMaxLength(36);
            input.setValue(applicationId);
            input.setHint(Component.literal("Application (client) ID do GoatClient"));
            input.setResponder(value -> {
                applicationId = value;
                if (error) {
                    error = false;
                    message = "Sem Application ID? Use a aba Launcher. O login próprio exige um app Microsoft configurado.";
                }
            });
            input.setEditable(!busy);
            addRenderableWidget(input);
            var login = new GoatButton(left + 12, top + 128, contentWidth, 24,
                    busy ? "Cancelar login" : "Entrar com Microsoft", button -> {
                        if (busy) cancelLogin();
                        else beginMicrosoft();
                    }, () -> busy);
            login.active = AccountManager.canSwitch(minecraft);
            addRenderableWidget(login);
        } else {
            int half = (contentWidth - 6) / 2;
            addRenderableWidget(new GoatButton(left + 12, top + 100, half, 24, "Abrir Microsoft", button ->
                    Blaze3D.openUri(deviceCode.verificationUri()), () -> false));
            addRenderableWidget(new GoatButton(left + 18 + half, top + 100, half, 24, "Copiar código", button ->
                    minecraft.keyboardHandler.setClipboard(deviceCode.userCode()), () -> false));
            addRenderableWidget(new GoatButton(left + 12, top + 128, contentWidth, 24, "Cancelar login",
                    button -> cancelLogin(), () -> false));
        }
    }

    private void switchPage(Page target) {
        cancelTask();
        page = target;
        error = false;
        message = switch (page) {
            case LAUNCHER -> "Use a sessão já autenticada pelo seu launcher.";
            case OFFLINE -> "Para mundos locais e servidores offline. O nick não é verificado pela Microsoft.";
            case MICROSOFT -> "Sem Application ID? Use a aba Launcher. O login próprio exige um app Microsoft configurado.";
        };
        rebuildWidgets();
    }

    private void beginMicrosoft() {
        if (!AccountManager.canSwitch(minecraft)) {
            fail("Saia do mundo ou servidor antes de trocar de conta.");
            return;
        }
        cancelTask();
        int generation = attempt;
        busy = true;
        error = false;
        message = "Solicitando código de login...";
        String clientId = applicationId.strip();
        rebuildWidgets();
        loginTask = Thread.ofVirtual().name("goatclient-microsoft-login").start(() -> {
            try {
                MicrosoftAuth auth = new MicrosoftAuth();
                var code = auth.begin(clientId);
                publish(generation, () -> {
                    deviceCode = code;
                    GoatClient.authSettings().microsoftClientId = code.clientId();
                    GoatClient.save();
                    message = "Abra a Microsoft e informe o código acima. A senha é digitada apenas no navegador.";
                    rebuildWidgets();
                });
                var account = auth.finish(code, progress -> publish(generation, () -> message = progress));
                var session = AccountManager.prepareMicrosoft(account);
                publish(generation, () -> {
                    try {
                        AccountManager.useMicrosoft(session);
                        finishSwitch();
                    } catch (IllegalStateException exception) {
                        fail(exception.getMessage());
                    }
                });
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (MicrosoftAuth.AuthException exception) {
                publish(generation, () -> fail(exception.getMessage()));
            } catch (IOException exception) {
                publish(generation, () -> fail("Não foi possível concluir o login. Confira sua conexão e a configuração do app Microsoft."));
            } catch (RuntimeException exception) {
                publish(generation, () -> fail("Não foi possível carregar essa conta. Tente novamente."));
            }
        });
    }

    private void publish(int generation, Runnable action) {
        minecraft.execute(() -> {
            if (attempt == generation && minecraft.gui.screen() == this) action.run();
        });
    }

    private void fail(String text) {
        cancelTask();
        error = true;
        message = text;
        rebuildWidgets();
    }

    private void cancelTask() {
        attempt++;
        if (loginTask != null) loginTask.interrupt();
        loginTask = null;
        busy = false;
        deviceCode = null;
    }

    private void cancelLogin() {
        cancelTask();
        message = "Login cancelado. Sua conta atual foi mantida.";
        error = false;
        rebuildWidgets();
    }

    private void finishSwitch() {
        minecraft.gui.setScreen(new GoatScreen(new TitleScreen()));
    }

    @Override
    protected void setInitialFocus() {
        if (input != null && page == Page.OFFLINE) setInitialFocus(input);
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
        String kind = switch (AccountManager.mode()) {
            case LAUNCHER -> "Launcher";
            case MICROSOFT -> "Microsoft";
            case OFFLINE -> "Offline";
        };
        GoatBrand.header(graphics, font, left, top, panelWidth, "GOAT / CONTAS",
                "Atual: " + minecraft.getUser().getName() + " / " + kind);
        String label = switch (page) {
            case LAUNCHER -> "Conta do launcher: " + AccountManager.launcherName();
            case OFFLINE -> "Nick offline";
            case MICROSOFT -> deviceCode == null ? "Application (client) ID" : "Código Microsoft: " + deviceCode.userCode();
        };
        graphics.text(font, font.plainSubstrByWidth(label, panelWidth - 24), left + 12, top + 84, 0xFFF0F4F8, false);
        graphics.textWithWordWrap(font, Component.literal(message), left + 12, top + 158,
                panelWidth - 24, error ? 0xFFFFB36B : 0xFF99A7B5, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public void removed() { cancelTask(); }
    @Override public boolean isPauseScreen() { return false; }
}
