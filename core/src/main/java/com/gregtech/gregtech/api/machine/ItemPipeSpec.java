package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/** GT6 item pipe specification — one per material+size combination. */
public record ItemPipeSpec(
        String id,
        GTMaterial material,
        ItemPipeSize size,
        long stepSize,
        int invSize,
        float diameter,
        boolean restrictive,
        boolean blocking
) {
    public enum ItemPipeSize {
        MEDIUM, LARGE, HUGE, RESTRICTIVE_MEDIUM, RESTRICTIVE_LARGE, RESTRICTIVE_HUGE;

        public boolean restrictive() {
            return this == RESTRICTIVE_MEDIUM || this == RESTRICTIVE_LARGE || this == RESTRICTIVE_HUGE;
        }

        public String textureName() {
            return "pipe" + (restrictive() ? "_restrictive_" : "_") + name().toLowerCase();
        }

        public float diameter() {
            return switch (this) {
                case MEDIUM, RESTRICTIVE_MEDIUM -> 0.500F;
                case LARGE, RESTRICTIVE_LARGE -> 0.750F;
                case HUGE, RESTRICTIVE_HUGE -> 1.000F;
                default -> 0.500F;
            };
        }

        public int invSizeMultiplier() {
            return switch (this) {
                case MEDIUM, RESTRICTIVE_MEDIUM -> 1;
                case LARGE, RESTRICTIVE_LARGE -> 2;
                case HUGE, RESTRICTIVE_HUGE -> 4;
                default -> 1;
            };
        }

        /** Step size for this variant relative to base. Restrictive pipes are 25-100x slower. */
        public long stepSize(long baseStepSize) {
            return switch (this) {
                case MEDIUM -> baseStepSize;
                case LARGE -> baseStepSize / 2;
                case HUGE -> baseStepSize / 4;
                case RESTRICTIVE_MEDIUM -> baseStepSize * 100;
                case RESTRICTIVE_LARGE -> baseStepSize / 2 * 100;
                case RESTRICTIVE_HUGE -> baseStepSize / 4 * 100;
                default -> baseStepSize;
            };
        }
    }

    public int tintRgb() { return material.getColor(); }
    public String materialName() { return material.getLocalName(); }

    public float blastResistance() {
        if (material.hasToolStats()) {
            return Math.max(3.0F, material.getToolQuality() * 2.0F + material.getToolDurability() / 400.0F);
        }
        return 6.0F;
    }

    public static ItemPipeSpec of(String id, GTMaterial material, ItemPipeSize size,
                                   long baseStepSize, int baseInvSize, boolean blocking) {
        return new ItemPipeSpec(id, material, size,
                size.stepSize(baseStepSize), baseInvSize * size.invSizeMultiplier(),
                size.diameter(), size.restrictive(), blocking);
    }
}
