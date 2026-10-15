package dev.goatclient.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.goatclient.auth.MicrosoftAccount;
import dev.goatclient.auth.OfflineAccount;
import java.io.IOException;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class LaunchCommand {
    public record Account(String name, UUID uuid, String token, String xuid, String clientId, boolean online, Instant expiresAt) {
        public static Account offline(String nickname) {
            String name = OfflineAccount.validateName(nickname);
            return new Account(name, OfflineAccount.uuid(name), "0", "", "", false, Instant.MAX);
        }
        public static Account microsoft(MicrosoftAccount account) {
            return new Account(account.name(), account.uuid(), account.accessToken(), account.xuid(), account.clientId(), true, account.expiresAt());
        }
        @Override public String toString() { return "Account[name=" + name + ", credentials=REDACTED]"; }
    }

    private LaunchCommand() {}

    public static List<String> arguments(MinecraftInstaller.Prepared prepared, Account account, int memoryMb, Platform platform) throws IOException {
        if (!account.expiresAt().isAfter(Instant.now())) throw new IOException("A sessão Microsoft expirou. Faça login novamente.");
        String classpath = prepared.classpath().stream().map(path -> path.toAbsolutePath().toString()).collect(Collectors.joining(File.pathSeparator));
        var values = new java.util.HashMap<String, String>();
        values.put("auth_player_name", account.name());
        values.put("auth_uuid", account.uuid().toString().replace("-", ""));
        values.put("auth_access_token", account.token());
        values.put("auth_session", "token:" + account.token() + ":" + values.get("auth_uuid"));
        values.put("auth_xuid", account.xuid());
        values.put("clientid", account.clientId());
        values.put("user_type", account.online() ? "msa" : "legacy");
        values.put("user_properties", "{}");
        values.put("version_name", prepared.fabric().get("id").getAsString());
        values.put("version_type", "release");
        values.put("game_directory", prepared.game().toString());
        values.put("assets_root", prepared.root().resolve("assets").toString());
        values.put("assets_index_name", prepared.minecraft().getAsJsonObject("assetIndex").get("id").getAsString());
        values.put("natives_directory", prepared.natives().toString());
        values.put("library_directory", prepared.root().resolve("libraries").toString());
        values.put("classpath", classpath);
        values.put("classpath_separator", File.pathSeparator);
        values.put("launcher_name", "GoatClientLauncher");
        values.put("launcher_version", "1.0.0");
        values.put("resolution_width", "1280");
        values.put("resolution_height", "720");
        Map<String, Boolean> features = Map.of("has_custom_resolution", true, "is_demo_user", false);
        List<String> arguments = new ArrayList<>();
        arguments.add("-Xms256M");
        arguments.add("-Xmx" + Math.max(1024, Math.min(16384, memoryMb)) + "M");
        // Keep temporary native-library extraction on the game drive, not a small /tmp mount.
        arguments.add("-Djava.io.tmpdir=" + prepared.natives());
        var vanilla = prepared.minecraft().getAsJsonObject("arguments");
        if (vanilla == null) throw new IOException("O launcher exige metadados modernos de Minecraft (arguments).");
        append(arguments, vanilla.getAsJsonArray("jvm"), platform, features, values);
        var fabric = prepared.fabric().getAsJsonObject("arguments");
        if (fabric != null) append(arguments, fabric.getAsJsonArray("jvm"), platform, features, values);
        if (prepared.logConfig() != null) {
            String logging = prepared.minecraft().getAsJsonObject("logging").getAsJsonObject("client").get("argument").getAsString();
            arguments.add(logging.replace("${path}", prepared.logConfig().toString()));
        }
        arguments.add(prepared.fabric().get("mainClass").getAsString());
        append(arguments, vanilla.getAsJsonArray("game"), platform, features, values);
        if (fabric != null) append(arguments, fabric.getAsJsonArray("game"), platform, features, values);
        return List.copyOf(arguments);
    }

    static void append(List<String> output, JsonArray arguments, Platform platform, Map<String, Boolean> features,
                       Map<String, String> values) throws IOException {
        if (arguments == null) return;
        for (var entry : arguments) {
            if (entry.isJsonPrimitive()) output.add(expand(entry.getAsString(), values));
            else {
                JsonObject object = entry.getAsJsonObject();
                if (!MinecraftRules.allowed(object, platform, features)) continue;
                var value = object.get("value");
                if (value.isJsonArray()) for (var text : value.getAsJsonArray()) output.add(expand(text.getAsString(), values));
                else output.add(expand(value.getAsString(), values));
            }
        }
    }

    static String expand(String input, Map<String, String> values) throws IOException {
        String output = input;
        for (var entry : values.entrySet()) output = output.replace("${" + entry.getKey() + "}", entry.getValue());
        if (output.contains("${")) throw new IOException("Essa versão usa um argumento ainda não suportado pelo launcher.");
        return output;
    }

    public static String argumentFile(List<String> arguments) {
        return arguments.stream().map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"").collect(Collectors.joining("\n"));
    }
}
