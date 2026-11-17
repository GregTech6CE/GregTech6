package gregtech.engine.fluid;

import static gregtech.engine.fluid.FluidHazard.ACID;
import static gregtech.engine.fluid.FluidHazard.GAS;
import static gregtech.engine.fluid.FluidHazard.MAGIC;
import static gregtech.engine.fluid.FluidHazard.PLASMA;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTFluids;
import java.util.Set;
import net.minecraft.world.level.material.Fluid;

/**
 * Registry-backed fluid hazard queries shared by vessel and pipe callers.
 * Acid comes from the resolved material; plasma and declared gas state come from fluid metadata.
 * Magic also has a material fallback in {@link #isMagic}; individual callers may use narrower rules.
 *
 * <p>Original predicates are FL.acid/gas/plasma/magic and the FluidsGT name sets.
 * Unknown modern fluids use the native lighter-than-air approximation for gas; it does not
 * reproduce the old independent isGaseous flag. These queries depend on the live registry.
 */
public final class FluidHazards {
    public static final int PIPE_MAGIC_DESTROY_CHANCE = 100;

    /** GT6 FL.magic: explicit fluid flags plus magical material-derived fluids (FL). */
    public static boolean isMagic(Fluid fluid) {
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) return false;
        var entry = GTFluids.entryForFluid(fluid);
        return entry != null
                && (entry.hasFlag(RegisteredFluids.FluidFlags.MAGIC)
                        || FluidVisualPolicy.material(entry).has(MaterialProperty.MAGICAL));
    }

    /** GT6 {@code MultiTileEntityPipeFluid}: {@code applyChemDamage(entity, 2)}. */
    public static final float PIPE_ACID_DAMAGE = 2.0F;
    /** GT6 {@code MultiTileEntityPipeFluid}: {@code if (rng(100) == 0)} destroys the pipe. */
    public static final int PIPE_ACID_DESTROY_CHANCE = 100;
    /** GT6 {@code MultiTileEntityPipeFluid} and {@code MultiTileEntityPipeFluid}:
     *  {@code applyTemperatureDamage(e, T, 2.0F, 10.0F)}. */
    public static final float PIPE_TEMPERATURE_MULTIPLIER = 2.0F;
    /** The damage cap of the same call. */
    public static final float PIPE_TEMPERATURE_CAP = 10.0F;

    private FluidHazards() {}

    /**
     * GT6 {@code FL} {@code FL.acid}: true when the fluid's material is
     * {@code TD.Properties.ACID} ({@code TD}, filled at {@code FL}).
     *
     * <p>Acids are ordinary liquids and gases as far as the state machine is concerned, so this is
     * independent of {@link #isGas}: hydrogen chloride and hydrogen fluoride are both
     * ({@code MT}), which is exactly why GT6 checks the two separately.
     */
    public static boolean isAcid(Fluid fluid) {
        RegisteredFluids.FluidEntry entry = GTFluids.entryForFluid(fluid);
        return entry != null && isAcid(entry);
    }

    /**
     * Declared GT gas state takes precedence. Unknown nonempty fluids fall back to the native
     * lighter-than-air flag, which is an approximation of the original independent gas predicate.
     * Empty fluid is excluded because its native type can report lighter than air.
     */
    public static boolean isGas(Fluid fluid) {
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) return false;
        RegisteredFluids.FluidEntry entry = GTFluids.entryForFluid(fluid);
        return entry != null ? entry.gas() : isNativeGas(fluid);
    }

    /**
     * GT6 {@code FL} {@code FL.lighter}: the registered density is below air's.
     *
     * <p>Strictly {@code density < 0} in {@code FluidType} units; the native
     * {@code FluidType.isLighterThanAir()} uses {@code <= 0}, which would also call the three
     * density-0 air fluids lighter than air. Air itself is the reference and must not rise.
     */
    public static boolean isLighterThanAir(Fluid fluid) {
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) return false;
        RegisteredFluids.FluidEntry entry = GTFluids.entryForFluid(fluid);
        return entry != null
                ? FluidPhysicalProperties.density(entry) < 0
                : fluid.getFluidType().getDensity() < 0;
    }

    /**
     * GT6 {@code FL} {@code FL.plasma}: the fluid was created with {@code STATE_PLASMA}
     * ({@code FL}), which the port carries as {@link RegisteredFluids.FluidFlags#PLASMA}.
     */
    public static boolean isPlasma(Fluid fluid) {
        RegisteredFluids.FluidEntry entry = GTFluids.entryForFluid(fluid);
        return entry != null && entry.hasFlag(RegisteredFluids.FluidFlags.PLASMA);
    }

    private static boolean isNativeGas(Fluid fluid) {
        return fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY
                && fluid.getFluidType().isLighterThanAir();
    }

    /** Whether any of the fluid's independent physical hazards exceeds the vessel's protection. */
    public static boolean hasUnprotectedPhysicalHazard(Fluid fluid, Set<FluidHazard> protections) {
        RegisteredFluids.FluidEntry entry = GTFluids.entryForFluid(fluid);
        if (entry == null) {
            return !protections.contains(GAS) && isNativeGas(fluid);
        }
        return !protections.contains(PLASMA) && entry.hasFlag(RegisteredFluids.FluidFlags.PLASMA)
                || !protections.contains(ACID) && isAcid(entry)
                || !protections.contains(GAS) && entry.gas();
    }

    /** GT6 checks magic first, independently of gas, plasma and acid leakage. */
    public static int magicLoss(Fluid fluid, Set<FluidHazard> protections) {
        if (protections.contains(MAGIC) || !isMagic(fluid)) return 0;
        return isGas(fluid) ? 16 : 4;
    }

    public record PipeLosses(int gas, int plasma, int acid) {}

    /** Losses can stack when a fluid belongs to multiple hazard classes. */
    public static PipeLosses pipeLosses(Fluid fluid, Set<FluidHazard> protections) {
        RegisteredFluids.FluidEntry entry = GTFluids.entryForFluid(fluid);
        boolean gas = !protections.contains(GAS) && (entry == null ? isNativeGas(fluid) : entry.gas());
        boolean plasma = !protections.contains(PLASMA) && entry != null
                && entry.hasFlag(RegisteredFluids.FluidFlags.PLASMA);
        boolean acid = !protections.contains(ACID) && entry != null && isAcid(entry);
        return new PipeLosses(gas ? 8 : 0, plasma ? 64 : 0, acid ? 16 : 0);
    }

    /**
     * The acid half of the table, against an already-resolved entry so the callers above share one
     * lookup. {@link FluidVisualPolicy#material} is the port's canonical "which material is this
     * fluid?" resolution ({@code materialKey}, then {@code boundMaterial}, then the name prefix), and
     * an unknown material resolves to {@code MaterialSentinels.INVALID} rather than {@code null}.
     */
    private static boolean isAcid(RegisteredFluids.FluidEntry entry) {
        GTMaterial material = FluidVisualPolicy.material(entry);
        return material.isValid() && material.has(MaterialProperty.ACID);
    }
}
