package dev.goatclient.cosmetic;

import java.util.Arrays;

/** Low-poly shoulder companion palette and shape. Rendered only for the local player. */
public enum PetStyle {
    GOAT("goat", "Cabrito", 0xFFF1E7D8, 0xFFD8A66B, 0xFF6F4937),
    FOX("fox", "Raposa", 0xFFF07832, 0xFFFFE5C6, 0xFF402E2B),
    BEE("bee", "Abelha", 0xFFFFC928, 0xFF2D2930, 0xFFFFF2CC),
    SLIME("slime", "Slime", 0xFF70D98C, 0xFFB7F3BD, 0xFF265A42),
    GHOST("ghost", "Fantasminha", 0xFFE8F2FF, 0xFFB89BFF, 0xFF434458);

    private final String id;
    private final String title;
    private final int body;
    private final int accent;
    private final int dark;

    PetStyle(String id, String title, int body, int accent, int dark) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.accent = accent;
        this.dark = dark;
    }

    public String id() { return id; }
    public String title() { return title; }
    public int body() { return body; }
    public int accent() { return accent; }
    public int dark() { return dark; }

    public static PetStyle byId(String id) {
        return Arrays.stream(values()).filter(pet -> pet.id.equals(id)).findFirst().orElse(GOAT);
    }
}
