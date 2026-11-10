package gregtech.content.data.generated;

import gregtech.content.food.*;
import gregtech.platform.Mc;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Platform binding of the shared radiation catalog. */
public final class GTFoodStatsGen {

    private GTFoodStatsGen() {}

    private static final List<String> MISSING_IDS = new ArrayList<>();
    public static final List<GTFoodStats.Row> ROWS = build();
    public static final List<String> MISSING = List.copyOf(MISSING_IDS);

    private static List<GTFoodStats.Row> build() {
        List<GTFoodStats.Row> rows = new ArrayList<>();
        for (var entry : FoodStatisticRows.ROWS) {
            Item item = resolveItem(entry.itemId());
            if (item == null || item == Items.AIR) {
                MISSING_IDS.add(entry.itemId());
                continue;
            }
            rows.add(new GTFoodStats.Row(item, entry.radiation()));
        }
        return List.copyOf(rows);
    }

    private static Item resolveItem(String itemId) {
        return Mc.item(ResourceLocation.parse(itemId));
    }
}
