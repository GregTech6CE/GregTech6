package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.cover.CoverUtilityBehaviors;
import com.gregtech.gregtech.content.logistics.FilterRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

/** Replays transfer failures and exercises filter/stack ownership at real inventory boundaries. */
@GameTestHolder("gt6ce_forge_transfer_regressions")
@PrefixGameTestTemplate(false)
public final class ForgeTransferRegressionTests {
    @GameTest(template = "test_blueprint_empty", timeoutTicks = 400)
    public static void longDistanceItemPipelineDisconnectsImmediately(GameTestHelper helper) {
        BlueprintRegressionTests.longDistanceItemPipelineDisconnectsImmediately(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 400)
    public static void extenderResolvesReplacedInventory(GameTestHelper helper) {
        BlueprintRegressionTests.extenderResolvesReplacedInventory(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 400)
    public static void legacyPipeAliasRetainsSourceFluidIdentity(GameTestHelper helper) {
        LongDistPipelineTypeTests.legacyPipeAliasRetainsSourceFluidIdentity(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 400)
    public static void itemLineUsesItemPipeAndRejectsFluidPipe(GameTestHelper helper) {
        LongDistPipelineTypeTests.itemLineUsesItemPipeAndRejectsFluidPipe(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 400)
    public static void fluidLineUsesFluidPipeAndRejectsItemPipe(GameTestHelper helper) {
        LongDistPipelineTypeTests.fluidLineUsesFluidPipeAndRejectsItemPipe(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void miniNetherPortalRelaysOppositeNeighbour(GameTestHelper helper) {
        DungeonKeysAndPortalsTests.miniNetherPortalRelaysOppositeNeighbour(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void miniEndPortalRelaysOppositeNeighbour(GameTestHelper helper) {
        DungeonKeysAndPortalsTests.miniEndPortalRelaysOppositeNeighbour(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void retrieverPullsFilteredItemsIntoTheMachine(GameTestHelper helper) {
        CoverBehaviorTests.retrieverPullsFilteredItemsIntoTheMachine(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void retrieverBlacklistModeKeepsTheFilteredItemOut(GameTestHelper helper) {
        CoverBehaviorTests.retrieverBlacklistModeKeepsTheFilteredItemOut(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 400)
    public static void sourceItemRoutingAndCapabilitySeparation(GameTestHelper helper) {
        SourceExtenderTests.sourceItemRoutingAndCapabilitySeparation(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 400)
    public static void sourceFluidRoutingAndLiveReplacement(GameTestHelper helper) {
        SourceExtenderTests.sourceFluidRoutingAndLiveReplacement(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 400)
    public static void sourceRelayCyclesTerminate(GameTestHelper helper) {
        SourceExtenderTests.sourceRelayCyclesTerminate(helper);
    }

    private record Barrels(BlockPos targetPos, BarrelBlockEntity source, BarrelBlockEntity target,
                           IItemHandler receiver) {
        void clear() {
            source.clearContent();
            target.clearContent();
        }
    }

    private static Barrels barrels(GameTestHelper helper) {
        var pos = new BlockPos(3, 3, 3);
        helper.setBlock(pos, Blocks.BARREL);
        helper.setBlock(pos.east(), Blocks.BARREL);
        var target = (BarrelBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var source = (BarrelBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos.east()));
        var receiver = target.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.EAST).orElseThrow(IllegalStateException::new);
        return new Barrels(helper.absolutePos(pos), source, target, receiver);
    }

    private static boolean retrieve(GameTestHelper helper, Barrels barrels, ItemStack filter, boolean inverted) {
        return CoverUtilityBehaviors.tickRetriever(helper.getLevel(), barrels.targetPos(), Direction.EAST,
                barrels.receiver(), filter, inverted, CoverUtilityBehaviors.RETRIEVER_PHASE, false);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 200)
    public static void emptyRetrieverTemplateAllowsBothModes(GameTestHelper helper) {
        var barrels = barrels(helper);
        for (boolean inverted : new boolean[]{false, true}) {
            barrels.clear();
            barrels.source().setItem(0, new ItemStack(Items.APPLE, 3));
            barrels.source().setItem(1, new ItemStack(Items.DIAMOND, 2));
            helper.assertTrue(retrieve(helper, barrels, ItemStack.EMPTY, inverted),
                    "a missing source filter set permits unfiltered retrieval in either mode");
            helper.assertTrue(barrels.source().getItem(0).isEmpty() && barrels.source().getItem(1).isEmpty()
                            && barrels.target().getItem(0).is(Items.APPLE) && barrels.target().getItem(0).getCount() == 3
                            && barrels.target().getItem(1).is(Items.DIAMOND) && barrels.target().getItem(1).getCount() == 2,
                    "all five items move once with no loss or duplication, mode=" + inverted);
        }
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 200)
    public static void retrieverUsesMetadataWithoutRelaxingLogisticsNbt(GameTestHelper helper) {
        var barrels = barrels(helper);
        var filter = new ItemStack(Items.IRON_PICKAXE);
        filter.setDamageValue(5);
        var candidate = filter.copy();
        candidate.getOrCreateTag().putString("retriever_candidate", "extra_nbt");
        helper.assertTrue(!FilterRules.itemMatches(filter, candidate),
                "the separate logistics template policy still compares nonempty NBT");
        barrels.source().setItem(0, candidate.copy());
        helper.assertTrue(retrieve(helper, barrels, filter, false)
                        && ItemStack.isSameItemSameTags(candidate, barrels.target().getItem(0))
                        && barrels.source().getItem(0).isEmpty(),
                "same item and metadata pass despite extra NBT, preserving the candidate data");

        barrels.clear();
        var otherDamage = candidate.copy();
        otherDamage.setDamageValue(6);
        barrels.source().setItem(0, otherDamage.copy());
        helper.assertTrue(!retrieve(helper, barrels, filter, false)
                        && barrels.source().getItem(0).getCount() == 1 && barrels.target().getItem(0).isEmpty(),
                "different metadata is refused without consuming the source");

        barrels.clear();
        barrels.source().setItem(0, candidate.copy());
        barrels.source().setItem(1, otherDamage.copy());
        helper.assertTrue(retrieve(helper, barrels, filter, true)
                        && barrels.source().getItem(0).getCount() == 1 && barrels.source().getItem(1).isEmpty()
                        && barrels.target().getItem(0).getDamageValue() == 6,
                "inversion excludes matching metadata but retrieves the different metadata");

        barrels.clear();
        var wildcard = filter.copy();
        wildcard.setDamageValue(Short.MAX_VALUE);
        barrels.source().setItem(0, otherDamage.copy());
        helper.assertTrue(retrieve(helper, barrels, wildcard, false)
                        && barrels.source().getItem(0).isEmpty() && barrels.target().getItem(0).getDamageValue() == 6,
                "source wildcard metadata admits other damage values without changing candidate data");
        helper.succeed();
    }
}
