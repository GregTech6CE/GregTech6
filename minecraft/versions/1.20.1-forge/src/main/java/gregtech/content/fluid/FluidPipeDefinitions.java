package gregtech.content.fluid;

import gregtech.content.registry.GTFluidPipes;
import gregtech.content.transport.fluid.FluidTransportDefinitions;

/** Platform registration over the shared original pipe catalog. */
public final class FluidPipeDefinitions {
    private FluidPipeDefinitions() {}

    public static void register() {
        gregtech.engine.addon.AddonMachines.collectContributors();
        for (var spec : FluidTransportDefinitions.pipes()) {
            String owner = gregtech.engine.addon.AddonPipes.owner(spec.id());
            if (owner == null) {
                var block = GTFluidPipes.register(spec);
                if (spec.id().equals("pipe_medium_steel")) GTFluidPipes.PIPE_MEDIUM_STEEL = block;
            } else {
                gregtech.content.registry.AddonPipeRegistration.queueFluid(owner, spec);
            }
        }
    }
}
