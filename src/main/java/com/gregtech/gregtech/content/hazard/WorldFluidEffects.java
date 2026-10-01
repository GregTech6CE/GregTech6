package com.gregtech.gregtech.content.hazard;

import com.gregtech.gregtech.api.fluid.FluidHazards;
import com.gregtech.gregtech.damage.GTHazmat;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.util.GTEntityHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * GT6's two <em>world fluid</em> hazard tables: the effects a fluid gives to a body that touches it
 * (<b>bathing</b>) and the effects it gives to a head that is inside it (<b>breathing</b>).
 *
 * <p>GT6 keeps these on the fluid <em>block</em>, not on the fluid: {@code BlockBaseFluid} has
 * {@code addEffectBathing} / {@code addEffectBreathing} ({@code gregapi/block/fluid/BlockBaseFluid.java:419,425})
 * and {@code BlockWaterlike} has a single {@code addEffect} list
 * ({@code gregtech/blocks/fluids/BlockWaterlike.java:218}) that is applied on the head-inside path.
 * Every row below is one registration line of {@code Loader_Blocks.java:137-153}; the port has a world
 * block for exactly those nine fluids ({@code Loader_Fluids.WORLD_FLUID_PATHS}), so the whole table is
 * reachable in game.
 *
 * <h2>The two entry points GT6 uses</h2>
 *
 * <pre>
 *   // BlockBaseFluid:404-409 - a body that overlaps the fluid block
 *   if (!world.isRemote &amp;&amp; !mEffectsBathing.isEmpty() &amp;&amp; entity instanceof EntityLivingBase
 *       &amp;&amp; !UT.Entities.isWearingFullChemHazmat(entity))
 *       for (int[] e : mEffectsBathing) UT.Entities.applyPotion(entity, e[0], e[1], e[2], F);
 *
 *   // BlockBaseFluid:411-415 / BlockWaterlike:226-231 - a head inside it
 *   if (!world.isRemote &amp;&amp; effects non-empty &amp;&amp; gate)
 *       for (int[] e : effects) UT.Entities.applyPotion(entity, e[0], e[1], e[2], F);
 *       if (getMaterial() != Material.water &amp;&amp; SERVER_TIME % 20 == 0)
 *           entity.attackEntityFrom(DamageSource.drown, 2.0F);
 * </pre>
 *
 * <p>Three details of that shape are reproduced literally and are easy to get wrong:
 *
 * <ol>
 *   <li><b>The effect list is the switch, not decoration.</b> A fluid that declares no row deals no
 *       drown damage either, which is why {@code seawater} and {@code riverwater} are completely
 *       harmless in GT6 ({@code Loader_Blocks.java:135,136} pass no effects) although they are water.</li>
 *   <li><b>The two head-inside paths have different gates.</b> {@code BlockBaseFluid} always asks
 *       {@code isImmuneToBreathingGases} ({@code :412}), even for the oils, which are liquids;
 *       {@code BlockWaterlike} picks per fluid - {@code FL.gas(fluid) ? !isImmuneToBreathingGases
 *       : !isWearingFullChemHazmat} ({@code :227}). The swamp is the only waterlike fluid with a row,
 *       and its material <em>is</em> water ({@code BlockWaterlike:65 super(aFluid, Material.water)}),
 *       so swamp water never drowns you - only the hunger and confusion land.</li>
 *   <li><b>Drown damage keys on the material, not on the fluid class.</b> Oils and natural gas carry
 *       {@code MaterialOil}/{@code MaterialGas} and therefore drown; the geothermal spring is
 *       {@code Material.water} ({@code Loader_Blocks.java:146}) and does not.</li>
 * </ol>
 *
 * <h2>What is not portable</h2>
 *
 * <p>Three of the oil bathing effects are <em>Immersive Engineering's</em> potions, mapped into GT6's
 * own id space ({@code GT_API.java:786-789}: {@code ID_FLAMMABLE = IEPotions.flammable.id},
 * {@code ID_SLIPPERY}, {@code ID_STICKY}; the ids themselves are the negatives
 * {@code CS.java:1577}). 1.20.1 has no Immersive Engineering, so those three are recorded as
 * not-applicable rather than invented - the vanilla {@code blindness 60/1} that sits next to them on
 * the same line is applied. The web behaviour ({@code .setWeb()} on the extra-heavy and heavy lines)
 * is ported: {@code setWeb()} is 1.7.10's {@code Entity#setInWeb}, i.e. vanilla cobweb's
 * {@code makeStuckInBlock(state, 0.25, 0.05, 0.25)}.</p>
 *
 * <p>Everything here is a pure function of a fluid (plus an entity to apply to), so a GameTest can
 * drive the whole table without a world.</p>
 */
