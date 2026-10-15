package dev.goatclient.cosmetic;

import java.util.Arrays;

/** The same pixel pattern is used for the wardrobe preview and the cape texture. */
public enum CapeStyle {
    NONE("none", "Original", 0xFF89939E, 0xFF28313B),
    GOAT("goat", "Goat Original", 0xFF8BE9A4, 0xFF172C25),
    EMBER("ember", "Ember", 0xFFFFB36B, 0xFF381C26),
    FROST("frost", "Frost", 0xFF91D7FF, 0xFF18283E);

    private final String id;
    private final String title;
    private final int accent;
    private final int background;

    CapeStyle(String id, String title, int accent, int background) {
        this.id = id;
        this.title = title;
        this.accent = accent;
        this.background = background;
    }

    public String id() { return id; }
    public String title() { return title; }
    public int accent() { return accent; }

    public static CapeStyle byId(String id) {
        return Arrays.stream(values()).filter(style -> style.id.equals(id)).findFirst().orElse(NONE);
    }

    /** Color of a pixel on the 10 x 16 cape face. */
    public int pixel(int x, int y) {
        if (x == 0 || x == 9 || y == 15) return accent;
        if (this == NONE) return background;
        // Two horns, a face and a chin form a small goat emblem.
        boolean horns = (y == 3 && (x == 2 || x == 7))
                || (y == 4 && (x == 2 || x == 3 || x == 6 || x == 7));
        boolean face = (y == 6 && x >= 3 && x <= 6)
                || (y >= 7 && y <= 9 && (x == 2 || x == 7))
                || (y == 10 && (x == 3 || x == 6))
                || (y == 11 && (x == 4 || x == 5));
        if (horns || face) return accent;
        if (this == EMBER && y >= 13 && (x + y) % 3 == 0) return accent;
        if (this == FROST && y >= 13 && x % 2 == 0) return accent;
        return background;
    }
}
