package gregtech.content.recipe.generated;

import static gregtech.engine.recipe.chem.FluidPhase.GAS;
import static gregtech.engine.recipe.chem.FluidPhase.LIQUID;
import static gregtech.engine.recipe.chem.Rounding.CEIL;
import static gregtech.engine.recipe.chem.Rounding.FLOOR;

import gregtech.api.material.GTValues;
import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialGroups;
import gregtech.content.registry.GTFluids;
import gregtech.engine.recipe.chem.ChemFluid;
import gregtech.engine.recipe.chem.ChemIngredient;
import gregtech.engine.recipe.chem.ChemRecipe;
import gregtech.engine.recipe.chem.MaterialAmount;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The original chemistry family ({@code Loader_Recipes_Chem}) in exact source terms.
 *
 * <p>Every row keeps the source's material units, phase and rounding. The producer never parses
 * legacy {@code omd:} or {@code m:} tokens: amounts are material units, and a material fluid is
 * resolved through the registered phase binding. Loops over source groups are expanded here so a
 * group is never reduced to a single representative item.</p>
 */
public final class GTChemRecipes {
    private static final String SOURCE = "gt6@3703e403/Loader_Recipes_Chem:";
    private static final int CERTAIN = 10_000;

    private final List<ChemRecipe> rows = new ArrayList<>();

    private GTChemRecipes() {}

    public static void register() {
        ChemRecipeResolver.clear();
        gregtech.engine.recipe.chem.MaterialFluidBinding.invalidate();
        new GTChemRecipes().run();
        int published = 0;
        for (gregtech.engine.recipe.chem.ChemResult result : ChemRecipeResolver.results()) {
            if (result.outcome() == gregtech.engine.recipe.chem.ChemOutcome.PUBLISHED
                    || result.outcome() == gregtech.engine.recipe.chem.ChemOutcome.SUBSTITUTED) published++;
        }
        ChemRecipeResolver.logSummary("GT6 chemistry recipes (" + published + " source rows resolved)");
    }

    private void run() {
        waterRows();
        saltAndAcidRows();
        metalRows();
        autoclaveAndAluminiumElectrolysisRows();
        waterElectrolysisRows();
        fermentationAndDistillationRows();
        cryoAndCrackingRows();
        bioAndPlasticRows();
        roastingRows();
        isotopeRows();
        meltingRows();
        dryingRows();
        portExtras();
        for (ChemRecipe row : rows) ChemRecipeResolver.publish(row);
    }

    // ── water loop: Loader_Recipes_Chem:37-96 ────────────────────────────────

    private void waterRows() {
        for (String water : new String[] {"Water", "MnWtr", "DistW"}) {
            int w1000 = 1000;
            int w750 = 750;
            int w3000 = 3000;
            int w2000 = 2000;
            int w30000 = 30000;
            int w1000b = 1000;
            int w6000 = 6000;
            int w18000 = 18000;

            row("Mixer", 16, 16, 38).item(dust("NaCl", U(1, 4)))
                    .fluid(nativeFluid(water, w750)).out(liquid("SaltWater", U(1))).add();
            row("Mixer", 16, 16, 39).item(dust("NaCl", U(1)))
                    .fluid(nativeFluid(water, w3000)).out(liquid("SaltWater", U(4))).add();
            row("Mixer", 16, 16, 40).item(dust("KCl", U(1, 4)))
                    .fluid(nativeFluid(water, w750)).out(liquid("SaltedWater", U(1))).add();
            row("Mixer", 16, 16, 41).item(dust("KCl", U(1)))
                    .fluid(nativeFluid(water, w3000)).out(liquid("SaltedWater", U(4))).add();
            row("Mixer", 16, 16, 42).item(dust("MgCl2", U(1)))
                    .fluid(nativeFluid(water, w2000)).out(dust("Bischofite", U(1))).add();
            row("Mixer", 16, 112, 43).item(dust("Na2SO4", U(7)))
                    .fluid(nativeFluid(water, w30000)).out(dust("Mirabilite", U(7))).add();
            row("Mixer", 16, 16, 44).item(dust("Na2CO3", U(1)))
                    .fluid(nativeFluid(water, w1000)).out(dust("Trona", U(1))).add();
            row("Mixer", 16, 16, 45).item(dust("CaSO4", U(1)))
                    .fluid(nativeFluid(water, w1000)).out(dust("Gypsum", U(1))).add();

            row("Mixer", 16, 112, 47).item(dust("Si", U(1)))
                    .fluid(nativeFluid(water, w6000)).out(gas("H", U(4), FLOOR)).out(dust("SiO2", U(3))).add();
            row("Mixer", 16, 112, 48).item(dust("Mn", U(1)))
                    .fluid(nativeFluid(water, w6000)).out(gas("H", U(4), FLOOR)).out(dust("MnO2", U(1))).add();
            row("Mixer", 16, 64, 49).item(dust("Na", U(1)))
                    .fluid(nativeFluid(water, w3000)).out(gas("H", U(1), FLOOR)).out(dust("NaOH", U(3))).add();
            row("Mixer", 16, 64, 50).item(dust("K", U(1)))
                    .fluid(nativeFluid(water, w3000)).out(gas("H", U(1), FLOOR)).out(dust("KOH", U(3))).add();
            row("Mixer", 16, 64, 51).item(dust("Li", U(1)))
                    .fluid(nativeFluid(water, w3000)).out(gas("H", U(1), FLOOR)).out(dust("LiOH", U(3))).add();

            row("Mixer", 16, 112, 53).item(dust("Ca", U(1)))
                    .fluid(gas("CO2", U(3), CEIL)).fluid(nativeFluid(water, w3000))
                    .out(gas("H", U(2), FLOOR)).out(dust("CaCO3", U(5))).add();
            row("Mixer", 16, 112, 54).item(dust("Mg", U(1)))
                    .fluid(gas("CO2", U(3), CEIL)).fluid(nativeFluid(water, w3000))
                    .out(gas("H", U(2), FLOOR)).out(dust("MgCO3", U(5))).add();

            row("Mixer", 16, 272, 56).item(dust("FeCl3", U(8)))
                    .fluid(nativeFluid(water, w6000)).fluid(gas("SO2", U(3), CEIL))
                    .out(liquid("H2SO4", U(7))).out(gas("HCl", U(4), FLOOR)).out(dust("FeCl2", U(6))).add();
            row("Mixer", 16, 208, 57).item(dust("FeCl3", U(4))).item(dust("NaOH", U(9)))
                    .fluid(nativeFluid(water, w18000)).out(liquid("SaltWater", U(24))).out(dust("FeO3H3", U(7))).add();
            row("Mixer", 16, 208, 58).item(dust("FeCl3", U(4))).item(dust("KOH", U(9)))
                    .fluid(nativeFluid(water, w18000)).out(liquid("SaltedWater", U(24))).out(dust("FeO3H3", U(7))).add();

            row("Mixer", 16, 64, 60).fluid(nativeFluid(water, w3000)).fluid(gas("Cl", U(2), CEIL))
                    .out(gas("HCl", U(4), FLOOR)).out(gas("O", U(1), FLOOR)).add();
            row("Mixer", 16, 64, 61).fluid(nativeFluid(water, w3000)).fluid(gas("SO3", U(4), CEIL))
                    .out(liquid("H2SO4", U(7))).add();
            row("Mixer", 16, 192, 62).fluid(nativeFluid(water, w3000)).fluid(gas("NO2", U(9), CEIL))
                    .out(liquid("HNO3", U(10))).out(gas("NO", U(2), FLOOR)).add();
            row("Mixer", 16, 224, 64).fluid(nativeFluid(water, w3000)).fluid(liquid("H2S2O7", U(11), CEIL))
                    .out(liquid("H2SO4", U(14))).add();

            row("Electrolyzer", 16, 1280, 66).item(form("dustSmall", "NaCl", 1))
                    .fluid(nativeFluid(water, 750))
                    .out(gas("Cl", U(1, 8))).out(gas("H", U(3, 8))).out(gas("O", U(1, 8))).out(dust("NaOH", U(3, 8))).add();
            row("Electrolyzer", 16, 5120, 67).item(form("dust", "NaCl", 1))
                    .fluid(nativeFluid(water, 3000))
                    .out(gas("Cl", U(1, 2))).out(gas("H", U(3, 2))).out(gas("O", U(1, 2))).out(dust("NaOH", U(3, 2))).add();
            row("Electrolyzer", 16, 1280, 68).item(form("dustSmall", "KCl", 1))
                    .fluid(nativeFluid(water, 750))
                    .out(gas("Cl", U(1, 8))).out(gas("H", U(3, 8))).out(gas("O", U(1, 8))).out(dust("KOH", U(3, 8))).add();
            row("Electrolyzer", 16, 5120, 69).item(form("dust", "KCl", 1))
                    .fluid(nativeFluid(water, 3000))
                    .out(gas("Cl", U(1, 2))).out(gas("H", U(3, 2))).out(gas("O", U(1, 2))).out(dust("KOH", U(3, 2))).add();
            row("Electrolyzer", 16, 1280, 70).item(form("dustSmall", "LiCl", 1))
                    .fluid(nativeFluid(water, 750))
                    .out(gas("Cl", U(1, 8))).out(gas("H", U(3, 8))).out(gas("O", U(1, 8))).out(dust("LiOH", U(3, 8))).add();
            row("Electrolyzer", 16, 5120, 71).item(form("dust", "LiCl", 1))
                    .fluid(nativeFluid(water, 3000))
                    .out(gas("Cl", U(1, 2))).out(gas("H", U(3, 2))).out(gas("O", U(1, 2))).out(dust("LiOH", U(3, 2))).add();

            vitriol(73, "BlueVitriol", "Cu", water, 3000);
            vitriol(74, "RedVitriol", "Co", water, 3000);
            vitriol(75, "PinkVitriol", "Mg", water, 3000);
            vitriol(76, "CyanVitriol", "Ni", water, 3000);
            vitriol(77, "WhiteVitriol", "Zn", water, 3000);
            vitriol(78, "GrayVitriol", "Mn", water, 3000);
            vitriol(79, "GreenVitriol", "Fe", water, 3000);
            vitriol(80, "MartianVitriol", "MeteoricIron", water, 3000);
            row("Electrolyzer", 64, 96, 81).kept(selector(1)).fluid(liquid("VitriolOfClay", U(17, 2), CEIL))
                    .fluid(nativeFluid(water, 4500)).out(liquid("H2SO4", U(21, 2))).out(dust("Al2O3", U(5, 2))).add();
            auric(83, "ChloroauricAcid", "Au", water, 4500);
            auric(84, "ChloroplatinicAcid", "Pt", water, 6000);
            auric(85, "StannicChloride", "Sn", water, 6000);

            row("Bath", 0, 2048, 91).item(dust("Mn", U(1)))
                    .fluid(nativeFluid(water, 6000)).out(gas("H", U(4), FLOOR)).out(dust("MnO2", U(1))).add();
            row("Bath", 0, 2048, 92).item(dust("Si", U(1)))
                    .fluid(nativeFluid(water, 6000)).out(gas("H", U(4), FLOOR)).out(dust("SiO2", U(3))).add();
            row("Bath", 0, 2048, 94).item(form("dust", "KAlO2", 4))
                    .fluid(nativeFluid(water, 6000)).out(form("dust", "AlO3H3", 4))
                    .out(form("dust", "AlO3H3", 3)).out(form("dust", "KOH", 3)).add();
            row("Bath", 0, 2048, 95).item(form("dust", "NaAlO2", 4))
                    .fluid(nativeFluid(water, 6000)).out(form("dust", "AlO3H3", 4))
                    .out(form("dust", "AlO3H3", 3)).out(form("dust", "NaOH", 3)).add();
        }
    }

