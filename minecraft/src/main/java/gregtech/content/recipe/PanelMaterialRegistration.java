/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Source constructors and CR.REV bookkeeping: panel-material-source-20261006.json. */
package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.transport.*;
import gregtech.engine.material.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Register original panel material fallbacks before recovery recipe creation. */
public final class PanelMaterialRegistration {
    private PanelMaterialRegistration() {}

    private static final Set<Item> RECOVERY_ITEMS = Collections.newSetFromMap(new IdentityHashMap<>());

    public static Set<Item> recoveryItems() {
        return Collections.unmodifiableSet(RECOVERY_ITEMS);
    }

    private static Item item(String id) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item == Items.AIR) throw new IllegalStateException("Missing panel material item " + id);
        return item;
    }

    private static void fallback(Item item, ItemComposition data, String declarationId) {
        ItemMaterialRegistry.register(
                item, data, ItemCompositionSource.Kind.RECIPE_REVERSE, declarationId, "GT6 panel material source");
        RECOVERY_ITEMS.add(item);
    }

    public static void register() {
        var fallbackData = new HashMap<String, ItemComposition>();
        for (String kind : List.of("concrete", "asphalt", "cfoam", "cfoam_fresh", "cfoam_slab")) {
            var material = switch (kind) {
                case "concrete" -> gregtech.content.data.generated.GT6Materials.Stones.Concrete;
                case "asphalt" -> gregtech.content.data.generated.GT6Materials.Compounds.Asphalt;
                default -> gregtech.content.data.generated.GT6Materials.Compounds.ConstructionFoam;
            };
            long amount = kind.endsWith("_slab") ? GTValues.U / 2 : GTValues.U;
            var id = "gt6ce:" + kind;
            var data = new ItemComposition(null, material, amount);
            fallbackData.put(id, data);
            fallback(item(id), data, "gt6ce:panel-base/" + kind);
        }
        var reinforced = new ItemComposition(
                null,
                List.of(
                        MaterialComponent.of(GTMaterialRegistry.get("Concrete"), GTValues.U),
                        MaterialComponent.of(GTMaterialRegistry.get("Iron"), MaterialPrefix.stick.getMaterialWeight())),
                GTMaterialRegistry.get("Concrete"),
                List.of(new ItemCompositionSource(
                        ItemCompositionSource.Kind.EXPLICIT,
                        "gt6ce:panel-base/concrete_reinforced",
                        "GT6 BlockConcreteReinforced",
                        "concrete_reinforced",
                        "")),
                true);
        fallbackData.put("gt6ce:concrete_reinforced", reinforced);
        fallback(item("gt6ce:concrete_reinforced"), reinforced, "gt6ce:panel-base/concrete_reinforced");
        for (var variant : gregtech.content.registry.GTConstructionVariants.all()) {
            var block = (gregtech.content.block.misc.FixedConstructionColor) variant.get();
            String family = "gt6ce:" + block.family();
            var composition = fallbackData.get(family);
            if (composition == null) {
                composition = gregtech.content.machine.OriginalDeviceMaterialData.block(block.family())
                        .orElseThrow(() -> new IllegalStateException("Missing panel construction family " + family));
                fallbackData.put(family, composition);
            }
            fallback(
                    variant.get().asItem(),
                    composition,
                    "gt6ce:panel-variant/"
                            + BuiltInRegistries.ITEM
                                    .getKey(variant.get().asItem())
                                    .getPath());
        }
        for (var spec : PanelCatalog.ALL) {
            var input = item(spec.input());
            ItemComposition inputData;
            if (spec.kind().equals("wood")) {
                inputData = new ItemComposition(null, PanelMaterialRules.plankMaterial(spec), GTValues.U);
                fallback(
                        input,
                        inputData,
                        "gt6ce:panel-plank-input/"
                                + BuiltInRegistries.ITEM.getKey(input).getPath());
            } else {
                inputData = fallbackData.get(spec.input());
                if (inputData == null)
                    throw new IllegalStateException("Missing panel input composition for " + spec.input());
            }
            fallback(item("gt6ce:" + spec.id()), PanelMaterialRules.panel(inputData), "gt6ce:panel/" + spec.id());
        }
    }
}
