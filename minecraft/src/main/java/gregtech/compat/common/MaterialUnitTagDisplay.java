package gregtech.compat.common;

import gregtech.engine.recipe.RecipeMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Shared, live tag membership for unit navigation in recipe viewers. */
public final class MaterialUnitTagDisplay {
    private MaterialUnitTagDisplay() {}

    public record Entry(String family, TagKey<Item> tag, ItemStack unit, List<ItemStack> members) {}

    public static List<Entry> entries() {
        var entries = new LinkedHashMap<TagKey<Item>, Entry>();
        for (var map : RecipeMap.RECIPE_MAP_LIST) {
            if (!map.mViewerAllowed) continue;
            for (var row : map.mRecipeList) {
                if (!row.mEnabled || row.mHidden) continue;
                for (int i = 0; i < row.mInputs.length; i++) {
                    var display = row.materialInputDisplay(i);
                    if (display == null || display.tag() == null || entries.containsKey(display.tag())) continue;
                    var tag = display.tag();
                    String family = tag.location().getPath().split("/", 2)[0];
                    if (!family.equals("smeltable") && !family.equals("extrudable")) continue;
                    var members = new ArrayList<ItemStack>();
                    BuiltInRegistries.ITEM
                            .getTag(tag)
                            .ifPresent(
                                    holders -> holders.forEach(holder -> members.add(new ItemStack(holder.value()))));
                    if (!members.isEmpty())
                        entries.put(tag, new Entry(family, tag, row.mInputs[i].copyWithCount(1), List.copyOf(members)));
                }
            }
        }
        return List.copyOf(entries.values());
    }
}