    private void vitriol(int line, String input, String output, String water, int waterMb) {
        row("Electrolyzer", 64, 64, line).kept(selector(1))
                .fluid(liquid(input, U(6), CEIL)).fluid(nativeFluid(water, waterMb))
                .out(liquid("H2SO4", U(7), CEIL)).out(gas("O", U(1), FLOOR))
                .out(dust(output, line == 80 ? U(2, 3) : U(1))).add();
    }

    private void auric(int line, String input, String output, String water, int waterMb) {
        row("Electrolyzer", 64, line == 84 ? 96 : 64, line).kept(selector(1))
                .fluid(liquid(input, U(line == 84 ? 9 : line == 85 ? 5 : 6), CEIL))
                .fluid(nativeFluid(water, waterMb))
                .out(gas("HCl", line == 84 ? U(12) : U(8), CEIL))
                .out(gas("O", line == 84 ? U(2) : line == 85 ? U(2) : U(1, 2), FLOOR))
                .out(dust(output, U(1))).add();
    }

    // Remaining sections are appended below.

    // ── helpers ─────────────────────────────────────────────────────────────

    private Row row(String map, long eut, long ticks, int line) {
        return new Row(map, false, eut, ticks, SOURCE + line);
    }

    private Row row(String map, boolean optimize, long eut, long ticks, String source) {
        return new Row(map, optimize, eut, ticks, source);
    }

    private final class Row {
        private final String map;
        private final boolean optimize;
        private final long eut;
        private final long ticks;
        private final String source;
        private final List<ChemRecipe.ChemInput> inputs = new ArrayList<>();
        private final List<ChemRecipe.ChemFluidInput> fluidInputs = new ArrayList<>();
        private final List<ChemRecipe.ChemFluidOutput> fluidOutputs = new ArrayList<>();
        private final List<ChemRecipe.ChemOutput> outputs = new ArrayList<>();

        private Row(String map, boolean optimize, long eut, long ticks, String source) {
            this.map = map;
            this.optimize = optimize;
            this.eut = eut;
            this.ticks = ticks;
            this.source = source;
        }

        private Row item(ChemIngredient ingredient) {
            inputs.add(new ChemRecipe.ChemInput(ingredient, ChemRecipe.Role.CONSUMED));
            return this;
        }

        private Row kept(ChemIngredient ingredient) {
            inputs.add(new ChemRecipe.ChemInput(ingredient, ChemRecipe.Role.KEPT));
            return this;
        }

        private Row fluid(ChemFluid fluid) {
            fluidInputs.add(new ChemRecipe.ChemFluidInput(fluid, ChemRecipe.Role.CONSUMED));
            return this;
        }

        private Row keptFluid(ChemFluid fluid) {
            fluidInputs.add(new ChemRecipe.ChemFluidInput(fluid, ChemRecipe.Role.KEPT));
            return this;
        }

        private Row presence(ChemFluid fluid) {
            fluidInputs.add(new ChemRecipe.ChemFluidInput(fluid, ChemRecipe.Role.PRESENCE));
            return this;
        }

        private Row out(ChemIngredient ingredient) {
            outputs.add(new ChemRecipe.ChemOutput(ingredient, CERTAIN));
            return this;
        }

        private Row out(ChemIngredient ingredient, int chance) {
            outputs.add(new ChemRecipe.ChemOutput(ingredient, chance));
            return this;
        }

        private Row out(ChemFluid fluid) {
            fluidOutputs.add(new ChemRecipe.ChemFluidOutput(fluid));
            return this;
        }

        private void add() {
            rows.add(new ChemRecipe(map, optimize, eut, ticks, inputs, fluidInputs, fluidOutputs, outputs, source));
        }
    }

    private static MaterialAmount U(long whole) {
        return MaterialAmount.units(Math.multiplyExact(whole, GTValues.U));
    }

    private static MaterialAmount U(long numerator, long denominator) {
        return MaterialAmount.fraction(numerator, denominator);
    }

    /** Source {@code factor * (U / denominator)} with the original integer division of the constant. */
    private static MaterialAmount src(long factor, long denominator) {
        return MaterialAmount.units(Math.multiplyExact(factor, GTValues.U / denominator));
    }

    private static ChemIngredient dust(String material, MaterialAmount amount) {
        return new ChemIngredient.DustUnits(material, amount);
    }

    private static ChemIngredient ingot(String material, MaterialAmount amount) {
        return new ChemIngredient.IngotUnits(material, amount);
    }

    private static ChemIngredient ingotOrDust(String material, MaterialAmount amount) {
        return new ChemIngredient.IngotOrDustUnits(material, amount);
    }

    private static ChemIngredient gem(String material, MaterialAmount amount) {
        return new ChemIngredient.GemUnits(material, amount);
    }

    private static ChemIngredient form(String prefix, String material, int count) {
        return new ChemIngredient.ItemForm(prefix, material, count);
    }

    private static ChemIngredient block(String prefix, String material, int count) {
        return new ChemIngredient.BlockForm(prefix, material, count);
    }

    private static ChemIngredient vanilla(String id, int count) {
        return new ChemIngredient.RegistryItem(id.contains(":") ? id : "minecraft:" + id, count);
    }

    private static ChemIngredient selector(int index) {
        return new ChemIngredient.Selector(index);
    }

    private static ChemFluid liquid(String material, MaterialAmount amount) {
        return new ChemFluid.MaterialFluid(material, LIQUID, amount, FLOOR);
    }

    private static ChemFluid liquid(String material, MaterialAmount amount, gregtech.engine.recipe.chem.Rounding rounding) {
        return new ChemFluid.MaterialFluid(material, LIQUID, amount, rounding);
    }

    private static ChemFluid gas(String material, MaterialAmount amount) {
        return new ChemFluid.MaterialFluid(material, GAS, amount, FLOOR);
    }

    private static ChemFluid gas(String material, MaterialAmount amount, gregtech.engine.recipe.chem.Rounding rounding) {
        return new ChemFluid.MaterialFluid(material, GAS, amount, rounding);
    }

    private static ChemFluid nativeFluid(String key, int millibuckets) {
        return new ChemFluid.NativeFluid(key, millibuckets);
    }

    private static ChemFluid water(MaterialAmount amount, gregtech.engine.recipe.chem.Rounding rounding) {
        return new ChemFluid.Water(amount, rounding);
    }

    private static Set<GTMaterial> group(GTMaterial group) {
        Set<GTMaterial> out = new LinkedHashSet<>();
        if (group != null && group.isValid()) out.add(group.resolve());
        if (group != null) for (GTMaterial material : group.getReRegistrations()) {
            if (material != null && material.isValid()) out.add(material.resolve());
        }
        return out;
    }

    private static boolean fluidExists(String key) {
        return GTFluids.stack(key, 1) != null && !GTFluids.stack(key, 1).isEmpty();
    }

    // ── salts and acids: Loader_Recipes_Chem:99-181 ──────────────────────────

