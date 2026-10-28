package gregtech.content.material;

import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.content.food.GTFoodItems;
import gregtech.content.food.MeatCutCatalog;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemMaterialRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Bind cut compositions before recovery recipes inspect the material registry. */
public final class MeatItemMaterials {
    private static final Set<Item> ITEMS = new HashSet<>();

    private MeatItemMaterials() {}

    public static boolean contains(Item item) {
        return ITEMS.contains(item);
    }

    public static void register() {
        for (var cut : MeatCutCatalog.CUTS) {
            bind(cut.raw(), "MeatRaw", cut.meat(), cut.bone());
            bind(cut.cooked(), "MeatCooked", cut.meat(), cut.bone());
        }
        bind("barbecue_ribs", "MeatCooked", 2 * GTValues.U, GTValues.U);
    }

    private static void bind(String id, String material, long meat, long bone) {
        var item = GTFoodItems.itemOrNull(id);
        if (item == null || item == Items.AIR) throw new IllegalStateException("Missing meat cut: " + id);
        var parts = new ArrayList<MaterialComponent>();
        parts.add(MaterialComponent.of(GTMaterialRegistry.get(material), meat));
        if (bone > 0) parts.add(MaterialComponent.of(GTMaterialRegistry.get("Bone"), bone));
        ItemMaterialRegistry.register(item, new ItemComposition(null, parts, "GT6 MultiItemFood meat cuts", true));
        ITEMS.add(item);
    }
}
