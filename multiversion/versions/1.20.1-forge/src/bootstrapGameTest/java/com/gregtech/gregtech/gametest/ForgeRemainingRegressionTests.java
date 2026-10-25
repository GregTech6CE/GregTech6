package com.gregtech.gregtech.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Replays the nine remaining records from the original default-suite failure ledger. */
@GameTestHolder("gt6ce_forge_remaining")
@PrefixGameTestTemplate(false)
public final class ForgeRemainingRegressionTests {
    @GameTest(template = "test_blueprint_large", timeoutTicks = 1200)
    public static void substitutionsAreReported(GameTestHelper h) { BasicMachineCraftingTests.substitutionsAreReported(h); }
    @GameTest(template = "test_blueprint_large", timeoutTicks = 1200)
    public static void toolShapedRecipesDoNotMirrorUnlessTheOriginalDid(GameTestHelper h) { HandCraftingTests.toolShapedRecipesDoNotMirrorUnlessTheOriginalDid(h); }
    @GameTest(template = "test_blueprint_large", timeoutTicks = 1200)
    public static void playerRadiationAccumulatesAndPersists(GameTestHelper h) { NuclearSurvivalTests.playerRadiationAccumulatesAndPersists(h); }
    @GameTest(template = "test_blueprint_large", timeoutTicks = 1200)
    public static void radiationSuitAndMedicine(GameTestHelper h) { NuclearSurvivalTests.radiationSuitAndMedicine(h); }
    @GameTest(template = "test_blueprint_large", timeoutTicks = 1200)
    public static void targetChunkOwnsEveryPlatformAndPipeWriteInEitherOrder(GameTestHelper h) { DungeonMobFarmOuterTests.targetChunkOwnsEveryPlatformAndPipeWriteInEitherOrder(h); }
    @GameTest(template = "test_blueprint_large", timeoutTicks = 1200)
    public static void tablesMatchGt6(GameTestHelper h) { SurfaceFloraTests.tablesMatchGt6(h); }
    @GameTest(template = "test_blueprint_large", timeoutTicks = 1200)
    public static void dungeonCellDispatchMatchesGt6(GameTestHelper h) { DungeonTests.dungeonCellDispatchMatchesGt6(h); }
    @GameTest(template = "test_blueprint_large", timeoutTicks = 1200)
    public static void netherPortalRoomBuildsGt6Frame(GameTestHelper h) { DungeonKeysAndPortalsTests.netherPortalRoomBuildsGt6Frame(h); }
    @GameTest(template = "test_blueprint_large", timeoutTicks = 1200)
    public static void queenDeathHandsOutTheBroodAndPromotesAPrincess(GameTestHelper h) { BumbliaryTests.queenDeathHandsOutTheBroodAndPromotesAPrincess(h); }
}