    private void saltAndAcidRows() {
        for (GTMaterial member : group(MaterialGroups.SiO2)) {
            String name = member.getName();
            row("Mixer", false, 16, 240, SOURCE + "100/" + name).item(dust(name, U(3)))
                    .fluid(gas("HF", U(12), CEIL)).out(water(U(6), FLOOR)).out(liquid("H2SiF6", U(9))).add();
        }
        row("HeatMixer", 16, 224, 101).item(dust("Al2O3", U(5))).fluid(liquid("H2SiF6", U(9), CEIL))
                .out(water(U(3), FLOOR)).out(liquid("AlF3", U(8))).out(dust("SiO2", U(3))).add();
        row("HeatMixer", 16, 224, 102).item(ingot("Al2O3", U(5))).fluid(liquid("H2SiF6", U(9), CEIL))
                .out(water(U(3), FLOOR)).out(liquid("AlF3", U(8))).out(dust("SiO2", U(3))).add();
        row("Mixer", 16, 752, 103).item(form("dust", "NaOH", 18)).item(dust("Al2O3", U(5)))
                .fluid(gas("HF", U(24), CEIL)).out(water(U(27), FLOOR)).out(liquid("Na3AlF6", U(20))).add();
        row("Mixer", 16, 752, 104).item(form("dust", "NaOH", 18)).item(ingot("Al2O3", U(5)))
                .fluid(gas("HF", U(24), CEIL)).out(water(U(27), FLOOR)).out(liquid("Na3AlF6", U(20))).add();
        row("Mixer", 16, 752, 105).item(block("blockDust", "NaOH", 2)).item(dust("Al2O3", U(5)))
                .fluid(gas("HF", U(24), CEIL)).out(water(U(27), FLOOR)).out(liquid("Na3AlF6", U(20))).add();
        row("Mixer", 16, 752, 106).item(block("blockDust", "NaOH", 2)).item(ingot("Al2O3", U(5)))
                .fluid(gas("HF", U(24), CEIL)).out(water(U(27), FLOOR)).out(liquid("Na3AlF6", U(20))).add();
        row("Mixer", 16, 560, 107).item(form("dust", "Ta2O5", 7)).item(dust("KF", U(8)))
                .fluid(gas("HF", U(20), CEIL)).out(water(U(15), FLOOR)).out(dust("K2TaF7", U(20))).add();
        row("Mixer", 16, 748, 108).item(form("dust", "Sugar", 45)).fluid(liquid("H2SO4", U(7, 4), CEIL))
                .out(water(U(135, 4), FLOOR)).out(gas("SO3", U(1))).out(dust("C", U(12))).add();
        row("Mixer", 16, 748, 109).item(block("blockDust", "Sugar", 5)).fluid(liquid("H2SO4", U(7, 4), CEIL))
                .out(water(U(135, 4), FLOOR)).out(gas("SO3", U(1))).out(dust("C", U(12))).add();
        row("Mixer", 16, 752, 110).item(dust("Borax", U(43))).fluid(liquid("HCl", U(4), CEIL))
                .out(water(U(3), FLOOR)).out(liquid("SaltWater", U(16))).out(dust("H3BO3", U(28))).add();
        row("Mixer", 16, 176, 111).item(dust("MnO2", U(1))).fluid(liquid("HCl", U(8), CEIL))
                .out(water(U(6), FLOOR)).out(gas("Cl", U(2))).out(dust("MnCl2", U(3))).add();
        carbonate(112, "CaCO3", U(5), "CaCl2", U(3), "HCl", GAS, U(4), 144);
        carbonate(113, "MgCO3", U(5), "MgCl2", U(3), "HCl", GAS, U(4), 144);
        carbonate(114, "CaCO3", U(5), "CaF2", U(3), "HF", GAS, U(4), 144);
        carbonate(116, "K2CO3", U(6), "KF", U(4), "HF", GAS, U(4), 160);
        carbonate(117, "Na2CO3", U(6), "NaF", U(4), "HF", GAS, U(4), 160);
        carbonate(118, "K2CO3", U(6), "KNO3", U(10), "HNO3", LIQUID, U(10), 256);
        carbonate(119, "Na2CO3", U(6), "NaNO3", U(10), "HNO3", LIQUID, U(10), 256);
        row("Mixer", 16, 128, 120).item(dust("NaHCO3", U(6))).fluid(liquid("HCl", U(2), CEIL))
                .out(liquid("SaltWater", U(4))).out(gas("CO2", U(3))).out(dust("NaCl", U(1))).add();
        row("HeatMixer", 16, 354, 121).item(dust("LiOH", U(18))).fluid(gas("Cl", U(6), CEIL))
                .out(water(U(9), FLOOR)).out(liquid("LiClO3", U(5))).out(dust("LiCl", U(10))).add();
        for (GTMaterial member : group(MaterialGroups.CaF2)) {
            String name = member.getName();
            row("Mixer", false, 16, 160, SOURCE + "123/" + name).item(dust(name, U(3)))
                    .fluid(liquid("H2SO4", U(7), CEIL)).out(gas("HF", U(4))).out(dust("CaSO4", U(6))).add();
        }
        row("HeatMixer", 16, 64, 124).item(dust("Al", U(1))).fluid(gas("F", U(3), CEIL))
                .out(liquid("AlF3", U(4))).add();
        row("Mixer", 16, 48, 125).item(dust("Ca", U(1))).fluid(gas("F", U(2), CEIL))
                .out(dust("CaF2", U(3))).add();
        row("HeatMixer", 16, 48, 126).item(dust("S", U(1))).fluid(gas("H", U(2), CEIL))
                .out(gas("H2S", U(3))).add();
        row("HeatMixer", 16, 48, 127).item(dust("Blaze", U(1, 9))).fluid(gas("H", U(2), CEIL))
                .out(gas("H2S", U(3))).add();
        row("HeatMixer", 16, 160, 128).item(dust("WO3", U(4))).fluid(gas("H", U(6), CEIL))
                .out(water(U(9), FLOOR)).out(dust("W", U(1))).add();
        row("Mixer", 16, 80, 130).item(dust("Ca", U(1))).fluid(liquid("HCl", U(4), CEIL))
                .out(gas("H", U(2))).out(dust("CaCl2", U(3))).add();
        row("Mixer", 16, 80, 131).item(dust("Mg", U(1))).fluid(liquid("HCl", U(4), CEIL))
                .out(gas("H", U(2))).out(dust("MgCl2", U(3))).add();
        row("Mixer", 16, 80, 132).item(dust("NaOH", U(3))).fluid(liquid("HCl", U(2), CEIL))
                .out(liquid("SaltWater", U(4))).out(dust("NaCl", U(1))).add();
        row("Mixer", 16, 80, 133).item(dust("KOH", U(3))).fluid(liquid("HCl", U(2), CEIL))
                .out(liquid("SaltedWater", U(4))).out(dust("KCl", U(1))).add();
        row("Mixer", 16, 144, 134).item(dust("NaOH", U(6))).fluid(gas("SO2", U(3), CEIL))
                .out(water(U(3), FLOOR)).out(dust("Na2SO3", U(6))).add();
        row("Mixer", 16, 128, 135).item(dust("KOH", U(3))).fluid(liquid("HNO3", U(5), CEIL))
                .out(water(U(3), FLOOR)).out(dust("KNO3", U(5))).add();
        row("Mixer", 16, 128, 136).item(dust("NaOH", U(3))).fluid(liquid("HNO3", U(5), CEIL))
                .out(water(U(3), FLOOR)).out(dust("NaNO3", U(5))).add();
        row("Mixer", 16, 144, 137).item(dust("KOH", U(6))).fluid(gas("CO2", U(3), CEIL))
                .out(water(U(3), FLOOR)).out(dust("K2CO3", U(6))).add();
        row("Mixer", 16, 144, 138).item(dust("NaOH", U(6))).fluid(gas("CO2", U(3), CEIL))
                .out(water(U(3), FLOOR)).out(dust("Na2CO3", U(6))).add();
        row("Mixer", 16, 32, 139).item(dust("Trona", U(1))).fluid(gas("CO2", U(1, 2), CEIL))
                .out(water(U(1, 2), FLOOR)).out(dust("NaHCO3", U(2))).add();
        row("Mixer", 16, 144, 140).item(dust("KOH", U(6))).fluid(gas("H2S", U(3), CEIL))
                .out(water(U(6), FLOOR)).out(dust("K2S", U(3))).add();
        row("Mixer", 16, 144, 141).item(dust("NaOH", U(6))).fluid(gas("H2S", U(3), CEIL))
                .out(water(U(6), FLOOR)).out(dust("Na2S", U(3))).add();
        row("Mixer", 16, 192, 142).item(dust("KNO3", U(5))).fluid(liquid("H2SO4", U(7), CEIL))
                .out(liquid("HNO3", U(5))).out(dust("KHSO4", U(7))).add();
        row("Mixer", 16, 192, 143).item(dust("NaNO3", U(5))).fluid(liquid("H2SO4", U(7), CEIL))
                .out(liquid("HNO3", U(5))).out(dust("NaHSO4", U(7))).add();
        row("Mixer", 16, 144, 144).item(dust("KCl", U(2))).fluid(liquid("H2SO4", U(7), CEIL))
                .out(gas("HCl", U(2))).out(dust("KHSO4", U(7))).add();
        row("Mixer", 16, 144, 145).item(dust("NaCl", U(2))).fluid(liquid("H2SO4", U(7), CEIL))
                .out(gas("HCl", U(2))).out(dust("NaHSO4", U(7))).add();
        row("Mixer", 16, 32, 146).item(dust("K2SO4", U(1))).fluid(liquid("H2SO4", U(1), CEIL))
                .out(dust("KHSO4", U(2))).add();
        row("Mixer", 16, 32, 147).item(dust("Na2SO4", U(1))).fluid(liquid("H2SO4", U(1), CEIL))
                .out(dust("NaHSO4", U(2))).add();
        row("Mixer", 16, 304, 148).item(dust("K2S", U(3))).fluid(liquid("H2O2", U(16), CEIL))
                .out(water(U(12), FLOOR)).out(dust("K2SO4", U(7))).add();
        row("Mixer", 16, 304, 149).item(dust("Na2S", U(3))).fluid(liquid("H2O2", U(16), CEIL))
                .out(water(U(12), FLOOR)).out(dust("Na2SO4", U(7))).add();
        row("Mixer", 16, 272, 150).fluid(liquid("Fe2O3", U(5), CEIL)).fluid(liquid("HCl", U(12), CEIL))
                .out(water(U(9), FLOOR)).out(dust("FeCl3", U(8))).add();
        row("Mixer", 16, 800, 151).fluid(liquid("Glycerol", U(14), CEIL)).fluid(liquid("HNO3", U(15), CEIL))
                .fluid(liquid("H2SO4", U(21), CEIL)).out(liquid("Glyceryl", U(20))).out(gas("SO2", U(6))).add();
        if (fluidExists("Reikygen")) {
            row("Mixer", 16, 48, 153).fluid(gas("NO", U(2), CEIL)).fluid(nativeFluid("Reikygen", 1000))
                    .out(gas("NO2", U(3))).add();
        }
        row("Mixer", 16, 48, 154).fluid(gas("NO", U(2), CEIL)).fluid(nativeFluid("Oxygen", 1000))
                .out(gas("NO2", U(3))).add();
        row("Mixer", 16, 32, 155).fluid(gas("H", U(1), CEIL)).fluid(gas("F", U(1), CEIL))
                .out(gas("HF", U(2))).add();
        row("Mixer", 16, 24, 156).fluid(gas("H", U(1), CEIL)).fluid(liquid("S", U(1, 2), CEIL))
                .out(gas("H2S", U(3, 2))).add();
        row("Mixer", 16, 160, 157).fluid(gas("NO2", U(6), CEIL)).fluid(liquid("H2O2", U(4), CEIL))
                .out(liquid("HNO3", U(10))).add();
        row("Mixer", 16, 176, 158).fluid(gas("SO3", U(4), CEIL)).fluid(liquid("H2SO4", U(7), CEIL))
                .out(liquid("H2S2O7", U(11))).add();
        row("HeatMixer", 16, 48, 159).fluid(gas("H2S", U(2), CEIL)).fluid(gas("SO2", U(1), CEIL))
                .out(water(U(2), FLOOR)).out(dust("S", U(1))).add();
        row("Mixer", 16, 48, 160).item(form("dust", "Na", 2)).item(form("dust", "S", 1))
                .out(form("dust", "Na2S", 3)).add();
        row("Mixer", 16, 48, 161).item(form("dust", "Na", 2)).item(form("dustTiny", "Blaze", 1))
                .out(form("dust", "Na2S", 3)).add();
        row("Mixer", 16, 48, 162).item(form("dust", "K", 2)).item(form("dust", "S", 1))
                .out(form("dust", "K2S", 3)).add();
        row("Mixer", 16, 48, 163).item(form("dust", "K", 2)).item(form("dustTiny", "Blaze", 1))
                .out(form("dust", "K2S", 3)).add();
        row("Mixer", 16, 48, 164).item(form("dust", "NaOH", 1)).item(form("dust", "NaHCO3", 2))
                .out(water(U(1), FLOOR)).out(form("dust", "Na2CO3", 2)).add();
        for (GTMaterial member : group(MaterialGroups.SiO2)) {
            String name = member.getName();
            row("HeatMixer", false, 16, 144, SOURCE + "166/" + name).item(dust(name, U(3)))
                    .item(form("dust", "CaSO4", 6)).out(gas("SO3", U(4))).out(dust("Wollastonite", U(5))).add();
        }
        for (GTMaterial member : group(MaterialGroups.Fe)) {
            String name = member.getName();
            row("Mixer", false, 16, 64, SOURCE + "169/" + name).item(dust(name, U(1)))
                    .fluid(gas("Cl", U(3), CEIL)).out(dust("FeCl3", U(4))).add();
            row("Mixer", false, 16, 80, SOURCE + "170/" + name).item(dust(name, U(1)))
                    .fluid(liquid("HCl", U(4), CEIL)).out(gas("H", U(2))).out(dust("FeCl2", U(3))).add();
            row("Mixer", false, 16, 144, SOURCE + "171/" + name).item(dust(name, U(1)))
                    .item(dust("FeCl3", U(8))).out(dust("FeCl2", U(9))).add();
        }
        row("HeatMixer", 16, 186, 174).item(dust("Uraninite", U(1))).fluid(gas("HF", U(8), CEIL))
                .out(water(U(6), FLOOR)).out(dust("UF4", U(5))).add();
        row("Mixer", 16, 112, 175).item(dust("UF4", U(5))).fluid(gas("F", U(2), CEIL))
                .out(gas("UF6", U(7), CEIL)).add();
        row("Mixer", 16, 144, 176).fluid(gas("H", U(2), CEIL)).fluid(gas("UF6", U(7), CEIL))
                .out(gas("HF", U(4))).out(dust("UF4", U(5))).add();
        row("Mixer", 16, 144, 177).fluid(gas("H", U(2), CEIL)).fluid(gas("U238F6", U(7), CEIL))
                .out(gas("HF", U(4))).out(dust("U238F4", U(5))).add();
        row("Mixer", 16, 144, 178).fluid(gas("H", U(2), CEIL)).fluid(gas("U235F6", U(7), CEIL))
                .out(gas("HF", U(4))).out(dust("U235F4", U(5))).add();
        row("Mixer", 16, 112, 179).fluid(liquid("Ca", U(2), CEIL)).fluid(liquid("UF4", U(5), CEIL))
                .out(liquid("CaF2", U(6))).out(liquid("U_238", U(1))).add();
        row("Mixer", 16, 112, 180).fluid(liquid("Ca", U(2), CEIL)).fluid(liquid("U238F4", U(5), CEIL))
                .out(liquid("CaF2", U(6))).out(liquid("U_238", U(1))).add();
        row("Mixer", 16, 112, 181).fluid(liquid("Ca", U(2), CEIL)).fluid(liquid("U235F4", U(5), CEIL))
                .out(liquid("CaF2", U(6))).out(liquid("U_235", U(1))).add();
        row("Centrifuge", true, 512, 120, SOURCE + 182)
                .fluid(gas("UF6", U(7), CEIL)).out(gas("U238F6", U(5))).out(gas("U235F6", U(2))).add();
    }

