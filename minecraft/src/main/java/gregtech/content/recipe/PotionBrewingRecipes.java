package gregtech.content.recipe;

import com.mojang.logging.LogUtils;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.stack.StackCompat;
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
            if (row.potion().isEmpty()
                    || row.ingredient().isEmpty()
                    || row.output().isEmpty()) continue;
            if (covered(seen, row)) continue;
            var recipe = new Recipe(
                    new ItemStack[] {
                        row.ingredient().copyWithCount(1), row.potion().copyWithCount(1)
                    },
                    new ItemStack[] {row.output().copy()},
                    null,
                    null,
                    null,
                    null,
                    DURATION,
                    EU,
                    0);
            recipe.mExactItemInputs = true;
            var added = map.addRecipe(recipe, false, false, false);
            if (added == null) continue;
            seen.add(row);
            GENERATED.add(added);
        }
        LOGGER.info("[gregtech] Fermenter potion rows: {}", GENERATED.size());
    }

    /** Removes duplicate stacks and normalizes the count to one. */
    public static List<ItemStack> distinct(Collection<ItemStack> stacks) {
        var result = new ArrayList<ItemStack>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            boolean duplicate = false;
            for (ItemStack existing : result) {
                if (StackCompat.sameItem(existing, stack)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) result.add(stack.copyWithCount(1));
        }
        return result;
    }

    private static boolean covered(List<Row> seen, Row row) {
        // The brewing stand returns the first matching mix; keep one row per input pair.
        for (var other : seen)
            if (StackCompat.sameItem(other.potion(), row.potion())
                    && StackCompat.sameItem(other.ingredient(), row.ingredient())) return true;
        return false;
    }
}
