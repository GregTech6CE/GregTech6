package gregtech.engine.fluid;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTFluids;
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

    /** GT6 FL.magic: explicit fluid flags plus magical material-derived fluids (FL:1119). */
    public static boolean isMagic(Fluid fluid) {
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) return false;
        var entry = GTFluids.entryForFluid(fluid);
        return entry != null && (entry.hasFlag(RegisteredFluids.FluidFlags.MAGIC)
                || FluidVisualPolicy.material(entry).has(MaterialProperty.MAGICAL));
    }

    /**
     * GT6's three hazard classes plus {@link #NONE}, with the amount of fluid a non-proof pipe loses
     * per tick for each of them.
     */
    public enum Kind {
        /** Not hazardous: water, lava, molten metal, food. GT6's {@code FluidsGT} has no such set. */
        NONE(0),
        /**
         * A fluid whose material carries {@code TD.Properties.ACID} ({@code FL.java:756},
         * {@code FL.java:1118}). Trash amount from {@code MultiTileEntityPipeFluid.java:309}.
         */
        ACID(16),
        /** A fluid created with {@code STATE_GASEOUS} ({@code FL.java:770}, {@code FL.java:1105}). */
        GAS(8),
        /** A fluid created with {@code STATE_PLASMA} ({@code FL.java:760}, {@code FL.java:1106}). */
        PLASMA(64);

        private final int pipeTrashPerTick;

        Kind(int pipeTrashPerTick) {
            this.pipeTrashPerTick = pipeTrashPerTick;
        }

        /**
         * Fluid a pipe that is not proof against this hazard loses per tick:
         * {@code GarbageGT.trash(tTank, 8)} for gas ({@code MultiTileEntityPipeFluid.java:297}),
         * {@code 64} for plasma ({@code :303}) and {@code 16} for acid ({@code :309}).
         */
        public int pipeTrashPerTick() {
            return pipeTrashPerTick;
        }
    }

    /** GT6 {@code MultiTileEntityPipeFluid.java:311}: {@code applyChemDamage(entity, 2)}. */
    public static final float PIPE_ACID_DAMAGE = 2.0F;
    /** GT6 {@code MultiTileEntityPipeFluid.java:312}: {@code if (rng(100) == 0)} destroys the pipe. */
    public static final int PIPE_ACID_DESTROY_CHANCE = 100;
    /** GT6 {@code MultiTileEntityPipeFluid.java:299/:305}: {@code applyTemperatureDamage(e, T, 2.0F, 10.0F)}. */
    public static final float PIPE_TEMPERATURE_MULTIPLIER = 2.0F;
    /** The damage cap of the same call. */
    public static final float PIPE_TEMPERATURE_CAP = 10.0F;

    private FluidHazards() {}

    /**
     * GT6 {@code FL.java:756} {@code FL.acid}: true when the fluid's material is
     * {@code TD.Properties.ACID} ({@code TD.java:386}, filled at {@code FL.java:1118}).
     *
     * <p>Acids are ordinary liquids and gases as far as the state machine is concerned, so this is
     * independent of {@link #isGas}: hydrogen chloride and hydrogen fluoride are both
     * ({@code MT.java:1020-1021}), which is exactly why GT6 checks the two separately.
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
        return entry != null ? entry.gas() : fluid.getFluidType().isLighterThanAir();
    }

    /**
     * GT6 {@code FL.java:760} {@code FL.plasma}: the fluid was created with {@code STATE_PLASMA}
     * ({@code FL.java:1106}), which the port carries as {@link RegisteredFluids.FluidFlags#PLASMA}.
     */
    public static boolean isPlasma(Fluid fluid) {
        RegisteredFluids.FluidEntry entry = GTFluids.entryForFluid(fluid);
        return entry != null && entry.hasFlag(RegisteredFluids.FluidFlags.PLASMA);
    }

    /**
     * The single most severe hazard class of {@code fluid}, for callers that want one answer instead
     * of the three independent predicates.
     *
     * <p>GT6 never picks one: it runs three consecutive tests over the same tank
     * ({@code MultiTileEntityPipeFluid.java:296} gas, {@code :302} plasma, {@code :308} acid). This
     * method exists only for callers that cannot act on more than one class, so it reports the class
     * GT6 hits hardest - {@link Kind#PLASMA} (64 units a tick) before {@link Kind#ACID} (16 a tick,
     * and the only class that can destroy the vessel) before {@link Kind#GAS} (8 a tick). A fluid that
     * is both acid and gas - hydrogen chloride again - therefore reports {@link Kind#ACID}; use
     * {@link #isGas} and friends as well when every applicable class has to be handled, which is what
     * the pipe and the barrel do.
     */
    public static Kind kindOf(Fluid fluid) {
        RegisteredFluids.FluidEntry entry = GTFluids.entryForFluid(fluid);
        if (entry == null) {
            return isGas(fluid) ? Kind.GAS : Kind.NONE;
        }
        if (entry.hasFlag(RegisteredFluids.FluidFlags.PLASMA)) return Kind.PLASMA;
        if (isAcid(entry)) return Kind.ACID;
        return entry.gas() ? Kind.GAS : Kind.NONE;
    }

    /**
     * The proof-flag truth table, straight out of GT6: does a vessel or pipe carrying these proof
     * flags refuse this hazard class?
     *
     * <p>Every GT6 vessel asks the same three questions in the same shape -
     * {@code MultiTileEntityPipeFluid.java:296-317} for the pipe,
     * {@code TileEntityBase08Barrel.java:251-254} for the barrel's fill gate and {@code :170-186} for
     * its per-tick corrosion, and {@code TileEntityBase08FluidContainer.java:420} for the general
     * container:
     * <pre>
     *   if (!mGasProof    &amp;&amp; FL.gas(aFluid))    return 0;
     *   if (!mAcidProof   &amp;&amp; FL.acid(aFluid))   return 0;
     *   if (!mPlasmaProof &amp;&amp; FL.plasma(aFluid)) return 0;
     * </pre>
     * so each flag guards exactly one class and {@link Kind#NONE} is never refused.
     */
    public static boolean proofRejects(Kind kind, boolean gasProof, boolean acidProof, boolean plasmaProof) {
        return switch (kind) {
            case NONE -> false;
            case GAS -> !gasProof;
            case ACID -> !acidProof;
            case PLASMA -> !plasmaProof;
        };
    }

    /**
     * {@link #proofRejects(FluidHazards.Kind, boolean, boolean, boolean)} for the classes a fluid
     * actually has: rejected when <em>any</em> of its hazard classes is unproofed.
     *
     * <p>This is the form the per-tick callers want, and it resolves the fluid's GT6 entry once for
     * all three questions. The {@code !xProof &&} short-circuits are on purpose: an acid-proof vessel
     * never pays for the material lookup, so a steel pipe carrying water does three comparisons and
     * no map traversal.
     */
    public static boolean proofRejects(Fluid fluid, boolean gasProof, boolean acidProof, boolean plasmaProof) {
        RegisteredFluids.FluidEntry entry = GTFluids.entryForFluid(fluid);
        if (entry == null) {
            return isGas(fluid) && !gasProof;
        }
        if (!plasmaProof && entry.hasFlag(RegisteredFluids.FluidFlags.PLASMA)) return true;
        if (!acidProof && isAcid(entry)) return true;
        return !gasProof && entry.gas();
    }

    /**
     * Fluid a non-proof pipe loses per tick, summed over the classes the fluid has, exactly as GT6
     * applies each test in turn on the same tank ({@code MultiTileEntityPipeFluid.java:296-317}): a
     * gas costs 8, plasma 64 and acid 16, so hydrogen chloride in a plain iron pipe costs 24.
     * Returns {@code 0} for {@link Kind#NONE}, and never more than the tank can hold - the caller
     * drains it.
     */
    public static int pipeTrashPerTick(Fluid fluid) {
        RegisteredFluids.FluidEntry entry = GTFluids.entryForFluid(fluid);
        if (entry == null) {
            return isGas(fluid) ? Kind.GAS.pipeTrashPerTick() : 0;
        }
        int trash = 0;
        if (entry.hasFlag(RegisteredFluids.FluidFlags.PLASMA)) trash += Kind.PLASMA.pipeTrashPerTick();
        if (isAcid(entry)) trash += Kind.ACID.pipeTrashPerTick();
        if (entry.gas()) trash += Kind.GAS.pipeTrashPerTick();
        return trash;
    }

    /**
     * The acid half of the table, against an already-resolved entry so the callers above share one
     * lookup. {@link FluidVisualPolicy#material} is the port's canonical "which material is this
     * fluid?" resolution ({@code materialKey}, then {@code boundMaterial}, then the name prefix), and
     * an unknown material resolves to {@code MaterialSentinels.Invalid} rather than {@code null}.
     */
    private static boolean isAcid(RegisteredFluids.FluidEntry entry) {
        GTMaterial material = FluidVisualPolicy.material(entry);
        return material.isValid() && material.has(MaterialProperty.ACID);
    }
}
