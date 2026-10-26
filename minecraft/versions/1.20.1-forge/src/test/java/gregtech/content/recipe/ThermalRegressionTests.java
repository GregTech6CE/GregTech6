package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.FluidCatalog;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.util.OM;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import static gregtech.content.recipe.SourceThermalNativeAdapter.*;

/** Default one-unit native thermal rows, not full recycling-listener or external-provider parity.
 * Independent source expectations: MT417/419/1302/3717, Loader_Fluids191/214, OreProcessing289-291.
 * Controlled snapshots below model missing/overridden evidence without changing global registries.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ThermalRegressionTests {
    private ThermalRegressionTests() {}

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void sourceThermalAfterStartup(GameTestHelper helper) {
        require(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY
                && MachineRecipeOverlay.applications() > 0, "native registration and server overlay must finish");
        require(GTValues.U == 648648000L, "source material unit, not a fluid-volume unit");
        defaultDispatchInventory();
        positive("Nickel", "GenMolten_Nickel", 144, 888, false);
        positive("Zinc", "Zinc", 144, 197, true);
        positive("Brass", "Brass", 144, 511, true);
        // Calcite's own smelting target is Calcite: source Melter exception despite no FURNACE.
        positive("Calcite", "Calcite", 144, 69, true);
        require(material("Lead").getMeltingPoint() == 600, "current Lead target temperature is600K");
        positive("Galena", "GenMolten_Lead", 48, 185, true);
        positive("Plastic", "Plastic", 666, 16, true); // floor((2U/3)*1000/U), not144 or667.
        positive("Alumina", "GenMolten_Alumina", 504, 153, false);
        require(fluid("GenMolten_Alumina", 1).getFluid() != fluid("GenMolten_Al2O3", 1).getFluid(),
                "source Alumina binding is not the compatibility short-name fluid");
        for (String field : List.of("Zinc", "Brass", "Calcite", "Plastic")) {
            var declaration = switch (field) {
                case "Zinc" -> FluidCatalog.Zinc;
                case "Brass" -> FluidCatalog.Brass;
                case "Calcite" -> FluidCatalog.Calcite;
                default -> FluidCatalog.Plastic;
            };
            require(FluidCatalog.get(field) == declaration
                    && GTFluids.entryForFluid(fluid(field, 1).getFluid()) == declaration,
                    field + " actual static declaration/holder identity");
        }
        // Source ObsidianRefined targets Lava, not its own port-generated molten artifact.
        positive("Ice", "Water", 1000, 16, false);
        positive("ObsidianRefined", "Lava", 111, 555, false); // floor(1000/9), then80EU/mB /16EU/t.
        var lava = fluid("Lava", 1);
        require(lava.getFluid() == Fluids.LAVA && new Snapshot().temperature(lava)
                == lava.getFluid().getFluidType().getTemperature(lava), "actual vanilla FluidType temperature");
        require(new Snapshot().temperature(lava) != FluidCatalog.Lava.temperature(),
                "vanilla Lava temperature must not be substituted by its300K catalog declaration");
        // Shipped post-init fuel metadata adds MELTING; this is not an external override or source permission.
        for (String coke : List.of("PetroleumCoke", "LigniteCoke")) {
            require(material(coke).has(MaterialProperty.MELTING), coke + " actual post-init fuel default");
            noDefaultRow(coke, Action.SOURCE_NO_ROW, Reason.SOURCE_NOT_MELTING);
        }
        noDefaultRow("Graphene", Action.SOURCE_NO_ROW, Reason.SOURCE_BLACKLISTED);
        noDefaultRow("Scandium", Action.SOURCE_NO_ROW, Reason.SOURCE_NO_DEFAULT_PHASE);
        require(!fluid("GenMolten_Graphene", 1).isEmpty() && !fluid("GenMolten_Scandium", 1).isEmpty(),
                "native generated fluids do not establish source eligibility or a source phase");
        for (String missing : List.of("TungstenTrioxide", "Carbon14")) {
            noDefaultRow(missing, Action.DEFERRED_NO_ROW, Reason.REQUIRED_NATIVE_FLUID_ABSENT);
            require(SourceThermalNativeAdapter.diagnostics().stream().anyMatch(d -> d.material().equals(missing)
                    && d.action() == Action.DEFERRED_NO_ROW && d.reason() == Reason.REQUIRED_NATIVE_FLUID_ABSENT),
                    missing + " startup producer records deferral, never revives legacy144mB");
        }
        reviewedRows();
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void missingBindingsOverridesAndRuntimeStats(GameTestHelper helper) {
        var nickel = material("Nickel");
        var input = selected("Nickel");
        outcome(inspect(nickel, ItemStack.EMPTY, new Snapshot()), Action.DEFERRED_NO_ROW, Reason.INPUT_ABSENT);
        outcome(inspect(nickel, input, new Snapshot() {
            @Override FluidCatalog.FluidEntry definition(String field) {
                return field.equals("GenMolten_Nickel") ? null : super.definition(field);
            }
        }), Action.DEFERRED_NO_ROW, Reason.REQUIRED_NATIVE_FLUID_ABSENT);
        outcome(inspect(nickel, input, new Snapshot(FluidCatalog.all(),
                (field, amount) -> field.equals("GenMolten_Nickel") ? FluidStack.EMPTY : GTFluids.stack(field, amount))),
                Action.DEFERRED_NO_ROW, Reason.REQUIRED_NATIVE_FLUID_ABSENT);
        // Equal values are not proof of the live canonical catalog definition.
        outcome(inspect(nickel, input, new Snapshot() {
            @Override FluidCatalog.FluidEntry definition(String field) {
                var entry = super.definition(field);
                return field.equals("GenMolten_Nickel") ? entry.withTemperature(entry.temperature()) : entry;
            }
        }), Action.LEGACY_UNSUPPORTED, Reason.FLUID_BINDING_OVERRIDE);
        outcome(inspect(nickel, input, new Snapshot(FluidCatalog.all(),
                (field, amount) -> field.equals("GenMolten_Nickel") ? new FluidStack(Fluids.WATER, amount)
                        : GTFluids.stack(field, amount))), Action.LEGACY_UNSUPPORTED, Reason.FLUID_BINDING_OVERRIDE);
        var tagged = input.copy();
        tagged.getOrCreateTag().putBoolean("thermal_fixture_payload", true);
        outcome(inspect(nickel, tagged, new Snapshot()), Action.LEGACY_UNSUPPORTED, Reason.ITEM_COMPOSITION_OVERRIDE);
        var doubleInput = input.copy(); doubleInput.setCount(2);
        outcome(inspect(nickel, doubleInput, new Snapshot()), Action.LEGACY_UNSUPPORTED, Reason.ITEM_COMPOSITION_OVERRIDE);
        // These controlled seams exercise dispatch boundaries, not actual addon integration.
        for (Reason reason : List.of(Reason.MATERIAL_TARGET_OVERRIDE, Reason.MATERIAL_ELIGIBILITY_OVERRIDE)) {
            outcome(inspect(nickel, input, new Snapshot() {
                @Override Reason materialOverride(GTMaterial material, SourceRecyclingFacts.Facts source) { return reason; }
            }), Action.LEGACY_UNSUPPORTED, reason);
        }
        outcome(inspect(material("Scandium"), selected("Scandium"), new Snapshot() {
            @Override boolean defaultNoPhase(GTMaterial material, SourceRecyclingFacts.Facts source) { return false; }
        }), Action.LEGACY_UNSUPPORTED, Reason.FLUID_BINDING_OVERRIDE);
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1, Double.MAX_VALUE}) {
            outcome(inspect(nickel, input, stats(invalid, 1728)), Action.DEFERRED_NO_ROW, Reason.INVALID_RUNTIME_STATS);
        }
        outcome(inspect(nickel, input, stats(8.912, -1)), Action.DEFERRED_NO_ROW, Reason.INVALID_RUNTIME_STATS);
        outcome(inspect(nickel, input, new Snapshot() {
            @Override int temperature(FluidStack fluid) { return -1; }
        }), Action.DEFERRED_NO_ROW, Reason.INVALID_RUNTIME_STATS);
        // Valid runtime statistics need not equal an imported/default density fingerprint.
        var changedDensity = inspect(nickel, input, stats(17.824, 1728));
        require(changedDensity.action() == Action.ROWS && changedDensity.rows().size() == 1
                && changedDensity.rows().get(0).ticks() == 1776, "valid runtime density changes source formula duration");
        // A controlled low target temperature makes actual Lava temperature observable rather than masked by max().
        var hotLava = inspect(material("ObsidianRefined"), selected("ObsidianRefined"), stats(100, 293));
        int actualTemperature = new FluidStack(Fluids.LAVA, 1).getFluid().getFluidType()
                .getTemperature(new FluidStack(Fluids.LAVA, 1));
        long expectedTicks = (long) Math.max(555, 100 * 111.111111 * (actualTemperature - 293) / 1600);
        require(expectedTicks > 555 && hotLava.action() == Action.ROWS && hotLava.rows().size() == 1
                && hotLava.rows().get(0).ticks() == expectedTicks && hotLava.rows().get(0).output().getAmount() == 111,
                "actual vanilla temperature and rational1000/(9U) binding reach native planner");
        // Ordinary admission retains an existing row; a candidate must not sweep or replace collisions.
        Recipe existing = row(MachineRecipeMaps.Smelter, input);
        var candidate = inspect(nickel, input, new Snapshot()).rows().get(0);
        require(candidate.admit() == null && row(MachineRecipeMaps.Smelter, input) == existing,
                "normal collision authority preserves existing native identity");
        Recipe independentIce = row(MachineRecipeMaps.Melter, selected("Ice"));
        check(independentIce, selected("Ice"), "Water", 1000, 2000);
        int applications = MachineRecipeOverlay.applications();
        MachineRecipeOverlay.apply(helper.getLevel().getRecipeManager());
        require(MachineRecipeOverlay.applications() == applications + 1
                && row(MachineRecipeMaps.Smelter, input) == existing, "thermal startup baseline survives real reload");
        require(row(MachineRecipeMaps.Melter, selected("Ice")) == independentIce,
                "independent fixed Ice Melter row survives thermal checks and real reload");
        positive("Nickel", "GenMolten_Nickel", 144, 888, false);
        noDefaultRow("Scandium", Action.SOURCE_NO_ROW, Reason.SOURCE_NO_DEFAULT_PHASE);
        reviewedRows();
        helper.succeed();
    }

    private static void defaultDispatchInventory() {
        var actions = new EnumMap<Action, Integer>(Action.class);
        var reasons = new EnumMap<Reason, Integer>(Reason.class);
        var unsupported = new ArrayList<String>();
        var unexpected = new ArrayList<String>();
        var seen = new HashSet<GTMaterial>();
        var snapshot = new Snapshot();
        for (var raw : GTMaterialRegistry.allMaterials()) {
            var material = raw.resolve();
            if (!material.isValid() || !seen.add(material) || material.has(MaterialProperty.ANTIMATTER)
                    || List.of("Cinnabar", "Titanium", "Iron", "Water", "Lava").contains(material.getName())) continue;
            var result = inspect(material, OM.dustOrIngot(material, GTValues.U), snapshot);
            actions.merge(result.action(), 1, Integer::sum);
            reasons.merge(result.reason(), 1, Integer::sum);
            if (result.action() == Action.LEGACY_UNSUPPORTED) {
                String detail = material.getName() + "/" + result.reason();
                unsupported.add(detail);
                if (!(material.getName().equals("Superconductor") && result.reason() == Reason.UNKNOWN_SOURCE_IDENTITY)
                        && !(material.getName().equals("Tritanium") && result.reason() == Reason.UNKNOWN_SOURCE_FACT))
                    unexpected.add(detail);
            }
        }
        System.out.println("[thermal-regression] default generic dispatch actions=" + actions + ", reasons=" + reasons
                + ", unsupported=" + unsupported + "; excludes reviewed Cinnabar/Ti/Fe and output-only Water/Lava");
        require(unexpected.isEmpty(), "unexpected default compatibility fallthrough: " + unexpected);
        require(actions.getOrDefault(Action.ROWS, 0) > 0 && actions.getOrDefault(Action.SOURCE_NO_ROW, 0) > 0
                && reasons.getOrDefault(Reason.REQUIRED_NATIVE_FLUID_ABSENT, 0) > 0,
                "dispatch inventory must observe positive, source-negative and missing-native decisions");
    }

    private static Snapshot stats(double density, long melting) {
        return new Snapshot() {
            @Override SourceThermalRecyclingRules.Stats stats(GTMaterial input, GTMaterial target) {
                return new SourceThermalRecyclingRules.Stats(density, melting);
            }
        };
    }
    private static void positive(String material, String field, int amount, long ticks, boolean melter) {
        ItemStack input = selected(material);
        var result = inspect(material(material), input, new Snapshot());
        require(result.action() == Action.ROWS && result.reason() == Reason.VERIFIED,
                material + " source/native proof: " + result.action() + "/" + result.reason());
        require(result.rows().size() == (melter ? 2 : 1), material + " exact source machine gate");
        for (var definition : result.rows()) {
            require(definition.map() == MachineRecipeMaps.Smelter || melter && definition.map() == MachineRecipeMaps.Melter,
                    material + " only intended thermal maps");
            require(ItemStack.matches(definition.input(), input) && definition.output().isFluidEqual(fluid(field, amount))
                    && definition.output().getAmount() == amount && definition.ticks() == ticks && definition.power() == 16,
                    material + " planned native input/phase/volume/duration, actual=" + definition);
        }
        check(row(MachineRecipeMaps.Smelter, input), input, field, amount, ticks);
        if (melter) check(row(MachineRecipeMaps.Melter, input), input, field, amount, ticks);
        else if (material.equals("Ice")) {
            // Separate Loader_Recipes_Chem import (GTChemGen321), not this generic source branch.
            check(row(MachineRecipeMaps.Melter, input), input, "Water", 1000, 2000);
        } else require(rows(MachineRecipeMaps.Melter, input).isEmpty(), material + " has no source Melter row");
    }
    private static void noDefaultRow(String name, Action action, Reason reason) {
        var input = selected(name);
        outcome(inspect(material(name), input, new Snapshot()), action, reason);
        require(rows(MachineRecipeMaps.Melter, input).isEmpty() && rows(MachineRecipeMaps.Smelter, input).isEmpty(),
                name + " source-owned negative/deferred decision must not fall through to legacy thermal rows");
    }
    private static void reviewedRows() {
        for (String name : List.of("Titanium", "Iron")) {
            int[] amounts = {144, 36, 16};
            long[] ticks = name.equals("Titanium") ? new long[]{519, 129, 57} : new long[]{830, 207, 92};
            var prefixes = new MaterialPrefix[]{MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny};
            for (int i = 0; i < prefixes.length; i++) {
                var input = GTItems.getStack(prefixes[i], material(name), 1);
                require(!input.isEmpty(), name + " reviewed fraction remains bound");
                check(row(MachineRecipeMaps.Smelter, input), input, "GenMolten_" + name, amounts[i], ticks[i]);
                require(rows(MachineRecipeMaps.Melter, input).isEmpty(), name + " reviewed no-Melter gate preserved");
            }
        }
        String[] inputs = {"dust", "dust_small", "dust_tiny", "crushed_purified", "crushed_purified_tiny",
                "crushed_centrifuged", "crushed_centrifuged_tiny", "block_dust"};
        int[] amounts = {48, 12, 5, 60, 6, 66, 7, 432};
        for (int i = 0; i < inputs.length; i++) {
            var input = item("gt6ce:" + inputs[i] + "_cinnabar");
            for (var map : List.of(MachineRecipeMaps.Melter, MachineRecipeMaps.Smelter))
                check(row(map, input), input, "GenLiquid_Mercury", amounts[i], i == 7 ? 34 : 16);
        }
        var tooSmall = GTItems.getStack(MaterialPrefix.dustDiv72, material("Cinnabar"), 1);
        require(!tooSmall.isEmpty() && rows(MachineRecipeMaps.Melter, tooSmall).isEmpty()
                && rows(MachineRecipeMaps.Smelter, tooSmall).isEmpty(), "reviewed zero-mB fraction stays absent");
    }
    private static void outcome(Outcome actual, Action action, Reason reason) {
        require(actual.action() == action && actual.reason() == reason && actual.rows().isEmpty(),
                "expected " + action + "/" + reason + ", actual=" + actual);
    }
    private static void check(Recipe recipe, ItemStack input, String field, int amount, long ticks) {
        String label = BuiltInRegistries.ITEM.getKey(input.getItem()) + " -> " + field;
        require(recipe.mDuration == ticks && recipe.mEUt == 16 && recipe.mSpecialValue == 0,
                label + " expected16EU/t," + ticks + "ticks, actual=" + recipe.mEUt + "/" + recipe.mDuration);
        require(recipe.mOutputs.length == 0 && recipe.mFluidInputs.length == 0 && recipe.mFluidOutputs.length == 1,
                label + " exact output and reagent slots");
        var output = recipe.mFluidOutputs[0];
        require(output.isFluidEqual(fluid(field, amount)) && output.getAmount() == amount,
                label + " expected" + amount + "mB, actual=" + BuiltInRegistries.FLUID.getKey(output.getFluid())
                        + "/" + output.getAmount());
    }
    private static List<Recipe> rows(RecipeMap map, ItemStack input) {
        return map.mRecipeList.stream().filter(r -> r.mInputs.length == 1 && r.mFluidInputs.length == 0
                && ItemStack.matches(r.mInputs[0], input)).toList();
    }
    private static Recipe row(RecipeMap map, ItemStack input) {
        var matches = rows(map, input);
        require(matches.size() == 1, map + " expected one row for " + BuiltInRegistries.ITEM.getKey(input.getItem())
                + ", actual=" + matches.size());
        require(map.findRecipe(List.of(input), List.of(), false, 1, 1) == matches.get(0), "indexed native thermal lookup");
        return matches.get(0);
    }
    private static ItemStack selected(String name) {
        var selected = OM.dustOrIngot(material(name), GTValues.U);
        require(!selected.isEmpty() && selected.getCount() == 1
                && ItemStack.matches(selected, GTItems.getStack(MaterialPrefix.dust, material(name), 1)),
                name + " actual current default selection is one dust, not all listener forms");
        return selected;
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
    private static FluidStack fluid(String field, int amount) {
        var fluid = GTFluids.stack(field, amount);
        require(fluid != null && !fluid.isEmpty() && fluid.getAmount() == amount, "bound fluid " + field);
        return fluid;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
