package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity;
import com.gregtech.gregtech.content.bumble.BumbleBeeGenes;
import com.gregtech.gregtech.content.bumble.BumbleBeeType;
import com.gregtech.gregtech.content.bumble.BumbleWorkplace;
import com.gregtech.gregtech.content.bumble.GTBumbleSpecies;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTToolBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6 {@code MultiItemBumbles.bumbleCanProduce} gates both kinds of bumbliary. */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BumbleWorkplaceTests {
    private static final BlockPos RANGE_SITE = new BlockPos(64000, 240, 64000);
    private static final BlockPos QUEEN_SITE = new BlockPos(64016, 240, 64000);

    private BumbleWorkplaceTests() {}

    private static Block gtBlock(String id) {
        return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", id));
    }

    private static CompoundTag allWeatherGenes() {
        CompoundTag genes = new CompoundTag();
        BumbleBeeGenes.setHumidityMin(genes, 0);
        BumbleBeeGenes.setHumidityMax(genes, 1);
        BumbleBeeGenes.setTemperatureMin(genes, -1000);
        BumbleBeeGenes.setTemperatureMax(genes, 100000);
        BumbleBeeGenes.setInsideActive(genes, true);
        BumbleBeeGenes.setOutsideActive(genes, true);
        BumbleBeeGenes.setDayActive(genes, true);
        BumbleBeeGenes.setNightActive(genes, true);
        BumbleBeeGenes.setRainproof(genes, true);
        BumbleBeeGenes.setStormproof(genes, true);
        BumbleBeeGenes.setWorkForce(genes, 10000);
        return genes;
    }

    private static BumbliaryBlockEntity readyQueen(GameTestHelper helper) {
        BumbliaryBlockEntity machine = new BumbliaryBlockEntity(QUEEN_SITE,
                GTToolBlocks.BUMBLIARY.get().defaultBlockState());
        machine.setLevel(helper.getLevel());
        ItemStack queen = BumbleBeeType.stack(GTBumbleSpecies.byId(30), BumbleBeeType.QUEEN,
                allWeatherGenes(), 1);
        machine.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_ROYAL, queen);
        CompoundTag progress = new CompoundTag();
        progress.putLong(BumbliaryBlockEntity.NBT_PROGRESS, 601);
        machine.load(progress);
        return machine;
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void registeredSpeciesUseTheirOwnWorkplace(GameTestHelper helper) {
        helper.assertTrue(BumbleWorkplace.matches(Blocks.POPPY.defaultBlockState(), 0),
                "ordinary bumbles work flowers");
        helper.assertTrue(!BumbleWorkplace.matches(Blocks.STONE.defaultBlockState(), 0),
                "ordinary bumbles reject stone");
        helper.assertTrue(BumbleWorkplace.matches(Blocks.WATER.defaultBlockState(), 1)
                        && !BumbleWorkplace.matches(Blocks.DIRT.defaultBlockState(), 1),
                "water bumbles require water");
        helper.assertTrue(BumbleWorkplace.matches(gtBlock("leaves_rainbowood").defaultBlockState(), 2),
                "magical bumbles accept GT6 rainbow leaves");
        helper.assertTrue(BumbleWorkplace.matches(Blocks.NETHER_WART.defaultBlockState(), 3)
                        && BumbleWorkplace.matches(Blocks.NETHER_WART.defaultBlockState(), 200),
                "nether and pyro bumbles need nether wart");
        helper.assertTrue(BumbleWorkplace.matches(Blocks.DRAGON_EGG.defaultBlockState(), 4)
                        && BumbleWorkplace.matches(Blocks.END_PORTAL.defaultBlockState(), 202),
                "end and aero bumbles accept GT6's portal or egg");
        helper.assertTrue(BumbleWorkplace.matches(Blocks.COBBLESTONE.defaultBlockState(), 5)
                        && BumbleWorkplace.matches(Blocks.STONE.defaultBlockState(), 203),
                "rock and tera bumbles need stone");
        helper.assertTrue(BumbleWorkplace.matches(GTBlocks.getStone(StoneType.GRANITE_BLACK,
                        StoneVariant.COBBLE_MOSSY).defaultBlockState(), 5)
                        && !BumbleWorkplace.matches(GTBlocks.getStone(StoneType.GRANITE_BLACK,
                        StoneVariant.BRICKS).defaultBlockState(), 5),
                "GT6 stone variant flags distinguish natural stone from bricks");
        helper.assertTrue(BumbleWorkplace.matches(Blocks.COCOA.defaultBlockState(), 6),
                "jungle bumbles need cocoa");
        helper.assertTrue(BumbleWorkplace.matches(Blocks.ICE.defaultBlockState(), 7)
                        && BumbleWorkplace.matches(Blocks.SNOW.defaultBlockState(), 201),
                "frost and cryo bumbles need snow or ice");
        helper.assertTrue(BumbleWorkplace.matches(Blocks.MYCELIUM.defaultBlockState(), 8),
                "mushroom bumbles need mycelium or mushrooms");
        helper.assertTrue(BumbleWorkplace.matches(Blocks.POTTED_CACTUS.defaultBlockState(), 9)
                        && BumbleWorkplace.matches(Blocks.CACTUS.defaultBlockState(), 105),
                "desert and military bumbles accept potted or unpotted cactus");
        helper.assertTrue(BumbleWorkplace.matches(Blocks.CLAY.defaultBlockState(), 100)
                        && BumbleWorkplace.matches(gtBlock("clay_red").defaultBlockState(), 100),
                "clay bumbles accept vanilla and GT6 raw clay");
        helper.assertTrue(BumbleWorkplace.matches(gtBlock("resin_hole_rubber").defaultBlockState(), 101),
                "sticky bumbles need a GT6 rubber resin hole");
        helper.assertTrue(BumbleWorkplace.matches(Blocks.SOUL_SAND.defaultBlockState(), 103),
                "soul bumbles need soul sand");
        helper.assertTrue(BumbleWorkplace.matches(Blocks.POTTED_POPPY.defaultBlockState(), 102),
                "royal bumbles can use potted flowers");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void advancedBumbliarySearchesOnlyOneBlock(GameTestHelper helper) {
        BlockPos outer = RANGE_SITE.east(2);
        BlockPos inner = RANGE_SITE.east();
        BlockPos outerSoil = outer.below();
        BlockPos innerSoil = inner.below();
        var level = helper.getLevel();
        level.setBlock(outerSoil, Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(outer, Blocks.POPPY.defaultBlockState(), 3);
        try {
            helper.assertTrue(BumbleWorkplace.hasWorkplace(level, RANGE_SITE, 30, false),
                    "standard bumbliary sees a flower two blocks away");
            helper.assertTrue(!BumbleWorkplace.hasWorkplace(level, RANGE_SITE, 30, true),
                    "advanced bumbliary must not see a flower two blocks away");
            level.setBlock(innerSoil, Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(inner, Blocks.POPPY.defaultBlockState(), 3);
            helper.assertTrue(BumbleWorkplace.hasWorkplace(level, RANGE_SITE, 30, true),
                    "advanced bumbliary sees a flower one block away");
            helper.succeed();
        } finally {
            level.setBlock(outer, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(inner, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(outerSoil, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(innerSoil, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void queenCannotProduceWithoutAWorkplace(GameTestHelper helper) {
        BlockPos flower = QUEEN_SITE.east();
        BlockPos soil = flower.below();
        var level = helper.getLevel();
        level.setBlock(flower, Blocks.AIR.defaultBlockState(), 3);
        BumbliaryBlockEntity machine = readyQueen(helper);
        helper.assertTrue(!BumbleWorkplace.hasWorkplace(level, QUEEN_SITE, 30, false),
                "the test site starts without a flower workplace");
        machine.tickLogic();
        helper.assertTrue(machine.life() == 600 && machine.inventory().getStackInSlot(0).isEmpty(),
                "a queen lives but makes no comb without her workplace");

        level.setBlock(soil, Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(flower, Blocks.POPPY.defaultBlockState(), 3);
        try {
            CompoundTag progress = new CompoundTag();
            progress.putLong(BumbliaryBlockEntity.NBT_PROGRESS, 601);
            machine.load(progress);
            machine.tickLogic();
            helper.assertTrue(machine.life() == 600 && !machine.inventory().getStackInSlot(0).isEmpty(),
                    "the same queen produces a comb once a flower is placed nearby");
            helper.succeed();
        } finally {
            level.setBlock(flower, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(soil, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
