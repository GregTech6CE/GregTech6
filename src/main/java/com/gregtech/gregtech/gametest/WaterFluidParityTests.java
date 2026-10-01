package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.api.fluid.FluidRenderLayers;
import com.gregtech.gregtech.api.fluid.FluidVisualPolicy;
import com.gregtech.gregtech.api.fluid.GTWaterParity;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.worldgen.GTWaterBodyFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Parity of GT6's three world waters (sea, river and swamp water) with vanilla {@code minecraft:water}.
 *
 * <p>These are separate fluids - GT6 keeps three kinds and {@code GTWaterBodyFeature} replaces the
 * water of oceans, rivers and swamps with them - so nothing made them inherit vanilla water's
 * attributes or rendering. The tests below pin every part of that parity:
 * <ul>
 *   <li>{@link #worldWatersAreInVanillaWaterTag}: all six fluid ids (still and flowing) are in
 *       {@code #minecraft:water}, which is what swimming, boats, waterlogged blocks, concrete,
 *       sponge, farmland hydration, the water fog and the camera overlay all check. Nothing else
 *       from the port is in that tag.</li>
 *   <li>{@link #fluidTypeMatchesVanillaWater}: the {@code FluidType} reports the same values as
 *       {@code ForgeMod.WATER_TYPE}.</li>
 *   <li>{@link #worldBlocksMatchVanillaWater}: the world blocks are plain {@code LiquidBlock}s with
 *       vanilla water's state shape, drops, explosion resistance and map colour.</li>
 *   <li>{@link #placedSourceSpreadsLikeVanillaWater}: a placed source actually spreads one level
 *       down into its four horizontal neighbours, exactly like a placed vanilla water source, and
 *       its tick delay / source conversion match vanilla water's.</li>
 *   <li>{@link #clientTintMatchesVanillaWater}: the client visual is vanilla's still sprite with
 *       vanilla's {@code 0x3F76E4} water tint.</li>
 *   <li>{@link #worldWatersReportVanillaWaterFluidType}: the fluids report
 *       {@code ForgeMod.WATER_TYPE}, which is the identity every Forge check for "the entity is in
 *       water" uses, so the splash sound, the swimming animation, the underwater overlay, the FOV
 *       change, the bubble HUD and drowning all answer like vanilla water.</li>
 *   <li>{@link #renderLayerMatchesVanillaWater}: the layer the port selects for the three is the
 *       layer vanilla water is built into, so the world water is translucent, not solid.</li>
 *   <li>{@link #waterloggedBlocksMatchVanillaWater}: a vanilla waterlogged block placed in a GT6
 *       water column keeps its waterlogged state and its water-tagged fluid state, and a GT6 water
 *       tick next to it waterlogs it exactly as a vanilla water tick does.</li>
 *   <li>{@link #waterBodyPassKeepsPlantsInGtWater}: the water-body pass replaces water and leaves
 *       seagrass and kelp, which carry vanilla water internally like they do in vanilla.</li>
 * </ul>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class WaterFluidParityTests {
    private static final List<String> WORLD_WATERS =
            List.of(GTWaterParity.SEAWATER, GTWaterParity.RIVERWATER, GTWaterParity.SWAMPWATER);

    /** A flat, dry test area with a stone floor, far away from the other water tests. */
    private static final int BASE_X = 52000;
    private static final int BASE_Z = 52000;
    private static final int BASE_Y = 100;

    private static ResourceLocation id(String path) {
        return GregTech.id(path);
    }

    /** All six registered ids, still and flowing, for the three world waters. */
    private static List<String> allIds() {
        List<String> ids = new ArrayList<>();
        for (String path : WORLD_WATERS) {
            ids.add(path);
            ids.add(path + "_flowing");
        }
        return ids;
    }

    // ==================== 1. the fluid tag ====================

    /**
     * The water <em>family</em> test, ported from TFC's own river water
     * ({@code RiverWaterFluid.isSame}: {@code super.isSame(fluid) || fluid == TFCFluids.RIVER_WATER.get()}).
     * Vanilla drives surface merging ({@code FlowingFluid.getHeight}) and the slope maths
     * ({@code getSlopeDistance}) through {@code Fluid#isSame}; without the override two adjacent world
     * waters - and a world water next to vanilla water - counted as different fluids, which is the render
     * seam and the "not quite like vanilla water" surface the port used to show.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void worldWatersAreOneWaterFamily(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        java.util.Set<Fluid> waters = new java.util.LinkedHashSet<>();
        for (String path : GTWaterParity.WORLD_WATER_REGISTRY_PATHS) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(id(path));
            helper.assertTrue(fluid != null, path + " is registered");
            if (fluid != null) {
                waters.add(fluid);
            }
        }
        helper.assertTrue(waters.size() == GTWaterParity.WORLD_WATER_REGISTRY_PATHS.size(),
                "all six world-water ids exist, got " + waters.size());

        Fluid still = ForgeRegistries.FLUIDS.getValue(id(GTWaterParity.SEAWATER));
        Fluid flowing = ForgeRegistries.FLUIDS.getValue(id(GTWaterParity.SEAWATER + "_flowing"));
        helper.assertTrue(still != null && flowing != null, "seawater and its flowing variant exist");
        if (still == null || flowing == null) {
            return;
        }
        for (Fluid other : waters) {
            helper.assertTrue(still.isSame(other),
                    "sea water reports every world water as the same fluid, failed for " + other);
        }
        helper.assertTrue(still.isSame(Fluids.WATER) && still.isSame(Fluids.FLOWING_WATER),
                "and it reports vanilla water (both variants) as the same fluid");
        helper.assertTrue(flowing.isSame(Fluids.WATER) && flowing.isSame(still),
                "the flowing variant does too, in both directions");

        // The observable consequence: a flowing world water merges its surface with a water block above it.
        BlockPos pos = new BlockPos(PLACEMENT_X, BASE_Y, BASE_Z + 6);
        prepareArea(level, PLACEMENT_X, BASE_Z + 6, BASE_Y);
        FluidState shallow = flowing.defaultFluidState()
                .setValue(BlockStateProperties.LEVEL_FLOWING, 4);
        level.setBlock(pos, shallow.createLegacyBlock(), 2);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
        helper.assertTrue(level.getFluidState(pos).getHeight(level, pos) < 1.0F,
                "a world water flowing at level 4 is not a full block on its own, got "
                        + level.getFluidState(pos).getHeight(level, pos));
        level.setBlock(pos.above(), Blocks.WATER.defaultBlockState(), 2);
        helper.assertTrue(level.getFluidState(pos).getHeight(level, pos) == 1.0F,
                "with vanilla water above it the surface merges into a full block (no seam), got "
                        + level.getFluidState(pos).getHeight(level, pos));
        clearArea(level, PLACEMENT_X, BASE_Z + 6, BASE_Y);
        helper.succeed();
    }

    /**
     * §105: the family has to be <em>symmetric</em>. §100 fixed the GT6 side of {@code isSame}, but
     * vanilla's own {@code WaterFluid.isSame} is an identity test ({@code WaterFluid.java:74-76}:
     * {@code fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER}), so a <b>vanilla</b> water block
     * next to GT6 water still counted as a different fluid: its surface did not merge with the GT6
     * water surface and vanilla water would not spread into a GT6 water body. {@code WaterFluid} is
     * vanilla code, so only a mixin can close that half — {@code mixin/WaterFluidMixin} does (same
     * family test as §100: membership of {@code #minecraft:water}).
     *
     * <p>This test is also the guard that the mixin config is actually applied: without
     * {@code WaterFluidMixin} every "vanilla water reports …" assertion below fails.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void vanillaWaterKnowsTheWorldWaters(GameTestHelper helper) {
        Fluid still = ForgeRegistries.FLUIDS.getValue(id(GTWaterParity.SEAWATER));
        Fluid flowing = ForgeRegistries.FLUIDS.getValue(id(GTWaterParity.SEAWATER + "_flowing"));
        helper.assertTrue(still != null && flowing != null, "seawater and its flowing variant exist");
        if (still == null || flowing == null) {
            return;
        }

        // (a) The mixin itself: vanilla water (source and flowing) reports the world waters as water.
        helper.assertTrue(Fluids.WATER.isSame(still) && Fluids.WATER.isSame(flowing),
                "vanilla water reports GT6's sea water as the same fluid (WaterFluidMixin)");
        helper.assertTrue(Fluids.FLOWING_WATER.isSame(still) && Fluids.FLOWING_WATER.isSame(flowing),
                "vanilla flowing water reports the world waters as the same fluid too");
        for (String path : GTWaterParity.WORLD_WATER_REGISTRY_PATHS) {
            Fluid other = ForgeRegistries.FLUIDS.getValue(id(path));
            helper.assertTrue(other != null && Fluids.WATER.isSame(other) && Fluids.FLOWING_WATER.isSame(other),
                    "vanilla water knows " + path + " as water");
            if (other != null) {
                BlockState worldWater = other.defaultFluidState().createLegacyBlock();
                BlockState vanillaWater = Blocks.WATER.defaultBlockState();
                helper.assertTrue(vanillaWater.skipRendering(worldWater, Direction.NORTH),
                        "vanilla water hides the shared face next to " + path);
                helper.assertTrue(worldWater.skipRendering(vanillaWater, Direction.SOUTH),
                        path + " hides the shared face next to vanilla water");
            }
        }

        // (b) Only the family grew: lava (not in #minecraft:water) keeps vanilla's own answer.
        helper.assertTrue(!Fluids.WATER.isSame(Fluids.LAVA) && !Fluids.WATER.isSame(Fluids.FLOWING_LAVA),
                "lava is still not water");
        helper.assertTrue(Fluids.WATER.isSame(Fluids.FLOWING_WATER) && Fluids.FLOWING_WATER.isSame(Fluids.WATER),
                "vanilla water and vanilla flowing water still match each other");

        // (c) The GT6 side (§100) is unchanged, so both directions agree now.
        helper.assertTrue(still.isSame(Fluids.WATER) && flowing.isSame(Fluids.FLOWING_WATER),
                "the world waters still report vanilla water as the same fluid (§100)");

        // (d) The consequence the renderer reads: LiquidBlockRenderer asks
        // `fluid.isSame(neighbourFluid)` with `fluid` = the block being rendered (LiquidBlockRenderer:44
        // and :329), so for a *vanilla* water block beside GT6 water this is now true and the surfaces
        // merge instead of showing a step.
        ServerLevel level = helper.getLevel();
        int x = BASE_X + 60;
        int z = BASE_Z + 60;
        prepareArea(level, x, z, BASE_Y);
        BlockPos below = new BlockPos(x, BASE_Y - 1, z);
        BlockPos above = new BlockPos(x, BASE_Y, z);
        BlockState shallow = flowing.defaultFluidState()
                .setValue(BlockStateProperties.LEVEL_FLOWING, 4).createLegacyBlock();
        level.setBlock(below, shallow, 2);
        level.setBlock(above, Blocks.WATER.defaultBlockState(), 2);
        helper.assertTrue(Fluids.WATER.isSame(level.getFluidState(below).getType()),
                "the renderer's own test - vanilla water against the world water below it - is true");
        helper.assertTrue(Fluids.WATER.isSame(level.getFluidState(above).getType()),
                "and vanilla water against vanilla water is still true");
        helper.assertTrue(level.getFluidState(below).getHeight(level, below) == 1.0F,
                "the world water below a vanilla water block is a full block (merged surface), got "
                        + level.getFluidState(below).getHeight(level, below));
        clearArea(level, x, z, BASE_Y);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void worldWatersAreInVanillaWaterTag(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.FLUID);
        helper.assertTrue(registry.getTag(FluidTags.WATER).isPresent(), "#minecraft:water is loaded");

        List<String> problems = new ArrayList<>();
        for (String path : allIds()) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(id(path));
            if (fluid == null) {
                problems.add(path + " is not registered");
            } else if (!fluid.is(FluidTags.WATER)) {
                problems.add(path + " is not in #minecraft:water");
            }
        }
        helper.assertTrue(problems.isEmpty(), "world waters in #minecraft:water: " + problems);

        // Vanilla's own pair must still be there (the tag file uses "replace": false).
        helper.assertTrue(Fluids.WATER.is(FluidTags.WATER) && Fluids.FLOWING_WATER.is(FluidTags.WATER),
                "minecraft:water and minecraft:flowing_water stay in the tag");

        // Nothing else from the port may have slipped into the tag by accident.
        Set<String> ours = new TreeSet<>();
        for (Holder<Fluid> holder : registry.getTagOrEmpty(FluidTags.WATER)) {
            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(holder.value());
            if (fluidId != null && fluidId.getNamespace().equals(GregTech.NAMESPACE)) ours.add(fluidId.getPath());
        }
        Set<String> expected = new TreeSet<>(allIds());
        helper.assertTrue(ours.equals(expected),
                "only GT6's three world waters and their flowing variants are in #minecraft:water, found " + ours);
        helper.succeed();
    }

    // ==================== 2. the FluidType ====================

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void fluidTypeMatchesVanillaWater(GameTestHelper helper) {
        FluidType vanilla = ForgeMod.WATER_TYPE.get();
        helper.assertTrue(vanilla != null, "ForgeMod.WATER_TYPE is registered");
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(BASE_X, BASE_Y, BASE_Z);
        FluidState reference = Fluids.WATER.defaultFluidState();

        List<String> problems = new ArrayList<>();
        for (String path : allIds()) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(id(path));
            helper.assertTrue(fluid != null, path + " is not registered");
            FluidType type = fluid.getFluidType();

            // Numbers
            if (type.getDensity() != vanilla.getDensity()) problems.add(path + " density " + type.getDensity());
            if (type.getViscosity() != vanilla.getViscosity()) problems.add(path + " viscosity " + type.getViscosity());
            if (type.getTemperature() != vanilla.getTemperature()) problems.add(path + " temperature " + type.getTemperature());
            if (type.getLightLevel() != vanilla.getLightLevel()) problems.add(path + " light " + type.getLightLevel());
            if (type.getRarity() != vanilla.getRarity()) problems.add(path + " rarity " + type.getRarity());

            // Entity attributes that decide swimming, drowning and falling into the fluid
            if (type.motionScale((Entity) null) != vanilla.motionScale((Entity) null)) problems.add(path + " motionScale");
            if (type.canPushEntity((Entity) null) != vanilla.canPushEntity((Entity) null)) problems.add(path + " canPushEntity");
            if (type.canSwim((Entity) null) != vanilla.canSwim((Entity) null)) problems.add(path + " canSwim");
            if (!type.canSwim((Entity) null)) problems.add(path + " cannot be swum in");
            if (type.canDrownIn((LivingEntity) null) != vanilla.canDrownIn((LivingEntity) null)) problems.add(path + " canDrown");
            if (type.supportsBoating((Boat) null) != vanilla.supportsBoating((Boat) null)) problems.add(path + " supportsBoating");
            if (type.canHydrate((Entity) null) != vanilla.canHydrate((Entity) null)) problems.add(path + " canHydrate");
            if (type.canExtinguish((Entity) null) != vanilla.canExtinguish((Entity) null)) problems.add(path + " canExtinguish");
            if (type.getFallDistanceModifier((Entity) null) != vanilla.getFallDistanceModifier((Entity) null)) {
                problems.add(path + " fallDistanceModifier");
            }

            // Level based attributes: source conversion, waterlogging and pathfinding
            if (type.canConvertToSource(reference, level, pos) != vanilla.canConvertToSource(reference, level, pos)) {
                problems.add(path + " canConvertToSource");
            }
            if (!type.canConvertToSource(reference, level, pos)) problems.add(path + " cannot convert to a source");
            if (type.canHydrate(reference, level, pos, Blocks.FARMLAND.defaultBlockState(), pos)
                    != vanilla.canHydrate(reference, level, pos, Blocks.FARMLAND.defaultBlockState(), pos)) {
                problems.add(path + " waterlogged/farmland canHydrate");
            }
            if (type.canExtinguish(reference, level, pos) != vanilla.canExtinguish(reference, level, pos)) {
                problems.add(path + " canExtinguish(state, level, pos)");
            }
            if (!Objects.equals(type.getBlockPathType(reference, level, pos, null, true),
                    vanilla.getBlockPathType(reference, level, pos, null, true))) {
                problems.add(path + " getBlockPathType(canFluidLog=true)");
            }
            if (!Objects.equals(type.getBlockPathType(reference, level, pos, null, false),
                    vanilla.getBlockPathType(reference, level, pos, null, false))) {
                problems.add(path + " getBlockPathType(canFluidLog=false)");
            }
            if (!Objects.equals(type.getAdjacentBlockPathType(reference, level, pos, null, BlockPathTypes.WATER),
                    vanilla.getAdjacentBlockPathType(reference, level, pos, null, BlockPathTypes.WATER))) {
                problems.add(path + " getAdjacentBlockPathType");
            }

            // Sound group
            if (!Objects.equals(type.getSound(SoundActions.BUCKET_FILL), vanilla.getSound(SoundActions.BUCKET_FILL))) {
                problems.add(path + " BUCKET_FILL sound");
            }
            if (!Objects.equals(type.getSound(SoundActions.BUCKET_EMPTY), vanilla.getSound(SoundActions.BUCKET_EMPTY))) {
                problems.add(path + " BUCKET_EMPTY sound");
            }
            if (!Objects.equals(type.getSound(SoundActions.FLUID_VAPORIZE), vanilla.getSound(SoundActions.FLUID_VAPORIZE))) {
                problems.add(path + " FLUID_VAPORIZE sound");
            }
        }
        helper.assertTrue(problems.isEmpty(), "world water FluidType parity: " + problems);

        // The bucket fill sound reaches the fluid itself, exactly like vanilla water.
        for (String path : WORLD_WATERS) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(id(path));
            helper.assertTrue(fluid != null && fluid.getPickupSound().equals(Fluids.WATER.getPickupSound()),
                    path + " reports vanilla water's bucket fill sound");
        }
        helper.succeed();
    }

    // ==================== 3. the world blocks ====================

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void worldBlocksMatchVanillaWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(BASE_X, BASE_Y, BASE_Z);
        Block vanilla = Blocks.WATER;
        BlockState vanillaState = vanilla.defaultBlockState();
        LootParams.Builder loot = new LootParams.Builder(level);

        List<String> problems = new ArrayList<>();
        for (String path : WORLD_WATERS) {
            Block block = ForgeRegistries.BLOCKS.getValue(id(path));
            if (block == null || block == Blocks.AIR) {
                problems.add(path + " block is not registered");
                continue;
            }
            if (!(block instanceof LiquidBlock)) {
                problems.add(path + " block is not a LiquidBlock");
                continue;
            }
            Fluid stillFluid = ForgeRegistries.FLUIDS.getValue(id(path));
            if (stillFluid == null) {
                problems.add(path + " fluid is not registered");
                continue;
            }
            BlockState state = block.defaultBlockState();

            // State shape: level 0..15 with level 0 being the full source, plus the falling flow at 15.
            if (block.getStateDefinition().getProperties().size()
                    != vanilla.getStateDefinition().getProperties().size()) {
                problems.add(path + " has a different state shape (" + block.getStateDefinition().getProperties() + ")");
            }
            if (state.getValue(LiquidBlock.LEVEL) != 0) {
                problems.add(path + " does not default to level 0");
            }
            FluidState fluidState = state.getFluidState();
            if (fluidState.getType() != stillFluid) problems.add(path + " block carries " + fluidState.getType());
            if (!fluidState.isSource()) problems.add(path + " default state is not a source");
            if (fluidState.getAmount() != vanillaState.getFluidState().getAmount()) {
                problems.add(path + " source amount " + fluidState.getAmount());
            }
            // The fluid state of a placed block is water-tagged: this is the check swimming,
            // waterlogged blocks, sponge and farmland hydration perform.
            if (!fluidState.is(FluidTags.WATER)) problems.add(path + " block fluid state is not water-tagged");
            boolean falling = state.setValue(LiquidBlock.LEVEL, 15).getFluidState().getValue(FlowingFluid.FALLING);
            boolean vanillaFalling = vanillaState.setValue(LiquidBlock.LEVEL, 15)
                    .getFluidState().getValue(FlowingFluid.FALLING);
            if (falling != vanillaFalling) problems.add(path + " level 15 is not the falling flow");

            // BlockBehaviour.Properties: map colour, explosion resistance, random ticking, drops.
            if (block.defaultMapColor() != MapColor.WATER || block.defaultMapColor() != vanilla.defaultMapColor()) {
                problems.add(path + " map colour is not water's");
            }
            if (block.getExplosionResistance() != vanilla.getExplosionResistance()) {
                problems.add(path + " explosion resistance " + block.getExplosionResistance()
                        + " instead of " + vanilla.getExplosionResistance());
            }
            if (block.isRandomlyTicking(state) != vanilla.isRandomlyTicking(vanillaState)) {
                problems.add(path + " random ticking differs from water");
            }
            if (!state.getDrops(loot).isEmpty() || !vanillaState.getDrops(loot).isEmpty()) {
                problems.add(path + " drops items");
            }
            if (state.getCollisionShape(level, pos).isEmpty() != vanillaState.getCollisionShape(level, pos).isEmpty()) {
                problems.add(path + " collision shape differs from water");
            }
            // Flow behaviour numbers: vanilla water is delay 5, drop off 1, slope distance 4,
            // explosion resistance 100.0F. Only the observable ones are reachable from here.
            FlowingFluid flowing = (FlowingFluid) fluidState.getType();
            if (flowing.getTickDelay(level) != Fluids.WATER.getTickDelay(level)) {
                problems.add(path + " tick delay " + flowing.getTickDelay(level));
            }
        }
        helper.assertTrue(problems.isEmpty(), "world water block parity: " + problems);
        helper.succeed();
    }

    // ==================== 4. real spreading ====================

    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void placedSourceSpreadsLikeVanillaWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = new BlockPos(BASE_X, BASE_Y, BASE_Z);
        prepareArea(level, BASE_X, BASE_Z, BASE_Y);

        // Calibrate against vanilla water in exactly the same spot.
        BlockState[] vanillaSides = spreadOnce(level, source, Blocks.WATER);
        helper.assertTrue(vanillaSides[0].is(Blocks.WATER) && vanillaSides[0].getValue(LiquidBlock.LEVEL) == 1,
                "calibration: a placed vanilla water source spreads to its neighbour at level 1, got " + vanillaSides[0]);

        List<String> problems = new ArrayList<>();
        for (String path : WORLD_WATERS) {
            Block block = ForgeRegistries.BLOCKS.getValue(id(path));
            if (block == null || block == Blocks.AIR) {
                problems.add(path + " block is not registered");
                continue;
            }
            Fluid stillFluid = ForgeRegistries.FLUIDS.getValue(id(path));
            if (stillFluid == null) {
                problems.add(path + " fluid is not registered");
                continue;
            }

            level.setBlock(source, block.defaultBlockState(), 3);
            FluidState placed = level.getFluidState(source);
            if (!placed.isSource() || placed.getAmount() != 8) {
                problems.add(path + " does not place as a full source (" + placed.getAmount() + ")");
            }
            FlowingFluid flowing = (FlowingFluid) placed.getType();
            if (flowing.getTickDelay(level) != Fluids.WATER.getTickDelay(level)) {
                problems.add(path + " tick delay " + flowing.getTickDelay(level));
            }
            // Vanilla water's canConvertToSource is the water-source game rule; ForgeFlowingFluid
            // would instead answer from the FluidType and ignore the game rule.
            if (placed.canConvertToSource(level, source)
                    != Fluids.WATER.defaultFluidState().canConvertToSource(level, source)) {
                problems.add(path + " source conversion differs from water");
            }
            if (!placed.canConvertToSource(level, source)) {
                problems.add(path + " cannot convert to a source");
            }

            // One real fluid tick: FlowingFluid.tick -> getNewLiquid -> spread.
            placed.tick(level, source);
            int index = 0;
            int spread = 0;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockState side = level.getBlockState(source.relative(direction));
                BlockState vanillaSide = vanillaSides[index];
                boolean spreadSide = side.is(block);
                boolean vanillaSpreadSide = vanillaSide.is(Blocks.WATER);
                if (spreadSide != vanillaSpreadSide) {
                    problems.add(path + " spread " + direction + " unlike water (" + side + " vs " + vanillaSide + ")");
                } else if (spreadSide && side.getValue(LiquidBlock.LEVEL)
                        != vanillaSide.getValue(LiquidBlock.LEVEL)) {
                    problems.add(path + " spread " + direction + " at level " + side.getValue(LiquidBlock.LEVEL)
                            + " instead of " + vanillaSide.getValue(LiquidBlock.LEVEL));
                }
                if (spreadSide) spread++;
                index++;
            }
            if (spread == 0) problems.add(path + " did not spread to any neighbour");
            clearSpot(level, source);
        }
        helper.assertTrue(problems.isEmpty(), "world water spreading: " + problems);

        clearArea(level, BASE_X, BASE_Z, BASE_Y);
        helper.succeed();
    }

    /**
     * A flat air pocket with a stone floor, so spreading behaves like a flat pond. The radius has to
     * be wider than vanilla's slope search (4 blocks from a neighbour, so 5 from the source), or the
     * slope-distance metric would drop some of the four horizontal neighbours from the spread map.
     */
    private static final int AREA_RADIUS = 6;

    private static void prepareArea(ServerLevel level, int x, int z, int y) {
        for (int dx = -AREA_RADIUS; dx <= AREA_RADIUS; dx++) {
            for (int dz = -AREA_RADIUS; dz <= AREA_RADIUS; dz++) {
                level.setBlock(new BlockPos(x + dx, y - 1, z + dz), Blocks.STONE.defaultBlockState(), 2);
                for (int dy = 0; dy <= 3; dy++) {
                    level.setBlock(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void clearSpot(ServerLevel level, BlockPos source) {
        level.setBlock(source, Blocks.AIR.defaultBlockState(), 2);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.setBlock(source.relative(direction), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void clearArea(ServerLevel level, int x, int z, int y) {
        for (int dx = -AREA_RADIUS; dx <= AREA_RADIUS; dx++) {
            for (int dz = -AREA_RADIUS; dz <= AREA_RADIUS; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    level.setBlock(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    /** Places one source, runs one fluid tick and returns the four horizontal neighbours' states. */
    private static BlockState[] spreadOnce(ServerLevel level, BlockPos source, Block fluidBlock) {
        level.setBlock(source, fluidBlock.defaultBlockState(), 3);
        level.getFluidState(source).tick(level, source);
        BlockState[] sides = new BlockState[4];
        int index = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            sides[index++] = level.getBlockState(source.relative(direction));
        }
        clearSpot(level, source);
        return sides;
    }

    // ==================== 5. the client tint ====================

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void clientTintMatchesVanillaWater(GameTestHelper helper) {
        helper.assertTrue(GTWaterParity.VANILLA_WATER_TINT == 0xFF3F76E4,
                "the tint constant is vanilla water's 0xFF3F76E4, got 0x"
                        + Integer.toHexString(GTWaterParity.VANILLA_WATER_TINT));

        List<RegisteredFluids.FluidEntry> entries = RegisteredFluids.all().values().stream()
                .filter(entry -> GTWaterParity.isWorldWater(entry.registryName()))
                .toList();
        helper.assertTrue(entries.size() == 3, "GT6 registers exactly three world waters, found " + entries.size());

        List<String> problems = new ArrayList<>();
        for (RegisteredFluids.FluidEntry entry : entries) {
            // exists -> false: the water look must not depend on a GT6 sprite being shipped, which is
            // what used to make seawater and swampwater fall back to the dark "molten" template.
            var visual = FluidVisualPolicy.select(entry, existing -> false);
            if (!visual.texture().equals(GTWaterParity.VANILLA_WATER_STILL_TEXTURE)) {
                problems.add(entry.registryName() + " still uses " + visual.texture());
            }
            if (visual.tint() != GTWaterParity.VANILLA_WATER_TINT) {
                problems.add(entry.registryName() + " tint 0x" + Integer.toHexString(visual.tint()));
            }
            var packed = FluidVisualPolicy.select(entry, existing -> true);
            if (!packed.equals(visual)) {
                problems.add(entry.registryName() + " changes with the resource pack: " + packed);
            }
        }
        helper.assertTrue(problems.isEmpty(), "world water client visuals: " + problems);
        helper.succeed();
    }

    // ==================== 6. the FluidType identity ====================

    /**
     * The three world waters must report {@code ForgeMod.WATER_TYPE}, because Forge identifies "the
     * entity is in water" by the {@code FluidType} instance and not by {@code #minecraft:water}:
     * {@code Entity.updateFluidHeightAndDoFluidPushing(TagKey, double)} returns
     * {@code isInFluidType(ForgeMod.WATER_TYPE.get())} for the water tag (Entity.java:3035-3040),
     * that per-type height map is keyed by {@code fluidstate.getFluidType()} (Entity.java:3078,3127),
     * {@code IForgeEntity.isInFluidType(FluidType)} compares the instance
     * (IForgeEntity.java:267-270) and {@code Entity.isEyeInFluid(FluidTags.WATER)} is
     * {@code isEyeInFluidType(ForgeMod.WATER_TYPE)} with an identity comparison
     * (Entity.java:1320-1325, IForgeEntity.java:316-319). {@code updateInWaterStateAndDoWaterCurrentPushing}
     * turns that answer into {@code wasTouchingWater} and into {@code doWaterSplashEffect}
     * (Entity.java:1208-1233, 1256-1282), so this single fact is what produces vanilla's splash and
     * enter-water sound, the swimming animation, drowning, the FOV change, the bubble HUD, the
     * underwater screen overlay and the water fog.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void worldWatersReportVanillaWaterFluidType(GameTestHelper helper) {
        FluidType vanilla = ForgeMod.WATER_TYPE.get();
        helper.assertTrue(vanilla != null, "ForgeMod.WATER_TYPE is registered");

        List<String> problems = new ArrayList<>();
        for (String path : allIds()) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(id(path));
            if (fluid == null) {
                problems.add(path + " is not registered");
                continue;
            }
            if (fluid.getFluidType() != vanilla) {
                problems.add(path + " reports " + ForgeRegistries.FLUID_TYPES.get().getKey(fluid.getFluidType()));
            }
            // FluidState.getFluidType() delegates to the fluid (IForgeFluidState.java:49-52) and is
            // what Entity reads while the entity is inside the fluid.
            if (fluid.defaultFluidState().getFluidType() != vanilla) {
                problems.add(path + " FluidState.getFluidType() is not vanilla water's");
            }
            // The engine sees vanilla water, but the port must still find its own GT6 entry for the
            // fluid (name, tooltip, flags, tank metadata) - GTFluids.entryForFluid is keyed by the
            // fluid, not by the type, exactly so this keeps working.
            if (GTFluids.entryForFluid(fluid) == null) {
                problems.add(path + " has no GT6 fluid entry any more");
            }
        }
        helper.assertTrue(problems.isEmpty(), "world waters report vanilla water's FluidType: " + problems);

        // The same through the world block, which is what an entity standing in the water reads.
        for (String path : WORLD_WATERS) {
            Block block = ForgeRegistries.BLOCKS.getValue(id(path));
            helper.assertTrue(block != null && block != Blocks.AIR, path + " block is not registered");
            if (block == null) continue;
            FluidState source = block.defaultBlockState().getFluidState();
            helper.assertTrue(source.getFluidType() == vanilla,
                    path + " world block's fluid state does not report vanilla water's FluidType");
            helper.assertTrue(source.getAmount() == 8, path + " world block is not a full source");
        }

        // A GT6 water next to lava now behaves like vanilla water next to lava: Forge registers that
        // interaction on the lava type and tests the neighbour's type against ForgeMod.WATER_TYPE
        // (FluidInteractionRegistry.java:79-92,133-136). The port registers no interaction of its
        // own, so canInteract finds none for a GT6 water source and GTWorldFluidBlock keeps
        // scheduling the fluid tick exactly like vanilla water's block does
        // (FluidInteractionRegistry.canInteract, FluidInteractionRegistry.java:59-77).
        Block waterBlock = ForgeRegistries.BLOCKS.getValue(id(GTWaterParity.SEAWATER));
        BlockPos scratch = new BlockPos(LOGGED_X, BASE_Y, BASE_Z + 8);
        helper.getLevel().setBlock(scratch, waterBlock.defaultBlockState(), 3);
        helper.assertTrue(!net.minecraftforge.fluids.FluidInteractionRegistry.canInteract(helper.getLevel(), scratch),
                "a GT6 water source has no fluid interaction of its own, so it still flows");
        helper.getLevel().setBlock(scratch, Blocks.AIR.defaultBlockState(), 2);
        helper.succeed();
    }

    // ==================== 7. the render layer ====================

    /**
     * The chunk mesher picks a fluid's layer with
     * {@code ItemBlockRenderTypes.getRenderLayer(FluidState)} (ChunkRenderDispatcher.java:620), and
     * vanilla's {@code ItemBlockRenderTypes.FLUID_RENDER_TYPES} contains only
     * {@code Fluids.WATER} / {@code Fluids.FLOWING_WATER} -> {@code RenderType.translucent()}
     * (ItemBlockRenderTypes.java:315-319) with {@code RenderType.solid()} as the default for every
     * other fluid (line 400). {@code RenderType} and {@code ItemBlockRenderTypes} are
     * {@code @OnlyIn(Dist.CLIENT)} and this GameTest runs on the dedicated GameTest server, so the
     * layer is asserted through {@link FluidRenderLayers}, the common policy the client registration
     * in {@code GregTechClient.clientSetup} maps one to one onto
     * {@code ItemBlockRenderTypes.setRenderLayer(fluid, RenderType.translucent())}.
     *
     * <p>The assertion walks {@link FluidRenderLayers#worldWaterLayers()} - the very map the client
     * iterates - so the ids the client registers and the ids checked here cannot drift apart. All six
     * matter: the flowing variants are registered fluids of their own
     * ({@code gregtech:seawater_flowing} and friends) and are what every flowing block of a GT6 water
     * body carries, so a still-only list left half of the water on the opaque solid layer.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void renderLayerMatchesVanillaWater(GameTestHelper helper) {
        helper.assertTrue(FluidRenderLayers.vanillaWater().equals("translucent"),
                "vanilla water's layer is RenderType.translucent() (ItemBlockRenderTypes.java:315-319), the"
                        + " port names it " + FluidRenderLayers.vanillaWater());

        Map<String, String> registrations = FluidRenderLayers.worldWaterLayers();
        helper.assertTrue(new TreeSet<>(registrations.keySet()).equals(new TreeSet<>(allIds())),
                "the client registers a layer for exactly the six world-water ids, still and flowing; got "
                        + new TreeSet<>(registrations.keySet()));

        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, String> registration : registrations.entrySet()) {
            String path = registration.getKey();
            String layer = registration.getValue();
            if (!layer.equals(FluidRenderLayers.vanillaWater())) {
                problems.add(path + " is drawn in the " + layer + " layer");
            }
            if (!FluidRenderLayers.isTranslucent(layer)) {
                problems.add(path + " would be registered as " + layer);
            }
            if (!FluidRenderLayers.select(path).equals(FluidRenderLayers.vanillaWater())) {
                problems.add(path + " selects " + FluidRenderLayers.select(path) + " on its own");
            }
            // The client skips an id whose fluid is not registered, so all six have to resolve here
            // for the registration to be complete.
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(id(path));
            if (fluid == null || fluid == Fluids.EMPTY) {
                problems.add(path + " is not registered, so there is nothing to register a layer for");
            }
        }
        helper.assertTrue(problems.isEmpty(), "world water render layers: " + problems);

        // Only the six world waters are touched; no other fluid of the port changes layer.
        helper.assertTrue(FluidRenderLayers.select(GTWaterParity.SEAWATER + "_flowing")
                        .equals(FluidRenderLayers.vanillaWater()),
                "a flowing world water resolves to the same layer as its still fluid");
        helper.assertTrue(FluidRenderLayers.select("watergeothermal").equals(FluidRenderLayers.TRANSLUCENT)
                        && FluidRenderLayers.select("watergeothermal_flowing").equals(FluidRenderLayers.TRANSLUCENT),
                "GT6 geothermal springs use their original translucent world render pass");
        helper.assertTrue(FluidRenderLayers.select("nitrogen").equals(FluidRenderLayers.SOLID),
                "a fluid without a world block keeps the default layer");
        var placedFluids = FluidRenderLayers.worldFluidLayers();
        helper.assertTrue(placedFluids.size() == 18,
                "all nine world fluids register both still and flowing translucent layers");
        for (var registration : placedFluids.entrySet()) {
            Fluid placed = ForgeRegistries.FLUIDS.getValue(id(registration.getKey()));
            helper.assertTrue(placed != null && placed != Fluids.EMPTY
                            && FluidRenderLayers.TRANSLUCENT.equals(registration.getValue()),
                    "placed fluid " + registration.getKey() + " has a registered translucent layer");
        }

        // The sprites and the overlay the renderer resolves for the world waters are vanilla's own:
        // ForgeHooksClient.getFluidSprites asks IClientFluidTypeExtensions for them
        // (ForgeHooksClient.java:487-496) and LiquidBlockRenderer renders the camera overlay from
        // getRenderOverlayTexture. Because the fluids report ForgeMod.WATER_TYPE
        // (worldWatersReportVanillaWaterFluidType) the extensions Forge returns for them are vanilla
        // water's own instance; the port's copy of the same values is pinned here.
        helper.assertTrue(GTWaterParity.VANILLA_WATER_STILL_TEXTURE.toString().equals("minecraft:block/water_still"),
                "still sprite is " + GTWaterParity.VANILLA_WATER_STILL_TEXTURE);
        helper.assertTrue(GTWaterParity.VANILLA_WATER_FLOWING_TEXTURE.toString().equals("minecraft:block/water_flow"),
                "flowing sprite is " + GTWaterParity.VANILLA_WATER_FLOWING_TEXTURE);
        helper.assertTrue(GTWaterParity.VANILLA_WATER_OVERLAY_TEXTURE.toString().equals("minecraft:block/water_overlay"),
                "block overlay sprite is " + GTWaterParity.VANILLA_WATER_OVERLAY_TEXTURE);
        helper.assertTrue(GTWaterParity.VANILLA_UNDERWATER_OVERLAY_TEXTURE.toString()
                        .equals("minecraft:textures/misc/underwater.png"),
                "camera overlay is " + GTWaterParity.VANILLA_UNDERWATER_OVERLAY_TEXTURE);
        helper.succeed();
    }

    // ==================== 8. waterlogged blocks ====================

    /** Clear of the spreading test area ({@link #BASE_X} +- {@link #AREA_RADIUS}). */
    private static final int LOGGED_X = 52016;

    /**
     * Vanilla's waterlogged-block contract is an instance comparison, not a tag test:
     * {@code SimpleWaterloggedBlock.canPlaceLiquid} is {@code fluid == Fluids.WATER}
     * (SimpleWaterloggedBlock.java:17-19) and {@code placeLiquid} needs
     * {@code fluidState.getType() == Fluids.WATER} (line 21-22) - never the <em>flowing</em> fluid.
     * Which one arrives is decided by {@code FlowingFluid.getSpread}/ {@code spreadToSides}: the
     * fluid handed to {@code canSpreadTo} is the new-liquid type of the target position
     * ({@code FlowingFluid.java:140-147}), and {@code getNewLiquid} only returns the source fluid
     * when the target has <b>two source neighbours and a solid block below it</b>
     * ({@code FlowingFluid.java:170-176}); with one neighbour it is the flowing fluid and nothing
     * happens. That is why the harness below flanks the block with two sources - a single source
     * only makes flowing water, which vanilla water itself never waterlogs anything with.
     *
     * <p>The GT6 waters answer that through {@code GTWorldWaterFluid.getNewLiquid} +
     * {@code VanillaWaterlogging}: for a {@code SimpleWaterloggedBlock} whose computed liquid is the
     * GT6 source, the spread pipeline is handed vanilla water's own source state - which is what
     * {@code getSpread} (private {@code canPassThrough}/{@code canHoldFluid}), {@code canSpreadTo}
     * and {@code spreadTo} all compare - and every other target keeps the GT6 fluid. The stairs'
     * fluid state therefore carries vanilla water internally (as it does in vanilla) while the GT6
     * water around it is untouched.
     *
     * <p>Every case is calibrated against vanilla water in the same geometry first, so "identical to
     * vanilla water" means the resulting {@link BlockState} is literally equal.
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void waterloggedBlocksMatchVanillaWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        prepareArea(level, LOGGED_X, BASE_Z, BASE_Y);

        Block waterBlock = ForgeRegistries.BLOCKS.getValue(id(GTWaterParity.SEAWATER));
        helper.assertTrue(waterBlock != null && waterBlock != Blocks.AIR, "the seawater block exists");
        if (waterBlock == null) return;

        BlockState dryStairs = Blocks.STONE_BRICK_STAIRS.defaultBlockState();
        helper.assertTrue(!dryStairs.getValue(BlockStateProperties.WATERLOGGED), "the vanilla stairs start dry");

        // (a) The calibration: vanilla water coming from the two source neighbours flanking the
        // stairs waterlogs it. One source would only make flowing water, which vanilla water never
        // waterlogs anything with - see the javadoc.
        BlockState vanillaResult = waterlogLane(level, dryStairs, Blocks.WATER, true);
        helper.assertTrue(vanillaResult.is(Blocks.STONE_BRICK_STAIRS)
                        && vanillaResult.getValue(BlockStateProperties.WATERLOGGED),
                "calibration: vanilla water from two source neighbours waterlogs the stairs between them, got "
                        + vanillaResult + " with fluid " + vanillaResult.getFluidState());

        // (b) A GT6 water waterlogs it to exactly the same state.
        BlockState gtResult = waterlogLane(level, dryStairs, waterBlock, true);
        helper.assertTrue(gtResult.equals(vanillaResult),
                "a GT6 water waterlogs the stairs exactly like vanilla water (" + gtResult + " vs " + vanillaResult + ")");
        helper.assertTrue(gtResult.getValue(BlockStateProperties.WATERLOGGED)
                        && gtResult.getFluidState().is(FluidTags.WATER),
                "the waterlogged stairs carry a water-tagged fluid state: " + gtResult.getFluidState());

        // (c) And no more than vanilla does: with a single source the new liquid at the stairs is the
        // *flowing* fluid, which canPlaceLiquid rejects, so a GT6 water has to leave the stairs dry
        // there too - the mirror image of the calibration.
        BlockState vanillaDry = waterlogLane(level, dryStairs, Blocks.WATER, false);
        helper.assertTrue(!vanillaDry.getValue(BlockStateProperties.WATERLOGGED),
                "calibration: one source only makes flowing water, which does not waterlog the stairs, got " + vanillaDry);
        BlockState gtDry = waterlogLane(level, dryStairs, waterBlock, false);
        helper.assertTrue(gtDry.equals(vanillaDry),
                "a single GT6 water source leaves the stairs as vanilla water does (" + gtDry + " vs " + vanillaDry + ")");

        // (d) The waterlogged stairs keep their state through a GT6 water neighbour update and tick.
        level.setBlock(TARGET, gtResult, 2);
        for (BlockPos s : List.of(WEST, EAST)) {
            level.setBlock(s, waterBlock.defaultBlockState(), 3);
        }
        level.getFluidState(WEST).tick(level, WEST);
        level.setBlock(TARGET.above(), Blocks.STONE.defaultBlockState(), 3);
        BlockState kept = level.getBlockState(TARGET);
        helper.assertTrue(kept.equals(gtResult), "the waterlogged stairs survive a GT6 water neighbour update: " + kept);
        helper.assertTrue(kept.getFluidState().is(FluidTags.WATER),
                "and still report a water-tagged fluid: " + kept.getFluidState());
        helper.assertTrue(level.getBlockState(WEST).is(waterBlock),
                "the GT6 water source next to it is untouched");
        level.setBlock(TARGET.above(), Blocks.AIR.defaultBlockState(), 2);
        clearLane(level);

        // (e) Vanilla's own ocean plants: seagrass and kelp refuse every fluid
        // (SeagrassBlock.canPlaceLiquid/KelpBlock.canPlaceLiquid return false, SeagrassBlock.java:77-83,
        // KelpBlock.java:38-44), so a GT6 water tick next to them must not replace them - and they
        // report vanilla water's source fluid, exactly as they do in vanilla
        // (SeagrassBlock.java:62-64, KelpBlock.java:56-58).
        for (Block plant : List.of(Blocks.SEAGRASS, Blocks.KELP)) {
            helper.assertTrue(plant instanceof SimpleWaterloggedBlock
                            || plant instanceof LiquidBlockContainer,
                    plant + " is a vanilla water plant");
            level.setBlock(TARGET, plant.defaultBlockState(), 2);
            level.setBlock(WEST, waterBlock.defaultBlockState(), 3);
            level.getFluidState(WEST).tick(level, WEST);
            helper.assertTrue(level.getBlockState(TARGET).is(plant),
                    plant + " survives a GT6 water tick next to it");
            helper.assertTrue(level.getBlockState(TARGET).getFluidState().is(FluidTags.WATER),
                    plant + " reports a water-tagged fluid state");
            helper.assertTrue(level.getBlockState(WEST).is(waterBlock),
                    "the GT6 water next to " + plant + " is untouched");
            // The scheduled tick vanilla's plant asks for (SeagrassBlock.updateShape, line 45-52)
            // runs the plant's own (vanilla) water next to the GT6 water; neither may destroy the
            // other, which is the "GT water flowing into / next to a waterlogged block" direction.
            level.getFluidState(TARGET).tick(level, TARGET);
            helper.assertTrue(level.getBlockState(TARGET).is(plant) && level.getBlockState(WEST).is(waterBlock),
                    "a vanilla-water tick inside " + plant + " leaves both blocks alone");
            clearLane(level);
        }

        // (f) The item path: the world block is replaceable (Blocks.WATER's own
        // .replaceable() property, Blocks.java:77) and SeagrassBlock/KelpBlock.getStateForPlacement
        // only require a water-tagged 8-amount fluid there (SeagrassBlock.java:39-43,
        // KelpBlock.java:50-54), so a seagrass or kelp item can be placed into a GT6 water column.
        level.setBlock(WEST, waterBlock.defaultBlockState(), 3);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(WEST), Direction.UP, WEST, false);
        for (var item : List.of(Items.SEAGRASS, Items.KELP)) {
            BlockPlaceContext context = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND,
                    new ItemStack(item), hit);
            helper.assertTrue(context.canPlace(),
                    "the GT6 water block is replaceable, so " + item + " can be placed into it");
            BlockState placement = Block.byItem(item).getStateForPlacement(context);
            helper.assertTrue(placement != null,
                    "getStateForPlacement accepts a GT6 water column for " + item
                            + " (fluid " + level.getFluidState(WEST) + ")");
        }
        helper.assertTrue(level.getFluidState(WEST).is(FluidTags.WATER)
                        && level.getFluidState(WEST).getAmount() == 8,
                "the GT6 water column is water-tagged and full, which is all the plant placement asks for");

        clearLane(level);
        clearArea(level, LOGGED_X, BASE_Z, BASE_Y);
        helper.succeed();
    }

    /**
     * Placement (not ticking) of a waterloggable block into GT6 water.
     *
     * <p>Vanilla's placement test is {@code fluid.getType() == Fluids.WATER}
     * ({@code SlabBlock.getStateForPlacement}, SlabBlock.java:70-77, feeding
     * {@code SimpleWaterloggedBlock.getStateForPlacement}), so a GT6 water body - which holds
     * {@code gregtech:seawater} and friends - made every GT waterloggable block come out dry.
     * {@code GTWaterloggable#getStateForPlacement} now asks the {@code #minecraft:water} fluid tag
     * instead, which contains all six world-water ids (see
     * {@link #worldWatersAreInVanillaWaterTag}).
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void placementIntoGtWaterIsWaterlogged(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(PLACEMENT_X, BASE_Y, BASE_Z);
        prepareArea(level, PLACEMENT_X, BASE_Z, BASE_Y);

        Block waterBlock = ForgeRegistries.BLOCKS.getValue(id(GTWaterParity.SEAWATER));
        helper.assertTrue(waterBlock != null && waterBlock != Blocks.AIR, "the seawater block exists");
        if (waterBlock == null) return;

        Block slab = firstGtStoneSlab();
        helper.assertTrue(slab instanceof SimpleWaterloggedBlock,
                "the port registers a waterloggable GT stone slab, got " + slab);
        if (!(slab instanceof SimpleWaterloggedBlock)) return;

        // (a) The predicate the placement path uses: vanilla water and each GT6 world water are
        // accepted, lava (and anything else untagged) is not.
        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
        helper.assertTrue(GTWaterloggable.isWaterForLogging(level.getFluidState(pos)),
                "vanilla water may waterlog a block");
        level.setBlock(pos, waterBlock.defaultBlockState(), 2);
        helper.assertTrue(GTWaterloggable.isWaterForLogging(level.getFluidState(pos)),
                "GT6 sea water may waterlog a block (" + level.getFluidState(pos) + ")");
        level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2);
        helper.assertTrue(!GTWaterloggable.isWaterForLogging(level.getFluidState(pos)),
                "lava may not waterlog a block (" + level.getFluidState(pos) + ")");

        // (b) The end-to-end placement: the real GT block's own getStateForPlacement, clicked into a
        // GT6 water column, comes out waterlogged. A player is required: vanilla
        // BlockPlaceContext#getNearestLookingDirection reads the player's view rotation.
        level.setBlock(pos, waterBlock.defaultBlockState(), 2);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        net.minecraft.world.entity.player.Player placer = helper.makeMockSurvivalPlayer();
        BlockPlaceContext inWater = new BlockPlaceContext(level, placer, InteractionHand.MAIN_HAND,
                new ItemStack(slab.asItem()), hit);
        helper.assertTrue(inWater.canPlace(), "a GT6 water block is replaceable, so the slab may be placed into it");
        BlockState placed = slab.getStateForPlacement(inWater);
        helper.assertTrue(placed != null && placed.getValue(BlockStateProperties.WATERLOGGED),
                "a GT stone slab placed into GT6 sea water is waterlogged, got " + placed
                        + " with fluid " + level.getFluidState(pos));

        // (c) The contrast, and the reason the helper exists: a *vanilla* waterloggable block placed
        // into the same GT6 water still comes out dry. This is vanilla's own hard-coded
        // `fluid == Fluids.WATER` test and cannot be changed without a mixin, so it is pinned here
        // as a known difference rather than papered over.
        BlockState vanillaPlaced = Blocks.OAK_SLAB.getStateForPlacement(inWater);
        helper.assertTrue(vanillaPlaced != null && !vanillaPlaced.getValue(BlockStateProperties.WATERLOGGED),
                "vanilla blocks placed into GT6 water stay dry (vanilla hard-codes its own water fluid), got "
                        + vanillaPlaced);

        // (d) And into air the same slab is dry, so (b) is really reading the fluid.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        BlockState inAir = slab.getStateForPlacement(new BlockPlaceContext(level, placer,
                InteractionHand.MAIN_HAND, new ItemStack(slab.asItem()), hit));
        helper.assertTrue(inAir != null && !inAir.getValue(BlockStateProperties.WATERLOGGED),
                "the same slab placed into air is dry, got " + inAir);

        clearArea(level, PLACEMENT_X, BASE_Z, BASE_Y);
        helper.succeed();
    }

    /** Clear of the waterlogging lane at {@link #LOGGED_X} (its area covers +-6 around it). */
    private static final int PLACEMENT_X = 52030;

    /** The first GT stone slab the port actually registered, or null when it registered none. */
    private static Block firstGtStoneSlab() {
        for (StoneType type : StoneType.values()) {
            for (StoneVariant variant : List.of(StoneVariant.STONE, StoneVariant.BRICKS, StoneVariant.SMOOTH)) {
                Block slab = GTBlocks.getStoneSlab(type, variant);
                if (slab != null) {
                    return slab;
                }
            }
        }
        return null;
    }

    /** Western source of the three-block waterlogging lane, and the GT6 water column used below. */
    private static final BlockPos WEST = new BlockPos(LOGGED_X, BASE_Y, BASE_Z);
    /** The stairs between the two sources of the lane. */
    private static final BlockPos TARGET = WEST.east();
    /** Eastern source of the lane. */
    private static final BlockPos EAST = TARGET.east();

    /**
     * Runs the three-block waterlogging lane: {@code dryTarget} (the stairs) at {@link #TARGET}, a
     * source of {@code fluidBlock} at {@link #WEST} and - when {@code twoSources} - one at
     * {@link #EAST} as well, then one fluid tick of the western source, and returns the state the
     * stairs ends up in.
     *
     * <p>Two sources are what makes the stairs' new liquid a <em>source</em> state, which is the one
     * case vanilla water waterlogs anything in ({@code FlowingFluid.getNewLiquid},
     * FlowingFluid.java:170-176, feeding {@code SimpleWaterloggedBlock.canPlaceLiquid},
     * SimpleWaterloggedBlock.java:17-19); with one source it is the flowing fluid and the stairs
     * stays dry.
     */
    private static BlockState waterlogLane(ServerLevel level, BlockState dryTarget, Block fluidBlock,
                                           boolean twoSources) {
        level.setBlock(TARGET, dryTarget, 2);
        level.setBlock(WEST, fluidBlock.defaultBlockState(), 3);
        level.setBlock(EAST, twoSources ? fluidBlock.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
        level.getFluidState(WEST).tick(level, WEST);
        BlockState result = level.getBlockState(TARGET);
        clearLane(level);
        return result;
    }

    /** Clears the waterlogging lane back to air. */
    private static void clearLane(ServerLevel level) {
        for (BlockPos pos : List.of(WEST, TARGET, EAST)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    // ==================== 9. the water-body pass and plants ====================

    /**
     * {@code GTWaterBodyFeature.convertColumn} only replaces blocks that are vanilla water (or, for
     * the swamp pass, GT6's own water blocks) and breaks on the first opaque block, so a seagrass or
     * kelp inside a GT6 ocean is skipped and survives with its vanilla water inside it - exactly the
     * state vanilla's own ocean decoration leaves it in.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void waterBodyPassKeepsPlantsInGtWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        BlockState sea = blockState(GTWaterParity.SEAWATER);
        helper.assertTrue(sea != null, "the seawater block exists");
        if (sea == null) return;

        int z = BASE_Z;
        List<String> problems = new ArrayList<>();
        int index = 0;
        for (Block plant : List.of(Blocks.SEAGRASS, Blocks.KELP)) {
            int x = LOGGED_X + 10 + index * 3;
            index++;
            // sea floor at y-1, the plant at y, vanilla water above it and GT6 water on top of that.
            level.setBlock(new BlockPos(x, BASE_Y - 1, z), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(new BlockPos(x, BASE_Y, z), plant.defaultBlockState(), 2);
            level.setBlock(new BlockPos(x, BASE_Y + 1, z), Blocks.WATER.defaultBlockState(), 2);
            level.setBlock(new BlockPos(x, BASE_Y + 2, z), sea, 2);

            GTWaterBodyFeature.convertColumn(gen, x, z, BASE_Y + 2, sea, false);

            if (!level.getBlockState(new BlockPos(x, BASE_Y, z)).is(plant)) {
                problems.add(plant + " was destroyed by the water-body pass");
            }
            if (!level.getBlockState(new BlockPos(x, BASE_Y, z)).getFluidState().is(FluidTags.WATER)) {
                problems.add(plant + " lost its water-tagged fluid state");
            }
            if (!level.getBlockState(new BlockPos(x, BASE_Y + 1, z)).is(sea.getBlock())) {
                problems.add("the vanilla water above " + plant + " was not converted to sea water");
            }
            if (!level.getBlockState(new BlockPos(x, BASE_Y + 2, z)).is(sea.getBlock())) {
                problems.add("the GT6 water above " + plant + " was replaced");
            }
            level.setBlock(new BlockPos(x, BASE_Y - 1, z), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(new BlockPos(x, BASE_Y, z), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(new BlockPos(x, BASE_Y + 1, z), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(new BlockPos(x, BASE_Y + 2, z), Blocks.AIR.defaultBlockState(), 2);
        }
        helper.assertTrue(problems.isEmpty(), "plants in GT6 water after the water-body pass: " + problems);
        helper.succeed();
    }

    /** A GT6 world water block's default state, or null when it is not registered. */
    private static BlockState blockState(String path) {
        Block block = ForgeRegistries.BLOCKS.getValue(id(path));
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }
}
