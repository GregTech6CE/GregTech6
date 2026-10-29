package gregtech.mixin;

import io.netty.handler.codec.CorruptedFrameException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Varint21FrameDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Matches {@link PacketFrameEncoderMixin}. A 4-byte length is how the dedicated registry packet fits.
 * Client-only: the server never receives a frame that large, and decoding runs before authentication.
 */
@Mixin(Varint21FrameDecoder.class)
public abstract class PacketFrameDecoderMixin {
    // 8 MiB is CompressionDecoder's uncompressed cap; the rest covers zlib and inner-varint overhead.
    private static final int MAX_FRAME_LENGTH = 8_388_608 + 8_192;

    @ModifyConstant(method = "decode", constant = @Constant(intValue = 3))
    private int widerLength(int maxBytes) {
        return 5;
    }

    @Redirect(
            method = "decode",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/network/FriendlyByteBuf;readVarInt()I"))
    private static int boundedLength(FriendlyByteBuf lengthPrefix) {
        int length = lengthPrefix.readVarInt();
        if (length < 0 || length > MAX_FRAME_LENGTH) {
            throw new CorruptedFrameException("frame length " + length + " exceeds " + MAX_FRAME_LENGTH);
        }
        return length;
    }
}
