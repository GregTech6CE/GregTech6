/* GregTech-6 Team: MultiItemRandomTools, Loader_Recipes_Other and Loader_Recipes_Handlers; LGPL-3.0-or-later. */
package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.content.block.misc.ConcreteBlock;
import gregtech.content.data.*;
import gregtech.content.registry.*;
import gregtech.engine.recipe.*;
import net.minecraft.world.item.*;

/** Missing source survival manufacturing routes; common tables, native stack boundary. */
public final class UtilityProcessingRecipes extends OriginalRecipeBatch {
    public static final UtilityProcessingRecipes INSTANCE = new UtilityProcessingRecipes();

    public static int register() {
        INSTANCE.load();
        return INSTANCE.entries().size();
    }

    public static final java.util.List<String> UNAVAILABLE_OPTIONAL_FORMS = new java.util.ArrayList<>();

    private void load() {
        for (var color : AsphaltRecipeRows.STONES.entrySet())
            for (String rock : color.getValue()) {
                var powder =
                        GTItems.getStack(MaterialPrefix.dust, gregtech.engine.material.GTMaterialRegistry.get(rock), 1);
                if (powder.isEmpty()) {
                    UNAVAILABLE_OPTIONAL_FORMS.add("asphalt/" + rock);
                    continue;
                }
                String name = color.getKey()
                        .replace("Gray2", "Gray")
                        .replace("LightBlue", "light_blue")
                        .replace("LightGray", "light_gray")
                        .toLowerCase(java.util.Locale.ROOT);
                var block = ((BlockItem) item("gt6ce:asphalt", 1).getItem()).getBlock();
                var asphalt = ConcreteBlock.coloredItem(block, DyeColor.byName(name, DyeColor.GRAY));
                add(
                        "asphalt/" + rock,
                        MachineRecipeMaps.Mixer,
                        16,
                        16,
                        items(powder),
                        items(asphalt),
                        fluids(gregtech.content.recipe.generated.GeneratedRecipeResolver.materialFluid("Asphalt", 144)),
                        null);
            }
        add(
                "road_stripe",
                MachineRecipeMaps.Mixer,
                64,
                16,
                items(mat(MaterialPrefix.dust, "Glass", 1), mat(MaterialPrefix.dust, "Asphalt", 1)),
                items(item("gt6ce:railroad", 4)),
                fluids(fluid("Latex", 144), fluid("Dye_Chemical_White", 144)),
                null);
        var pelletInputs = new java.util.LinkedHashMap<Integer, java.util.List<ItemStack>>();
        for (String wood : new String[] {
            "Wood", "Bark", "LiveRoot", "Livingwood", "Greatwood", "Dreamwood", "Shimmerwood", "Silverwood"
        }) {
            var powder =
                    GTItems.getStack(MaterialPrefix.dust, gregtech.engine.material.GTMaterialRegistry.get(wood), 1);
            if (powder.isEmpty()) {
                UNAVAILABLE_OPTIONAL_FORMS.add("pellet/" + wood);
                continue;
            }
            int count = java.util.Set.of("LiveRoot", "Livingwood", "Greatwood").contains(wood)
                    ? 2
                    : java.util.Set.of("Dreamwood", "Shimmerwood", "Silverwood").contains(wood) ? 4 : 1;
            pelletInputs
                    .computeIfAbsent(count, ignored -> new java.util.ArrayList<>())
                    .add(powder);
        }
        // Magical woods have different yields and glue consumption; only equal-yield dusts
        // share a tag, rather than accepting every wood in the cheapest recipe.
        for (var entry : pelletInputs.entrySet()) {
            int count = entry.getKey();
            var group = RecipeInputGroups.items("wood_pellet_dusts/yield_" + count, entry.getValue());
            var recipe = new Recipe(
                    items(group.stack(1)),
                    items(item("gt6ce:wood_pellet", count)),
                    null,
                    null,
                    fluids(fluid("Glue", 25 * count)),
                    null,
                    16L * count,
                    16,
                    0);
            publish("pellet/yield_" + count, MachineRecipeMaps.Mixer, group.apply(recipe, 0));
        }
        add(
                "pellet_planks",
                MachineRecipeMaps.Compressor,
                16,
                16,
                items(item("gt6ce:wood_pellet", 2)),
                items(item("gt6ce:planks_compressed", 1)),
                null,
                null);
        add(
                "pellet_coke",
                MachineRecipeMaps.CokeOven,
                900,
                0,
                items(item("gt6ce:wood_pellet", 1)),
                items(mat(MaterialPrefix.chunkGt, "Charcoal", 1)),
                null,
                fluids(fluid("Oil_Creosote", 25)));
        for (String acid : new String[] {"SulfuricAcid", "NitricAcid"})
            add(
                    "foam_remover/" + acid,
                    MachineRecipeMaps.Canner,
                    256,
                    16,
                    items(item("gt6ce:empty_spray_can", 1)),
                    items(item("gt6ce:c_foam_removal_spray", 1)),
                    fluids(gregtech.content.recipe.generated.GeneratedRecipeResolver.materialFluid(acid, 16000)),
                    null);
        add(
                "foam_hardener",
                MachineRecipeMaps.Canner,
                256,
                16,
                items(item("minecraft:sand", 7), item("gt6ce:empty_spray_can", 1)),
                items(item("gt6ce:hardening_spray", 1)),
                null,
                null);
    }
}
