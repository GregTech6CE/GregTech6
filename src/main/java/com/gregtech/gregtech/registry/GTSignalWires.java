package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.energy.SignalWireBlock;
import com.gregtech.gregtech.content.material.Materials;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

/** GT6 source IDs 27000/27006, 27050/27056, 27500/27506. These carry redstone, never EU. */
public final class GTSignalWires {
    private static final List<RegistryObject<SignalWireBlock>> ALL = new ArrayList<>();
    private GTSignalWires() {}
    public static List<RegistryObject<SignalWireBlock>> all() { return Collections.unmodifiableList(ALL); }
    public static void register() {
        for(var def:com.gregtech.gregtech.content.energy.SignalWireCatalog.families())family(def.name(),def.material(),def.range(),def.luminous());
    }
    private static void family(String name, GTMaterial material, int range, boolean luminous) {
        for (boolean insulated : new boolean[]{false, true}) {
            String id = (insulated ? "cable_01_" : "wire_01_") + name;
            var block = GTBlocks.BLOCKS.register(id, () -> new SignalWireBlock(material, range, insulated,
                    luminous && !insulated, BlockBehaviour.Properties.of().strength(1, 2).noOcclusion()));
            ALL.add(block);
            GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        }
    }
}
