package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.misc.DiggableBlock;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6 BlockDiggable metadata 0–6: drop identity, gravity, walk speed and plant support. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DiggableBlockTests {
    private static final String[] IDS = {
            "mud", "clay_brown", "turf", "clay_red", "clay_yellow", "clay_blue", "clay_white",
            "diggable_clay", "diggable_peat"
    };
    private static final String[] DROP_IDS = {
            "mud_2", "brown_clay", "ingot_peat", "red_clay", "yellow_clay", "blue_clay", "white_clay",
            "brown_clay", "ingot_peat"
    };
    private static final DiggableBlock.Variant[] VARIANTS = {
            DiggableBlock.Variant.MUD, DiggableBlock.Variant.BROWN_CLAY, DiggableBlock.Variant.TURF,
            DiggableBlock.Variant.RED_CLAY, DiggableBlock.Variant.YELLOW_CLAY,
            DiggableBlock.Variant.BLUE_CLAY, DiggableBlock.Variant.WHITE_CLAY,
            DiggableBlock.Variant.BROWN_CLAY, DiggableBlock.Variant.TURF
    };

    private static DiggableBlock block(String id) {
        Block found = ForgeRegistries.BLOCKS.getValue(GregTech.id(id));
        return found instanceof DiggableBlock diggable ? diggable : null;
    }

    @GameTest(template = "test_empty")
    public static void allSevenVariantsAndLegacyIdsDropFourMatchingItems(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        for (int i = 0; i < IDS.length; i++) {
            DiggableBlock block = block(IDS[i]);
            helper.assertTrue(block != null && block.variant() == VARIANTS[i],
                    IDS[i] + " has its GT6 metadata behavior");
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            var state = level.getBlockState(pos);
            helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_SHOVEL),
                    IDS[i] + " is mined with a shovel");
            helper.assertTrue(state.getDestroySpeed(level, pos) == 0.5F,
                    IDS[i] + " has dirt's 0.5 hardness");
            var drops = Block.getDrops(state, level, pos, null);
            helper.assertTrue(drops.size() == 1 && drops.get(0).getCount() == 4,
                    IDS[i] + " drops one stack of four, independent of Fortune");
            ItemStack expected = DROP_IDS[i].equals("ingot_peat")
                    ? GTItems.getStack(MaterialPrefix.ingot, Materials.Peat, 4)
                    : new ItemStack(ForgeRegistries.ITEMS.getValue(GregTech.id(DROP_IDS[i])), 4);
            helper.assertTrue(!expected.isEmpty() && ItemStack.isSameItemSameTags(drops.get(0), expected),
                    IDS[i] + " drops " + DROP_IDS[i] + " rather than itself or generic clay");
        }
        helper.assertTrue(DiggableBlock.Variant.BROWN_CLAY.material() == Materials.ClayBrown
                        && DiggableBlock.Variant.TURF.material() == Materials.Peat
                        && DiggableBlock.Variant.RED_CLAY.material() == Materials.ClayRed
                        && DiggableBlock.Variant.YELLOW_CLAY.material() == Materials.Bentonite
                        && DiggableBlock.Variant.BLUE_CLAY.material() == Materials.Palygorskite
                        && DiggableBlock.Variant.WHITE_CLAY.material() == Materials.Kaolinite,
                "all diggable metadata values keep their GT6 material composition");
        level.removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void mudAndTurfFallSlowAndGrowPlantsButClayDoesNot(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
        DiggableBlock mud = block("mud");
        DiggableBlock turf = block("turf");
        DiggableBlock clay = block("clay_brown");
        helper.assertTrue(mud != null && turf != null && clay != null,
                "all test variants are registered as GT6 diggable blocks");
        for (DiggableBlock soil : new DiggableBlock[]{mud, turf}) {
            var state = soil.defaultBlockState();
            for (Block plant : new Block[]{Blocks.OAK_SAPLING, Blocks.CACTUS, Blocks.SUGAR_CANE,
                    Blocks.WHEAT, Blocks.LILY_PAD}) {
                helper.assertTrue(state.canSustainPlant(level, pos, Direction.UP, (IPlantable) plant),
                        soil.variant() + " supports " + plant);
            }
            helper.assertTrue(state.canSustainPlant(level, pos, Direction.UP, (IPlantable) Blocks.NETHER_WART),
                    soil.variant() + " accepts bush plants, as GT6 does even for Nether Wart");
        }
        var clayState = clay.defaultBlockState();
        helper.assertTrue(!clayState.canSustainPlant(level, pos, Direction.UP, (IPlantable) Blocks.OAK_SAPLING)
                        && !clayState.canSustainPlant(level, pos, Direction.UP, (IPlantable) Blocks.CACTUS),
                "GT6 clay metadata cannot sustain plants");

        var cow = helper.spawn(EntityType.COW, new BlockPos(2, 4, 2));
        cow.setDeltaMovement(new Vec3(1, 0, 1));
        mud.stepOn(level, pos, mud.defaultBlockState(), cow);
        helper.assertTrue(cow.getDeltaMovement().x == 0.5 && cow.getDeltaMovement().z == 0.5,
                "mud halves a living creature's horizontal walk speed");
        cow.setDeltaMovement(new Vec3(1, 0, 1));
        turf.stepOn(level, pos, turf.defaultBlockState(), cow);
        helper.assertTrue(cow.getDeltaMovement().x == 0.5 && cow.getDeltaMovement().z == 0.5,
                "turf halves a living creature's horizontal walk speed");
        cow.setDeltaMovement(new Vec3(1, 0, 1));
        clay.stepOn(level, pos, clayState, cow);
        helper.assertTrue(cow.getDeltaMovement().x == 1 && cow.getDeltaMovement().z == 1,
                "clay does not slow walking");
        cow.discard();

        level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, clayState);
        clay.tick(clayState, level, pos, level.random);
        helper.assertTrue(level.getBlockState(pos).is(clay), "clay remains suspended over air");
        level.setBlockAndUpdate(pos, mud.defaultBlockState());
        mud.tick(mud.defaultBlockState(), level, pos, level.random);
        helper.assertTrue(level.getBlockState(pos).isAir()
                        && !level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(pos).inflate(2)).isEmpty(),
                "mud uses GT6 gravity and becomes a falling block entity");
        helper.succeed();
    }
}
