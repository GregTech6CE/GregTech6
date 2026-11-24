package gregtech.content.logistics;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** A logistics dump reservation from template items or one inferred prefix. */
public final class LogisticsItemFilter {
    private record Template(ItemStack stack, boolean ignoreItemData) {}

    private final List<Template> items;
    private final String prefix;

    private LogisticsItemFilter(List<Template> items, String prefix) {
        this.items = items;
        this.prefix = prefix;
    }

    /** Snapshot the whitelist and its per-slot modes for one logistics routing pass. */
    public static LogisticsItemFilter items(Container templates, BitSet ignoreItemData) {
        List<Template> copies = new ArrayList<>();
        for (int slot = 0; slot < templates.getContainerSize(); slot++) {
            ItemStack stack = templates.getItem(slot);
            if (!stack.isEmpty()) copies.add(new Template(stack.copyWithCount(1), ignoreItemData.get(slot)));
        }
        return new LogisticsItemFilter(List.copyOf(copies), null);
    }

    /** Uses the same single-prefix adapter as the prefix filter block. */
    public static LogisticsItemFilter prefix(String selectedPrefix) {
        return new LogisticsItemFilter(List.of(), selectedPrefix);
    }

    public boolean matches(ItemStack candidate) {
        if (candidate.isEmpty()) return false;
        if (prefix != null) return prefix.equals(FilterRules.prefix(candidate));
        for (Template template : items)
            if (FilterRules.itemMatches(template.stack(), candidate, template.ignoreItemData())) return true;
        return false;
    }
}
