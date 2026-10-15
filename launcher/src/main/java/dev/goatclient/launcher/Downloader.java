package dev.goatclient.launcher;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/** Atomic, checksum-verified downloads. Only complete files become visible to the game. */
public final class Downloader {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private final Path seed;

    public Downloader(Path seed) { this.seed = seed; }

    public static void requireHttps(URI uri) throws IOException {
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IOException("Use um endereço HTTPS válido, sem credenciais na URL.");
        }
    }

    public String text(URI uri) throws IOException, InterruptedException {
        requireHttps(uri);
        var response = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(45)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) throw new IOException("Download recusado pelo servidor (HTTP " + response.statusCode() + ").");
        if (response.body().length() > 16 * 1024 * 1024) throw new IOException("Resposta do servidor muito grande.");
        return response.body();
    }

    public JsonObject metadata(URI uri, Path cache) throws IOException, InterruptedException {
        try {
            String text = text(uri);
            JsonObject json = JsonIO.object(text);
            JsonIO.write(cache, text);
            return json;
        } catch (IOException exception) {
            if (Files.isRegularFile(cache)) return JsonIO.object(Files.readString(cache));
            throw exception;
        }
    }

    public String sha1(URI uri, Path cache) throws IOException, InterruptedException {
        requireHttps(uri);
        if (Files.isRegularFile(cache)) {
            String cached = Files.readString(cache).strip();
            if (cached.matches("[0-9a-fA-F]{40}")) return cached;
        }
        String checksum = text(uri).strip().split("\\s+")[0];
        if (!checksum.matches("[0-9a-fA-F]{40}")) throw new IOException("Checksum inválido recebido do servidor.");
        JsonIO.write(cache, checksum);
        return checksum;
    }

    public void file(URI uri, Path target, String checksum, String algorithm) throws IOException, InterruptedException {
        checkInterrupted();
        if (checksum == null || !checksum.matches(algorithm.equals("SHA-256") ? "[0-9a-fA-F]{64}" : "[0-9a-fA-F]{40}")) {
            throw new IOException("Checksum ausente ou inválido para um download.");
        }
        if (Files.isRegularFile(target) && JsonIO.hash(target, algorithm).equalsIgnoreCase(checksum)) return;
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(target.toAbsolutePath().getParent(), "download-", ".part");
        try {
            if ("resource".equals(uri.getScheme())) {
                try (var stream = Downloader.class.getResourceAsStream(uri.getPath())) {
                    if (stream == null) throw new IOException("O client incluído no launcher não foi encontrado.");
                    Files.copy(stream, temporary, StandardCopyOption.REPLACE_EXISTING);
                }
            } else if ("file".equals(uri.getScheme())) {
                Files.copy(Path.of(uri), temporary, StandardCopyOption.REPLACE_EXISTING);
            } else {
                requireHttps(uri);
                // Optional read-only asset cache reuse, also verified by its official hash.
                Path seeded = seed != null && uri.getHost().equals("resources.download.minecraft.net")
                        ? seed.resolve("assets/objects" + uri.getPath()) : null;
                if (seeded != null && Files.isRegularFile(seeded) && JsonIO.hash(seeded, algorithm).equalsIgnoreCase(checksum)) {
                    Files.copy(seeded, temporary, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    var response = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(10)).GET().build(),
                            HttpResponse.BodyHandlers.ofInputStream());
                    try (var input = response.body(); var output = Files.newOutputStream(temporary)) {
                        if (response.statusCode() != 200) throw new IOException("Download recusado (HTTP " + response.statusCode() + ").");
                        byte[] buffer = new byte[65536];
                        int length;
                        while ((length = input.read(buffer)) >= 0) {
                            checkInterrupted();
                            output.write(buffer, 0, length);
                        }
                    }
                }
            }
            checkInterrupted();
            if (!JsonIO.hash(temporary, algorithm).equalsIgnoreCase(checksum)) {
                throw new IOException("Um arquivo falhou na verificação de integridade. Tente novamente.");
            }
            JsonIO.replace(temporary, target);
        } finally { Files.deleteIfExists(temporary); }
    }

    public static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Operação cancelada");
    }
}
