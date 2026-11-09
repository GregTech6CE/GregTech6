package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Wave 50 N15/N16: Laser and Quantum energy blocks. LU = Laser Energy, QU = Quantum Energy. */
public final class GTLasers {
    private static final List<RegisteredBlock<? extends Block>> ALL = new ArrayList<>();

    // N15: Lasers (CO2 ×5, Flux ×5, Absorber ×5, Fiber ×1)
    public static RegisteredBlock<Block> CO2_LASER_LV;
    public static RegisteredBlock<Block> CO2_LASER_MV;
    public static RegisteredBlock<Block> CO2_LASER_HV;
    public static RegisteredBlock<Block> CO2_LASER_EV;
    public static RegisteredBlock<Block> CO2_LASER_IV;
    public static RegisteredBlock<Block> FLUX_LASER_LV;
    public static RegisteredBlock<Block> FLUX_LASER_MV;
    public static RegisteredBlock<Block> FLUX_LASER_HV;
    public static RegisteredBlock<Block> FLUX_LASER_EV;
    public static RegisteredBlock<Block> FLUX_LASER_IV;
    public static RegisteredBlock<Block> LASER_ABSORBER_LV;
    public static RegisteredBlock<Block> LASER_ABSORBER_MV;
    public static RegisteredBlock<Block> LASER_ABSORBER_HV;
    public static RegisteredBlock<Block> LASER_ABSORBER_EV;
    public static RegisteredBlock<Block> LASER_ABSORBER_IV;
    public static RegisteredBlock<Block> LASER_FIBER_WIRE;

    // N16: Quantum Energizers ×5, ZPM ×3
    public static RegisteredBlock<Block> QUANTUM_ENERGIZER_EV;
    public static RegisteredBlock<Block> QUANTUM_ENERGIZER_IV;
    public static RegisteredBlock<Block> QUANTUM_ENERGIZER_LUV;
    public static RegisteredBlock<Block> QUANTUM_ENERGIZER_ZPM;
    public static RegisteredBlock<Block> QUANTUM_ENERGIZER_UV;
//? if >=1.21.1 {
//?} else {
    public static RegisteredBlock<Block> ZPM;
    public static RegisteredBlock<Block> ZPM_DISCHARGER_BASIC;
    public static RegisteredBlock<Block> ZPM_DISCHARGER_ADVANCED;
    public static RegisteredBlock<Block> ZPM_DISCHARGER_ELITE;
//?}

    private GTLasers() {}

    public static List<RegisteredBlock<? extends Block>> all() { return Collections.unmodifiableList(ALL); }

    private static <T extends Block> RegisteredBlock<T> reg(
            ContentRegistrar registrar, String id, Supplier<T> blockSupplier) {
        RegisteredBlock<T> ro = registrar.block(
                REGISTRY_NAMESPACE,
                id,
                blockSupplier,
                bare -> new BlockItem(bare, new Item.Properties()));
        ALL.add(ro);
        return ro;
    }

    public static void registerAll(ContentRegistrar registrar) {
        // N15: CO2 Lasers (EU → LU)
        CO2_LASER_LV = reg(registrar, "co2_laser_lv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 1)));
        CO2_LASER_MV = reg(registrar, "co2_laser_mv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 2)));
        CO2_LASER_HV = reg(registrar, "co2_laser_hv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 3)));
        CO2_LASER_EV = reg(registrar, "co2_laser_ev", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 4)));
        CO2_LASER_IV = reg(registrar, "co2_laser_iv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ELECTRIC, 5)));

        // N15: Flux Lasers (RF/FE → LU)
        FLUX_LASER_LV = reg(registrar, "flux_laser_lv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.FLUX, 1)));
        FLUX_LASER_MV = reg(registrar, "flux_laser_mv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.FLUX, 2)));
        FLUX_LASER_HV = reg(registrar, "flux_laser_hv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.FLUX, 3)));
        FLUX_LASER_EV = reg(registrar, "flux_laser_ev", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.FLUX, 4)));
        FLUX_LASER_IV = reg(registrar, "flux_laser_iv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.FLUX, 5)));

        // N15: Laser Absorbers (LU → EU)
        LASER_ABSORBER_LV = reg(registrar, "laser_absorber_lv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ABSORBER, 1)));
        LASER_ABSORBER_MV = reg(registrar, "laser_absorber_mv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ABSORBER, 2)));
        LASER_ABSORBER_HV = reg(registrar, "laser_absorber_hv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ABSORBER, 3)));
        LASER_ABSORBER_EV = reg(registrar, "laser_absorber_ev", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ABSORBER, 4)));
        LASER_ABSORBER_IV = reg(registrar, "laser_absorber_iv", () -> new gregtech.content.block.energy.LaserConverterBlock(laserProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.ABSORBER, 5)));

        // N15: Laser Fiber Wire
        LASER_FIBER_WIRE = reg(registrar, "laser_fiber_wire", () -> new gregtech.content.block.energy.LaserFiberBlock(laserProps()));

        // N16: Quantum Energizers (QU energy)
        QUANTUM_ENERGIZER_EV = reg(registrar, "quantum_energizer_ev", () -> new gregtech.content.block.energy.LaserConverterBlock(qProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.QUANTUM, 1)));
        QUANTUM_ENERGIZER_IV = reg(registrar, "quantum_energizer_iv", () -> new gregtech.content.block.energy.LaserConverterBlock(qProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.QUANTUM, 2)));
        QUANTUM_ENERGIZER_LUV = reg(registrar, "quantum_energizer_luv", () -> new gregtech.content.block.energy.LaserConverterBlock(qProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.QUANTUM, 3)));
        QUANTUM_ENERGIZER_ZPM = reg(registrar, "quantum_energizer_zpm", () -> new gregtech.content.block.energy.LaserConverterBlock(qProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.QUANTUM, 4)));
        QUANTUM_ENERGIZER_UV = reg(registrar, "quantum_energizer_uv", () -> new gregtech.content.block.energy.LaserConverterBlock(qProps(), new gregtech.content.energy.LaserSpec(gregtech.content.energy.LaserSpec.Kind.QUANTUM, 5)));

//? if >=1.21.1 {
//?} else {
        // N16: ZPM Dischargers
        ZPM = reg(registrar, "zpm", () -> new gregtech.content.block.energy.ZpmModuleBlock(BlockBehaviour.Properties.of().strength(0.5f,1f)));
        ZPM_DISCHARGER_BASIC = reg(registrar, "zpm_discharger_basic", () -> new gregtech.content.block.energy.ZpmDischargerBlock(BlockBehaviour.Properties.of().strength(4f,50f).requiresCorrectToolForDrops(),false));
        ZPM_DISCHARGER_ADVANCED = reg(registrar, "zpm_discharger_advanced", () -> new gregtech.content.block.energy.ZpmDischargerBlock(BlockBehaviour.Properties.of().strength(4f,50f).requiresCorrectToolForDrops(),true));
        ZPM_DISCHARGER_ELITE = ZPM_DISCHARGER_BASIC;
//?}
    }

    private static BlockBehaviour.Properties laserProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6f, 6f).requiresCorrectToolForDrops();
    }
    private static BlockBehaviour.Properties qProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(16f, 16f).requiresCorrectToolForDrops();
    }
}
