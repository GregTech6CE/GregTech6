package gregtech.content.data;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Original bottle filling dictionary patterns over shared catalogs; owned by {@link RecipePublishing}. */
public final class BottleFillingRecipePack {
    private static final Gson GSON = new Gson();

    private BottleFillingRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        int before = generated.size();
        for (var family : gregtech.content.food.BottleFillingRows.all())
            for (int count = 4; count >= 1; count--) {
                var ingredients = new ArrayList<Map<String, Object>>();
                ingredients.add(Map.of("type", "gt6ce:finite_fluid_container_1000", "fluid", family.fluidKey()));
                for (int i = 0; i < count; i++)
                    ingredients.add(Map.of(
                            "item",
                            BuiltInRegistries.ITEM
                                    .getKey(
                                            family.itemId().equals("minecraft:honey_bottle")
                                                    ? net.minecraft.world.item.Items.GLASS_BOTTLE
                                                    : gregtech.content.item.BottleItem.emptyBottle()
                                                            .getItem())
                                    .toString()));
                var recipe = Map.of(
                        "type",
                        "gt6ce:finite_bottle_filling",
                        "group",
                        "gt.bottles",
                        "category",
                        "misc",
                        "ingredients",
                        ingredients,
                        "result",
                        Map.of("id", family.itemId(), "count", count));
                generated.put(
                        ResourceLocation.fromNamespaceAndPath(
                                "gt6ce", "recipe/bottles/" + family.bottleId() + "_x" + count + ".json"),
                        GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8));
            }
        com.mojang.logging.LogUtils.getLogger()
                .info("[gregtech] Original finite bottle filling datapack: {} rows", generated.size() - before);
    }
}
