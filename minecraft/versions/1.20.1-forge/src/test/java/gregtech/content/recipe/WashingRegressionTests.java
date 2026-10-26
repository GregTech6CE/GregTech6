package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.data.FluidCatalog;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTFluids;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeInputs;
import gregtech.engine.recipe.RecipeMap;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real bound holders and native recipe maps, not a second copy of the pure washing planner. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class WashingRegressionTests {
    private static final List<String> WATERS = List.of("Water", "MnWtr", "DistW", "SpDew");
    private static final List<String> CRYO = List.of("GoldenAmber", "DominicanAmber", "Glowstone",
            "GlowstoneCeres", "GlowstoneIo", "GlowstoneEnceladus", "GlowstoneProteus", "GlowstonePluto",
            "Gloomstone", "Carminite", "Nikolite", "Redstone", "Cinnabar");
    private WashingRegressionTests() {}

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void sourceWashingAfterStartup(GameTestHelper helper) {
        require(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY,
                "actual material bootstrap must finish");
        require(MachineRecipeOverlay.applications() > 0, "actual tag/recipe overlay must run");
        for (boolean tiny : new boolean[]{false, true}) {
            ItemStack input = form(tiny ? MaterialPrefix.crushedTiny : MaterialPrefix.crushed, "Gold", 1);
            check(row(MachineRecipeMaps.Bath, new ItemStack[]{input}, fluid("GenLiquid_Mercury", tiny ? 16 : 144)),
                    0, tiny ? 16 : 144, new long[tiny ? 2 : 6], bathOutputs("Gold", "Gold", tiny));
        }
        for (String material : List.of("Copper", "Cobaltite")) {
            String bonus = material.equals("Copper") ? "Cobaltite" : "Cobalt";
            reagentBaths(material, bonus, List.of("SodiumPersulfate", "PotassiumPersulfate"), false);
        }
        reagentBaths("Chromite", "Chromite", List.of("Firestone", "Octine"), true);
        cinnabarWater();
        petrotheum();
        cryotheum();
        helper.succeed();
    }

    private static void reagentBaths(String material, String bonus, List<String> reagents, boolean steam) {
        MaterialPrefix[] inputs = {MaterialPrefix.crushed, MaterialPrefix.crushedTiny,
                MaterialPrefix.crushed, MaterialPrefix.crushedTiny};
        MaterialPrefix[] reagentForms = {MaterialPrefix.dust, MaterialPrefix.dust,
                MaterialPrefix.dustTiny, MaterialPrefix.dustTiny};
        int[] inputCounts = {1, 9, 1, 1}, reagentCounts = {1, 1, 9, 1};
        for (String reagent : reagents) for (String water : WATERS) for (int shape = 0; shape < 4; shape++) {
            boolean tiny = shape == 3;
            int waterMb = tiny ? 100 : 900;
            Recipe recipe = row(MachineRecipeMaps.Bath, new ItemStack[]{form(inputs[shape], material, inputCounts[shape]),
                    form(reagentForms[shape], reagent, reagentCounts[shape])}, fluid(water, waterMb));
            check(recipe, 0, tiny ? 16 : 144, new long[tiny ? 2 : 6], bathOutputs(material, bonus, tiny),
                    steam ? new FluidStack[]{fluid("Steam", waterMb * 160)} : new FluidStack[0]);
        }
    }

    private static ItemStack[] bathOutputs(String material, String bonus, boolean tiny) {
        if (tiny) return new ItemStack[]{form(MaterialPrefix.crushedPurifiedTiny, material, 1),
                form(MaterialPrefix.crushedPurifiedTiny, bonus, 1)};
        return new ItemStack[]{form(MaterialPrefix.crushedPurified, material, 1),
                form(MaterialPrefix.crushedPurifiedTiny, bonus, 2), form(MaterialPrefix.crushedPurifiedTiny, bonus, 2),
                form(MaterialPrefix.crushedPurifiedTiny, bonus, 2), form(MaterialPrefix.crushedPurifiedTiny, bonus, 2),
                form(MaterialPrefix.crushedPurifiedTiny, bonus, 1)};
    }

    private static void cinnabarWater() {
        String[] bonuses = {"Redstone", "Sulfur", "Glowstone", "Selenium", "Cinnabar", "Cinnabar", "Cinnabar", "Cinnabar"};
        for (boolean tiny : new boolean[]{false, true}) for (String water : WATERS) {
            int amount = tiny ? 100 : 900;
            ItemStack[] outputs = new ItemStack[9];
            outputs[0] = form(tiny ? MaterialPrefix.crushedPurifiedTiny : MaterialPrefix.crushedPurified, "Cinnabar", 1);
            for (int slot = 0; slot < bonuses.length; slot++)
                outputs[slot + 1] = form(MaterialPrefix.crushedPurifiedTiny, bonuses[slot], tiny ? 1 : 9);
            check(row(MachineRecipeMaps.Sluice, new ItemStack[]{form(tiny ? MaterialPrefix.crushedTiny : MaterialPrefix.crushed,
                            "Cinnabar", 1)}, fluid(water, amount)), 16, tiny ? 16 : 144,
                    new long[]{10000, 300, 300, 300, 300, 300, 300, 300, 300}, outputs, fluid("Sluice", amount));
        }
    }

    private static void petrotheum() {
        for (String material : List.of("Copper", "Cinnabar")) for (boolean tiny : new boolean[]{false, true}) {
            Recipe recipe = row(MachineRecipeMaps.Sluice,
                    new ItemStack[]{form(tiny ? MaterialPrefix.crushedTiny : MaterialPrefix.crushed, material, 1)},
                    fluid("Petrotheum", tiny ? 5 : 45));
            check(recipe, 16, tiny ? 16 : 144, new long[]{10000, 5000, 10000},
                    new ItemStack[]{form(tiny ? MaterialPrefix.crushedPurifiedTiny : MaterialPrefix.crushedPurified, material, 2),
                            form(MaterialPrefix.crushedPurifiedTiny, material, tiny ? 1 : 9),
                            form(tiny ? MaterialPrefix.dustTiny : MaterialPrefix.dust, "SluiceSand", 1)});
        }
    }

    private static void cryotheum() {
        var cryoFluid = fluid("Cryotheum", 1).getFluid();
        long owned = SharedOreWashingRows.owned().stream().filter(r -> r.mFluidInputs.length == 1
                && r.mFluidInputs[0].getFluid() == cryoFluid).count();
        require(owned == 39, "13 strict Cryotheum identities each have three independent native rows, got " + owned);
        for (String material : CRYO) {
            ItemStack block = item("gt6ce:block_dust_" + material.toLowerCase(Locale.ROOT), 1);
            require(block.getItem() instanceof BlockItem && ((BlockItem) block.getItem()).getBlock() instanceof MaterialBlockLike,
                    material + " Cryotheum input is a real material block");
            var typed = (MaterialBlockLike) ((BlockItem) block.getItem()).getBlock();
            require(typed.prefix() == BlockMaterialPrefix.blockDust && typed.prefix().getMaterialWeight() == 9 * GTValues.U
                    && typed.material().resolve() == material(material), material + " bound blockDust identity/9U");
            check(row(MachineRecipeMaps.Bath, new ItemStack[]{block}, fluid("Cryotheum", 25)), 0, 256,
                    new long[]{10000}, new ItemStack[]{form(MaterialPrefix.gemExquisite, material, 1)});
            for (boolean tiny : new boolean[]{false, true})
                check(row(MachineRecipeMaps.Bath, new ItemStack[]{form(tiny ? MaterialPrefix.crushedTiny : MaterialPrefix.crushed,
                                material, tiny ? 9 : 1)}, fluid("Cryotheum", 13)), 0, 256,
                        new long[]{10000}, new ItemStack[]{form(MaterialPrefix.gemFlawless, material, 1)});
            require(MachineRecipeMaps.Bath.mRecipeList.stream().noneMatch(r -> r.mInputs.length == 1
                    && r.mInputs[0].getItem() == form(MaterialPrefix.dust, material, 1).getItem()
                    && uses(r, cryoFluid)), material + " physical block must never become dust9");
        }
        require(MachineRecipeMaps.Bath.mRecipeList.stream().filter(r -> uses(r, cryoFluid))
                .noneMatch(r -> r.mFluidInputs[0].getAmount() == 100 || r.mFluidInputs[0].getAmount() == 50),
                "retired 100/50 mB Cryotheum producer must stay absent");
        for (var prefix : List.of(MaterialPrefix.crushed, MaterialPrefix.crushedTiny))
            require(MachineRecipeMaps.Bath.mRecipeList.stream().noneMatch(r -> r.mInputs.length == 1
                    && BuiltInRegistries.ITEM.getKey(r.mInputs[0].getItem()).equals(
                            new ResourceLocation("gt6ce:" + prefix.getItemId(material("Amber")))) && uses(r, cryoFluid)),
                    "ordinary Amber is not source ANY.Amber");
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void failClosedAndOwnOnlyReload(GameTestHelper helper) {
        missingBindings();
        catalystCancellation();
        Recipe foreign = null;
        Recipe original = row(MachineRecipeMaps.Sluice, new ItemStack[]{form(MaterialPrefix.crushed, "Copper", 1)},
                fluid("Petrotheum", 45));
        int expectedOwned = SharedOreWashingRows.owned().size();
        try {
            require(MachineRecipeMaps.Sluice.unlink(original), "remove one owned row to install collision fixture");
            foreign = new Recipe(Arrays.stream(original.mInputs).map(ItemStack::copy).toArray(ItemStack[]::new),
                    new ItemStack[]{form(MaterialPrefix.dust, "Copper", 1)}, null, new long[]{10000},
                    new FluidStack[]{fluid("Petrotheum", 45)}, new FluidStack[0], 17, 7, 0);
            require(MachineRecipeMaps.Sluice.addRecipe(foreign) == foreign, "foreign collision fixture admitted");
            for (int reload = 0; reload < 2; reload++) {
                SharedOreWashingRows.rebuild();
                require(MachineRecipeMaps.Sluice.mRecipeList.contains(foreign), "own-only rebuild preserves foreign identity");
                require(!SharedOreWashingRows.owned().contains(foreign), "foreign collision is never adopted as owned");
                require(!SharedOreWashingRows.owned().contains(original), "old accepted identity cannot survive rebuild");
                require(SharedOreWashingRows.owned().size() == expectedOwned - 1, "collision rejects only its own row");
            }
        } finally {
            if (foreign != null) MachineRecipeMaps.Sluice.unlink(foreign);
            SharedOreWashingRows.rebuild();
        }
        Recipe baseline = MachineRecipeMaps.Centrifuge.mRecipeList.stream()
                .filter(r -> r.mInputs.length == 1 && r.mInputs[0].getItem() == item("minecraft:egg", 1).getItem())
                .findFirst().orElseThrow();
        for (int reload = 0; reload < 2; reload++) {
            var oldOwned = SharedOreWashingRows.owned();
            MachineRecipeOverlay.apply(helper.getLevel().getServer().getRecipeManager());
            require(SharedOreWashingRows.owned().size() == expectedOwned, "overlay rebuild count is stable");
            require(SharedOreWashingRows.owned().stream().noneMatch(oldOwned::contains), "overlay owns fresh accepted identities");
            require(MachineRecipeMaps.Centrifuge.mRecipeList.contains(baseline), "overlay retains unrelated startup baseline identity");
        }
        cryotheum();
        helper.succeed();
    }

    private static void missingBindings() {
        var selected = List.of(material("Copper"), material("Cinnabar"));
        var normal = SharedOreWashingRows.definitions(selected, new SharedOreWashingRows.Snapshot());
        for (String absent : List.of("Petrotheum", "Cryotheum")) {
            var snapshot = new SharedOreWashingRows.Snapshot(FluidCatalog.all(),
                    (field, amount) -> field.equals(absent) ? FluidStack.EMPTY : GTFluids.stack(field, amount));
            var missing = SharedOreWashingRows.definitions(selected, snapshot);
            var absentFluid = fluid(absent, 1).getFluid();
            long removed = normal.stream().filter(d -> d.fluidInputs().length == 1
                    && d.fluidInputs()[0].getFluid() == absentFluid).count();
            require(removed > 0 && missing.size() == normal.size() - removed, absent + " failure is independent");
            require(missing.stream().allMatch(d -> d.fluidInputs().length > 0
                    && Arrays.stream(d.fluidInputs()).noneMatch(FluidStack::isEmpty)), "no fluid-free fallback row");
        }
        for (MaterialPrefix absent : List.of(MaterialPrefix.crushedPurified, MaterialPrefix.crushedPurifiedTiny,
                MaterialPrefix.dust, MaterialPrefix.dustTiny)) {
            var masked = new SharedOreWashingRows.Snapshot() {
                @Override SharedOreWashingRows.NativeForm direct(MaterialPrefix prefix, GTMaterial material, int count) {
                    return prefix == absent && material == material("Copper")
                            && (absent == MaterialPrefix.crushedPurified || absent == MaterialPrefix.crushedPurifiedTiny)
                            ? SharedOreWashingRows.NativeForm.absent() : super.direct(prefix, material, count);
                }
                @Override SharedOreWashingRows.NativeForm sand(SourceOreWashingRules.Size size, GTMaterial material) {
                    return absent == size.sand() ? SharedOreWashingRows.NativeForm.absent() : super.sand(size, material);
                }
            };
            var petro = SharedOreWashingRows.definitions(List.of(material("Copper")), masked).stream()
                    .filter(d -> d.fluidInputs().length == 1
                            && d.fluidInputs()[0].getFluid() == fluid("Petrotheum", 1).getFluid()).toList();
            int expected = absent == MaterialPrefix.crushedPurified ? 1 : absent == MaterialPrefix.crushedPurifiedTiny ? 0 : 2;
            require(petro.size() == expected, "Petrotheum requires both direct purified outputs: " + absent.getName());
            if (expected == 2) require(petro.stream().filter(d -> d.outputs().length == 2).count() == 1,
                    "missing optional sand only omits that shape's tail");
        }
        var missingExquisite = new SharedOreWashingRows.Snapshot() {
            @Override SharedOreWashingRows.NativeForm direct(MaterialPrefix prefix, GTMaterial material, int count) {
                return prefix == MaterialPrefix.gemExquisite && material == material("Cinnabar")
                        ? SharedOreWashingRows.NativeForm.absent() : super.direct(prefix, material, count);
            }
        };
        var missingShape = SharedOreWashingRows.definitions(selected, missingExquisite);
        require(missingShape.size() == normal.size() - 1, "missing exquisite output drops only physical-block Cryotheum row");
        require(missingShape.stream().filter(d -> d.fluidInputs().length == 1
                && d.fluidInputs()[0].getFluid() == fluid("Cryotheum", 1).getFluid()).count() == 2,
                "both independent crushed Cryotheum shapes survive missing exquisite form");
        var absentPrimary = new SharedOreWashingRows.Snapshot() {
            @Override SharedOreWashingRows.NativeForm output(MaterialPrefix prefix, GTMaterial material, int count) {
                return material == material("Copper") && (prefix == MaterialPrefix.crushedPurified
                        || prefix == MaterialPrefix.crushedCentrifuged || prefix == MaterialPrefix.dust)
                        ? SharedOreWashingRows.NativeForm.absent() : super.output(prefix, material, count);
            }
        };
        var emptyMain = SharedOreWashingRows.definitions(List.of(material("Copper")), absentPrimary).stream()
                .filter(d -> d.map() == MachineRecipeMaps.Sluice && d.fluidOutputs().length == 1
                        && d.inputs()[0].getItem() == form(MaterialPrefix.crushed, "Copper", 1).getItem()).toList();
        require(emptyMain.size() == WATERS.size() && emptyMain.stream().allMatch(d -> d.outputs().length == 9
                && d.outputs()[0].isEmpty()), "ABSENT primary retains an empty raw slot, never fabricates an output");
        var definition = normal.get(0);
        require(new SharedOreWashingRows.Definition(definition.map(), false, definition.inputs(), definition.outputs(),
                definition.chances(), new FluidStack[]{FluidStack.EMPTY}, definition.fluidOutputs(),
                definition.duration(), definition.power()).admit() == null, "EMPTY required fluid rejected before native trimming");
        var iron = SourceOreOutputRows.resolvingOutputs(() -> new SharedOreWashingRows.Snapshot()
                .purified(SourceOreWashingRules.Size.WHOLE, material("Iron"), 1));
        require(iron.availability() == SourceOreWashingRules.Availability.PRESENT
                && ItemStack.matches(iron.stack(), form(MaterialPrefix.dust, "Iron", 1)), "Iron primary OM direct dust1 fallback");
    }

    private static void catalystCancellation() {
        Recipe candidate = null, displaced = null;
        var input = form(MaterialPrefix.crushed, "Octine", 1);
        var reagent = form(MaterialPrefix.dust, "Octine", 1);
        var water = fluid("Water", 900);
        try {
            displaced = MachineRecipeMaps.Bath.findRecipe(List.of(input, reagent), List.of(water), false, 2, 9);
            if (displaced != null) require(MachineRecipeMaps.Bath.unlink(displaced), "temporarily isolate exact cancellation fixture");
            candidate = new SharedOreWashingRows.Definition(MachineRecipeMaps.Bath, true,
                    new ItemStack[]{input, reagent}, new ItemStack[]{reagent.copy(), form(MaterialPrefix.dustTiny, "Zircon", 1)},
                    new long[0], new FluidStack[]{water}, new FluidStack[]{fluid("Steam", 144000)}, 144, 0).admit();
            require(candidate != null && Arrays.equals(candidate.explicitCatalystInputs(), new int[]{1}),
                    "exact equal Octine reagent/primary cancellation retains explicit reagent input");
            require(candidate.mOutputs[0].isEmpty(), "cancelled primary is not fabricated back");
            require(RecipeInputs.consume(candidate, List.of(input), List.of(water), 1) == null, "retained reagent is required");
            var remaining = RecipeInputs.consume(candidate, List.of(input, reagent), List.of(water), 1);
            require(remaining != null && remaining.items().get(0).isEmpty()
                    && ItemStack.matches(remaining.items().get(1), reagent) && remaining.fluids().get(0).isEmpty(),
                    "native consumption drains ore/water, never the retained reagent");
        } finally {
            if (candidate != null) MachineRecipeMaps.Bath.unlink(candidate);
            if (displaced != null) MachineRecipeMaps.Bath.add(displaced);
        }
    }

    private static Recipe row(RecipeMap map, ItemStack[] inputs, FluidStack fluid) {
        var rows = map.mRecipeList.stream().filter(r -> equalItems(r.mInputs, inputs)
                && r.mFluidInputs.length == 1 && sameFluid(r.mFluidInputs[0], fluid)).toList();
        require(rows.size() == 1, "exact native " + map.mNameInternal + " row for " + Arrays.toString(inputs)
                + " / " + fluid + ", got " + rows.size());
        Recipe recipe = rows.get(0);
        require(SharedOreWashingRows.owned().contains(recipe), "normal row must be accepted identity-owned");
        require(map.findRecipe(Arrays.asList(inputs), List.of(fluid), false, inputs.length, 9) == recipe, "native indexed lookup finds exact row");
        require(RecipeInputs.consume(recipe, Arrays.asList(inputs), List.of(), 1) == null, "required fluid cannot be omitted");
        return recipe;
    }

    private static void check(Recipe recipe, long power, long ticks, long[] chances, ItemStack[] outputs, FluidStack... fluids) {
        require(recipe.mEUt == power && recipe.mDuration == ticks, "native power/duration");
        require(recipe.mSpecialItems == null && recipe.mSpecialValue == 0 && recipe.mEnabled && !recipe.mFakeRecipe
                && !recipe.mHidden && recipe.mCanBeBuffered, "ordinary enabled recipe flags");
        require(equalItems(recipe.mOutputs, outputs), "ordered raw output slots");
        // An empty source chances array means all outputs are guaranteed after native normalization.
        if (Arrays.stream(chances).allMatch(c -> c == 0)) require(Arrays.stream(recipe.mChances).allMatch(c -> c == 10000), "guaranteed outputs");
        else require(Arrays.equals(recipe.mChances, chances), "ordered raw chances, without merging duplicate slots");
        require(recipe.mChances.length == outputs.length && recipe.mMaxChances.length == outputs.length
                && Arrays.stream(recipe.mMaxChances).allMatch(c -> c == 10000), "native chance slots/denominator");
        require(recipe.mFluidOutputs.length == fluids.length, "fluid output slots");
        for (int i = 0; i < fluids.length; i++) require(sameFluid(recipe.mFluidOutputs[i], fluids[i]), "fluid output identity/amount " + i);
    }
    private static boolean equalItems(ItemStack[] a, ItemStack[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (!ItemStack.matches(a[i], b[i])) return false;
        return true;
    }
    private static boolean sameFluid(FluidStack a, FluidStack b) {
        return a.getFluid() == b.getFluid() && a.getAmount() == b.getAmount() && !a.hasTag();
    }
    private static boolean uses(Recipe recipe, net.minecraft.world.level.material.Fluid fluid) {
        return recipe.mFluidInputs.length == 1 && recipe.mFluidInputs[0].getFluid() == fluid;
    }
    private static GTMaterial material(String name) {
        GTMaterial value = GTMaterialRegistry.get(name);
        require(value != null && value.isValid(), "missing expected material " + name);
        return value.resolve();
    }
    private static ItemStack form(MaterialPrefix prefix, String name, int count) {
        return item("gt6ce:" + prefix.getItemId(material(name)), count);
    }
    private static ItemStack item(String id, int count) {
        var key = new ResourceLocation(id);
        require(BuiltInRegistries.ITEM.containsKey(key), "missing actual item holder " + id);
        return new ItemStack(BuiltInRegistries.ITEM.get(key), count);
    }
    private static FluidStack fluid(String field, int amount) {
        FluidStack value = SharedOreWashingRows.checkedFluid(field, amount);
        require(!value.isEmpty(), "missing exact builtin fluid " + field);
        return value;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
