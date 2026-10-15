package dev.goatclient.auth;

import java.time.Instant;
import java.util.UUID;

/** Access tokens remain in memory and must never appear in diagnostic output. */
public record MicrosoftAccount(String name, UUID uuid, String accessToken, String xuid,
                               String clientId, Instant expiresAt) {
    @Override
    public String toString() {
        return "MicrosoftAccount[name=" + name + ", uuid=" + uuid + ", credentials=REDACTED]";
    }
}
