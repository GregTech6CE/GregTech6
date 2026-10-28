package gregtech.engine.fluid;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;

/** Material-U/mB conversion at crucible fluid boundaries; saved values are never rescaled. */
public final class MoltenMaterialUnits {
    private MoltenMaterialUnits() {}

    public static final Ratio STANDARD = new Ratio(144);
    // GT6 Loader_Fluids:214, 3703e403: createMolten(MT.Al2O3, 504).
    public static final Ratio ALUMINA = new Ratio(504);
    // GT6 Loader_Fluids:191: Plastic retains its 1000 mB per material U binding.
    public static final Ratio PLASTIC = new Ratio(1000);

    public record Ratio(int millibucketsPerUnit) {
        public Ratio {
            if (millibucketsPerUnit <= 0 || GTValues.U % millibucketsPerUnit != 0)
                throw new IllegalArgumentException("Non-integral molten material ratio: " + millibucketsPerUnit);
        }

        public long unitsPerMillibucket() {
            return GTValues.U / millibucketsPerUnit;
        }

        public long toUnits(int millibuckets) {
            return millibuckets <= 0 ? 0 : Math.multiplyExact((long) millibuckets, unitsPerMillibucket());
        }
        /** Whole mB only: retain sub-mB material in its existing U storage. */
        public int millibuckets(long units) {
            return units <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, units / unitsPerMillibucket());
        }
    }

    /** Direct material APIs represent the canonical primary molten phase. */
    public static Ratio ratio(GTMaterial material) {
        return ratio(material, true);
    }

    /**
     * Fluid adapters must verify the built-in Alumina (primary or Al2O3 alias) or Plastic binding before passing true.
     * An alternate addon fluid of the same material retains its existing 144 behavior.
     */
    public static Ratio ratio(GTMaterial material, boolean verifiedCanonicalPhase) {
        if (!verifiedCanonicalPhase
                || material == null
                || !material.isValid()
                || material.resolve() != material
                || GTMaterialRegistry.get(material.getId()) != material
                || GTMaterialRegistry.get(material.getName()) != material
                || AddonMaterials.owns(material)) return STANDARD;
        if (material.getId() == 8008 && material.getName().equals("Alumina")) return ALUMINA;
        if (material.getId() == 8218 && material.getName().equals("Plastic")) return PLASTIC;
        return STANDARD;
    }
}
