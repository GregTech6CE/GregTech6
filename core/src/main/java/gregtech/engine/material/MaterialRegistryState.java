package gregtech.engine.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialSentinels;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One installation core, also usable in isolated rule tests without resetting production state. */
final class MaterialRegistryState {
    enum Mode {
        SENTINEL,
        BUILTIN,
        GROUP,
        ADDON
    }

    final Map<String, GTMaterial> names = new LinkedHashMap<>();
    final Map<Integer, GTMaterial> ids = new LinkedHashMap<>();
    final Set<GTMaterial> materials = new LinkedHashSet<>();

    GTMaterial install(GTMaterial material, Mode mode, List<String> aliases) {
        String key = material.getName();
        int id = material.getId();
        if (key.isBlank() || !GTMaterial.sanitize(key).equals(key))
            throw new IllegalArgumentException("Invalid canonical material name: " + key);
        boolean validId = switch (mode) {
            case SENTINEL -> material == MaterialSentinels.INVALID || material == MaterialSentinels.EMPTY;
            case BUILTIN -> id > 0 && id < GTMaterialRegistry.ADDON_ID_START;
            case GROUP -> id == GTMaterial.INVALID_ID;
            case ADDON -> id >= GTMaterialRegistry.ADDON_ID_START;
        };
        if (!validId) throw new IllegalArgumentException("Invalid " + mode + " material id: " + key + " / " + id);
        if (names.containsKey(key)) throw new IllegalArgumentException("Material name already registered: " + key);
        if ((id > 0 || mode == Mode.SENTINEL) && ids.containsKey(id))
            throw new IllegalArgumentException(
                    "Material ID " + id + " already used by " + ids.get(id).getName() + "; cannot register " + key);
        for (String alias : aliases) {
            String normalized = requireAlias(alias);
            if (names.containsKey(normalized))
                throw new IllegalArgumentException("Material alias already registered: " + normalized + " -> " + key);
        }
        // No indexes change before every identity and alias check succeeds.
        names.put(key, material);
        if (id > 0 || mode == Mode.SENTINEL) ids.put(id, material);
        materials.add(material);
        for (String alias : aliases) names.put(GTMaterial.sanitize(alias), material);
        return material;
    }

    void alias(String alias, String target) {
        String key = requireAlias(alias);
        GTMaterial material = names.get(GTMaterial.sanitize(target));
        if (material == null) throw new IllegalArgumentException("Unknown alias target: " + alias + " -> " + target);
        GTMaterial previous = names.get(key);
        if (previous != null && previous != material)
            throw new IllegalArgumentException(
                    "Conflicting material alias: " + key + " -> " + previous.getName() + " / " + material.getName());
        names.put(key, material);
    }

    private static String requireAlias(String alias) {
        String key = GTMaterial.sanitize(java.util.Objects.requireNonNull(alias, "alias"));
        if (key.isBlank()) throw new IllegalArgumentException("Empty material alias");
        return key;
    }

    Collection<GTMaterial> all() {
        return Collections.unmodifiableSet(materials);
    }
}
