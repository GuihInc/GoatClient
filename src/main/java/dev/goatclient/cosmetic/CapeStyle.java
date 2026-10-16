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

    /** Color of a pixel on the 10 x 16 cape face, with a shaded cloth base and a restrained emblem. */
    public int pixel(int x, int y) {
        if (this == NONE) return background;
        int cloth = background;
        if (x == 0 || x == 9 || y == 15) cloth = shade(background, 0.72F);
        else if ((x == 1 || x == 8) && y > 1) cloth = shade(background, 0.84F);
        else if ((x + y * 2) % 11 == 0) cloth = shade(background, 1.18F);

        boolean mark = switch (this) {
            case GOAT -> goat(x, y);
            case EMBER -> flame(x, y);
            case FROST -> snowflake(x, y);
            case AURORA -> aurora(x, y);
            case VOID -> star(x, y, 2, 3) || star(x, y, 7, 5) || star(x, y, 4, 10)
                    || star(x, y, 8, 12) || star(x, y, 1, 8) || star(x, y, 6, 2);
            case OCEAN -> ocean(x, y);
            case SUNSET -> sunset(x, y);
            case ROSE -> rose(x, y);
            case MINT -> mint(x, y);
            case CHERRY -> cherry(x, y);
            default -> false;
        };
        if (!mark) return cloth;
        return (x + y) % 5 == 0 ? shade(accent, 1.2F) : accent;
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
        int center = y < 8 ? 4 : 5;
        int spread = y < 4 ? 1 : y < 8 ? 2 : y < 12 ? 3 : 2;
        boolean outerFlame = y >= 3 && y <= 13 && Math.abs(x - center) <= spread;
        boolean innerFlame = y >= 7 && y <= 12 && Math.abs(x - 4) <= 1;
        return outerFlame && !innerFlame || innerFlame && y >= 9;
    }

    private static boolean snowflake(int x, int y) {
        boolean center = (x == 4 || x == 5) && y >= 4 && y <= 11;
        boolean cross = (y == 7 || y == 8) && x >= 2 && x <= 7;
        boolean branches = (x == 2 && (y == 5 || y == 10)) || (x == 7 && (y == 5 || y == 10))
                || (x == 3 && (y == 4 || y == 11)) || (x == 6 && (y == 4 || y == 11));
        return center || cross || branches;
    }

    private static boolean aurora(int x, int y) {
        int ribbonOne = y < 8 ? 2 + y / 3 : 4 + (y - 8) / 3;
        int ribbonTwo = y < 8 ? 7 - y / 4 : 5 - (y - 8) / 3;
        return (x == ribbonOne || x == ribbonOne + 1 || x == ribbonTwo)
                && y >= 2 && y <= 13;
    }

    private static boolean star(int x, int y, int sx, int sy) {
        return x == sx && y == sy || x == sx && Math.abs(y - sy) == 1
                || y == sy && Math.abs(x - sx) == 1;
    }

    private static boolean ocean(int x, int y) {
        int wave = 8 + (int) Math.round(1.5 * Math.sin(x * 0.9));
        int second = 12 + (int) Math.round(1.2 * Math.sin(x * 0.85 + 1));
        return Math.abs(y - wave) <= 1 || Math.abs(y - second) <= 1;
    }

    private static boolean sunset(int x, int y) {
        int dx = x - 4;
        int dy = y - 6;
        boolean sun = dx * dx + dy * dy <= 5 && y >= 3;
        boolean horizon = y == 10 && x >= 1 && x <= 8;
        boolean reflection = y == 12 && x >= 3 && x <= 6;
        return sun || horizon || reflection;
    }

    private static boolean rose(int x, int y) {
        boolean petals = (x >= 3 && x <= 6 && y >= 3 && y <= 6)
                || (x >= 2 && x <= 7 && y >= 4 && y <= 5);
        boolean stem = x == 4 && y >= 7 && y <= 12;
        boolean leaves = (x == 3 && y == 9) || (x == 5 && y == 11) || (x == 2 && y == 8);
        return petals || stem || leaves;
    }

    private static boolean mint(int x, int y) {
        if (y < 3 || y > 13) return false;
        int center = 4 + (y - 3) / 2;
        return x == center || x == 9 - (y - 3) / 2 || x == 5 && y >= 5 && y <= 12;
    }

    private static boolean cherry(int x, int y) {
        return (y == 4 && (x == 3 || x == 4 || x == 6 || x == 7))
                || (y >= 5 && y <= 7 && x >= 3 && x <= 7)
                || (y == 8 && x >= 4 && x <= 6)
                || (y == 9 && x == 5);
    }

    private static int shade(int color, float amount) {
        int r = Math.min(255, (int) (((color >>> 16) & 0xFF) * amount));
        int g = Math.min(255, (int) (((color >>> 8) & 0xFF) * amount));
        int b = Math.min(255, (int) ((color & 0xFF) * amount));
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
