package gregtech.content.material;

import gregtech.content.material.catalog.*;
import gregtech.engine.material.MaterialDefinition;
import gregtech.engine.material.MaterialInfoDefinition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure, ordered canonical declarations. Loading this directory never installs runtime materials. */
public final class MaterialDefinitions {
    private MaterialDefinitions() {}

    private static final List<MaterialDefinition.Declaration> DECLARATIONS = collect();
    private static final List<MaterialDefinition> ALL =
            DECLARATIONS.stream().map(MaterialDefinition.Declaration::material).toList();
    private static final Map<String, MaterialDefinition> BY_NAME = index();
    private static final Map<String, MaterialInfoDefinition> INFO = indexInfo();

    private static List<MaterialDefinition.Declaration> collect() {
        var result = new ArrayList<MaterialDefinition.Declaration>();
        ElementMaterials.definitions().stream().map(MaterialPhaseFlags::apply).forEach(result::add);
        CompoundMaterials.definitions().stream().map(MaterialPhaseFlags::apply).forEach(result::add);
        OreMaterials.definitions().stream().map(MaterialPhaseFlags::apply).forEach(result::add);
        StoneMaterials.definitions().stream().map(MaterialPhaseFlags::apply).forEach(result::add);
        WoodMaterials.definitions().stream().map(MaterialPhaseFlags::apply).forEach(result::add);
        SpecialMaterials.definitions().stream().map(MaterialPhaseFlags::apply).forEach(result::add);
        GroupMaterials.definitions().stream().map(MaterialPhaseFlags::apply).forEach(result::add);
        result.sort(Comparator.comparing(MaterialDefinition.Declaration::name));
        return List.copyOf(result);
    }

    private static Map<String, MaterialDefinition> index() {
        var result = new LinkedHashMap<String, MaterialDefinition>();
        var ids = new HashSet<Integer>();
        for (var definition : ALL) {
            if (result.putIfAbsent(definition.name(), definition) != null)
                throw new IllegalArgumentException("Duplicate canonical declaration: " + definition.name());
            if (definition.id() > 0 && !ids.add(definition.id()))
                throw new IllegalArgumentException("Duplicate canonical ID: " + definition.id());
        }
        for (var definition : ALL) {
            for (var alias : definition.aliases()) {
                if (result.putIfAbsent(alias, definition) != null)
                    throw new IllegalArgumentException(
                            "Conflicting catalog alias: " + alias + " -> " + definition.name());
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, MaterialInfoDefinition> indexInfo() {
        var result = new LinkedHashMap<String, MaterialInfoDefinition>();
        for (var declaration : DECLARATIONS) result.put(declaration.name(), declaration.info());
        return Map.copyOf(result);
    }

    public static List<MaterialDefinition> all() {
        return ALL;
    }

    public static List<MaterialDefinition.Declaration> declarations() {
        return DECLARATIONS;
    }

    public static MaterialDefinition find(String name) {
        return BY_NAME.get(name);
    }

    public static MaterialInfoDefinition info(String name) {
        return INFO.getOrDefault(name, MaterialInfoDefinition.empty());
    }

    /** Recipe-family snapshots remain explicit, after all built-in facts and before addon callbacks. */
    public static void captureDefaults() {
        SourceLatheMaterialProfiles.captureDefaults();
    }
}
