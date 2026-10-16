package dev.goatclient.auth;

/** Public application ID and nickname only; no credentials are persisted. */
public final class AuthSettings {
    public static final String DEFAULT_MICROSOFT_CLIENT_ID = "d1fa3c11-c22b-43e1-a6fa-4027c342c7e1";

    public String microsoftClientId = DEFAULT_MICROSOFT_CLIENT_ID;
    public String offlineName = "";
}
