package dev.goatclient.mixin;

import com.mojang.realmsclient.client.RealmsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RealmsClient.class)
public interface RealmsClientAccessor {
    @Accessor("realmsClientInstance")
    static void goatclient$resetClient(RealmsClient client) {
        throw new AssertionError("Mixin accessor was not applied");
    }
}
