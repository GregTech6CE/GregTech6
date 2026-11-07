package gregtech.content.material;

import gregtech.api.material.MaterialProperty;
import gregtech.engine.material.MaterialDefinition;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Import corrections for fluid phase flags that must exist before material publication. */
final class MaterialPhaseFlags {
    private static final Set<MaterialProperty> LIQUID = Set.of(MaterialProperty.LIQUID);
    private static final Set<MaterialProperty> MOLTEN = Set.of(MaterialProperty.MOLTEN);
    private static final Set<MaterialProperty> GAS = Set.of(MaterialProperty.GAS);
    private static final Map<String, Set<MaterialProperty>> ADDITIONAL = Map.ofEntries(
            flags("Mercury", LIQUID),
            flags("SemiheavyWater", LIQUID),
            flags("HeavyWater", LIQUID),
            flags("TritiatedWater", LIQUID),
            flags("Carbon", MOLTEN),
            flags("Carbon13", MOLTEN),
            flags("Adamantium", MOLTEN),
            flags("Nikolite", MOLTEN),
            flags("Glowstone", MOLTEN),
            flags("VanadiumMagnetite", MOLTEN),
            flags("TitaniumTetrachloride", LIQUID),
            flags("NitricAcid", LIQUID),
            flags("SulfuricAcid", LIQUID),
            flags("DisulfuricAcid", LIQUID),
            flags("HexafluorosilicicAcid", LIQUID),
            flags("Cryolite", LIQUID),
            flags("LithiumChloride", LIQUID),
            flags("LithiumChlorate", LIQUID),
            flags("AluminiumFluoride", LIQUID),
            flags("AquaRegia", LIQUID),
            flags("ChloroauricAcid", LIQUID),
            flags("ChloroplatinicAcid", LIQUID),
            flags("StannicChloride", LIQUID),
            flags("BlueVitriol", LIQUID),
            flags("CyanVitriol", LIQUID),
            flags("GrayVitriol", LIQUID),
            flags("GreenVitriol", LIQUID),
            flags("PinkVitriol", LIQUID),
            flags("RedVitriol", LIQUID),
            flags("WhiteVitriol", LIQUID),
            flags("VitriolOfClay", LIQUID),
            flags("Sulfur", MOLTEN),
            flags("Alumina", MOLTEN),
            flags("Hematite", MOLTEN),
            flags("CalciumChloride", MOLTEN),
            flags("Fluorite", MOLTEN),
            flags("MagnesiumCarbonate", MOLTEN),
            flags("MagnesiumChloride", MOLTEN),
            flags("ManganeseChloride", MOLTEN),
            flags("SodiumCarbonate", MOLTEN),
            flags("UraniumTetrafluoride", MOLTEN),
            flags("Uranium235Tetrafluoride", MOLTEN),
            flags("Uranium238Tetrafluoride", MOLTEN),
            flags("HydrochloricAcid", GAS));

    private MaterialPhaseFlags() {}

    static MaterialDefinition.Declaration apply(MaterialDefinition.Declaration declaration) {
        Set<MaterialProperty> additional = ADDITIONAL.get(declaration.name());
        if (additional == null) return declaration;
        var material = declaration.material();
        var properties = new LinkedHashSet<>(material.properties());
        properties.addAll(additional);
        var corrected = new MaterialDefinition(
                material.identity(),
                material.presentation(),
                material.physical(),
                Set.copyOf(properties),
                material.source());
        return new MaterialDefinition.Declaration(corrected, declaration.info());
    }

    private static Map.Entry<String, Set<MaterialProperty>> flags(String material, Set<MaterialProperty> properties) {
        return Map.entry(material, properties);
    }
}
