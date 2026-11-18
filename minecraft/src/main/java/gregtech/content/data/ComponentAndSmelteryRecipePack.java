package gregtech.content.data;

import gregtech.content.recipe.ComponentAndSmelteryRecipeRules;
import gregtech.content.recipe.ComponentAndSmelteryRecipeRules.Shaped;
import gregtech.content.recipe.ComponentAndSmelteryRecipeRules.Shapeless;
import gregtech.content.recipe.ComponentAndSmelteryRecipeRules.Smelting;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Emits the {@code components} and {@code smeltery_survival} recipe directories from
 * {@link ComponentAndSmelteryRecipeRules}.
 *
 * <p>The rules already carry the final ingredient specs, so this pack only maps a row onto the
 * matching {@link GeneratedRecipeJson} builder: shaped rows keep their optional mirror flag,
 * shapeless rows keep their ingredient order, and the ceramic crucible firing row goes through the
 * shared smelting helper, which owns the per-version result format.</p>
 */
public final class ComponentAndSmelteryRecipePack {
    private ComponentAndSmelteryRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        for (var row : ComponentAndSmelteryRecipeRules.all()) {
            if (row instanceof Shaped shaped) {
                GeneratedRecipeJson.put(
                        generated,
                        shaped.path(),
                        GeneratedRecipeJson.shaped(
                                shaped.type(),
                                shaped.pattern(),
                                gridKey(shaped.key()),
                                shaped.result(),
                                shaped.count(),
                                shaped.allowMirror()));
            } else if (row instanceof Shapeless shapeless) {
                List<Object> ingredients =
                        new ArrayList<>(shapeless.ingredients().size());
                for (String spec : shapeless.ingredients()) {
                    ingredients.add(GeneratedRecipeJson.spec(spec));
                }
                GeneratedRecipeJson.put(
                        generated,
                        shapeless.path(),
                        GeneratedRecipeJson.shapeless(
                                shapeless.type(), ingredients, shapeless.result(), shapeless.count()));
            } else if (row instanceof Smelting smelting) {
                GeneratedRecipeJson.put(
                        generated,
                        smelting.path(),
                        GeneratedRecipeJson.smelting(
                                smelting.ingredient(),
                                smelting.result(),
                                smelting.experience(),
                                smelting.cookingTime()));
            } else {
                throw new IllegalStateException("Unhandled recipe row: " + row);
            }
        }
    }

    private static Map<String, Object> gridKey(Map<String, String> specs) {
        Map<String, Object> key = new LinkedHashMap<>();
        specs.forEach((symbol, spec) -> key.put(symbol, GeneratedRecipeJson.spec(spec)));
        return key;
    }
}
