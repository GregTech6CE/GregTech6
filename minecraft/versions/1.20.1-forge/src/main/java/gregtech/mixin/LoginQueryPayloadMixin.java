package gregtech.mixin;

import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Forge sends the item registry as one login query. That blob is larger than the vanilla 1MB cap. */
@Mixin(ClientboundCustomQueryPacket.class)
public abstract class LoginQueryPayloadMixin {
    @ModifyConstant(
            method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V",
            constant = @Constant(intValue = 1048576))
    private int fitRegistrySync(int limit) {
        return 8_388_608;
    }
}
