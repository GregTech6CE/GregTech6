package gregtech.engine.recipe.chem;

/**
 * One item-side input or output of an original chemistry row.
 *
 * <p>{@link DustUnits}, {@link IngotUnits} and {@link GemUnits} keep the source's {@code OM.*}
 * material-amount call, including its form selection. The other variants name the physical
 * carrier explicitly. A prefix or a tag never decides the material amount.</p>
 */
public sealed interface ChemIngredient {
    record DustUnits(String material, MaterialAmount units) implements ChemIngredient {}

    record IngotUnits(String material, MaterialAmount units) implements ChemIngredient {}

    record IngotOrDustUnits(String material, MaterialAmount units) implements ChemIngredient {}

    record GemUnits(String material, MaterialAmount units) implements ChemIngredient {}

    record ItemForm(String prefix, String material, int count) implements ChemIngredient {
        public ItemForm {
            if (prefix == null || prefix.isBlank() || material == null || material.isBlank() || count <= 0)
                throw new IllegalArgumentException("Item form");
        }
    }

    record BlockForm(String prefix, String material, int count) implements ChemIngredient {
        public BlockForm {
            if (prefix == null || prefix.isBlank() || material == null || material.isBlank() || count <= 0)
                throw new IllegalArgumentException("Block form");
        }
    }

    record RegistryItem(String id, int count) implements ChemIngredient {
        public RegistryItem {
            if (id == null || !id.contains(":") || count <= 0) throw new IllegalArgumentException("Registry item");
        }
    }

    record FluidItem(String fluidKey, int count) implements ChemIngredient {
        public FluidItem {
            if (fluidKey == null || fluidKey.isBlank() || count <= 0) throw new IllegalArgumentException("Fluid item");
        }
    }

    record Selector(int index) implements ChemIngredient {
        public Selector {
            if (index < 0) throw new IllegalArgumentException("Selector index");
        }
    }
}
