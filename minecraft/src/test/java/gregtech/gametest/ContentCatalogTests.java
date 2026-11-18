package gregtech.gametest;

import gregtech.content.block.machine.TankControllerBlock;
import gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity;
import gregtech.content.book.GTBooks;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.multiblock.TankValveParameters;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import gregtech.content.tool.ElectricToolAssembly;
import gregtech.engine.fluid.FluidHazard;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.Arrays;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ContentCatalogTests {
    private ContentCatalogTests() {}

    @GameTest(template = "test_empty", batch = "content_catalog")
    public static void electricWrenchKeepsItsAssemblyAndWearCosts(GameTestHelper helper) {
        var wrench = ElectricToolAssembly.WRENCH;
        var definition = wrench.definition();
        helper.assertTrue(definition.blockCost() == 50 && definition.attackCost() == 200,
                "The LV electric wrench has distinct block and attack costs");
        var steel = GTMaterialRegistry.get("VanadiumSteel");
        var recipe = wrench.recipe(steel);
        helper.assertTrue(recipe != null && recipe.getWidth() == 3 && recipe.getHeight() == 3,
                "The LV wrench has a registered assembly grid");
        var head = GTItems.getStack(MaterialPrefix.toolHeadWrench, steel, 1);
        helper.assertTrue(recipe.getIngredients().get(1).test(head), "The wrench head sits in the top center slot");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "content_catalog")
    public static void tankBlocksAndEntitiesUseTheCatalogProtections(GameTestHelper helper) {
        var wood = valve("wood_tank_main_valve");
        var dense = valve("large_dense_adamantium_tank_main_valve");
        helper.assertTrue(wood.valveSpec() == TankValveParameters.find(17001)
                        && wood.valveSpec().simpleOnly() && wood.valveSpec().protections().isEmpty(),
                "The wooden valve uses its shared catalog entry and only permits simple fluids");
        helper.assertTrue(dense.valveSpec() == TankValveParameters.find(17065)
                        && dense.valveSpec().protections().equals(Set.of(
                                FluidHazard.GAS, FluidHazard.ACID, FluidHazard.PLASMA, FluidHazard.MAGIC)),
                "The large dense adamantium valve has all four protections");
        var entity = (MultiblockTankControllerBlockEntity) dense.newBlockEntity(BlockPos.ZERO, dense.defaultBlockState());
        helper.assertTrue(entity.valveSpec() == dense.valveSpec()
                        && entity.fluidCapacityForInspection() == 2_048_000_000L,
                "The block entity uses the same data and retains the dense tank capacity");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "content_catalog")
    public static void fusionKeepsGasAndMoltenInputs(GameTestHelper helper) {
        var recipe = MachineRecipeMaps.Fusion.recipeRows.stream()
                .filter(row -> row.specialValue == 437_256_192L)
                .findFirst().orElseThrow();
        var inputs = recipe.fluidInputs;
        helper.assertTrue(recipe.durationTicks == 3336 && recipe.energyPerTick == -8192 && inputs.length == 2,
                "The deuterium and lithium-6 row retains its costs and two input slots");
        helper.assertTrue(GTFluids.entryForFluid(inputs[0].getFluid()).registryName().equals("deuterium")
                        && inputs[0].getAmount() == 1000
                        && GTFluids.entryForFluid(inputs[1].getFluid()).registryName().equals("molten.lithium6")
                        && inputs[1].getAmount() == 144,
                "Deuterium is gas and lithium-6 is molten, in their original quantities");
        helper.assertTrue(recipe.fluidOutputs.length == 4, "The row retains all four output fluids");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "content_catalog")
    public static void printerKeepsManualRecipeAndBlueprintCatalyst(GameTestHelper helper) {
        var rows = MachineRecipeMaps.Printer.recipeRows;
        var manual = GTBooks.bookStack("Manual_Printer");
        helper.assertTrue(rows.stream().anyMatch(row -> !row.fakeRecipe
                        && row.itemInputs.length == 1 && row.itemInputs[0].is(Items.BOOK)
                        && row.itemOutputs.length == 1 && row.itemOutputs[0].is(manual.getItem())
                        && row.durationTicks == 256 && row.energyPerTick == 16
                        && row.fluidInputs.length == 1 && row.fluidInputs[0].getAmount() == 144
                        && row.explicitCatalystInputs().length == 0),
                "The printer manual remains a real recipe with its original cost");
        var emptyBlueprint = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "empty_blueprint"));
        helper.assertTrue(rows.stream().anyMatch(row -> row.fakeRecipe
                        && row.itemInputs.length == 2
                        && row.itemInputs[0].is(emptyBlueprint)
                        && row.itemInputs[1].is(GTTechnological.get("usb1_stick"))
                        && row.durationTicks == 32 && row.energyPerTick == 16
                        && row.fluidInputs.length == 1 && row.fluidInputs[0].getAmount() == 16
                        && Arrays.equals(row.explicitCatalystInputs(), new int[] {1})),
                "The blueprint display row explicitly keeps the USB stick");
        helper.succeed();
    }

    private static TankControllerBlock valve(String path) {
        return (TankControllerBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path));
    }
}
