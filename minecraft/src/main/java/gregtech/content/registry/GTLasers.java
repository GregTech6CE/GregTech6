package gregtech.content.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.registries.RegistryObject;
//?}

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Wave 50 N15/N16: Laser and Quantum energy blocks. LU = Laser Energy, QU = Quantum Energy. */
public final class GTLasers {
//? if >=1.21.1 {
    /*private static final List<DeferredHolder<Block,? extends Block>> ALL = new ArrayList<>();
*///?} else {
    private static final List<RegistryObject<? extends Block>> ALL = new ArrayList<>();
//?}

    // N15: Lasers (CO2 ×5, Flux ×5, Absorber ×5, Fiber ×1)
//? if >=1.21.1 {
    /*public static DeferredHolder<Block,Block> CO2_LASER_LV;
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
*///?} else {
    public static RegistryObject<Block> CO2_LASER_LV;
    public static RegistryObject<Block> CO2_LASER_MV;
    public static RegistryObject<Block> CO2_LASER_HV;
    public static RegistryObject<Block> CO2_LASER_EV;
    public static RegistryObject<Block> CO2_LASER_IV;
    public static RegistryObject<Block> FLUX_LASER_LV;
    public static RegistryObject<Block> FLUX_LASER_MV;
    public static RegistryObject<Block> FLUX_LASER_HV;
    public static RegistryObject<Block> FLUX_LASER_EV;
    public static RegistryObject<Block> FLUX_LASER_IV;
    public static RegistryObject<Block> LASER_ABSORBER_LV;
    public static RegistryObject<Block> LASER_ABSORBER_MV;
    public static RegistryObject<Block> LASER_ABSORBER_HV;
    public static RegistryObject<Block> LASER_ABSORBER_EV;
    public static RegistryObject<Block> LASER_ABSORBER_IV;
    public static RegistryObject<Block> LASER_FIBER_WIRE;
//?}

    // N16: Quantum Energizers ×5, ZPM ×3
//? if >=1.21.1 {
    /*public static DeferredHolder<Block,Block> QUANTUM_ENERGIZER_EV;
    public static DeferredHolder<Block,Block> QUANTUM_ENERGIZER_IV;
    public static DeferredHolder<Block,Block> QUANTUM_ENERGIZER_LUV;
    public static DeferredHolder<Block,Block> QUANTUM_ENERGIZER_ZPM;
    public static DeferredHolder<Block,Block> QUANTUM_ENERGIZER_UV;
*///?} else {
    public static RegistryObject<Block> QUANTUM_ENERGIZER_EV;
    public static RegistryObject<Block> QUANTUM_ENERGIZER_IV;
    public static RegistryObject<Block> QUANTUM_ENERGIZER_LUV;
    public static RegistryObject<Block> QUANTUM_ENERGIZER_ZPM;
    public static RegistryObject<Block> QUANTUM_ENERGIZER_UV;
    public static RegistryObject<Block> ZPM;
    public static RegistryObject<Block> ZPM_DISCHARGER_BASIC;
    public static RegistryObject<Block> ZPM_DISCHARGER_ADVANCED;
    public static RegistryObject<Block> ZPM_DISCHARGER_ELITE;
//?}

    // F8 batch 4: Logistics Core
//? if >=1.21.1 {
//?} else {
    public static RegistryObject<Block> LOGISTICS_CORE;
//?}

