package com.gregtech.gregtech.mixin;

import net.minecraft.network.Varint21FrameDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Matches {@link PacketFrameEncoderMixin}. A 4-byte length is how the dedicated registry packet fits. */
@Mixin(Varint21FrameDecoder.class)
public abstract class PacketFrameDecoderMixin {
    @ModifyConstant(method = "decode", constant = @Constant(intValue = 3))
    private int widerLength(int maxBytes) {
        return 5;
    }
}
