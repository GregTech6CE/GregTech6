package com.gregtech.gregtech.worldgen.dungeon;
import com.gregtech.gregtech.api.material.GTMaterial;import com.gregtech.gregtech.registry.*;import net.minecraft.core.registries.BuiltInRegistries;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.level.block.Block;
/** Resolves the existing registered platform blocks. Does not register a second content family. */
public final class DungeonBindings {private DungeonBindings(){}
 public static Block block(String id){var key=ResourceLocation.fromNamespaceAndPath("gregtech",id);if(!BuiltInRegistries.BLOCK.containsKey(key))throw new IllegalStateException("Dungeon dependency missing: "+key);return BuiltInRegistries.BLOCK.get(key);}
 public static java.util.function.Supplier<Block> supplier(String id){return ()->block(id);}
 public static com.gregtech.gregtech.block.misc.AdvancedCraftingTableBlock craftingTable(GTMaterial material){for(var h:GTCraftingTables.ADVANCED)if(h.get().material()==material)return h.get();throw new IllegalArgumentException("Dungeon table "+material.getName());}
 public static com.gregtech.gregtech.block.inventory.DrawerQuadBlock drawer(GTMaterial material){for(var h:GTStorageContainers.DRAWERS)if(h.get().material()==material)return h.get();throw new IllegalArgumentException("Dungeon drawer "+material.getName());}
 public static com.gregtech.gregtech.block.tool.ScaffoldBlock scaffold(GTMaterial material){for(var h:GTManualStations.SCAFFOLDS)if(h.get().material()==material)return h.get();throw new IllegalArgumentException("Dungeon scaffold "+material.getName());}
}
