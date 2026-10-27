package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.content.data.FluidCatalog;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeInputs;
import gregtech.engine.recipe.RecipeMap;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import static gregtech.content.recipe.SourceThermalNativeAdapter.*;

/** Bounded single-component inputs, not the default 1U producer or a full recycling listener. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ThermalInputRegressionTests {
    private ThermalInputRegressionTests() {}
    private static final List<MaterialPrefix> PREFIXES = List.of(MaterialPrefix.dustSmall, MaterialPrefix.dustTiny,
            MaterialPrefix.dustDiv72, MaterialPrefix.gem, MaterialPrefix.crushedPurified, MaterialPrefix.crushedPurifiedTiny,
            MaterialPrefix.crushedCentrifuged, MaterialPrefix.crushedCentrifugedTiny, MaterialPrefix.gemChipped,
            MaterialPrefix.gemFlawed, MaterialPrefix.gemFlawless, MaterialPrefix.gemExquisite, MaterialPrefix.gemLegendary);
    private static final List<RecipeMap> MAPS = List.of(MachineRecipeMaps.Melter, MachineRecipeMaps.Smelter);
    private record Golden(String material, MaterialPrefix prefix, long units, String fluid, int amount, boolean melter) {}
    // Independent OPL255-291 / OP137-140,155-157,173 / MT439,1381,3717 / FL214 literals.
    private static final List<Golden> GOLDENS = List.of(
            new Golden("Tin", MaterialPrefix.dustSmall, 162162000, "GenMolten_Tin", 36, true),
            new Golden("Tin", MaterialPrefix.dustTiny, 72072000, "GenMolten_Tin", 16, true),
            new Golden("Tin", MaterialPrefix.dustDiv72, 9009000, "GenMolten_Tin", 2, true),
            new Golden("Ruby", MaterialPrefix.gem, 648648000, "GenMolten_Alumina", 378, false),
            new Golden("Ruby", MaterialPrefix.gemChipped, 162162000, "GenMolten_Alumina", 94, false),
            new Golden("Ruby", MaterialPrefix.gemFlawed, 324324000, "GenMolten_Alumina", 189, false),
            new Golden("Ruby", MaterialPrefix.gemFlawless, 1297296000, "GenMolten_Alumina", 756, false),
            new Golden("Ruby", MaterialPrefix.gemExquisite, 2594592000L, "GenMolten_Alumina", 1512, false),
            new Golden("Ruby", MaterialPrefix.gemLegendary, 5189184000L, "GenMolten_Alumina", 3024, false),
            new Golden("Galena", MaterialPrefix.crushedPurified, 810810000, "GenMolten_Lead", 60, true),
            new Golden("Galena", MaterialPrefix.crushedPurifiedTiny, 90090000, "GenMolten_Lead", 6, true),
            new Golden("Galena", MaterialPrefix.crushedCentrifuged, 891891000, "GenMolten_Lead", 66, true),
            new Golden("Galena", MaterialPrefix.crushedCentrifugedTiny, 99099000, "GenMolten_Lead", 7, true));

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void thirteenThermalInputsUseRealSingleComponents(GameTestHelper helper) {
        require(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY
                && MachineRecipeOverlay.applications() > 0, "real registration and first overlay completed");
        inventory();
        goldens();
        preservedRows();
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void thermalInputBoundariesAndOwnOnlyReload(GameTestHelper helper) {
        var baseline = preservedRows();
        controlledDefinitions();
        ownOnlyLifecycle(baseline);
        var previous = SharedThermalInputRows.owned();
        MachineRecipeOverlay.apply(helper.getLevel().getRecipeManager());
        require(SharedThermalInputRows.owned().stream().noneMatch(previous::contains), "actual overlay replaces only own accepted objects");
        require(preservedRows().equals(baseline), "default, reviewed and imported objects retain baseline ownership across overlay");
        inventory();
        goldens();
        helper.succeed();
    }

    private static void goldens() {
        for (var expected : GOLDENS) {
            var input = form(expected.prefix(), expected.material(), 1);
            exactComposition(input, expected.prefix(), material(expected.material()), expected.units());
            var plan = inspectForm(expected.prefix(), material(expected.material()), input, new Snapshot());
            require(plan.action() == Action.ROWS && plan.rows().size() == (expected.melter() ? 2 : 1),
                    "independent machine gate for " + expected + ": " + plan);
            for (var definition : plan.rows()) {
                require(ItemStack.matches(definition.input(), input) && definition.power() == 16
                        && definition.output().isFluidEqual(fluid(expected.fluid(), expected.amount()))
                        && definition.output().getAmount() == expected.amount(), "selected input/phase/mass reaches native definition");
                var recipe = row(definition.map(), input);
                check(recipe, input, expected.fluid(), expected.amount(), -1);
                require(SharedThermalInputRows.owned().contains(recipe), "new input is actually accepted by this producer");
                var remaining = RecipeInputs.consume(recipe, List.of(input), List.of(), 1);
                require(remaining != null && remaining.items().get(0).isEmpty(), "one actual input is consumed, including a single tiny ore");
            }
            if (!expected.melter()) require(rows(MachineRecipeMaps.Melter, input).isEmpty(), "Ruby target Alumina has no Melter permission");
            var dust = form(MaterialPrefix.dust, expected.material(), 1);
            require(!MaterialEquivalence.matches(dust, input), "same material but another prefix is not an alias for default dust");
        }
        var zero = form(MaterialPrefix.dustDiv72, "Galena", 1);
        outcome(inspectForm(MaterialPrefix.dustDiv72, material("Galena"), zero, new Snapshot()),
                Action.SOURCE_NO_ROW, Reason.SOURCE_ZERO_VOLUME);
        for (var map : MAPS) require(rows(map, zero).isEmpty(), "zero mB never becomes an invented 1 mB row");
        // OPL290-291 uses ceil(mB * 80 / 16), NOT max(16, ...), for the Lava energy floor.
        for (String name : List.of("Obsidian", "ObsidianRefined")) {
            var input = form(MaterialPrefix.dustDiv72, name, 1);
            var recipe = row(MachineRecipeMaps.Smelter, input);
            check(recipe, input, "Lava", 1, 5);
            require(SharedThermalInputRows.owned().contains(recipe) && rows(MachineRecipeMaps.Melter, input).isEmpty(),
                    "positive 1 mB Lava keeps its source 5-tick energy floor and Smelter-only permission");
        }
    }

    /** These are actual earlier producers, not presumed legacy objects from the new family. */
    private static List<Recipe> preservedRows() {
        var result = new ArrayList<Recipe>();
        for (String name : List.of("Nickel", "Tin")) {
            var input = form(MaterialPrefix.dust, name, 1);
            var smelter = row(MachineRecipeMaps.Smelter, input); result.add(smelter);
            require(!SharedThermalInputRows.owned().contains(smelter) && !smelter.mMaterialRecovery
                    && !smelter.mRequiresEmptyContainerInputs, "default 1U producer retains ownership and existing runtime flags");
        }
        for (String name : List.of("Titanium", "Iron")) {
            var prefixes = List.of(MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72);
            int[] amounts = {144, 36, 16, 2};
            long[] ticks = name.equals("Titanium") ? new long[]{519, 129, 57, 16} : new long[]{830, 207, 92, 16};
            for (int i = 0; i < prefixes.size(); i++) {
                var input = form(prefixes.get(i), name, 1); var recipe = row(MachineRecipeMaps.Smelter, input);
                check(recipe, input, "GenMolten_" + name, amounts[i], ticks[i]); result.add(recipe);
                require(!SharedThermalInputRows.owned().contains(recipe) && rows(MachineRecipeMaps.Melter, input).isEmpty(),
                        "reviewed Ti/Fe owner includes div72 without granting Melter permission");
            }
        }
        var cinnabarPrefixes = List.of(MaterialPrefix.dustSmall, MaterialPrefix.dustTiny, MaterialPrefix.crushedPurified,
                MaterialPrefix.crushedPurifiedTiny, MaterialPrefix.crushedCentrifuged, MaterialPrefix.crushedCentrifugedTiny);
        int[] mercury = {12, 5, 60, 6, 66, 7};
        for (int i = 0; i < cinnabarPrefixes.size(); i++) for (var map : MAPS) {
            var input = form(cinnabarPrefixes.get(i), "Cinnabar", 1); var recipe = row(map, input);
            check(recipe, input, "GenLiquid_Mercury", mercury[i], 16); result.add(recipe);
            require(!SharedThermalInputRows.owned().contains(recipe), "reviewed Cinnabar input is not stolen by the generic extension");
        }
        for (String name : List.of("Ice", "Snow")) {
            var tiny = form(MaterialPrefix.dustTiny, name, 1);
            require(ItemStack.matches(GeneratedRecipeResolver.resolveSpec("omdu:" + name + ":72072000"), tiny),
                    "exact source U9 resolves to tiny1");
            require(ItemStack.matches(GeneratedRecipeResolver.resolveSpec("omd:" + name + ":111"), form(MaterialPrefix.dustDiv72, name, 7)),
                    "old rounded token keeps its global compatibility meaning");
            for (var map : MAPS) {
                var recipe = row(map, tiny); check(recipe, tiny, "Water", 111, 222); result.add(recipe);
                require(!SharedThermalInputRows.owned().contains(recipe), "explicit Ice/Snow cost wins normal admission");
                var small = form(MaterialPrefix.dustSmall, name, 1); var old = row(map, small);
                check(old, small, "Water", 250, 500); result.add(old);
            }
            var dry = row(MachineRecipeMaps.Drying, tiny); check(dry, tiny, "DistW", 111, 444); result.add(dry);
        }
        return List.copyOf(result);
    }

    private static void inventory() {
        var owned = SharedThermalInputRows.owned(); var admissions = SharedThermalInputRows.admissions();
        require(!owned.isEmpty() && admissions.stream().filter(SharedThermalInputRows.Admission::accepted).count() == owned.size(),
                "accepted outcomes count actual owned objects, not the audit definition pool");
        Map<String, Integer> accepted = new TreeMap<>(), collisions = new TreeMap<>(), deferred = new TreeMap<>();
        Set<String> contracts = new HashSet<>(); Set<MaterialPrefix> prefixes = new HashSet<>();
        var snapshot = new Snapshot();
        for (var recipe : owned) {
            var maps = MAPS.stream().filter(map -> map.mRecipeList.contains(recipe)).toList();
            require(maps.size() == 1 && recipe.mInputs.length == 1, "each owned object is live in exactly one intended map");
            var input = recipe.mInputs[0];
            require(input.getItem() instanceof MaterialFormItem, "real registered native material holder");
            var typed = (MaterialFormItem)input.getItem(); var prefix = typed.getPrefix(); var material = typed.getMaterial().resolve();
            require(PREFIXES.contains(prefix) && GTMaterialRegistry.isCoreMaterial(material)
                    && GTItems.getStack(prefix, material, 1).getItem() == input.getItem(), "bounded exact selected holder and canonical identity");
            exactComposition(input, prefix, material, prefix.getMaterialWeight());
            require(snapshot.inputUnits(material, prefix, input) == prefix.getMaterialWeight(), "selected input proof uses actual component mass");
            String detail = "map=" + maps.get(0).mNameInternal + ", material=" + material.getName() + ", prefix=" + prefix.getName()
                    + ", input=" + BuiltInRegistries.ITEM.getKey(input.getItem()) + "x" + input.getCount()
                    + ", outputs=" + Arrays.stream(recipe.mFluidOutputs).map(f -> BuiltInRegistries.FLUID.getKey(f.getFluid()) + "x" + f.getAmount()).toList()
                    + ", ticks=" + recipe.mDuration
                    + ", EUt=" + recipe.mEUt + ", recovery=" + recipe.mMaterialRecovery + ", empty=" + recipe.mRequiresEmptyContainerInputs;
            require(recipe.mOutputs.length == 0 && recipe.mFluidInputs.length == 0 && recipe.mFluidOutputs.length == 1
                    && !recipe.mFluidOutputs[0].isEmpty() && recipe.mEUt == 16
                    && recipe.mMaterialRecovery && recipe.mRequiresEmptyContainerInputs,
                    "positive native fluid, source power and recovery guards: " + detail);
            var output = recipe.mFluidOutputs[0];
            long minimumTicks = output.getFluid() == Fluids.LAVA ? 5L * output.getAmount() : 16;
            require(recipe.mDuration >= minimumTicks, "source phase-specific energy floor " + minimumTicks + ": " + detail);
            require(output.getFluid() == Fluids.WATER || output.getFluid() == Fluids.LAVA
                    || GTFluids.entryForFluid(output.getFluid()) != null, "actual bound phase holder");
            var map = maps.get(0); prefixes.add(prefix);
            String key = map.mNameInternal + "/" + prefix.getName(); accepted.merge(key, 1, Integer::sum);
            require(contracts.add(key + "/" + material.getId() + "/" + prefix.getMaterialWeight()), "no duplicate alias contract");
            require(row(map, input) == recipe, "actual indexed lookup of every owned contract");
        }
        for (var admission : admissions) if (!admission.accepted())
            collisions.merge(admission.map() + "/" + admission.prefix().getName(), 1, Integer::sum);
        for (var entry : SharedThermalInputRows.deferred())
            deferred.merge(entry.action() + "/" + entry.reason(), 1, Integer::sum);
        require(prefixes.equals(new HashSet<>(PREFIXES)), "all thirteen independently selected prefixes have accepted runtime rows");
        System.out.println("[thermal-input-regression] actualAccepted=" + owned.size() + ", plannedAdmissions=" + admissions.size()
                + ", acceptedByMapPrefix=" + accepted + ", collisionsByMapPrefix=" + collisions + ", deferred=" + deferred);
    }

    private static void controlledDefinitions() {
        var tin = material("Tin"); var small = form(MaterialPrefix.dustSmall, "Tin", 1);
        for (var prefix : List.of(MaterialPrefix.oreRaw, MaterialPrefix.crushed, MaterialPrefix.crushedTiny)) {
            var input = form(prefix, "Galena", 1);
            outcome(inspectForm(prefix, material("Galena"), input, new Snapshot()), Action.SOURCE_NO_ROW, Reason.SOURCE_PREFIX_EXCLUDED);
            for (var map : MAPS) require(rows(map, input).isEmpty(), "ORE/DIRTY prefixes are source NO, not missing thermal coverage");
        }
        outcome(inspectForm(MaterialPrefix.dust, tin, form(MaterialPrefix.dust, "Tin", 1), new Snapshot()),
                Action.LEGACY_UNSUPPORTED, Reason.UNSUPPORTED_INPUT_PREFIX);
        outcome(inspectForm(MaterialPrefix.dustSmall, tin, ItemStack.EMPTY, new Snapshot()), Action.DEFERRED_NO_ROW, Reason.INPUT_ABSENT);
        outcome(inspectForm(MaterialPrefix.dustSmall, tin, form(MaterialPrefix.dustTiny, "Tin", 1), new Snapshot()),
                Action.LEGACY_UNSUPPORTED, Reason.ITEM_COMPOSITION_OVERRIDE);
        outcome(inspectForm(MaterialPrefix.dustSmall, tin, new ItemStack(Items.IRON_SWORD), new Snapshot()),
                Action.LEGACY_UNSUPPORTED, Reason.ITEM_COMPOSITION_OVERRIDE);
        var tagged = small.copy(); tagged.getOrCreateTag().putBoolean("thermal_input_fixture", true);
        outcome(inspectForm(MaterialPrefix.dustSmall, tin, tagged, new Snapshot()), Action.LEGACY_UNSUPPORTED, Reason.ITEM_COMPOSITION_OVERRIDE);
        outcome(inspectForm(MaterialPrefix.dustSmall, tin, small.copyWithCount(2), new Snapshot()),
                Action.LEGACY_UNSUPPORTED, Reason.ITEM_COMPOSITION_OVERRIDE);
        var foreign = new GTMaterial(tin.getId(), "Tin", "Unregistered thermal input fixture", 0);
        outcome(inspectForm(MaterialPrefix.dustSmall, foreign, small, new Snapshot()), Action.LEGACY_UNSUPPORTED, Reason.UNKNOWN_SOURCE_IDENTITY);
        for (long units : new long[]{-1, GTValues.U}) outcome(inspectForm(MaterialPrefix.dustSmall, tin, small, new Snapshot() {
            @Override long inputUnits(GTMaterial material, MaterialPrefix prefix, ItemStack stack) { return units; }
        }), Action.LEGACY_UNSUPPORTED, Reason.ITEM_COMPOSITION_OVERRIDE);
        for (String name : List.of("Graphene", "Scandium", "PetroleumCoke")) {
            var reason = name.equals("Graphene") ? Reason.SOURCE_BLACKLISTED
                    : name.equals("Scandium") ? Reason.SOURCE_NO_DEFAULT_PHASE : Reason.SOURCE_NOT_MELTING;
            outcome(inspectForm(MaterialPrefix.dustTiny, material(name), form(MaterialPrefix.dustTiny, name, 1), new Snapshot()),
                    Action.SOURCE_NO_ROW, reason);
        }
        outcome(inspectForm(MaterialPrefix.dustSmall, tin, small, new Snapshot(FluidCatalog.all(),
                (field, amount) -> field.equals("GenMolten_Tin") ? FluidStack.EMPTY : GTFluids.stack(field, amount))),
                Action.DEFERRED_NO_ROW, Reason.REQUIRED_NATIVE_FLUID_ABSENT);
        outcome(inspectForm(MaterialPrefix.dustSmall, tin, small, new Snapshot(FluidCatalog.all(),
                (field, amount) -> new FluidStack(Fluids.WATER, amount))), Action.LEGACY_UNSUPPORTED, Reason.FLUID_BINDING_OVERRIDE);
        outcome(inspectForm(MaterialPrefix.dustSmall, tin, small, new Snapshot() {
            @Override FluidCatalog.FluidEntry definition(String field) {
                var entry = super.definition(field);
                return field.equals("GenMolten_Tin") ? entry.withTemperature(entry.temperature()) : entry;
            }
        }), Action.LEGACY_UNSUPPORTED, Reason.FLUID_BINDING_OVERRIDE);
        for (Reason reason : List.of(Reason.MATERIAL_TARGET_OVERRIDE, Reason.MATERIAL_ELIGIBILITY_OVERRIDE))
            outcome(inspectForm(MaterialPrefix.dustSmall, tin, small, new Snapshot() {
                @Override Reason materialOverride(GTMaterial material, SourceRecyclingFacts.Facts source) { return reason; }
            }), Action.LEGACY_UNSUPPORTED, reason);
        outcome(inspectForm(MaterialPrefix.dustSmall, tin, small, new Snapshot() {
            @Override SourceThermalRecyclingRules.Stats stats(GTMaterial input, GTMaterial target) {
                return new SourceThermalRecyclingRules.Stats(Double.NaN, target.getMeltingPoint());
            }
        }), Action.DEFERRED_NO_ROW, Reason.INVALID_RUNTIME_STATS);
        var cold = inspectForm(MaterialPrefix.dustSmall, tin, small, new Snapshot() {
            @Override SourceThermalRecyclingRules.Stats stats(GTMaterial input, GTMaterial target) {
                return new SourceThermalRecyclingRules.Stats(0, target.getMeltingPoint());
            }
        });
        require(cold.action() == Action.ROWS && cold.rows().size() == 2
                && cold.rows().stream().allMatch(r -> r.ticks() == 16 && r.output().getAmount() == 36),
                "runtime density affects heat cost, not verified fraction mass or output volume");
        var snapshot = new SharedThermalInputRows.Snapshot();
        var single = SharedThermalInputRows.definitions(List.of(tin), snapshot);
        var repeated = SharedThermalInputRows.definitions(List.of(tin, tin, GTMaterialRegistry.get("Tin")), new SharedThermalInputRows.Snapshot());
        require(!single.isEmpty() && repeated.size() == single.size(), "material aliases do not multiply selected contracts");
        var missing = SharedThermalInputRows.definitions(List.of(tin), new SharedThermalInputRows.Snapshot() {
            @Override ItemStack selected(MaterialPrefix prefix, GTMaterial material) {
                return prefix == MaterialPrefix.dustTiny ? ItemStack.EMPTY : super.selected(prefix, material);
            }
        });
        require(missing.size() == single.size() - 2 && missing.stream().noneMatch(d -> d.prefix() == MaterialPrefix.dustTiny),
                "one missing shape defers only its two machine rows, without a joint all-form gate");
        var unknown = SharedThermalInputRows.definitions(List.of(tin), new SharedThermalInputRows.Snapshot() {
            @Override long inputUnits(GTMaterial material, MaterialPrefix prefix, ItemStack stack) { return -1; }
        });
        require(unknown.isEmpty(), "unknown input evidence cannot invoke an old generic fallback");
        var diamond = form(MaterialPrefix.gem, "Diamond", 1); var vanilla = new ItemStack(Items.DIAMOND);
        require(MaterialEquivalence.matches(diamond, vanilla), "actual native/vanilla gem alias has the same prefix and material");
        var diamondRecipe = row(MachineRecipeMaps.Smelter, diamond);
        require(MachineRecipeMaps.Smelter.findRecipe(List.of(vanilla), List.of(), false, 1, 1) == diamondRecipe,
                "equivalent alias uses the same accepted row, not another producer claim");
        var aliasRemainder = RecipeInputs.consume(diamondRecipe, List.of(vanilla), List.of(), 1);
        require(aliasRemainder != null && aliasRemainder.items().get(0).isEmpty(), "runtime recovery guard preserves clean equivalent alias consumption");
        for (var map : MAPS) {
            var recipe = row(map, small);
            for (String key : List.of("Items", "BlockEntityTag", "EntityTag")) {
                var payload = small.copy();
                if (key.equals("Items")) payload.getOrCreateTag().put(key, new net.minecraft.nbt.ListTag());
                else payload.getOrCreateTag().put(key, new net.minecraft.nbt.CompoundTag());
                require(map.findRecipe(List.of(payload), List.of(), false, 1, map.mOutputItemsCount) == null
                        && RecipeInputs.consume(recipe, List.of(payload), List.of(), 1) == null,
                        "actual admitted row refuses stored payload at lookup AND consumption: " + map.mNameInternal + "/" + key);
            }
        }
        // Source generation conditions are not a listener permission gate for an actual legacy form.
        var obsidian = form(MaterialPrefix.gemChipped, "Obsidian", 1);
        var legacyForm = row(MachineRecipeMaps.Smelter, obsidian);
        check(legacyForm, obsidian, "Lava", 27, -1); // floor((U/4) * 1000 / (9U)).
        require(SharedThermalInputRows.owned().contains(legacyForm), "registered legacy gem is processed without inventing a generation whitelist");
    }

    private static void ownOnlyLifecycle(List<Recipe> baseline) {
        var input = form(MaterialPrefix.dustSmall, "Tin", 1); var map = MachineRecipeMaps.Smelter;
        var original = row(map, input); int expected = SharedThermalInputRows.owned().size();
        require(SharedThermalInputRows.owned().contains(original) && map.unlink(original), "isolate one actual new owned row");
        var foreign = new Recipe(new ItemStack[]{input.copy()}, new ItemStack[0], null, null, new FluidStack[0],
                new FluidStack[]{fluid("Water", 17)}, 91, 7, 0);
        try {
            require(map.addRecipe(foreign, true, false, true) == foreign, "foreign hidden recipe admitted through normal collision authority");
            for (int repeat = 0; repeat < 2; repeat++) {
                var previous = SharedThermalInputRows.owned(); SharedThermalInputRows.rebuild();
                require(SharedThermalInputRows.owned().size() == expected - 1
                        && SharedThermalInputRows.owned().stream().noneMatch(previous::contains)
                        && !SharedThermalInputRows.owned().contains(foreign) && row(map, input) == foreign
                        && foreign.mHidden && !foreign.mFakeRecipe, "foreign identity/flags win while own rows replace without accumulation");
                require(preservedRows().equals(baseline), "own-only rebuild preserves default and explicit objects");
            }
            var owned = SharedThermalInputRows.owned(); SharedThermalInputRows.clear();
            require(SharedThermalInputRows.owned().isEmpty() && owned.stream().noneMatch(r -> MAPS.stream().anyMatch(m -> m.mRecipeList.contains(r)))
                    && map.mRecipeList.contains(foreign) && foreign.mHidden, "clear unlinks only accepted own objects");
            require(preservedRows().equals(baseline), "clear retains earlier producers and exact import costs");
        } finally { map.unlink(foreign); SharedThermalInputRows.rebuild(); }
        require(SharedThermalInputRows.owned().size() == expected, "initialized scope rebuilds after clear without losing or adding contracts");
    }

    private static void exactComposition(ItemStack input, MaterialPrefix prefix, GTMaterial material, long units) {
        var data = ItemMaterialRegistry.explicit(input.getItem()).orElseThrow(() -> new AssertionError("missing explicit native composition " + input));
        require(data.recoverable() && data.prefix() == prefix && data.components().size() == 1 && data.material().resolve() == material
                && data.amount() == units && prefix.getMaterialWeight() == units && input.getCount() == 1,
                "real single component and source prefix mass: " + input + ", data=" + data);
    }
    private static void outcome(Outcome result, Action action, Reason reason) {
        require(result.action() == action && result.reason() == reason && result.rows().isEmpty(),
                "expected " + action + "/" + reason + ", actual=" + result);
    }
    private static void check(Recipe recipe, ItemStack input, String field, int amount, long ticks) {
        require(recipe.mInputs.length == 1 && ItemStack.matches(recipe.mInputs[0], input) && recipe.mOutputs.length == 0
                && recipe.mFluidInputs.length == 0 && recipe.mFluidOutputs.length == 1 && recipe.mSpecialValue == 0
                && recipe.mEUt == 16 && recipe.mDuration >= (field.equals("Lava") ? 5L * amount : 16)
                && (ticks < 0 || recipe.mDuration == ticks), "exact thermal slots, input and cost for " + input
                        + ": expected ticks=" + ticks + ", actual=" + recipe.mDuration + "/" + recipe.mEUt);
        require(recipe.mFluidOutputs[0].isFluidEqual(fluid(field, amount)) && recipe.mFluidOutputs[0].getAmount() == amount,
                "expected " + field + "/" + amount + ", actual=" + recipe.mFluidOutputs[0]);
    }
    private static List<Recipe> rows(RecipeMap map, ItemStack input) {
        return map.mRecipeList.stream().filter(r -> r.mInputs.length == 1 && r.mFluidInputs.length == 0
                && ItemStack.matches(r.mInputs[0], input)).toList();
    }
    private static Recipe row(RecipeMap map, ItemStack input) {
        var matches = rows(map, input);
        require(matches.size() == 1, map + " input " + input + ": expected one row, actual=" + matches.size());
        require(map.findRecipe(List.of(input), List.of(), false, 1, map.mOutputItemsCount) == matches.get(0), "indexed native lookup returns exact accepted object");
        return matches.get(0);
    }
    private static GTMaterial material(String name) {
        var material = GTMaterialRegistry.get(name).resolve();
        require(material.isValid() && material.getName().equals(name), "canonical material " + name); return material;
    }
    private static ItemStack form(MaterialPrefix prefix, String name, int count) {
        var stack = GTItems.getStack(prefix, material(name), count);
        require(!stack.isEmpty() && stack.getCount() == count, "real form " + prefix.getName() + "/" + name); return stack;
    }
    private static FluidStack fluid(String field, int amount) {
        var stack = GTFluids.stack(field, amount); require(!stack.isEmpty() && stack.getAmount() == amount, "real phase " + field); return stack;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
