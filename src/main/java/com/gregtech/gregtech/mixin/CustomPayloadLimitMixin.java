package com.gregtech.gregtech.mixin;

import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Same 1MB cap on play-phase custom payloads. The dedicated recipe channel can exceed it. */
@Mixin(ClientboundCustomPayloadPacket.class)
public abstract class CustomPayloadLimitMixin {
    @ModifyConstant(method = {
            "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)V",
            "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V"
    }, constant = @Constant(intValue = 1048576))
    private int fitLargePayload(int limit) {
        return 8_388_608;
    }
}
