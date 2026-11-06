package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.block.machine.GTMachineBlockItem;
import gregtech.engine.machine.PipeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.Item;

/** GT6 fluid pipe blocks — holder populated by {@code Loader_MultiTileEntities}. */
public final class GTFluidPipes {
    private static final List<RegisteredBlock<FluidPipeBlock>> ALL = new ArrayList<>();

    private GTFluidPipes() {}

    public static RegisteredBlock<FluidPipeBlock> PIPE_MEDIUM_STEEL;

    public static RegisteredBlock<FluidPipeBlock> register(
            ContentRegistrar registrar,
            String id,
            GTMaterial material,
            PipeSpec.PipeSize size,
            long baseCapacity,
            boolean gasProof,
            boolean acidProof,
            boolean plasmaProof) {
        PipeSpec spec = PipeSpec.of(id, material, size, baseCapacity, gasProof, acidProof, plasmaProof, false);
        return register(registrar, spec);
    }

    public static RegisteredBlock<FluidPipeBlock> register(ContentRegistrar registrar, PipeSpec spec) {
        String id = spec.id();
        GTMaterial material = spec.material();
        PipeSpec.PipeSize size = spec.size();
        RegisteredBlock<FluidPipeBlock> block = registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new FluidPipeBlock(spec, FluidPipeBlock.defaultProperties(spec)),
                bare -> new GTMachineBlockItem(
                        bare,
                        new Item.Properties().stacksTo(stackSize(size)),
                        new gregtech.engine.machine.MachineSpec(
                                id,
                                material.getLocalName(),
                                material.getColor(),
                                0,
                                0,
                                gregtech.engine.machine.MachineTextures.BURNING_SOLID,
                                4.0F,
                                3.0F)));
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
