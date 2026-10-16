package dev.goatclient;

import dev.goatclient.cosmetic.CapeStyle;
import dev.goatclient.auth.AccountManager;
import dev.goatclient.auth.ClientSession;
import dev.goatclient.auth.OfflineAccount;
import dev.goatclient.gui.AccountScreen;
import dev.goatclient.gui.GoatScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.gui.screens.TitleScreen;

/** Exercises the real UI, textures and mixin inside Minecraft. */
public final class GoatClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        context.runOnClient(client -> {
            client.options.guiScale().set(2);
            client.resizeGui();
            GoatClient.profile().showCoordinates = false;
            GoatClient.profile().cosmeticsEnabled = true;
        });
        var launcherUser = context.computeOnClient(client -> client.getUser());
        context.runOnClient(client -> {
            // A preparation failure must roll back credentials and keep the old session usable.
            var broken = new ClientSession(launcherUser, java.util.concurrent.CompletableFuture.completedFuture(null),
                    com.mojang.authlib.minecraft.UserApiService.OFFLINE,
                    java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("simulated permission failure")),
                    ProfileKeyPairManager.EMPTY_KEY_MANAGER, false);
            try {
                AccountManager.useMicrosoft(broken);
                throw new AssertionError("Broken session was accepted");
            } catch (IllegalStateException expected) {
                require(client.getUser() == launcherUser, "Failed session switch did not restore the launcher user");
                require(AccountManager.mode() == AccountManager.Mode.LAUNCHER, "Failed switch changed the account mode");
            }
        });
        context.clickScreenButton("GOAT");
        context.waitForScreen(GoatScreen.class);
        context.waitTicks(3);
        context.clickScreenButton("Ember");
        context.runOnClient(client -> require(GoatClient.profile().capeStyle() == CapeStyle.EMBER,
                "Wardrobe selection did not update the profile"));
        context.takeScreenshot("goatclient-wardrobe");
        context.clickScreenButton("Mods");
        context.clickScreenButton("Coordenadas XYZ: OFF");
        context.runOnClient(client -> require(GoatClient.profile().showCoordinates,
                "HUD toggle did not update the profile"));
        context.takeScreenshot("goatclient-hud-settings");
        context.clickScreenButton("Direção: OFF");
        context.runOnClient(client -> require(GoatClient.profile().showDirection,
                "Direction module did not update the profile"));
        context.clickScreenButton("Cosméticos");
        context.clickScreenButton("Pet cabrito no HUD: OFF");
        context.runOnClient(client -> require(GoatClient.profile().petEnabled,
                "Pet toggle did not update the profile"));
        context.clickScreenButton("GG");
        context.runOnClient(client -> require("GG!".equals(dev.goatclient.hud.EmoteOverlay.current()),
                "Local emote did not appear"));

        // Exercise the smallest supported GUI size and ensure controls stay on-screen.
        context.runOnClient(client -> {
            var screen = client.gui.screen();
            screen.resize(320, 240);
            for (var widget : Screens.getWidgets(screen)) {
                require(widget.getX() >= 0 && widget.getY() >= 0
                                && widget.getRight() <= 320 && widget.getBottom() <= 240,
                        "Widget outside compact screen: " + widget.getMessage().getString());
            }
        });
        context.clickScreenButton("Voltar");
        context.waitForScreen(TitleScreen.class);

        context.clickScreenButton("GOAT");
        context.clickScreenButton("Contas");
        context.waitForScreen(AccountScreen.class);
        context.clickScreenButton("Microsoft");
        context.runOnClient(client -> edit(client.gui.screen()).setValue(""));
        context.clickScreenButton("Entrar com Microsoft");
        // Validation must fail locally without replacing the launcher's session.
        context.waitTicks(5);
        context.runOnClient(client -> require(client.getUser() == launcherUser,
                "An unconfigured Microsoft login replaced the current session"));
        context.clickScreenButton("Offline");
        context.runOnClient(client -> edit(client.gui.screen()).setValue("a"));
        context.clickScreenButton("Entrar offline");
        context.runOnClient(client -> require(client.getUser() == launcherUser,
                "Invalid offline nickname replaced the current session"));
        context.runOnClient(client -> edit(client.gui.screen()).setValue("GoatOffline"));
        context.takeScreenshot("goatclient-accounts");
        context.clickScreenButton("Entrar offline");
        context.waitForScreen(GoatScreen.class);
        context.runOnClient(client -> {
            require(client.getUser().getProfileId().equals(OfflineAccount.uuid("GoatOffline")), "Incorrect offline UUID");
            require(client.getGameProfile().name().equals("GoatOffline"), "Game profile retained the previous identity");
            require(client.getUser().getAccessToken().equals("0"), "Offline session retained an online token");
            require(client.getProfileKeyPairManager() == ProfileKeyPairManager.EMPTY_KEY_MANAGER, "Offline session retained online chat keys");
            require(!client.allowsRealms() && !client.friendsEnabled(), "Offline session retained online-only services");
        });
        context.clickScreenButton("Frost");
        context.clickScreenButton("Contas");
        context.clickScreenButton("Usar conta do launcher");
        context.runOnClient(client -> {
            require(client.getUser() == launcherUser, "Launcher session was not restored");
            require(GoatClient.profile().capeStyle() == CapeStyle.EMBER, "Launcher cosmetics were overwritten by offline settings");
        });
        context.clickScreenButton("Contas");
        context.clickScreenButton("Offline");
        context.runOnClient(client -> edit(client.gui.screen()).setValue("GoatOffline"));
        context.clickScreenButton("Entrar offline");
        context.runOnClient(client -> require(GoatClient.profile().capeStyle() == CapeStyle.FROST,
                "Offline cosmetics were not restored by nickname"));
        context.clickScreenButton("Voltar");
        context.waitForScreen(TitleScreen.class);

        try (var world = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null);
            context.runOnClient(client -> {
                require(client.player.getGameProfile().name().equals("GoatOffline"), "World player did not use the offline name");
                require(client.player.getUUID().equals(OfflineAccount.uuid("GoatOffline")), "World player did not use the offline UUID");
                try {
                    AccountManager.useLauncher();
                    throw new AssertionError("Account switch was allowed inside a world");
                } catch (IllegalStateException expected) {
                    require(client.getUser().getName().equals("GoatOffline"), "Blocked switch partially replaced the session");
                }
                var profile = GoatClient.profile();
                profile.cosmeticsEnabled = false;
                var original = client.player.getSkin();
                profile.cosmeticsEnabled = true;
                for (var style : CapeStyle.values()) {
                    profile.cape = style.id();
                    var skin = client.player.getSkin();
                    require(skin.body().equals(original.body()), "Cape changed the player's body skin");
                    require(java.util.Objects.equals(skin.elytra(), original.elytra()), "Cape changed the elytra");
                    if (style == CapeStyle.NONE) {
                        require(skin.equals(original), "Original cape was not restored");
                    } else {
                        require(skin.cape() != null && skin.cape().texturePath().equals(GoatClient.id("capes/" + style.id())),
                                "Local player mixin did not apply " + style.id());
                    }
                }
                profile.wingsEnabled = true;
                profile.cape = CapeStyle.GOAT.id();
                var wingedSkin = client.player.getSkin();
                require(wingedSkin.elytra() != null
                                && wingedSkin.elytra().texturePath().equals(GoatClient.id("wings/goat")),
                        "Enabled wings texture was not applied to the player skin");
                profile.wingsEnabled = false;
                profile.cape = CapeStyle.GOAT.id();
                profile.showFps = true;
                profile.showKeystrokes = true;
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                client.gui.setScreen(null);
            });
            context.waitTicks(20);
            context.takeScreenshot("goatclient-in-game");
        }
        context.runOnClient(client -> AccountManager.useLauncher());
    }

    private static EditBox edit(net.minecraft.client.gui.screens.Screen screen) {
        return Screens.getWidgets(screen).stream().filter(EditBox.class::isInstance)
                .map(EditBox.class::cast).findFirst().orElseThrow();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
