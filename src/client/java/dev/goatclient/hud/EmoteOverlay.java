package dev.goatclient.hud;

/** Small local-only emote toast; it does not send chat or packets to a server. */
public final class EmoteOverlay {
    private static String message = "";
    private static long expiresAt;

    private EmoteOverlay() {}

    public static void show(String text) {
        message = text;
        expiresAt = System.currentTimeMillis() + 2200;
    }

    public static String current() {
        return System.currentTimeMillis() < expiresAt ? message : "";
    }
}
