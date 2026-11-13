package gregtech.content.material.catalog;

import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.content.material.MaterialFactKeys;
import gregtech.engine.material.MaterialDefinition;
import gregtech.engine.material.MaterialInfoDefinition;
import gregtech.engine.recipe.UnitConversion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Explicit source phase bindings for audited chemistry and fixed Centrifuge rows.
 * Original CS.U=648648000; FL.createMolten uses144mB/U, createGas1000mB/U.
 * Physical temperature, texture, and current legacy fluid names do not determine this dimension.
 */
public final class MaterialFluidUnitFacts {
    public static final long SOURCE_UNIT = GTValues.U;

    public record Binding(
            String materialName,
            int materialId,
            String phase,
            String fluidField,
            String registryName,
            long referenceSourceUnits,
            long referenceMillibuckets) {}

    private static final Binding[] BINDINGS = {
        new Binding("AquaRegia", 9827, "liquid", "GenLiquid_AquaRegia", "aquaregia", SOURCE_UNIT, 1000),
        new Binding("Glass", 8001, "liquid", "Glass", "glass", SOURCE_UNIT, 144),
        new Binding("Blaze", 8211, "liquid", "Blaze", "blaze", SOURCE_UNIT, 1296),
        new Binding("BlueVitriol", 8404, "liquid", "GenLiquid_BlueVitriol", "bluevitriol", SOURCE_UNIT, 1000),
        new Binding("Plastic", 8218, "liquid", "Plastic", "molten.plastic", SOURCE_UNIT, 1000),
        new Binding("Ender", 8318, "liquid", "Ender", "molten.enderpearl", SOURCE_UNIT, 144),
        new Binding("Redstone", 8333, "liquid", "Redstone", "molten.redstone", SOURCE_UNIT, 144),
        new Binding("Lava", 9810, "liquid", "Lava", "lava", 9 * SOURCE_UNIT, 1000),
        new Binding("CFoam", 9883, "liquid", "CFoam", "ic2constructionfoam", SOURCE_UNIT, 100),
        new Binding("Rubber", 8217, "liquid", "GenMolten_Rubber", "molten.rubber", SOURCE_UNIT, 144),
        new Binding("Sugar", 9703, "liquid", "GenMolten_Sugar", "molten.sugar", SOURCE_UNIT, 144),
        new Binding("Cheese", 9780, "liquid", "GenMolten_Cheese", "molten.cheese", SOURCE_UNIT, 144),
        new Binding("Cryolite", 8009, "liquid", "GenLiquid_Cryolite", "cryolite", SOURCE_UNIT, 144),
        new Binding(
                "AluminiumFluoride",
                8010,
                "liquid",
                "GenLiquid_AluminiumFluoride",
                "aluminiumfluoride",
                SOURCE_UNIT,
                144),
        new Binding(
                "LithiumChloride", 8029, "liquid", "GenLiquid_LithiumChloride", "lithiumchloride", SOURCE_UNIT, 144),
        new Binding(
                "LithiumChlorate", 8033, "liquid", "GenLiquid_LithiumChlorate", "lithiumchlorate", SOURCE_UNIT, 144),
        new Binding("CarbonDioxide", 9836, "gas", "CarbonDioxide", "carbondioxide", SOURCE_UNIT, 1000),
        new Binding("Chlorine", 170, "gas", "GenGas_Chlorine", "chlorine", SOURCE_UNIT, 1000),
        new Binding(
                "ChloroplatinicAcid",
                8401,
                "liquid",
                "GenLiquid_ChloroplatinicAcid",
                "chloroplatinicacid",
                SOURCE_UNIT,
                1000),
        new Binding("CyanVitriol", 8408, "liquid", "GenLiquid_CyanVitriol", "cyanvitriol", SOURCE_UNIT, 1000),
        new Binding("Fluorine", 90, "gas", "GenGas_Fluorine", "fluorine", SOURCE_UNIT, 1000),
        new Binding("Chocolate", 9783, "liquid", "GenMolten_Chocolate", "molten.chocolate", SOURCE_UNIT, 144),
        new Binding("Deuterium", 11, "gas", "Deuterium", "deuterium", SOURCE_UNIT, 1000),
        new Binding("FishOil", 9871, "liquid", "Oil_Fish", "fishoil", SOURCE_UNIT, 1000),
        new Binding("GrayVitriol", 8410, "liquid", "GenLiquid_GrayVitriol", "grayvitriol", SOURCE_UNIT, 1000),
        new Binding("GreenVitriol", 8405, "liquid", "GenLiquid_GreenVitriol", "greenvitriol", SOURCE_UNIT, 1000),
        new Binding("HeavyWater", 9812, "liquid", "GenGas_HeavyWater", "heavywater", SOURCE_UNIT, 1000),
        new Binding("Helium", 20, "gas", "Helium", "helium", SOURCE_UNIT, 1000),
        new Binding("Helium3", 21, "gas", "GenGas_Helium3", "helium3", SOURCE_UNIT, 1000),
        new Binding("Honey", 9791, "liquid", "Honey", "honey", SOURCE_UNIT, 1000),
        new Binding("Honeydew", 9793, "liquid", "Honeydew", "honeydew", SOURCE_UNIT, 1000),
        new Binding(
                "HydrochloricAcid",
                9826,
                "gas",
                "GenGas_HydrochloricAcid",
                "hydrochloricacid",
                SOURCE_UNIT,
                1000),
        new Binding("HydrogenFluoride", 9829, "gas", "GenGas_HydrogenFluoride", "hydrogenfluoride", SOURCE_UNIT, 1000),
        new Binding("Hydrogen", 10, "gas", "Hydrogen", "hydrogen", SOURCE_UNIT, 1000),
        new Binding("Ice", 9802, "liquid", "Ice", "ice", SOURCE_UNIT, 1000),
        new Binding("Mercury", 800, "liquid", "GenLiquid_Mercury", "mercury", SOURCE_UNIT, 144),
        new Binding("NitrogenDioxide", 9831, "gas", "GenGas_NitrogenDioxide", "nitrogendioxide", SOURCE_UNIT, 1000),
        new Binding("NitrogenMonoxide", 9837, "gas", "GenGas_NitrogenMonoxide", "nitrogenmonoxide", SOURCE_UNIT, 1000),
        new Binding("PinkVitriol", 8407, "liquid", "GenLiquid_PinkVitriol", "pinkvitriol", SOURCE_UNIT, 1000),
        new Binding("RedVitriol", 8406, "liquid", "GenLiquid_RedVitriol", "redvitriol", SOURCE_UNIT, 1000),
        new Binding("SemiheavyWater", 9811, "liquid", "GenGas_SemiheavyWater", "semiheavywater", SOURCE_UNIT, 1000),
        new Binding("SulfuricAcid", 9824, "liquid", "GenLiquid_SulfuricAcid", "sulfuricacid", SOURCE_UNIT, 1000),
        new Binding("TritiatedWater", 9813, "liquid", "GenGas_TritiatedWater", "tritiatedwater", SOURCE_UNIT, 1000),
        new Binding(
                "Uranium235Hexafluoride",
                9012,
                "gas",
                "GenGas_Uranium235Hexafluoride",
                "uranium235hexafluoride",
                SOURCE_UNIT,
                1000),
        new Binding(
                "Uranium238Hexafluoride",
                9010,
                "gas",
                "GenGas_Uranium238Hexafluoride",
                "uranium238hexafluoride",
                SOURCE_UNIT,
                1000),
        new Binding(
                "UraniumHexafluoride",
                9008,
                "gas",
                "GenGas_UraniumHexafluoride",
                "uraniumhexafluoride",
                SOURCE_UNIT,
                1000),
        new Binding("VitriolOfClay", 8412, "liquid", "GenLiquid_VitriolOfClay", "vitriolofclay", SOURCE_UNIT, 1000),
        new Binding("Water", 9800, "liquid", "Water", "water", SOURCE_UNIT, 1000),
        new Binding("Wax", 8235, "liquid", "GenMolten_Wax", "molten.wax", SOURCE_UNIT, 144),
        new Binding("WaxBee", 8236, "liquid", "GenMolten_WaxBee", "molten.waxbee", SOURCE_UNIT, 144),
        new Binding("WaxParaffin", 8238, "liquid", "GenMolten_WaxParaffin", "molten.waxparaffin", SOURCE_UNIT, 144),
        new Binding("WaxPlant", 8239, "liquid", "GenMolten_WaxPlant", "molten.waxplant", SOURCE_UNIT, 144),
        new Binding("WhiteVitriol", 8409, "liquid", "GenLiquid_WhiteVitriol", "whitevitriol", SOURCE_UNIT, 1000),
    };