public final class WorldFluidEffects {

    /**
     * One GT6 {@code int[] {effectId, duration, level}} row, with the registration line it came from.
     * {@code amplifier} is GT6's third element ("level"), passed through unchanged - GT6 hands it to
     * {@code applyPotion} as {@code aLevel}, which is vanilla's amplifier.
     */
    public record EffectRow(MobEffect effect, int duration, int amplifier, String source) {
        /** GT6 {@code UT.Entities.applyPotion(entity, id, duration, level, F)}: ambient is false. */
        public MobEffectInstance instance() {
            return new MobEffectInstance(effect, duration, amplifier);
        }
    }

    /** Which immunity GT6 asks on the head-inside path for a given fluid. */
    public enum HeadGate {
        /** {@code BlockBaseFluid:412}: {@code !UT.Entities.isImmuneToBreathingGases(entity)}. */
        BREATHING_SUIT,
        /** {@code BlockWaterlike:227}: gas fluids use the gas suit, everything else the chem suit. */
        WATERLIKE
    }

    // ---------------------------------------------------------------------------------------------
    // The nine fluids the port puts into the world (Loader_Fluids.WORLD_FLUID_PATHS)
    // ---------------------------------------------------------------------------------------------

    public static final String EXTRA_HEAVY_OIL = "liquid_extra_heavy_oil";
    public static final String HEAVY_OIL = "liquid_heavy_oil";
    public static final String MEDIUM_OIL = "liquid_medium_oil";
    public static final String LIGHT_OIL = "liquid_light_oil";
    public static final String NATURAL_GAS = "gas_natural_gas";
    public static final String GEOTHERMAL_WATER = "watergeothermal";
    public static final String SWAMP_WATER = "swampwater";

    /** GT6 {@code BlockBaseFluid:414} / {@code BlockWaterlike:229}: {@code SERVER_TIME % 20 == 0}. */
    public static final int DROWN_INTERVAL = 20;
    /** GT6 {@code :414}: {@code attackEntityFrom(DamageSource.drown, 2.0F)}. */
    public static final float DROWN_DAMAGE = 2.0F;

    private WorldFluidEffects() {}

