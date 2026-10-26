package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.recipe.Recipe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Native purified Sifter source rows. Optional fossil fixtures are lookup seams, not UB integration. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class SiftingRegressionTests {
    private static final List<String> SPECIAL = List.of("Asbestos", "Tin", "Cassiterite", "Bone", "Dolomite",
            "ChargedCertusQuartz", "CertusQuartz", "NetherQuartz", "VoidQuartz", "MilkyQuartz", "Fluix");
    private SiftingRegressionTests() {}

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void sourceSiftingAfterStartup(GameTestHelper helper) {
        require(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY
                && MachineRecipeOverlay.applications() > 0, "actual registration and overlay must finish");
        specialRows();
        for (Recipe recipe : SharedOreSiftingRows.owned()) {
            var form = MaterialEquivalence.form(recipe.mInputs[0]);
            require(form != null, "owned purified input has actual material binding");
            GTMaterial material = form.material().resolve();
            if (!SPECIAL.contains(material.getName())) require(!material.has(MaterialProperty.ANTIMATTER)
                    && material != material("Bedrock"), "generic excludes antimatter and Bedrock");
        }
        for (boolean tiny : new boolean[]{false, true})
            check(row(input("Cinnabar", tiny)), tiny ? 16 : 144, genericChances(tiny), genericOutputs("Cinnabar"));
        helper.succeed();
    }

    private static void specialRows() {
        int checked = 0;
        for (String material : List.of("Asbestos", "Tin", "Cassiterite")) for (boolean tiny : new boolean[]{false, true}) {
            String target = material.equals("Asbestos") ? "Jade" : "Zircon";
            var outputs = new ItemStack[]{form(MaterialPrefix.crushedCentrifugedTiny, material, tiny ? 1 : 9),
                    form(MaterialPrefix.gemExquisite, target, 1), form(MaterialPrefix.gemFlawless, target, 1),
                    form(MaterialPrefix.gem, target, 1), form(MaterialPrefix.gemFlawed, target, 2),
                    form(MaterialPrefix.gemChipped, target, 4), form(MaterialPrefix.dust, target, 1)};
            long[] chances = genericChances(tiny); chances[0] = 10000;
            check(row(input(material, tiny)), tiny ? 16 : 144, chances, outputs); checked++;
        }
        for (String material : List.of("Bone", "Dolomite")) for (boolean tiny : new boolean[]{false, true}) {
            var outputs = new ItemStack[12];
            outputs[0] = form(MaterialPrefix.crushedCentrifugedTiny, material, tiny ? 1 : 9);
            outputs[1] = new ItemStack(Items.SKELETON_SKULL); outputs[2] = new ItemStack(Items.BONE);
            for (int slot = 3; slot < 12; slot++) outputs[slot] = form(MaterialPrefix.dust, "Bone", 1);
            long[] chances = new long[12]; Arrays.fill(chances, tiny ? 400 : 3600);
            chances[0] = 10000; chances[1] = tiny ? 40 : 360;
            check(row(input(material, tiny)), tiny ? 16 : 144, chances, outputs); checked++;
        }
        for (String material : SPECIAL.subList(5, 11)) for (boolean tiny : new boolean[]{false, true}) {
            boolean certus = material.equals("ChargedCertusQuartz") || material.equals("CertusQuartz");
            var outputs = new ArrayList<ItemStack>();
            if (certus) outputs.add(form(MaterialPrefix.gem, "ChargedCertusQuartz", 1));
            // The source still resolves gem/NetherQuartz directly. Ordinary RecipeMap admission
            // canonicalizes that equivalent output to vanilla quartz; no special producer override.
            outputs.add(material.equals("NetherQuartz") ? new ItemStack(Items.QUARTZ)
                    : form(MaterialPrefix.gem, certus ? "CertusQuartz" : material, 1));
            for (int slot = 0; slot < (tiny ? 1 : 9); slot++)
                outputs.add(form(MaterialPrefix.dustTiny, certus ? "CertusQuartz" : material, 1));
            long[] chances = new long[certus ? 11 : 10]; Arrays.fill(chances, 5000);
            chances[0] = tiny ? 800 : 7200;
            if (certus) {
                chances[1] = tiny ? 100 : 900;
                if (material.equals("CertusQuartz")) { long swap = chances[0]; chances[0] = chances[1]; chances[1] = swap; }
            }
            // Source tiny arrays have unused tails. Keep ordinary native chance normalization.
            check(row(input(material, tiny)), tiny ? 16 : 144, chances, outputs.toArray(ItemStack[]::new)); checked++;
        }
        require(checked == 22, "all eleven strict identities have both source special rows");
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void directFormsPrecedenceAndReload(GameTestHelper helper) {
        independentFixedForms();
        controlledFossils();
        genericGatesAndCapture();
        controlledExternalGemPrecedence();
        Recipe original = row(input("Asbestos", false)), foreign = null;
        int count = SharedOreSiftingRows.owned().size();
        try {
            require(MachineRecipeMaps.Sifting.unlink(original), "isolate foreign collision fixture");
            foreign = new Recipe(new ItemStack[]{original.mInputs[0].copy()}, new ItemStack[]{new ItemStack(Items.FLINT)},
                    null, new long[]{10000}, new FluidStack[0], new FluidStack[0], 17, 7, 0);
            require(MachineRecipeMaps.Sifting.addRecipe(foreign) == foreign, "foreign fixture admitted normally");
            for (int reload = 0; reload < 2; reload++) {
                var oldOwned = SharedOreSiftingRows.owned();
                SharedOreSiftingRows.rebuild();
                require(MachineRecipeMaps.Sifting.mRecipeList.contains(foreign) && !SharedOreSiftingRows.owned().contains(foreign),
                        "foreign collision is preserved, never adopted");
                require(SharedOreSiftingRows.owned().size() == count - 1
                        && SharedOreSiftingRows.owned().stream().noneMatch(oldOwned::contains), "only accepted fresh identities are owned");
            }
        } finally {
            if (foreign != null) MachineRecipeMaps.Sifting.unlink(foreign);
            SharedOreSiftingRows.rebuild();
        }
        var oldOwned = SharedOreSiftingRows.owned();
        MachineRecipeOverlay.apply(helper.getLevel().getServer().getRecipeManager());
        require(SharedOreSiftingRows.owned().size() == count
                && SharedOreSiftingRows.owned().stream().noneMatch(oldOwned::contains), "real overlay rebuilds without stale generic baseline");
        specialRows();
        helper.succeed();
    }

    private static void independentFixedForms() {
        var asbestos = material("Asbestos");
        for (var absent : List.of(MaterialPrefix.crushedPurifiedTiny, MaterialPrefix.dust, MaterialPrefix.gem)) {
            var snapshot = new SharedOreSiftingRows.Snapshot() {
                @Override SharedOreWashingRows.NativeForm direct(MaterialPrefix prefix, GTMaterial material, int count) {
                    return material == asbestos && prefix == absent ? SharedOreWashingRows.NativeForm.absent()
                            : super.direct(prefix, material, count);
                }
            };
            var definitions = SharedOreSiftingRows.definitions(List.of(asbestos), snapshot).stream().filter(d -> d.special()).toList();
            require(definitions.size() == (absent == MaterialPrefix.crushedPurifiedTiny ? 1 : 2),
                    "fixed shapes are independent of counterpart, own dust and own gem");
        }
        var missingOutputs = new SharedOreSiftingRows.Snapshot() {
            @Override SharedOreWashingRows.NativeForm direct(MaterialPrefix prefix, GTMaterial material, int count) {
                return (material == asbestos && prefix == MaterialPrefix.crushedCentrifugedTiny)
                        || (material == material("Jade") && prefix == MaterialPrefix.gemExquisite)
                        ? SharedOreWashingRows.NativeForm.absent() : super.direct(prefix, material, count);
            }
        };
        var definitions = SharedOreSiftingRows.definitions(List.of(asbestos), missingOutputs);
        require(definitions.size() == 2, "missing fixed direct outputs do not suppress valid remaining output slots");
        for (var definition : definitions) require(definition.outputs().length == 7
                && definition.outputs()[0].isEmpty() && definition.outputs()[1].isEmpty()
                && ItemStack.matches(definition.outputs()[2], form(MaterialPrefix.gemFlawless, "Jade", 1)),
                "fixed missing main/grade stay EMPTY; no OM dust or ordinary-gem fallback");
        Recipe original = row(input("Asbestos", false)), missing = null;
        try {
            require(MachineRecipeMaps.Sifting.unlink(original), "isolate missing fixed-output admission");
            missing = definitions.stream().filter(d -> ItemStack.matches(d.input(), original.mInputs[0])).findFirst().orElseThrow().admit();
            require(missing != null && missing.mOutputs.length == 7 && missing.mOutputs[0].isEmpty()
                    && missing.mOutputs[1].isEmpty() && missing.mChances[2] == 360,
                    "native normalization preserves interior EMPTY slots and their chance alignment");
        } finally {
            if (missing != null) MachineRecipeMaps.Sifting.unlink(missing);
            MachineRecipeMaps.Sifting.add(original);
        }
        var unknown = new SharedOreSiftingRows.Snapshot() {
            @Override SharedOreWashingRows.NativeForm direct(MaterialPrefix prefix, GTMaterial material, int count) {
                if (material == asbestos && prefix == MaterialPrefix.gem)
                    return SharedOreWashingRows.NativeForm.present(new ItemStack(Items.PRISMARINE_CRYSTALS, count));
                if (material == material("Jade") && prefix == MaterialPrefix.gemExquisite)
                    return SharedOreWashingRows.NativeForm.unknown();
                return super.direct(prefix, material, count);
            }
        };
        var captured = SharedOreSiftingRows.captureGeneric(List.of(asbestos), unknown);
        require(captured.get(asbestos).availability() == SourceOreWashingRules.Availability.PRESENT
                && SharedOreSiftingRows.definitions(List.of(asbestos), unknown, captured).isEmpty(),
                "UNKNOWN fixed grade cannot silently become an otherwise eligible lower-priority generic row");
    }

    private static void controlledFossils() {
        var snapshot = new SharedOreSiftingRows.Snapshot() {
            @Override SharedOreWashingRows.NativeForm fossil(int index) {
                return switch (index) {
                    case 0 -> SharedOreWashingRows.NativeForm.present(new ItemStack(Items.FLINT));
                    case 3 -> SharedOreWashingRows.NativeForm.present(new ItemStack(Items.AMETHYST_SHARD));
                    case 7 -> SharedOreWashingRows.NativeForm.present(new ItemStack(Items.PRISMARINE_SHARD));
                    default -> SharedOreWashingRows.NativeForm.absent();
                };
            }
        };
        var definitions = SharedOreSiftingRows.definitions(List.of(material("Bone")), snapshot);
        require(definitions.size() == 2, "controlled optional lookup still yields both fossil shapes");
        for (var definition : definitions) for (int index = 0; index < 8; index++) {
            ItemStack expected = switch (index) {
                case 0 -> new ItemStack(Items.FLINT); case 3 -> new ItemStack(Items.AMETHYST_SHARD);
                case 7 -> new ItemStack(Items.PRISMARINE_SHARD); default -> form(MaterialPrefix.dust, "Bone", 1);
            };
            require(ItemStack.matches(definition.outputs()[index + 4], expected), "independent controlled fossil slot " + index);
        }
    }

    private static void genericGatesAndCapture() {
        var cinnabar = material("Cinnabar");
        for (var absent : List.of(MaterialPrefix.dust, MaterialPrefix.crushedPurified, MaterialPrefix.crushedPurifiedTiny, MaterialPrefix.gem)) {
            var snapshot = new SharedOreSiftingRows.Snapshot() {
                @Override SharedOreWashingRows.NativeForm direct(MaterialPrefix prefix, GTMaterial material, int count) {
                    return material == cinnabar && prefix == absent ? SharedOreWashingRows.NativeForm.absent()
                            : super.direct(prefix, material, count);
                }
            };
            require(SharedOreSiftingRows.definitions(List.of(cinnabar), snapshot).isEmpty(), "generic requires actual gate " + absent.getName());
        }
        var captured = SharedOreSiftingRows.captureGeneric(List.of(cinnabar), new SharedOreSiftingRows.Snapshot());
        var laterGrade = new SharedOreSiftingRows.Snapshot() {
            @Override ItemStack grade(MaterialPrefix prefix, GTMaterial material, int count, ItemStack gem, int fallback) {
                throw new AssertionError("late grade rebinding must not rewrite captured startup outputs");
            }
        };
        var unchanged = SharedOreSiftingRows.definitions(List.of(cinnabar), laterGrade, captured);
        require(unchanged.size() == 2, "captured generic shapes remain available");
        for (var definition : unchanged) require(equalItems(definition.outputs(), genericOutputs("Cinnabar")), "startup grades remain fixed");
        var ice = material("Ice");
        var iceCapture = SharedOreSiftingRows.captureGeneric(List.of(ice), new SharedOreSiftingRows.Snapshot());
        require(SharedOreSiftingRows.definitions(List.of(ice), new SharedOreSiftingRows.Snapshot(), iceCapture).isEmpty(),
                "reviewed dormant Ice inputs do not wake from legacy generated holders");
        var continuation = new SharedOreSiftingRows.Snapshot() {
            @Override SharedOreWashingRows.NativeForm input(MaterialPrefix prefix, GTMaterial material, int count) {
                return material == ice ? direct(prefix, material, count) : super.input(prefix, material, count);
            }
            @Override ItemStack grade(MaterialPrefix prefix, GTMaterial material, int count, ItemStack gem, int fallback) {
                throw new AssertionError("controlled input continuation must retain startup grades");
            }
        };
        require(SharedOreSiftingRows.definitions(List.of(ice), continuation, iceCapture).size() == 2,
                "controlled supported input continuation resumes both sizes with captured outputs");
        var iceGem = form(MaterialPrefix.gem, "Ice", 1);
        MaterialPrefix[] grades = {MaterialPrefix.gemLegendary, MaterialPrefix.gemExquisite, MaterialPrefix.gemFlawless};
        int[] fallback = {8, 4, 2};
        for (int index = 0; index < grades.length; index++) require(ItemStack.matches(
                SourceGemGradeResolver.gradeOrGem(grades[index], material("Ice"), 1, iceGem, fallback[index]),
                form(MaterialPrefix.gem, "Ice", fallback[index])), "existing Ice upper-grade fallback policy");
    }

    private static void controlledExternalGemPrecedence() {
        for (String name : SPECIAL.subList(0, 5)) {
            GTMaterial material = material(name);
            require(!MaterialPrefix.gem.isValidFor(material), "fixture has false own-gem generation condition");
            // Controlled resolved-binding seam using an actual external holder, not a global registration.
            var snapshot = new SharedOreSiftingRows.Snapshot() {
                @Override SharedOreWashingRows.NativeForm direct(MaterialPrefix prefix, GTMaterial value, int count) {
                    return value == material && prefix == MaterialPrefix.gem
                            ? SharedOreWashingRows.NativeForm.present(new ItemStack(Items.PRISMARINE_CRYSTALS, count))
                            : super.direct(prefix, value, count);
                }
            };
            var captured = SharedOreSiftingRows.captureGeneric(List.of(material), snapshot);
            var definitions = SharedOreSiftingRows.definitions(List.of(material), snapshot, captured);
            require(definitions.stream().filter(d -> !d.special()).count() == 2, "actual external gem admits generic independent of generation flag");
            var old = List.of(row(input(name, false)), row(input(name, true)));
            var accepted = new ArrayList<Recipe>();
            try {
                old.forEach(MachineRecipeMaps.Sifting::unlink);
                for (var definition : definitions) {
                    Recipe recipe = definition.admit();
                    if (recipe != null) { require(definition.special(), "fixed source call must precede same-input generic"); accepted.add(recipe); }
                }
                require(accepted.size() == 2, "only the two source specials win normal native admission");
            } finally {
                accepted.forEach(MachineRecipeMaps.Sifting::unlink);
                old.forEach(MachineRecipeMaps.Sifting::add);
            }
        }
    }

    private static long[] genericChances(boolean tiny) {
        return tiny ? new long[]{1, 10, 40, 150, 200, 400, 500} : new long[]{9, 90, 360, 1350, 1800, 3600, 4500};
    }
    private static ItemStack[] genericOutputs(String material) {
        return new ItemStack[]{form(MaterialPrefix.gemLegendary, material, 1), form(MaterialPrefix.gemExquisite, material, 1),
                form(MaterialPrefix.gemFlawless, material, 1), form(MaterialPrefix.gem, material, 1),
                form(MaterialPrefix.gemFlawed, material, 2), form(MaterialPrefix.gemChipped, material, 4), form(MaterialPrefix.dust, material, 1)};
    }
    private static ItemStack input(String name, boolean tiny) {
        return form(tiny ? MaterialPrefix.crushedPurifiedTiny : MaterialPrefix.crushedPurified, name, 1);
    }
    private static Recipe row(ItemStack input) {
        var rows = MachineRecipeMaps.Sifting.mRecipeList.stream().filter(r -> r.mInputs.length == 1
                && r.mInputs[0].getItem() == input.getItem()).toList();
        require(rows.size() == 1 && ItemStack.matches(rows.get(0).mInputs[0], input), "exact unique Sifter row " + input);
        Recipe recipe = rows.get(0);
        require(SharedOreSiftingRows.owned().contains(recipe), "accepted Sifter identity is owned");
        require(MachineRecipeMaps.Sifting.findRecipe(List.of(input), List.of(), false, 1, 12) == recipe, "native lookup selects source-owned row");
        return recipe;
    }
    private static void check(Recipe recipe, long ticks, long[] chances, ItemStack[] outputs) {
        require(recipe.mDuration == ticks && recipe.mEUt == 16 && recipe.mSpecialValue == 0 && recipe.mSpecialItems == null,
                "exact Sifter time/power/no special gate");
        require(recipe.mFluidInputs.length == 0 && recipe.mFluidOutputs.length == 0, "source no-fluid row");
        String input = describe(recipe.mInputs[0]);
        require(recipe.mOutputs.length == outputs.length, input + " output count: actual=" + recipe.mOutputs.length
                + ", expected=" + outputs.length + "; actual=" + Arrays.stream(recipe.mOutputs).map(SiftingRegressionTests::describe).toList());
        for (int slot = 0; slot < outputs.length; slot++) require(ItemStack.matches(recipe.mOutputs[slot], outputs[slot]),
                input + " output slot " + slot + ": actual=" + describe(recipe.mOutputs[slot])
                        + ", expected=" + describe(outputs[slot]));
        require(Arrays.equals(recipe.mChances, Arrays.copyOf(chances, outputs.length))
                && recipe.mMaxChances.length == outputs.length && Arrays.stream(recipe.mMaxChances).allMatch(c -> c == 10000),
                input + " effective native chances: actual=" + Arrays.toString(recipe.mChances)
                        + ", expected=" + Arrays.toString(Arrays.copyOf(chances, outputs.length)));
    }
    private static String describe(ItemStack stack) {
        return stack.isEmpty() ? "EMPTY" : BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount()
                + (stack.hasTag() ? " " + stack.getTag() : "");
    }
    private static boolean equalItems(ItemStack[] a, ItemStack[] b) {
        if (a.length != b.length) return false;
        for (int index = 0; index < a.length; index++) if (!ItemStack.matches(a[index], b[index])) return false;
        return true;
    }
    private static GTMaterial material(String name) {
        GTMaterial value = GTMaterialRegistry.get(name);
        require(value != null && value.isValid(), "missing expected material " + name); return value.resolve();
    }
    private static ItemStack form(MaterialPrefix prefix, String name, int count) {
        var key = new ResourceLocation("gt6ce:" + prefix.getItemId(material(name)));
        require(BuiltInRegistries.ITEM.containsKey(key), "missing expected bound form " + key);
        return new ItemStack(BuiltInRegistries.ITEM.get(key), count);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
