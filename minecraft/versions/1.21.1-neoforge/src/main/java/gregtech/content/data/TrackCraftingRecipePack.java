package gregtech.content.data;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Original track patterns over shared catalogs; the publishing pack is owned by {@link RecipePublishing}. */
public final class TrackCraftingRecipePack {
    private static final Gson GSON = new Gson();

    private TrackCraftingRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        int before = generated.size();
        int skipped = 0;
        for (var row : gregtech.content.transport.TrackRecipeCatalog.rows()) {
            var output = new net.minecraft.world.item.ItemStack(
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse(row.output())));
            Map<String, Object> keys = new HashMap<>();
            boolean missing = output.isEmpty();
            for (var key : row.ingredients().entrySet()) {
                var stack = resolve(key.getValue());
                if (stack.isEmpty()) {
                    missing = true;
                    break;
                }
                keys.put(
                        key.getKey(),
                        Map.of(
                                "item",
                                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
            }
            if (missing) {
                skipped++;
                com.mojang.logging.LogUtils.getLogger()
                        .warn("[gregtech] Missing source rail crafting identity {}", row.id());
                continue;
            }
            var json = Map.of(
                    "type",
                    "minecraft:crafting_shaped",
                    "category",
                    "misc",
                    "group",
                    "gt.tracks",
                    "pattern",
                    row.pattern().split("\\|"),
                    "key",
                    keys,
                    "result",
                    Map.of("id", row.output(), "count", row.count()));
            generated.put(
                    ResourceLocation.fromNamespaceAndPath("gt6ce", "recipe/" + row.id() + ".json"),
                    GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
        }
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "[gregtech] Native original track crafting rows: {}; missing={}",
                        generated.size() - before,
                        skipped);
    }

    private static net.minecraft.world.item.ItemStack resolve(String value) {
        if (value.startsWith("item:"))
            return new net.minecraft.world.item.ItemStack(
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse(value.substring(5))));
        int colon = value.indexOf(':');
        var material = gregtech.engine.material.GTMaterialRegistry.get(value.substring(colon + 1));
        if (material == null || !material.isValid()) return net.minecraft.world.item.ItemStack.EMPTY;
        return gregtech.content.registry.GTItems.getStack(
                value.startsWith("rail:") ? MaterialPrefix.railGt : MaterialPrefix.stick, material.resolve(), 1);
    }
}
