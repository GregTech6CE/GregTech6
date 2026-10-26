/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Source constructors and CR.REV bookkeeping: panel-material-source-20261006.json. */
package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.engine.material.*;
import gregtech.content.transport.*;
import gregtech.content.data.MaterialPrefix;
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
                case "concrete" -> gregtech.content.data.generated.GT6Materials.Stones.Concrete;
                case "asphalt" -> gregtech.content.data.generated.GT6Materials.Compounds.Asphalt;
                default -> gregtech.content.data.generated.GT6Materials.Compounds.ConstructionFoam;
            };
            long amount = kind.endsWith("_slab") ? GTValues.U / 2 : GTValues.U;
            known(item("gt6ce:" + kind), new ItemComposition(null, material, amount));
        }
        known(item("gt6ce:concrete_reinforced"), new ItemComposition(null, List.of(
                MaterialComponent.of(GTMaterialRegistry.get("Concrete"), GTValues.U),
                MaterialComponent.of(GTMaterialRegistry.get("Iron"), MaterialPrefix.stick.getMaterialWeight())),
                "GT6 BlockConcreteReinforced", true));
        for (var variant : gregtech.content.registry.GTConstructionVariants.all()) {
            var block = (gregtech.content.block.misc.FixedConstructionColor) variant.get();
            var composition = ItemMaterialRegistry.base(item("gt6ce:" + block.family())).orElse(null);
            if (composition != null) known(variant.get().asItem(), composition);
        }
        for (var spec : PanelCatalog.ALL) {
            var input = item(spec.input());
            if (spec.kind().equals("wood") && spec.input().startsWith("gt6ce:"))
                known(input, new ItemComposition(null, PanelMaterialRules.plankMaterial(spec), GTValues.U));
            var inputData = ItemMaterialRegistry.base(input).orElse(null);
            known(item("gt6ce:" + spec.id()), PanelMaterialRules.panel(inputData));
        }
    }
}
