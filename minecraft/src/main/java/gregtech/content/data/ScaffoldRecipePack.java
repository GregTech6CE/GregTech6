package gregtech.content.data;

import gregtech.content.registry.GTStorageMetals;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Emits the 44 original scaffold recipes from the metalset table {@link GTStorageMetals#ALL}.
 *
 * <p>GT6 registers one scaffold per metalset material ({@code Loader_MultiTileEntities}) with
 * the same {@code TPT / SdS} pattern: a plate between two screws over two rods around a
 * screwdriver. The plain {@code scaffold} id is the steel one, and {@code scaffold_osmium} is built
 * from the elemental osmium material, exactly as the blocks are registered. Scaffolds are not
 * mirror-craftable.</p>
 */
public final class ScaffoldRecipePack {
    private static final String TYPE = "gt6ce:tool_shaped";
    private static final List<String> PATTERN = List.of("TPT", "SdS");

    private ScaffoldRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        for (GTStorageMetals.Spec spec : GTStorageMetals.ALL) {
            String id = spec.scaffoldId();
            String material = MaterialPrefix.camelToSnake(spec.material().getName());
            var key = new LinkedHashMap<String, Object>();
            key.put("T", GeneratedRecipeJson.tag("forge:screws/" + material));
            key.put("P", GeneratedRecipeJson.tag("forge:plates/" + material));
            key.put("S", GeneratedRecipeJson.tag("forge:rods/" + material));
            key.put("d", GeneratedRecipeJson.item("gt6ce:tool_screwdriver"));
            GeneratedRecipeJson.put(
                    generated,
                    "scaffolds/" + id,
                    GeneratedRecipeJson.shaped(TYPE, PATTERN, key, "gt6ce:" + id, 1, false));
        }
    }
}
