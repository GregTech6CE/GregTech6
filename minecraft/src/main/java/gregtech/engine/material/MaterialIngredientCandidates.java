package gregtech.engine.material;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A material can reach an ingredient through its explicit item and several equivalent tags. */
public final class MaterialIngredientCandidates {
    private MaterialIngredientCandidates() {}

    public static ItemStack[] unique(ItemStack[] candidates) {
        if (candidates.length < 2) return candidates;
        var byItem = new IdentityHashMap<Item, List<ItemStack>>();
        var result = new ArrayList<ItemStack>(candidates.length);
        for (var candidate : candidates) {
            var sameItem = byItem.computeIfAbsent(candidate.getItem(), ignored -> new ArrayList<>());
            boolean duplicate = false;
            for (var existing : sameItem) {
//? if >=1.21.1 {
                /*boolean sameData = ItemStack.isSameItemSameComponents(existing, candidate);
*///?} else {
                boolean sameData = ItemStack.isSameItemSameTags(existing, candidate);
//?}
                if (sameData && existing.getCount() == candidate.getCount()) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                sameItem.add(candidate);
                result.add(candidate);
            }
        }
        // Leave unrelated ingredients alone, including optional mods' custom representations.
        if (result.size() == candidates.length
                || result.stream().noneMatch(stack -> MaterialEquivalence.form(stack) != null)) return candidates;
        return result.toArray(ItemStack[]::new);
    }
}
