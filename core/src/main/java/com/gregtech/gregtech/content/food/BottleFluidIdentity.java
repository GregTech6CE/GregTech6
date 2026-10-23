package com.gregtech.gregtech.content.food;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.FluidCatalog;
import com.gregtech.gregtech.data.ImportedMaterialData;

/** Resolve generated bottle expressions without confusing missing fluids with water. */
public final class BottleFluidIdentity {
    private BottleFluidIdentity() {}
    public static FluidCatalog.FluidEntry resolve(String key) {
        if (key == null || key.isBlank()) return null;
        var entry = FluidCatalog.get(key);
        if (entry != null) return entry;
        String path = FluidCatalog.sanitizePath(key);
        for (var candidate : FluidCatalog.all().values())
            if (FluidCatalog.sanitizePath(candidate.registryName()).equals(path)) return candidate;
        // MultiItemBottles also uses MT.<field>.liquid(), e.g. Hg rather than Mercury.
        GTMaterial material = GTMaterialRegistry.get(key).resolve();
        if (!material.isValid()) try {
            Object value = ImportedMaterialData.class.getField(key).get(null);
            if (value instanceof GTMaterial found) material = found.resolve();
        } catch (ReflectiveOperationException ignored) { }
        if (!material.isValid()) return null;
        // A bottle holds the ordinary liquid, not a metal's separate molten/plasma phase.
        return FluidCatalog.get("GenLiquid_" + material.getName());
    }
}
