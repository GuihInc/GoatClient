package dev.goatclient;

import com.mojang.blaze3d.platform.InputConstants;
import dev.goatclient.config.ConfigStore;
import dev.goatclient.config.ProfileSettings;
import dev.goatclient.auth.AccountManager;
import dev.goatclient.auth.AuthSettings;
import dev.goatclient.cosmetic.CapeTextures;
import dev.goatclient.gui.GoatScreen;
import dev.goatclient.gui.GoatMenuButton;
import dev.goatclient.gui.NicknameScreen;
import dev.goatclient.hud.GoatHud;
import java.io.IOException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GoatClient implements ClientModInitializer {
    public static final String ID = "goatclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    private static ConfigStore config = ConfigStore.memoryOnly();
    private static boolean lastSaveSucceeded;

    @Override
    public void onInitializeClient() {
        try {
            config = ConfigStore.load(FabricLoader.getInstance().getConfigDir().resolve("goatclient.json"));
            lastSaveSucceeded = true;
        } catch (IOException exception) {
            LOGGER.error("Could not load GoatClient preferences; using memory only", exception);
        }
        KeyMapping.Category category = KeyMapping.Category.register(id("client"));
        KeyMapping wardrobe = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.goatclient.wardrobe", InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT, category));
        KeyMapping nickname = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.goatclient.nickname", InputConstants.Type.KEYBOARD, InputConstants.KEY_F5, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (wardrobe.consumeClick()) {
                if (client.gui.screen() == null) client.gui.setScreen(new GoatScreen(null));
            }
            while (nickname.consumeClick()) {
                if (client.gui.screen() == null && client.player != null) {
                    client.gui.setScreen(new NicknameScreen(null));
                }
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof TitleScreen) && !(screen instanceof PauseScreen)) return;
            // Keep the entry point outside the vanilla menu's central button column.
            Screens.getWidgets(screen).add(new GoatMenuButton(screen));
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("hud"), GoatHud::extract);
        ClientLifecycleEvents.CLIENT_STARTED.register(CapeTextures::register);
        ClientLifecycleEvents.CLIENT_STARTED.register(AccountManager::initialize);
        LOGGER.info("GoatClient initialized for Minecraft 26.3");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    public static ProfileSettings profile() {
        return config.profile(Minecraft.getInstance().getUser().getProfileId());
    }

    public static String displayNickname() {
        String nickname = profile().displayNickname;
        return nickname == null || nickname.isBlank() ? Minecraft.getInstance().getUser().getName() : nickname;
    }

    public static AuthSettings authSettings() {
        return config.authSettings();
    }

    public static boolean save() {
        try {
            config.save();
            lastSaveSucceeded = true;
        } catch (IOException exception) {
            lastSaveSucceeded = false;
            LOGGER.error("Could not save GoatClient preferences", exception);
        }
        return lastSaveSucceeded;
    }

    public static boolean preferencesSaved() {
        return config.isPersistent() && lastSaveSucceeded;
    }
}
