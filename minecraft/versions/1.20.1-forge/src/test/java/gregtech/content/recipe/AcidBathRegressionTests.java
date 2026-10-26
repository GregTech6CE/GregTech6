package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.FluidCatalog;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeInputs;
import gregtech.engine.recipe.RecipeMap;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import static gregtech.content.recipe.SharedOreWashingRows.NativeForm;
import static gregtech.content.recipe.SourcePurifiedAcidData.Phase;
import static gregtech.content.recipe.SourcePurifiedAcidRules.Shape;
import static gregtech.content.recipe.SourceRecyclingFacts.Identity;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;

/** Real whole-family admission/holders; expected representatives are pinned Ores108-159 literals. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class AcidBathRegressionTests {
    private AcidBathRegressionTests() {}
    private static final RecipeMap BATH = MachineRecipeMaps.Bath;
    private record F(String field, int amount) {}
    private record Golden(int line, int id, String material, F acid, String tail, int tailCount, List<F> fluids) {}
    // Independent audit goldens, not values calculated by SourcePurifiedAcidRules.
    private static final List<Golden> GOLDENS = List.of(
            vitriol(108, 120, "Magnesium", "PinkVitriol"), vitriol(112, 280, "Nickel", "CyanVitriol"),
            vitriol(116, 250, "Manganese", "GrayVitriol"), vitriol(120, 8750, "Desh", "MartianVitriol"),
            vitriol(124, 9104, "Hematite", "GreenVitriol"), vitriol(128, 290, "Copper", "BlueVitriol"),
            vitriol(132, 270, "Cobalt", "RedVitriol"), vitriol(136, 300, "Zinc", "WhiteVitriol"),
            new Golden(140, 9101, "Almandine", new F("GenLiquid_SulfuricAcid", 10500), "Almandine", 2,
                    List.of(new F("GenLiquid_VitriolOfClay", 8500), new F("Hydrogen", 3000))),
            new Golden(144, 440, "Ruthenium", new F("GenLiquid_AquaRegia", 9750), "PlatinumGroupSludge", 8,
                    List.of(new F("GenLiquid_ChloroplatinicAcid", 4500), new F("GenGas_NitrogenMonoxide", 1500), new F("Water", 4125))),
            new Golden(148, 230, "Vanadium", new F("GenGas_HydrogenFluoride", 500), "Vanadium", 2, List.of()),
            new Golden(152, 8334, "Coal", new F("GenGas_HydrogenFluoride", 500), "Graphite", 2, List.of()),
            new Golden(156, 9164, "Wollastonite", new F("GenGas_HydrochloricAcid", 4000), "ManganeseChloride", 6,
                    List.of(new F("Water", 3000), new F("GenGas_Chlorine", 1000))));
    private record Binding(int id, String name, String field, boolean gas) {}
    private static final List<Binding> BINDINGS = List.of(
            liquid(9827, "AquaRegia"), liquid(9824, "SulfuricAcid"), liquid(8401, "ChloroplatinicAcid"),
            liquid(8404, "BlueVitriol"), liquid(8405, "GreenVitriol"), liquid(8406, "RedVitriol"),
            liquid(8407, "PinkVitriol"), liquid(8408, "CyanVitriol"), liquid(8409, "WhiteVitriol"),
            liquid(8410, "GrayVitriol"), liquid(8411, "MartianVitriol"), liquid(8412, "VitriolOfClay"),
            new Binding(170, "Chlorine", "GenGas_Chlorine", true),
            new Binding(9826, "HydrochloricAcid", "GenGas_HydrochloricAcid", true),
            new Binding(9829, "HydrogenFluoride", "GenGas_HydrogenFluoride", true),
            new Binding(9837, "NitrogenMonoxide", "GenGas_NitrogenMonoxide", true),
            new Binding(10, "Hydrogen", "Hydrogen", true), new Binding(9800, "Water", "Water", false));

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void purifiedAcidFamiliesUseRealHolders(GameTestHelper helper) {
        require(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY
                && MachineRecipeOverlay.applications() > 0, "native registration and first overlay completed");
        inventory();
        goldens();
        imported();
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void purifiedAcidBoundariesAndOwnOnlyReload(GameTestHelper helper) {
        var imported = imported();
        var washing = SharedOreWashingRows.owned().stream().filter(BATH.mRecipeList::contains).toList();
        controlledDefinitions();
        phaseFailures();
        dynamicSelector();
        collisionAndClear(imported, washing);
        for (int reload = 0; reload < 2; reload++) {
            var previous = SharedPurifiedAcidRows.owned();
            MachineRecipeOverlay.apply(helper.getLevel().getRecipeManager());
            require(SharedPurifiedAcidRows.owned().size() == 180
                    && SharedPurifiedAcidRows.owned().stream().noneMatch(previous::contains), "overlay owns180 fresh accepted rows, without accumulation");
            require(imported().equals(imported), "all10 imported baseline objects survive the actual overlay");
        }
        inventory();
        goldens();
        helper.succeed();
    }

    private static void inventory() {
        var owned = SharedPurifiedAcidRows.owned();
        require(owned.size() == 180 && owned.stream().allMatch(BATH.mRecipeList::contains), "180 actual accepted Bath objects, not source-call count");
        require(SharedPurifiedAcidRows.admissions().size() == 180
                && SharedPurifiedAcidRows.admissions().stream().allMatch(SharedPurifiedAcidRows.Admission::accepted),
                "all180 ordinary admissions succeeded alongside180 actual owned objects");
        Set<Item> items = new HashSet<>(); Set<net.minecraft.world.level.material.Fluid> fluids = new HashSet<>();
        Set<GTMaterial> materials = new HashSet<>(); Map<String, Integer> byAcid = new TreeMap<>();
        int tiny = 0;
        for (var recipe : owned) {
            require(recipe.mInputs.length == 1 && recipe.mFluidInputs.length == 1 && recipe.mEUt == 0 && recipe.mDuration == 256,
                    "source input arity and0EU/256ticks are retained");
            var input = recipe.mInputs[0];
            require(input.getItem() instanceof MaterialFormItem, "registered material input, not a synthetic stack");
            var typed = (MaterialFormItem)input.getItem(); materials.add(typed.getMaterial().resolve());
            if (typed.getPrefix() == MaterialPrefix.crushedPurifiedTiny) { require(input.getCount() == 9, "tiny input is9"); tiny++; }
            else require(typed.getPrefix() == MaterialPrefix.crushedPurified && input.getCount() == 1, "only whole purified1 or tiny9 belong here");
            require(ItemStack.matches(recipe.mOutputs[0], form(MaterialPrefix.crushedCentrifuged, typed.getMaterial().getName(), 1)),
                    "both input shapes yield same-material whole refined1");
            for (var stack : recipe.mInputs) verifyItem(stack, items);
            for (var stack : recipe.mOutputs) verifyItem(stack, items);
            for (var stack : recipe.mFluidInputs) fluids.add(stack.getFluid());
            for (var stack : recipe.mFluidOutputs) fluids.add(stack.getFluid());
            byAcid.merge(BuiltInRegistries.FLUID.getKey(recipe.mFluidInputs[0].getFluid()).getPath(), 1, Integer::sum);
            require(find(input, recipe.mFluidInputs[0]) == recipe, "native indexed lookup returns each accepted tuple");
        }
        require(items.size() == 292 && fluids.size() == 18 && materials.size() == 76 && tiny == 90,
                "actual holder inventory: items=" + items.size() + ", phases=" + fluids.size() + ", materials=" + materials.size() + ", tiny=" + tiny);
        require(byAcid.equals(Map.of("sulfuricacid", 106, "aquaregia", 22, "hydrogenfluoride", 28, "hydrochloricacid", 24)),
                "independent fixed/dynamic family admission totals: " + byAcid);
        for (var binding : BINDINGS) {
            var material = material(binding.name()); var entry = FluidCatalog.all().get(binding.field());
            var stack = fluid(binding.field(), 1000);
            require(material.getId() == binding.id() && entry != null && entry.gas() == binding.gas()
                    && fluids.contains(stack.getFluid()), "exact material and phase: " + binding);
            String expectedId = binding.field().equals("Water") ? "minecraft:water" : "gt6ce:" + binding.name().toLowerCase(Locale.ROOT);
            require(BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString().equals(expectedId), "exact registered fluid holder: " + binding);
            if (binding.field().equals("Water")) require(stack.getFluid() == Fluids.WATER && entry == FluidCatalog.Water, "vanilla source water");
            else {
                require(GTFluids.entryForFluid(stack.getFluid()) == entry, "holder is bound to its exact catalogue declaration");
                if (binding.field().startsWith("Gen")) require(FluidCatalog.isGeneratedGasLiquidDefinition(binding.field(), entry)
                        && binding.name().equals(entry.materialKey()), "actual generated phase provenance");
                else require(entry == FluidCatalog.Hydrogen && FluidCatalog.isBuiltInDefinition(entry)
                        && binding.name().equals(FluidCatalog.boundMaterial(entry.registryName())), "static Hydrogen declaration provenance");
            }
        }
        var dynamic = owned.stream().filter(r -> r.mFluidInputs[0].getFluid() == fluid("GenGas_HydrochloricAcid", 1).getFluid())
                .map(r -> ((MaterialFormItem)r.mInputs[0].getItem()).getMaterial().getName()).collect(java.util.stream.Collectors.toSet());
        require(dynamic.equals(Set.of("Ilmenite", "Hematite", "Chromite", "Bromargyrite", "Manganese", "Columbite",
                "Tantalite", "Coltan", "Wollastonite", "Tungstate", "Scheelite", "MagnesiumCarbonate")), "actual12 default direct-MnO2 members");
        System.out.println("[acid-bath-regression] accepted=" + owned.size() + ", itemHolders=" + items.size()
                + ", phaseHolders=" + fluids.size() + ", inputIdentities=" + materials.size() + ", families=13, shapes=90whole/90tiny"
                + ", byAcid=" + byAcid + ", deferred=" + SharedPurifiedAcidRows.deferred());
    }

    private static void goldens() {
        for (var expected : GOLDENS) for (boolean tiny : new boolean[]{false, true}) {
            require(material(expected.material()).getId() == expected.id(), "independent golden identity " + expected.material());
            var input = form(tiny ? MaterialPrefix.crushedPurifiedTiny : MaterialPrefix.crushedPurified, expected.material(), tiny ? 9 : 1);
            var acid = fluid(expected.acid().field(), expected.acid().amount()); var recipe = row(input, acid);
            ItemStack[] outputs = new ItemStack[expected.line() == 156 ? 2 : 6];
            outputs[0] = form(MaterialPrefix.crushedCentrifuged, expected.material(), 1);
            for (int slot = 1; slot < outputs.length; slot++) outputs[slot] = form(expected.line() == 156 ? MaterialPrefix.dustSmall
                    : MaterialPrefix.crushedCentrifugedTiny, expected.tail(), expected.tailCount());
            long[] chances = expected.line() == 156 ? new long[]{10000, 10000} : new long[]{10000, 5000, 5000, 5000, 5000, 5000};
            check(recipe, input, acid, outputs, chances, expected.fluids().stream().map(f -> fluid(f.field(), f.amount())).toArray(FluidStack[]::new));
            require(SharedPurifiedAcidRows.owned().contains(recipe), "new family representative is genuinely owned: " + expected.line());
            if (expected.line() != 156) for (int slot = 1; slot < 6; slot++) {
                final boolean succeeds = slot % 2 == 1;
                require(recipe.rollOutputCount(slot, 1, bound -> succeeds ? 0 : bound - 1) == (succeeds ? expected.tailCount() : 0),
                        "five separate50% output slots remain independently rollable");
            }
            var consumed = RecipeInputs.consume(recipe, List.of(input), List.of(acid), 1);
            require(consumed != null && consumed.items().get(0).isEmpty() && consumed.fluids().get(0).isEmpty(), "actual input and acid consumption");
            if (tiny) require(find(input.copyWithCount(8), acid) == null, "eight tiny ores cannot satisfy the nine-item source input");
            var insufficient = acid.copy(); insufficient.setAmount(acid.getAmount() - 1);
            require(find(input, insufficient) == null, "an incomplete acid quantity cannot start the source recipe");
        }
    }

    private static List<Recipe> imported() {
        var result = new ArrayList<Recipe>();
        for (String name : List.of("Gold", "Copper", "Midasium", "Tin", "Cassiterite")) for (boolean tiny : new boolean[]{false, true}) {
            var input = form(tiny ? MaterialPrefix.crushedPurifiedTiny : MaterialPrefix.crushedPurified, name, tiny ? 9 : 1);
            var recipe = row(input, fluid("GenLiquid_AquaRegia", name.equals("Tin") || name.equals("Cassiterite") ? 13000 : 6500));
            require(!SharedPurifiedAcidRows.owned().contains(recipe) && recipe.mDuration == 256 && recipe.mEUt == 0
                    && recipe.mOutputs.length == 6 && !recipe.mHidden && !recipe.mFakeRecipe, "imported row remains independently owned: " + name);
            result.add(recipe);
        }
        require(new HashSet<>(result).size() == 10, "ten actual imported accepted rows remain, not ten source text calls");
        return List.copyOf(result);
    }

    private static void controlledDefinitions() {
        var magnesium = material("Magnesium"); var selected = List.of(magnesium);
        for (var absent : List.of(MaterialPrefix.crushedPurified, MaterialPrefix.crushedPurifiedTiny)) {
            var snapshot = new SharedPurifiedAcidRows.Snapshot() {
                @Override NativeForm item(MaterialPrefix prefix, Identity id, int count) {
                    return id.id() == 120 && prefix == absent ? NativeForm.absent() : super.item(prefix, id, count);
                }
            };
            var definitions = SharedPurifiedAcidRows.definitions(selected, snapshot);
            require(definitions.size() == 1 && definitions.get(0).input().getItem() == form(
                    absent == MaterialPrefix.crushedPurified ? MaterialPrefix.crushedPurifiedTiny : MaterialPrefix.crushedPurified,
                    "Magnesium", absent == MaterialPrefix.crushedPurified ? 9 : 1).getItem() && snapshot.deferred.isEmpty(),
                    "each known-absent input blocks only its own shape, without a joint full/tiny gate");
        }
        for (var absent : List.of(MaterialPrefix.crushedCentrifuged, MaterialPrefix.crushedCentrifugedTiny)) {
            var definitions = SharedPurifiedAcidRows.definitions(selected, new SharedPurifiedAcidRows.Snapshot() {
                @Override NativeForm item(MaterialPrefix prefix, Identity id, int count) {
                    return id.id() == 120 && prefix == absent ? NativeForm.absent() : super.item(prefix, id, count);
                }
            });
            require(definitions.size() == 2, "known missing solid does not invent a whole-row gate");
            for (var definition : definitions) {
                require(definition.outputs().length == 6, "source six raw positions retained before native normalization");
                for (int slot = 0; slot < 6; slot++) require(definition.outputs()[slot].isEmpty()
                        == (absent == MaterialPrefix.crushedCentrifuged ? slot == 0 : slot > 0),
                        "missing direct form remains EMPTY, never a dust substitute");
            }
        }
        var unknown = new SharedPurifiedAcidRows.Snapshot() {
            @Override NativeForm item(MaterialPrefix prefix, Identity id, int count) {
                return id.id() == 120 && prefix == MaterialPrefix.crushedCentrifugedTiny ? NativeForm.unknown() : super.item(prefix, id, count);
            }
        };
        require(SharedPurifiedAcidRows.definitions(selected, unknown).isEmpty() && unknown.deferred.size() == 2,
                "unknown output provenance explicitly defers both affected shapes");
        var oneUnknown = new SharedPurifiedAcidRows.Snapshot() {
            @Override NativeForm item(MaterialPrefix prefix, Identity id, int count) {
                return id.id() == 120 && prefix == MaterialPrefix.crushedPurifiedTiny ? NativeForm.unknown() : super.item(prefix, id, count);
            }
        };
        require(SharedPurifiedAcidRows.definitions(selected, oneUnknown).size() == 1 && oneUnknown.deferred.size() == 1,
                "unknown tiny input leaves whole input independent");
        var fixedAnti = new SharedPurifiedAcidRows.Snapshot() { @Override boolean anti(GTMaterial m) { return true; } };
        require(SharedPurifiedAcidRows.definitions(selected, fixedAnti).size() == 2, "fixed loops do not inherit the dynamic ANTIMATTER gate");
        var foreign = new GTMaterial(120, "Magnesium", "Unregistered acid fixture", 0);
        var provenance = new SharedPurifiedAcidRows.Snapshot();
        require(SharedPurifiedAcidRows.definitions(List.of(foreign), provenance).isEmpty() && !provenance.deferred.isEmpty(),
                "matching id/name without actual core identity does not acquire source ownership");
        for (int missing = 1; missing <= 3; missing++) {
            final int absent = missing;
            var definitions = SharedPurifiedAcidRows.definitions(List.of(material("Wollastonite")), new SharedPurifiedAcidRows.Snapshot() {
                @Override NativeForm item(MaterialPrefix prefix, Identity id, int count) {
                    boolean masked = prefix == MaterialPrefix.dustSmall || absent >= 2 && prefix == MaterialPrefix.dustTiny
                            || absent >= 3 && prefix == MaterialPrefix.dustDiv72;
                    return id.id() == 8031 && masked ? NativeForm.absent() : super.item(prefix, id, count);
                }
            });
            require(definitions.size() == 2, "MnCl2 optional output absence does not remove independent primary outputs");
            for (var definition : definitions) require(missing == 3 ? definition.outputs()[1].isEmpty()
                    : ItemStack.matches(definition.outputs()[1], form(missing == 1 ? MaterialPrefix.dustTiny : MaterialPrefix.dustDiv72,
                    "ManganeseChloride", missing == 1 ? 13 : 64)), "exact OM1.5U fallback, not rounded-up guessed dust");
        }
    }

    private static void phaseFailures() {
        var catalog = FluidCatalog.all();
        for (var phase : Phase.values()) {
            var normal = new SharedPurifiedAcidRows.Snapshot().fluid(phase, 1000);
            require(normal.availability() == Availability.PRESENT && sameFluid(normal.stack(), fluid(phase.nativeField(), 1000)),
                    "all18 source phases bind to actual runtime holders");
            var missing = new HashMap<>(catalog); missing.remove(phase.nativeField());
            require(new SharedPurifiedAcidRows.Snapshot(missing, GTFluids::stack).fluid(phase, 1000).availability() == Availability.UNKNOWN,
                    "missing native source-positive phase is UNKNOWN, not source ABSENT");
            var copied = new HashMap<>(catalog); var entry = copied.get(phase.nativeField());
            copied.put(phase.nativeField(), entry.withTemperature(entry.temperature()));
            require(new SharedPurifiedAcidRows.Snapshot(copied, GTFluids::stack).fluid(phase, 1000).availability() == Availability.UNKNOWN,
                    "an equal copied declaration is not factory/holder provenance");
        }
        var selected = List.of(material("Magnesium"));
        for (String unavailable : List.of("GenLiquid_SulfuricAcid", "GenLiquid_PinkVitriol", "Hydrogen")) {
            var missing = new SharedPurifiedAcidRows.Snapshot(catalog,
                    (field, amount) -> field.equals(unavailable) ? FluidStack.EMPTY : GTFluids.stack(field, amount));
            require(SharedPurifiedAcidRows.definitions(selected, missing).isEmpty() && missing.deferred.size() == 2,
                    "missing required input OR output holder defers both dependent shapes: " + unavailable);
        }
        for (var invalid : List.of(
                new SharedPurifiedAcidRows.Snapshot(catalog, (field, amount) -> new FluidStack(Fluids.WATER, amount)),
                new SharedPurifiedAcidRows.Snapshot(catalog, (field, amount) -> GTFluids.stack(field, amount - 1))))
            require(SharedPurifiedAcidRows.definitions(selected, invalid).isEmpty() && invalid.deferred.size() == 2,
                    "wrong holder or quantity cannot create free acid washing or discard outputs");
        var wrongPhase = new HashMap<>(catalog);
        wrongPhase.put("GenLiquid_SulfuricAcid", catalog.get("GenLiquid_SulfuricAcid").withGas());
        var gasAcid = new SharedPurifiedAcidRows.Snapshot(wrongPhase, GTFluids::stack);
        require(SharedPurifiedAcidRows.definitions(selected, gasAcid).isEmpty(), "gas SulfuricAcid cannot stand in for the proven liquid phase");
        var unrelated = new SharedPurifiedAcidRows.Snapshot(catalog,
                (field, amount) -> field.equals("GenGas_HydrogenFluoride") ? FluidStack.EMPTY : GTFluids.stack(field, amount));
        require(SharedPurifiedAcidRows.definitions(selected, unrelated).size() == 2, "missing HF cannot suppress a sulfuric reaction");
    }

    private static void dynamicSelector() {
        var snapshot = new SharedPurifiedAcidRows.Snapshot(); var copper = material("Copper");
        require(snapshot.dynamicDecision(material("Pyrolusite")) == Decision.NO && snapshot.dynamicDecision(copper) == Decision.NO,
                "neither input identity=MnO2 nor an unrelated composition is direct byproduct membership");
        var reordered = new SharedPurifiedAcidRows.Snapshot() {
            @Override List<Identity> directByproducts(GTMaterial material) {
                var values = new ArrayList<>(super.directByproducts(material)); Collections.reverse(values); return values;
            }
        };
        require(SharedPurifiedAcidRows.definitions(List.of(material("Wollastonite")), reordered).size() == 2,
                "direct contains does not depend on first slot or a frozen whole-list fingerprint");
        var newPositive = new SharedPurifiedAcidRows.Snapshot() {
            @Override List<Identity> directByproducts(GTMaterial material) {
                return material == copper ? List.of(new Identity(9126, "Pyrolusite")) : super.directByproducts(material);
            }
        };
        require(SharedPurifiedAcidRows.definitions(List.of(copper), newPositive).stream().filter(d -> d.producer().equals("manganese")).count() == 2,
                "proven core identity newly containing MnO2 is not blocked by the default12 audit roster");
        var uncertain = new SharedPurifiedAcidRows.Snapshot() {
            @Override List<Identity> directByproducts(GTMaterial material) { return Collections.singletonList(null); }
        };
        require(uncertain.dynamicDecision(copper) == Decision.UNKNOWN
                && SharedPurifiedAcidRows.definitions(List.of(copper), uncertain).size() == 2
                && uncertain.deferred.stream().filter(d -> d.producer().equals("manganese")).count() == 2,
                "unknown direct member defers only dynamic shapes while fixed family remains valid");
        var anti = new SharedPurifiedAcidRows.Snapshot() { @Override boolean anti(GTMaterial material) { return true; } };
        require(SharedPurifiedAcidRows.definitions(List.of(material("Wollastonite")), anti).isEmpty(), "dynamic ANTIMATTER exclusion remains effective");
    }

    private static void collisionAndClear(List<Recipe> imported, List<Recipe> washing) {
        var input = form(MaterialPrefix.crushedPurified, "Magnesium", 1); var acid = fluid("GenLiquid_SulfuricAcid", 3500);
        var original = row(input, acid);
        require(SharedPurifiedAcidRows.owned().contains(original) && BATH.unlink(original), "isolate one actually accepted new row, not an invented legacy object");
        var foreign = new Recipe(new ItemStack[]{input.copy()}, new ItemStack[]{new ItemStack(Items.DIAMOND)}, null,
                new long[]{10000}, new FluidStack[]{acid.copy()}, new FluidStack[0], 17, 7, 0);
        try {
            require(BATH.addRecipe(foreign, true, false, true) == foreign, "independent hidden collision admitted normally");
            for (int rebuild = 0; rebuild < 2; rebuild++) {
                SharedPurifiedAcidRows.rebuild();
                require(SharedPurifiedAcidRows.owned().size() == 179 && !SharedPurifiedAcidRows.owned().contains(foreign)
                        && row(input, acid) == foreign && foreign.mHidden && !foreign.mFakeRecipe,
                        "normal collision preserves foreign identity/flags and rejects only the matching candidate");
                require(SharedPurifiedAcidRows.admissions().size() == 180
                        && SharedPurifiedAcidRows.admissions().stream().filter(a -> !a.accepted()).count() == 1,
                        "one collision is reported separately from179 accepted rows");
                preserved(imported, washing);
            }
            var previouslyOwned = SharedPurifiedAcidRows.owned(); SharedPurifiedAcidRows.clear();
            require(SharedPurifiedAcidRows.owned().isEmpty() && previouslyOwned.stream().noneMatch(BATH.mRecipeList::contains)
                    && BATH.mRecipeList.contains(foreign) && foreign.mHidden, "clear removes only accepted own objects");
            preserved(imported, washing);
        } finally { BATH.unlink(foreign); SharedPurifiedAcidRows.rebuild(); }
        require(SharedPurifiedAcidRows.owned().size() == 180, "rebuild after clear reuses initialized scope without accumulating rows");
        preserved(imported, washing);
    }
    private static void preserved(List<Recipe> imported, List<Recipe> washing) {
        require(imported().equals(imported) && washing.stream().allMatch(BATH.mRecipeList::contains)
                && washing.stream().allMatch(SharedOreWashingRows.owned()::contains), "acid-only lifecycle preserves imported and washing objects");
        var cryo = fluid("Cryotheum", 1).getFluid();
        require(SharedOreWashingRows.owned().stream().filter(r -> r.mFluidInputs.length == 1 && r.mFluidInputs[0].getFluid() == cryo).count() == 39,
                "all39 independent Cryotheum rows remain untouched");
    }

    private static void verifyItem(ItemStack stack, Set<Item> seen) {
        require(!stack.isEmpty() && stack.getItem() instanceof MaterialFormItem, "bound direct material form");
        var typed = (MaterialFormItem)stack.getItem(); var material = typed.getMaterial().resolve();
        var holder = GTItems.getObject(typed.getPrefix(), material);
        require(GTMaterialRegistry.isCoreMaterial(material) && GTMaterialRegistry.get(material.getName()).resolve() == material
                && holder != null && holder.isPresent() && holder.get() == stack.getItem()
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(new ResourceLocation("gt6ce", typed.getPrefix().getItemId(material))),
                "exact canonical item holder/prefix/material: " + stack);
        seen.add(stack.getItem());
    }
    private static Golden vitriol(int line, int id, String material, String vitriol) {
        return new Golden(line, id, material, new F("GenLiquid_SulfuricAcid", 3500), material, 2,
                List.of(new F("GenLiquid_" + vitriol, 3000), new F("Hydrogen", 1000)));
    }
    private static Binding liquid(int id, String name) { return new Binding(id, name, "GenLiquid_" + name, false); }
    private static GTMaterial material(String name) {
        var material = GTMaterialRegistry.get(name).resolve();
        require(material.isValid() && material.getName().equals(name), "canonical material " + name); return material;
    }
    private static ItemStack form(MaterialPrefix prefix, String name, int count) {
        var stack = GTItems.getStack(prefix, material(name), count);
        require(!stack.isEmpty() && stack.getCount() == count, "real form " + prefix.getName() + "/" + name); return stack;
    }
    private static FluidStack fluid(String field, int amount) {
        var stack = GTFluids.stack(field, amount); require(!stack.isEmpty() && stack.getAmount() == amount, "real fluid " + field); return stack;
    }
    private static Recipe find(ItemStack input, FluidStack acid) { return BATH.findRecipe(List.of(input), List.of(acid), false, 1, BATH.mOutputItemsCount); }
    private static Recipe row(ItemStack input, FluidStack acid) {
        var matching = BATH.mRecipeList.stream().filter(r -> r.mEnabled && !r.mFakeRecipe && r.mInputs.length == 1
                && r.mInputs[0].getItem() == input.getItem() && new RecipeInputs.Matcher(List.of(input), List.of(acid)).matches(r, 1)).toList();
        require(matching.size() == 1, "exact native input/fluid tuple for " + input + "/" + acid + ", rows=" + matching.size());
        require(find(input, acid) == matching.get(0), "native indexed Bath lookup"); return matching.get(0);
    }
    private static void check(Recipe recipe, ItemStack input, FluidStack acid, ItemStack[] outputs, long[] chances, FluidStack[] fluids) {
        require(recipe.mInputs.length == 1 && ItemStack.matches(recipe.mInputs[0], input)
                && recipe.mEUt == 0 && recipe.mDuration == 256 && recipe.mSpecialValue == 0, "exact source input/count/cost");
        require(recipe.mOutputs.length == outputs.length && Arrays.equals(recipe.mChances, chances), "source output slots and ordered independent chances");
        for (int i = 0; i < outputs.length; i++) require(ItemStack.matches(recipe.mOutputs[i], outputs[i]), "source output slot" + i);
        require(recipe.mFluidInputs.length == 1 && sameFluid(recipe.mFluidInputs[0], acid) && recipe.mFluidOutputs.length == fluids.length,
                "source fluid slot count and exact acid");
        for (int i = 0; i < fluids.length; i++) require(sameFluid(recipe.mFluidOutputs[i], fluids[i]), "source output phase/quantity slot" + i);
    }
    private static boolean sameFluid(FluidStack left, FluidStack right) { return left.isFluidEqual(right) && left.getAmount() == right.getAmount(); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
