package gregtech.content.tool;

import net.minecraft.world.item.ItemStack;

/** Empty recipe entries represent GT6's explicit empty-workpiece marker. */
public final class AnvilWorkInputs {
    private AnvilWorkInputs() {}

    public static ItemStack[] consume(ItemStack[] work, ItemStack[] inputs) {
        return consume(
                work, inputs, (input, stack) -> gregtech.engine.recipe.RecipeInputs.matches(inputs[input], stack));
    }

    public static ItemStack[] consume(ItemStack[] work, gregtech.engine.recipe.Recipe recipe) {
        return consume(work, recipe.mInputs, recipe::matchesItemInput);
    }

    private static ItemStack[] consume(
            ItemStack[] work, ItemStack[] inputs, java.util.function.BiPredicate<Integer, ItemStack> accepts) {
        if (inputs.length > work.length) return null;
        var slots = new int[inputs.length];
        if (!assign(work, inputs, accepts, slots, new boolean[work.length], 0)) return null;
        var result = java.util.Arrays.stream(work).map(ItemStack::copy).toArray(ItemStack[]::new);
        for (int i = 0; i < inputs.length; i++)
            if (inputs[i] != null && !inputs[i].isEmpty()) result[slots[i]].shrink(inputs[i].getCount());
        return result;
    }

    /** Each workpiece occupies its own side; mirrored recipes still match. */
    private static boolean assign(
            ItemStack[] work,
            ItemStack[] inputs,
            java.util.function.BiPredicate<Integer, ItemStack> accepts,
            int[] slots,
            boolean[] used,
            int input) {
        if (input == inputs.length) return true;
        var needed = inputs[input];
        for (int slot = 0; slot < work.length; slot++) {
            if (used[slot]) continue;
            boolean matches = needed == null || needed.isEmpty()
                    ? work[slot].isEmpty()
                    : work[slot].getCount() >= needed.getCount() && accepts.test(input, work[slot]);
            if (!matches) continue;
            slots[input] = slot;
            used[slot] = true;
            if (assign(work, inputs, accepts, slots, used, input + 1)) return true;
            used[slot] = false;
        }
        return false;
    }
}
