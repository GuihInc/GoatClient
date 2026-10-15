package dev.goatclient.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Release data is separate from launcher code, so future clients need no launcher rebuild. */
public final class ReleaseCatalog {
    public record Artifact(URI url, String sha256) {}
    public record Release(String id, String name, String minecraft, String loader, String fabricApi, Artifact client) {
        @Override public String toString() { return name + " / Minecraft " + minecraft; }
    }

    private final Path root;
    public ReleaseCatalog(Path root) { this.root = root; }

    public List<Release> load() throws IOException {
        var releases = new LinkedHashMap<String, Release>();
        try (var stream = ReleaseCatalog.class.getResourceAsStream("/catalog.json")) {
            if (stream == null) throw new IOException("O catálogo incluído no launcher não foi encontrado.");
            for (Release release : parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), null, true)) {
                releases.put(release.id, release);
            }
        }
        Path custom = root.resolve("catalog.json");
        if (Files.exists(custom)) for (Release release : parse(Files.readString(custom), custom.toUri(), false)) {
            releases.put(release.id, release);
        }
        return List.copyOf(releases.values());
    }

    public void importFile(Path file) throws IOException {
        merge(parse(Files.readString(file), file.toUri(), false));
    }

    public void update(URI source, Downloader downloads) throws IOException, InterruptedException {
        Downloader.requireHttps(source);
        merge(parse(downloads.text(source), source, false));
    }

    private void merge(List<Release> imported) throws IOException {
        var all = new LinkedHashMap<String, Release>();
        Path file = root.resolve("catalog.json");
        if (Files.exists(file)) for (var release : parse(Files.readString(file), file.toUri(), false)) all.put(release.id, release);
        for (var release : imported) all.put(release.id, release);
        JsonObject output = new JsonObject();
        output.addProperty("schemaVersion", 1);
        JsonArray entries = new JsonArray();
        for (var release : all.values()) entries.add(JsonIO.GSON.toJsonTree(release));
        output.add("releases", entries);
        JsonIO.write(file, JsonIO.GSON.toJson(output));
    }

    public static List<Release> parse(String json, URI base, boolean bundled) throws IOException {
        try {
            JsonObject catalog = JsonIO.object(json);
            if (catalog.get("schemaVersion").getAsInt() != 1) throw new IOException("Versão de catálogo não suportada.");
            List<Release> result = new ArrayList<>();
            var ids = new java.util.HashSet<String>();
            for (var element : catalog.getAsJsonArray("releases")) {
                JsonObject entry = element.getAsJsonObject();
                String id = value(entry, "id");
                if (!id.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,95}") || !ids.add(id)) {
                    throw new IOException("ID de versão inválido ou duplicado.");
                }
                String minecraft = version(entry, "minecraft");
                String loader = version(entry, "loader");
                String api = version(entry, "fabricApi");
                JsonObject client = entry.getAsJsonObject("client");
                URI uri = URI.create(value(client, "url"));
                if (!uri.isAbsolute()) {
                    if (base == null) throw new IOException("O arquivo da versão precisa de um endereço completo.");
                    uri = base.resolve(uri);
                }
                if ("resource".equals(uri.getScheme())) {
                    if (!bundled) throw new IOException("Somente o catálogo incluído pode usar recursos internos.");
                } else if ("file".equals(uri.getScheme())) {
                    if (base == null || !"file".equals(base.getScheme())) throw new IOException("Catálogos remotos não podem acessar arquivos locais.");
                } else Downloader.requireHttps(uri);
                String sha = value(client, "sha256");
                if (!sha.matches("[a-fA-F0-9]{64}")) throw new IOException("A versão precisa de um SHA-256 válido.");
                result.add(new Release(id, value(entry, "name"), minecraft, loader, api, new Artifact(uri, sha.toLowerCase())));
            }
            if (result.isEmpty()) throw new IOException("O catálogo não contém versões.");
            return List.copyOf(result);
        } catch (RuntimeException exception) { throw new IOException("Catálogo de versões inválido."); }
    }

    private static String value(JsonObject object, String field) throws IOException {
        String value = object.get(field).getAsString();
        if (value.isBlank()) throw new IOException("Campo obrigatório ausente: " + field);
        return value;
    }

    private static String version(JsonObject object, String field) throws IOException {
        String value = value(object, field);
        if (!value.matches("[a-zA-Z0-9._+\\-]+")) throw new IOException("Versão inválida: " + field);
        return value;
    }
}
