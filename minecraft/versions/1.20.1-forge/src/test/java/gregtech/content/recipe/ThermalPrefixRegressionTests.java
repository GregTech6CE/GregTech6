package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.item.MaterialItem;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
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

/** Ordinary registered one-component forms; not a claim about the unavailable source PrefixItem class. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ThermalPrefixRegressionTests {
    private ThermalPrefixRegressionTests() {}
    private static final List<RecipeMap> MAPS = List.of(MachineRecipeMaps.Melter, MachineRecipeMaps.Smelter);
    private record PrefixFact(MaterialPrefix prefix, long units) {}
    // Independent pinned OP quantities from the complete 68-prefix audit, not planner output.
    private static final List<PrefixFact> FACTS = List.of(
            new PrefixFact(MaterialPrefix.rockGt, 1459458000L),
            new PrefixFact(MaterialPrefix.nugget, 72072000L),
            new PrefixFact(MaterialPrefix.chunkGt, 162162000L),
            new PrefixFact(MaterialPrefix.billet, 432432000L),
            new PrefixFact(MaterialPrefix.ingot, 648648000L),
            new PrefixFact(MaterialPrefix.ingotDouble, 1297296000L),
            new PrefixFact(MaterialPrefix.ingotTriple, 1945944000L),
            new PrefixFact(MaterialPrefix.ingotQuadruple, 2594592000L),
            new PrefixFact(MaterialPrefix.ingotQuintuple, 3243240000L),
            new PrefixFact(MaterialPrefix.plateGemTiny, 72072000L),
            new PrefixFact(MaterialPrefix.plateGem, 648648000L),
            new PrefixFact(MaterialPrefix.plateTiny, 72072000L),
            new PrefixFact(MaterialPrefix.plate, 648648000L),
            new PrefixFact(MaterialPrefix.plateDouble, 1297296000L),
            new PrefixFact(MaterialPrefix.plateTriple, 1945944000L),
            new PrefixFact(MaterialPrefix.plateQuadruple, 2594592000L),
            new PrefixFact(MaterialPrefix.plateQuintuple, 3243240000L),
            new PrefixFact(MaterialPrefix.plateDense, 5837832000L),
            new PrefixFact(MaterialPrefix.plateCurved, 648648000L),
            new PrefixFact(MaterialPrefix.foil, 162162000L),
            new PrefixFact(MaterialPrefix.scrapGt, 72072000L),
            new PrefixFact(MaterialPrefix.stick, 324324000L),
            new PrefixFact(MaterialPrefix.stickLong, 648648000L),
            new PrefixFact(MaterialPrefix.bolt, 81081000L),
            new PrefixFact(MaterialPrefix.screw, 72072000L),
            new PrefixFact(MaterialPrefix.gearGt, 2594592000L),
            new PrefixFact(MaterialPrefix.gearGtSmall, 648648000L),
            new PrefixFact(MaterialPrefix.ring, 162162000L),
            new PrefixFact(MaterialPrefix.chain, 648648000L),
            new PrefixFact(MaterialPrefix.spring, 648648000L),
            new PrefixFact(MaterialPrefix.springSmall, 162162000L),
            new PrefixFact(MaterialPrefix.rotor, 2756754000L),
            new PrefixFact(MaterialPrefix.lens, 486486000L),
            new PrefixFact(MaterialPrefix.round, 72072000L),
            new PrefixFact(MaterialPrefix.itemCasing, 324324000L),
            new PrefixFact(MaterialPrefix.wireFine, 81081000L),
            new PrefixFact(MaterialPrefix.minecartWheels, 648648000L),
            new PrefixFact(MaterialPrefix.railGt, 162162000L),
            new PrefixFact(MaterialPrefix.toolHeadSword, 1225224000L),
            new PrefixFact(MaterialPrefix.toolHeadRawSword, 1297296000L),
            new PrefixFact(MaterialPrefix.toolHeadPickaxe, 1873872000L),
            new PrefixFact(MaterialPrefix.toolHeadRawPickaxe, 1945944000L),
            new PrefixFact(MaterialPrefix.toolHeadShovel, 576576000L),
            new PrefixFact(MaterialPrefix.toolHeadRawShovel, 648648000L),
            new PrefixFact(MaterialPrefix.toolHeadSpade, 576576000L),
            new PrefixFact(MaterialPrefix.toolHeadRawSpade, 648648000L),
            new PrefixFact(MaterialPrefix.toolHeadAxe, 1873872000L),
            new PrefixFact(MaterialPrefix.toolHeadRawAxe, 1945944000L),
            new PrefixFact(MaterialPrefix.toolHeadAxeDouble, 3099096000L),
            new PrefixFact(MaterialPrefix.toolHeadRawAxeDouble, 3243240000L),
            new PrefixFact(MaterialPrefix.toolHeadHoe, 1225224000L),
            new PrefixFact(MaterialPrefix.toolHeadRawHoe, 1297296000L),
            new PrefixFact(MaterialPrefix.toolHeadHammer, 3891888000L),
            new PrefixFact(MaterialPrefix.toolHeadFile, 972972000L),
            new PrefixFact(MaterialPrefix.toolHeadChisel, 900900000L),
            new PrefixFact(MaterialPrefix.toolHeadRawChisel, 972972000L),
            new PrefixFact(MaterialPrefix.toolHeadSaw, 1225224000L),
            new PrefixFact(MaterialPrefix.toolHeadRawSaw, 1297296000L),
            new PrefixFact(MaterialPrefix.toolHeadScrewdriver, 648648000L),
            new PrefixFact(MaterialPrefix.toolHeadUniversalSpade, 504504000L),
            new PrefixFact(MaterialPrefix.toolHeadRawUniversalSpade, 576576000L),
            new PrefixFact(MaterialPrefix.toolHeadSense, 1873872000L),
            new PrefixFact(MaterialPrefix.toolHeadRawSense, 1945944000L),
            new PrefixFact(MaterialPrefix.toolHeadPlow, 2522520000L),
            new PrefixFact(MaterialPrefix.toolHeadRawPlow, 2594592000L),
            new PrefixFact(MaterialPrefix.toolHeadBuzzSaw, 2594592000L),
            new PrefixFact(MaterialPrefix.toolHeadArrow, 72072000L),
            new PrefixFact(MaterialPrefix.toolHeadRawArrow, 81081000L));
    private static final Set<MaterialPrefix> SCOPE = FACTS.stream().map(PrefixFact::prefix).collect(java.util.stream.Collectors.toSet());
    private static final List<MaterialPrefix> MASS_MISMATCH = List.of(MaterialPrefix.bouleGt, MaterialPrefix.plantGtBerry,
            MaterialPrefix.plantGtBlossom, MaterialPrefix.plantGtFiber, MaterialPrefix.plantGtWart,
            MaterialPrefix.toolHeadConstructionPickaxe, MaterialPrefix.toolHeadBuilderwand);
    private static final List<MaterialPrefix> SECONDARY = List.of(MaterialPrefix.plantGtTwig, MaterialPrefix.toolHeadPickaxeGem,
            MaterialPrefix.toolHeadDrill, MaterialPrefix.toolHeadChainsaw, MaterialPrefix.toolHeadWrench,
            MaterialPrefix.arrowGtWood, MaterialPrefix.arrowGtPlastic, MaterialPrefix.bulletGtSmall,
            MaterialPrefix.bulletGtMedium, MaterialPrefix.bulletGtLarge);
    private static final List<MaterialPrefix> SOURCE_NO = List.of(MaterialPrefix.dustImpure, MaterialPrefix.ingotHot,
            MaterialPrefix.crushed, MaterialPrefix.crushedTiny, MaterialPrefix.oreRaw);
    private static final List<MaterialPrefix> IDENTITY = List.of(MaterialPrefix.unit, MaterialPrefix.coin, MaterialPrefix.glasstube);
    private record Golden(String material, MaterialPrefix prefix, String fluid, int amount, boolean melter) {}
    // OPL255-291, OP141/167/170/196/216/224/239/240; MT439/414/3717 and reviewed Hg target.
    private static final List<Golden> GOLDENS = List.of(
            new Golden("Tin", MaterialPrefix.ingot, "GenMolten_Tin", 144, true),
            new Golden("Tin", MaterialPrefix.nugget, "GenMolten_Tin", 16, true),
            new Golden("Tin", MaterialPrefix.plate, "GenMolten_Tin", 144, true),
            new Golden("Tin", MaterialPrefix.gearGt, "GenMolten_Tin", 576, true),
            new Golden("Tin", MaterialPrefix.rotor, "GenMolten_Tin", 612, true),
            new Golden("Iron", MaterialPrefix.toolHeadSword, "GenMolten_Iron", 272, false),
            new Golden("Iron", MaterialPrefix.toolHeadPickaxe, "GenMolten_Iron", 416, false),
            new Golden("Galena", MaterialPrefix.rockGt, "GenMolten_Lead", 108, true),
            new Golden("Cinnabar", MaterialPrefix.rockGt, "GenLiquid_Mercury", 108, true));

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void sixtyEightThermalPrefixesUseRealInputs(GameTestHelper helper) {
        require(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY
                && MachineRecipeOverlay.applications() > 0, "actual registration and first overlay completed");
        inventory(); goldens(); exclusions(); helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void thermalPrefixIngredableAndOwnOnlyLifecycle(GameTestHelper helper) {
        var independent = independent();
        ingredableBoundaries(); aliasAndPayloads();
        lifecycle(independent);
        var previous = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>()); previous.addAll(additionalOwned());
        MachineRecipeOverlay.apply(helper.getLevel().getRecipeManager());
        require(additionalOwned().size() == previous.size() && additionalOwned().stream().noneMatch(previous::contains),
                "actual overlay rebuilds the additional scope without accumulating accepted objects");
        goldens(); preserve(independent);
        helper.succeed();
    }

    private static void inventory() {
        require(FACTS.size() == 68 && SCOPE.size() == 68, "complete independently named additional scope");
        var snapshot = new SharedThermalInputRows.Snapshot();
        var materials = Collections.newSetFromMap(new IdentityHashMap<GTMaterial, Boolean>());
        GTMaterialRegistry.allMaterials().forEach(m -> materials.add(m.resolve()));
        Map<String, Integer> selected = new TreeMap<>(), accepted = new TreeMap<>(), collisions = new TreeMap<>(), deferred = new TreeMap<>();
        Map<String, String> unprovedSelected = new TreeMap<>();
        for (var fact : FACTS) for (var material : materials) {
            var input = snapshot.selected(fact.prefix(), material);
            if (input.isEmpty()) continue;
            selected.merge(fact.prefix().getName(), 1, Integer::sum);
            if (!snapshot.ingredable(input) || snapshot.inputUnits(material, fact.prefix(), input) != fact.units()) {
                var proof = inspectForm(fact.prefix(), material, input, snapshot);
                require(proof.action() != Action.ROWS && proof.rows().isEmpty(),
                        "unproved selected alias cannot manufacture a thermal definition: " + input + "/" + proof);
                require(SharedThermalInputRows.admissions().stream().noneMatch(a -> a.accepted()
                        && a.prefix() == fact.prefix() && a.material().equals(material.getName())),
                        "unproved selected alias is not admitted: " + input);
                unprovedSelected.put(fact.prefix().getName() + "/" + material.getName(), proof.action() + "/" + proof.reason());
                continue;
            }
            exactInput(input, fact, material);
            require(input.getItem().getClass() == MaterialItem.class && !input.isDamageableItem() && input.getMaxDamage() == 0
                    && !input.hasCraftingRemainingItem() && input.getCraftingRemainingItem().isEmpty(),
                    "actual plain MaterialItem behavior proves ingredable: " + input);
        }
        require(selected.size() == 68, "all 68 prefixes have actual selected native holders, not merely definitions");
        var treatedPlate = form(MaterialPrefix.plate, "WoodTreated");
        require(!(treatedPlate.getItem() instanceof MaterialFormItem)
                && unprovedSelected.containsKey("plate/WoodTreated"),
                "registered placeable treated planks remain an explicitly unproved selected alias");
        outcome(inspectForm(MaterialPrefix.plate, material("WoodTreated"), treatedPlate, snapshot),
                Action.DEFERRED_NO_ROW, Reason.UNVERIFIED_INPUT_BEHAVIOR);
        var owned = additionalOwned();
        var admissions = SharedThermalInputRows.admissions().stream().filter(a -> SCOPE.contains(a.prefix())).toList();
        require(admissions.stream().filter(SharedThermalInputRows.Admission::accepted).count() == owned.size(),
                "actual accepted identities agree with ordinary admission outcomes");
        Map<Recipe, RecipeMap> live = new IdentityHashMap<>();
        for (var map : MAPS) for (var recipe : map.mRecipeList) live.put(recipe, map);
        Set<String> unique = new HashSet<>(); Set<MaterialPrefix> positivePrefixes = new HashSet<>();
        for (var recipe : owned) {
            var input = recipe.mInputs[0]; var typed = (MaterialFormItem)input.getItem();
            var prefix = typed.getPrefix(); var material = typed.getMaterial().resolve();
            var map = live.get(recipe); require(map != null, "owned row is actually live");
            String label = map.mNameInternal + "/" + prefix.getName() + "/" + material.getName();
            exactInput(input, fact(prefix), material);
            require(recipe.mOutputs.length == 0 && recipe.mFluidInputs.length == 0 && recipe.mFluidOutputs.length == 1
                    && !recipe.mFluidOutputs[0].isEmpty() && recipe.mEUt == 16 && recipe.mMaterialRecovery
                    && recipe.mRequiresEmptyContainerInputs, "admitted source slots/power/runtime guard: " + label);
            var fluid = recipe.mFluidOutputs[0];
            require(recipe.mDuration >= (fluid.getFluid() == Fluids.LAVA ? 5L * fluid.getAmount() : 16), "source phase-specific duration floor: " + label);
            require(map.findRecipe(List.of(input), List.of(), false, 1, 1) == recipe && unique.add(label),
                    "one indexed accepted contract per prefix/material/map: " + label);
            positivePrefixes.add(prefix); accepted.merge(map.mNameInternal + "/" + prefix.getName(), 1, Integer::sum);
        }
        require(positivePrefixes.equals(SCOPE), "all 68 prefixes have actual positive accepted witnesses");
        for (var admission : admissions) if (!admission.accepted()) collisions.merge(admission.map() + "/" + admission.prefix().getName(), 1, Integer::sum);
        for (var entry : SharedThermalInputRows.deferred()) if (SCOPE.contains(entry.prefix()))
            deferred.merge(entry.action() + "/" + entry.reason(), 1, Integer::sum);
        long prior = SharedThermalInputRows.owned().size() - owned.size();
        require(prior == 2408, "thirteen-prefix coverage retains 2396 prior rows and twelve independently checked Cinnabar gem rows");
        System.out.println("[thermal-prefix-regression] scope=68, actualSelected=" + selected.values().stream().mapToInt(Integer::intValue).sum()
                + ", actualAccepted=" + owned.size() + ", plannedAdmissions=" + admissions.size() + ", prior13Accepted=" + prior
                + ", selectedByPrefix=" + selected + ", acceptedByMapPrefix=" + accepted
                + ", collisionsByMapPrefix=" + collisions + ", unprovedSelected=" + unprovedSelected + ", deferred=" + deferred);
    }

    private static void goldens() {
        for (var expected : GOLDENS) {
            var input = form(expected.prefix(), expected.material());
            exactInput(input, fact(expected.prefix()), material(expected.material()));
            for (var map : expected.melter() ? MAPS : List.of(MachineRecipeMaps.Smelter)) {
                var recipe = row(map, input);
                check(recipe, input, expected.fluid(), expected.amount());
                require(SharedThermalInputRows.owned().contains(recipe), "new prefix is actually owned, not covered by the old dust producer");
                var consumed = RecipeInputs.consume(recipe, List.of(input), List.of(), 1);
                require(consumed != null && consumed.items().get(0).isEmpty(), "single selected input consumption");
            }
            if (!expected.melter()) require(rows(MachineRecipeMaps.Melter, input).isEmpty(), "Iron target does not acquire Melter permission from tool-head generation");
        }
    }

    private static void exclusions() {
        require(MASS_MISMATCH.size() == 7 && SECONDARY.size() == 10 && SOURCE_NO.size() == 5 && IDENTITY.size() == 3, "complete separate negative/unknown categories");
        for (var group : List.of(MASS_MISMATCH, SECONDARY, IDENTITY, SOURCE_NO)) for (var prefix : group) {
            var input = any(prefix); var typed = (MaterialFormItem)input.getItem();
            var result = inspectForm(prefix, typed.getMaterial(), input, new Snapshot());
            boolean no = group == SOURCE_NO;
            outcome(result, no ? Action.SOURCE_NO_ROW : Action.LEGACY_UNSUPPORTED,
                    no ? Reason.SOURCE_PREFIX_EXCLUDED : Reason.UNSUPPORTED_INPUT_PREFIX);
            require(!SCOPE.contains(prefix) && SharedThermalInputRows.owned().stream().noneMatch(r -> r.mInputs[0].getItem() instanceof MaterialFormItem f
                    && f.getPrefix() == prefix), "excluded prefix never acquires ownership: " + prefix.getName());
        }
        for (var prefix : List.of(MaterialPrefix.ingotHot, MaterialPrefix.glasstube)) {
            var data = ItemMaterialRegistry.explicit(any(prefix).getItem()).orElseThrow();
            require(data.recoverable() && data.components().size() == 1, "native recoverable/single-component is deliberately insufficient source evidence: " + prefix.getName());
        }
        require(GTItems.getStack(MaterialPrefix.bouleGt, material("Ruby"), 1).isEmpty(), "do not invent the nonexistent Ruby boule witness");
    }

    private static void ingredableBoundaries() {
        var tin = material("Tin"); var ingot = form(MaterialPrefix.ingot, "Tin"); var snapshot = new Snapshot();
        require(snapshot.ingredable(ingot) && snapshot.ingredable(form(MaterialPrefix.toolHeadSword, "Iron")), "real ordinary ingot and unfinished head are ingredable");
        // Use registered witnesses: constructing new Item instances after registry freeze is invalid.
        var durable = new ItemStack(Items.IRON_SWORD);
        var remainder = new ItemStack(Items.WATER_BUCKET);
        var subclass = any(MaterialPrefix.coin);
        require(durable.isDamageableItem() && durable.getMaxDamage() > 0, "registered durable witness");
        require(remainder.hasCraftingRemainingItem() && remainder.getCraftingRemainingItem().is(Items.BUCKET),
                "registered container/crafting remainder witness");
        require(subclass.getItem() instanceof MaterialItem && subclass.getItem().getClass() != MaterialItem.class,
                "registered CoinItem is a real MaterialItem subclass, not a synthetic registry entry");
        for (var unknown : List.of(durable, remainder, subclass)) {
            require(!snapshot.ingredable(unknown), "unproved item behavior cannot borrow ordinary MaterialItem permission");
            outcome(inspectForm(MaterialPrefix.ingot, tin, unknown, snapshot), Action.DEFERRED_NO_ROW, Reason.UNVERIFIED_INPUT_BEHAVIOR);
        }
        var denied = new SharedThermalInputRows.Snapshot() {
            @Override boolean ingredable(ItemStack stack) {
                return stack.getItem() instanceof MaterialFormItem f && f.getPrefix() == MaterialPrefix.ingot ? false : super.ingredable(stack);
            }
        };
        var normal = SharedThermalInputRows.definitions(List.of(tin), new SharedThermalInputRows.Snapshot());
        var reduced = SharedThermalInputRows.definitions(List.of(tin), denied);
        require(reduced.size() == normal.size() - 2 && reduced.stream().noneMatch(d -> d.prefix() == MaterialPrefix.ingot)
                && reduced.stream().anyMatch(d -> d.prefix() == MaterialPrefix.dustSmall), "unknown behavior defers only its new input, not the original thirteen forms");
        require(denied.deferred.stream().filter(d -> d.prefix() == MaterialPrefix.ingot && d.reason() == Reason.UNVERIFIED_INPUT_BEHAVIOR).count() == 1,
                "one unresolved input contract is diagnosed, not two manufactured machine recipes");
        var veto = new Snapshot() { @Override boolean ingredable(ItemStack stack) { return false; } };
        require(inspectForm(MaterialPrefix.dustSmall, tin, form(MaterialPrefix.dustSmall, "Tin"), veto).action() == Action.ROWS,
                "new ingredable gate does not silently rewrite original thirteen-prefix ownership");
        var absent = new SharedThermalInputRows.Snapshot() {
            @Override ItemStack selected(MaterialPrefix prefix, GTMaterial material) {
                return prefix == MaterialPrefix.rotor ? ItemStack.EMPTY : super.selected(prefix, material);
            }
        };
        require(SharedThermalInputRows.definitions(List.of(tin), absent).size() == normal.size() - 2, "missing rotor does not gate all other selected forms");
    }

    private static void aliasAndPayloads() {
        var input = form(MaterialPrefix.ingot, "Iron"); var recipe = row(MachineRecipeMaps.Smelter, input);
        var vanilla = new ItemStack(Items.IRON_INGOT);
        require(MachineRecipeMaps.Smelter.findRecipe(List.of(vanilla), List.of(), false, 1, 1) == recipe, "native/vanilla ingot alias uses one contract");
        var consumed = RecipeInputs.consume(recipe, List.of(vanilla), List.of(), 1);
        require(consumed != null && consumed.items().get(0).isEmpty(), "clean alias remains recoverable at consumption");
        var plate = form(MaterialPrefix.plate, "Tin");
        for (var map : MAPS) for (String key : List.of("Items", "BlockEntityTag", "EntityTag")) {
            var payload = plate.copy(); payload.getOrCreateTag().put(key, key.equals("Items") ? new net.minecraft.nbt.ListTag() : new net.minecraft.nbt.CompoundTag());
            require(map.findRecipe(List.of(payload), List.of(), false, 1, 1) == null
                    && RecipeInputs.consume(row(map, plate), List.of(payload), List.of(), 1) == null,
                    "new input refuses filled payload at lookup and consumption: " + key);
        }
    }

    private record Saved(RecipeMap map, ItemStack input, Recipe recipe) {}
    private static List<Saved> independent() {
        var result = new ArrayList<Saved>();
        for (String name : List.of("Tin", "Cinnabar")) for (var map : MAPS) {
            var input = form(MaterialPrefix.dust, name); result.add(new Saved(map, input, row(map, input)));
        }
        var iron = form(MaterialPrefix.dust, "Iron"); result.add(new Saved(MachineRecipeMaps.Smelter, iron, row(MachineRecipeMaps.Smelter, iron)));
        for (String name : List.of("Ice", "Snow")) for (var map : MAPS) {
            var input = form(MaterialPrefix.dustTiny, name); var recipe = row(map, input);
            check(recipe, input, "Water", 111); require(recipe.mDuration == 222, "explicit imported cost"); result.add(new Saved(map, input, recipe));
        }
        preserve(result); return List.copyOf(result);
    }
    private static void preserve(List<Saved> saved) {
        for (var prior : saved) require(row(prior.map(), prior.input()) == prior.recipe()
                && !SharedThermalInputRows.owned().contains(prior.recipe()), "earlier independent input keeps exact identity and owner");
    }
    private static void lifecycle(List<Saved> prior) {
        var map = MachineRecipeMaps.Smelter; var input = form(MaterialPrefix.plate, "Tin"); var original = row(map, input);
        int expected = additionalOwned().size();
        require(SharedThermalInputRows.owned().contains(original) && map.unlink(original), "isolate an actual new plate row");
        var foreign = new Recipe(new ItemStack[]{input.copy()}, new ItemStack[0], null, null, new FluidStack[0], new FluidStack[]{fluid("Water", 17)}, 91, 7, 0);
        try {
            require(map.addRecipe(foreign, true, false, true) == foreign, "ordinary foreign admission");
            SharedThermalInputRows.rebuild();
            require(additionalOwned().size() == expected - 1 && row(map, input) == foreign && foreign.mHidden && !foreign.mFakeRecipe
                    && !SharedThermalInputRows.owned().contains(foreign), "foreign object and flags retain normal priority");
            preserve(prior);
            var owned = SharedThermalInputRows.owned(); SharedThermalInputRows.clear();
            require(owned.stream().noneMatch(r -> MAPS.stream().anyMatch(m -> m.mRecipeList.contains(r)))
                    && map.mRecipeList.contains(foreign) && foreign.mHidden, "clear removes own accepted rows only");
            preserve(prior);
        } finally { map.unlink(foreign); SharedThermalInputRows.rebuild(); }
        require(additionalOwned().size() == expected, "rebuild after clear restores initialized scope without accumulation");
        preserve(prior);
    }

    private static List<Recipe> additionalOwned() {
        return SharedThermalInputRows.owned().stream().filter(r -> r.mInputs.length == 1
                && r.mInputs[0].getItem() instanceof MaterialFormItem f && SCOPE.contains(f.getPrefix())).toList();
    }
    private static PrefixFact fact(MaterialPrefix prefix) { return FACTS.stream().filter(f -> f.prefix() == prefix).findFirst().orElseThrow(); }
    private static void exactInput(ItemStack input, PrefixFact fact, GTMaterial material) {
        var data = ItemMaterialRegistry.explicit(input.getItem()).orElseThrow(() -> new AssertionError("no actual composition for " + input));
        require(input.getCount() == 1 && input.getItem() instanceof MaterialFormItem f && f.getPrefix() == fact.prefix()
                && f.getMaterial().resolve() == material && data.prefix() == fact.prefix() && data.components().size() == 1
                && data.material().resolve() == material && data.amount() == fact.units() && data.recoverable()
                && fact.prefix().getMaterialWeight() == fact.units() && GTItems.getStack(fact.prefix(), material, 1).getItem() == input.getItem(),
                "selected native holder and single component equal independent source amount: " + input + "/" + fact.units());
    }
    private static ItemStack any(MaterialPrefix prefix) {
        for (var material : GTMaterialRegistry.allMaterials()) { var stack = GTItems.getStack(prefix, material.resolve(), 1); if (!stack.isEmpty()) return stack; }
        throw new AssertionError("expected actual audited native prefix " + prefix.getName());
    }
    private static GTMaterial material(String name) { var value = GTMaterialRegistry.get(name).resolve(); require(value.isValid(), "real material " + name); return value; }
    private static ItemStack form(MaterialPrefix prefix, String name) { var value = GTItems.getStack(prefix, material(name), 1); require(!value.isEmpty(), "real form " + prefix.getName() + "/" + name); return value; }
    private static FluidStack fluid(String field, int count) { var value = GTFluids.stack(field, count); require(!value.isEmpty() && value.getAmount() == count, "real fluid " + field); return value; }
    private static void outcome(Outcome result, Action action, Reason reason) { require(result.action() == action && result.reason() == reason && result.rows().isEmpty(), "expected " + action + "/" + reason + ", actual=" + result); }
    private static List<Recipe> rows(RecipeMap map, ItemStack input) { return map.mRecipeList.stream().filter(r -> r.mInputs.length == 1 && r.mFluidInputs.length == 0 && ItemStack.matches(r.mInputs[0], input)).toList(); }
    private static Recipe row(RecipeMap map, ItemStack input) {
        var values = rows(map, input); require(values.size() == 1, map.mNameInternal + "/" + BuiltInRegistries.ITEM.getKey(input.getItem()) + " expected one row, actual=" + values.size());
        require(map.findRecipe(List.of(input), List.of(), false, 1, 1) == values.get(0), "indexed lookup returns actual accepted object"); return values.get(0);
    }
    private static void check(Recipe recipe, ItemStack input, String field, int amount) {
        require(recipe.mInputs.length == 1 && ItemStack.matches(recipe.mInputs[0], input) && recipe.mOutputs.length == 0 && recipe.mFluidInputs.length == 0
                && recipe.mFluidOutputs.length == 1 && recipe.mEUt == 16 && recipe.mDuration >= 16 && recipe.mFluidOutputs[0].isFluidEqual(fluid(field, amount))
                && recipe.mFluidOutputs[0].getAmount() == amount, "independent native golden " + input + " -> " + field + "/" + amount);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
