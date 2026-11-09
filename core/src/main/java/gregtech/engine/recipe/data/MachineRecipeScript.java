package gregtech.engine.recipe.data;

import gregtech.api.addon.recipe.FluidComponent;
import gregtech.api.addon.recipe.ItemComponent;
import gregtech.api.addon.recipe.ItemResult;
import gregtech.api.addon.recipe.MachineRecipeDocument;
import gregtech.api.addon.recipe.MachineRecipeDocuments;
import gregtech.api.addon.recipe.MachineRecipeRemovalDocument;
import gregtech.engine.addon.AddonMachines;
import java.util.List;

/**
 * Builds the JSON object a KubeJS machine-recipe schema submits.
 * The field names are the ones {@link MachineRecipeDocuments} already accepts.
 */
public final class MachineRecipeScript {
    private MachineRecipeScript() {}

    public static List<String> maps() {
        return List.copyOf(AddonMachines.knownRecipeMapNames());
    }

    /** Fails while the script is loading when the map is not built in and not declared by an addon. */
    public static void requireKnownMap(String map) {
        if (map == null || !AddonMachines.knownRecipeMapNames().contains(map))
            throw new IllegalArgumentException("Unknown machine recipe map '" + map + "'");
    }

    public static String addition(
            String map,
            List<ItemComponent> itemInputs,
            List<FluidComponent> fluidInputs,
            List<ItemResult> itemOutputs,
            List<FluidComponent> fluidOutputs,
            long euPerTick,
            long duration,
            long special) {
        return addition(map, itemInputs, fluidInputs, itemOutputs, fluidOutputs, euPerTick, duration, special, 0L);
    }

    /** The same row with an explicit lookup priority; higher wins, equal priorities keep publication order. */
    public static String addition(
            String map,
            List<ItemComponent> itemInputs,
            List<FluidComponent> fluidInputs,
            List<ItemResult> itemOutputs,
            List<FluidComponent> fluidOutputs,
            long euPerTick,
            long duration,
            long special,
            long priority) {
        requireKnownMap(map);
        return MachineRecipeDocuments.canonical(new MachineRecipeDocument(
                map, itemInputs, fluidInputs, itemOutputs, fluidOutputs, euPerTick, duration, special, null, priority));
    }

    public static String removal(
            String map,
            List<ItemComponent> itemInputs,
            List<FluidComponent> fluidInputs,
            List<ItemResult> itemOutputs,
            List<FluidComponent> fluidOutputs) {
        requireKnownMap(map);
        return MachineRecipeDocuments.canonical(
                new MachineRecipeRemovalDocument(map, false, itemInputs, fluidInputs, itemOutputs, fluidOutputs));
    }

    public static String clear(String map) {
        requireKnownMap(map);
        return MachineRecipeDocuments.canonical(
                new MachineRecipeRemovalDocument(map, true, List.of(), List.of(), List.of(), List.of()));
    }

    /** Prefixes a canonical body with the vanilla recipe type. */
    public static String typed(String type, String body) {
        if (type == null || type.isBlank() || body == null || !body.startsWith("{"))
            throw new IllegalArgumentException("json");
        return "{\"type\":\"" + type + "\"," + body.substring(1);
    }

    /** {@code #namespace:path} is a tag. Anything else is an item id. */
    public static ItemComponent item(String text, int count) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("item");
        if (text.startsWith("#")) return new ItemComponent(null, text.substring(1), count);
        return new ItemComponent(text, null, count);
    }

    public static ItemResult result(String item, int count, int chance) {
        return new ItemResult(item, count, chance);
    }

    public static ItemResult resultTag(String tag, int count, int chance) {
        return ItemResult.tag(tag, count, chance);
    }

    public static ItemResult resultMaterial(String material, String form, int count, int chance) {
        return ItemResult.material(material, form, count, chance);
    }

    public static ItemComponent material(String material, String form, int count) {
        return ItemComponent.material(material, form, count);
    }

    public static FluidComponent fluid(String fluid, long amount) {
        return fluid != null && fluid.startsWith("#")
                ? new FluidComponent(null, fluid.substring(1), amount)
                : new FluidComponent(fluid, amount);
    }
}
