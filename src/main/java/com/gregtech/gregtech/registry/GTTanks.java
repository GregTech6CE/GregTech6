package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.api.machine.TankSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.machine.TankBlock;
import com.gregtech.gregtech.block.machine.GTMachineBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 fluid container blocks — holder populated by {@code Loader_MultiTileEntities}. */
public final class GTTanks {
    private static final List<RegistryObject<TankBlock>> ALL = new ArrayList<>();

    private GTTanks() {}

    public static RegistryObject<TankBlock> WOOD_BARREL;
    public static RegistryObject<TankBlock> DRUM_BRONZE;
    public static RegistryObject<TankBlock> DRUM_STEEL;
    /** GT6 32072, a one-million-litre direct logistics storage node. */
    public static RegistryObject<TankBlock> LOGISTICS_TANK;

    public static RegistryObject<TankBlock> register(
            String id, GTMaterial material, TankSpec.TankType type,
            long capacity, boolean gasProof, boolean acidProof,
            boolean plasmaProof, boolean magicProof,
            boolean simpleOnly, float hardness, float blastResistance) {
        TankSpec spec = TankSpec.of(id, material, type, capacity,
                gasProof, acidProof, plasmaProof, magicProof, simpleOnly, hardness, blastResistance);
        RegistryObject<TankBlock> block = GTBlocks.BLOCKS.register(id,
                () -> new TankBlock(spec, TankBlock.defaultProperties(spec)));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(id,
                () -> new GTMachineBlockItem(block.get(), new Item.Properties(), new MachineSpec(
                        id, material.getLocalName(), material.getColor(), 0, 0,
                        com.gregtech.gregtech.api.machine.MachineTextures.BURNING_SOLID, hardness, blastResistance)));
        return block;
    }

    public static RegistryObject<TankBlock> registerLogisticsTank() {
        String id = "logistics_tank";
        var material = com.gregtech.gregtech.content.material.Materials.Tungsten;
        TankSpec spec = TankSpec.of(id, material, TankSpec.TankType.LOGISTICS_BARREL,
                1_000_000, true, true, true, true, false, 1.0F, 10.0F)
                .withMaxTemperature(100_000);
        RegistryObject<TankBlock> block = GTBlocks.BLOCKS.register(id,
                () -> new com.gregtech.gregtech.block.machine.LogisticsTankBlock(
                        spec, TankBlock.defaultProperties(spec)));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(id,
                () -> new GTMachineBlockItem(block.get(), new Item.Properties().stacksTo(16), new MachineSpec(
                        id, "Logistics Tank", material.getColor(), 0, 0,
                        com.gregtech.gregtech.api.machine.MachineTextures.BURNING_SOLID,
                        1.0F, 10.0F)));
        return block;
    }

    public static List<RegistryObject<TankBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTTanks failed to initialize");
    }
}
