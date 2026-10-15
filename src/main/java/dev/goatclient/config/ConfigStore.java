package dev.goatclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.goatclient.auth.AuthSettings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Stores preferences by player UUID; never stores login credentials. */
public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final State state;

    private ConfigStore(Path file, State state) {
        this.file = file;
        this.state = state;
    }

    public static ConfigStore memoryOnly() {
        return new ConfigStore(null, new State());
    }

    public static ConfigStore load(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        if (!Files.exists(absolute)) return new ConfigStore(absolute, new State());
        State state;
        String json = Files.readString(absolute, StandardCharsets.UTF_8);
        try {
            state = GSON.fromJson(json, State.class);
            if (state == null) throw new JsonParseException("Empty configuration");
            if (state.schemaVersion != 1) {
                // Preserve newer configurations rather than rewriting their unknown fields.
                throw new IOException("Unsupported GoatClient config schema: " + state.schemaVersion);
            }
            if (state.profiles == null) state.profiles = new HashMap<>();
            if (state.auth == null) state.auth = new AuthSettings();
            state.profiles.entrySet().removeIf(entry -> entry.getValue() == null);
            state.profiles.values().forEach(profile -> profile.cape = profile.capeStyle().id());
        } catch (JsonParseException | IllegalStateException exception) {
            Path backup = absolute.resolveSibling(absolute.getFileName() + ".broken-" + UUID.randomUUID());
            Files.move(absolute, backup);
            state = new State();
        }
        return new ConfigStore(absolute, state);
    }

    public ProfileSettings profile(UUID uuid) {
        return state.profiles.computeIfAbsent(uuid.toString(), ignored -> new ProfileSettings());
    }

    public boolean isPersistent() {
        return file != null;
    }

    public AuthSettings authSettings() {
        return state.auth;
    }

    public void save() throws IOException {
        if (file == null) throw new IOException("Preferences are in memory only");
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "goatclient-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(state), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static final class State {
        int schemaVersion = 1;
        Map<String, ProfileSettings> profiles = new HashMap<>();
        AuthSettings auth = new AuthSettings();
    }
}
