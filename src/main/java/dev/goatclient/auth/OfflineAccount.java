package dev.goatclient.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class OfflineAccount {
    private OfflineAccount() {}

    public static String validateName(String input) {
        String name = input == null ? "" : input.strip();
        if (!name.matches("[A-Za-z0-9_]{3,16}")) {
            throw new IllegalArgumentException("Use de 3 a 16 letras, números ou _ no nick.");
        }
        return name;
    }

    public static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + validateName(name)).getBytes(StandardCharsets.UTF_8));
    }
}