    private void carbonate(int line, String input, MaterialAmount inputAmount, String product,
                           MaterialAmount productAmount, String acid, gregtech.engine.recipe.chem.FluidPhase phase,
                           MaterialAmount acidAmount, long ticks) {
        ChemFluid actualAcid = phase == GAS ? gas(acid, acidAmount, CEIL) : liquid(acid, acidAmount, CEIL);
        row("Mixer", 16, ticks, line).item(dust(input, inputAmount)).fluid(actualAcid)
                .out(water(U(3), FLOOR)).out(gas("CO2", U(3))).out(dust(product, productAmount)).add();
    }
    private void metalRows() {
        row("Bath", 0, 512, 187).item(dust("Fe2O3", U(5))).fluid(liquid("Al", U(2), CEIL))
                .out(liquid("Fe", U(2))).out(dust("Al2O3", U(5))).add();
        row("Bath", 0, 512, 188).item(dust("Nb2O5", U(7))).fluid(liquid("Al", src(10, 3), CEIL))
                .out(liquid("Al2O3", src(25, 3))).out(dust("Nb", U(2))).add();
        row("Bath", 0, 512, 189).item(dust("K2TaF7", U(10))).fluid(liquid("Na", U(5), CEIL))
                .out(gem("Ta", U(1))).out(dust("NaF", U(10))).out(dust("KF", U(4))).add();
        row("Bath", 0, 512, 190).item(dust("NaCl", U(4))).fluid(liquid("CaCO3", U(5), CEIL))
                .out(dust("CaCl2", U(3))).out(dust("Na2CO3", U(3))).out(dust("Na2CO3", U(3))).add();
        row("Bath", 0, 512, 191).item(dust("NaCl", U(4))).fluid(liquid("MgCO3", U(5), CEIL))
                .out(dust("MgCl2", U(3))).out(dust("Na2CO3", U(3))).out(dust("Na2CO3", U(3))).add();
        row("Bath", 0, 512, 192).item(dust("Scheelite", U(6))).fluid(liquid("HCl", U(4), CEIL))
                .out(dust("CaCl2", U(3))).out(dust("H2WO4", U(4))).out(dust("H2WO4", U(3))).add();
        row("Bath", 0, 512, 193).item(dust("Wolframite", U(6))).fluid(liquid("HCl", U(4), CEIL))
                .out(dust("MgCl2", U(3))).out(dust("H2WO4", U(4))).out(dust("H2WO4", U(3))).add();
        row("Bath", 0, 512, 194).item(dust("Ferberite", U(6))).fluid(liquid("HCl", U(4), CEIL))
                .out(dust("FeCl2", U(3))).out(dust("H2WO4", U(4))).out(dust("H2WO4", U(3))).add();
        row("Bath", 0, 512, 195).item(dust("Huebnerite", U(6))).fluid(liquid("HCl", U(4), CEIL))
                .out(dust("MnCl2", U(3))).out(dust("H2WO4", U(4))).out(dust("H2WO4", U(3))).add();
        row("Bath", 0, 512, 196).item(dust("Tungstate", U(7))).fluid(liquid("HCl", U(4), CEIL))
                .out(dust("LiCl", U(4))).out(dust("H2WO4", U(7))).add();
        row("Bath", 0, 512, 197).item(vanilla("sand", 1)).fluid(liquid("HCl", U(12), CEIL))
                .out(water(U(9), FLOOR)).out(dust("FeCl3", U(2))).out(dust("FeCl3", U(2)))
                .out(dust("FeCl3", U(2))).out(dust("FeCl3", U(2))).add();
        row("Bath", 0, 512, 198).item(dust("RedSand", U(9))).fluid(liquid("HCl", U(12), CEIL))
                .out(water(U(9), FLOOR)).out(dust("FeCl3", U(8))).add();
        row("Bath", 0, 512, 199).item(dust("Fe2O3", U(5))).fluid(liquid("HCl", U(12), CEIL))
                .out(water(U(9), FLOOR)).out(dust("FeCl3", U(4))).out(dust("FeCl3", U(4))).add();
        row("Bath", 0, 512, 200).item(dust("YellowLimonite", U(4))).fluid(liquid("HCl", U(6), CEIL))
                .out(water(U(6), FLOOR)).out(dust("FeCl3", U(4))).add();
        row("Bath", 0, 512, 201).item(dust("BrownLimonite", U(4))).fluid(liquid("HCl", U(6), CEIL))
                .out(water(U(6), FLOOR)).out(dust("FeCl3", U(4))).add();
        row("Bath", 0, 512, 203).item(dust("FeCl3", U(4))).fluid(nativeFluid("Steam", 1_440_000))
                .out(liquid("HCl", U(6), CEIL)).out(dust("FeO3H3", U(7))).add();

        if (fluidExists("Br")) {
            row("Freezer", 16, 512, 207).kept(selector(0)).fluid(liquid("Br", U(1), CEIL))
                    .out(ingotOrDust("Br", U(1))).add();
        }
        if (fluidExists("Hg")) {
            row("Freezer", 16, 512, 208).kept(selector(0)).fluid(liquid("Hg", U(1), CEIL))
                    .out(ingotOrDust("Hg", U(1))).add();
        }

        for (String oxygen : oxygenFluids()) {
            if (fluidExists("Liquid_Reikygen")) {
                row("Freezer", false, 16, 256, SOURCE + "212/" + oxygen)
                        .kept(selector(1)).fluid(nativeFluid(oxygen, 1000))
                        .out(nativeFluid("Liquid_Reikygen", 1000)).add();
            }
            row("Freezer", false, 16, 256, SOURCE + "213/" + oxygen)
                    .kept(selector(0)).fluid(nativeFluid(oxygen, 1000))
                    .out(nativeFluid("Liquid_Oxygen", 1000)).add();
            row("Lightning", false, 256, 16, SOURCE + "216/" + oxygen)
                    .kept(selector(2)).fluid(gas("N", U(1, 100), CEIL)).fluid(nativeFluid(oxygen, 10))
                    .out(gas("NO", U(1, 50))).add();
            row("Lightning", false, 256, 512, SOURCE + "217/" + oxygen)
                    .kept(selector(2)).item(dust("K2S", U(3))).fluid(nativeFluid(oxygen, 4000))
                    .out(dust("K2SO4", U(7))).add();
            row("Lightning", false, 256, 512, SOURCE + "218/" + oxygen)
                    .kept(selector(2)).item(dust("Na2S", U(3))).fluid(nativeFluid(oxygen, 4000))
                    .out(dust("Na2SO4", U(7))).add();
            row("BurnMixer", false, 16, 24, SOURCE + "220/" + oxygen)
                    .fluid(gas("H", U(1), CEIL)).fluid(nativeFluid(oxygen, 500))
                    .out(nativeFluid("DistW", 1500)).add();
            row("BurnMixer", false, 16, 24, SOURCE + "221/" + oxygen)
                    .fluid(gas("T", U(1), CEIL)).fluid(nativeFluid(oxygen, 500))
                    .out(liquid("T2O", U(3, 2))).add();
            row("Mixer", false, 16, 16, SOURCE + "223/" + oxygen)
                    .kept(form("dust", "Pt", 1)).fluid(gas("SO2", U(3, 4), CEIL))
                    .fluid(nativeFluid(oxygen, 250)).out(gas("SO3", U(1))).add();
            row("Mixer", false, 16, 16, SOURCE + "224/" + oxygen)
                    .kept(form("dust", "V2O5", 1)).fluid(gas("SO2", U(3, 4), CEIL))
                    .fluid(nativeFluid(oxygen, 250)).out(gas("SO3", U(1))).add();
            row("Mixer", false, 16, 176, SOURCE + "225/" + oxygen)
                    .item(dust("NaCl", U(3))).fluid(gas("SO2", U(3), CEIL)).fluid(nativeFluid(oxygen, 1000))
                    .fluid(liquid("SaltWater", U(4), CEIL)).out(gas("HCl", U(4))).out(dust("Na2SO4", U(7))).add();
            row("Mixer", false, 16, 176, SOURCE + "226/" + oxygen)
                    .item(dust("KCl", U(3))).fluid(gas("SO2", U(3), CEIL)).fluid(nativeFluid(oxygen, 1000))
                    .fluid(liquid("SaltedWater", U(4), CEIL)).out(gas("HCl", U(4))).out(dust("K2SO4", U(7))).add();
        }

        for (String air : new String[] {"Air", "Air_End", "Air_Nether"}) {
            row("Mixer", false, 16, 16, SOURCE + "229/" + air)
                    .kept(form("dust", "Pt", 1)).fluid(gas("SO2", U(3, 4), CEIL))
                    .fluid(nativeFluid(air, 1000)).out(gas("SO3", U(1))).add();
            row("Mixer", false, 16, 16, SOURCE + "230/" + air)
                    .kept(form("dust", "V2O5", 1)).fluid(gas("SO2", U(3, 4), CEIL))
                    .fluid(nativeFluid(air, 1000)).out(gas("SO3", U(1))).add();
            row("Mixer", false, 16, 176, SOURCE + "231/" + air)
                    .item(dust("NaCl", U(3))).fluid(gas("SO2", U(3), CEIL)).fluid(nativeFluid(air, 4000))
                    .fluid(liquid("SaltWater", U(4), CEIL)).out(gas("HCl", U(4))).out(dust("Na2SO4", U(7))).add();
            row("Mixer", false, 16, 176, SOURCE + "232/" + air)
                    .item(dust("KCl", U(3))).fluid(gas("SO2", U(3), CEIL)).fluid(nativeFluid(air, 4000))
                    .fluid(liquid("SaltedWater", U(4), CEIL)).out(gas("HCl", U(4))).out(dust("K2SO4", U(7))).add();
        }

        for (GTMaterial member : group(MaterialGroups.Coal)) {
            if (member.getName().equals("Graphene")) continue;
            String name = member.getName();
            row("BurnMixer", false, 16, 144, SOURCE + "237/" + name)
                    .item(dust("Na2SO4", U(7))).item(dust(name, U(2)))
                    .out(gas("CO2", U(6))).out(dust("Na2S", U(3))).add();
            row("BurnMixer", false, 16, 144, SOURCE + "238/" + name)
                    .item(dust("K2SO4", U(7))).item(dust(name, U(2)))
                    .out(gas("CO2", U(6))).out(dust("K2S", U(3))).add();
            row("BurnMixer", false, 16, 128, SOURCE + "239/" + name)
                    .item(dust(name, U(2))).fluid(liquid("Na2CO3", U(6), CEIL))
                    .out(gas("CO", U(6))).out(dust("Na", U(2))).add();
            row("Bath", false, 0, 512, SOURCE + "240/" + name)
                    .item(dust(name, U(3))).fluid(liquid("HNO3", U(20), CEIL))
                    .out(gas("CO2", U(9))).out(gas("NO", U(8), CEIL)).out(water(U(6), FLOOR)).add();
        }

        tiCl4(244, "TiO2", U(1), "C", U(1), U(4), U(1), U(5), "CO2", U(3), null, null);
        tiCl4(245, "TiO2", U(2), "C", U(2), U(8), U(2), U(10), "CO2", U(6), null, null);
        tiCl4(246, "Ilmenite", U(5), "C", U(3), U(7), U(1), U(5), "CO", U(6), "FeCl3", U(4));
        tiCl4(247, "Ilmenite", U(10), "C", U(6), U(14), U(2), U(10), "CO", U(12), "FeCl3", U(8));
        tiCl4(248, "TiO2", U(1), "Graphite", U(1), U(4), U(1), U(5), "CO2", U(3), null, null);
        tiCl4(249, "TiO2", U(2), "Graphite", U(2), U(8), U(2), U(10), "CO2", U(6), null, null);
        tiCl4(250, "Ilmenite", U(5), "Graphite", U(3), U(7), U(1), U(5), "CO", U(6), "FeCl3", U(4));
        tiCl4(251, "Ilmenite", U(10), "Graphite", U(6), U(14), U(2), U(10), "CO", U(12), "FeCl3", U(8));
        tiCl4(252, "TiO2", U(1), "CoalCoke", U(1, 2), U(4), U(1), U(5), "CO2", U(3), null, null);
        tiCl4(253, "TiO2", U(2), "CoalCoke", U(1), U(8), U(2), U(10), "CO2", U(6), null, null);
        tiCl4(254, "Ilmenite", U(5), "CoalCoke", U(3, 2), U(7), U(1), U(5), "CO", U(6), "FeCl3", U(4));
        tiCl4(255, "Ilmenite", U(10), "CoalCoke", U(3), U(14), U(2), U(10), "CO", U(12), "FeCl3", U(8));
        tiCl4(256, "TiO2", U(1), "PetCoke", U(1, 2), U(4), U(1), U(5), "CO2", U(3), null, null);
        tiCl4(257, "TiO2", U(2), "PetCoke", U(1), U(8), U(2), U(10), "CO2", U(6), null, null);
        tiCl4(258, "Ilmenite", U(5), "PetCoke", U(3, 2), U(7), U(1), U(5), "CO", U(6), "FeCl3", U(4));
        tiCl4(259, "Ilmenite", U(10), "PetCoke", U(3), U(14), U(2), U(10), "CO", U(12), "FeCl3", U(8));

        row("Bath", 0, 512, 260).item(dust("Ilmenite", U(5))).fluid(liquid("H2SO4", U(7), CEIL))
                .out(liquid("GreenVitriol", U(6))).out(nativeFluid("Water", 3000)).out(dust("TiO2", U(1))).add();
        row("Bath", 0, 512, 261).item(dust("Na", U(4))).fluid(liquid("TiCl4", U(5), CEIL))
                .out(dust("Ti", U(1))).out(dust("NaCl", U(2))).out(dust("NaCl", U(2)))
                .out(dust("NaCl", U(2))).out(dust("NaCl", U(2))).add();
        row("Bath", 0, 512, 262).item(dust("Mg", U(2))).fluid(liquid("TiCl4", U(5), CEIL))
                .out(dust("Ti", U(1))).out(dust("MgCl2", U(2))).out(dust("MgCl2", U(2)))
                .out(dust("MgCl2", U(2))).add();
        row("Bath", 0, 512, 263).item(dust("Eudialyte", U(16))).fluid(liquid("H2SO4", U(7, 2), CEIL))
                .out(liquid("GrayVitriol", U(3))).out(nativeFluid("Saltwater", 2000)).out(gas("O", U(1)))
                .out(dust("Zircon", U(9, 4))).out(dust("SiO2", U(9))).out(dust("Na", U(3, 2)))
                .out(dust("Ca", U(3, 4))).add();
        row("Mixer", 16, 16, 266).fluid(liquid("Fuel", U(2, 5), CEIL)).fluid(liquid("Glyceryl", U(10), CEIL))
                .out(liquid("NitroFuel", U(2))).add();
        row("Mixer", 16, 64, 267).fluid(liquid("HNO3", U(5), CEIL)).fluid(liquid("HCl", U(8), CEIL))
                .out(liquid("AquaRegia", U(13))).add();
        row("Mixer", 16, 16, 268).fluid(gas("He", U(1, 200), CEIL)).fluid(gas("Ne", U(1, 200), CEIL))
                .out(gas("HeNe", U(1, 100))).add();
        row("Lightning", true, 2048, 128, SOURCE + 271).kept(selector(1))
                .item(dust("Adamantine", U(7))).out(gas("O", U(4))).out(dust("Ad", U(3))).add();
    }

