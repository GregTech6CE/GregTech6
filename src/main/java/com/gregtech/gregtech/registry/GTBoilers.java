package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.machine.BoilerSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.machine.BoilerTankBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6 single-block Steam Boiler Tanks ({@code MultiTileEntityBoilerTank}):
 * 13 materials × {normal, strong}. The strong (dense-plate) variants carry the
 * high steam outputs. Values follow {@code Loader_MultiTileEntities} 1200-1262
 * ({@code NBT_OUTPUT_SU = tableValue * STEAM_PER_EU}, STEAM_PER_EU = 2).
 */
public final class GTBoilers {
    private static final List<RegistryObject<BoilerTankBlock>> ALL = new ArrayList<>();

    private GTBoilers() {}

    public static List<RegistryObject<BoilerTankBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void add(BoilerSpec spec) {
        RegistryObject<BoilerTankBlock> block = GTBlocks.BLOCKS.register(spec.id(),
                () -> new BoilerTankBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(spec.hardness(), spec.hardness())
                        .requiresCorrectToolForDrops()));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(spec.id(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void registerAll(){com.gregtech.gregtech.content.energy.BoilerCatalog.all().forEach(GTBoilers::add);}
}
