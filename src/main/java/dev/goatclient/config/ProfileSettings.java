package dev.goatclient.config;

import dev.goatclient.cosmetic.CapeStyle;

public final class ProfileSettings {
    public String cape = CapeStyle.GOAT.id();
    public boolean cosmeticsEnabled = true;
    public boolean showFps = true;
    public boolean showCoordinates = false;
    public boolean showKeystrokes = false;

    public CapeStyle capeStyle() {
        return CapeStyle.byId(cape);
    }
}
