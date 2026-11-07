package gregtech.content.recipe;

import com.mojang.logging.LogUtils;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.stack.StackCompat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/** Runtime fermenter rows for the probed vanilla brewing table. */
public final class PotionBrewingRecipes {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final List<Recipe> GENERATED = new ArrayList<>();
    private static List<Row> STAGED = List.of();
    private static final int DURATION = 16;
    private static final long EU = 16;

    public record Row(ItemStack potion, ItemStack ingredient, ItemStack output) {}

    private PotionBrewingRecipes() {}

    /** Stages and publishes the probe result; the staged rows survive an overlay baseline restore. */
    public static synchronized void rebuild(Collection<Row> rows) {
        STAGED = List.copyOf(rows);
        rebuildStaged();
    }

    /** Re-publishes the last staged probe result after the overlay restores its baseline. */
    public static synchronized void rebuildStaged() {
        var map = MachineRecipeMaps.Fermenter;
        map.removeAll(GENERATED);
        GENERATED.clear();
        var seen = new ArrayList<Row>();
        for (Row row : STAGED) {
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

    /** Rows this producer currently owns; excluded from the overlay baseline. */
    public static synchronized Set<Recipe> owned() {
        return Set.copyOf(GENERATED);
    }

    /**
     * Drops the published probe rows on server stop. The staged probe result survives: on Forge the
     * brewing table is probed once per process, so the next world must be able to republish it.
     */
    public static synchronized void clear() {
        MachineRecipeMaps.Fermenter.removeAll(GENERATED);
        GENERATED.clear();
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
