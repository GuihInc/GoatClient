package dev.goatclient.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfflineAccountTest {
    @Test void matchesMinecraftOfflineIdentity() {
        assertEquals(UUID.fromString("5627dd98-e6be-3c21-b8a8-e92344183641"), OfflineAccount.uuid("Steve"));
        assertEquals(OfflineAccount.uuid("Goat_123"), OfflineAccount.uuid(" Goat_123 "));
        assertNotEquals(OfflineAccount.uuid("Goat"), OfflineAccount.uuid("goat"));
    }

    @Test void rejectsInvalidNicknames() {
        for (String input : new String[]{"", "ab", "abcdefghijklmnopq", "Player Name", "José", "goat/../", "goat@example.com"}) {
            assertThrows(IllegalArgumentException.class, () -> OfflineAccount.validateName(input));
        }
        assertThrows(IllegalArgumentException.class, () -> OfflineAccount.validateName(null));
        assertEquals("Goat_123", OfflineAccount.validateName(" Goat_123 "));
    }
}