    private void tiCl4(int line, String titanium, MaterialAmount titaniumAmount, String carbon,
                       MaterialAmount carbonAmount, MaterialAmount chlorine, MaterialAmount calciumCarbonate,
                       MaterialAmount product, String gas, MaterialAmount gasAmount, String byproduct,
                       MaterialAmount byproductAmount) {
        Row row = row("BurnMixer", 16, 256, line)
                .item(dust(titanium, titaniumAmount)).item(dust(carbon, carbonAmount))
                .fluid(gas("Cl", chlorine, CEIL)).fluid(liquid("CaCO3", calciumCarbonate, CEIL))
                .out(liquid("TiCl4", product)).out(gas(gas, gasAmount));
        if (byproduct != null) row.out(dust(byproduct, byproductAmount));
        row.add();
    }

    private static List<String> oxygenFluids() {
        List<String> fluids = new ArrayList<>();
        fluids.add("Oxygen");
        if (fluidExists("Reikygen")) fluids.add("Reikygen");
        return fluids;
    }
    private void autoclaveAndAluminiumElectrolysisRows() {
        autoclave(274, "dust", "Bauxite", 1, "dustSmall", "KOH", 6, 48000, 1050, "KAlO2", 2, 1500);
        autoclave(275, "dustSmall", "Bauxite", 4, "dustSmall", "KOH", 6, 48000, 1050, "KAlO2", 2, 1500);
        autoclave(276, "dustTiny", "Bauxite", 9, "dustSmall", "KOH", 6, 48000, 1050, "KAlO2", 2, 1500);
        autoclave(277, "dust", "Bauxite", 2, "dust", "KOH", 3, 96000, 2100, "KAlO2", 4, 3000);
        autoclave(278, "dustSmall", "Bauxite", 8, "dust", "KOH", 3, 96000, 2100, "KAlO2", 4, 3000);
        autoclave(279, "dustTiny", "Bauxite", 18, "dust", "KOH", 3, 96000, 2100, "KAlO2", 4, 3000);
        autoclave(280, "dust", "Bauxite", 1, "dustSmall", "NaOH", 6, 48000, 1050, "NaAlO2", 2, 1500);
        autoclave(281, "dustSmall", "Bauxite", 4, "dustSmall", "NaOH", 6, 48000, 1050, "NaAlO2", 2, 1500);
        autoclave(282, "dustTiny", "Bauxite", 9, "dustSmall", "NaOH", 6, 48000, 1050, "NaAlO2", 2, 1500);
        autoclave(283, "dust", "Bauxite", 2, "dust", "NaOH", 3, 96000, 2100, "NaAlO2", 4, 3000);
        autoclave(284, "dustSmall", "Bauxite", 8, "dust", "NaOH", 3, 96000, 2100, "NaAlO2", 4, 3000);
        autoclave(285, "dustTiny", "Bauxite", 18, "dust", "NaOH", 3, 96000, 2100, "NaAlO2", 4, 3000);

        for (GTMaterial member : group(MaterialGroups.Coal)) {
            if (member.getName().equals("Graphene")) continue;
            String coal = member.getName();
            aluminiumElectrolysis(291, coal, "blockDust", 1, "dust", 30, U(1, 24), U(1, 12), U(27), src(7, 80), U(12), 6144);
            aluminiumElectrolysis(292, coal, "blockDust", 1, "ingot", 30, U(1, 24), U(1, 12), U(27), src(7, 80), U(12), 6144);
            aluminiumElectrolysis(293, coal, "dust", 3, "dust", 10, U(1, 72), U(1, 36), U(9), src(7, 240), U(4), 2048);
            aluminiumElectrolysis(294, coal, "dust", 3, "ingot", 10, U(1, 72), U(1, 36), U(9), src(7, 240), U(4), 2048);
            aluminiumElectrolysis(295, coal, "dustTiny", 27, "dust", 10, U(1, 72), U(1, 36), U(9), src(7, 240), U(4), 2048);
            aluminiumElectrolysis(296, coal, "dustTiny", 27, "ingot", 10, U(1, 72), U(1, 36), U(9), src(7, 240), U(4), 2048);
            aluminiumElectrolysis(297, coal, "dustSmall", 6, "dust", 5, U(1, 144), U(1, 72), U(9, 2), src(7, 480), U(2), 1024);
            aluminiumElectrolysis(298, coal, "dustSmall", 6, "ingot", 5, U(1, 144), U(1, 72), U(9, 2), src(7, 480), U(2), 1024);
        }

        row("Electrolyzer", 16, 512, 300).kept(selector(0)).item(dust("NaHSO4", U(7)))
                .out(gas("H", U(1), CEIL)).out(dust("NaSO4", U(6))).add();
        row("Electrolyzer", 16, 512, 301).kept(selector(0)).item(dust("KHSO4", U(7)))
                .out(gas("H", U(1), CEIL)).out(dust("KSO4", U(6))).add();
        chlorideElectrolysis(302, "CaCl2", "Ca", 64, 1024, false);
        chlorideElectrolysis(303, "MgCl2", "Mg", 64, 1024, false);
        chlorideElectrolysis(304, "MnCl2", "Mn", 64, 2048, false);
        chlorideElectrolysis(305, "CaCl2", "Ca", 16, 2048, true);
        chlorideElectrolysis(306, "MgCl2", "Mg", 16, 2048, true);
        chlorideElectrolysis(307, "MnCl2", "Mn", 16, 4096, true);
        row("Electrolyzer", 16, 256, 309).kept(selector(1)).fluid(liquid("LiClO3", U(5), CEIL))
                .fluid(gas("O", U(1), CEIL)).out(dust("LiClO4", U(6))).add();
    }

    private void autoclave(int line, String bauxitePrefix, String bauxite, int bauxiteCount,
                           String alkaliPrefix, String alkali, int alkaliCount, int steam, int distilled,
                           String aluminate, int aluminateCount, long ticks) {
        Row row = row("Autoclave", 0, ticks, line)
                .item(form(bauxitePrefix, bauxite, bauxiteCount)).item(form(alkaliPrefix, alkali, alkaliCount))
                .fluid(nativeFluid("Steam", steam)).out(nativeFluid("DistW", distilled));
        row.out(form("dust", aluminate, aluminateCount), 10000);
        row.out(form("crushedCentrifugedTiny", "Ilmenite", aluminateCount), 5000);
        row.out(form("crushedCentrifugedTiny", "TiO2", aluminateCount / 2), 5000);
        row.add();
    }

    private void aluminiumElectrolysis(int line, String coal, String coalPrefix, int coalCount,
                                       String aluminaPrefix, int aluminaCount, MaterialAmount cryolite,
                                       MaterialAmount aluminiumFluoride, MaterialAmount carbonDioxide,
                                       MaterialAmount fluorine, MaterialAmount aluminium, long ticks) {
        ChemIngredient coalIngredient = coalPrefix.startsWith("block")
                ? block(coalPrefix, coal, coalCount) : form(coalPrefix, coal, coalCount);
        row("Electrolyzer", 16, ticks, line).item(coalIngredient)
                .item(form(aluminaPrefix, "Al2O3", aluminaCount))
                .fluid(liquid("Na3AlF6", cryolite, CEIL)).fluid(liquid("AlF3", aluminiumFluoride, CEIL))
                .out(gas("CO2", carbonDioxide)).out(gas("F", fluorine)).out(dust("Al", aluminium)).add();
    }

