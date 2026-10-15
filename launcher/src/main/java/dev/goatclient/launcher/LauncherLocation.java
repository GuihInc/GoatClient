package dev.goatclient.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stores the selected data folder outside that folder, so it survives location changes. */
public final class LauncherLocation {
    private final Path preferenceFile;

    public LauncherLocation(Platform platform) {
        preferenceFile = platform.configDirectory().resolve("launcher-location.json");
    }

    public Path load(Path fallback) throws IOException {
        if (!Files.exists(preferenceFile)) return fallback;
        try {
            var preference = JsonIO.GSON.fromJson(Files.readString(preferenceFile), Preference.class);
            if (preference == null || preference.dataDirectory == null || preference.dataDirectory.isBlank()) return fallback;
            return Path.of(preference.dataDirectory).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public void save(Path directory) throws IOException {
        JsonIO.write(preferenceFile, JsonIO.GSON.toJson(new Preference(directory.toAbsolutePath().normalize().toString())));
    }

    private record Preference(String dataDirectory) {}
}
