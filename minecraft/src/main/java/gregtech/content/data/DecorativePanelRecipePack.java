package gregtech.content.data;

import gregtech.content.transport.PanelCatalog;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Emits the 75 original decorative panel recipes from {@link PanelCatalog#CANONICAL}.
 *
 * <p>GT6 registers one panel per construction identity ({@code Loader_MultiTileEntities}): the same
 * {@code TsT / TPT / TdT} pattern with the panel's own construction block in the centre, six panels
 * per craft, and mirror-craftable. Panels of a dye colour also record it as
 * {@code construction_color} so the crafting preview paints the result; wood panels carry no
 * colour and leave the field out.</p>
 */
public final class DecorativePanelRecipePack {
    private static final String TYPE = "gt6ce:tool_shaped";
    private static final List<String> PATTERN = List.of("TsT", "TPT", "TdT");
    private static final int RESULT_COUNT = 6;

    private DecorativePanelRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        for (PanelCatalog.Spec spec : PanelCatalog.CANONICAL) {
            GeneratedRecipeJson.put(
                    generated,
                    "decorative_panels/" + spec.id(),
                    GeneratedRecipeJson.shaped(
                            TYPE,
                            PATTERN,
                            key(spec.input()),
                            "gt6ce:" + spec.id(),
                            RESULT_COUNT,
                            true,
                            null,
                            null,
                            spec.color().isEmpty() ? null : spec.color(),
                            null));
        }
    }

    private static Map<String, Object> key(String construction) {
        var key = new LinkedHashMap<String, Object>();
        key.put("T", GeneratedRecipeJson.tag("gt6ce:screws/any_iron_or_steel"));
        key.put("P", GeneratedRecipeJson.item(construction));
        key.put("s", GeneratedRecipeJson.item("gt6ce:tool_saw"));
        key.put("d", GeneratedRecipeJson.item("gt6ce:tool_screwdriver"));
        return key;
    }
}
