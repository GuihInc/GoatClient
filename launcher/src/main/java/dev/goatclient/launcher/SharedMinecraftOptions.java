package dev.goatclient.launcher;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Keeps Minecraft video, audio, controls, and accessibility settings shared across launcher instances. */
final class SharedMinecraftOptions {
    private SharedMinecraftOptions() {}

    static void load(Path launcherDirectory, Path gameDirectory) throws IOException {
        Path shared = launcherDirectory.resolve("options.txt");
        Path instance = gameDirectory.resolve("options.txt");
        if (Files.isRegularFile(shared)) {
            Files.copy(shared, instance, StandardCopyOption.REPLACE_EXISTING);
        } else if (Files.isRegularFile(instance)) {
            saveAtomically(instance, shared);
        }
    }

    static void save(Path launcherDirectory, Path gameDirectory) throws IOException {
        Path instance = gameDirectory.resolve("options.txt");
        if (Files.isRegularFile(instance)) saveAtomically(instance, launcherDirectory.resolve("options.txt"));
    }

    private static void saveAtomically(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), "minecraft-options-", ".tmp");
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
