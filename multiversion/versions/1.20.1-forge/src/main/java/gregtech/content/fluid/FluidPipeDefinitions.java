package gregtech.content.fluid;
import gregtech.content.transport.fluid.FluidTransportDefinitions;
import gregtech.registry.GTFluidPipes;
/** Platform registration over the shared original pipe catalog. */
public final class FluidPipeDefinitions {
    private FluidPipeDefinitions() {}
    public static void register() {
        gregtech.api.addon.AddonMachines.collectContributors();
        for (var spec : FluidTransportDefinitions.pipes()) {
            String owner = gregtech.api.addon.AddonPipes.owner(spec.id());
            if (owner == null) {
                var block = GTFluidPipes.register(spec);
                if (spec.id().equals("pipe_medium_steel")) GTFluidPipes.PIPE_MEDIUM_STEEL = block;
            } else {
                gregtech.registry.AddonPipeRegistration.queueFluid(owner, spec);
            }
        }
    }
}
