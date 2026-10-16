package dev.goatclient.cosmetic;

import java.util.Arrays;

/** Small HUD companion sprites. All pets are rendered locally and never sent to a server. */
public enum PetStyle {
    GOAT("goat", "Cabrito", 0xFFFFF1DF, 0xFFD9A15D, new String[]{
            "  a        a   ", "  aa      aa   ", "   bbbbbbbb    ", "  bbbbbbbbbbb  ",
            " bbbbbbbbbbbbb ", " bbbbbdbbbdbbb ", " bbbbbbbbbbbbb ", "  bbbbbbdbbbb  ",
            "   bbbbbbbb    ", "   bb    bb     "}),
    FOX("fox", "Raposa", 0xFFFFA34D, 0xFFFFE7C2, new String[]{
            "  a          a ", " aaa        aaa", "  aaaaaaaaaaaa ", "  aaaaaaaaaaaa ",
            " aaaaadaaaaadaa", " aaaaaaabbaaaaa", "  aaaaaaaaabbb ", "   aaaaaaaaaa  ",
            "    aaaaaaaa   ", "    aa    aa    "}),
    BEE("bee", "Abelha", 0xFFFFD84D, 0xFFF5F8FF, new String[]{
            "    b      b   ", "   bbb    bbb  ", "    aaaaaaaaa  ", "   aaaaaaaaaaa ",
            "   aaaaaddaaaa ", "   aaaaaaaaaaa ", "   ddddddddddd ", "   aaaaaaaaaaa ",
            "    aaaaaaaaa  ", "      aa  aa    "}),
    SLIME("slime", "Slime", 0xFF74E89A, 0xFFB8FFD0, new String[]{
            "                ", "    aaaaaaaa    ", "   aaaaaaaaaa   ", "  aaaaaaaaaaaa  ",
            "  aaaaaaaaaaaa  ", "  aaddaaaaaddaa  ", "  aaddaaaaaddaa  ", "  aaaaaaaaaaaa  ",
            "  aaaaaaaaaaaa  ", "  aaaaaaaaaaaa  "}),
    GHOST("ghost", "Fantasminha", 0xFFE9F5FF, 0xFFB79CFF, new String[]{
            "     aaaaaa     ", "   aaaaaaaaaa   ", "  aaaaaaaaaaaa  ", "  aaaaaaaaaaaa  ",
            " aaaaad aaddaaa ", " aaaaad aaddaaa ", " aaaaaaaaaaaaaa ", " aaaaaaaaaaaaaa ",
            " aaaaaaaaaaaaaa ", "  aaa  aaa  aaa  "});

    private final String id;
    private final String title;
    private final int body;
    private final int accent;
    private final String[] pixels;

    PetStyle(String id, String title, int body, int accent, String[] pixels) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.accent = accent;
        this.pixels = pixels;
    }

    public String id() { return id; }
    public String title() { return title; }
    public int body() { return body; }
    public int accent() { return accent; }
    public int width() { return Arrays.stream(pixels).mapToInt(String::length).max().orElse(0); }
    public int height() { return pixels.length; }

    public int pixel(int x, int y) {
        if (y < 0 || y >= pixels.length || x < 0 || x >= pixels[y].length()) return 0;
        return switch (pixels[y].charAt(x)) {
            case 'a' -> body;
            case 'b' -> accent;
            case 'd' -> 0xFF26303A;
            default -> 0;
        };
    }

    public static PetStyle byId(String id) {
        return Arrays.stream(values()).filter(pet -> pet.id.equals(id)).findFirst().orElse(GOAT);
    }
}
