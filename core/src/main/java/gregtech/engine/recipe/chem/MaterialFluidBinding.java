package gregtech.engine.recipe.chem;

import gregtech.content.data.FluidCatalog;
import gregtech.content.recipe.OriginalMaterialFluidUnits;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The registered fluid that carries a material's phase, and the source-unit reference that
 * converts material units to millibuckets. Built from the existing fluid registry and the
 * reviewed source bindings; it does not register or rename fluids.
 */
public record MaterialFluidBinding(
        String material,
        FluidPhase phase,
        String field,
        String registryName,
        long referenceUnits,
        int referenceMillibuckets) {

    public int millibuckets(MaterialAmount amount, Rounding rounding) {
        int value = OriginalMaterialFluidUnits.millibuckets(
                amount.units(), referenceUnits, referenceMillibuckets, rounding == Rounding.CEIL);
        if (value <= 0) throw new IllegalArgumentException("Fluid amount out of range: " + amount);
        return value;
    }

    private record Key(String material, FluidPhase phase) {}

    private static volatile Map<Key, MaterialFluidBinding> INDEX;

    public static Optional<MaterialFluidBinding> find(String material, FluidPhase phase) {
        return Optional.ofNullable(index().get(new Key(material, phase)));
    }

    /**
     * Resolves the requested phase, or the material's only registered phase when the source's
     * liquid/gas label differs from the port's registration. The substitution is reported to the
     * caller instead of being silent.
     */
    public record Resolved(MaterialFluidBinding binding, boolean phaseSubstituted) {}

    public static Optional<Resolved> resolve(String material, FluidPhase phase) {
        var map = index();
        var exact = map.get(new Key(material, phase));
        if (exact != null) return Optional.of(new Resolved(exact, false));
        MaterialFluidBinding only = null;
        for (var entry : map.entrySet()) {
            if (!entry.getKey().material.equals(material)) continue;
            if (only != null) return Optional.empty();
            only = entry.getValue();
        }
        return only == null ? Optional.empty() : Optional.of(new Resolved(only, true));
    }

    /** Rebuilds the index after the material fluid registry has been finalized. */
    public static synchronized void invalidate() {
        INDEX = null;
    }

    public static Map<String, MaterialFluidBinding> snapshot() {
        var out = new LinkedHashMap<String, MaterialFluidBinding>();
        for (var entry : index().entrySet())
            out.put(entry.getKey().material + "/" + entry.getKey().phase, entry.getValue());
        return Map.copyOf(out);
    }

    private static Map<Key, MaterialFluidBinding> index() {
        var local = INDEX;
        if (local == null) {
            synchronized (MaterialFluidBinding.class) {
                if (INDEX == null) INDEX = build();
                local = INDEX;
            }
        }
        return local;
    }

    private static Map<Key, MaterialFluidBinding> build() {
        var out = new LinkedHashMap<Key, MaterialFluidBinding>();
        for (var entry : FluidCatalog.all().entrySet()) {
            String field = entry.getKey();
            FluidCatalog.FluidEntry fluid = entry.getValue();
            String materialKey = fluid.materialKey();
            if (materialKey == null || materialKey.isBlank())
                materialKey = FluidCatalog.boundMaterial(fluid.registryName());
            if (materialKey == null || materialKey.isBlank()) continue;
            FluidPhase phase = fluid.gas() ? FluidPhase.GAS : FluidPhase.LIQUID;
            int reference = reference(materialKey, field, fluid, phase);
            var binding = new MaterialFluidBinding(
                    materialKey, phase, field, fluid.registryName(), OriginalMaterialFluidUnits.SOURCE_UNIT, reference);
            var key = new Key(binding.material(), binding.phase());
            var previous = out.putIfAbsent(key, binding);
            if (previous != null && previous.referenceMillibuckets() != reference)
                throw new IllegalStateException("Conflicting material fluid bindings for " + key.material() + "/"
                        + key.phase() + ": " + previous.field() + " and " + field);
        }
        return Map.copyOf(out);
    }

    private static int reference(String material, String field, FluidCatalog.FluidEntry entry, FluidPhase phase) {
        var explicit = OriginalMaterialFluidUnits.binding(material, phase.sourceName());
        if (explicit != null) {
            long millibuckets = explicit.referenceMillibuckets();
            if (millibuckets <= 0 || millibuckets > Integer.MAX_VALUE)
                throw new IllegalStateException("Invalid material fluid reference for " + material + "/" + phase);
            return (int) millibuckets;
        }
        if (entry.textureMode() == FluidCatalog.FluidTextureMode.GENERIC_MOLTEN) return 144;
        if (FluidCatalog.isGeneratedMoltenDefinition(field, entry)) return 144;
        // Generated and dedicated gas/liquid material fluids use the original 1000 mB/U unit.
        return 1000;
    }
}
