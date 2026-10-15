package dev.goatclient.launcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public final class JavaRuntime {
    private JavaRuntime() {}

    public static Path resolve(int required, String override, Path root, Platform platform,
                               Downloader downloads, Consumer<String> progress) throws IOException, InterruptedException {
        if (override != null && !override.isBlank()) {
            Path java = Path.of(override).toAbsolutePath();
            if (major(java) < required) throw new IOException("O Java selecionado precisa ser versão " + required + " ou superior.");
            return java;
        }
        Path installation = root.resolve("runtimes/java-" + required + "-" + platform.adoptiumArch());
        Path installed = installation.resolve("bin/" + platform.javaExecutable());
        if (Files.isRegularFile(installed) && major(installed) >= required) return installed;
        Path current = Path.of(System.getProperty("java.home"), "bin", platform.javaExecutable());
        if (Runtime.version().feature() == required && Files.isRegularFile(current)) return current;

        progress.accept("Baixando Java " + required + " para " + platform.adoptiumOs() + "...");
        String url = "https://api.adoptium.net/v3/assets/latest/" + required + "/hotspot?architecture="
                + platform.adoptiumArch() + "&image_type=jre&os=" + platform.adoptiumOs() + "&vendor=eclipse";
        var assets = JsonParser.parseString(downloads.text(URI.create(url))).getAsJsonArray();
        if (assets.isEmpty()) throw new IOException("Não há Java disponível para este sistema/arquitetura.");
        JsonObject archive = assets.get(0).getAsJsonObject().getAsJsonObject("binary").getAsJsonObject("package");
        String extension = platform.os().equals("windows") ? ".zip" : ".tar.gz";
        Path download = root.resolve("downloads/java-" + required + "-" + platform.adoptiumOs() + "-" + platform.adoptiumArch() + extension);
        downloads.file(URI.create(archive.get("link").getAsString()), download, archive.get("checksum").getAsString(), "SHA-256");
        Files.createDirectories(installation.getParent());
        Path staging = Files.createTempDirectory(installation.getParent(), "java-install-");
        try {
            extract(download, staging, platform.os().equals("windows"));
            Path java;
            try (var files = Files.walk(staging)) {
                java = files.filter(path -> path.getFileName().toString().equals(platform.javaExecutable())
                        && path.getParent().getFileName().toString().equals("bin")).findFirst()
                        .orElseThrow(() -> new IOException("O pacote Java não contém um executável."));
            }
            Path home = java.getParent().getParent();
            if (Files.exists(installation)) throw new IOException("Há uma instalação Java incompleta. Escolha um Java nas configurações.");
            Files.move(home, installation);
            if (!platform.os().equals("windows")) installed.toFile().setExecutable(true, true);
            if (major(installed) < required) throw new IOException("O Java baixado não corresponde à versão necessária.");
            return installed;
        } finally {
            // This is exclusively the unique temporary extraction directory created above.
            try (var files = Files.walk(staging)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    static int major(Path executable) throws IOException, InterruptedException {
        if (!Files.isRegularFile(executable)) throw new IOException("Executável Java não encontrado.");
        Process process = new ProcessBuilder(executable.toString(), "-version").redirectErrorStream(true).start();
        if (!process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("O executável Java não respondeu.");
        }
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        var matcher = java.util.regex.Pattern.compile("version \\\"(?:1\\.)?(\\d+)").matcher(output);
        if (process.exitValue() != 0 || !matcher.find()) throw new IOException("Não foi possível identificar a versão do Java.");
        return Integer.parseInt(matcher.group(1));
    }

    static void extract(Path archive, Path destination, boolean zip) throws IOException {
        if (zip) {
            try (var input = new ZipInputStream(Files.newInputStream(archive))) {
                java.util.zip.ZipEntry entry;
                while ((entry = input.getNextEntry()) != null) {
                    Path target = JsonIO.inside(destination, entry.getName());
                    if (entry.isDirectory()) Files.createDirectories(target);
                    else { Files.createDirectories(target.getParent()); Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING); }
                }
            }
        } else {
            try (var input = new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(archive)))) {
                org.apache.commons.compress.archivers.tar.TarArchiveEntry entry;
                while ((entry = input.getNextEntry()) != null) {
                    Path target = JsonIO.inside(destination, entry.getName());
                    if (entry.isDirectory()) Files.createDirectories(target);
                    else if (entry.isSymbolicLink()) {
                        Path link = Path.of(entry.getLinkName());
                        if (link.isAbsolute() || !target.getParent().resolve(link).normalize().startsWith(destination)) {
                            throw new IOException("Link inválido no pacote Java.");
                        }
                        Files.createDirectories(target.getParent());
                        Files.createSymbolicLink(target, link);
                    } else if (entry.isFile()) {
                        Files.createDirectories(target.getParent());
                        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                        if ((entry.getMode() & 0111) != 0) target.toFile().setExecutable(true, true);
                    } else throw new IOException("Entrada não suportada no pacote Java.");
                }
            }
        }
    }
}