    private GTLasers() {}

//? if >=1.21.1 {
    /*public static List<DeferredHolder<Block,? extends Block>> all() { return Collections.unmodifiableList(ALL); }
*///?} else {
    public static List<RegistryObject<? extends Block>> all() { return Collections.unmodifiableList(ALL); }
//?}

//? if >=1.21.1 {
    /*private static <T extends Block> DeferredHolder<Block,T> reg(String id, Supplier<T> blockSupplier) {
        DeferredHolder<Block,T> ro = GTBlocks.BLOCKS.register(id, blockSupplier);
*///?} else {
    private static <T extends Block> RegistryObject<T> reg(String id, Supplier<T> blockSupplier) {
        RegistryObject<T> ro = GTBlocks.BLOCKS.register(id, blockSupplier);
//?}
        ALL.add(ro);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(ro.get(), new Item.Properties()));
        return ro;
    }

    public static void registerAll() {
        // N15: CO2 Lasers (EU → LU)
        CO2_LASER_LV = reg("co2_laser_lv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 1)));
        CO2_LASER_MV = reg("co2_laser_mv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 2)));
        CO2_LASER_HV = reg("co2_laser_hv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 3)));
        CO2_LASER_EV = reg("co2_laser_ev", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 4)));
        CO2_LASER_IV = reg("co2_laser_iv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 5)));

        // N15: Flux Lasers (RF/FE → LU)
        FLUX_LASER_LV = reg("flux_laser_lv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.FLUX, 1)));
        FLUX_LASER_MV = reg("flux_laser_mv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.FLUX, 2)));
        FLUX_LASER_HV = reg("flux_laser_hv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.FLUX, 3)));
        FLUX_LASER_EV = reg("flux_laser_ev", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.FLUX, 4)));
        FLUX_LASER_IV = reg("flux_laser_iv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.FLUX, 5)));

        // N15: Laser Absorbers (LU → EU)
        LASER_ABSORBER_LV = reg("laser_absorber_lv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ABSORBER, 1)));
        LASER_ABSORBER_MV = reg("laser_absorber_mv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ABSORBER, 2)));
        LASER_ABSORBER_HV = reg("laser_absorber_hv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ABSORBER, 3)));
        LASER_ABSORBER_EV = reg("laser_absorber_ev", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ABSORBER, 4)));
        LASER_ABSORBER_IV = reg("laser_absorber_iv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ABSORBER, 5)));

        // N15: Laser Fiber Wire
        LASER_FIBER_WIRE = reg("laser_fiber_wire", () -> new gregtech.content.block.energy.LaserFiberBlock(laserProps()));

        // N16: Quantum Energizers (QU energy)
        QUANTUM_ENERGIZER_EV = reg("quantum_energizer_ev", () -> new gregtech.content.block.energy.LaserConverterBlock(qProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.QUANTUM, 1)));
        QUANTUM_ENERGIZER_IV = reg("quantum_energizer_iv", () -> new gregtech.content.block.energy.LaserConverterBlock(qProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.QUANTUM, 2)));
        QUANTUM_ENERGIZER_LUV = reg("quantum_energizer_luv", () -> new gregtech.content.block.energy.LaserConverterBlock(qProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.QUANTUM, 3)));
        QUANTUM_ENERGIZER_ZPM = reg("quantum_energizer_zpm", () -> new gregtech.content.block.energy.LaserConverterBlock(qProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.QUANTUM, 4)));
        QUANTUM_ENERGIZER_UV = reg("quantum_energizer_uv", () -> new gregtech.content.block.energy.LaserConverterBlock(qProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.QUANTUM, 5)));

//? if >=1.21.1 {
//?} else {
        // N16: ZPM Dischargers
        ZPM = reg("zpm", () -> new gregtech.content.block.energy.ZpmModuleBlock(BlockBehaviour.Properties.of().strength(0.5f,1f)));
        ZPM_DISCHARGER_BASIC = reg("zpm_discharger_basic", () -> new gregtech.content.block.energy.ZpmDischargerBlock(BlockBehaviour.Properties.of().strength(4f,50f).requiresCorrectToolForDrops(),false));
        ZPM_DISCHARGER_ADVANCED = reg("zpm_discharger_advanced", () -> new gregtech.content.block.energy.ZpmDischargerBlock(BlockBehaviour.Properties.of().strength(4f,50f).requiresCorrectToolForDrops(),true));
        ZPM_DISCHARGER_ELITE = ZPM_DISCHARGER_BASIC;

        // F8: Logistics Core
        LOGISTICS_CORE = MultiblockRegistration.register("gt6ce", "logistics_core",
                () -> new gregtech.content.block.machine.LogisticsCoreControllerBlock(laserProps()), GTLasers::reg);
//?}
    }

    private static BlockBehaviour.Properties laserProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6f, 6f).requiresCorrectToolForDrops();
    }
    private static BlockBehaviour.Properties qProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(16f, 16f).requiresCorrectToolForDrops();
    }
}
