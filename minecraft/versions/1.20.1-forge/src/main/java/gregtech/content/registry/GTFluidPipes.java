package gregtech.content.registry;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.block.machine.GTMachineBlockItem;
import gregtech.engine.machine.PipeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/** GT6 fluid pipe blocks — holder populated by {@code Loader_MultiTileEntities}. */
public final class GTFluidPipes {
    private static final List<RegistryObject<FluidPipeBlock>> ALL = new ArrayList<>();

    private GTFluidPipes() {}

    public static RegistryObject<FluidPipeBlock> PIPE_MEDIUM_STEEL;

    public static RegistryObject<FluidPipeBlock> register(
            String id,
            GTMaterial material,
            PipeSpec.PipeSize size,
            long baseCapacity,
            boolean gasProof,
            boolean acidProof,
            boolean plasmaProof) {
        PipeSpec spec = PipeSpec.of(id, material, size, baseCapacity, gasProof, acidProof, plasmaProof, false);
        return register(spec);
    }

    public static RegistryObject<FluidPipeBlock> register(PipeSpec spec) {
        String id = spec.id();
        GTMaterial material = spec.material();
        PipeSpec.PipeSize size = spec.size();
        RegistryObject<FluidPipeBlock> block =
                GTBlocks.BLOCKS.register(id, () -> new FluidPipeBlock(spec, FluidPipeBlock.defaultProperties(spec)));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(
                id,
                () -> new GTMachineBlockItem(
                        block.get(),
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

    public static List<RegistryObject<FluidPipeBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTFluidPipes failed to initialize");
    }
}
