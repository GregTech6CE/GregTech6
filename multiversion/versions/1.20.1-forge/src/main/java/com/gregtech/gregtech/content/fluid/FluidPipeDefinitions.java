package com.gregtech.gregtech.content.fluid;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions;
import com.gregtech.gregtech.registry.GTFluidPipes;
/** Platform registration over the shared original pipe catalog. */
public final class FluidPipeDefinitions {
    private FluidPipeDefinitions() {}
    public static void register() {
        com.gregtech.gregtech.api.addon.AddonMachines.collectContributors();
        for (var spec : FluidTransportDefinitions.pipes()) {
            String owner = com.gregtech.gregtech.api.addon.AddonPipes.owner(spec.id());
            if (owner == null) {
                var block = GTFluidPipes.register(spec);
                if (spec.id().equals("pipe_medium_steel")) GTFluidPipes.PIPE_MEDIUM_STEEL = block;
            } else {
                com.gregtech.gregtech.registry.AddonPipeRegistration.queueFluid(owner, spec);
            }
        }
    }
}
