package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/** GT6 fluid pipe specification — one per material+size combination. */
public record PipeSpec(
        String id,
        GTMaterial material,
        PipeSize size,
        long capacity,
        int tankCount,
        float diameter,
        boolean gasProof,
        boolean acidProof,
        boolean plasmaProof,
        boolean magicProof,
        long maxTemperature,
        int flammability
) {
    /** Compatibility constructor for existing callers; old definitions had no fire behavior. */
    public PipeSpec(String id, GTMaterial material, PipeSize size, long capacity, int tankCount,
                    float diameter, boolean gasProof, boolean acidProof, boolean plasmaProof,
                    boolean magicProof, long maxTemperature) {
        this(id, material, size, capacity, tankCount, diameter, gasProof, acidProof,
                plasmaProof, magicProof, maxTemperature, 0);
    }

    public enum PipeSize {
        TINY, SMALL, MEDIUM, LARGE, HUGE, QUADRUPLE, NONUPLE;

        public String textureName() {
            return "pipe" + name().toLowerCase();
        }

        public long capacityMultiplier() {
            return switch (this) {
                case TINY -> 1;
                case SMALL -> 2;
                case MEDIUM -> 6;
                case LARGE -> 12;
                case HUGE -> 24;
                case QUADRUPLE -> 6;
                case NONUPLE -> 2;
            };
        }

        public float diameter() {
            return switch (this) {
                case TINY -> 0.250F;
                case SMALL -> 0.375F;
                case MEDIUM -> 0.500F;
                case LARGE -> 0.750F;
                case HUGE, QUADRUPLE, NONUPLE -> 1.000F;
            };
        }

        public int tankCount() {
            return switch (this) {
                case QUADRUPLE -> 4;
                case NONUPLE -> 9;
                default -> 1;
            };
        }
    }

    public int tintRgb() { return material.getColor(); }
    public String materialName() { return material.getLocalName(); }

    /** Blast resistance derived from material tool quality and durability. */
    public float blastResistance() {
        if (material.hasToolStats()) {
            return Math.max(3.0F, material.getToolQuality() * 2.0F + material.getToolDurability() / 400.0F);
        }
        return 6.0F;
    }

    public static PipeSpec of(String id, GTMaterial material, PipeSize size, long baseCapacity,
                              boolean gasProof, boolean acidProof, boolean plasmaProof, boolean magicProof) {
        return of(id, material, size, baseCapacity, gasProof, acidProof, plasmaProof, magicProof,
                (long)(material.getMeltingPoint() * 1.25D));
    }

    /** Explicit GT6 wood/plastic/rubber limits must survive registration and serialization. */
    public static PipeSpec of(String id, GTMaterial material, PipeSize size, long baseCapacity,
                              boolean gasProof, boolean acidProof, boolean plasmaProof, boolean magicProof,
                              long maxTemperature) {
        return of(id, material, size, baseCapacity, gasProof, acidProof, plasmaProof, magicProof, maxTemperature, 0);
    }

    public static PipeSpec of(String id, GTMaterial material, PipeSize size, long baseCapacity,
                              boolean gasProof, boolean acidProof, boolean plasmaProof, boolean magicProof,
                              long maxTemperature, int flammability) {
        return new PipeSpec(id, material, size,
                baseCapacity * size.capacityMultiplier(), size.tankCount(), size.diameter(),
                gasProof, acidProof, plasmaProof, magicProof,
                maxTemperature, flammability);
    }
}
