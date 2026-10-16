package com.gregtech.gregtech.api.fluid;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.world.level.material.Fluid;

/**
 * GT6 physical hazard classes - acid, gas and plasma - as pure functions, plus the proof-flag
 * truth table used by existing vessels. Magic is classified separately by isMagic for pipe ticks.
 *
 * <p><b>Where GT6 keeps the answer.</b> Not on the block and not on the pipe: every GT6 hazard test
 * is {@code FL.acid}/{@code FL.gas}/{@code FL.plasma} against a fluid <em>name</em> held in a global
 * set ({@code gregapi/data/CS.java:1508 FluidsGT}):
 * <ul>
 *   <li>{@code FL.java:756} {@code acid(Fluid)} = {@code FluidsGT.ACID.contains(name)},</li>
 *   <li>{@code FL.java:760} {@code plasma(Fluid)} = {@code FluidsGT.PLASMA.contains(name)},</li>
 *   <li>{@code FL.java:770} {@code gas(Fluid)} = {@code !FluidsGT.LIQUID.contains(name) &&
 *       (fluid.isGaseous() || FluidsGT.GAS.contains(name))}.</li>
 * </ul>
 * Those sets are filled at fluid-creation time, and the two rules behind them are different in kind:
 * <ul>
 *   <li><b>gas and plasma are the fluid's physical state</b> - {@code FL.java:1105} adds every fluid
 *       created with {@code STATE_GASEOUS} to {@code FluidsGT.GAS}, {@code FL.java:1106} every
 *       {@code STATE_PLASMA} fluid to {@code FluidsGT.PLASMA}, and {@code FL.java:1104} every
 *       {@code STATE_LIQUID} fluid to {@code FluidsGT.LIQUID} (which is what makes the {@code gas()}
 *       test subtract liquids);</li>
 *   <li><b>acid is a material property, not a state</b> - {@code FL.java:1118} adds the fluid to
 *       {@code FluidsGT.ACID} when its {@code OreDictMaterial} carries {@code TD.Properties.ACID}
 *       ({@code TD.java:386}), and {@code MT.java:121-126} is what puts it there for the
 *       {@code gasacid*}/{@code lqudacid*} factories. So an acid can be a gas and a liquid at once:
 *       hydrogen chloride is {@code gasaciddcmp} ({@code MT.java:1020}) and is in both sets.</li>
 * </ul>
 * That is why {@link #isAcid}, {@link #isGas} and {@link #isPlasma} are three <em>independent</em>
 * predicates and GT6 tests them one after another ({@code MultiTileEntityPipeFluid.java:296},
 * {@code :302}, {@code :308}) instead of switching on a single category. {@link #kindOf} exists for
 * callers that want one answer and documents its precedence.
 *
 * <p><b>How the port answers the same questions.</b> The port keeps the GT6 metadata on
 * {@link RegisteredFluids.FluidEntry} instead of in name sets: {@link RegisteredFluids.FluidEntry#gas()}
 * is the {@code STATE_GASEOUS} bit and {@link RegisteredFluids.FluidFlags#PLASMA} the
 * {@code STATE_PLASMA} bit, both resolved for a live {@link Fluid} through
 * {@link GTFluids#entryForFluid} - which also resolves the {@code _flowing} variant, so a flowing gas
 * is classified like its still fluid. The acid property survives on the material, so it is read back
 * with {@link FluidVisualPolicy#material} (the same {@code materialKey} / {@code boundMaterial}
 * resolution the renderer uses) and tested against {@link MaterialProperty#ACID}, which the port
 * already fills for GT6's acid materials ({@code MaterialFormCorrections.java:45-52}, the same list
 * {@code FL.java:1118} derives from {@code TD.Properties.ACID}).
 *
 * <p>This class is deliberately stateless and cache-free: every method is a registry lookup plus a
 * comparison, so the per-tick callers below can call it on the hot path. The acid branch is the only
 * one that has to resolve a material, and {@link #proofRejects(Fluid, boolean, boolean, boolean)}
 * short-circuits it away for any vessel that is already acid proof.
 *
 * @see #proofRejects(Fluid, boolean, boolean, boolean) the accept/reject decision the vessels use
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
     * GT6 {@code FL.java:770} {@code FL.gas}: the fluid's own state is {@code STATE_GASEOUS}
     * ({@code FL.java:1105}).
     *
     * <p>For a fluid with no GT6 declaration the {@code FluidsGT.GAS} name set cannot answer, so the
     * question falls to Forge's own lighter-than-air flag, which is the 1.20.1 equivalent of the
     * {@code fluid.isGaseous()} half of GT6's test. GT6's {@code !FluidsGT.LIQUID.contains(name)} guard
     * is already encoded in the port's metadata: {@code withGas()} clears
     * {@link RegisteredFluids.FluidFlags#LIQUID}, so {@code entry.gas()} cannot be true for a declared
     * liquid.
     *
     * <p>{@code Fluids.EMPTY} is never a gas: it has no GT6 declaration, and Forge's empty fluid type
     * reports {@code isLighterThanAir()} — so the fallback has to exclude it explicitly, or "no fluid"
     * would count as a breathable gas (the gate caught exactly that).
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
