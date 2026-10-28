package gregtech.content.block.misc;

import net.minecraft.world.item.DyeColor;

/** Small color-bound leaves retain each source block's actual behavior and slab placement. */
public final class FixedConstructionBlocks {
    private FixedConstructionBlocks() {}

    public static final class Asphalt extends AsphaltBlock implements FixedConstructionColor {
        private final DyeColor color;

        public Asphalt(DyeColor color, Properties properties) {
            super(properties);
            this.color = color;
        }

        public DyeColor color() {
            return color;
        }

        public String family() {
            return "asphalt";
        }
    }

    public static final class Concrete extends ConcreteBlock implements FixedConstructionColor {
        private final DyeColor color;

        public Concrete(DyeColor color, boolean reinforced, Properties properties) {
            super(reinforced, properties);
            this.color = color;
        }

        public DyeColor color() {
            return color;
        }

        public String family() {
            return reinforced() ? "concrete_reinforced" : "concrete";
        }
    }

    public static final class Foam extends CFoamBlock implements FixedConstructionColor {
        private final DyeColor color;

        public Foam(DyeColor color, Properties properties) {
            super(properties);
            this.color = color;
        }

        public DyeColor color() {
            return color;
        }

        public String family() {
            return "cfoam";
        }
    }

    public static final class WetFoam extends FreshCFoamBlock implements FixedConstructionColor {
        private final DyeColor color;

        public WetFoam(DyeColor color, Properties properties) {
            super(properties);
            this.color = color;
        }

        public DyeColor color() {
            return color;
        }

        public String family() {
            return "cfoam_fresh";
        }
    }

    public static final class FoamSlab extends CFoamSlabBlock implements FixedConstructionColor {
        private final DyeColor color;

        public FoamSlab(DyeColor color, Properties properties) {
            super(properties);
            this.color = color;
        }

        public DyeColor color() {
            return color;
        }

        public String family() {
            return "cfoam_slab";
        }
    }

    public static final class Glass extends ColoredGlassBlock implements FixedConstructionColor {
        private final DyeColor color;

        public Glass(DyeColor color, boolean glow, Properties properties) {
            super(glow, properties);
            this.color = color;
        }

        public DyeColor color() {
            return color;
        }

        public String family() {
            return glow() ? "glass_glow" : "glass_clear";
        }
    }

    public static final class GlassSlab extends ColoredGlassSlabBlock implements FixedConstructionColor {
        private final DyeColor color;
        private final boolean glow;

        public GlassSlab(DyeColor color, boolean glow, Properties properties) {
            super(properties);
            this.color = color;
            this.glow = glow;
        }

        public DyeColor color() {
            return color;
        }

        public String family() {
            return glow ? "glass_glow_slab" : "glass_clear_slab";
        }
    }
}
