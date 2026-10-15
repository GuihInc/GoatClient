package dev.goatclient.auth;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.ProfileResult;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;

/** All credential-dependent state required for an account switch. Never persisted. */
public record ClientSession(User user, CompletableFuture<ProfileResult> profile,
                            UserApiService api, CompletableFuture<UserApiService.UserProperties> properties,
                            ProfileKeyPairManager keys, boolean offline) {
    @Override public String toString() { return "ClientSession[credentials=REDACTED]"; }
}
