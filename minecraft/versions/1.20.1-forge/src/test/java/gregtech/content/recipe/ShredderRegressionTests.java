package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import static gregtech.content.recipe.SourceOreMacerationAdapter.*;
import static gregtech.content.recipe.SharedOreWashingRows.NativeForm;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Native six-prefix handlers from Handlers138-150; not generic recycling or the queued host-ore rewrite. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ShredderRegressionTests {
    private ShredderRegressionTests() {}
    private static final MaterialPrefix[] INPUTS = {MaterialPrefix.crushed, MaterialPrefix.crushedPurified,
            MaterialPrefix.crushedCentrifuged, MaterialPrefix.crushedTiny, MaterialPrefix.crushedPurifiedTiny,
            MaterialPrefix.crushedCentrifugedTiny};
    private static final long[] FAST = {18, 20, 22, 2, 3, 3};
    private static final long[] FAST_QUALITY_TWO = {54, 60, 66, 6, 7, 8};
    private static final long[] FAST_QUALITY_THREE = {72, 80, 88, 8, 9, 10};
    private static final long[] SLOW = {288, 320, 352, 32, 36, 40};

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void sourceShredderAfterStartup(GameTestHelper helper) {
        require(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY
                && MachineRecipeOverlay.applications() > 0, "native registration and server overlay must finish");
        // Preserve all twelve earlier reviewed profiles, using independent source literals rather than planner output.
        for (String name : List.of("Cinnabar", "Copper", "Galena", "Chalcopyrite", "Pentlandite", "Sphalerite",
                "Tetrahedrite", "Pyrite", "Cooperite", "YellowLimonite")) golden(name, name, FAST);
        golden("Nickel", "Nickel", FAST_QUALITY_TWO);
        golden("NetherQuartz", "NetherQuartz", FAST_QUALITY_THREE);
        // Original Boron metalloid factory:2349K, no added MORTAR; actual input quality0.
        require(material("Boron").getToolQuality() == 0, "runtime Boron quality0");
        golden("Boron", "Boron", SLOW);
        require(material("ChargedCertusQuartz").getTargetPulverMaterial().resolve() == material("CertusQuartz"),
                "actual charged quartz pulver target differs from its input material");
        golden("ChargedCertusQuartz", "CertusQuartz", FAST);
        // Default generation exclusions do not prohibit handlers for forms that already exist.
        for (String name : List.of("Iron", "Aluminium")) {
            for (var prefix : INPUTS) require(ReviewedCrushedFormGeneration.sourceExcluded(material(name), prefix),
                    name + " is in the source item-generation exclusion list");
            golden(name, name, FAST_QUALITY_TWO);
        }
        copperRock();
        unchangedCinnabarOreRoutes();
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void requiredFormsBoundariesAndReload(GameTestHelper helper) {
        var copper = material("Copper");
        MaterialPrefix[] required = {MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72};
        int[][] affected = {{0, 1, 2}, {1, 2}, {0, 2, 3, 4, 5}, {0, 2, 3, 4, 5}};
        for (int missing = 0; missing < required.length; missing++) {
            var absent = required[missing];
            var snapshot = new Snapshot() {
                @Override NativeForm direct(MaterialPrefix prefix, GTMaterial material) {
                    return material == copper && prefix == absent ? NativeForm.absent() : super.direct(prefix, material);
                }
            };
            for (int shape = 0; shape < INPUTS.length; shape++) {
                var plan = shredder(copper, INPUTS[shape], form(INPUTS[shape], "Copper", 1), snapshot);
                final int index = shape;
                if (Arrays.stream(affected[missing]).anyMatch(value -> value == index)) noRow(plan, Action.DEFERRED_NO_ROW);
                else definition(plan, FAST[shape], outputs("Copper", shape));
            }
        }
        var input = form(MaterialPrefix.crushed, "Copper", 1);
        noRow(shredder(copper, INPUTS[0], ItemStack.EMPTY, new Snapshot()), Action.DEFERRED_NO_ROW);
        noRow(shredder(copper, INPUTS[0], new ItemStack(Items.FLINT), new Snapshot()), Action.LEGACY_UNSUPPORTED);
        var unknown = new GTMaterial(31000, "ShredderExternalFixture", "Unregistered external fixture", 0xFFFFFF);
        noRow(shredder(unknown, INPUTS[0], new ItemStack(Items.FLINT), new Snapshot()), Action.LEGACY_UNSUPPORTED);
        noRow(shredder(copper, INPUTS[0], input, new Snapshot() {
            @Override Decision mortar(GTMaterial material) { return Decision.UNKNOWN; }
        }), Action.LEGACY_UNSUPPORTED);
        noRow(shredder(copper, INPUTS[0], input, new Snapshot() {
            @Override boolean anti(GTMaterial material) { return true; }
        }), Action.SOURCE_NO_ROW);
        noRow(shredder(copper, INPUTS[0], input, new Snapshot() {
            @Override GTMaterial target(GTMaterial material) { return null; }
        }), Action.SOURCE_NO_ROW);
        noRow(shredder(copper, INPUTS[0], input, new Snapshot() {
            @Override NativeForm direct(MaterialPrefix prefix, GTMaterial material) {
                return prefix == MaterialPrefix.dust ? NativeForm.unknown() : super.direct(prefix, material);
            }
        }), Action.LEGACY_UNSUPPORTED);
        var acceptedOrChanged = new Snapshot() {
            @Override boolean overridden(GTMaterial material, Collection<MaterialPrefix> prefixes) { return true; }
        };
        noRow(shredder(copper, INPUTS[0], input, acceptedOrChanged), Action.LEGACY_UNSUPPORTED);
        require(!retireRock(copper, form(MaterialPrefix.rockGt, "Copper", 1), acceptedOrChanged),
                "accepted/changed form evidence preserves the old rock producer boundary");
        require(retireRock(material("Boron"), form(MaterialPrefix.rockGt, "Boron", 1), new Snapshot()),
                "rock Crusher ownership does not require MORTAR YES");
        // Local input models vary runtime amount and quality; only holder/identity resolution is controlled.
        // No registered material, accepted-form list or item composition is mutated.
        for (long amount : new long[]{0, GTValues.U / 2, 2 * GTValues.U}) {
            var model = new GTMaterial(290, "Copper", "Local copper model", 0xFFFFFF)
                    .setToolQuality(3).setPulver(material("Gold"), amount);
            var snapshot = new Snapshot() {
                @Override boolean identity(GTMaterial material) { return material == model || super.identity(material); }
                @Override NativeForm direct(MaterialPrefix prefix, GTMaterial material) {
                    return super.direct(prefix, material == model ? copper : material);
                }
            };
            for (int shape = 0; shape < INPUTS.length; shape++)
                definition(shredder(model, INPUTS[shape], form(INPUTS[shape], "Copper", 1), snapshot),
                        FAST_QUALITY_THREE[shape], outputs("Gold", shape));
            var rockOutput = MortarGrindingRecipes.pulverize(model, 9 * GTValues.U / 4);
            var expected = amount == 0 ? ItemStack.EMPTY : amount == GTValues.U / 2
                    ? form(MaterialPrefix.dustTiny, "Gold", 10) : form(MaterialPrefix.dustSmall, "Gold", 18);
            require(ItemStack.matches(rockOutput, expected), "existing rock remains, unlike fixed six rows, scale pulver amount");
        }
        dormantContinuation();
        Recipe existing = row(MachineRecipeMaps.Shredder, input), foreign = null;
        Recipe rock = row(MachineRecipeMaps.Crusher, form(MaterialPrefix.rockGt, "Copper", 1));
        Recipe raw = row(MachineRecipeMaps.Crusher, form(MaterialPrefix.oreRaw, "Cinnabar", 1));
        try {
            require(MachineRecipeMaps.Shredder.unlink(existing), "isolate one ordinary collision fixture");
            foreign = new Recipe(new ItemStack[]{input.copy()}, new ItemStack[]{new ItemStack(Items.DIAMOND)},
                    null, new long[]{10000}, existing.mFluidInputs, existing.mFluidOutputs, 17, 7, 0);
            require(MachineRecipeMaps.Shredder.addRecipe(foreign) == foreign, "foreign collision admitted normally");
            require(shredder(copper, INPUTS[0], input, new Snapshot()).row().admit() == null
                    && row(MachineRecipeMaps.Shredder, input) == foreign, "source producer never replaces a foreign collision");
        } finally {
            if (foreign != null) MachineRecipeMaps.Shredder.unlink(foreign);
            if (!MachineRecipeMaps.Shredder.mRecipeList.contains(existing)) MachineRecipeMaps.Shredder.add(existing);
        }
        for (int reload = 0; reload < 2; reload++) {
            MachineRecipeOverlay.apply(helper.getLevel().getRecipeManager());
            require(row(MachineRecipeMaps.Shredder, input) == existing
                    && row(MachineRecipeMaps.Crusher, form(MaterialPrefix.rockGt, "Copper", 1)) == rock
                    && row(MachineRecipeMaps.Crusher, form(MaterialPrefix.oreRaw, "Cinnabar", 1)) == raw,
                    "startup source, existing rock family and reviewed raw identities survive actual baseline reload");
        }
        golden("Copper", "Copper", FAST);
        copperRock();
        unchangedCinnabarOreRoutes();
        helper.succeed();
    }

    private static void dormantContinuation() {
        long reviewed = GTMaterialRegistry.allMaterials().stream().map(GTMaterial::resolve).distinct()
                .filter(m -> ReviewedOreInputProfile.decision(m) == ReviewedOreInputProfile.Evidence.NO).count();
        require(reviewed == 53, "the existing53 non-ore policy roster is unchanged");
        var ice = material("Ice");
        var snapshot = new Snapshot();
        for (var prefix : INPUTS) {
            var input = form(prefix, "Ice", 1);
            noRow(shredder(ice, prefix, input, snapshot), Action.LEGACY_UNSUPPORTED);
            require(rows(MachineRecipeMaps.Shredder, input).isEmpty() && rows(MachineRecipeMaps.Crusher, input).isEmpty(),
                    "default Ice legacy ore forms remain dormant, not newly source-owned");
        }
        // Exercise the existing ledger with one exact old OPL definition pair and controlled activation.
        // This is not an assertion that an external provider or accepted form was globally installed.
        var input = form(MaterialPrefix.crushed, "Ice", 1);
        var ledger = new DormantOreRows();
        for (var map : List.of(MachineRecipeMaps.Crusher, MachineRecipeMaps.Shredder))
            ledger.define(ice, "controlled-old-maceration", map, true, input,
                    new ItemStack[]{form(MaterialPrefix.dust, "Ice", 1)}, new long[]{10000}, null, 64, 16);
        ledger.seal();
        try {
            require(ledger.reconcile(material -> false) == 0, "controlled dormant state emits no old rows");
            for (int activation = 0; activation < 2; activation++) {
                require(ledger.reconcile(material -> material == ice) == 2, "continuation keeps two old compatibility rows without accumulation");
                for (var map : List.of(MachineRecipeMaps.Crusher, MachineRecipeMaps.Shredder))
                    check(row(map, input), 64, new ItemStack[]{form(MaterialPrefix.dust, "Ice", 1)});
            }
        } finally { ledger.clear(); }
        require(rows(MachineRecipeMaps.Shredder, input).isEmpty() && rows(MachineRecipeMaps.Crusher, input).isEmpty(),
                "controlled continuation fixture restores the actual dormant default");
    }
    private static void noRow(Outcome plan, Action action) {
        require(plan.action() == action && plan.row() == null, "expected " + action + ", actual=" + plan);
    }
    private static void definition(Outcome plan, long ticks, ItemStack[] outputs) {
        require(plan.action() == Action.SOURCE_ROW && plan.row() != null && plan.row().duration() == ticks,
                "expected source definition duration" + ticks + ", actual=" + plan);
        var actual = plan.row().outputs();
        require(actual.length == outputs.length, "controlled exact ordered output slots");
        for (int slot = 0; slot < outputs.length; slot++) require(ItemStack.matches(actual[slot], outputs[slot]),
                "controlled slot" + slot + " expected=" + describe(outputs[slot]) + ", actual=" + describe(actual[slot]));
    }

    private static void golden(String inputMaterial, String outputMaterial, long[] ticks) {
        for (int shape = 0; shape < INPUTS.length; shape++) {
            var input = form(INPUTS[shape], inputMaterial, 1);
            check(row(MachineRecipeMaps.Shredder, input), ticks[shape], outputs(outputMaterial, shape));
            require(rows(MachineRecipeMaps.Crusher, input).isEmpty(),
                    "owned maceration input must not retain the synthetic OPL Crusher row: " + describe(input));
        }
    }
    private static ItemStack[] outputs(String material, int shape) {
        var d = MaterialPrefix.dust; var s = MaterialPrefix.dustSmall;
        var t = MaterialPrefix.dustTiny; var v = MaterialPrefix.dustDiv72;
        MaterialPrefix[][] prefixes = {{d, t, v}, {d, s}, {d, s, t, v}, {t, v}, {t, v, v}, {t, v, v, v}};
        return Arrays.stream(prefixes[shape]).map(prefix -> form(prefix, material, 1)).toArray(ItemStack[]::new);
    }
    private static void copperRock() {
        var rock = form(MaterialPrefix.rockGt, "Copper", 1);
        var recipe = row(MachineRecipeMaps.Crusher, rock);
        check(recipe, 36, new ItemStack[]{form(MaterialPrefix.dustSmall, "Copper", 9)});
        require(CrusherFamilyRecipes.entries().stream().anyMatch(e -> e.recipe() == recipe
                && e.route().equals("rockGt -> remains")), "existing CrusherFamily producer owns the accepted Copper rock row");
        require(rows(MachineRecipeMaps.Shredder, rock).isEmpty() && rows(MachineRecipeMaps.Hammer, rock).isEmpty(),
                "no invented native rock Shredder or Hammer fallback");
    }
    private static void unchangedCinnabarOreRoutes() {
        var crushed = form(MaterialPrefix.crushed, "Cinnabar", 1);
        var raw = form(MaterialPrefix.oreRaw, "Cinnabar", 1);
        var rawRecipe = row(MachineRecipeMaps.Crusher, raw);
        check(rawRecipe, 128, new ItemStack[]{crushed, crushed.copy(), form(MaterialPrefix.gem, "Cinnabar", 1)},
                new long[]{10000, 10000, 3000});
        // Slice2 now owns the host route; raw Cinnabar and all six-prefix Shredder rows remain unchanged.
        var block = GTBlocks.getStack(BlockMaterialPrefix.ore, material("Cinnabar"));
        require(!block.isEmpty(), "actual ordinary Cinnabar ore block");
        check(row(MachineRecipeMaps.Crusher, block), 640, new ItemStack[]{crushed, crushed.copy(),
                form(MaterialPrefix.gem, "Cinnabar", 1), form(MaterialPrefix.dust, "Stone", 4)},
                new long[]{10000, 10000, 3000, 10000});
        check(row(MachineRecipeMaps.Hammer, block), 544, new ItemStack[]{crushed});
    }

    private static void check(Recipe recipe, long ticks, ItemStack[] outputs) {
        long[] chances = new long[outputs.length]; Arrays.fill(chances, 10000);
        check(recipe, ticks, outputs, chances);
    }
    private static void check(Recipe recipe, long ticks, ItemStack[] outputs, long[] chances) {
        String input = describe(recipe.mInputs[0]);
        require(recipe.mEUt == 16 && recipe.mDuration == ticks && recipe.mSpecialValue == 0,
                input + " expected16EU/t," + ticks + "ticks, actual=" + recipe.mEUt + "/" + recipe.mDuration);
        require(recipe.mOutputs.length == outputs.length, input + " exact ordered slot count, actual=" + recipe.mOutputs.length);
        for (int slot = 0; slot < outputs.length; slot++) require(ItemStack.matches(recipe.mOutputs[slot], outputs[slot]),
                input + " slot" + slot + " expected=" + describe(outputs[slot]) + ", actual=" + describe(recipe.mOutputs[slot]));
        require(Arrays.equals(recipe.mChances, chances), input + " per-slot chances, actual=" + Arrays.toString(recipe.mChances));
        require(recipe.mFluidInputs.length == 0 && recipe.mFluidOutputs.length == 0, input + " no fluid slots");
    }
    private static List<Recipe> rows(RecipeMap map, ItemStack input) {
        return map.mRecipeList.stream().filter(r -> r.mInputs.length == 1 && r.mFluidInputs.length == 0
                && ItemStack.matches(r.mInputs[0], input)).toList();
    }
    private static Recipe row(RecipeMap map, ItemStack input) {
        var matches = rows(map, input);
        require(matches.size() == 1, map + " expected one row for " + describe(input) + ", actual=" + matches.size());
        require(map.findRecipe(List.of(input), List.of(), false, 1, 12) == matches.get(0), "indexed native lookup: " + describe(input));
        return matches.get(0);
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
    private static String describe(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount(); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
