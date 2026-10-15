package dev.goatclient.auth;

public interface SessionAccess {
    ClientSession goatclient$captureSession();
    void goatclient$applySession(ClientSession session);
    boolean goatclient$isOffline();
}
