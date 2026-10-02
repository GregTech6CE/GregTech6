package com.gregtech.gregtech.content.fluid;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions;
import com.gregtech.gregtech.registry.GTFluidPipes;
/** Platform registration over the shared original pipe catalog. */
public final class FluidPipeDefinitions {
    private FluidPipeDefinitions() {}
    public static void register() {
        for (var s:FluidTransportDefinitions.pipes()) {
            var block=GTFluidPipes.register(s);
            if(s.id().equals("pipe_medium_steel"))GTFluidPipes.PIPE_MEDIUM_STEEL=block;
        }
    }
}
