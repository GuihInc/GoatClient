package dev.goatclient.launcher;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.ZipInputStream;

public final class MinecraftInstaller {
    public record Prepared(ReleaseCatalog.Release release, JsonObject minecraft, JsonObject fabric,
                           Path root, Path game, Path natives, Path java, List<Path> classpath, Path logConfig) {}
    private final Path root;
    private final Platform platform;
    private final Downloader downloads;

    public MinecraftInstaller(Path root, Platform platform, Downloader downloads) {
        this.root = root.toAbsolutePath().normalize();
        this.platform = platform;
        this.downloads = downloads;
    }

    public Prepared prepare(ReleaseCatalog.Release release, String javaOverride, Consumer<String> progress)
            throws IOException, InterruptedException {
        Files.createDirectories(root);
        Path game = root.resolve("instances/" + release.id());
        Path natives = game.resolve("natives");
        Files.createDirectories(natives);
        progress.accept("Consultando Minecraft " + release.minecraft() + "...");
        JsonObject manifest = downloads.metadata(URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"),
                root.resolve("metadata/minecraft-manifest.json"));
        JsonObject version = null;
        for (var element : manifest.getAsJsonArray("versions")) {
            var entry = element.getAsJsonObject();
            if (entry.get("id").getAsString().equals(release.minecraft())) { version = entry; break; }
        }
        if (version == null) throw new IOException("Essa versão do Minecraft não foi encontrada no catálogo oficial.");
        Path metadata = root.resolve("versions/" + release.minecraft() + "/" + release.minecraft() + ".json");
        download(version, metadata);
        JsonObject minecraft = JsonIO.object(Files.readString(metadata));
        if (!minecraft.get("id").getAsString().equals(release.minecraft())) throw new IOException("Metadados de Minecraft inconsistentes.");
        JsonObject fabric = downloads.metadata(URI.create("https://meta.fabricmc.net/v2/versions/loader/"
                + release.minecraft() + "/" + release.loader() + "/profile/json"), root.resolve("metadata/" + release.id() + "-fabric.json"));
        if (!fabric.get("inheritsFrom").getAsString().equals(release.minecraft())) throw new IOException("Perfil Fabric incompatível com a versão selecionada.");

        List<Path> classpath = new ArrayList<>();
        List<JsonObject> libraries = new ArrayList<>();
        for (var element : minecraft.getAsJsonArray("libraries")) libraries.add(element.getAsJsonObject());
        for (var element : fabric.getAsJsonArray("libraries")) libraries.add(element.getAsJsonObject());
        // Fabric overrides matching vanilla coordinates while native classifiers remain separate.
        var selected = new java.util.LinkedHashMap<String, JsonObject>();
        for (var library : libraries) {
            String[] name = library.get("name").getAsString().split(":");
            String key = name[0] + ":" + name[1] + (name.length > 3 ? ":" + name[3] : "");
            if (MinecraftRules.allowed(library, platform, Map.of())) selected.put(key, library);
        }
        int count = 0;
        for (var library : selected.values()) {
            Downloader.checkInterrupted();
            progress.accept("Bibliotecas " + (++count) + "/" + selected.size());
            if (library.has("downloads")) {
                var artifacts = library.getAsJsonObject("downloads");
                if (artifacts.has("artifact")) {
                    var artifact = artifacts.getAsJsonObject("artifact");
                    Path target = JsonIO.inside(root.resolve("libraries"), artifact.get("path").getAsString());
                    download(artifact, target);
                    classpath.add(target);
                }
                if (library.has("natives") && library.getAsJsonObject("natives").has(platform.os())) {
                    String classifier = library.getAsJsonObject("natives").get(platform.os()).getAsString()
                            .replace("${arch}", platform.arch().equals("x86") ? "32" : "64");
                    var artifact = artifacts.getAsJsonObject("classifiers").getAsJsonObject(classifier);
                    if (artifact == null) throw new IOException("Biblioteca nativa indisponível para este sistema.");
                    Path target = JsonIO.inside(root.resolve("libraries"), artifact.get("path").getAsString());
                    download(artifact, target);
                    extractNatives(target, natives);
                }
            } else {
                String path = mavenPath(library.get("name").getAsString());
                String repository = library.has("url") ? library.get("url").getAsString() : "https://libraries.minecraft.net/";
                URI url = URI.create(repository.endsWith("/") ? repository : repository + "/").resolve(path);
                Path target = JsonIO.inside(root.resolve("libraries"), path);
                String sha = library.has("sha1") ? library.get("sha1").getAsString()
                        : downloads.sha1(URI.create(url + ".sha1"), target.resolveSibling(target.getFileName() + ".sha1"));
                downloads.file(url, target, sha, "SHA-1");
                classpath.add(target);
            }
        }

        progress.accept("Baixando o jogo...");
        Path client = root.resolve("versions/" + release.minecraft() + "/" + release.minecraft() + ".jar");
        download(minecraft.getAsJsonObject("downloads").getAsJsonObject("client"), client);
        classpath.add(client);
        prepareAssets(minecraft, progress);

        progress.accept("Instalando GoatClient e Fabric API...");
        downloads.file(release.client().url(), game.resolve("mods/goatclient.jar"), release.client().sha256(), "SHA-256");
        URI api = URI.create("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/" + release.fabricApi()
                + "/fabric-api-" + release.fabricApi() + ".jar");
        String apiSha = downloads.sha1(URI.create(api + ".sha1"), root.resolve("metadata/" + release.id() + "-fabric-api.sha1"));
        downloads.file(api, game.resolve("mods/fabric-api.jar"), apiSha, "SHA-1");
        Path logConfig = null;
        if (minecraft.has("logging") && minecraft.getAsJsonObject("logging").has("client")) {
            var logging = minecraft.getAsJsonObject("logging").getAsJsonObject("client").getAsJsonObject("file");
            logConfig = JsonIO.inside(root.resolve("assets/log_configs"), logging.get("id").getAsString());
            download(logging, logConfig);
        }
        int javaMajor = minecraft.getAsJsonObject("javaVersion").get("majorVersion").getAsInt();
        Path java = JavaRuntime.resolve(javaMajor, javaOverride, root, platform, downloads, progress);
        return new Prepared(release, minecraft, fabric, root, game, natives, java, List.copyOf(classpath), logConfig);
    }

