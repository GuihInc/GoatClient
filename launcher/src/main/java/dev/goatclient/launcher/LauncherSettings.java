package dev.goatclient.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LauncherSettings {
    public String nickname = "GoatPlayer";
    public String catalogUrl = "";
    public String javaPath = "";
    public String selectedRelease = "";
    public int memoryMb = 2048;

    public static LauncherSettings load(Path root) throws IOException {
        Path file = root.resolve("launcher-settings.json");
        if (!Files.exists(file)) return new LauncherSettings();
        try {
            var settings = JsonIO.GSON.fromJson(Files.readString(file), LauncherSettings.class);
            if (settings == null) throw new IllegalArgumentException();
            settings.memoryMb = Math.max(1024, Math.min(16384, settings.memoryMb));
            if (settings.nickname == null) settings.nickname = "GoatPlayer";
            if (settings.catalogUrl == null) settings.catalogUrl = "";
            if (settings.javaPath == null) settings.javaPath = "";
            if (settings.selectedRelease == null) settings.selectedRelease = "";
            return settings;
        } catch (RuntimeException exception) {
            Files.move(file, file.resolveSibling("launcher-settings.broken-" + System.currentTimeMillis() + ".json"));
            return new LauncherSettings();
        }
    }

    public void save(Path root) throws IOException {
        JsonIO.write(root.resolve("launcher-settings.json"), JsonIO.GSON.toJson(this));
    }
}