    private MaterialFluidUnitFacts() {}

    /** Unknown or conflicting source identities are deliberately unsupported, not guessed. */
    static Binding binding(String canonicalName, int materialId, String phase) {
        for (Binding binding : BINDINGS) {
            if (binding.materialName().equals(canonicalName)
                    && binding.materialId() == materialId
                    && binding.phase().equals(phase)) return binding;
        }
        return null;
    }

    public static Binding binding(GTMaterial material, String phase) {
        if (material == null || !material.isValid()) return null;
        var resolved = material.resolve();
        return MaterialFactKeys.value(resolved, MaterialFactKeys.MATERIAL_FLUID_UNIT)
                .flatMap(rule -> rule.units().stream()
                        .filter(value -> value.phase().equals(phase))
                        .findFirst())
                .map(value -> new Binding(
                        resolved.getName(),
                        resolved.getId(),
                        value.phase(),
                        value.fluidField(),
                        value.registryPath(),
                        value.referenceSourceUnits(),
                        value.referenceMillibuckets()))
                .orElse(null);
    }

    public static MaterialDefinition.Declaration apply(MaterialDefinition.Declaration declaration) {
        var values = new java.util.ArrayList<MaterialFactKeys.MaterialFluidUnit>();
        for (Binding binding : BINDINGS) {
            if (binding.materialId() != declaration.id() || !binding.materialName().equals(declaration.name())) continue;
            values.add(new MaterialFactKeys.MaterialFluidUnit(
                    binding.phase(),
                    binding.fluidField(),
                    binding.registryName(),
                    binding.referenceSourceUnits(),
                    binding.referenceMillibuckets()));
        }
        if (values.isEmpty()) return declaration;
        var info = declaration.info();
        var rules = new LinkedHashMap<>(info.processes().rules());
        rules.put(
                MaterialFactKeys.MATERIAL_FLUID_UNIT,
                List.of(MaterialProcessRule.anyForm(new MaterialFactKeys.MaterialFluidUnits(Set.copyOf(values)))));
        return new MaterialDefinition.Declaration(
                declaration.material(),
                new MaterialInfoDefinition(
                        info.tooling(),
                        info.composition(),
                        info.relations(),
                        new MaterialInfoDefinition.Processes(rules)));
    }

    /**
     * Original UT.Code.units ratio and final rounding, without signed multiplication overflow.
     * Returns -1 for invalid inputs or results outside native FluidStack int capacity; zero stays
     * zero, never coerced to1. This is quantity arithmetic, not zero-volume catalyst semantics.
     */
    public static int millibuckets(long sourceAmount, long sourceUnit, long referenceMillibuckets, boolean roundUp) {
        return UnitConversion.fluidAmount(sourceUnit, referenceMillibuckets, sourceAmount, roundUp);
    }
}
