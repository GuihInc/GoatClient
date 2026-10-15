package dev.goatclient.launcher;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LauncherTest {
    @TempDir Path directory;
    private final Platform linux = new Platform("linux", "x86_64", "7.0.0");
    private final Platform windows = new Platform("windows", "x86_64", "10.0.22631");
    private final Platform mac = new Platform("osx", "aarch64", "15.5");

    @Test void importsFutureReleasesWithoutReplacingTheBundledClient() throws Exception {
        Path jar = directory.resolve("next.jar");
        Files.writeString(jar, "test future client");
        Path manifest = directory.resolve("versions.json");
        Files.writeString(manifest, catalog("next.jar", JsonIO.hash(jar, "SHA-256")));
        ReleaseCatalog catalog = new ReleaseCatalog(directory.resolve("data"));
        catalog.importFile(manifest);
        var releases = catalog.load();
        assertEquals(2, releases.size());
        var future = releases.stream().filter(release -> release.id().equals("goat-next")).findFirst().orElseThrow();
        assertEquals(jar.toUri(), future.client().url());
        assertEquals("26.4", future.minecraft());
        // Reloading a cached/imported manifest keeps absolute local paths valid.
        assertEquals(releases, new ReleaseCatalog(directory.resolve("data")).load());
    }

    @Test void rejectsTraversalIdsMissingHashesAndRemoteLocalFiles() {
        String sha = "a".repeat(64);
        assertThrows(java.io.IOException.class, () -> ReleaseCatalog.parse(catalog("client.jar", sha).replace("goat-next", "../escape"), URI.create("https://example.com/versions.json"), false));
        assertThrows(java.io.IOException.class, () -> ReleaseCatalog.parse(catalog("client.jar", "bad"), URI.create("https://example.com/versions.json"), false));
        assertThrows(java.io.IOException.class, () -> ReleaseCatalog.parse(catalog("file:///etc/passwd", sha), URI.create("https://example.com/versions.json"), false));
        assertThrows(java.io.IOException.class, () -> ReleaseCatalog.parse(catalog("http://example.com/client.jar", sha), URI.create("https://example.com/versions.json"), false));
    }

    @Test void verifiesChecksumBeforeReplacingExistingFiles() throws Exception {
        Path source = directory.resolve("source.jar");
        Path target = directory.resolve("mods/client.jar");
        Files.writeString(source, "downloaded file");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "previous client");
        Downloader downloader = new Downloader(null);
        assertThrows(java.io.IOException.class, () -> downloader.file(source.toUri(), target, "a".repeat(64), "SHA-256"));
        assertEquals("previous client", Files.readString(target));
        downloader.file(source.toUri(), target, JsonIO.hash(source, "SHA-256"), "SHA-256");
        assertEquals("downloaded file", Files.readString(target));
        try (var files = Files.list(target.getParent())) { assertEquals(1, files.count()); }
    }

    @Test void extractsNativeLibrariesWithoutZipSlip() throws Exception {
        Path zip = directory.resolve("natives.zip");
        try (var output = new ZipOutputStream(Files.newOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("../../escaped.so"));
            output.write(new byte[]{1});
            output.closeEntry();
        }
        Path destination = directory.resolve("natives");
        Files.createDirectories(destination);
        assertThrows(java.io.IOException.class, () -> MinecraftInstaller.extractNatives(zip, destination));
        assertFalse(Files.exists(directory.resolve("escaped.so")));
        assertThrows(java.io.IOException.class, () -> JavaRuntime.extract(zip, destination, true));
    }

    @Test void reusesVerifiedVersionChecksumsWithoutNetwork() throws Exception {
        Path cached = directory.resolve("library.jar.sha1");
        Files.writeString(cached, "b".repeat(40));
        assertEquals("b".repeat(40), new Downloader(null).sha1(URI.create("https://example.invalid/library.jar.sha1"), cached));
    }

    @Test void selectsLibrariesForAllSupportedPlatformsAndHonorsRuleOrder() {
        var armMac = JsonParser.parseString("""
                {"rules":[{"action":"allow","os":{"name":"osx","arch":"aarch64"}}]}
                """).getAsJsonObject();
        assertTrue(MinecraftRules.allowed(armMac, mac, Map.of()));
        assertFalse(MinecraftRules.allowed(armMac, linux, Map.of()));
        assertFalse(MinecraftRules.allowed(armMac, windows, Map.of()));
        var rule = JsonParser.parseString("""
                {"rules":[{"action":"allow"},{"action":"disallow","os":{"name":"linux"}}]}
                """).getAsJsonObject();
        assertFalse(MinecraftRules.allowed(rule, linux, Map.of()));
        assertTrue(MinecraftRules.allowed(rule, windows, Map.of()));
    }

    @Test void expandsOnlyEnabledMinecraftArgumentsAndPreservesPathsWithSpaces() throws Exception {
        var array = JsonParser.parseString("""
                ["--gameDir","${game_directory}",
                 {"rules":[{"action":"allow","features":{"is_demo_user":true}}],"value":"--demo"},
                 {"rules":[{"action":"allow","os":{"name":"windows"}}],"value":["--windows","enabled"]}]
                """).getAsJsonArray();
        var arguments = new java.util.ArrayList<String>();
        LaunchCommand.append(arguments, array, windows, Map.of("is_demo_user", false), Map.of("game_directory", "C:\\My Games\\GoatClient"));
        assertEquals(List.of("--gameDir", "C:\\My Games\\GoatClient", "--windows", "enabled"), arguments);
        assertTrue(LaunchCommand.argumentFile(arguments).contains("C:\\\\My Games\\\\GoatClient"));
        assertThrows(java.io.IOException.class, () -> LaunchCommand.expand("${unsupported}", Map.of()));
    }

    @Test void storesPreferencesWithoutAccountTokensAndDetectsTheInstalledJava() throws Exception {
        LauncherSettings settings = new LauncherSettings();
        settings.nickname = "GoatTest";
        settings.memoryMb = 3072;
        settings.save(directory);
        assertEquals(3072, LauncherSettings.load(directory).memoryMb);
        assertFalse(Files.readString(directory.resolve("launcher-settings.json")).contains("token"));
        Path java = Path.of(System.getProperty("java.home"), "bin", Platform.current().javaExecutable());
        assertEquals(Runtime.version().feature(), JavaRuntime.major(java));
        assertFalse(LaunchCommand.Account.offline("GoatTest").toString().contains("token"));
    }

    private static String catalog(String url, String sha) {
        return """
                {"schemaVersion":1,"releases":[{"id":"goat-next","name":"GoatClient Next", "minecraft":"26.4",
                "loader":"0.20.0","fabricApi":"0.163.0+26.4","client":{"url":"%s","sha256":"%s"}}]}
                """.formatted(url, sha);
    }
}
