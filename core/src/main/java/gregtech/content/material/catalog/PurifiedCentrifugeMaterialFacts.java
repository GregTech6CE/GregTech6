package gregtech.content.material.catalog;

import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.content.material.MaterialFactKeys;
import gregtech.engine.material.MaterialDefinition;
import gregtech.engine.material.MaterialInfoDefinition;
import java.util.LinkedHashMap;
import java.util.List;

/** Material-owned ambient phase and explicit no-default-phase facts for purified Centrifuge. */
public final class PurifiedCentrifugeMaterialFacts {
    private PurifiedCentrifugeMaterialFacts() {}

    private record Phase(int id, String name, MaterialFactKeys.PurifiedCentrifugePhase value) {}

    private record None(int id, String name, MaterialFactKeys.PurifiedCentrifugeNoDefault value) {}

    private static final List<Phase> PHASES = List.of(
            new Phase(21, "Helium3", new MaterialFactKeys.PurifiedCentrifugePhase("gas", 1, 4)),
            new Phase(90, "Fluorine", new MaterialFactKeys.PurifiedCentrifugePhase("gas", 53, 85)),
            new Phase(800, "Mercury", new MaterialFactKeys.PurifiedCentrifugePhase("liquid", 234, 629)),
            new Phase(9791, "Honey", new MaterialFactKeys.PurifiedCentrifugePhase("liquid", 273, 373)),
            new Phase(9793, "Honeydew", new MaterialFactKeys.PurifiedCentrifugePhase("liquid", 273, 373)),
            new Phase(9802, "Ice", new MaterialFactKeys.PurifiedCentrifugePhase("liquid", 273, 373)));

    private static final List<None> NONE = List.of(
            new None(1120, "Copernicium", new MaterialFactKeys.PurifiedCentrifugeNoDefault("liquid", 150, 357)),
            new None(1180, "Oganesson", new MaterialFactKeys.PurifiedCentrifugeNoDefault("gas", 258, 263)),
            new None(9110, "Celestine", new MaterialFactKeys.PurifiedCentrifugeNoDefault("liquid", 275, 455)),
            new None(
                    9199, "DuraniumHexafluoride", new MaterialFactKeys.PurifiedCentrifugeNoDefault("liquid", 216, 428)),
            new None(9801, "Snow", new MaterialFactKeys.PurifiedCentrifugeNoDefault("liquid", 273, 373)),
            new None(9851, "OilSand", new MaterialFactKeys.PurifiedCentrifugeNoDefault("liquid", 100, 400)),
            new None(9852, "CrudeOil", new MaterialFactKeys.PurifiedCentrifugeNoDefault("liquid", 100, 400)));

    public static MaterialDefinition.Declaration apply(MaterialDefinition.Declaration declaration) {
        MaterialFactKeys.PurifiedCentrifugePhase phase = null;
        MaterialFactKeys.PurifiedCentrifugeNoDefault none = null;
        for (Phase value : PHASES)
            if (value.id == declaration.id() && value.name.equals(declaration.name())) phase = value.value;
        for (None value : NONE)
            if (value.id == declaration.id() && value.name.equals(declaration.name())) none = value.value;
        if (phase == null && none == null) return declaration;
        var info = declaration.info();
        var rules = new LinkedHashMap<>(info.processes().rules());
        if (phase != null)
            rules.put(MaterialFactKeys.SOURCE_PURIFIED_CENTRIFUGE_PHASE, List.of(MaterialProcessRule.anyForm(phase)));
        if (none != null)
            rules.put(
                    MaterialFactKeys.SOURCE_PURIFIED_CENTRIFUGE_NO_DEFAULT, List.of(MaterialProcessRule.anyForm(none)));
        return new MaterialDefinition.Declaration(
                declaration.material(),
                new MaterialInfoDefinition(
                        info.tooling(),
                        info.composition(),
                        info.relations(),
                        new MaterialInfoDefinition.Processes(rules)));
    }
}
