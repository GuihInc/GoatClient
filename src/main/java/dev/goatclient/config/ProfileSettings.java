package dev.goatclient.config;

import dev.goatclient.cosmetic.CapeStyle;
import dev.goatclient.cosmetic.PetStyle;

public final class ProfileSettings {
    public String cape = CapeStyle.GOAT.id();
    public boolean cosmeticsEnabled = true;
    public boolean showFps = true;
    public boolean showCoordinates = false;
    public boolean showKeystrokes = false;
    public boolean showDirection = false;
    public boolean showClock = false;
    public boolean showSprintStatus = false;
    public boolean wingsEnabled = false;
    public boolean petEnabled = false;
    public String pet = PetStyle.GOAT.id();
    public String displayNickname = "";
    public boolean showGoatSymbol = true;

    public CapeStyle capeStyle() {
        return CapeStyle.byId(cape);
    }

    public PetStyle petStyle() {
        return PetStyle.byId(pet);
    }
}
