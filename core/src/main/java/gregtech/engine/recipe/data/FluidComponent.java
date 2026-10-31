package gregtech.engine.recipe.data;

/** One fluid identity or input tag in millibuckets. Zero denotes a presence requirement. */
public record FluidComponent(String fluid, String tag, long amount) {
    public FluidComponent(String fluid, long amount) {
        this(fluid, null, amount);
    }

    public FluidComponent {
        if ((fluid == null) == (tag == null) || fluid != null && fluid.isBlank() || tag != null && tag.isBlank())
            throw new IllegalArgumentException("fluid component needs exactly one of fluid or tag");
        if (amount < 0) throw new IllegalArgumentException("amount");
    }
}
