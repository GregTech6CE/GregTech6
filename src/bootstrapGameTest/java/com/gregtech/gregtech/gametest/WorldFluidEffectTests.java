package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.content.hazard.BathingEffectEvents;
import com.gregtech.gregtech.content.hazard.BreathingGasEvents;
import com.gregtech.gregtech.content.hazard.WorldFluidEffects;
import com.gregtech.gregtech.damage.GTHazmat;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * GT6's two remaining world-fluid hazards: the <em>bathing</em> effects a body gets from the fluid it
 * stands in ({@code BlockBaseFluid:404-409}) and the <em>head inside</em> effects of the fluids that
 * are not gases ({@code BlockWaterlike:226-231}, i.e. the swamp).
 *
 * <p>§107 pinned the gas half (natural gas: poison 300 / confusion 120 plus 2.0 drown every 20 ticks).
 * What was still missing is everything {@code Loader_Blocks.java:137-152} declares for the other eight
 * world fluids of the port, plus the two differences between the GT6 paths that are easy to get wrong:
 *
 * <ul>
 *   <li>a fluid with no effect row is <em>harmless</em> - clean sea and river water have no row at all
 *       ({@code :135,136}), so "it is water" is not what makes it safe;</li>
 *   <li>drown damage follows the fluid's <em>material</em>, not its type: the oils and the natural gas
 *       carry {@code MaterialOil}/{@code MaterialGas} and drown, while the swamp is
 *       {@code Material.water} and only starves you ({@code BlockWaterlike:229});</li>
 *   <li>the immunity gate differs per path: {@code BlockBaseFluid:412} always asks the gas suit (even
 *       for the liquid oils), {@code BlockWaterlike:227} asks the gas suit for gases and the chem suit
 *       for everything else - which is the swamp.</li>
 * </ul>
 *
 * All coordinates are absolute (BASE 35000) because {@code test_empty} is 1x1x1; the world fluid blocks
 * come from {@code Fluid#defaultFluidState().createLegacyBlock()}, which is the port's
 * {@code GTWorldFluidBlock} for exactly these nine fluids ({@code Loader_Fluids.WORLD_FLUID_PATHS}).
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class WorldFluidEffectTests {
    private static final int BASE_X = 35000;
    private static final int BASE_Z = 35000;
    private static final int BASE_Y = 100;

    private static BlockPos at(int dx, int dy, int dz) {
        return new BlockPos(BASE_X + dx, BASE_Y + dy, BASE_Z + dz);
    }

    /** A world fluid by the registry path the port registers it under ({@code Loader_Fluids:39-42}). */
    private static Fluid worldFluid(GameTestHelper helper, String path) {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", path));
        helper.assertTrue(fluid != null, "gregtech:" + path + " is a registered fluid, got null");
        return fluid;
    }

    /** Places a fluid source at an absolute position and asserts the block really holds that fluid. */
    private static BlockPos placeFluid(GameTestHelper helper, Fluid fluid, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), 3);
        helper.assertTrue(level.getFluidState(pos).getType() == fluid,
                "the block at " + pos + " holds " + registryName(fluid) + ", got "
                        + registryName(level.getFluidState(pos).getType()));
        return pos;
    }

    private static String registryName(Fluid fluid) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        return id == null ? String.valueOf(fluid) : id.toString();
    }

    /** GT6 reads the block at {@code roundDown(posY + getEyeHeight())} ({@code GT_API_Proxy:523}). */
    private static void putEyesIn(net.minecraft.world.entity.LivingEntity entity, BlockPos pos) {
        entity.moveTo(pos.getX() + 0.5D, pos.getY() + 0.5D - entity.getEyeHeight(), pos.getZ() + 0.5D,
                entity.getYRot(), entity.getXRot());
        entity.invulnerableTime = 0;
    }

    // ---------------------------------------------------------------------------------------------
    // 1) The tables themselves
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void worldFluidTablesMatchGt6sRegistrationLines(GameTestHelper helper) {
        // Loader_Blocks:135,136 - the river and the ocean declare no effect at all, so they are inert
        // even though they are water; a fluid without a row never drowns anyone either.
        for (String clean : new String[]{"seawater", "riverwater"}) {
            Fluid fluid = worldFluid(helper, clean);
            helper.assertTrue(WorldFluidEffects.headInsideRows(fluid).isEmpty(),
                    clean + " has no head-inside row, got " + WorldFluidEffects.headInsideRows(fluid));
            helper.assertTrue(WorldFluidEffects.bathingRows(fluid).isEmpty(),
                    clean + " has no bathing row, got " + WorldFluidEffects.bathingRows(fluid));
            helper.assertTrue(!WorldFluidEffects.actsLikeWeb(fluid), clean + " is not a web");
            helper.assertTrue(!WorldFluidEffects.headInsideDrowns(fluid), clean + " does not drown");
        }

        // Loader_Blocks:146 - geothermal water heals: regeneration 100/0 and resistance 2400/2.
        Fluid geothermal = worldFluid(helper, "watergeothermal");
        var geoRows = WorldFluidEffects.bathingRows(geothermal);
        helper.assertTrue(geoRows.size() == 2, "geothermal water has 2 bathing rows, got " + geoRows.size());
        helper.assertTrue(geoRows.get(0).effect() == MobEffects.REGENERATION
                        && geoRows.get(0).duration() == 100 && geoRows.get(0).amplifier() == 0,
                "row 0 is regeneration 100/0, got " + geoRows.get(0));
        helper.assertTrue(geoRows.get(1).effect() == MobEffects.DAMAGE_RESISTANCE
                        && geoRows.get(1).duration() == 2400 && geoRows.get(1).amplifier() == 2,
                "row 1 is resistance 2400/2, got " + geoRows.get(1));
        helper.assertTrue(!WorldFluidEffects.headInsideDrowns(geothermal),
                "the spring is Material.water (Loader_Blocks:146), so it does not drown");

        // Loader_Blocks:149-153 - every oil breathes poison 300/0 + confusion 120/0 and blinds 60/1;
        // the extra-heavy and heavy oils are the two that also call .setWeb().
        for (String oilPath : new String[]{"liquid_extra_heavy_oil", "liquid_heavy_oil",
                "liquid_medium_oil", "liquid_light_oil"}) {
            Fluid oil = worldFluid(helper, oilPath);
            var head = WorldFluidEffects.headInsideRows(oil);
            helper.assertTrue(head.size() == 2 && head.get(0).effect() == MobEffects.POISON
                            && head.get(0).duration() == 300 && head.get(0).amplifier() == 0
                            && head.get(1).effect() == MobEffects.CONFUSION
                            && head.get(1).duration() == 120 && head.get(1).amplifier() == 0,
                    oilPath + " breathes poison 300/0 + confusion 120/0, got " + head);
            var bath = WorldFluidEffects.bathingRows(oil);
            helper.assertTrue(bath.size() == 1 && bath.get(0).effect() == MobEffects.BLINDNESS
                            && bath.get(0).duration() == 60 && bath.get(0).amplifier() == 1,
                    oilPath + " bathes blindness 60/1 (the other three rows are Immersive Engineering "
                            + "potions, recorded in WorldFluidEffects.OIL_IE_EFFECTS), got " + bath);
            helper.assertTrue(WorldFluidEffects.headInsideDrowns(oil),
                    oilPath + " is MaterialOil, so it drowns");
        }
        helper.assertTrue(WorldFluidEffects.actsLikeWeb(worldFluid(helper, "liquid_extra_heavy_oil"))
                        && WorldFluidEffects.actsLikeWeb(worldFluid(helper, "liquid_heavy_oil")),
                "the two heavy oils call .setWeb() (Loader_Blocks:149,150)");
        helper.assertTrue(!WorldFluidEffects.actsLikeWeb(worldFluid(helper, "liquid_medium_oil"))
                        && !WorldFluidEffects.actsLikeWeb(worldFluid(helper, "liquid_light_oil")),
                "the medium and light oils do not");

        // Loader_Blocks:137 - the swamp starves and confuses, on the waterlike path, and never drowns.
        Fluid swamp = worldFluid(helper, "swampwater");
        var swampRows = WorldFluidEffects.headInsideRows(swamp);
        helper.assertTrue(swampRows.size() == 2 && swampRows.get(0).effect() == MobEffects.HUNGER
                        && swampRows.get(0).duration() == 300 && swampRows.get(0).amplifier() == 0
                        && swampRows.get(1).effect() == MobEffects.CONFUSION
                        && swampRows.get(1).duration() == 120,
                "the swamp gives hunger 300/0 + confusion 120/0, got " + swampRows);
        helper.assertTrue(WorldFluidEffects.headGate(swamp) == WorldFluidEffects.HeadGate.WATERLIKE,
                "the swamp is registered through BlockWaterlike (Loader_Blocks:137), so it uses the "
                        + "waterlike gate");
        helper.assertTrue(WorldFluidEffects.headGate(worldFluid(helper, "gas_natural_gas"))
                        == WorldFluidEffects.HeadGate.BREATHING_SUIT,
                "natural gas goes through BlockBaseFluid (Loader_Blocks:153)");

        // The whole table in one gate-log line, so a future change to any of it is visible.
        StringBuilder summary = new StringBuilder();
        for (String path : WorldFluidEffects.worldFluidPaths()) {
            Fluid fluid = worldFluid(helper, path);
            summary.append(path).append("[head=").append(WorldFluidEffects.headInsideRows(fluid).size())
                    .append(",bath=").append(WorldFluidEffects.bathingRows(fluid).size())
                    .append(",web=").append(WorldFluidEffects.actsLikeWeb(fluid) ? 1 : 0)
                    .append(",drown=").append(WorldFluidEffects.headInsideDrowns(fluid) ? 1 : 0)
                    .append("] ");
        }
        GregTech.LOGGER.info("[hazard] world fluid effect table: {}", summary.toString().trim());
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // 2) Bathing: body contact
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void bathingEffectsReachTheBodyAndRespectTheChemSuit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Fluid geothermal = worldFluid(helper, "watergeothermal");
        BlockPos pos = placeFluid(helper, geothermal, at(0, 0, 0));

        Cow cow = helper.spawn(EntityType.COW, BlockPos.ZERO);
        cow.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        helper.assertTrue(BathingEffectEvents.fluidsTouching(level, cow).contains(geothermal),
                "the cow's bounding box overlaps the spring, found "
                        + BathingEffectEvents.fluidsTouching(level, cow));

        // GT6 BlockBaseFluid:404-407 runs on every tick the body overlaps the block.
        BathingEffectEvents.onLivingTick(new LivingEvent.LivingTickEvent(cow));
        MobEffectInstance regen = cow.getEffect(MobEffects.REGENERATION);
        MobEffectInstance resistance = cow.getEffect(MobEffects.DAMAGE_RESISTANCE);
        helper.assertTrue(regen != null && regen.getDuration() == 100 && regen.getAmplifier() == 0,
                "the spring regenerates for 100 ticks at amplifier 0, got "
                        + (regen == null ? "no effect" : regen.getDuration() + "/" + regen.getAmplifier()));
        helper.assertTrue(resistance != null && resistance.getDuration() == 2400
                        && resistance.getAmplifier() == 2,
                "and gives resistance 2400/2, got " + (resistance == null ? "no effect"
                        : resistance.getDuration() + "/" + resistance.getAmplifier()));

        // GT6 :406 - the chemical hazmat suit is the gate, and creative mode counts as wearing it
        // (GTHazmat.isChemProtected), which is exactly how the existing hazard tests gate a mock player.
        Player creative = helper.makeMockPlayer();
        helper.assertTrue(GTHazmat.isChemProtected(creative), "a creative player is chem protected");
        creative.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        BathingEffectEvents.onLivingTick(new LivingEvent.LivingTickEvent(creative));
        helper.assertTrue(creative.getEffect(MobEffects.REGENERATION) == null,
                "a chem-protected body gets nothing from the spring, got "
                        + creative.getEffect(MobEffects.REGENERATION));
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // 3) Head inside: oil vs swamp vs clean water
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void headInsideRulesDifferBetweenOilSwampAndCleanWater(GameTestHelper helper) {
        // The swamp is Material.water: hunger and confusion, but no drown damage at all.
        BlockPos swampPos = placeFluid(helper, worldFluid(helper, "swampwater"), at(0, 8, 0));
        Cow cow = helper.spawn(EntityType.COW, BlockPos.ZERO);
        putEyesIn(cow, swampPos);
        helper.assertTrue(BreathingGasEvents.headPos(cow).equals(swampPos),
                "the eyes are in the swamp block, got " + BreathingGasEvents.headPos(cow));
        helper.assertTrue(!BreathingGasEvents.breatheIn(cow),
                "swamp water deals no drown damage (BlockWaterlike:229 getMaterial() == water)");
        helper.assertTrue(cow.getHealth() == cow.getMaxHealth(),
                "so the cow is unhurt, health " + cow.getHealth());
        MobEffectInstance hunger = cow.getEffect(MobEffects.HUNGER);
        MobEffectInstance confusion = cow.getEffect(MobEffects.CONFUSION);
        helper.assertTrue(hunger != null && hunger.getDuration() == 300 && hunger.getAmplifier() == 0,
                "the swamp starves for 300 ticks at amplifier 0, got "
                        + (hunger == null ? "no effect" : hunger.getDuration() + "/" + hunger.getAmplifier()));
        helper.assertTrue(confusion != null && confusion.getDuration() == 120,
                "and confuses for 120 ticks, got "
                        + (confusion == null ? "no effect" : confusion.getDuration()));

        // BlockSwamp:194 - a slime is immune to the swamp before any effect is applied.
        Slime slime = helper.spawn(EntityType.SLIME, BlockPos.ZERO);
        putEyesIn(slime, swampPos);
        helper.assertTrue(!WorldFluidEffects.applyHeadInside(slime, worldFluid(helper, "swampwater")),
                "a slime is immune to swamp water (BlockSwamp:194)");
        helper.assertTrue(slime.getEffect(MobEffects.HUNGER) == null,
                "so the slime is not starving, got " + slime.getEffect(MobEffects.HUNGER));

        // An oil is a liquid, yet it poisons, confuses and drowns: MaterialOil, not Material.water.
        BlockPos oilPos = placeFluid(helper, worldFluid(helper, "liquid_light_oil"), at(8, 8, 0));
        Cow oily = helper.spawn(EntityType.COW, BlockPos.ZERO);
        putEyesIn(oily, oilPos);
        helper.assertTrue(BreathingGasEvents.breatheIn(oily),
                "a light oil drowns whoever breathes it (MaterialOil)");
        helper.assertTrue(Math.abs(10.0F - oily.getHealth() - BreathingGasEvents.DROWN_DAMAGE) < 0.001F,
                "exactly 2.0, health " + oily.getHealth());
        MobEffectInstance poison = oily.getEffect(MobEffects.POISON);
        helper.assertTrue(poison != null && poison.getDuration() == 300 && poison.getAmplifier() == 0,
                "the oil poisons for 300 ticks at amplifier 0, got "
                        + (poison == null ? "no effect" : poison.getDuration() + "/" + poison.getAmplifier()));
        helper.assertTrue(WorldFluidEffects.applyBathing(oily, worldFluid(helper, "liquid_light_oil")),
                "and bathing in it blinds (Loader_Blocks:151 blindness 60/1)");
        MobEffectInstance blindness = oily.getEffect(MobEffects.BLINDNESS);
        helper.assertTrue(blindness != null && blindness.getDuration() == 60 && blindness.getAmplifier() == 1,
                "blindness 60/1, got " + (blindness == null ? "no effect"
                        : blindness.getDuration() + "/" + blindness.getAmplifier()));
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // 4) The web flag of the two heavy oils
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void heavyOilsActLikeWebAndMediumOilDoesNot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos heavyPos = placeFluid(helper, worldFluid(helper, "liquid_extra_heavy_oil"), at(8, 0, 0));
        BlockPos mediumPos = placeFluid(helper, worldFluid(helper, "liquid_medium_oil"), at(12, 0, 0));

        Cow webbed = helper.spawn(EntityType.COW, BlockPos.ZERO);
        Cow free = helper.spawn(EntityType.COW, BlockPos.ZERO);
        webbed.moveTo(heavyPos.getX() + 0.5D, heavyPos.getY(), heavyPos.getZ() + 0.5D);
        free.moveTo(mediumPos.getX() + 0.5D, mediumPos.getY(), mediumPos.getZ() + 0.5D);

        double webbedStep = step(level, webbed, heavyPos);
        double freeStep = step(level, free, mediumPos);
        helper.assertTrue(freeStep > 0.5D, "a cow walks through medium oil, moved " + freeStep);
        helper.assertTrue(webbedStep < freeStep * 0.5D,
                "the extra-heavy oil holds it like a cobweb (BlockBaseFluid:405 setInWeb), moved "
                        + webbedStep + " against " + freeStep + " in medium oil");
        helper.succeed();
    }

    /**
     * Vanilla calls {@code BlockState#entityInside} for every block an entity's box overlaps
     * ({@code Entity#checkInsideBlocks}); this does the same for one block and then moves the entity by
     * one block per tick, which is where {@code makeStuckInBlock}'s multiplier is consumed.
     *
     * <p>The fluid positions of this class are absolute ({@code BASE_X/Y/Z}) and the world there is
     * whatever worldgen produced, so a hillside just east of the cow clips {@code Entity.move} to the
     * block face and the measurement reads "barely moved" instead of "walks freely" - a false negative of
     * the §111 kind, where the path itself was never open. Clear the two blocks the cow walks into (not
     * the fluid block it stands in) before measuring.</p>
     */
    private static double step(Level level, Cow cow, BlockPos pos) {
        for (int dx = 1; dx <= 2; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                level.removeBlock(pos.offset(dx, dy, 0), false);
            }
        }
        level.getBlockState(pos).entityInside(level, pos, cow);
        double before = cow.getX();
        cow.setDeltaMovement(1.0D, 0.0D, 0.0D);
        cow.move(MoverType.SELF, cow.getDeltaMovement());
        return cow.getX() - before;
    }

    // ---------------------------------------------------------------------------------------------
    // 5) The tick cadence: effects every tick, damage every 20
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void effectsLandEveryTickAndTheDrownDamageEveryTwenty(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos gasPos = placeFluid(helper, worldFluid(helper, "gas_natural_gas"), at(0, 4, 0));
        Cow cow = helper.spawn(EntityType.COW, BlockPos.ZERO);
        putEyesIn(cow, gasPos);

        float before = cow.getHealth();
        // GT6 BlockBaseFluid:412-414 applies the effects on every tick it is inside and only throttles
        // the damage, so the damage of one event call is decided by the game clock, not by the effects.
        boolean drownTick = BreathingGasEvents.isDrownTick(level.getGameTime());
        BreathingGasEvents.onLivingTick(new LivingEvent.LivingTickEvent(cow));

        MobEffectInstance poison = cow.getEffect(MobEffects.POISON);
        helper.assertTrue(poison != null && poison.getDuration() == 300,
                "the effects land whatever the tick is, got "
                        + (poison == null ? "no effect" : poison.getDuration()));
        float expectedLoss = drownTick ? BreathingGasEvents.DROWN_DAMAGE : 0.0F;
        helper.assertTrue(Math.abs((before - cow.getHealth()) - expectedLoss) < 0.001F,
                "game time " + level.getGameTime() + " is " + (drownTick ? "" : "not ")
                        + "a drown tick, so the cow loses " + expectedLoss + ", lost "
                        + (before - cow.getHealth()));
        helper.succeed();
    }
}
