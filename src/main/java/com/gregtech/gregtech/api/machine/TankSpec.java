package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/** GT6 fluid container specification. */
public record TankSpec(
        String id,
        GTMaterial material,
        TankType type,
        long capacity,
        boolean gasProof,
        boolean acidProof,
        boolean plasmaProof,
        boolean magicProof,
        boolean simpleOnly,
        float hardness,
        float blastResistance,
        long maxTemperature
) {
    public enum TankType {
        WOOD_BARREL,
        PLASTIC_CANISTER,
        METAL_DRUM,
        LOGISTICS_BARREL;

        public String langKey() {
            return switch (this) {
                case WOOD_BARREL -> "gt.tank.wood_barrel";
                case PLASTIC_CANISTER -> "gt.tank.plastic_canister";
                case METAL_DRUM -> "gt.tank.metal_drum";
                case LOGISTICS_BARREL -> "gt.tank.logistics_barrel";
            };
        }
    }

    public int tintRgb() { return material.getColor(); }
    public String materialName() { return material.getLocalName(); }

    /** GT6's explicit {@code NBT_CAPACITY_HU} overrides the barrel's melting point in kelvin. */
    public TankSpec withMaxTemperature(long kelvin) {
        return new TankSpec(id, material, type, capacity, gasProof, acidProof, plasmaProof,
                magicProof, simpleOnly, hardness, blastResistance, kelvin);
    }

    public static TankSpec of(String id, GTMaterial material, TankType type, long capacity,
                              boolean gasProof, boolean acidProof, boolean plasmaProof, boolean magicProof,
                              boolean simpleOnly, float hardness, float blastResistance) {
        return new TankSpec(id, material, type, capacity,
                gasProof, acidProof, plasmaProof, magicProof, simpleOnly,
                hardness, blastResistance,
                Math.round(material.getMeltingPoint() * 1.25D));
    }
}
