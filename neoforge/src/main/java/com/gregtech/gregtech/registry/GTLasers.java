package com.gregtech.gregtech.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Wave 50 N15/N16: Laser and Quantum energy blocks. LU = Laser Energy, QU = Quantum Energy. */
public final class GTLasers {
    private static final List<DeferredHolder<Block,? extends Block>> ALL = new ArrayList<>();

    // N15: Lasers (CO2 ×5, Flux ×5, Absorber ×5, Fiber ×1)
    public static DeferredHolder<Block,Block> CO2_LASER_LV;
    public static DeferredHolder<Block,Block> CO2_LASER_MV;
    public static DeferredHolder<Block,Block> CO2_LASER_HV;
    public static DeferredHolder<Block,Block> CO2_LASER_EV;
    public static DeferredHolder<Block,Block> CO2_LASER_IV;
    public static DeferredHolder<Block,Block> FLUX_LASER_LV;
    public static DeferredHolder<Block,Block> FLUX_LASER_MV;
    public static DeferredHolder<Block,Block> FLUX_LASER_HV;
    public static DeferredHolder<Block,Block> FLUX_LASER_EV;
    public static DeferredHolder<Block,Block> FLUX_LASER_IV;
    public static DeferredHolder<Block,Block> LASER_ABSORBER_LV;
    public static DeferredHolder<Block,Block> LASER_ABSORBER_MV;
    public static DeferredHolder<Block,Block> LASER_ABSORBER_HV;
    public static DeferredHolder<Block,Block> LASER_ABSORBER_EV;
    public static DeferredHolder<Block,Block> LASER_ABSORBER_IV;
    public static DeferredHolder<Block,Block> LASER_FIBER_WIRE;

    // N16: Quantum Energizers ×5, ZPM ×3
    public static DeferredHolder<Block,Block> QUANTUM_ENERGIZER_EV;
    public static DeferredHolder<Block,Block> QUANTUM_ENERGIZER_IV;
    public static DeferredHolder<Block,Block> QUANTUM_ENERGIZER_LUV;
    public static DeferredHolder<Block,Block> QUANTUM_ENERGIZER_ZPM;
    public static DeferredHolder<Block,Block> QUANTUM_ENERGIZER_UV;

    // F8 batch 4: Logistics Core

    private GTLasers() {}

    public static List<DeferredHolder<Block,? extends Block>> all() { return Collections.unmodifiableList(ALL); }

    private static <T extends Block> DeferredHolder<Block,T> reg(String id, Supplier<T> blockSupplier) {
        DeferredHolder<Block,T> ro = GTBlocks.BLOCKS.register(id, blockSupplier);
        ALL.add(ro);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(ro.get(), new Item.Properties()));
        return ro;
    }

    public static void registerAll() {
        // N15: CO2 Lasers (EU → LU)
        CO2_LASER_LV = reg("co2_laser_lv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 1)));
        CO2_LASER_MV = reg("co2_laser_mv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 2)));
        CO2_LASER_HV = reg("co2_laser_hv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 3)));
        CO2_LASER_EV = reg("co2_laser_ev", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 4)));
        CO2_LASER_IV = reg("co2_laser_iv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 5)));

        // N15: Flux Lasers (RF/FE → LU)
        FLUX_LASER_LV = reg("flux_laser_lv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.FLUX, 1)));
        FLUX_LASER_MV = reg("flux_laser_mv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.FLUX, 2)));
        FLUX_LASER_HV = reg("flux_laser_hv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.FLUX, 3)));
        FLUX_LASER_EV = reg("flux_laser_ev", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.FLUX, 4)));
        FLUX_LASER_IV = reg("flux_laser_iv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.FLUX, 5)));

        // N15: Laser Absorbers (LU → EU)
        LASER_ABSORBER_LV = reg("laser_absorber_lv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.ABSORBER, 1)));
        LASER_ABSORBER_MV = reg("laser_absorber_mv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.ABSORBER, 2)));
        LASER_ABSORBER_HV = reg("laser_absorber_hv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.ABSORBER, 3)));
        LASER_ABSORBER_EV = reg("laser_absorber_ev", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.ABSORBER, 4)));
        LASER_ABSORBER_IV = reg("laser_absorber_iv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(laserProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.ABSORBER, 5)));

        // N15: Laser Fiber Wire
        LASER_FIBER_WIRE = reg("laser_fiber_wire", () -> new com.gregtech.gregtech.block.energy.LaserFiberBlock(laserProps()));

        // N16: Quantum Energizers (QU energy)
        QUANTUM_ENERGIZER_EV = reg("quantum_energizer_ev", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(qProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.QUANTUM, 1)));
        QUANTUM_ENERGIZER_IV = reg("quantum_energizer_iv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(qProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.QUANTUM, 2)));
        QUANTUM_ENERGIZER_LUV = reg("quantum_energizer_luv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(qProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.QUANTUM, 3)));
        QUANTUM_ENERGIZER_ZPM = reg("quantum_energizer_zpm", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(qProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.QUANTUM, 4)));
        QUANTUM_ENERGIZER_UV = reg("quantum_energizer_uv", () -> new com.gregtech.gregtech.block.energy.LaserConverterBlock(qProps(), new com.gregtech.gregtech.content.energy.LaserSpec(com.gregtech.gregtech.content.energy.LaserSpec.Kind.QUANTUM, 5)));

    }

    private static BlockBehaviour.Properties laserProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6f, 6f).requiresCorrectToolForDrops();
    }
    private static BlockBehaviour.Properties qProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(16f, 16f).requiresCorrectToolForDrops();
    }
}
