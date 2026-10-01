package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.energy.MagnetBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 magnet blocks (N4) — used in motor/generator recipes and magnetic effects. */
public final class GTMagnets {
    private static final List<RegistryObject<MagnetBlock>> ALL = new ArrayList<>();

    private GTMagnets() {}

    public static List<RegistryObject<MagnetBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void add(String id, GTMaterial mat, String materialName) {
        RegistryObject<MagnetBlock> block = GTBlocks.BLOCKS.register(id, () ->
                new MagnetBlock(materialName, mat, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3f + mat.getDensity() / 2000f, 3f + mat.getDensity() / 2000f)
                        .requiresCorrectToolForDrops()));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void registerAll() {
        add("magnet_iron", Materials.IronMagnetic, "Iron");
        add("magnet_steel", Materials.SteelMagnetic, "Steel");
        add("magnet_neodymium", Materials.NeodymiumMagnetic, "Neodymium");
        add("magnet_cobalt", Materials.CobaltBrass, "Cobalt");
        add("magnet_alnico", Materials.Invar, "Alnico");
        add("magnet_ferrite", Materials.CastIron, "Ferrite");
        add("magnet_electromagnetic_steel", Materials.Steel, "Electromagnetic Steel");
        add("magnet_electromagnetic_aluminium", Materials.Aluminium, "Electromagnetic Aluminium");
        add("magnet_electromagnetic_galvanized", Materials.SteelGalvanized, "Electromagnetic Galvanized Steel");
        add("magnet_tungsten_steel", Materials.Tungstensteel, "Tungsten Steel");
    }
}
