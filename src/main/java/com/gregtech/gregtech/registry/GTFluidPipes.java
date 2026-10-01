package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.block.machine.GTMachineBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 fluid pipe blocks — holder populated by {@code Loader_MultiTileEntities}. */
public final class GTFluidPipes {
    private static final List<RegistryObject<FluidPipeBlock>> ALL = new ArrayList<>();

    private GTFluidPipes() {}

    public static RegistryObject<FluidPipeBlock> PIPE_MEDIUM_STEEL;

    public static RegistryObject<FluidPipeBlock> register(
            String id, GTMaterial material, PipeSpec.PipeSize size, long baseCapacity,
            boolean gasProof, boolean acidProof, boolean plasmaProof) {
        PipeSpec spec = PipeSpec.of(id, material, size, baseCapacity, gasProof, acidProof, plasmaProof, false);
        RegistryObject<FluidPipeBlock> block = GTBlocks.BLOCKS.register(id,
                () -> new FluidPipeBlock(spec, FluidPipeBlock.defaultProperties(spec)));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(id,
                () -> new GTMachineBlockItem(block.get(),
                        new Item.Properties().stacksTo(stackSize(size)),
                        new com.gregtech.gregtech.api.machine.MachineSpec(
                        id, material.getLocalName(), material.getColor(), 0, 0,
                        com.gregtech.gregtech.api.machine.MachineTextures.BURNING_SOLID, 4.0F, 3.0F)));
        return block;
    }

    /**
     * GT6's stack sizes for fluid pipes ({@code MultiTileEntityPipeFluid.addFluidPipes:92-98}):
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