    private void chlorideElectrolysis(int line, String chloride, String metal, long eut, long ticks, boolean fluid) {
        Row row = row("Electrolyzer", false, eut, ticks, SOURCE + line).kept(selector(0));
        if (fluid) row.fluid(liquid(chloride, U(3), CEIL));
        else row.item(dust(chloride, U(3)));
        row.out(gas("Cl", U(2), CEIL)).out(dust(metal, U(1))).add();
    }
    private void waterElectrolysisRows() {
        for (String water : new String[] {"Water", "MnWtr", "DistW"}) {
            row("Electrolyzer", false, 16, 3840, SOURCE + "312/" + water).kept(selector(0))
                    .fluid(nativeFluid(water, 3000)).out(gas("H", U(2))).out(gas("O", U(1))).add();
        }
        optionalElectrolysis(313, "Water_Geothermal", 3000, 3840, "NaOH");
        optionalElectrolysis(314, "Water_Boiling", 3000, 3840, "NaOH");
        optionalElectrolysis(315, "Water_Hot", 3000, 3840, "NaOH");
        optionalElectrolysis(316, "Hot_Water", 3000, 3840, "NaOH");
        optionalElectrolysis(317, "Cold_Water", 3000, 3840, "NaOH");
        row("Electrolyzer", 16, 3840, 318).kept(selector(0)).fluid(liquid("D2O", U(3), CEIL))
                .out(gas("D", U(2))).out(gas("O", U(1))).add();
        row("Electrolyzer", 16, 3840, 319).kept(selector(0)).fluid(liquid("T2O", U(3), CEIL))
                .out(gas("T", U(2))).out(gas("O", U(1))).add();
        saltElectrolysis(320, "Tropics_Water", 7000, 8960, "NaOH", U(35, 8), U(17, 8));
        saltElectrolysis(321, "OceanGrC", 7000, 8960, "NaOH", U(35, 8), U(17, 8));
        saltElectrolysis(322, "Ocean", 7000, 8960, "NaOH", U(35, 8), U(17, 8));
        saltElectrolysis(323, "Brine", 1000, 1280, "NaOH", U(3, 8), U(1, 8));
        row("Electrolyzer", 16, 1280, 324).kept(selector(0)).fluid(liquid("SaltWater", U(1), CEIL))
                .out(gas("Cl", U(1, 8))).out(gas("H", U(3, 8))).out(gas("O", U(1, 8)))
                .out(dust("NaOH", U(3, 8))).add();
        row("Electrolyzer", 16, 1280, 325).kept(selector(0)).fluid(liquid("SaltedWater", U(1), CEIL))
                .out(gas("Cl", U(1, 8))).out(gas("H", U(3, 8))).out(gas("O", U(1, 8)))
                .out(dust("KOH", U(3, 8))).add();
    }

    private void optionalElectrolysis(int line, String fluid, int mb, long ticks, String hydroxide) {
        if (!fluidExists(fluid)) {
            ChemRecipeResolver.optionalAbsent(SOURCE + line, fluid + " is not registered");
            return;
        }
        row("Electrolyzer", false, 16, ticks, SOURCE + line).kept(selector(0)).fluid(nativeFluid(fluid, mb))
                .out(gas("H", U(2))).out(gas("O", U(1))).add();
    }

    private void saltElectrolysis(int line, String key, int mb, long ticks, String hydroxide,
                                  MaterialAmount hydrogen, MaterialAmount oxygen) {
        if (!fluidExists(key)) {
            ChemRecipeResolver.optionalAbsent(SOURCE + line, key + " is not registered");
            return;
        }
        row("Electrolyzer", false, 16, ticks, SOURCE + line).kept(selector(0)).fluid(nativeFluid(key, mb))
                .out(gas("Cl", U(1, 8))).out(gas("H", hydrogen)).out(gas("O", oxygen))
                .out(dust(hydroxide, U(3, 8))).add();
    }
    private void fermentationAndDistillationRows() {
        row("Fermenter", 16, 24, 329).kept(selector(0)).fluid(nativeFluid("Biomass", 40))
                .out(nativeFluid("Methane", 8)).add();
        if (fluidExists("BiomassIC2")) {
            row("Fermenter", 16, 24, 330).kept(selector(0)).fluid(nativeFluid("BiomassIC2", 40))
                    .out(nativeFluid("Methane", 8)).add();
        } else {
            ChemRecipeResolver.optionalAbsent(SOURCE + 330, "BiomassIC2 is not registered");
        }
        row("Distillery", 16, 24, 333).kept(selector(0)).fluid(nativeFluid("Biomass", 40))
                .out(nativeFluid("BioEthanol", 12)).out(nativeFluid("DistW", 20)).add();
        if (fluidExists("BiomassIC2")) {
            row("Distillery", 16, 24, 334).kept(selector(0)).fluid(nativeFluid("BiomassIC2", 40))
                    .out(nativeFluid("BioEthanol", 12)).out(nativeFluid("DistW", 20)).add();
        } else {
            ChemRecipeResolver.optionalAbsent(SOURCE + 334, "BiomassIC2 is not registered");
        }
        distilleryOil(335, "Oil_ExtraHeavy", 35, 50, false);
        distilleryOil(336, "Oil_Heavy", 30, 40, false);
        distilleryOil(337, "Oil_Heavy2", 30, 40, true);
        distilleryOil(338, "Oil_Medium", 25, 25, false);
        distilleryOil(339, "Oil_Normal", 25, 25, false);
        distilleryOil(340, "Oil_HotCrude", 25, 25, true);
        distilleryOil(341, "Oil_Light", 15, 15, false);
        distilleryOil(342, "Oil_Light2", 15, 15, true);
        distilleryOil(343, "Oil_Soulsand", 10, 40, false);

        row("Distillery", 16, 24, 346).kept(selector(1)).fluid(nativeFluid("Biomass", 40))
                .out(liquid("Glycerol", U(1, 50))).out(nativeFluid("DistW", 20)).add();
        if (fluidExists("BiomassIC2")) {
            row("Distillery", 16, 24, 347).kept(selector(1)).fluid(nativeFluid("BiomassIC2", 40))
                    .out(liquid("Glycerol", U(1, 50))).out(nativeFluid("DistW", 20)).add();
        } else {
            ChemRecipeResolver.optionalAbsent(SOURCE + 347, "BiomassIC2 is not registered");
        }

        towerBiomass(350, "Biomass");
        towerBiomass(351, "BiomassIC2");
        towerOil(352, "Oil_ExtraHeavy", 5000, 35, 25, 25, 20, 5, 5, 50, 256, false);
        towerOil(353, "Oil_Heavy", 4000, 30, 20, 20, 15, 10, 10, 40, 196, false);
        towerOil(354, "Oil_Heavy2", 4000, 30, 20, 20, 15, 10, 10, 40, 196, true);
        towerOil(355, "Oil_Medium", 3000, 25, 15, 15, 15, 15, 15, 25, 128, false);
        towerOil(356, "Oil_Normal", 3000, 25, 15, 15, 15, 15, 15, 25, 128, false);
        towerOil(357, "Oil_HotCrude", 3000, 25, 15, 15, 15, 15, 15, 25, 64, true);
        towerOil(358, "Oil_Light", 2000, 15, 10, 10, 10, 25, 25, 15, 64, false);
        towerOil(359, "Oil_Light2", 2000, 15, 10, 10, 10, 25, 25, 15, 64, true);
        towerOil(360, "Oil_Soulsand", 1000, 10, 5, 5, 5, 5, 5, 40, 64, false);
    }

    private void distilleryOil(int line, String oil, int fuel, int lubricant, boolean optional) {
        if (optional && !fluidExists(oil)) {
            ChemRecipeResolver.optionalAbsent(SOURCE + line, oil + " is not registered");
            return;
        }
        if (!fluidExists(oil)) {
            ChemRecipeResolver.optionalAbsent(SOURCE + line, oil + " is not registered");
            return;
        }
        row("Distillery", false, 16, 16, SOURCE + line).kept(selector(0)).fluid(nativeFluid(oil, 25))
                .out(nativeFluid("Fuel", fuel)).out(nativeFluid("Lubricant", lubricant)).add();
    }

    private void towerBiomass(int line, String biomass) {
        if (!fluidExists(biomass)) {
            ChemRecipeResolver.optionalAbsent(SOURCE + line, biomass + " is not registered");
            return;
        }
        row("DistillationTower", false, 64, 16, SOURCE + line)
                .fluid(nativeFluid(biomass, 80))
                .out(nativeFluid("BioEthanol", 20)).out(liquid("Glycerol", U(1, 50)))
                .out(nativeFluid("Methane", 4)).out(nativeFluid("DistW", 50)).add();
    }

    private void towerOil(int line, String oil, int chance, int fuel, int diesel, int kerosine, int petrol,
                          int propane, int butane, int lubricant, long ticks, boolean optional) {
        if (!fluidExists(oil)) {
            ChemRecipeResolver.optionalAbsent(SOURCE + line, oil + " is not registered");
            return;
        }
        row("DistillationTower", false, 64, ticks, SOURCE + line).fluid(nativeFluid(oil, 25))
                .out(nativeFluid("Fuel", fuel)).out(nativeFluid("Diesel", diesel))
                .out(nativeFluid("Kerosine", kerosine)).out(nativeFluid("Petrol", petrol))
                .out(nativeFluid("Propane", propane)).out(nativeFluid("Butane", butane))
                .out(nativeFluid("Lubricant", lubricant))
                .out(form("dustTiny", "WaxParaffin", 1), chance)
                .out(form("dustTiny", "Asphalt", 1), chance)
                .out(form("dustTiny", "PetCoke", 1), chance).add();
    }
    private void cryoAndCrackingRows() {
        row("CryoDistillationTower", 64, 64, 363).out(gas("N", src(1, 7), CEIL)).out(gas("O", U(1, 20), CEIL))
                .out(gas("CO2", U(1, 100), CEIL)).out(gas("He", U(1, 1000), CEIL))
                .out(gas("Ne", U(1, 1000), CEIL)).out(gas("Ar", U(1, 1000), CEIL))
                .fluid(nativeFluid("Air", 200)).out(form("dustTiny", "Ice", 1), 9000).add();
        row("CryoDistillationTower", 64, 64, 364).out(gas("N", src(1, 7), CEIL)).out(gas("O", U(1, 20), CEIL))
                .out(gas("SO2", U(1, 100), CEIL)).out(gas("He", U(1, 1000), CEIL))
                .out(gas("Ne", U(1, 1000), CEIL)).out(gas("Ar", U(1, 1000), CEIL))
                .fluid(nativeFluid("Air_Nether", 200)).out(form("dustTiny", "Ash", 1), 7000).add();
        row("CryoDistillationTower", 64, 64, 365).out(gas("N", src(1, 7), CEIL)).out(gas("O", U(1, 20), CEIL))
                .out(gas("CO2", U(1, 100), CEIL)).out(gas("Kr", U(1, 1000), CEIL))
                .out(gas("Xe", U(1, 1000), CEIL)).out(gas("Rn", U(1, 1000), CEIL))
                .fluid(nativeFluid("Air_End", 200)).out(form("dustTiny", "Ice", 1), 6000).add();

        row("SteamCracking", 16, 64, 368).fluid(nativeFluid("Steam", 1000)).fluid(nativeFluid("Propane", 100))
                .out(nativeFluid("Hydrogen", 2)).out(nativeFluid("Methane", 27))
                .out(nativeFluid("Ethylene", 42)).out(nativeFluid("Propylene", 19)).add();
        row("SteamCracking", 16, 64, 369).fluid(nativeFluid("Steam", 1000)).fluid(nativeFluid("Butane", 100))
                .out(nativeFluid("Hydrogen", 5)).out(nativeFluid("Methane", 9))
                .out(nativeFluid("Ethylene", 78)).out(nativeFluid("Propylene", 3)).add();

        if (fluidExists("Reikanol")) {
            catalyticCracking(373, "Reikanol", 20, 5);
        } else {
            ChemRecipeResolver.optionalAbsent(SOURCE + 373, "Reikanol is not registered");
        }
        catalyticCracking(374, "BioEthanol", 20, 5);
        catalyticCracking(375, "Petrol", 30, 20);
        catalyticCracking(376, "Fuel", 40, 10);
        ChemRecipeResolver.exclude(SOURCE + "378-382", "cooking-oil family is not registered in this port");
    }

    private void catalyticCracking(int line, String feed, int ethylene, int propylene) {
        row("CatalyticCracking", false, 16, 64, SOURCE + line).kept(form("dust", "Pt", 1))
                .fluid(nativeFluid("Hydrogen", 100)).fluid(nativeFluid(feed, 100))
                .out(nativeFluid("Ethylene", ethylene)).out(nativeFluid("Propylene", propylene)).add();
    }

