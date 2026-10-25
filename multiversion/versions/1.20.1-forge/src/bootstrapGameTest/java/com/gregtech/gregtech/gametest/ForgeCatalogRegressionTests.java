package com.gregtech.gregtech.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Batched failures: fixed catalog contracts and recipe-network stability. */
@GameTestHolder("gt6ce_forge_catalog_regressions")
@PrefixGameTestTemplate(false)
public final class ForgeCatalogRegressionTests {
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void craftingTagsContainOriginalIngredients(GameTestHelper helper) throws Exception {
        SurvivalStandardizationTests.craftingTagsContainOriginalIngredients(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void dungeonKeysAreRegisteredInBothLanguages(GameTestHelper helper) throws Exception {
        DungeonKeysAndPortalsTests.dungeonKeysAreRegisteredInBothLanguages(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void dungeonKeyAndPortalAssetsExist(GameTestHelper helper) throws Exception {
        DungeonKeysAndPortalsTests.dungeonKeyAndPortalAssetsExist(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void allManufacturingIngredientsAreRegistered(GameTestHelper helper) throws Exception {
        CrucibleSurvivalTests.allManufacturingIngredientsAreRegistered(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void basicMachinesHaveSurvivalRecipes(GameTestHelper helper) throws Exception {
        MaterialCompatibilityTests.basicMachinesHaveSurvivalRecipes(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void allAxialRecipesAcceptEveryChemistryAtOnlyTheOriginalTier(GameTestHelper helper) throws Exception {
        AxialBatteryRecipeTests.allAxialRecipesAcceptEveryChemistryAtOnlyTheOriginalTier(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 600)
    public static void axialRecipesLoadAndToolsRemain(GameTestHelper helper) throws Exception {
        AxialGeneratorTests.axialRecipesLoadAndToolsRemain(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 600)
    public static void originalSteamAndDynamoHousingsFormAtEveryGrade(GameTestHelper helper) throws Exception {
        OriginalAxialControllerTests.originalSteamAndDynamoHousingsFormAtEveryGrade(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 600)
    public static void originalGasTurbineHousingsFormAndUseOriginalSteamHousing(GameTestHelper helper) throws Exception {
        OriginalAxialControllerTests.originalGasTurbineHousingsFormAndUseOriginalSteamHousing(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void axialControllersHaveOneOriginalManufacturingRoute(GameTestHelper helper) throws Exception {
        OriginalTankAndAxialRecipeTests.axialControllersHaveOneOriginalManufacturingRoute(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void originalFusionRecipesAndCraftingResolve(GameTestHelper helper) throws Exception {
        FusionRegressionTests.originalFusionRecipesAndCraftingResolve(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void originalMatterRulesAndManufacturingExist(GameTestHelper helper) throws Exception {
        MatterFabricatorRegressionTests.originalMatterRulesAndManufacturingExist(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void everyBaleHasOriginalFireHarvestModelAndSurvivalRecipe(GameTestHelper helper) throws Exception {
        BaleRepairTests.everyBaleHasOriginalFireHarvestModelAndSurvivalRecipe(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void rejectedPacketsNeverEnergizeWireAndAcceptedPowerAccumulates(GameTestHelper helper) throws Exception {
        ElectricWireRepairTests.rejectedPacketsNeverEnergizeWireAndAcceptedPowerAccumulates(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void cableCycleAndBranchConserveAcceptedPackets(GameTestHelper helper) throws Exception {
        ChargingCraftingRepairTests.cableCycleAndBranchConserveAcceptedPackets(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void realChargerAndToolAssemblyUseChemicalCapacity(GameTestHelper helper) throws Exception {
        ChemicalBatteryTests.realChargerAndToolAssemblyUseChemicalCapacity(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void originalLvPatternsMaterialsAndToolRemainders(GameTestHelper helper) throws Exception {
        ElectricToolAssemblyTests.originalLvPatternsMaterialsAndToolRemainders(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void capacityTintAndEnergyArithmetic(GameTestHelper helper) throws Exception {
        ElectricToolAssemblyTests.capacityTintAndEnergyArithmetic(helper);
    }

    @GameTest(template = "coin_pile_space", timeoutTicks = 600)
    public static void allEighteenIndicatorsHaveOriginalMetaAndMaterials(GameTestHelper helper) throws Exception {
        BedrockFlowerRepairTests.allEighteenIndicatorsHaveOriginalMetaAndMaterials(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void onlyTransformersAndBatteryBoxesExistAtUln(GameTestHelper helper) throws Exception {
        VoltageTierAlignmentTests.onlyTransformersAndBatteryBoxesExistAtUln(helper);
    }

    @GameTest(template = "test_empty", batch = "recipe_reload_only", timeoutTicks = 2400)
    public static void realReloadsPreserveGeneratedRecipesOverridesAndFurnaceMirrors(GameTestHelper helper) throws Exception {
        RuntimeRecipeReloadTests.realReloadsPreserveGeneratedRecipesOverridesAndFurnaceMirrors(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void generatedElectricRowsReachServerAndNetwork(GameTestHelper helper) {
        ElectricToolAssemblyTests.generatedRowsReachServerAndNetwork(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void repeatedToolExpansionAndNetworkRoundTripAreStable(GameTestHelper helper) {
        var ingredient = net.minecraft.world.item.crafting.Ingredient.of(
                com.gregtech.gregtech.api.tool.GTToolHelper.displayTool(com.gregtech.gregtech.api.tool.GTToolType.SCREWDRIVER));
        var first = com.gregtech.gregtech.recipe.CraftingTools.expand(ingredient);
        var second = com.gregtech.gregtech.recipe.CraftingTools.expand(first);
        helper.assertTrue(java.util.Arrays.stream(first.getItems()).anyMatch(s -> s.getItem() instanceof com.gregtech.gregtech.item.PocketToolItem)
                        && java.util.Arrays.stream(first.getItems()).anyMatch(s -> s.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem),
                "both pocket and powered tool families are present in the expansion guard");
        helper.assertTrue(java.util.Arrays.equals(first.getItems(), second.getItems())
                        || stacks(first).equals(stacks(second)),
                "repeated expansion preserves complete tool choices without duplicates");
        var inputs = net.minecraft.core.NonNullList.of(net.minecraft.world.item.crafting.Ingredient.EMPTY, first);
        var base = new net.minecraft.world.item.crafting.ShapedRecipe(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", "test/tool_expansion"), "",
                net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT, 1, 1, inputs,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
        var recipe = new com.gregtech.gregtech.recipe.ToolShapedRecipe(base, false);
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            com.gregtech.gregtech.recipe.ToolShapedRecipe.SERIALIZER.toNetwork(buffer, recipe);
            var decoded = com.gregtech.gregtech.recipe.ToolShapedRecipe.SERIALIZER.fromNetwork(recipe.getId(), buffer);
            helper.assertTrue(!buffer.isReadable() && !decoded.allowMirror()
                            && stacks(recipe.getIngredients().get(0)).equals(stacks(decoded.getIngredients().get(0))),
                    "network decoding preserves authoritative tool choices and consumes exactly its payload");
        } finally { buffer.release(); }
        helper.succeed();
    }

    private static java.util.List<String> stacks(net.minecraft.world.item.crafting.Ingredient ingredient) {
        return java.util.Arrays.stream(ingredient.getItems())
                .map(s -> s.save(new net.minecraft.nbt.CompoundTag()).toString()).sorted().toList();
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void captureRuntimeCatalogForRemainingFailureDiagnosis(GameTestHelper helper) throws Exception {
        var ids = new java.util.TreeSet<String>();
        net.minecraftforge.registries.ForgeRegistries.ITEMS.getKeys().forEach(id -> ids.add(id.toString()));
        var recipes = new java.util.TreeMap<String,String>();
        for (var recipe : helper.getLevel().getRecipeManager().getRecipes()) {
            var result = recipe.getResultItem(helper.getLevel().registryAccess());
            recipes.put(recipe.getId().toString(), net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(result.getItem())
                    + "*" + result.getCount() + "|" + net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()));
        }
        var machines = new java.util.ArrayList<java.util.Map<String,Object>>();
        for (var entry : com.gregtech.gregtech.registry.GTBasicMachines.all()) {
            var spec = entry.get().basicSpec();
            machines.add(java.util.Map.of("namespace", spec.namespace(), "id", spec.id(),
                    "machine", spec.machineName(), "tier", spec.tier(),
                    "sourceTable", com.gregtech.gregtech.data.BasicMachineCraftingRecipes.find(spec.machineName(),spec.tier()) != null));
        }
        var report = java.nio.file.Path.of(System.getProperty("gregtech.integration.gameTestReport", "forge-catalog.xml"))
                .toAbsolutePath().normalize().resolveSibling("forge-batch-runtime-catalog.json");
        java.nio.file.Files.writeString(report, new com.google.gson.Gson().toJson(java.util.Map.of(
                "items",ids,"recipes",recipes,"basicMachines",machines)));
        helper.assertTrue(ids.size()>10000 && recipes.size()>1000,"runtime catalog captures initialized registries and recipes");
        helper.succeed();
    }
}