    // ---------------------------------------------------------------------------------------------
    // Bathing - Loader_Blocks:146-152
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code Loader_Blocks.java:149-152}: the four oil blocks. The flammable/slippery/sticky rows are
     * Immersive Engineering potions ({@code GT_API:786-789}) and are listed in {@link #OIL_IE_EFFECTS}
     * instead of being applied.
     */
    private static final List<EffectRow> OIL_BATHING = List.of(
            new EffectRow(MobEffects.BLINDNESS, 60, 1, "Loader_Blocks.java:149-152 blindness 60/1"));

    /** Recorded, not applied: GT6 gets these three from Immersive Engineering ({@code GT_API:786-789}). */
    public static final List<String> OIL_IE_EFFECTS = List.of("flammable 300/1", "sticky 300/1", "slippery 300/1");

    /** {@code Loader_Blocks.java:146}: the geothermal spring heals and hardens whoever touches it. */
    private static final List<EffectRow> GEOTHERMAL_BATHING = List.of(
            new EffectRow(MobEffects.REGENERATION, 100, 0, "Loader_Blocks.java:146 regeneration 100/0"),
            // GT6 names Potion.resistance; 1.20.1 calls the same effect MobEffects.DAMAGE_RESISTANCE.
            new EffectRow(MobEffects.DAMAGE_RESISTANCE, 2400, 2, "Loader_Blocks.java:146 resistance 2400/2"));

    /** Which fluids have a bathing row, and their rows. */
    public static List<EffectRow> bathingRows(Fluid fluid) {
        String path = path(fluid);
        return switch (path) {
            case EXTRA_HEAVY_OIL, HEAVY_OIL, MEDIUM_OIL, LIGHT_OIL -> OIL_BATHING;
            case GEOTHERMAL_WATER -> GEOTHERMAL_BATHING;
            default -> List.of();
        };
    }

    public static boolean hasBathingEffects(Fluid fluid) {
        return !bathingRows(fluid).isEmpty();
    }

    /** {@code BlockBaseFluid:406}: {@code !UT.Entities.isWearingFullChemHazmat(entity)}. */
    public static boolean bathingGatePasses(LivingEntity entity) {
        return !GTHazmat.isChemProtected(entity);
    }

    /**
     * The bathing half of {@code BlockBaseFluid:404-409} for one fluid, without the tick cadence
     * (GT6 re-applies on every collision, which is every tick).
     *
     * @return whether any effect was applied
     */
    public static boolean applyBathing(LivingEntity entity, Fluid fluid) {
        List<EffectRow> rows = bathingRows(fluid);
        if (rows.isEmpty() || entity.level().isClientSide || entity.isDeadOrDying()) return false;
        if (!bathingGatePasses(entity)) return false;
        for (EffectRow row : rows) entity.addEffect(row.instance());
        return true;
    }

    /**
     * {@code Loader_Blocks.java:149,150} {@code .setWeb()} - and {@code BlockBaseFluid:374}, where a
     * non-empty bathing list is what makes {@code getBlocksMovement} answer true. Only the two heavy
     * oils call {@code setWeb}; the medium and light ones intentionally do not.
     */
    public static boolean actsLikeWeb(Fluid fluid) {
        String path = path(fluid);
        return EXTRA_HEAVY_OIL.equals(path) || HEAVY_OIL.equals(path);
    }

    // ---------------------------------------------------------------------------------------------
    // Head inside - Loader_Blocks:137,149-153
    // ---------------------------------------------------------------------------------------------

    /** {@code Loader_Blocks.java:149-152}: breathing oils poisons and confuses. */
    private static final List<EffectRow> OIL_BREATHING = List.of(
            new EffectRow(MobEffects.POISON, 300, 0, "Loader_Blocks.java:149-152 poison 300/0"),
            new EffectRow(MobEffects.CONFUSION, 120, 0, "Loader_Blocks.java:149-152 confusion 120/0"));

    /** {@code Loader_Blocks.java:153}: the natural-gas spring, the gas row this port already had. */
    private static final List<EffectRow> GAS_BREATHING = List.of(
            new EffectRow(MobEffects.POISON, 300, 0, "Loader_Blocks.java:153 poison 300/0"),
            new EffectRow(MobEffects.CONFUSION, 120, 0, "Loader_Blocks.java:153 confusion 120/0"));

    /** {@code Loader_Blocks.java:137}: {@code BlockSwamp(...).addEffect(hunger 300/0, confusion 120/0)}. */
    private static final List<EffectRow> SWAMP_HEAD = List.of(
            new EffectRow(MobEffects.HUNGER, 300, 0, "Loader_Blocks.java:137 hunger 300/0"),
            new EffectRow(MobEffects.CONFUSION, 120, 0, "Loader_Blocks.java:137 confusion 120/0"));

    /** The head-inside rows of one fluid. */
    public static List<EffectRow> headInsideRows(Fluid fluid) {
        String path = path(fluid);
        return switch (path) {
            case EXTRA_HEAVY_OIL, HEAVY_OIL, MEDIUM_OIL, LIGHT_OIL -> OIL_BREATHING;
            case NATURAL_GAS -> GAS_BREATHING;
            case SWAMP_WATER -> SWAMP_HEAD;
            default -> List.of();
        };
    }

    public static boolean hasHeadInsideEffects(Fluid fluid) {
        return !headInsideRows(fluid).isEmpty();
    }

    /**
     * Which immunity GT6's head-inside path asks for. {@code Loader_Blocks:137} registers the swamp
     * through {@code BlockWaterlike}, everything else through {@code BlockBaseFluid}.
     */
    public static HeadGate headGate(Fluid fluid) {
        return SWAMP_WATER.equals(path(fluid)) ? HeadGate.WATERLIKE : HeadGate.BREATHING_SUIT;
    }

    /**
     * The gate of {@code BlockBaseFluid:412} / {@code BlockWaterlike:227} for one fluid and entity.
     * The waterlike branch asks the gas suit for gases and the chem suit for everything else; the base
     * branch always asks the gas suit - including for oils, which are liquids.
     */
    public static boolean headGatePasses(LivingEntity entity, Fluid fluid) {
        if (headGate(fluid) == HeadGate.WATERLIKE) {
            return FluidHazards.isGas(fluid)
                    ? !GTEntityHelper.isImmuneToBreathingGases(entity)
                    : !GTHazmat.isChemProtected(entity);
        }
        return !GTEntityHelper.isImmuneToBreathingGases(entity);
    }

    /**
     * {@code BlockSwamp.java:194}: {@code if (aEntity instanceof EntitySlime) return;} - the swamp's
     * own addition to the waterlike rule, applied before any effect or damage.
     */
    public static boolean headInsideImmune(LivingEntity entity, Fluid fluid) {
        return SWAMP_WATER.equals(path(fluid)) && entity instanceof Slime;
    }

    /**
     * {@code BlockBaseFluid:414} / {@code BlockWaterlike:229}: {@code getMaterial() != Material.water}.
     * Only the oils and the natural gas carry a non-water material, so only they drown.
     */
    public static boolean headInsideDrowns(Fluid fluid) {
        String path = path(fluid);
        return switch (path) {
            case EXTRA_HEAVY_OIL, HEAVY_OIL, MEDIUM_OIL, LIGHT_OIL, NATURAL_GAS -> true;
            default -> false;
        };
    }

    /** The effects half of the head-inside rule, without any damage. */
    public static boolean applyHeadInside(LivingEntity entity, Fluid fluid) {
        List<EffectRow> rows = headInsideRows(fluid);
        if (rows.isEmpty() || entity.level().isClientSide || entity.isDeadOrDying()) return false;
        if (headInsideImmune(entity, fluid)) return false;
        if (!headGatePasses(entity, fluid)) return false;
        for (EffectRow row : rows) entity.addEffect(row.instance());
        return true;
    }

    /** {@code SERVER_TIME % 20 == 0}, the cadence of the drown damage only. */
    public static boolean isDrownTick(long gameTime) {
        return gameTime % DROWN_INTERVAL == 0;
    }

    // ---------------------------------------------------------------------------------------------
    // Shared fluid lookup
    // ---------------------------------------------------------------------------------------------

    /**
     * Registry path of a fluid with the {@code _flowing} suffix removed, matching
     * {@link RegisteredFluids#sanitizePath} - so a flowing variant is classified like its still fluid,
     * exactly as {@link FluidHazards} does.
     */
    public static String path(Fluid fluid) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        if (id == null) return "";
        String path = RegisteredFluids.sanitizePath(id.getPath());
        return path.endsWith("_flowing") ? path.substring(0, path.length() - "_flowing".length()) : path;
    }

    /** Every fluid of the port's nine world-fluid blocks, for the coverage assertion in the test. */
    public static List<String> worldFluidPaths() {
        return List.of(EXTRA_HEAVY_OIL, HEAVY_OIL, MEDIUM_OIL, LIGHT_OIL, NATURAL_GAS, GEOTHERMAL_WATER,
                SWAMP_WATER, "seawater", "riverwater");
    }
}
