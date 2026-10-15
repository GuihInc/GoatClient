package dev.goatclient.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class JsonIO {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private JsonIO() {}

    public static JsonObject object(String json) throws IOException {
        try { return JsonParser.parseString(json).getAsJsonObject(); }
        catch (RuntimeException exception) { throw new IOException("Arquivo JSON inválido."); }
    }

    public static Path inside(Path root, String relative) throws IOException {
        Path result = root.resolve(relative).normalize();
        if (Path.of(relative).isAbsolute() || !result.startsWith(root.normalize()) || result.equals(root.normalize())) {
            throw new IOException("Caminho inválido nos arquivos da versão.");
        }
        return result;
    }

    public static String hash(Path file, String algorithm) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[65536];
                int length;
                while ((length = input.read(buffer)) >= 0) digest.update(buffer, 0, length);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) { throw new IllegalArgumentException(algorithm, exception); }
    }

    public static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "goat-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            replace(temporary, file);
        } finally { Files.deleteIfExists(temporary); }
    }

    public static void replace(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException exception) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); }
    }
}
