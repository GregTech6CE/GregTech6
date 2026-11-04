package gregtech.engine.machine;

import gregtech.api.material.GTMaterial;

/** Static hopper/queuehopper parameters (GT6 {@code MultiTileEntityRegistry.add} NBT fields). */
public record HopperSpec(
        String id,
        GTMaterial material,
        int slotCount,
        float hardness,
        float blastResistance,
        int tint,
        boolean vacuum) {
    public HopperSpec {
        if (slotCount < 1) throw new IllegalArgumentException("slotCount must be >= 1");
    }

    /** Material colour unless {@link #tint()} overrides it. */
    public int tintRgb() {
        return tint != 0 ? tint : material.getColor();
    }

    public String materialName() {
        return material.getLocalName();
    }

    public static HopperSpec of(String id, GTMaterial material, int slotCount, float hardness, float blastResistance) {
        return new HopperSpec(id, material, slotCount, hardness, blastResistance, 0, false);
    }

    public static HopperSpec vacuum(
            String id, GTMaterial material, int slotCount, float hardness, float blastResistance, int tint) {
        return new HopperSpec(id, material, slotCount, hardness, blastResistance, tint, true);
    }
}
