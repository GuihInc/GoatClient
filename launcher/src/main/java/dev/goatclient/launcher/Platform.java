package dev.goatclient.launcher;

import java.nio.file.Path;
import java.util.Locale;

public record Platform(String os, String arch, String version) {
    public static Platform current() {
        String name = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        return new Platform(name.contains("win") ? "windows" : name.contains("mac") ? "osx" : "linux",
                arch.equals("aarch64") || arch.equals("arm64") ? "aarch64" : arch.equals("x86") ? "x86" : "x86_64",
                System.getProperty("os.version"));
    }

    public String adoptiumOs() { return os.equals("osx") ? "mac" : os; }
    public String adoptiumArch() { return arch.equals("x86_64") ? "x64" : arch; }
    public String javaExecutable() { return os.equals("windows") ? "java.exe" : "java"; }

    public Path dataDirectory() {
        String user = System.getProperty("user.home");
        if (os.equals("windows")) {
            String appData = System.getenv("APPDATA");
            return Path.of(appData == null ? user + "/AppData/Roaming" : appData, "GoatClient");
        }
        if (os.equals("osx")) return Path.of(user, "Library", "Application Support", "GoatClient");
        String xdg = System.getenv("XDG_DATA_HOME");
        return Path.of(xdg == null || xdg.isBlank() ? user + "/.local/share" : xdg, "GoatClient");
    }
}
