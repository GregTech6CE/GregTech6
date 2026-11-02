package gregtech.content.multiblock;

import gregtech.api.multiblock.PortChannel;
import gregtech.api.multiblock.PortRule;
import gregtech.api.multiblock.StructurePorts;
import gregtech.api.multiblock.StructureRole;

/** Physical tank indices exposed by native controller families, independent of their active outlets. */
public final class OriginalMultiblockPortRules {
    private OriginalMultiblockPortRules() {}

    public static final StructurePorts BOILER_WATER =
            StructurePorts.builder().fluidInput(PortRule.ALL.slots(0)).build();
    public static final StructurePorts BOILER_STEAM =
            StructurePorts.builder().fluidOutput(PortRule.ALL.slots(0)).build();
    public static final StructurePorts HOT_FLUID =
            StructurePorts.builder().fluidInput(PortRule.ALL.slots(0)).build();

    public static StructurePorts turbine(StructureRole role, boolean gas) {
        var ports = StructurePorts.builder();
        if (role.allows(PortChannel.FLUID_INPUT)) ports.fluidInput(PortRule.ALL.slots(0));
        if (role.allows(PortChannel.FLUID_OUTPUT))
            ports.fluidOutput(gas ? PortRule.ALL.slots(1, 2, 3) : PortRule.ALL.slots(1));
        return ports.build();
    }
}
