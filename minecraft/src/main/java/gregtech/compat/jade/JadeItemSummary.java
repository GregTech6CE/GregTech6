package gregtech.compat.jade;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.ITooltip;

/** Display totals remain separate from ItemStack counts and never mutate the inspected inventory. */
final class JadeItemSummary {
    private final List<Entry> entries = new ArrayList<>();

    void add(ItemStack stack) {
        if (stack.isEmpty()) return;
        for (var entry : entries) {
//? if >=1.21.1 {
            /*boolean same = ItemStack.isSameItemSameComponents(entry.icon, stack);
*///?} else {
            boolean same = ItemStack.isSameItemSameTags(entry.icon, stack);
//?}
            if (same) {
                entry.count += stack.getCount();
                return;
            }
        }
        entries.add(new Entry(stack));
    }

    void render(ITooltip tooltip) {
        for (var entry : entries) JadeText.item(tooltip, entry.icon, entry.count);
    }

    private static final class Entry {
        private final ItemStack icon;
        private long count;

        private Entry(ItemStack stack) {
            icon = stack.copyWithCount(1);
            count = stack.getCount();
        }
    }
}
