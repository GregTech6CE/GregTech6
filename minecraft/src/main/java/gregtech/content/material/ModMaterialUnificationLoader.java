package gregtech.content.material;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.ModMaterialUnification;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Binds another mod's item to the GT material form it stands for (GT6 OreDictionary unification). */
public final class ModMaterialUnificationLoader {
    private ModMaterialUnificationLoader() {}

    public static void register() {
        for (var form : ModMaterialUnification.ITEM_FORMS) {
            ResourceLocation id = ResourceLocation.tryParse(form.itemId());
            if (id == null) continue;
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR) continue;
            MaterialPrefix prefix = gregtech.engine.prefix.PrefixRegistry.byName(form.prefix());
            var material = GTMaterialRegistry.get(form.material());
            if (prefix == null || !material.isValid() || !prefix.isValidFor(material.resolve())) continue;
            ItemMaterialRegistry.register(item, prefix, material.resolve());
        }
    }
}
