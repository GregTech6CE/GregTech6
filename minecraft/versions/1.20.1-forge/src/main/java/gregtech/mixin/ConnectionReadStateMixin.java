package gregtech.mixin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps protocol read-state transitions ordered on Forge's connection event loop. */
@Mixin(Connection.class)
public abstract class ConnectionReadStateMixin {
    @Shadow
    private Channel channel;

    @Redirect(
            method = "sendPacket",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lio/netty/channel/ChannelConfig;setAutoRead(Z)Lio/netty/channel/ChannelConfig;"))
    private ChannelConfig serializeReadState(ChannelConfig config, boolean autoRead) {
        // Forge queues the enable in setProtocol. An off-loop disable can enqueue removal of
        // read interest after that enable, leaving autoRead true but no socket reads registered.
        if (channel.eventLoop().inEventLoop()) {
            config.setAutoRead(autoRead);
        } else {
            channel.eventLoop().execute(() -> config.setAutoRead(autoRead));
        }
        return config;
    }
}