    private void bioAndPlasticRows() {
        row("Mixer", 16, 16, 385).kept(form("dust", "MgCl2", 1))
                .fluid(liquid("TiCl4", U(1, 1000), CEIL)).fluid(nativeFluid("Ethylene", 100))
                .out(form("dust", "Plastic", 1)).add();
        row("Mixer", 16, 16, 386).kept(form("dust", "MgCl2", 1))
                .fluid(liquid("TiCl4", U(1, 1000), CEIL)).fluid(nativeFluid("Propylene", 100))
                .out(form("dust", "Plastic", 1)).add();

        row("Injector", 64, 1152, 389).item(form("dust", "Th", 1)).fluid(liquid("LiCl", U(144), CEIL))
                .out(nativeFluid("Thorium_Salt", 20736)).add();
        row("Injector", 64, 288, 390).item(form("dustSmall", "Th", 1)).fluid(liquid("LiCl", U(36), CEIL))
                .out(nativeFluid("Thorium_Salt", 5184)).add();
        row("Injector", 64, 128, 391).item(form("dustTiny", "Th", 1)).fluid(liquid("LiCl", U(16), CEIL))
                .out(nativeFluid("Thorium_Salt", 2304)).add();
        row("Injector", 64, 16, 392).item(form("dustDiv72", "Th", 1)).fluid(liquid("LiCl", U(2), CEIL))
                .out(nativeFluid("Thorium_Salt", 288)).add();
        row("Injector", 16, 256, 394).item(dust("Desh", U(1))).fluid(liquid("Hg", U(1), CEIL))
                .fluid(nativeFluid("Fuel", 400)).out(dust("DeshAlloy", U(1))).add();
    }

    private void roastingRows() {
        for (GTMaterial member : group(MaterialGroups.C)) {
            if (member.getName().equals("Graphene")) continue;
            String name = member.getName();
            row("Roasting", false, 16, 16, SOURCE + "398/" + name).item(dust(name, U(1)))
                    .fluid(gas("CO2", U(3), CEIL)).out(gas("CO", U(4))).add();
        }
        row("Roasting", 16, 16, 399).item(dust("Charcoal", U(1))).fluid(gas("CO2", U(3), CEIL))
                .out(gas("CO", U(4))).add();
        row("Roasting", 16, 16, 400).item(dust("Coal", U(1))).fluid(gas("CO2", U(6), CEIL))
                .out(gas("CO", U(8))).add();
        row("Roasting", 16, 16, 401).item(dust("CoalCoke", U(1))).fluid(gas("CO2", U(6), CEIL))
                .out(gas("CO", U(8))).add();
        for (GTMaterial member : group(MaterialGroups.Diamond)) {
            String name = member.getName();
            row("Roasting", false, 16, 16, SOURCE + "402/" + name).item(dust(name, U(1)))
                    .fluid(gas("CO2", U(12), CEIL)).out(gas("CO", U(16))).add();
        }

        roastSulfide(408, false, "Pyrite", "Oxygen", 1834, U(2), "Fe2O3", U(5, 6), null, null, null, null);
        roastSulfide(409, false, "Realgar", "Oxygen", 1000, U(3, 2), "As", U(1, 2), null, null, null, null);
        roastSulfide(410, false, "Sphalerite", "Oxygen", 1000, U(3, 2), "Zn", U(1, 2), null, null, null, null);
        roastSulfide(411, false, "Molybdenite", "Oxygen", 1334, U(2), "Mo", U(1, 3), null, null, null, null);
        roastSulfide(412, false, "Stibnite", "Oxygen", 1200, U(9, 5), "Sb", U(2, 5), null, null, null, null);
        roastSulfide(413, false, "Pentlandite", "Oxygen", 942, src(24, 17), "Ni", src(9, 17), null, null, null, null);
        roastSulfide(414, false, "Chalcopyrite", "Oxygen", 1375, U(3, 2), "Fe2O3", U(5, 8), "Cu", U(1, 4), null, null);
        roastSulfide(415, false, "Arsenopyrite", "Oxygen", 1167, U(1), "Fe2O3", U(5, 6), "As", U(1, 3), null, null);
        roastSulfide(416, false, "Cobaltite", "Oxygen", 667, U(1), "Co", U(1, 3), "As", U(1, 3), null, null);
        roastSulfide(417, false, "Galena", "Oxygen", 500, U(3, 4), "Ag", U(3, 8), "Pb", U(3, 8), null, null);
        roastSulfide(418, false, "Cooperite", "Oxygen", 334, U(1, 2), "Pt", U(1, 2), "Ni", U(1, 6), "Pd", U(1, 6));
        roastSulfide(419, false, "Tetrahedrite", "Oxygen", 938, U(9, 8), "Cu", U(3, 8), "Sb", U(1, 8), "Fe2O3", U(5, 16));
        roastSulfide(420, false, "Stannite", "Oxygen", 1188, U(3, 2), "Cu", U(1, 4), "Sn", U(1, 8), "Fe2O3", U(5, 16));
        roastSulfide(421, false, "Kesterite", "Oxygen", 1000, U(3, 2), "Cu", U(1, 4), "Zn", U(1, 8), "Sn", U(1, 8));

        row("Roasting", 16, 128, 423).item(dust("Li", U(1))).fluid(nativeFluid("Oxygen", 500))
                .out(dust("Li2O", U(3, 2))).add();
        row("Roasting", 16, 128, 424).item(dust("V", U(1))).fluid(nativeFluid("Oxygen", 2500))
                .out(dust("V2O5", U(7, 2))).add();
        row("Roasting", 16, 128, 425).item(dust("Nb", U(1))).fluid(nativeFluid("Oxygen", 2500))
                .out(dust("Nb2O5", U(7, 2))).add();
        row("Roasting", 16, 128, 426).item(dust("Ta", U(1))).fluid(nativeFluid("Oxygen", 2500))
                .out(dust("Ta2O5", U(7, 2))).add();
        row("Roasting", 16, 128, 427).item(dust("Cr", U(1))).fluid(nativeFluid("Oxygen", 2000))
                .out(dust("CrO2", U(1))).add();
        row("Roasting", 16, 16, 428).item(form("dust", "S", 1)).fluid(nativeFluid("Oxygen", 2000))
                .out(gas("SO2", U(3))).add();
        row("Roasting", 16, 144, 429).item(block("blockDust", "S", 1)).fluid(nativeFluid("Oxygen", 18000))
                .out(gas("SO2", U(27))).add();
        row("Roasting", 16, 16, 430).item(form("dustTiny", "Blaze", 1)).fluid(nativeFluid("Oxygen", 2000))
                .out(gas("SO2", U(3))).add();
        row("Roasting", 16, 144, 431).item(form("dust", "Blaze", 1)).fluid(nativeFluid("Oxygen", 18000))
                .out(gas("SO2", U(27))).add();
        row("Roasting", 16, 1296, 432).item(block("blockDust", "Blaze", 1)).fluid(nativeFluid("Oxygen", 162000))
                .out(gas("SO2", U(243))).add();
        for (GTMaterial member : group(MaterialGroups.C)) {
            if (member.getName().equals("Graphene")) continue;
            String name = member.getName();
            row("Roasting", false, 16, 16, SOURCE + "434/" + name).item(dust(name, U(1)))
                    .fluid(nativeFluid("Oxygen", 2000)).out(gas("CO2", U(3))).add();
        }
        row("Roasting", 16, 16, 436).item(dust("Charcoal", U(1))).fluid(nativeFluid("Oxygen", 2000))
                .out(gas("CO2", U(3))).add();
        row("Roasting", 16, 16, 437).item(dust("Coal", U(1))).fluid(nativeFluid("Oxygen", 4000))
                .out(gas("CO2", U(6))).add();
        row("Roasting", 16, 16, 438).item(dust("CoalCoke", U(1))).fluid(nativeFluid("Oxygen", 4000))
                .out(gas("CO2", U(6))).add();
        for (GTMaterial member : group(MaterialGroups.Diamond)) {
            String name = member.getName();
            row("Roasting", false, 16, 16, SOURCE + "439/" + name).item(dust(name, U(1)))
                    .fluid(nativeFluid("Oxygen", 8000)).out(gas("CO2", U(12))).add();
        }

        roastSulfide(446, true, "Pyrite", "Air", 8000, U(2), "Fe2O3", U(5, 6), null, null, null, null);
        roastSulfide(447, true, "Realgar", "Air", 4000, U(3, 2), "As", U(1, 2), null, null, null, null);
        roastSulfide(448, true, "Sphalerite", "Air", 4000, U(3, 2), "Zn", U(1, 2), null, null, null, null);
        roastSulfide(449, true, "Molybdenite", "Air", 6000, U(2), "Mo", U(1, 3), null, null, null, null);
        roastSulfide(450, true, "Stibnite", "Air", 5000, U(9, 5), "Sb", U(2, 5), null, null, null, null);
        roastSulfide(451, true, "Pentlandite", "Air", 4000, src(24, 17), "Ni", src(9, 17), null, null, null, null);
        roastSulfide(452, true, "Chalcopyrite", "Air", 6000, U(3, 2), "Fe2O3", U(5, 8), "Cu", U(1, 4), null, null);
        roastSulfide(453, true, "Arsenopyrite", "Air", 5000, U(1), "Fe2O3", U(5, 6), "As", U(1, 3), null, null);
        roastSulfide(454, true, "Cobaltite", "Air", 3000, U(1), "Co", U(1, 3), "As", U(1, 3), null, null);
        roastSulfide(455, true, "Galena", "Air", 2000, U(3, 4), "Ag", U(3, 8), "Pb", U(3, 8), null, null);
        roastSulfide(456, true, "Cooperite", "Air", 2000, U(1, 2), "Pt", U(1, 2), "Ni", U(1, 6), "Pd", U(1, 6));
        roastSulfide(457, true, "Tetrahedrite", "Air", 4000, U(9, 8), "Cu", U(3, 8), "Sb", U(1, 8), "Fe2O3", U(5, 16));
        roastSulfide(458, true, "Stannite", "Air", 5000, U(3, 2), "Cu", U(1, 4), "Sn", U(1, 8), "Fe2O3", U(5, 16));
        roastSulfide(459, true, "Kesterite", "Air", 4000, U(3, 2), "Cu", U(1, 4), "Zn", U(1, 8), "Sn", U(1, 8));

        row("Roasting", 16, 16, 460).item(form("dust", "S", 1)).fluid(nativeFluid("Air", 8000))
                .out(gas("SO2", U(3))).add();
        row("Roasting", 16, 144, 461).item(block("blockDust", "S", 1)).fluid(nativeFluid("Air", 72000))
                .out(gas("SO2", U(27))).add();
        row("Roasting", 16, 16, 462).item(form("dustTiny", "Blaze", 1)).fluid(nativeFluid("Air", 8000))
                .out(gas("SO2", U(3))).add();
        row("Roasting", 16, 144, 463).item(form("dust", "Blaze", 1)).fluid(nativeFluid("Air", 72000))
                .out(gas("SO2", U(27))).add();
        row("Roasting", 16, 1296, 464).item(block("blockDust", "Blaze", 1)).fluid(nativeFluid("Air", 648000))
                .out(gas("SO2", U(243))).add();
    }

