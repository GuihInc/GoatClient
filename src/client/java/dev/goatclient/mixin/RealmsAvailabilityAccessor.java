package dev.goatclient.mixin;

import com.mojang.realmsclient.RealmsAvailability;
import java.util.concurrent.CompletableFuture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RealmsAvailability.class)
public interface RealmsAvailabilityAccessor {
    @Accessor("future")
    static void goatclient$resetAvailability(CompletableFuture<RealmsAvailability.Result> future) {
        throw new AssertionError("Mixin accessor was not applied");
    }
}
