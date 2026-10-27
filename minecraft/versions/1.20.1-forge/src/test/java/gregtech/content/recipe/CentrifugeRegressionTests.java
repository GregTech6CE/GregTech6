package gregtech.content.recipe;

import gregtech.GregTechConfig;
import gregtech.api.material.GTValues;
import gregtech.content.block.MaterialBlockLike;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Focused native registration/matching checks; run without optional recipe providers or addons. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class CentrifugeRegressionTests {
    private CentrifugeRegressionTests() {}

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void sourceRecipesAfterStartup(GameTestHelper helper) {
        require(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY,
                "materials must finish bootstrap before observing recipes");
        require(GregTechConfig.SPEC.isLoaded(), "COMMON configuration must be loaded");
        require(MachineRecipeOverlay.applications() > 0, "server recipe/tag overlay must have run");
        for (String provider : List.of("thermal", "ThermalExpansion", "immersiveengineering",
                "ImmersiveEngineering", "factorization", "forestry", "Forestry", "ic2", "IC2",
                "IC2-Classic-Spmod", "exoticbirds")) {
            require(!ModList.get().isLoaded(provider), "default-profile test excludes " + provider);
        }
        optionalFallbacks();
        uraniumGate();
        physicalBlockInputs();
        builtinEvents();
        moonStone();
        helper.succeed();
    }

    private static void optionalFallbacks() {
        // GT6 Ores: two distinct Ash slots; do not merge them or prefer a guessed slag.
        checkRecipe(recipe("gt6ce:dust_darkashes", 2), 64, 144, new long[]{10000, 10000},
                new ItemStack[]{item("gt6ce:dust_ashes", 1), item("gt6ce:dust_ashes", 1)});
        checkRecipe(recipe("gt6ce:sticky_comb", 1), 16, 64, new long[]{10000, 3000},
                new ItemStack[]{item("gt6ce:dust_waxbee", 1), item("gt6ce:rubber_resin", 1)},
                fluid("gt6ce:latex", 144));
    }

    private static void uraniumGate() {
        boolean enabled = GregTechConfig.easyU235Extraction();
        boolean expected = Boolean.parseBoolean(System.getProperty("gregtech.test.expectedEasyU235Extraction", "false"));
        require(enabled == expected, "actual startup U235 config differs from requested test profile");
        for (boolean tiny : new boolean[]{true, false}) {
            String input = tiny ? "gt6ce:crushed_centrifuged_tiny_uranium" : "gt6ce:crushed_centrifuged_uranium";
            if (!enabled) {
                require(rows(input).isEmpty(), "U235 extraction must be absent by default: " + input);
            } else {
                checkRecipe(recipe(input, 1), 1024, tiny ? 256 : 2304,
                        tiny ? new long[]{10000, 200, 100} : new long[]{10000, 1800, 900},
                        new ItemStack[]{item(tiny ? "gt6ce:dust_tiny_uranium" : "gt6ce:dust_uranium", 1),
                                item("gt6ce:dust_tiny_uranium235", 8), item("gt6ce:dust_tiny_uranium233", 8)});
            }
        }
        // An unaffected neighboring refined-material rule must not inherit the uranium gate.
        checkRecipe(recipe("gt6ce:crushed_centrifuged_beryllium", 1), 256, 2304,
                new long[]{10000, 1350, 1350}, new ItemStack[]{item("gt6ce:dust_beryllium", 1),
                        item("gt6ce:dust_tiny_beryllium7", 8), item("gt6ce:dust_tiny_beryllium8", 8)});
    }

    private static void physicalBlockInputs() {
        for (String material : List.of("bedrock", "netherrack", "endstone", "whiteendsand",
                "blackendsand", "spacestone", "moonturf", "moonstone")) {
            require(rows("gt6ce:dust_" + material).stream().noneMatch(r -> r.mInputs[0].getCount() == 9),
                    "physical block input must not become nine dusts: " + material);
        }
        for (String material : List.of("Bedrock", "Netherrack", "Endstone", "WhiteEndSand",
                "BlackEndSand", "SpaceStone", "MoonTurf", "MoonStone")) {
            physicalBlock(material);
        }
        checkRecipe(recipe("gt6ce:block_dust_bedrock", 1), 256, 1296,
                new long[]{7000, 3000, 3000, 3000, 3000, 3000}, bedrockOutputs(9));
        checkRecipe(recipe("gt6ce:block_dust_netherrack", 1), 64, 144,
                new long[]{8000, 1000, 500, 500, 100},
                new ItemStack[]{item("gt6ce:dust_stone", 9), item("gt6ce:dust_tiny_sulfur", 18),
                        item("gt6ce:dust_tiny_redstone", 9), item("gt6ce:dust_tiny_coal", 9),
                        item("gt6ce:dust_tiny_gold", 9)});
        for (String material : List.of("endstone", "whiteendsand", "blackendsand")) {
            checkRecipe(recipe("gt6ce:block_dust_" + material, 1), 64, 144,
                    new long[]{8000, 100, 300, 100, 10},
                    new ItemStack[]{item("gt6ce:dust_sand", 9), item("gt6ce:dust_tiny_platinum", 9),
                            item("gt6ce:dust_tiny_tungstentrioxide", 9), item("gt6ce:dust_tiny_endium", 9),
                            item("gt6ce:gem_sugilite", 9)},
                    fluid("gt6ce:helium", 125), fluid("gt6ce:helium3", 18),
                    fluid("gt6ce:nitrogendioxide", 250), fluid("gt6ce:carbondioxide", 250),
                    fluid("gt6ce:hydrogen", 125), fluid("gt6ce:deuterium", 125));
        }
        checkRecipe(recipe("gt6ce:block_dust_spacestone", 1), 64, 144,
                new long[]{8000, 100, 100}, new ItemStack[]{item("gt6ce:dust_sand", 9),
                        item("gt6ce:dust_tiny_platinum", 9), item("gt6ce:dust_tiny_meteoriciron", 9)}, endGases());
        checkRecipe(recipe("gt6ce:block_dust_moonturf", 1), 64, 288,
                new long[]{8000, 1000, 1000, 1000, 1000, 100},
                new ItemStack[]{item("gt6ce:dust_sand", 9), item("gt6ce:dust_tiny_basalt", 9),
                        item("gt6ce:dust_tiny_alumina", 9), item("gt6ce:dust_tiny_ilmenite", 9),
                        item("gt6ce:dust_tiny_pyrolusite", 9), item("gt6ce:dust_tiny_meteoriciron", 9)}, endGases());
        // Separate, legitimate source dust1 rule survives the physical-block correction.
        checkRecipe(recipe("gt6ce:dust_bedrock", 1), 256, 144,
                new long[]{7000, 3000, 3000, 3000, 3000, 3000}, bedrockOutputs(1));
    }

    private static void physicalBlock(String name) {
        var material = GTMaterialRegistry.get(name).resolve();
        ItemStack stack = item("gt6ce:block_dust_" + name.toLowerCase(java.util.Locale.ROOT), 1);
        require(stack.getItem() instanceof BlockItem, name + " actual physical BlockItem");
        var block = ((BlockItem) stack.getItem()).getBlock();
        require(block instanceof MaterialBlockLike, name + " actual material block");
        var form = (MaterialBlockLike) block;
        require(form.prefix() == BlockMaterialPrefix.blockDust, name + " blockDust prefix identity");
        require(form.prefix().getMaterialWeight() == 9 * GTValues.U, name + " physical nine-unit block");
        require(form.material().resolve() == material, name + " canonical material identity");
        require(block.asItem() == stack.getItem() && BuiltInRegistries.BLOCK.getKey(block).equals(
                BuiltInRegistries.ITEM.getKey(stack.getItem())), name + " bound block/item holder identity");
    }

    private static FluidStack[] endGases() {
        return new FluidStack[]{fluid("gt6ce:helium", 125), fluid("gt6ce:helium3", 18),
                fluid("gt6ce:nitrogendioxide", 250), fluid("gt6ce:carbondioxide", 250),
                fluid("gt6ce:hydrogen", 125), fluid("gt6ce:deuterium", 125)};
    }

    private static ItemStack[] bedrockOutputs(int count) {
        return new ItemStack[]{item("gt6ce:dust_deepslate", count), item("gt6ce:dust_tiny_adamantite", count),
                item("gt6ce:dust_tiny_draconium", count), item("gt6ce:dust_tiny_monazite", 2 * count),
                item("gt6ce:dust_tiny_zircon", 3 * count), item("gt6ce:dust_tiny_graphite", 3 * count)};
    }

    private static void builtinEvents() {
        for (String egg : List.of("minecraft:egg", "gt6ce:egg")) {
            checkRecipe(recipe(egg, 1), 16, 64, new long[]{10000, 10000, 10000},
                    new ItemStack[]{item("gt6ce:egg_white", 1), item("gt6ce:egg_yolk", 1),
                            item("minecraft:bone_meal", 1)});
        }
        checkRecipe(recipe("minecraft:slime_ball", 1), 16, 64, new long[0], new ItemStack[0],
                fluid("gt6ce:latex", 72), fluid("gt6ce:glue", 250));
        checkRecipe(recipe("gt6ce:sticky_goo", 1), 16, 64, new long[0], new ItemStack[0],
                fluid("gt6ce:glue", 250));
        require(rows("gt6ce:slimeball").isEmpty(), "Borax slime is not a fifth positive event");
        var outcomes = BuiltinEventCentrifugeRecipes.outcomes();
        require(outcomes.size() == 5, "exact five bounded startup observations");
        require(outcomes.stream().filter(o -> o.acceptedRecipe() != null).count() == 4,
                "exact four accepted startup rows");
        for (var outcome : outcomes) {
            if (outcome.acceptedRecipe() != null) {
                require(outcome.status().equals("ADDED"), "accepted row must report ADDED");
                require(MachineRecipeMaps.Centrifuge.mRecipeList.contains(outcome.acceptedRecipe()),
                        "accepted startup row must still be registered");
            }
        }
    }

    private static void moonStone() {
        checkRecipe(recipe("gt6ce:dust_moonstone", 1), 64, 16,
                new long[]{8000, 500, 400, 300, 100, 50},
                new ItemStack[]{item("gt6ce:dust_basalt", 1), item("gt6ce:dust_tiny_silicondioxide", 1),
                        item("gt6ce:dust_tiny_alumina", 1), item("gt6ce:dust_tiny_ilmenite", 1),
                        item("gt6ce:dust_tiny_rareearth", 1), item("gt6ce:dust_tiny_platinum", 1)});
        checkRecipe(recipe("gt6ce:block_dust_moonstone", 1), 64, 144,
                new long[]{8000, 500, 400, 300, 100, 50},
                new ItemStack[]{item("gt6ce:dust_basalt", 9), item("gt6ce:dust_tiny_silicondioxide", 9),
                        item("gt6ce:dust_tiny_alumina", 9), item("gt6ce:dust_tiny_ilmenite", 9),
                        item("gt6ce:dust_tiny_rareearth", 9), item("gt6ce:dust_tiny_platinum", 9)});
    }

    private static List<Recipe> rows(String inputId) {
        ItemStack input = item(inputId, 1);
        return MachineRecipeMaps.Centrifuge.mRecipeList.stream()
                .filter(r -> r.mInputs.length == 1 && r.mInputs[0].getItem() == input.getItem()).toList();
    }

    private static Recipe recipe(String inputId, int count) {
        List<Recipe> matches = rows(inputId);
        require(matches.size() == 1, "expected exactly one Centrifuge row for " + inputId + ", got " + matches.size());
        Recipe recipe = matches.get(0);
        require(ItemStack.matches(recipe.mInputs[0], item(inputId, count)), "exact input/count: " + inputId);
        Recipe found = MachineRecipeMaps.Centrifuge.findRecipe(List.of(item(inputId, count)), List.of(), false, 1, 6);
        require(found == recipe, "native lookup must find registered row: " + inputId);
        return recipe;
    }

    private static void checkRecipe(Recipe recipe, long power, long ticks, long[] chances,
                                    ItemStack[] outputs, FluidStack... fluids) {
        String label = BuiltInRegistries.ITEM.getKey(recipe.mInputs[0].getItem()).toString();
        require(recipe.mEUt == power && recipe.mDuration == ticks, label + " native RU/ticks");
        require(recipe.mSpecialValue == 0 && recipe.mSpecialItems == null, label + " no special gate");
        require(recipe.mFluidInputs.length == 0, label + " no fluid input gate");
        require(recipe.mEnabled && !recipe.mFakeRecipe && !recipe.mHidden && recipe.mCanBeBuffered,
                label + " enabled ordinary recipe");
        require(!recipe.mNeedsEmptyOutput && !recipe.mMaterialRecovery && !recipe.mRequiresEmptyContainerInputs
                && !recipe.mExactItemInputs && !recipe.mExplicitCatalystsOnly, label + " ordinary matching flags");
        require(recipe.explicitCatalystInputs().length == 0 && recipe.explicitCatalystFluidInputs().length == 0
                && recipe.explicitPresenceFluidInputs().length == 0, label + " no retained inputs");
        require(Arrays.equals(recipe.mChances, chances), label + " ordered raw chances");
        require(recipe.mMaxChances.length == chances.length
                && Arrays.stream(recipe.mMaxChances).allMatch(value -> value == 10000), label + " chance denominator");
        require(recipe.mOutputs.length == outputs.length, label + " output slot count");
        for (int i = 0; i < outputs.length; i++) {
            require(ItemStack.matches(recipe.mOutputs[i], outputs[i]), label + " output slot " + i);
        }
        require(recipe.mFluidOutputs.length == fluids.length, label + " fluid output slot count");
        for (int i = 0; i < fluids.length; i++) {
            FluidStack actual = recipe.mFluidOutputs[i];
            require(actual.getFluid() == fluids[i].getFluid() && actual.getAmount() == fluids[i].getAmount()
                    && !actual.hasTag(), label + " fluid output slot " + i);
        }
    }

    private static ItemStack item(String id, int count) {
        ResourceLocation key = new ResourceLocation(id);
        require(BuiltInRegistries.ITEM.containsKey(key), "missing expected item: " + id);
        return new ItemStack(BuiltInRegistries.ITEM.get(key), count);
    }

    private static FluidStack fluid(String id, int amount) {
        ResourceLocation key = new ResourceLocation(id);
        require(BuiltInRegistries.FLUID.containsKey(key), "missing expected fluid: " + id);
        return new FluidStack(BuiltInRegistries.FLUID.get(key), amount);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
