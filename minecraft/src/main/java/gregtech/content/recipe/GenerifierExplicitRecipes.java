package gregtech.content.recipe;

/**
 * The pinned source registers no explicit {@code RM.generify} row for the port's dye conversions.
 * Dye equivalence is an ore-dictionary re-registration ({@code dust<Color> -> dyeMixable<Color> ->
 * dye<Color>}, {@code LoaderOreDictReRegistrations:540-570}), which the port models as item tags in
 * {@link gregtech.content.data.MaterialTagPack}. The former material-machine conversions here had no
 * source row and are no longer published; the bootstrap entry stays so the loader chain is unchanged.
 */
public final class GenerifierExplicitRecipes {
    private GenerifierExplicitRecipes() {}

    public static int register() {
        return 0;
    }
}
