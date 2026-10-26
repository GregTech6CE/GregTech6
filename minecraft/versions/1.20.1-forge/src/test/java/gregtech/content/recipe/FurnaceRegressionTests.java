package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.DenseOreBlock;
import gregtech.content.block.OreBlock;
import gregtech.content.block.VanillaOreBlock;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.generated.MaterialWorkability;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import static gregtech.content.recipe.SourceFurnaceNativeAdapter.*;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;

/** Native producer boundaries, not a global ban on independent cooking recipes.
 * Source: Loader_OreProcessing311/345-348 and Loader_Recipes_Furnace193-198.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class FurnaceRegressionTests {
    private FurnaceRegressionTests() {}

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void sourceFurnaceAfterStartup(GameTestHelper helper) {
        require(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY
                && MachineRecipeOverlay.applications() > 0, "native registration and cooking overlay must finish");
        var manager = helper.getLevel().getRecipeManager();
        for (String name : List.of("Nickel", "Gold", "Iron", "Titanium")) {
            require(OreSourcePolicy.furnaceDecision(material(name)) == Decision.NO, name + " source input FURNACE is NO");
            require(!FurnaceSmeltingRules.allows(material(name)), name + " form listener shares the source NO");
            // Nonidentical ore/crushed inputs distinguish a gate from same-dust recipe cancellation.
            for (var prefix : List.of(MaterialPrefix.oreRaw, MaterialPrefix.crushed, MaterialPrefix.crushedPurified,
                    MaterialPrefix.crushedCentrifuged)) absentOwnedInput(form(prefix, name, 1), manager.getRecipes());
            require(FurnaceSmeltingRecipes.entries().stream().noneMatch(e -> e.material().resolve() == material(name)),
                    name + " no owned form-listener entries");
        }
        var aluminium = material("Aluminium");
        require(aluminium.getTargetCrushingMaterial().resolve() == material("Alumina"), "actual Al crushing target is Alumina");
        require(OreSourcePolicy.furnaceDecision(aluminium) == Decision.YES
                && OreSourcePolicy.furnaceDecision(material("Alumina")) == Decision.NO,
                "original form permission and crushed block permission differ");
        var aluminiumBlock = ore("Aluminium");
        outcome(block(aluminium, material("Alumina"), aluminiumBlock, new Snapshot()), Action.SOURCE_NO_ROW);
        absentOwnedInput(aluminiumBlock, manager.getRecipes());
        var aluminiumRaw = form(MaterialPrefix.oreRaw, "Aluminium", 1);
        var aluminiumIngot = form(MaterialPrefix.ingot, "Aluminium", 1);
        check(row(aluminiumRaw), aluminiumIngot);
        require(MaterialPrefix.oreRaw.getMaterialWeight() == 2 * GTValues.U,
                "oreRaw input contains2U but source furnace listener deliberately fixes output basis to1U");
        require(FurnaceSmeltingRecipes.entries().stream().anyMatch(e -> e.prefix().equals("oreRaw")
                && e.material().resolve() == aluminium && ItemStack.matches(e.output(), aluminiumIngot)),
                "native oreRaw Al is owned by the form listener, not the ordinary block branch");
        checkCooking(manager.getRecipes(), RecipeType.SMELTING, aluminiumRaw, aluminiumIngot, 200, 3);
        checkCooking(manager.getRecipes(), RecipeType.BLASTING, aluminiumRaw, aluminiumIngot, 100, 3);

        var galena = material("Galena");
        var nuggets = form(MaterialPrefix.nugget, "Lead", 3);
        for (var input : galenaBlocks()) {
            var plan = block(galena, galena.getTargetCrushingMaterial().resolve(), input, new Snapshot());
            require(plan.action() == Action.SOURCE_ROW && ItemStack.matches(plan.ingot(), nuggets) && !plan.food(),
                    "each Galena ore shape uses fixed PbU/3 ->3 nuggets, never crusher/dense multiplier or dust");
            check(row(input), nuggets);
            var passive = SourceFurnaceNativeAdapter.cooking(row(input));
            require(passive.isPresent() && passive.get().experience() == 0 && !passive.get().food()
                    && FurnaceSmeltingRecipes.cooking(input.getItem()).isEmpty(),
                    "block cooking metadata belongs to accepted recipe identity, never input replacement authority");
            checkCooking(manager.getRecipes(), RecipeType.SMELTING, input, nuggets, 200, 0);
            checkCooking(manager.getRecipes(), RecipeType.BLASTING, input, nuggets, 100, 0);
        }
        var galenaRaw = form(MaterialPrefix.oreRaw, "Galena", 1);
        check(row(galenaRaw), nuggets);
        require(FurnaceSmeltingRecipes.entries().stream().anyMatch(e -> e.prefix().equals("oreRaw")
                && e.material().resolve() == galena && ItemStack.matches(e.output(), nuggets)),
                "Galena oreRaw keeps its independent source form-listener ownership");
        checkCooking(manager.getRecipes(), RecipeType.SMELTING, galenaRaw, nuggets, 200, 1);
        checkCooking(manager.getRecipes(), RecipeType.BLASTING, galenaRaw, nuggets, 100, 1);
        require(OreSourcePolicy.furnaceDecision(material("Cinnabar")) == Decision.NO
                && OreSourcePolicy.furnaceDecision(material("Mercury")) == Decision.YES,
                "Cinnabar input NO cannot be replaced by its Mercury target YES");
        require(FurnaceSmeltingRecipes.entries().stream().noneMatch(e -> e.material().resolve() == material("Cinnabar")),
                "no Cinnabar form-listener entry");
        for (var prefix : List.of(MaterialPrefix.oreRaw, MaterialPrefix.dust, MaterialPrefix.dustSmall,
                MaterialPrefix.dustTiny, MaterialPrefix.gem, MaterialPrefix.rockGt, MaterialPrefix.crushed,
                MaterialPrefix.crushedTiny, MaterialPrefix.crushedPurified, MaterialPrefix.crushedPurifiedTiny,
                MaterialPrefix.crushedCentrifuged, MaterialPrefix.crushedCentrifugedTiny))
            absentOwnedInput(form(prefix, "Cinnabar", 1), manager.getRecipes());
        absentOwnedInput(ore("Cinnabar"), manager.getRecipes());
        absentOwnedInput(item("gt6ce:crystal_ore_cinnabar"), manager.getRecipes());
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void missingFormsAndIndependentCooking(GameTestHelper helper) {
        selectedHolderBoundaries();
        var galena = material("Galena");
        var input = ore("Galena");
        require(!form(MaterialPrefix.dustTiny, "Lead", 3).isEmpty(), "dust exists but is not a permitted source fallback");
        outcome(block(galena, galena, input, new Snapshot() {
            @Override ItemStack ingot(GTMaterial target, long amount) { return ItemStack.EMPTY; }
            @Override ItemStack gem(GTMaterial target, long amount) { return ItemStack.EMPTY; }
        }), Action.DEFERRED_NO_ROW);
        outcome(block(galena, galena, input, new Snapshot() {
            @Override Decision decision(GTMaterial material) { return Decision.UNKNOWN; }
        }), Action.LEGACY_UNSUPPORTED); // Modeled changed metadata, not live addon integration.
        outcome(block(galena, galena, input, new Snapshot() {
            @Override boolean hidden(GTMaterial material) { return true; }
        }), Action.SOURCE_NO_ROW);
        outcome(block(galena, galena, new ItemStack(Items.STONE), new Snapshot()), Action.LEGACY_UNSUPPORTED);
        outcome(block(galena, galena, ItemStack.EMPTY, new Snapshot()), Action.DEFERRED_NO_ROW);
        // Unregistered local objects exercise unknown/addon-style identity without registry mutation.
        var external = new GTMaterial(31000, "FurnaceExternalFixture", "Furnace external fixture", 0xFFFFFF)
                .put(MaterialProperty.MELTING);
        require(!GTMaterialRegistry.isCoreMaterial(external) && OreSourcePolicy.furnaceDecision(external) == Decision.UNKNOWN,
                "unregistered external identity remains unknown, not source NO");
        require(OreSourcePolicy.furnaceInput(external), "unknown early producer retains its legacy MELTING boundary");
        require(FurnaceSmeltingRules.allows(external) == MaterialWorkability.isFurnace(external),
                "unknown form listener retains its distinct old workability boundary");
        Recipe existing = row(input);
        Recipe formListener = row(form(MaterialPrefix.oreRaw, "Galena", 1));
        var clone = new Recipe(existing.mInputs, existing.mOutputs, null, existing.mChances,
                existing.mFluidInputs, existing.mFluidOutputs, existing.mDuration, existing.mEUt, existing.mSpecialValue);
        require(SourceFurnaceNativeAdapter.cooking(clone).isEmpty(),
                "equal inputs and outputs do not grant a foreign recipe the accepted row's passive metadata");
        require(!block(galena, galena, input, new Snapshot()).admit() && row(input) == existing,
                "ordinary source admission never removes an existing furnace collision");

        var vanilla = helper.getLevel().getRecipeManager().byKey(new ResourceLocation("minecraft:stone")).orElseThrow();
        require(vanilla.getType() == RecipeType.SMELTING, "real existing vanilla stone cooking recipe");
        var foreign = new SmeltingRecipe(new ResourceLocation("fixture_provider:galena_smelting"), "fixture",
                CookingBookCategory.MISC, Ingredient.of(input), new ItemStack(Items.DIAMOND), 0.75F, 321);
        var foreignBlast = new BlastingRecipe(new ResourceLocation("fixture_provider:galena_blasting"), "fixture",
                CookingBookCategory.MISC, Ingredient.of(input), new ItemStack(Items.EMERALD), 0.5F, 123);
        var foreignCinnabar = new SmeltingRecipe(new ResourceLocation("fixture_provider:cinnabar_smelting"), "fixture",
                CookingBookCategory.MISC, Ingredient.of(form(MaterialPrefix.crushed, "Cinnabar", 1)),
                new ItemStack(Items.REDSTONE), 0.25F, 222);
        // An unclaimed local list receives the real passive block cooking metadata.
        var unclaimed = new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>();
        OvenRecipeBridge.mirrorIntoFurnaceForTests(unclaimed);
        checkCooking(unclaimed, RecipeType.SMELTING, input, form(MaterialPrefix.nugget, "Lead", 3), 200, 0);
        checkCooking(unclaimed, RecipeType.BLASTING, input, form(MaterialPrefix.nugget, "Lead", 3), 100, 0);
        // Local recipe lists run the actual production mirror. No live RecipeManager or registry writes.
        var recipes = new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(
                List.of(vanilla, foreign, foreignBlast, foreignCinnabar));
        for (int reload = 0; reload < 2; reload++) {
            OvenRecipeBridge.mirrorIntoFurnaceForTests(recipes);
            require(recipes.stream().anyMatch(r -> r == vanilla) && recipes.stream().anyMatch(r -> r == foreign)
                    && recipes.stream().anyMatch(r -> r == foreignBlast) && recipes.stream().anyMatch(r -> r == foreignCinnabar),
                    "source-native metadata/NO permission cannot remove existing vanilla or third-party cooking choices");
            checkCooking(recipes, RecipeType.SMELTING, input, new ItemStack(Items.DIAMOND), 321, 0.75F);
            checkCooking(recipes, RecipeType.BLASTING, input, new ItemStack(Items.EMERALD), 123, 0.5F);
            checkCooking(recipes, RecipeType.SMELTING, form(MaterialPrefix.crushed, "Cinnabar", 1),
                    new ItemStack(Items.REDSTONE), 222, 0.25F);
        }
        require(row(input) == existing, "local cooking mirrors do not mutate the native map's existing source row");
        int applications = MachineRecipeOverlay.applications();
        MachineRecipeOverlay.apply(helper.getLevel().getRecipeManager());
        require(MachineRecipeOverlay.applications() == applications + 1 && row(input) == existing
                && SourceFurnaceNativeAdapter.cooking(existing).isPresent()
                && row(form(MaterialPrefix.oreRaw, "Galena", 1)) == formListener,
                "real baseline reload preserves passive block identity/metadata and independent form-listener row");
        helper.succeed();
    }

    private static void selectedHolderBoundaries() {
        // Controlled selected-holder arguments, not global GTItems rebinding or installed-addon integration.
        var foreign = new ItemStack(Items.FLINT);
        var nickel = material("Nickel");
        for (var prefix : List.of(MaterialPrefix.oreRaw, MaterialPrefix.crushed)) {
            var nativeInput = form(prefix, "Nickel", 1);
            require(nativeForm(prefix, nickel, nativeInput) && !earlyFormAllowed(prefix, nickel, nativeInput),
                    "native Nickel form receives source NO rather than the old early MELTING branch");
            require(!nativeForm(prefix, nickel, foreign) && earlyFormAllowed(prefix, nickel, foreign),
                    "foreign selected holder retains Nickel's old early MELTING boundary");
            require(listenerAllows(prefix, nickel, foreign) == MaterialWorkability.isFurnace(nickel),
                    "foreign selected holder retains the distinct old form-listener gate");
        }
        var stone = material("Stone");
        var nativeStone = form(MaterialPrefix.dust, "Stone", 1);
        require(nativeForm(MaterialPrefix.dust, stone, nativeStone)
                && !earlyFormAllowed(MaterialPrefix.dust, stone, nativeStone)
                && listenerAllows(MaterialPrefix.dust, stone, nativeStone),
                "source-positive native Stone delegates to its listener even without an old workability positive");
        require(!listenerAllows(MaterialPrefix.dust, stone, foreign),
                "foreign Stone holder must not inherit the newly expanded native listener permission");
        require(!nativeForm(MaterialPrefix.oreRaw, nickel, form(MaterialPrefix.dust, "Nickel", 1)),
                "a GT namespace alone does not prove the requested prefix identity");
    }

    private static List<ItemStack> galenaBlocks() {
        var dense = item("gt6ce:crystal_ore_galena");
        var icon = item("gt6ce:block_ore_galena");
        require(dense.getItem() instanceof BlockItem d && d.getBlock() instanceof DenseOreBlock ore
                && ore.material().resolve() == material("Galena"), "actual dense Galena holder");
        require(icon.getItem() instanceof BlockItem b && b.getBlock() instanceof VanillaOreBlock ore
                && ore.material().resolve() == material("Galena"), "actual vanilla-style Galena holder");
        return List.of(ore("Galena"), dense, icon);
    }
    private static void absentOwnedInput(ItemStack input, Iterable<net.minecraft.world.item.crafting.Recipe<?>> recipes) {
        require(rows(input).isEmpty(), "unexpected native Furnace row for " + describe(input));
        for (var recipe : recipes) if (recipe.getId().getNamespace().equals("gt6ce")
                && recipe instanceof AbstractCookingRecipe && matches(recipe, input))
            throw new AssertionError("unexpected generated cooking row " + recipe.getId() + " for " + describe(input));
    }
    private static void outcome(Outcome outcome, Action action) {
        require(outcome.action() == action && outcome.ingot().isEmpty() && outcome.gem().isEmpty() && !outcome.admit(),
                "expected empty " + action + ", actual=" + outcome);
    }
    private static void check(Recipe recipe, ItemStack output) {
        require(recipe.mInputs.length == 1 && recipe.mInputs[0].getCount() == 1 && recipe.mOutputs.length == 1
                && ItemStack.matches(recipe.mOutputs[0], output), "exact native Furnace output for "
                + describe(recipe.mInputs[0]) + ": expected=" + describe(output) + ", actual="
                + (recipe.mOutputs.length == 1 ? describe(recipe.mOutputs[0]) : recipe.mOutputs.length + " slots"));
        require(recipe.mDuration == 16 && recipe.mEUt == 16 && recipe.mFluidInputs.length == 0
                && recipe.mFluidOutputs.length == 0, "native Furnace map adapter stats (not vanilla cooking duration)");
    }
    private static void checkCooking(Iterable<net.minecraft.world.item.crafting.Recipe<?>> recipes, RecipeType<?> type,
                                     ItemStack input, ItemStack output, int ticks, float experience) {
        var matches = new ArrayList<AbstractCookingRecipe>();
        for (var recipe : recipes) if (recipe.getType() == type && matches(recipe, input))
            matches.add((AbstractCookingRecipe) recipe);
        require(matches.size() == 1, type + " expected one cooking recipe for " + describe(input) + ", actual=" + matches.size());
        var recipe = matches.get(0);
        require(ItemStack.matches(recipe.getResultItem(RegistryAccess.EMPTY), output) && recipe.getCookingTime() == ticks
                && recipe.getExperience() == experience, type + " exact output/time/XP for " + describe(input)
                + ": actual=" + describe(recipe.getResultItem(RegistryAccess.EMPTY)) + "/" + recipe.getCookingTime()
                + "/" + recipe.getExperience());
    }
    private static boolean matches(net.minecraft.world.item.crafting.Recipe<?> recipe, ItemStack input) {
        return recipe.getIngredients().size() == 1 && recipe.getIngredients().get(0).test(input);
    }
    private static List<Recipe> rows(ItemStack input) {
        return MachineRecipeMaps.Furnace.mRecipeList.stream().filter(r -> r.mInputs.length == 1
                && r.mFluidInputs.length == 0 && ItemStack.matches(r.mInputs[0], input)).toList();
    }
    private static Recipe row(ItemStack input) {
        var matches = rows(input);
        require(matches.size() == 1, "expected one Furnace row for " + describe(input) + ", actual=" + matches.size());
        require(MachineRecipeMaps.Furnace.findRecipe(List.of(input), List.of(), false, 1, 1) == matches.get(0),
                "actual indexed Furnace lookup for " + describe(input));
        return matches.get(0);
    }
    private static ItemStack ore(String name) {
        var stack = GTBlocks.getStack(BlockMaterialPrefix.ore, material(name));
        require(!stack.isEmpty() && stack.getItem() instanceof BlockItem item && item.getBlock() instanceof OreBlock ore
                && ore.prefix() == BlockMaterialPrefix.ore && ore.material().resolve() == material(name),
                name + " actual registered ordinary ore block identity");
        return stack;
    }
    private static ItemStack form(MaterialPrefix prefix, String name, int count) {
        var stack = GTItems.getStack(prefix, material(name), count);
        require(!stack.isEmpty() && stack.getCount() == count, "actual registered " + prefix.getName() + "/" + name);
        return stack;
    }
    private static GTMaterial material(String name) {
        var material = GTMaterialRegistry.get(name).resolve();
        require(material.isValid() && material.getName().equals(name), "canonical material " + name);
        return material;
    }
    private static ItemStack item(String id) {
        var key = new ResourceLocation(id);
        require(BuiltInRegistries.ITEM.containsKey(key), "registered item " + id);
        return new ItemStack(BuiltInRegistries.ITEM.get(key));
    }
    private static String describe(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount(); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
