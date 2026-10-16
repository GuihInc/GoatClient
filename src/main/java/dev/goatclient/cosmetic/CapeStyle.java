package dev.goatclient.cosmetic;

import java.util.Arrays;

/** The same pixel pattern is used for the wardrobe preview and the cape texture. */
public enum CapeStyle {
    NONE("none", "Original", 0xFF89939E, 0xFF28313B),
    GOAT("goat", "Goat Original", 0xFF8BE9A4, 0xFF172C25),
    EMBER("ember", "Ember", 0xFFFFB36B, 0xFF381C26),
    FROST("frost", "Frost", 0xFF91D7FF, 0xFF18283E),
    AURORA("aurora", "Aurora", 0xFF72F1C2, 0xFF182D46),
    VOID("void", "Void", 0xFFCA8BFF, 0xFF171329),
    OCEAN("ocean", "Ocean", 0xFF4AD6D6, 0xFF10354A),
    SUNSET("sunset", "Sunset", 0xFFFF7892, 0xFF41223D),
    ROSE("rose", "Rose", 0xFFFF9BCB, 0xFF45213C),
    MINT("mint", "Mint", 0xFFB6F27C, 0xFF233B2A),
    CHERRY("cherry", "Cherry", 0xFFFF596B, 0xFF3E1826);

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
        boolean mark = switch (this) {
            case GOAT -> goat(x, y);
            case EMBER -> flame(x, y);
            case FROST -> snowflake(x, y);
            case AURORA -> Math.floorMod(y + x / 2, 6) <= 1 || (y > 7 && x == (y + 1) % 10);
            case VOID -> (x * 7 + y * 11) % 19 == 0 || (x == 4 && y == 7) || (x == 5 && y == 8);
            case OCEAN -> Math.floorMod(y + (x / 2), 5) <= 1;
            case SUNSET -> (y >= 5 && y <= 8 && x >= 3 && x <= 6)
                    || (y == 9 && (x == 2 || x == 7)) || (y >= 11 && y % 2 == 0);
            case ROSE -> (y >= 4 && y <= 7 && (x == 4 || x == 5))
                    || (y == 5 && (x == 3 || x == 6)) || (y >= 8 && x == 5)
                    || (y == 10 && (x == 3 || x == 4 || x == 6 || x == 7));
            case MINT -> (y >= 3 && y <= 12 && x == (y < 8 ? y - 2 : 14 - y))
                    || (y >= 3 && y <= 12 && x == (y < 8 ? 11 - y : y - 4));
            case CHERRY -> (y >= 4 && y <= 6 && (x == 3 || x == 6))
                    || (y == 3 && (x == 2 || x == 4 || x == 5 || x == 7))
                    || (y >= 7 && x >= y - 5 && x <= 9 - (y - 7));
            default -> false;
        };
        return mark ? accent : background;
    }

    private static boolean goat(int x, int y) {
        boolean horns = (y == 3 && (x == 2 || x == 7))
                || (y == 4 && (x == 2 || x == 3 || x == 6 || x == 7));
        boolean face = (y == 6 && x >= 3 && x <= 6)
                || (y >= 7 && y <= 9 && (x == 2 || x == 7))
                || (y == 10 && (x == 3 || x == 6))
                || (y == 11 && (x == 4 || x == 5));
        return horns || face;
    }

    private static boolean flame(int x, int y) {
        int center = y < 7 ? 4 : (y < 12 ? 3 : 4);
        int spread = y < 3 ? 1 : y < 7 ? 2 : y < 12 ? 3 : 2;
        return Math.abs(x - center) <= spread && y >= 2 && y <= 14
                && ((x + y) % 4 != 0 || y > 11);
    }

    private static boolean snowflake(int x, int y) {
        return (x == 4 || x == 5 || y == 7 || y == 8)
                || ((x + y == 11 || x + y == 12 || x - y == -3 || x - y == -4)
                && x >= 2 && x <= 7 && y >= 4 && y <= 11);
    }
}
