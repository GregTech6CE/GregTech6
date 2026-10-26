package gregtech.content.logistics;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** A logistics dump reservation from template items or one inferred prefix. */
public final class LogisticsItemFilter {
    private final List<ItemStack> items;
    private final String prefix;

    private LogisticsItemFilter(List<ItemStack> items, String prefix) {
        this.items = items;
        this.prefix = prefix;
    }

    /** The ordinary Filter exposes its ghost slots as an ItemStackSet, even when empty. */
    public static LogisticsItemFilter items(List<ItemStack> templates) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack template : templates)
            if (!template.isEmpty()) copies.add(template.copyWithCount(1));
        return new LogisticsItemFilter(List.copyOf(copies), null);
    }

    /** Uses the same single-prefix adapter as the prefix filter block. */
    public static LogisticsItemFilter prefix(String selectedPrefix) {
        return new LogisticsItemFilter(List.of(), selectedPrefix);
    }

    public boolean matches(ItemStack candidate) {
        if (candidate.isEmpty())
            return false;
        if (prefix != null)
            return prefix.equals(FilterRules.prefix(candidate));
        for (ItemStack template : items)
            if (LogisticsCoverInteraction.matches(template, candidate))
                return true;
        return false;
    }
}
