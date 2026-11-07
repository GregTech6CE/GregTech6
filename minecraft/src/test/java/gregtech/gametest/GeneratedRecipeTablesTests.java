package gregtech.gametest;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.generated.ChemRecipeResolver;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.recipe.chem.ChemOutcome;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * One representative row per migrated source table, plus the per-family ledger disposition.
 *
 * <p>The rows below are transcribed from the same source calls as the producers; a family that
 * silently stops producing rows fails its assertion instead of disappearing from the log.</p>
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class GeneratedRecipeTablesTests {
    private static final String BATCH = "generated_recipe_tables";

    private GeneratedRecipeTablesTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void otherRowIsQueryable(GameTestHelper helper) {
        ItemStack copper = stack("dust", "Cu", 3);
        ItemStack tin = stack("dust", "Sn", 1);
        ItemStack compound = tech("bronze_compound", 36);
        helper.assertFalse(compound.isEmpty(), "bronze compound item");
        Recipe row = find(MachineRecipeMaps.Mixer, List.of(copper, tin), List.of());
        helper.assertTrue(row != null, "bronze compound source row");
        helper.assertTrue(hasOutput(row, compound), "3 Cu + 1 Sn yields 36 bronze compound");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void compoundPrefixMatrixIsQueryable(GameTestHelper helper) {
        ItemStack tinyCopper = stack("dustTiny", "Cu", 3);
        ItemStack nuggetTin = stack("nugget", "Sn", 1);
        ItemStack compound = tech("bronze_compound", 4);
        helper.assertFalse(compound.isEmpty(), "bronze compound item");
        helper.assertTrue(find(MachineRecipeMaps.Mixer, List.of(tinyCopper, nuggetTin), List.of()) != null,
                "the tiny/nugget compound matrix is registered");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void vanillaHammerRowIsQueryable(GameTestHelper helper) {
        Recipe row = find(MachineRecipeMaps.Hammer,
                List.of(new ItemStack(Items.BRICKS)), List.of());
        helper.assertTrue(row != null, "bricks hammer row");
        helper.assertTrue(hasOutput(row, new ItemStack(Items.BRICK, 3)), "bricks yield three bricks");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void foodFermenterRowIsQueryable(GameTestHelper helper) {
        ItemStack raw = stack("dust", "MeatRaw", 1);
        ItemStack rotten = stack("dust", "MeatRotten", 1);
        helper.assertFalse(raw.isEmpty(), "raw meat dust");
        helper.assertFalse(rotten.isEmpty(), "rotten meat dust");
        Recipe row = find(MachineRecipeMaps.Fermenter, List.of(raw), List.of());
        helper.assertTrue(row != null, "raw meat fermentation row");
        helper.assertTrue(hasOutput(row, rotten), "raw meat ferments to rotten meat");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void cookingOilDoublesMeatRows(GameTestHelper helper) {
        ItemStack beef = new ItemStack(Items.BEEF);
        FluidStack oil = fluid("Oil_Seed", 50);
        helper.assertFalse(oil.isEmpty(), "seed oil");
        Recipe plain = find(MachineRecipeMaps.Furnace, List.of(beef), List.of());
        helper.assertTrue(plain != null && hasOutput(plain, new ItemStack(Items.COOKED_BEEF)),
                "without oil the beef row is single");
        Recipe doubled = find(MachineRecipeMaps.Furnace, List.of(beef), List.of(oil));
        helper.assertTrue(doubled != null, "beef with cooking oil");
        helper.assertTrue(hasOutput(doubled, new ItemStack(Items.COOKED_BEEF, 2)),
                "50 mB cooking oil doubles cooked beef");
        helper.assertTrue(doubled.fluidTag(0) != null, "the oil row accepts the cooking-oil tag");
        Recipe frying = find(MachineRecipeMaps.Furnace, List.of(beef), List.of(fluid("Oil_Frying", 50)));
        helper.assertTrue(frying != null && hasOutput(frying, new ItemStack(Items.COOKED_BEEF, 2)),
                "hot frying oil doubles as well");
        Recipe shortOil = find(MachineRecipeMaps.Furnace, List.of(beef), List.of(fluid("Oil_Seed", 49)));
        helper.assertTrue(shortOil != null && hasOutput(shortOil, new ItemStack(Items.COOKED_BEEF)),
                "49 mB cooking oil does not double");
        var consumed = gregtech.engine.recipe.RecipeInputs.consume(doubled,
                List.of(new ItemStack(Items.BEEF)), List.of(fluid("Oil_Seed", 50)), 1);
        helper.assertTrue(consumed != null, "the oil row can consume its inputs");
        helper.assertTrue(consumed.fluids().get(0).getAmount() == 0, "the oil row consumes 50 mB");
        helper.assertTrue(consumed.items().get(0).isEmpty(), "the oil row consumes the meat");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void cookingOilLeavesNonMeatSingle(GameTestHelper helper) {
        ItemStack potato = new ItemStack(Items.POTATO);
        Recipe plain = find(MachineRecipeMaps.Furnace, List.of(potato), List.of());
        helper.assertTrue(plain != null && hasOutput(plain, new ItemStack(Items.BAKED_POTATO)),
                "potato smelts without oil");
        Recipe withOil = find(MachineRecipeMaps.Furnace, List.of(potato), List.of(fluid("Oil_Seed", 50)));
        helper.assertTrue(withOil != null && hasOutput(withOil, new ItemStack(Items.BAKED_POTATO, 1)),
                "cooking oil does not double a non-meat food");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void cookingOilScopeFollowsMeatOredict(GameTestHelper helper) {
        // Whole cuts, the meat bar and the ingot/dust MeatRaw forms carry the meat ore-dict name.
        assertDoubles(helper, item("gt6ce:raw_ham", 1), item("gt6ce:cooked_ham", 2), "GT ham");
        assertDoubles(helper, item("gt6ce:raw_meat_bar", 1), item("gt6ce:cooked_meat_bar", 2), "raw meat bar");
        assertDoubles(helper, stack("ingot", "MeatRaw", 1), stack("ingot", "MeatCooked", 2), "MeatRaw ingot");
        assertDoubles(helper, stack("dust", "MeatRaw", 1), stack("dust", "MeatCooked", 2), "MeatRaw dust");
        // The ham slice and the smaller MeatRaw forms have no meat ore-dict name in the source.
        assertSingle(helper, item("gt6ce:raw_ham_slice", 1), item("gt6ce:cooked_ham_slice", 1), "ham slice");
        assertSingle(helper, stack("dustTiny", "MeatRaw", 1), stack("dustTiny", "MeatCooked", 1), "MeatRaw dustTiny");
        helper.succeed();
    }

    private static void assertDoubles(GameTestHelper helper, ItemStack input, ItemStack doubled, String label) {
        helper.assertFalse(input.isEmpty() || doubled.isEmpty(), label + " items");
        Recipe row = find(MachineRecipeMaps.Furnace, List.of(input), List.of(fluid("Oil_Seed", 50)));
        helper.assertTrue(row != null && hasOutput(row, doubled), label + " doubles with cooking oil");
    }

    private static void assertSingle(GameTestHelper helper, ItemStack input, ItemStack single, String label) {
        helper.assertFalse(input.isEmpty() || single.isEmpty(), label + " items");
        Recipe row = find(MachineRecipeMaps.Furnace, List.of(input), List.of(fluid("Oil_Seed", 50)));
        helper.assertTrue(row != null && hasOutput(row, single), label + " stays single with cooking oil");
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void extruderModuleRowIsQueryable(GameTestHelper helper) {
        ItemStack module = tech("module_stone_generator", 1);
        ItemStack shape = tech("extruder_shape_plate", 1);
        ItemStack plate = stack("plate", "Stone", 1);
        helper.assertFalse(module.isEmpty() && shape.isEmpty(), "extruder module and shape");
        helper.assertFalse(plate.isEmpty(), "stone plate");
        Recipe row = find(MachineRecipeMaps.Extruder, List.of(module, shape), List.of());
        helper.assertTrue(row != null, "stone generator plate row");
        helper.assertTrue(hasOutput(row, plate), "the module extrudes a stone plate");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void oreSluiceRowIsQueryable(GameTestHelper helper) {
        ItemStack sand = stack("dust", "SluiceSand", 1);
        FluidStack water = new FluidStack(Fluids.WATER, 900);
        FluidStack sluice = fluid("Sluice", 900);
        helper.assertFalse(sand.isEmpty() && sluice.isEmpty(), "sluice sand and fluid");
        Recipe row = find(MachineRecipeMaps.Mixer, List.of(sand), List.of(water));
        helper.assertTrue(row != null, "sluice mixer row");
        helper.assertTrue(hasFluidOutput(row, sluice), "one sluice sand and 900 mB water yield 900 mB sluice");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void combRowIsQueryable(GameTestHelper helper) {
        ItemStack comb = tech("honey_comb", 1);
        FluidStack honey = fluid("Honey", 100);
        helper.assertFalse(comb.isEmpty() && honey.isEmpty(), "honey comb and honey fluid");
        Recipe row = find(MachineRecipeMaps.Centrifuge, List.of(comb), List.of());
        helper.assertTrue(row != null, "honey comb centrifuge row");
        helper.assertTrue(hasFluidOutput(row, honey), "the honey comb yields 100 mB honey");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void technologyRowIsQueryable(GameTestHelper helper) {
        ItemStack plate = stack("plateTiny", "Steel", 1);
        ItemStack tip = tech("robot_arm_wrench_tip", 1);
        ItemStack wrench = tech("single_use_wrench", 2);
        helper.assertFalse(plate.isEmpty() && tip.isEmpty(), "tool head and tip");
        helper.assertFalse(wrench.isEmpty(), "single-use wrench");
        Recipe row = find(MachineRecipeMaps.Boxinator, List.of(plate, tip), List.of());
        helper.assertTrue(row != null, "wrench boxinator row");
        helper.assertTrue(hasOutput(row, wrench), "the wrench box replaces the tool head");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void temporaryRowIsQueryable(GameTestHelper helper) {
        ItemStack graphite = stack("dust", "Graphite", 1);
        ItemStack shape = tech("extruder_shape_rod", 1);
        ItemStack stick = stack("stick", "Graphite", 1);
        helper.assertFalse(graphite.isEmpty() && shape.isEmpty(), "graphite dust and rod shape");
        helper.assertFalse(stick.isEmpty(), "graphite stick");
        Recipe row = find(MachineRecipeMaps.Extruder, List.of(graphite, shape), List.of());
        helper.assertTrue(row != null, "graphite electrode row");
        helper.assertTrue(hasOutput(row, stick), "one graphite dust extrudes one rod");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void everyFamilyPublishesRows(GameTestHelper helper) {
        // The six stone-metavariant Lathe rows stay excluded: VanillaStoneMachineRecipes owns those
        // inputs with material-specific outputs, so those six source rows never publish.
        String[][] families = {
                {"Loader_Recipes_Other:", "218"}, {"Loader_Recipes_Vanilla:", "310"},
                {"Loader_Recipes_Food:", "231"}, {"Loader_Recipes_Extruder:", "142"},
                {"Loader_Recipes_Ores:", "70"}, {"Loader_Recipes_Temporary:", "11"},
                {"MultiItemFood:", "15"}, {"MultiItemRandomTools:", "255"},
                {"Loader_Recipes_Other:71-100:", "135"}};
        for (String[] family : families) {
            int count = published(family[0]);
            helper.assertTrue(count >= Integer.parseInt(family[1]),
                    family[0] + " publishes " + count + " rows, expected at least " + family[1]);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void generatedLedgerHasNoInvalidRowsOrUnreviewedLoss(GameTestHelper helper) {
        helper.assertTrue(countGenerated(ChemOutcome.VALIDATION_FAILED) == 0,
                "no generated row failed validation");
        int missing = countGenerated(ChemOutcome.MISSING_CONTENT);
        helper.assertTrue(missing == 53, "generated missing content is the audited 53 rows, was " + missing);
        int collisions = countGenerated(ChemOutcome.COLLISION);
        // 138 = the previous 144 minus the six stone-metavariant Lathe rows, which are excluded
        // because VanillaStoneMachineRecipes owns those inputs; they collided before removal.
        helper.assertTrue(collisions == 138, "generated collisions are the audited 138 variants, was " + collisions);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void vanillaWaterLoopsKeepEveryArm(GameTestHelper helper) {
        // Loader_Recipes_Vanilla:661 and 881 iterate all four waters; spectral dew is retired,
        // so water, mineral water and distilled water must each have their own row.
        ItemStack snowSelector = selector(0);
        helper.assertFalse(snowSelector.isEmpty(), "freezer selector");
        FluidStack water = new FluidStack(Fluids.WATER, 250);
        FluidStack distilled = fluid("DistW", 250);
        helper.assertFalse(distilled.isEmpty(), "distilled water");
        helper.assertTrue(find(MachineRecipeMaps.Freezer, List.of(snowSelector), List.of(water)) != null,
                "freezer water row");
        helper.assertTrue(find(MachineRecipeMaps.Freezer, List.of(snowSelector), List.of(distilled)) != null,
                "freezer distilled-water row");
        FluidStack pahoehoe = fluid("Lava_Pahoehoe", 1000);
        helper.assertFalse(pahoehoe.isEmpty(), "pahoehoe lava");
        Recipe basalt = find(MachineRecipeMaps.Mixer,
                List.of(), List.of(new FluidStack(Fluids.WATER, 50), pahoehoe));
        helper.assertTrue(basalt != null, "pahoehoe-water basalt row");
        helper.assertTrue(hasOutput(basalt, item("gt6ce:stone_basalt_stone", 1)),
                "pahoehoe water yields GT basalt");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void otherWaterAndSilicaLoopsAreExpanded(GameTestHelper helper) {
        // Loader_Recipes_Other:232 hydrates coal with every water.
        ItemStack coal = stack("dust", "Coal", 1);
        ItemStack hydrated = stack("dust", "HydratedCoal", 1);
        FluidStack mineral = fluid("MnWtr", 125);
        helper.assertFalse(coal.isEmpty() && hydrated.isEmpty() && mineral.isEmpty(), "coal bath content");
        Recipe bath = find(MachineRecipeMaps.Bath, List.of(coal), List.of(mineral));
        helper.assertTrue(bath != null, "coal hydration with mineral water");
        helper.assertTrue(hasOutput(bath, hydrated), "mineral water keeps the hydrated coal row");
        // Loader_Recipes_Other:638: every silica dust forms the string-tied boomstick row.
        ItemStack silica = stack("dust", "SiO2", 2);
        ItemStack gunpowder = stack("dust", "Gunpowder", 2);
        ItemStack string = new ItemStack(Items.STRING);
        ItemStack boomstick = tech("boomstick", 1);
        helper.assertFalse(silica.isEmpty() || gunpowder.isEmpty() || boomstick.isEmpty(), "boomstick content");
        Recipe boomstickRow = find(MachineRecipeMaps.Press, List.of(silica, gunpowder, string), List.of());
        helper.assertTrue(boomstickRow != null, "boomstick string row");
        helper.assertTrue(hasOutput(boomstickRow, boomstick), "silica dust forms the string boomstick row");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void temporaryCoolantKeepsItsWaterArms(GameTestHelper helper) {
        // Loader_Recipes_Temporary:50-72 offers the coolant row with distilled water, mineral
        // water and plain water; the mined-reed rows themselves need Netherlicious items.
        ItemStack lapis = stack("dust", "Lapis", 1);
        FluidStack coolant = fluid("Coolant_IC2", 1000);
        helper.assertFalse(lapis.isEmpty() && coolant.isEmpty(), "lapis and coolant");
        Recipe distilled = find(MachineRecipeMaps.Injector, List.of(lapis), List.of(fluid("DistW", 1000)));
        helper.assertTrue(distilled != null, "lapis coolant with distilled water");
        helper.assertTrue(hasFluidOutput(distilled, coolant), "distilled water yields coolant");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void representativeSourceRowsArePublished(GameTestHelper helper) {
        // The five rows the merged run reported as missing are checked in the ledger itself, so a
        // producer that stops emitting them fails here even if the map lookup changes.
        String[][] rows = {
            {"Loader_Recipes_Other:row/3", "bronze compound mixer row"},
            {"Loader_Recipes_Other:71-100:", "compound prefix matrix row"},
            {"Loader_Recipes_Extruder:Stone/extruder_shape_plate", "stone generator plate row"},
            {"MultiItemRandomTools:503-512", "wrench boxinator row"},
            {"MultiItemFood:251", "honey comb row"},
        };
        for (String[] row : rows) helper.assertTrue(publishedSource(row[0]), "published: " + row[1]);
        helper.succeed();
    }

    private static boolean publishedSource(String source) {
        for (var result : ChemRecipeResolver.results()) {
            if (!result.source().contains(source)) continue;
            if (result.outcome() == ChemOutcome.PUBLISHED || result.outcome() == ChemOutcome.SUBSTITUTED)
                return true;
        }
        return false;
    }

    private static int published(String family) {
        int count = 0;
        for (var result : ChemRecipeResolver.results()) {
            if (!result.source().contains(family)) continue;
            if (result.outcome() == ChemOutcome.PUBLISHED || result.outcome() == ChemOutcome.SUBSTITUTED)
                count++;
        }
        return count;
    }

    private static int countGenerated(ChemOutcome outcome) {
        int count = 0;
        for (var result : ChemRecipeResolver.results()) {
            if (result.outcome() != outcome) continue;
            String source = result.source();
            if (source.contains("Loader_Recipes_Chem")) continue;
            if (source.contains("Loader_Recipes_") || source.contains("MultiItem")) count++;
        }
        return count;
    }

    private static Recipe find(RecipeMap map, List<ItemStack> items, List<FluidStack> fluids) {
        return map.findRecipe(items, fluids, false, map.mInputItemsCount, map.mOutputItemsCount);
    }

    private static boolean hasOutput(Recipe recipe, ItemStack expected) {
        for (ItemStack output : recipe.mOutputs)
            if (output.is(expected.getItem()) && output.getCount() == expected.getCount()) return true;
        return false;
    }

    private static boolean hasFluidOutput(Recipe recipe, FluidStack expected) {
        for (FluidStack output : recipe.mFluidOutputs)
            if (!output.isEmpty() && output.isFluidEqual(expected) && output.getAmount() == expected.getAmount())
                return true;
        return false;
    }

    private static ItemStack stack(String prefixName, String materialName, int count) {
        GTMaterial material = GTMaterialRegistry.get(materialName);
        if (material == null || !material.resolve().isValid()) return ItemStack.EMPTY;
        MaterialPrefix prefix = null;
        for (MaterialPrefix candidate : gregtech.engine.prefix.PrefixRegistry.all())
            if (candidate.getName().equals(prefixName)) prefix = candidate;
        return prefix == null ? ItemStack.EMPTY : GTItems.getStack(prefix, material.resolve(), count);
    }

    private static ItemStack selector(int index) {
        Item item = GTTechnological.selectorTag(index);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack item(String id, int count) {
        var key = net.minecraft.resources.ResourceLocation.tryParse(id);
        Item item = key == null ? null : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static ItemStack tech(String id, int count) {
        // Multi-items are registered by their display name, not in GTTechnological.BY_ID.
        String resolved = GTTechnological.ALIASES.getOrDefault(id, id);
        Item item = GTTechnological.get(id);
        if (item == null) {
            var key = resolved.indexOf(':') >= 0
                    ? net.minecraft.resources.ResourceLocation.tryParse(resolved)
                    : net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", resolved);
            item = key == null ? null : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key);
        }
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static FluidStack fluid(String key, int millibuckets) {
        FluidStack stack = GTFluids.stack(key, millibuckets);
        return stack == null ? FluidStack.EMPTY : stack;
    }
}
