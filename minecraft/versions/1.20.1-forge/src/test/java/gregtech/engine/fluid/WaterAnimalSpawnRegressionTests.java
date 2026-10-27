package gregtech.engine.fluid;

import gregtech.content.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class WaterAnimalSpawnRegressionTests {
    private WaterAnimalSpawnRegressionTests() {}

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void waterTagsAndVanillaSpawnPredicatesAgree(GameTestHelper helper) {
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(4, 3, 4));
        var pos = new BlockPos(anchor.getX(), level.getSeaLevel() - 5, anchor.getZ());
        var deep = pos.atY(level.getSeaLevel() - 34);
        var random = RandomSource.create(0);
        for (String field : new String[]{"Water", "Ocean", "River_Water", "Swampwater"}) {
            var fluid = field.equals("Water") ? Fluids.WATER : GTFluids.still(field).get();
            for (int amount : new int[]{0, 1}) {
                var water = fluid.defaultFluidState().createLegacyBlock().setValue(LiquidBlock.LEVEL, amount);
                helper.assertTrue(water.getFluidState().is(FluidTags.WATER), field + " must carry minecraft:water");
                for (var p : new BlockPos[]{pos.below(), pos, pos.above(), deep}) level.setBlock(p, water, 2);
                for (var type : java.util.List.of(EntityType.COD, EntityType.SALMON, EntityType.PUFFERFISH,
                        EntityType.DOLPHIN, EntityType.SQUID, EntityType.TROPICAL_FISH)) {
                    helper.assertTrue(SpawnPlacements.Type.IN_WATER.canSpawnAt(level, pos, type), field + " water placement: " + type);
                    helper.assertTrue(SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos, random),
                            field + " vanilla spawn predicate: " + type);
                }
                helper.assertTrue(!GlowSquid.checkGlowSquideSpawnRules(EntityType.GLOW_SQUID, level,
                        MobSpawnType.NATURAL, pos, random), "Glow squid must retain their depth restriction");
                helper.assertTrue(!WaterAnimal.checkSurfaceWaterAnimalSpawnRules(EntityType.COD, level,
                        MobSpawnType.NATURAL, deep, random), "Surface animals must retain their height restriction");
            }
        }
        var waterlogged = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlock(pos.above(), waterlogged, 2);
        helper.assertTrue(!WaterAnimal.checkSurfaceWaterAnimalSpawnRules(EntityType.COD, level,
                MobSpawnType.NATURAL, pos, random), "Waterlogged slabs are not open water above a fish");
        helper.assertTrue(!TropicalFish.checkTropicalFishSpawnRules(EntityType.TROPICAL_FISH, level,
                MobSpawnType.NATURAL, pos, random), "Tropical fish must also reject a waterlogged slab");
        helper.assertTrue(!WaterFamilyIdentity.matchesWaterBlock(Blocks.LAVA.defaultBlockState(), Blocks.WATER),
                "Lava must not become water");
        helper.assertTrue(!WaterFamilyIdentity.matchesWaterBlock(GTFluids.still("Oil_Medium").get()
                .defaultFluidState().createLegacyBlock(), Blocks.WATER), "Oil must not become water");
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void glowSquidUseTaggedWaterAndStillRequireDarkness(GameTestHelper helper) {
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(4, 3, 4));
        var pos = anchor.atY(level.getSeaLevel() - 34);
        // A small sealed cell makes this independent of the test world's terrain and time of day.
        for (var p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2)))
            level.setBlock(p, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(level.getRawBrightness(pos, 0) == 0, "The test cell must be dark");
            for (String field : new String[]{"Water", "Ocean", "River_Water", "Swampwater"}) {
                var fluid = field.equals("Water") ? Fluids.WATER : GTFluids.still(field).get();
                level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), 2);
                helper.assertTrue(GlowSquid.checkGlowSquideSpawnRules(EntityType.GLOW_SQUID, level,
                        MobSpawnType.NATURAL, pos, RandomSource.create(0)), field + " glow squid in dark deep water");
            }
            level.setBlock(pos.above(), Blocks.GLOWSTONE.defaultBlockState(), 3);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(level.getRawBrightness(pos, 0) > 0, "The lit cell must have block light");
                helper.assertTrue(!GlowSquid.checkGlowSquideSpawnRules(EntityType.GLOW_SQUID, level,
                        MobSpawnType.NATURAL, pos, RandomSource.create(0)), "Glow squid still require darkness");
                helper.succeed();
            });
        });
    }
}
