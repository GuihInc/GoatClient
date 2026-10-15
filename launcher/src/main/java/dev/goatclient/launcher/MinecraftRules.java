package dev.goatclient.launcher;

import com.google.gson.JsonObject;
import java.util.Map;

/** Evaluates Mojang's OS and feature rules rather than hardcoding one platform's libraries. */
public final class MinecraftRules {
    private MinecraftRules() {}

    public static boolean allowed(JsonObject object, Platform platform, Map<String, Boolean> features) {
        if (!object.has("rules")) return true;
        boolean allowed = false;
        for (var element : object.getAsJsonArray("rules")) {
            JsonObject rule = element.getAsJsonObject();
            if (matches(rule, platform, features)) allowed = rule.get("action").getAsString().equals("allow");
        }
        return allowed;
    }

    private static boolean matches(JsonObject rule, Platform platform, Map<String, Boolean> features) {
        if (rule.has("os")) {
            JsonObject os = rule.getAsJsonObject("os");
            if (os.has("name") && !os.get("name").getAsString().equals(platform.os())) return false;
            if (os.has("arch")) {
                String arch = os.get("arch").getAsString();
                if (!(arch.equals(platform.arch()) || (arch.equals("amd64") && platform.arch().equals("x86_64")))) return false;
            }
            if (os.has("version") && !platform.version().matches(os.get("version").getAsString())) return false;
            if (os.has("versionRange")) {
                JsonObject range = os.getAsJsonObject("versionRange");
                if (range.has("min") && compare(platform.version(), range.get("min").getAsString()) < 0) return false;
                if (range.has("max") && compare(platform.version(), range.get("max").getAsString()) > 0) return false;
            }
        }
        if (rule.has("features")) for (var entry : rule.getAsJsonObject("features").entrySet()) {
            if (features.getOrDefault(entry.getKey(), false) != entry.getValue().getAsBoolean()) return false;
        }
        return true;
    }

    private static int compare(String first, String second) {
        String[] a = first.split("[^0-9]+");
        String[] b = second.split("[^0-9]+");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            long x = i < a.length && !a[i].isEmpty() ? Long.parseLong(a[i]) : 0;
            long y = i < b.length && !b[i].isEmpty() ? Long.parseLong(b[i]) : 0;
            if (x != y) return Long.compare(x, y);
        }
        return 0;
    }
}
