package gregtech.engine.recipe.chem;

/**
 * One item-side input or output of an original generated recipe row.
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

    /** A technological item ({@code IL.*}) named by its source field or registered id. */
    record TechnologyItem(String name, int count) implements ChemIngredient {
        public TechnologyItem {
            if (name == null || name.isBlank() || count <= 0) throw new IllegalArgumentException("Technology item");
        }
    }

    /** A written book from the source's book loader, named by its manual title. */
    record Book(String name) implements ChemIngredient {
        public Book {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Book");
        }
    }

    /**
     * A physical block-of-dust input ({@code OM.blockDust}). The row is kept only when the named
     * material has a reviewed exact block; it is never projected onto nine loose dusts.
     */
    record CentrifugeBlockDust(String material, int count) implements ChemIngredient {
        public CentrifugeBlockDust {
            if (material == null || material.isBlank() || count <= 0) throw new IllegalArgumentException("Block dust");
        }
    }

    /**
     * A reviewed source output whose preferred target is a vanilla item while the material form
     * stays the documented fallback.
     */
    record SourceItem(String material, String prefix, int count) implements ChemIngredient {
        public SourceItem {
            if (material == null || material.isBlank() || prefix == null || prefix.isBlank() || count <= 0)
                throw new IllegalArgumentException("Source item");
        }
    }

    /**
     * An optional-mod tail output. The row is kept only when the reviewed optional holder can be
     * resolved; a deferring plan drops the row rather than inventing an item.
     */
    record OptionalOutput(String plan) implements ChemIngredient {
        public OptionalOutput {
            if (plan == null || plan.isBlank()) throw new IllegalArgumentException("Optional output");
        }
    }
}
