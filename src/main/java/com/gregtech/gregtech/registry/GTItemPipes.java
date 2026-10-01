package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.machine.ItemPipeSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import com.gregtech.gregtech.block.machine.GTMachineBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 item pipe blocks — holder populated by {@code Loader_MultiTileEntities}. */
public final class GTItemPipes {
    private static final List<RegistryObject<ItemPipeBlock>> ALL = new ArrayList<>();

    private GTItemPipes() {}

    public static RegistryObject<ItemPipeBlock> PIPE_MEDIUM_BRASS;

    public static RegistryObject<ItemPipeBlock> register(
            String id, GTMaterial material, ItemPipeSpec.ItemPipeSize size, long stepSize, int invSize, boolean routing) {
        ItemPipeSpec spec = ItemPipeSpec.of(id, material, size, stepSize, invSize, routing);
        RegistryObject<ItemPipeBlock> block = GTBlocks.BLOCKS.register(id,
                () -> new ItemPipeBlock(spec, ItemPipeBlock.defaultProperties(spec)));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(id,
                () -> new GTMachineBlockItem(block.get(),
                        new Item.Properties().stacksTo(stackSize(size)),
                        new com.gregtech.gregtech.api.machine.MachineSpec(
                                id, material.getLocalName(), material.getColor(), 0, 0,
                                com.gregtech.gregtech.api.machine.MachineTextures.BURNING_SOLID,
                                1.5F, spec.blastResistance())));
        return block;
    }

    /**
     * GT6's stack sizes for item pipes ({@code MultiTileEntityPipeItem.addItemPipes:77-82}):
     * medium 64, large 32, huge 16 — the restrictive variants follow their base size.
     */
    public static int stackSize(ItemPipeSpec.ItemPipeSize size) {
        return switch (size) {
            case MEDIUM, RESTRICTIVE_MEDIUM -> 64;
            case LARGE, RESTRICTIVE_LARGE -> 32;
            case HUGE, RESTRICTIVE_HUGE -> 16;
        };
    }

    public static List<RegistryObject<ItemPipeBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTItemPipes failed to initialize");
    }
}