    private void roastSulfide(int line, boolean air, String ore, String oxidizer, int mb, MaterialAmount so2,
                              String solid1, MaterialAmount amount1, String solid2, MaterialAmount amount2,
                              String solid3, MaterialAmount amount3) {
        Row row = row("Roasting", false, 16, 512, SOURCE + line).item(dust(ore, U(1)))
                .fluid(nativeFluid(oxidizer, mb)).out(gas("SO2", so2));
        int chance = air ? 8000 : CERTAIN;
        if (solid1 != null) row.out(dust(solid1, amount1), chance);
        if (solid2 != null) row.out(dust(solid2, amount2), chance);
        if (solid3 != null) row.out(dust(solid3, amount3), chance);
        row.add();
    }
    private void isotopeRows() {
        if (fluidExists("Heavy_Reiker")) {
            isotope(467, "Heavy_Reiker", 10000);
        } else {
            ChemRecipeResolver.optionalAbsent(SOURCE + 467, "Heavy_Reiker is not registered");
        }
        for (String water : new String[] {"Water", "MnWtr", "DistW"}) {
            isotope(SOURCE + "470/" + water, water, 100000);
        }
        optionalIsotope(471, "Water_Geothermal", 50000);
        optionalIsotope(472, "Water_Boiling", 100000);
        optionalIsotope(473, "Water_Hot", 100000);
        optionalIsotope(474, "Hot_Water", 100000);
        optionalIsotope(475, "Cold_Water", 100000);
        row("Centrifuge", 64, 64, 476).fluid(liquid("HDO", U(1), CEIL))
                .out(liquid("D2O", U(1, 10))).out(liquid("T2O", U(1, 100))).add();
        row("Centrifuge", 64, 64, 477).fluid(liquid("D2O", U(1), CEIL))
                .out(liquid("T2O", U(1, 10))).add();
    }

    private void isotope(int line, String key, int mb) {
        isotope(SOURCE + line, key, mb);
    }

    private void isotope(String source, String key, int mb) {
        row("Centrifuge", false, 64, 64, source).fluid(nativeFluid(key, mb))
                .out(liquid("HDO", U(1, 10))).out(liquid("D2O", U(1, 100)))
                .out(liquid("T2O", U(1, 1000))).add();
    }

    private void optionalIsotope(int line, String key, int mb) {
        if (!fluidExists(key)) {
            ChemRecipeResolver.optionalAbsent(SOURCE + line, key + " is not registered");
            return;
        }
        isotope(line, key, mb);
    }

    private void meltingRows() {
        for (String map : new String[] {"Melter", "Smelter"}) {
            int line = map.equals("Melter") ? 480 : 495;
            iceRow(map, line, "dust", "Ice", 1, U(1, 9), 111, 222);
            iceRow(map, line + 1, "dust", "Ice", 1, U(1, 4), 250, 500);
            iceRow(map, line + 2, "dust", "Ice", 1, U(1), 1000, 2000);
            iceRow(map, line + 3, "gemChipped", "Ice", 1, null, 250, 500);
            iceRow(map, line + 4, "gemFlawed", "Ice", 1, null, 500, 1000);
            iceRow(map, line + 5, "gem", "Ice", 1, null, 1000, 2000);
            iceRow(map, line + 6, "vanilla", "ice", 1, null, 1000, 2000);
            iceRow(map, line + 7, "vanilla", "packed_ice", 1, null, 2000, 4000);
            iceRow(map, line + 8, "dust", "Snow", 1, U(1, 9), 111, 222);
            iceRow(map, line + 9, "dust", "Snow", 1, U(1, 4), 250, 500);
            iceRow(map, line + 10, "dust", "Snow", 1, U(1), 1000, 2000);
            iceRow(map, line + 11, "vanilla", "snowball", 1, null, 250, 500);
            iceRow(map, line + 12, "vanilla", "snow", 1, null, 1000, 2000);
        }
        dryingIce(510, "dust", "Ice", U(1, 9), 111, 444);
        dryingIce(511, "dust", "Ice", U(1, 4), 250, 1000);
        dryingIce(512, "dust", "Ice", U(1), 1000, 4000);
        dryingIce(513, "gemChipped", "Ice", null, 250, 1000);
        dryingIce(514, "gemFlawed", "Ice", null, 500, 2000);
        dryingIce(515, "gem", "Ice", null, 1000, 4000);
        dryingIce(516, "vanilla", "ice", null, 1000, 4000);
        dryingIce(517, "vanilla", "packed_ice", null, 2000, 8000);
        dryingIce(518, "dust", "Snow", U(1, 9), 111, 444);
        dryingIce(519, "dust", "Snow", U(1, 4), 250, 1000);
        dryingIce(520, "dust", "Snow", U(1), 1000, 4000);
        dryingIce(521, "vanilla", "snowball", null, 250, 1000);
        dryingIce(522, "vanilla", "snow", null, 1000, 4000);
    }

    private void iceRow(String map, int line, String prefix, String material, int count,
                        MaterialAmount units, int mb, long ticks) {
        Row row = row(map, false, 16, ticks, SOURCE + line);
        if (prefix.equals("vanilla")) row.item(vanilla(material, count));
        else if (units != null) row.item(dust(material, units));
        else row.item(form(prefix, material, count));
        row.out(nativeFluid("Water", mb)).add();
    }

    private void dryingIce(int line, String prefix, String material, MaterialAmount units, int mb, long ticks) {
        Row row = row("Drying", false, 16, ticks, SOURCE + line);
        if (prefix.equals("vanilla")) row.item(vanilla(material, 1));
        else if (units != null) row.item(dust(material, units));
        else row.item(form(prefix, material, 1));
        row.out(nativeFluid("DistW", mb)).add();
    }
    /** Port-reviewed chains that the original loader expressed through another map or helper. */
    private void portExtras() {
        row("Mixer", false, 64, 320, "port:ChemicalRecipeLoader:79-80")
                .fluid(gas("N", U(2))).fluid(gas("H", U(6))).out(gas("NH3", U(8))).add();
        row("BurnMixer", false, 16, 96, "port:ChemicalRecipeLoader:74")
                .fluid(gas("SO2", U(3))).fluid(nativeFluid("Oxygen", 1000))
                .out(gas("SO3", U(4))).add();
    }

    private void dryingRows() {
        dryingWater(525, "Water", 10, 8, false);
        if (fluidExists("SpDew")) dryingWater(526, "SpDew", 10, 8, false);
        else ChemRecipeResolver.optionalAbsent(SOURCE + 526, "SpDew is not registered");
        dryingWater(527, "MnWtr", 10, 8, false);
        optionalDryingWater(528, "Water_Geothermal", 25, 20);
        optionalDryingWater(529, "Water_Boiling", 25, 20);
        optionalDryingWater(530, "Water_Hot", 25, 20);
        optionalDryingWater(531, "Hot_Water", 25, 20);
        optionalDryingWater(532, "Cold_Water", 25, 20);

        distillingWater(534, "Water", 10, 8);
        if (fluidExists("SpDew")) distillingWater(535, "SpDew", 10, 8);
        else ChemRecipeResolver.optionalAbsent(SOURCE + 535, "SpDew is not registered");
        distillingWater(536, "MnWtr", 10, 8);
        optionalDistillingWater(537, "Water_Geothermal", 25, 20);
        optionalDistillingWater(538, "Water_Boiling", 25, 20);
        optionalDistillingWater(539, "Water_Hot", 25, 20);
        optionalDistillingWater(540, "Hot_Water", 25, 20);
        optionalDistillingWater(541, "Cold_Water", 25, 20);

        saltyDrying(545, "Tropics_Water", 7000, 6750, "NaCl");
        saltyDrying(546, "OceanGrC", 7000, 6750, "NaCl");
        saltyDrying(548, "Ocean", 7000, 6750, "NaCl");
        saltyDrying(550, "Brine", 1000, 750, "NaCl");
        row("Drying", 16, 1600, 551).fluid(liquid("SaltWater", U(1), CEIL))
                .out(nativeFluid("DistW", 750)).out(dust("NaCl", U(1, 4))).add();
        row("Drying", 16, 1600, 552).fluid(liquid("SaltedWater", U(1), CEIL))
                .out(nativeFluid("DistW", 750)).out(dust("KCl", U(1, 4))).add();
        if (fluidExists("Dirty_Water")) {
            row("Drying", 16, 16000, 553).fluid(nativeFluid("Dirty_Water", 8000))
                    .out(nativeFluid("DistW", 7000)).out(vanilla("dirt", 1)).add();
        } else {
            ChemRecipeResolver.optionalAbsent(SOURCE + 553, "Dirty_Water is not registered");
        }
        optionalSimpleDrying(555, "Swampwater", 10, 5);
        optionalSimpleDrying(557, "Stagnant_Water", 10, 5);

        row("Drying", 16, 60000, 559).item(dust("Mirabilite", U(7)))
                .out(nativeFluid("DistW", 30000)).out(dust("Na2SO4", U(7))).add();
        row("Drying", 16, 18000, 560).item(dust("FeO3H3", U(14)))
                .out(nativeFluid("DistW", 9000)).out(dust("Fe2O3", U(5))).add();
        row("Drying", 16, 18000, 561).item(dust("AlO3H3", U(14)))
                .out(nativeFluid("DistW", 9000)).out(dust("Al2O3", U(5))).add();
        row("Drying", 16, 6000, 562).item(dust("H2WO4", U(7)))
                .out(nativeFluid("DistW", 3000)).out(dust("WO3", U(4))).add();
        row("Drying", 16, 4000, 563).item(dust("Bischofite", U(1)))
                .out(nativeFluid("DistW", 2000)).out(dust("MgCl2", U(1))).add();
        row("Drying", 16, 2000, 564).item(dust("Trona", U(1)))
                .out(nativeFluid("DistW", 1000)).out(dust("Na2CO3", U(1))).add();
        row("Drying", 16, 2000, 565).item(dust("Gypsum", U(1)))
                .out(nativeFluid("DistW", 1000)).out(dust("CaSO4", U(1))).add();
        row("Drying", 16, 2000, 566).item(dust("Perlite", U(1)))
                .out(nativeFluid("DistW", 1000)).out(dust("Obsidian", U(1))).add();
        for (GTMaterial member : group(MaterialGroups.Clay)) {
            String name = member.getName();
            row("Drying", false, 16, 1000, SOURCE + "568/" + name).item(dust(name, U(1)))
                    .out(nativeFluid("DistW", 500)).out(dust("Ceramic", U(1))).add();
        }
    }

    private void dryingWater(int line, String key, int in, int out, boolean distilled) {
        row("Drying", false, 16, 16, SOURCE + line).fluid(nativeFluid(key, in))
                .out(nativeFluid("DistW", out)).add();
    }

    private void optionalDryingWater(int line, String key, int in, int out) {
        if (!fluidExists(key)) {
            ChemRecipeResolver.optionalAbsent(SOURCE + line, key + " is not registered");
            return;
        }
        dryingWater(line, key, in, out, false);
    }

    private void distillingWater(int line, String key, int in, int out) {
        row("Distillery", false, 16, 16, SOURCE + line).kept(selector(0)).fluid(nativeFluid(key, in))
                .out(nativeFluid("DistW", out)).add();
    }

    private void optionalDistillingWater(int line, String key, int in, int out) {
        if (!fluidExists(key)) {
            ChemRecipeResolver.optionalAbsent(SOURCE + line, key + " is not registered");
            return;
        }
        distillingWater(line, key, in, out);
    }

    private void saltyDrying(int line, String key, int in, int out, String salt) {
        if (!fluidExists(key)) {
            ChemRecipeResolver.optionalAbsent(SOURCE + line, key + " is not registered");
            return;
        }
        row("Drying", false, 16, line == 550 ? 1600 : 11200, SOURCE + line).fluid(nativeFluid(key, in))
                .out(nativeFluid("DistW", out)).out(dust(salt, U(1, 4))).add();
    }

    private void optionalSimpleDrying(int line, String key, int in, int out) {
        if (!fluidExists(key)) {
            ChemRecipeResolver.optionalAbsent(SOURCE + line, key + " is not registered");
            return;
        }
        row("Drying", false, 16, 16, SOURCE + line).fluid(nativeFluid(key, in))
                .out(nativeFluid("DistW", out)).add();
    }
}
