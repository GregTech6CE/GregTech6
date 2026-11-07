package gregtech.gametest;

import gregtech.api.material.GTValues;
import gregtech.content.block.machine.MoldBlock;
import gregtech.content.block.machine.MoldItemData;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.GTToolItem;
import gregtech.content.material.Materials;
import gregtech.content.recipe.MachineRecipeOverlay;
import gregtech.content.recipe.MaterialRecoveryRules;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import gregtech.engine.machine.crucible.CrucibleItemInput;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.tool.GTToolType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Observations and correctness checks are separate; known differences are named in each assertion. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MaterialBaselineTests {
    private static final String BATCH = "material_baseline";

    private MaterialBaselineTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void materialSpiInstallsLinkedCompositionAndGeneratedForm(GameTestHelper helper) {
        var material = gregtech.engine.material.GTMaterialRegistry.get("BaselineRuntimeCompound");
        helper.assertTrue(material.isValid(), "SPI material installed");
        helper.assertTrue("material_baseline_test".equals(gregtech.engine.addon.AddonMaterials.owner(material)), "SPI ownership");
        helper.assertTrue(!gregtech.engine.material.GTMaterialRegistry.isCoreMaterial(material), "addon identity is separate from core");
        helper.assertTrue(material.getCompositionComponents().size() == 2 && material.getCompositionDivider() == 3, "SPI composition linked");
        var dust = GTItems.getStack(MaterialPrefix.dust, material, 3);
        helper.assertFalse(dust.isEmpty(), "host generated the requested dust form");
        helper.assertTrue(find(MachineRecipeMaps.Centrifuge, List.of(dust), List.of()) != null, "SPI decomposition is queryable");
        observe(helper, "spi_dust", dust);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void storageBlocksExposeCompositionAndBothAdmissions(GameTestHelper helper) {
        var iron = ItemMaterialRegistry.get(new ItemStack(Items.IRON_BLOCK)).orElseThrow();
        helper.assertTrue(iron.components().size() == 1 && iron.material().resolve() == Materials.Iron.resolve()
                && iron.amount() == 9 * GTValues.U, "iron block contains exactly 9U Iron");
        helper.assertTrue(CrucibleItemInput.isValid(new ItemStack(Items.IRON_BLOCK)), "crucible admits iron block");
        for (Item item : List.of(Items.IRON_BLOCK, Items.COPPER_BLOCK)) {
            var stack = new ItemStack(item);
            helper.assertTrue(ItemMaterialRegistry.get(stack).isPresent(), "missing composition: " + id(stack));
            observe(helper, id(stack), stack);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void seventyTwoUnitsExposePlanAndQueryableOutput(GameTestHelper helper) {
        var stack = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
        var data = ItemMaterialRegistry.get(stack).orElseThrow();
        helper.assertTrue(data.components().size() == 1 && data.material().resolve() == Materials.Gold.resolve()
                && data.amount() == 72 * GTValues.U, "enchanted golden apple carries 72U Gold");
        var plan = MaterialRecoveryRules.dust(data.amount());
        helper.assertTrue(plan != null && plan.count() == 64 && plan.prefix() == MaterialPrefix.dust,
                "observed difference: current 72U dust planner caps at 64U; phase 05 owns the correction");
        var recipe = find(MachineRecipeMaps.Shredder, List.of(stack), List.of());
        helper.assertTrue(recipe != null, "72U item has a queryable recycling recipe");
        helper.assertTrue(recipe.mOutputs[0].is(GTItems.getStack(MaterialPrefix.dust, Materials.Gold, 1).getItem())
                && recipe.mOutputs[0].getCount() == 64, "observed difference: final shredder output also caps at 64U Gold");
        write(helper, "72u.tsv", List.of("input\t" + describe(stack), "plan\t" + plan.prefix().getName() + "\t" + plan.count(),
                "lookup\t" + (recipe == null ? "absent" : signature(MachineRecipeMaps.Shredder, recipe))));
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void compositeHeadKeepsEveryComponentAcrossQueries(GameTestHelper helper) {
        var stack = GTItems.getStack(MaterialPrefix.toolHeadDrill, Materials.Titanium, 1);
        helper.assertFalse(stack.isEmpty(), "composite drill head exists");
        var explicit = ItemMaterialRegistry.explicit(stack.getItem()).orElseThrow();
        var actual = ItemMaterialRegistry.get(stack).orElseThrow();
        helper.assertTrue(explicit.equals(actual), "registry and pristine stack agree");
        helper.assertTrue(actual.components().size() == 2, "drill head has two components");
        helper.assertTrue(actual.components().get(0).material().resolve() == Materials.Titanium.resolve()
                && actual.components().get(0).amount() == 4 * GTValues.U, "primary: 4U Titanium");
        helper.assertTrue(actual.components().get(1).material().resolve() == Materials.Steel.resolve()
                && actual.components().get(1).amount() == 4 * GTValues.U, "secondary: 4U Steel");
        var form = MaterialEquivalence.form(stack);
        helper.assertTrue(form != null && form.prefix() == MaterialPrefix.toolHeadDrill,
                "form query represents the primary identity; composition retains the steel");
        observe(helper, "composite_head", stack);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void damagedToolsAndContainersExposeStateAdmission(GameTestHelper helper) {
        var tool = new ItemStack(Items.IRON_PICKAXE);
        helper.assertTrue(ItemMaterialRegistry.get(tool).isPresent(), "iron pickaxe composition exists");
        var full = ItemMaterialRegistry.get(tool).orElseThrow();
        observe(helper, "tool_full", tool);
        tool.setDamageValue(tool.getMaxDamage() / 2);
        var half = ItemMaterialRegistry.get(tool).orElseThrow();
        for (int i = 0; i < full.components().size(); i++)
            helper.assertTrue(half.components().get(i).amount() == full.components().get(i).amount()
                    * (tool.getMaxDamage() - tool.getDamageValue()) / tool.getMaxDamage(), "damage scales component " + i);
        observe(helper, "tool_partial", tool);
        tool.setDamageValue(tool.getMaxDamage());
        helper.assertTrue(ItemMaterialRegistry.get(tool).orElseThrow().components().isEmpty(), "exhausted tool has no positive material");
        helper.assertFalse(ItemMaterialRegistry.canRecover(tool), "no recovery from exhausted tool");
        observe(helper, "tool_exhausted", tool);
        var nativeTool = GTToolItem.create(GTToolType.PICKAXE, Materials.Iron, gregtech.engine.material.GTMaterialRegistry.get("Wood"));
        helper.assertFalse(nativeTool.isEmpty(), "native material tool exists");
        helper.assertTrue(ItemMaterialRegistry.get(nativeTool).isEmpty(), "observed difference: native tool instance has no registry composition");
        observe(helper, "native_tool", nativeTool);
        var empty = new ItemStack(Items.BUCKET);
        var filled = new ItemStack(Items.WATER_BUCKET);
        helper.assertTrue(CrucibleItemInput.isValid(empty), "empty bucket is meltable");
        helper.assertFalse(CrucibleItemInput.isValid(filled), "filled bucket is protected");
        observe(helper, "bucket_empty", empty);
        observe(helper, "bucket_filled", filled);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void shapedMoldsExposeShapeCompositionAndRejection(GameTestHelper helper) {
        var block = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", "mold_stone"));
        helper.assertTrue(block instanceof MoldBlock, "stone casting mold is registered on both nodes");
        var empty = new ItemStack(block);
        int mask = gregtech.content.blockentity.machine.MoldBlockEntity.randomDungeonShape(net.minecraft.util.RandomSource.create(0));
        var shaped = MoldItemData.withShape(empty.copy(), mask);
        var composition = ItemMaterialRegistry.get(empty).orElseThrow();
        helper.assertTrue(composition.components().size() == 1 && composition.amount() == 45 * GTValues.U,
                "stone mold carries its 45U construction composition");
        helper.assertTrue(ItemMaterialRegistry.canRecover(empty), "plain stone mold is admitted for recovery");
        helper.assertTrue(MoldItemData.shape(shaped) == mask, "casting cavity survives on the item");
        helper.assertTrue(ItemMaterialRegistry.get(empty).equals(ItemMaterialRegistry.get(shaped)), "shape keeps composition");
        helper.assertTrue(ItemMaterialRegistry.hasStoredContents(shaped), "observed difference: cavity data is treated as stored contents");
        helper.assertFalse(CrucibleItemInput.isValid(shaped), "observed shaped casting mold rejection (#349)");
        observe(helper, "casting_mold_empty", empty);
        observe(helper, "casting_mold_shaped", shaped);
        var press = GTTechnological.get("press_bullet_casing_shape_small");
        helper.assertTrue(press != null, "press mold is registered");
        helper.assertTrue(ItemMaterialRegistry.get(new ItemStack(press)).isEmpty(),
                "observed difference: registered press mold has no composition (#350)");
        observe(helper, "press_mold", new ItemStack(press));
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void solidAndFluidDecompositionAreQueryable(GameTestHelper helper) {
        var salt = GTItems.getStack(MaterialPrefix.dust, gregtech.engine.material.GTMaterialRegistry.get("Powellite"), 6);
        var solid = find(MachineRecipeMaps.Electrolyzer,
                List.of(new ItemStack(GTTechnological.selectorTag(0)), salt), List.of());
        helper.assertTrue(solid != null, "Powellite decomposition is queryable with mode 0 (accepted #318 rule)");
        var water = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 3000);
        var liquid = find(MachineRecipeMaps.Electrolyzer,
                List.of(new ItemStack(GTTechnological.selectorTag(0))), List.of(water));
        helper.assertTrue(liquid != null, "water decomposition is queryable with fluid input");
        helper.assertTrue(Arrays.stream(solid.mOutputs).anyMatch(s -> s != null && !s.isEmpty())
                || Arrays.stream(solid.mFluidOutputs).anyMatch(s -> s != null && !s.isEmpty()), "solid decomposition has outputs");
        helper.assertTrue(Arrays.stream(liquid.mFluidOutputs).anyMatch(s -> s != null && !s.isEmpty()), "water decomposition has fluid outputs");
        write(helper, "decomposition.tsv", List.of(signature(MachineRecipeMaps.Electrolyzer, solid), signature(MachineRecipeMaps.Electrolyzer, liquid)));
        helper.succeed();
    }

    // GameTest executes batches sequentially. This batch contains only this asynchronous reload test.
    @GameTest(template = "test_empty", batch = "material_baseline_reload", timeoutTicks = 2400)
    public static void realResourceReloadDoesNotAccumulateRules(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var packs = List.copyOf(server.getPackRepository().getSelectedIds());
        int applications = MachineRecipeOverlay.applications();
        capture(helper, "startup");
        var before = ownedCounts();
        var sample = describe(new ItemStack(Items.IRON_BLOCK));
        server.reloadResources(packs).thenCompose(ignored -> server.reloadResources(packs)).whenComplete((ignored, failure) -> server.execute(() -> {
            if (failure != null) { helper.fail("resource reload failed: " + failure); return; }
            try {
                helper.assertTrue(MachineRecipeOverlay.applications() >= applications + 2, "both real reloads applied the overlay");
                helper.assertTrue(before.equals(ownedCounts()), "owned rules do not accumulate: " + before + " -> " + ownedCounts());
                helper.assertTrue(sample.equals(describe(new ItemStack(Items.IRON_BLOCK))), "fixed composition is stable after reload");
                helper.assertTrue(find(MachineRecipeMaps.Shredder, List.of(new ItemStack(Items.IRON_BLOCK)), List.of()) != null,
                        "iron block recycling remains queryable after reload");
                capture(helper, "reload");
                helper.succeed();
            } catch (Throwable error) { helper.fail("reload assertion: " + error); }
        }));
    }

    private static List<Integer> ownedCounts() {
        return List.of(gregtech.content.recipe.ExternalOreProcessing.owned().size(),
                gregtech.content.recipe.SharedThermalInputRows.owned().size());
    }

    private static Recipe find(RecipeMap map, List<ItemStack> items, List<FluidStack> fluids) {
        return map.findRecipe(items, fluids, false, map.mInputItemsCount, map.mOutputItemsCount);
    }

    private static String id(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }

    private static String describe(ItemStack stack) {
        var data = ItemMaterialRegistry.get(stack).orElse(null);
        String state = "\tdamage=" + stack.getDamageValue() + "/" + stack.getMaxDamage() + "\tshape=" + MoldItemData.shape(stack);
        if (data == null) return id(stack) + "\tabsent" + state;
        return id(stack) + "\t" + (data.prefix() == null ? "-" : data.prefix().getName())
                + "\t" + data.material().resolve().getName() + "\t" + data.components().stream()
                .map(c -> c.material().resolve().getName() + "=" + c.amount()).sorted().collect(Collectors.joining(","))
                + "\t" + data.recoverable() + "\t" + ItemMaterialRegistry.canRecover(stack)
                + "\t" + ItemMaterialRegistry.canMelt(stack) + "\t" + ItemMaterialRegistry.hasStoredContents(stack)
                + "\t" + CrucibleItemInput.parse(stack).stream().map(c -> c.material.getName() + "=" + c.amount).sorted().collect(Collectors.joining(","))
                + "\t" + data.source() + state;
    }

    private static void observe(GameTestHelper helper, String name, ItemStack stack) {
        write(helper, name.replace(':', '_') + ".tsv", List.of(describe(stack)));
    }

    private static String signature(RecipeMap map, Recipe row) {
        var slots = new ArrayList<String>();
        for (int i = 0; i < row.mInputs.length; i++) {
            var unit = row.materialInputDisplay(i);
            // A tag's representative is selected from an unordered set; it is not the input contract.
            String input = row.itemTag(i) == null ? item(row.mInputs[i])
                    : "tag:" + row.itemTag(i).location() + "*" + row.mInputs[i].getCount();
            slots.add("in[" + i + "]=" + input + (unit == null ? "" : ":unit:" + unit.amount() + ":" + unit.rounding())
                    + ":tag=" + (row.itemTag(i) == null ? "-" : row.itemTag(i).location())
                    + ":catalyst=" + row.isCatalystInput(i) + ":guard=" + row.hasItemInputGuard(i));
        }
        for (int i = 0; i < row.mOutputs.length; i++) slots.add("out[" + i + "]=" + item(row.mOutputs[i]) + ":chance=" + row.getOutputChance(i));
        for (int i = 0; i < row.mFluidInputs.length; i++) slots.add("fluidIn[" + i + "]="
                + (row.fluidTag(i) == null ? fluid(row.mFluidInputs[i])
                    : "tag:" + row.fluidTag(i).location() + "*" + row.mFluidInputs[i].getAmount())
                + ":tag=" + (row.fluidTag(i) == null ? "-" : row.fluidTag(i).location())
                + ":catalyst=" + row.isCatalystFluidInput(i) + ":presence=" + row.isPresenceFluidInput(i));
        for (int i = 0; i < row.mFluidOutputs.length; i++) slots.add("fluidOut[" + i + "]=" + fluid(row.mFluidOutputs[i]));
        return map.mNameInternal + "\t" + String.join(";", slots) + "\t" + row.mDuration + "\t" + row.mEUt + "\t" + row.mSpecialValue
                + "\t" + row.mEnabled + "/" + row.mFakeRecipe + "/" + row.mNeedsEmptyOutput + "/" + row.mMaterialRecovery
                + "/" + row.mRequiresEmptyContainerInputs + "/" + row.mExactItemInputs + "/" + row.mExplicitCatalystsOnly;
    }

    private static String item(ItemStack stack) { return stack == null || stack.isEmpty() ? "-" : id(stack) + "*" + stack.getCount(); }
    private static String fluid(FluidStack stack) { return stack == null || stack.isEmpty() ? "-" : BuiltInRegistries.FLUID.getKey(stack.getFluid()) + "*" + stack.getAmount(); }

    private static void capture(GameTestHelper helper, String phase) {
        write(helper, phase + "-items.tsv", BuiltInRegistries.ITEM.stream().map(ItemStack::new)
                .filter(s -> ItemMaterialRegistry.base(s.getItem()).isPresent()).map(MaterialBaselineTests::describe).sorted().toList());
        var rows = new ArrayList<String>();
        for (var map : RecipeMap.RECIPE_MAP_LIST) for (var row : map.mRecipeList) rows.add(signature(map, row));
        rows.sort(String::compareTo);
        write(helper, phase + "-recipes.tsv", rows);
        var tags = new java.util.TreeMap<String, String>();
        for (var map : RecipeMap.RECIPE_MAP_LIST) for (var row : map.mRecipeList) {
            for (int i = 0; i < row.mInputs.length; i++) {
                var tag = row.itemTag(i);
                if (tag != null) tags.computeIfAbsent("item:" + tag.location(), key -> BuiltInRegistries.ITEM.getTag(tag)
                        .map(set -> set.stream().map(holder -> BuiltInRegistries.ITEM.getKey(holder.value()).toString())
                                .sorted().collect(Collectors.joining(","))).orElse(""));
            }
            for (int i = 0; i < row.mFluidInputs.length; i++) {
                var tag = row.fluidTag(i);
                if (tag != null) tags.computeIfAbsent("fluid:" + tag.location(), key -> BuiltInRegistries.FLUID.getTag(tag)
                        .map(set -> set.stream().map(holder -> BuiltInRegistries.FLUID.getKey(holder.value()).toString())
                                .sorted().collect(Collectors.joining(","))).orElse(""));
            }
        }
        write(helper, phase + "-tags.tsv", tags.entrySet().stream().map(entry -> entry.getKey() + "\t" + entry.getValue()).toList());
        write(helper, phase + "-forms.tsv", BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof gregtech.engine.material.MaterialFormItem)
                .map(item -> {
                    var form = (gregtech.engine.material.MaterialFormItem) item;
                    return BuiltInRegistries.ITEM.getKey(item) + "\t" + form.getPrefix().getName() + "\t"
                            + form.getMaterial().resolve().getName() + "\t" + form.getPrefix().getMaterialWeight()
                            + "\t" + form.getPrefix().isValidFor(form.getMaterial());
                }).sorted().toList());
        write(helper, phase + "-block-forms.tsv", BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof gregtech.content.block.MaterialBlockLike)
                .map(block -> {
                    var form = (gregtech.content.block.MaterialBlockLike) block;
                    return BuiltInRegistries.BLOCK.getKey(block) + "\t" + BuiltInRegistries.ITEM.getKey(block.asItem())
                            + "\t" + form.prefix().getName() + "\t" + form.material().resolve().getName()
                            + "\t" + form.prefix().getMaterialWeight() + "\t" + form.prefix().isValidFor(form.material());
                }).sorted().toList());
    }

    private static void write(GameTestHelper helper, String file, List<String> rows) {
        try {
            Path run = Path.of("").toAbsolutePath();
            Path root = run;
            while (root != null && !Files.isRegularFile(root.resolve("settings.gradle"))) root = root.getParent();
            if (root == null) throw new IllegalStateException("cannot find repository root for baseline output");
            Path output = root.resolve("work/material-system-refactor/runtime")
                    .resolve(net.minecraft.SharedConstants.getCurrentVersion().getName()).resolve(run.getFileName());
            Files.createDirectories(output);
            Files.write(output.resolve(file), rows);
        } catch (Exception failure) { helper.fail("cannot write material baseline: " + failure); }
    }
}
