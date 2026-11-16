package gregtech.content.material.catalog;

import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.content.material.MaterialFactKeys;
import gregtech.engine.material.MaterialDefinition;
import gregtech.engine.material.MaterialInfoDefinition;
import java.util.LinkedHashMap;
import java.util.List;

/** Material-specific source Crusher/Hammer facts; host rocks are owned by {@code OreHostFacts}. */
public final class SourceOreCrushingMaterialFacts {
    private SourceOreCrushingMaterialFacts() {}

    public static MaterialDefinition.Declaration apply(MaterialDefinition.Declaration declaration) {
        var hexorium = hexorium(declaration.id(), declaration.name());
        var reviewed = SourceRecyclingMaterialFacts.find(declaration.id(), declaration.name());
        if (reviewed == null && hexorium == MaterialFactKeys.HexoriumKind.NONE) return declaration;
        var fact = new MaterialFactKeys.OreCrushingFact(
                pulverizing(declaration.id(), declaration.name(), hexorium), hexorium);
        var info = declaration.info();
        var rules = new LinkedHashMap<>(info.processes().rules());
        MaterialFactKeys.put(rules, MaterialFactKeys.SOURCE_ORE_CRUSHING, List.of(MaterialProcessRule.anyForm(fact)));
        return new MaterialDefinition.Declaration(
                declaration.material(),
                new MaterialInfoDefinition(
                        info.tooling(),
                        info.composition(),
                        info.relations(),
                        new MaterialInfoDefinition.Processes(rules)));
    }

    private static MaterialFactKeys.Decision pulverizing(int id, String name, MaterialFactKeys.HexoriumKind hexorium) {
        if (hexorium != MaterialFactKeys.HexoriumKind.NONE) return MaterialFactKeys.Decision.NO;
        return id == 800 && name.equals("Mercury")
                        || id == 8333 && name.equals("Redstone")
                        || id == 9114 && name.equals("Cinnabar")
                ? MaterialFactKeys.Decision.YES
                : MaterialFactKeys.Decision.NO;
    }

    private static MaterialFactKeys.HexoriumKind hexorium(int id, String name) {
        return switch (id) {
            case 9224 ->
                name.equals("HexoriumBlack")
                        ? MaterialFactKeys.HexoriumKind.BLACK_WHITE
                        : MaterialFactKeys.HexoriumKind.NONE;
            case 9239 ->
                name.equals("HexoriumWhite")
                        ? MaterialFactKeys.HexoriumKind.BLACK_WHITE
                        : MaterialFactKeys.HexoriumKind.NONE;
            case 9225 ->
                name.equals("HexoriumRed")
                        ? MaterialFactKeys.HexoriumKind.RED_GREEN_BLUE
                        : MaterialFactKeys.HexoriumKind.NONE;
            case 9226 ->
                name.equals("HexoriumGreen")
                        ? MaterialFactKeys.HexoriumKind.RED_GREEN_BLUE
                        : MaterialFactKeys.HexoriumKind.NONE;
            case 9228 ->
                name.equals("HexoriumBlue")
                        ? MaterialFactKeys.HexoriumKind.RED_GREEN_BLUE
                        : MaterialFactKeys.HexoriumKind.NONE;
            default -> MaterialFactKeys.HexoriumKind.NONE;
        };
    }
}
