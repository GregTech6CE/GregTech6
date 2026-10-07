/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Source constructors and CR.REV bookkeeping: panel-material-source-20261006.json. */
package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.content.transport.*;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

/** Register once after both vanilla and native material bindings, before recovery recipe creation. */
public final class PanelMaterialRegistration {
    private PanelMaterialRegistration() {}
    private static final Set<Item> RECOVERY_ITEMS = Collections.newSetFromMap(new IdentityHashMap<>());
    public static Set<Item> recoveryItems() { return Collections.unmodifiableSet(RECOVERY_ITEMS); }
    private static Item item(String id) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item == Items.AIR) throw new IllegalStateException("Missing panel material item " + id);
        return item;
    }
    private static void known(Item item, ItemComposition data) {
        // OM.data/addItemData_ preserves an earlier explicit composition.
        if (ItemMaterialRegistry.base(item).isEmpty()) ItemMaterialRegistry.register(item, data);
        RECOVERY_ITEMS.add(item);
    }
    public static void register() {
        for (String kind : List.of("concrete", "asphalt", "cfoam", "cfoam_fresh", "cfoam_slab")) {
            var material = switch (kind) {
                case "concrete" -> com.gregtech.gregtech.data.generated.GT6Materials.Stones.Concrete;
                case "asphalt" -> com.gregtech.gregtech.data.generated.GT6Materials.Compounds.Asphalt;
                default -> com.gregtech.gregtech.data.generated.GT6Materials.Compounds.ConstructionFoam;
            };
            long amount = kind.endsWith("_slab") ? GTValues.U / 2 : GTValues.U;
            known(item("gregtech:" + kind), new ItemComposition(null, material, amount));
        }
        known(item("gregtech:concrete_reinforced"), new ItemComposition(null, List.of(
                MaterialComponent.of(GTMaterialRegistry.get("Concrete"), GTValues.U),
                MaterialComponent.of(GTMaterialRegistry.get("Iron"), MaterialPrefix.stick.getMaterialWeight())),
                "GT6 BlockConcreteReinforced", true));
        for (var spec : PanelCatalog.ALL) {
            var input = item(spec.input());
            if (spec.kind().equals("wood") && spec.input().startsWith("gregtech:"))
                known(input, new ItemComposition(null, PanelMaterialRules.plankMaterial(spec), GTValues.U));
            var inputData = ItemMaterialRegistry.base(input).orElse(null);
            known(item("gregtech:" + spec.id()), PanelMaterialRules.panel(inputData));
        }
    }
}
