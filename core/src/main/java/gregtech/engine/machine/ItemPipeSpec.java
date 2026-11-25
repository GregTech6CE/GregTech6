package gregtech.engine.machine;

import gregtech.api.material.GTMaterial;

/** GT6 item pipe specification — one per material+size combination. */
public record ItemPipeSpec(
        String id,
        GTMaterial material,
        ItemPipeSize size,
        long stepSize,
        int invSize,
        float diameter,
        boolean restrictive,
        boolean blocking) {
    public enum ItemPipeSize {
        MEDIUM(1, 0.500F, false),
        LARGE(2, 0.750F, false),
        HUGE(4, 1.000F, false),
        RESTRICTIVE_MEDIUM(1, 0.500F, true),
        RESTRICTIVE_LARGE(2, 0.750F, true),
        RESTRICTIVE_HUGE(4, 1.000F, true);

        private final int scale;
        private final float diameter;
        private final boolean restrictive;

        ItemPipeSize(int scale, float diameter, boolean restrictive) {
            this.scale = scale;
            this.diameter = diameter;
            this.restrictive = restrictive;
        }

        public boolean restrictive() {
            return restrictive;
        }

        public String textureName() {
            return "pipe" + (restrictive() ? "_restrictive_" : "_") + name().toLowerCase();
        }

        public float diameter() {
            return diameter;
        }

        public int invSizeMultiplier() {
            return scale;
        }

        /** Routing cost, not travel time: restrictive variants multiply their same-size cost by 100. */
        public long stepSize(long baseStepSize) {
            return baseStepSize / scale * (restrictive ? 100 : 1);
        }
    }

    public int tintRgb() {
        return material.getColor();
    }

    public String materialName() {
        return material.getLocalName();
    }

    public float blastResistance() {
        if (material.hasToolStats()) {
            return Math.max(3.0F, material.getToolQuality() * 2.0F + material.getToolDurability() / 400.0F);
        }
        return 6.0F;
    }

    public static ItemPipeSpec of(
            String id, GTMaterial material, ItemPipeSize size, long baseStepSize, int baseInvSize, boolean blocking) {
        return new ItemPipeSpec(
                id,
                material,
                size,
                size.stepSize(baseStepSize),
                baseInvSize * size.invSizeMultiplier(),
                size.diameter(),
                size.restrictive(),
                blocking);
    }
}
