package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.block.machine.GTMachineBlockItem;
import gregtech.content.transport.fluid.FluidTransportDefinitions;
import gregtech.engine.machine.PipeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.Item;

/** GT6 fluid pipe blocks, populated from the shared transport catalog. */
public final class GTFluidPipes {
    private static final List<RegisteredBlock<FluidPipeBlock>> ALL = new ArrayList<>();

    private GTFluidPipes() {}

    public static RegisteredBlock<FluidPipeBlock> PIPE_MEDIUM_STEEL;

    public static void registerAll(ContentRegistrar registrar) {
        gregtech.engine.addon.AddonMachines.collectContributors();
        for (var spec : FluidTransportDefinitions.pipes()) {
            String owner = gregtech.engine.addon.AddonPipes.owner(spec.id());
            if (owner == null) {
                var block = register(registrar, spec);
                if (spec.id().equals("pipe_medium_steel")) PIPE_MEDIUM_STEEL = block;
            } else {
                AddonPipeRegistration.queueFluid(registrar, owner, spec);
            }
        }
    }

    public static RegisteredBlock<FluidPipeBlock> register(ContentRegistrar registrar, PipeSpec spec) {
        String id = spec.id();
        RegisteredBlock<FluidPipeBlock> block = registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new FluidPipeBlock(spec, FluidPipeBlock.defaultProperties(spec)),
                bare -> new GTMachineBlockItem(bare, new Item.Properties().stacksTo(stackSize(spec.size()))));
        ALL.add(block);
        return block;
    }

    /**
     * GT6's stack sizes for fluid pipes ({@code MultiTileEntityPipeFluid.addFluidPipes}):
     * tiny/small 64, medium 32, large/huge/quadruple/nonuple 16.
     */
    public static int stackSize(PipeSpec.PipeSize size) {
        return switch (size) {
            case TINY, SMALL -> 64;
            case MEDIUM -> 32;
            case LARGE, HUGE, QUADRUPLE, NONUPLE -> 16;
        };
    }

    public static List<RegisteredBlock<FluidPipeBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }
}
