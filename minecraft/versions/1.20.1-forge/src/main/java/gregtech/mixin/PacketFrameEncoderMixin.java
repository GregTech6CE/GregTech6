package gregtech.mixin;

import net.minecraft.network.Varint21LengthFieldPrepender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The item registry and recipe sync both exceed a 3-byte length. FriendlyByteBuf already allows 5. */
@Mixin(Varint21LengthFieldPrepender.class)
public abstract class PacketFrameEncoderMixin {
    @ModifyConstant(
            method =
                    "encode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Lio/netty/buffer/ByteBuf;)V",
            constant = @Constant(intValue = 3))
    private int widerLength(int maxBytes) {
        return 5;
    }
}
