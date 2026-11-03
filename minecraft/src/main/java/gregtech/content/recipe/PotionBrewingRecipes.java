package gregtech.content.recipe;

import com.mojang.logging.LogUtils;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.engine.recipe.Recipe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Runtime fermenter rows for the probed vanilla brewing table. */
public final class PotionBrewingRecipes {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final List<Recipe> GENERATED = new ArrayList<>();
    private static final int DURATION = 16;
    private static final long EU = 16;

    public record Row(ItemStack potion, ItemStack ingredient, ItemStack output) {}

    private PotionBrewingRecipes() {}

    /** Replaces the previous probe result; safe to call on every brewing table rebuild. */
    public static synchronized void rebuild(Collection<Row> rows) {
        var map = MachineRecipeMaps.Fermenter;
        if (!GENERATED.isEmpty()) map.mRecipeList.removeAll(GENERATED);
        GENERATED.clear();
        var seen = new ArrayList<Row>();
        for (Row row : rows) {
            if (row.potion().isEmpty() || row.ingredient().isEmpty() || row.output().isEmpty()) continue;
            if (duplicate(seen, row)) continue;
            seen.add(row);
            var recipe = new Recipe(
                    new ItemStack[] {row.ingredient().copyWithCount(1), row.potion().copyWithCount(1)},
                    new ItemStack[] {row.output().copy()}, null, null, null, null, DURATION, EU, 0);
            recipe.mExactItemInputs = true;
            var added = map.addRecipe(recipe, false, false, false);
            if (added != null) GENERATED.add(added);
        }
        LOGGER.info("[gregtech] Fermenter potion rows: {}", GENERATED.size());
    }

    private static boolean duplicate(List<Row> seen, Row row) {
        for (var other : seen)
            if (same(other.potion(), row.potion())
                    && same(other.ingredient(), row.ingredient())
                    && same(other.output(), row.output())) return true;
        return false;
    }

    private static boolean same(ItemStack left, ItemStack right) {
//? if >=1.21.1 {
        /*return ItemStack.isSameItemSameComponents(left, right);
*///?} else {
        return ItemStack.isSameItemSameTags(left, right);
//?}
    }
}
