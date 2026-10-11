package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.content.multiblock.TankValveSpec;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MultiblockCraftingRecipes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

/** Guards GT6 170xx/17996 manufacturing and the 172xx axial progression. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class OriginalTankAndAxialRecipeTests {
    private OriginalTankAndAxialRecipeTests() {}

    @GameTest(template = "test_empty")
    public static void allTankValvesConsumeTheirOriginalWallOrSmallerValve(GameTestHelper h) {
        h.assertTrue(TankValveSpec.all().size() == 25, "GT6 has 25 tank valves");
        for (TankValveSpec valve : TankValveSpec.all()) {
            int id = valve.originalId();
            Item output = LargeMachineParts.block(id).asItem();
            int predecessor = valve.size() == 5 ? id - 40 : valve.wallId();
            Item mainIngredient = LargeMachineParts.block(predecessor).asItem();
            String path = ForgeRegistries.ITEMS.getKey(output).getPath();
            var entry = MultiblockCraftingRecipes.find(path);
            h.assertTrue(entry != null, "source recipe table covers valve " + id);
            h.assertTrue(entry.keys().get('M').equals("item:gregtech:"
                    + ForgeRegistries.ITEMS.getKey(mainIngredient).getPath()),
                    "valve " + id + " preserves GT6's wall/prior-valve M ingredient");

            ShapedRecipe recipe = recipe(h, "machines/multiblock/" + path);
            h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess()).is(output),
                    "loaded valve recipe outputs GT6 " + id);
            h.assertTrue(recipe.getWidth() == 3 && recipe.getHeight() == 3,
                    "valve " + id + " has original 3x3 shape");
            h.assertTrue(recipe.getIngredients().get(4).test(new ItemStack(mainIngredient)),
                    "loaded valve " + id + " consumes correct M ingredient");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void everyValveWallHasASurvivalEntry(GameTestHelper h) {
        ShapedRecipe wood = recipe(h, "machines/multiblock/wood_wall");
        h.assertTrue(wood.getResultItem(h.getLevel().registryAccess())
                        .is(LargeMachineParts.block(18001).asItem())
                        && wood.getIngredients().get(0).test(new ItemStack(
                        ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(
                                "gregtech", "planks_treated")))),
                "wood tank wall consumes GT6's treated plank plate");
        for (int wallId : new int[]{18002, 18003, 18004, 18005, 18006, 18007,
                18022, 18023, 18024, 18025, 18026, 18027}) {
            Item wall = LargeMachineParts.block(wallId).asItem();
            h.assertTrue(MachineRecipeMaps.Welder.mRecipeList.stream().anyMatch(
                            r -> r.mOutputs.length == 1 && r.mOutputs[0].is(wall)),
                    "tank wall " + wallId + " has its GT6 four-plate welding route");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void vonDaGraaggHasSourceComponents(GameTestHelper h) {
        var entry = MultiblockCraftingRecipes.find("von_da_graagg_generator");
        h.assertTrue(entry != null && entry.pattern().equals("CSC|PMP|CEC"),
                "17996 preserves GT6's CSC/PMP/CEC pattern");
        h.assertTrue(entry.keys().get('M').equals("block:casingMachine@SteelGalvanized")
                        && entry.keys().get('P').equals("item:gregtech:crystal_processor_ruby"),
                "17996 uses galvanized casing and ruby crystal processor");
        ShapedRecipe recipe = recipe(h, "machines/multiblock/von_da_graagg_generator");
        h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess())
                        .is(LargeMachineParts.block(17996).asItem()), "17996 recipe loads");
        h.assertTrue(recipe.getIngredients().get(1).test(new ItemStack(net.minecraft.world.item.Items.NETHER_STAR))
                        && recipe.getIngredients().get(7).test(new ItemStack(net.minecraft.world.item.Items.ENDER_EYE)),
                "17996 consumes Nether Star and Eye of Ender at GT6 positions");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void oldAxialControllersCannotBypass172xxManufacturing(GameTestHelper h) {
        Map<String, Integer> originals = Map.ofEntries(
                Map.entry("large_turbine_main", 17211),
                Map.entry("large_steam_turbine_trinitanium", 17212),
                Map.entry("large_steam_turbine_graphene", 17213),
                Map.entry("large_steam_turbine_vibramantium", 17214),
                Map.entry("large_dynamo_main", 17221),
                Map.entry("large_dynamo_titanium", 17222),
                Map.entry("large_dynamo_tungstensteel", 17223),
                Map.entry("large_dynamo_adamantium", 17224),
                Map.entry("large_gas_turbine_main", 17231),
                Map.entry("large_gas_turbine_trinitanium", 17232),
                Map.entry("large_gas_turbine_graphene", 17233),
                Map.entry("large_gas_turbine_vibramantium", 17234));
        for (var conversion : originals.entrySet()) {
            Item legacy = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(
                    "gregtech", conversion.getKey()));
            Item original = LargeMachineParts.block(conversion.getValue()).asItem();
            h.assertTrue(legacy != null, "legacy axial controller registered: " + conversion.getKey());
            ShapedRecipe recipe = recipe(h, "machines/multiblock/" + conversion.getKey());
            h.assertTrue(recipe.getWidth() == 1 && recipe.getHeight() == 1
                            && recipe.getIngredients().get(0).test(new ItemStack(original)),
                    "legacy " + conversion.getKey() + " converts only from 172xx original");
            int routes = 0;
            for (var candidate : h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                if (!candidate.getResultItem(h.getLevel().registryAccess()).is(legacy)) continue;
                routes++;
                h.assertTrue(candidate instanceof ShapedRecipe shaped
                                && shaped.getWidth() == 1 && shaped.getHeight() == 1
                                && shaped.getIngredients().get(0).test(new ItemStack(original)),
                        "no alternate cheaper crafting route for " + conversion.getKey());
            }
            h.assertTrue(routes == 1, "exactly one legacy conversion route for " + conversion.getKey());
        }
        for (int i = 0; i < 4; i++) {
            int gasId = 17231 + i;
            Item gas = LargeMachineParts.block(gasId).asItem();
            Item steam = LargeMachineParts.block(17211 + i).asItem();
            String axialId = switch (i) {
                case 0 -> "large_gas_turbine_main";
                case 1 -> "large_gas_turbine_trinitanium";
                case 2 -> "large_gas_turbine_graphene";
                default -> "large_gas_turbine_vibramantium";
            };
            ShapedRecipe originalRecipe = recipe(h, "axial/" + axialId);
            h.assertTrue(originalRecipe.getResultItem(h.getLevel().registryAccess()).is(gas)
                            && originalRecipe.getIngredients().get(4).test(new ItemStack(steam)),
                    "GT6 gas " + gasId + " requires steam turbine " + (17211 + i));
        }
        h.succeed();
    }

    private static ShapedRecipe recipe(GameTestHelper h, String path) {
        var id = ResourceLocation.fromNamespaceAndPath("gregtech", path);
        return (ShapedRecipe) h.getLevel().getRecipeManager().byKey(id)
                .orElseThrow(() -> new AssertionError("Missing recipe " + id));
    }
}
