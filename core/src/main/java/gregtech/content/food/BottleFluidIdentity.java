package gregtech.content.food;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.FluidCatalog;
import gregtech.engine.material.GTMaterialRegistry;

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
        // GT6 bottle rows name materials by their short field symbol (e.g. Hg); those symbols
        // are registered as aliases next to the canonical material name.
        GTMaterial material = GTMaterialRegistry.get(key);
        if (!material.isValid()) return null;
        // A bottle holds the ordinary liquid, not a metal's separate molten/plasma phase.
        return FluidCatalog.get("GenLiquid_" + material.getName());
    }
}
