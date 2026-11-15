package gregtech.content.material;

import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.content.block.wood.WoodSpecies;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.plant.WoodSlabCatalog;
import gregtech.content.recipe.NaturalWoodCraftingRows;
import gregtech.content.registry.GTWoods;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemCompositionSource;
import gregtech.engine.material.ItemMaterialRegistry;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Fixed GT wood item compositions, installed before recipe families run. */
public final class WoodMaterialCompositions {
    private WoodMaterialCompositions() {}

    public static void register() {
        registerNativeWoods();
        registerNaturalWoods();
        registerVanillaWoods();
    }

    private static void registerNativeWoods() {
        for (String wood : WoodSlabCatalog.WOODS) {
            var material = GTMaterialRegistry.get(WoodSlabCatalog.material(wood));
            ItemMaterialRegistry.register(item("gt6ce:planks_" + wood), MaterialPrefix.plate, material, GTValues.U);
            ItemMaterialRegistry.register(item("gt6ce:slab_" + wood), null, material, GTValues.U / 2);
        }
        for (var species : WoodSpecies.values()) {
            var material = GTMaterialRegistry.get(WoodSlabCatalog.material(species.id()));
            var bark = GTMaterialRegistry.get(species == WoodSpecies.CINNAMON ? "Cinnamon" : "Bark");
            ItemMaterialRegistry.register(
                    GTWoods.log(species).asItem(),
                    new ItemComposition(
                            null,
                            List.of(
                                    MaterialComponent.of(material, 8 * GTValues.U),
                                    MaterialComponent.of(bark, GTValues.U)),
                            material,
                            List.of(new ItemCompositionSource(
                                    ItemCompositionSource.Kind.EXPLICIT,
                                    "gt6ce:wood-log/" + species.id(),
                                    "GT6 WoodEntry",
                                    species.id(),
                                    "")),
                            true),
                    ItemCompositionSource.Kind.EXPLICIT,
                    "gt6ce:wood-log/" + species.id(),
                    "GT6 WoodEntry");
            ItemMaterialRegistry.register(GTWoods.strippedLog(species).asItem(), null, material, 8 * GTValues.U);
        }
    }

    private static void registerNaturalWoods() {
        for (var wood : NaturalWoodCraftingRows.SPECIES) {
            var material = GTMaterialRegistry.get(wood.material());
            var bark = GTMaterialRegistry.get(wood.barkMaterial());
            var components =
                    List.of(MaterialComponent.of(material, 5 * GTValues.U), MaterialComponent.of(bark, GTValues.U));
            ItemMaterialRegistry.register(
                    item("gt6ce:log_" + wood.kind()),
                    new ItemComposition(
                            null,
                            components,
                            material,
                            List.of(new ItemCompositionSource(
                                    ItemCompositionSource.Kind.EXPLICIT,
                                    "gt6ce:wood-log/" + wood.kind(),
                                    "GT6 WoodEntry Log1",
                                    wood.kind(),
                                    "")),
                            true),
                    ItemCompositionSource.Kind.EXPLICIT,
                    "gt6ce:wood-log/" + wood.kind(),
                    "GT6 WoodEntry Log1");
            ItemMaterialRegistry.register(
                    item("gt6ce:planks_" + wood.kind()), MaterialPrefix.plate, material, GTValues.U);
        }
    }

    private static void registerVanillaWoods() {
        for (String wood :
                new String[] {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"})
            registerVanillaWood(wood, "log", "wood");
        registerVanillaWood("crimson", "stem", "hyphae");
        registerVanillaWood("warped", "stem", "hyphae");
    }

    private static void registerVanillaWood(String wood, String... types) {
        var material = GTMaterialRegistry.get(WoodSlabCatalog.material(wood));
        var bark = GTMaterialRegistry.get("Bark");
        for (String type : types) {
            String raw = wood + "_" + type;
            registerVanilla(
                    raw,
                    new ItemComposition(
                            null,
                            List.of(
                                    MaterialComponent.of(material, 8 * GTValues.U),
                                    MaterialComponent.of(bark, GTValues.U)),
                            material,
                            "GT6 WoodEntry (modern wood binding)",
                            true));
            registerVanilla("stripped_" + raw, new ItemComposition(null, material, 8 * GTValues.U));
        }
        registerVanilla(wood + "_planks", new ItemComposition(MaterialPrefix.plate, material, GTValues.U));
        registerVanilla(wood + "_stairs", new ItemComposition(null, material, 3 * GTValues.U / 4));
        registerVanilla(wood + "_slab", new ItemComposition(null, material, GTValues.U / 2));
    }

    private static void registerVanilla(String path, ItemComposition composition) {
        var item = item("minecraft:" + path);
        ItemMaterialRegistry.register(
                item,
                composition,
                ItemCompositionSource.Kind.EXPLICIT,
                "gt6ce:wood-entry/" + path,
                "GT6 wood material binding",
                VanillaCompositionLoader.declarationId(item).orElse(null));
    }

    private static Item item(String id) {
        var value = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (value == Items.AIR) throw new IllegalStateException("Missing wood material item " + id);
        return value;
    }
}
