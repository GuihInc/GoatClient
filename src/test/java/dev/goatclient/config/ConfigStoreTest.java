package dev.goatclient.config;

import static org.junit.jupiter.api.Assertions.*;

import dev.goatclient.cosmetic.CapeStyle;
import dev.goatclient.cosmetic.PetStyle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigStoreTest {
    @TempDir Path directory;

    @Test
    void persistsOnlyPublicAuthPreferences() throws IOException {
        Path file = directory.resolve("goatclient.json");
        ConfigStore store = ConfigStore.load(file);
        store.authSettings().offlineName = "GoatOffline";
        store.authSettings().microsoftClientId = "21ce9eb9-4ae6-45bf-a7de-a72e75fda111";
        store.save();
        var loaded = ConfigStore.load(file);
        assertEquals("GoatOffline", loaded.authSettings().offlineName);
        assertEquals(store.authSettings().microsoftClientId, loaded.authSettings().microsoftClientId);
        assertFalse(Files.readString(file).toLowerCase().contains("token"));
    }

    @Test
    void keepsAuthenticatedProfilesSeparateAcrossReloads() throws IOException {
        Path file = directory.resolve("nested/goatclient.json");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ConfigStore store = ConfigStore.load(file);
        store.profile(first).cape = CapeStyle.EMBER.id();
        store.profile(first).pet = PetStyle.FOX.id();
        store.profile(first).showCoordinates = true;
        store.profile(second).cape = CapeStyle.FROST.id();
        store.profile(second).cosmeticsEnabled = false;
        store.save();

        ConfigStore loaded = ConfigStore.load(file);
        assertEquals(CapeStyle.EMBER, loaded.profile(first).capeStyle());
        assertEquals(PetStyle.FOX, loaded.profile(first).petStyle());
        assertTrue(loaded.profile(first).showCoordinates);
        assertEquals(CapeStyle.FROST, loaded.profile(second).capeStyle());
        assertFalse(loaded.profile(second).cosmeticsEnabled);
        assertFalse(loaded.profile(second).showCoordinates);
    }

    @Test
    void preservesCorruptFileBeforeRecovering() throws IOException {
        Path file = directory.resolve("goatclient.json");
        String corrupt = "{ damaged json";
        Files.writeString(file, corrupt);
        ConfigStore loaded = ConfigStore.load(file);
        loaded.save();
        try (var files = Files.list(directory)) {
            Path backup = files.filter(path -> path.getFileName().toString().startsWith("goatclient.json.broken-"))
                    .findFirst().orElseThrow();
            assertEquals(corrupt, Files.readString(backup));
        }
        assertTrue(Files.readString(file).contains("schemaVersion"));
    }

    @Test
    void unknownCapeAndNullProfileDoNotBreakLoading() throws IOException {
        UUID player = UUID.randomUUID();
        Path file = directory.resolve("goatclient.json");
        Files.writeString(file, """
                {"schemaVersion":1,"profiles":{"%s":{"cape":"missing-style"},"removed":null}}
                """.formatted(player));
        ConfigStore loaded = ConfigStore.load(file);
        assertEquals(CapeStyle.NONE, loaded.profile(player).capeStyle());
        assertEquals(PetStyle.GOAT, loaded.profile(player).petStyle());
        assertTrue(loaded.profile(player).showFps);
        loaded.save();
        assertFalse(Files.readString(file).contains("removed"));
    }

    @Test
    void preservesNewerSchemaWithoutRewritingIt() throws IOException {
        Path file = directory.resolve("goatclient.json");
        String newer = "{\"schemaVersion\":2,\"profiles\":{}}";
        Files.writeString(file, newer);
        assertThrows(IOException.class, () -> ConfigStore.load(file));
        assertEquals(newer, Files.readString(file));
    }

    @Test
    void replacingPreferencesLeavesNoTemporaryFiles() throws IOException {
        Path file = directory.resolve("goatclient.json");
        UUID player = UUID.randomUUID();
        ConfigStore store = ConfigStore.load(file);
        store.save();
        store.profile(player).cape = CapeStyle.NONE.id();
        store.save();
        assertEquals(CapeStyle.NONE, ConfigStore.load(file).profile(player).capeStyle());
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }
}
