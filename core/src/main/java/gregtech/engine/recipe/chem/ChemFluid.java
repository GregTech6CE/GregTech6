package gregtech.engine.recipe.chem;

/**
 * One fluid-side input or output of an original chemistry row.
 *
 * <p>{@link MaterialFluid} keeps the source's material, phase and unit amount. The native
 * millibucket value is produced only when the binding is resolved. {@link NativeFluid} is for
 * source rows that already name a fluid and an explicit millibucket volume.</p>
 */
public sealed interface ChemFluid {
    record MaterialFluid(String material, FluidPhase phase, MaterialAmount units, Rounding rounding)
            implements ChemFluid {
        public MaterialFluid {
            if (material == null || material.isBlank() || phase == null || units == null || rounding == null)
                throw new IllegalArgumentException("Material fluid");
        }
    }

    record NativeFluid(String key, int millibuckets) implements ChemFluid {
        public NativeFluid {
            if (key == null || key.isBlank() || millibuckets <= 0) throw new IllegalArgumentException("Native fluid");
        }
    }

    record Water(MaterialAmount units, Rounding rounding) implements ChemFluid {
        public Water {
            if (units == null || rounding == null) throw new IllegalArgumentException("Water");
        }
    }
}