    private void prepareAssets(JsonObject minecraft, Consumer<String> progress) throws IOException, InterruptedException {
        var assetIndex = minecraft.getAsJsonObject("assetIndex");
        String indexId = assetIndex.get("id").getAsString();
        Path file = JsonIO.inside(root.resolve("assets/indexes"), indexId + ".json");
        download(assetIndex, file);
        var index = JsonIO.object(Files.readString(file));
        if (index.has("virtual") && index.get("virtual").getAsBoolean()) throw new IOException("Essa versão antiga usa assets virtuais ainda não suportados.");
        var hashes = new LinkedHashSet<String>();
        for (var value : index.getAsJsonObject("objects").entrySet()) {
            String hash = value.getValue().getAsJsonObject().get("hash").getAsString();
            if (!hash.matches("[a-f0-9]{40}")) throw new IOException("Asset com identificador inválido.");
            hashes.add(hash);
        }
        AtomicInteger completed = new AtomicInteger();
        progress.accept("Preparando " + hashes.size() + " assets...");
        var pool = Executors.newFixedThreadPool(8);
        try {
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
            for (String hash : hashes) futures.add(pool.submit(() -> {
                String path = hash.substring(0, 2) + "/" + hash;
                downloads.file(URI.create("https://resources.download.minecraft.net/" + path), root.resolve("assets/objects/" + path), hash, "SHA-1");
                int done = completed.incrementAndGet();
                if (done % 50 == 0 || done == hashes.size()) progress.accept("Assets " + done + "/" + hashes.size());
                return null;
            }));
            for (var future : futures) future.get();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof IOException io) throw io;
            if (exception.getCause() instanceof InterruptedException interrupted) throw interrupted;
            throw new IOException("Falha ao preparar os assets do Minecraft.");
        } finally { pool.shutdownNow(); }
    }

    private void download(JsonObject artifact, Path target) throws IOException, InterruptedException {
        downloads.file(URI.create(artifact.get("url").getAsString()), target, artifact.get("sha1").getAsString(), "SHA-1");
    }

    static String mavenPath(String coordinate) throws IOException {
        String[] parts = coordinate.split(":");
        if (parts.length < 3 || parts.length > 4) throw new IOException("Dependência inválida no catálogo Fabric.");
        return parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2]
                + (parts.length == 4 ? "-" + parts[3] : "") + ".jar";
    }

    static void extractNatives(Path archive, Path directory) throws IOException {
        try (var zip = new ZipInputStream(Files.newInputStream(archive))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || entry.getName().startsWith("META-INF/")) continue;
                Path target = JsonIO.inside(directory, entry.getName());
                Files.createDirectories(target.getParent());
                Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
