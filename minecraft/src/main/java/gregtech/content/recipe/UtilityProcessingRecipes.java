/* GregTech-6 Team: MultiItemRandomTools, Loader_Recipes_Other and Loader_Recipes_Handlers; LGPL-3.0-or-later. */
package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.content.block.misc.ConcreteBlock;
import gregtech.content.data.*;
import gregtech.content.registry.*;
import gregtech.engine.recipe.*;
import net.minecraft.core.registries.BuiltInRegistries;
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
            add(
                    "pellet/" + wood,
                    MachineRecipeMaps.Mixer,
                    16L * count,
                    16,
                    items(powder),
                    items(item("gt6ce:wood_pellet", count)),
                    fluids(fluid("Glue", 25 * count)),
                    null);
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
        // Full material crates pack 64 pieces; partial crates are normally loot and unpack 16.
        for (var block : BuiltInRegistries.BLOCK)
            if (block instanceof gregtech.content.block.MaterialBlockLike materialBlock
                    && materialBlock.prefix().isCrate()) {
                String prefix = materialBlock.prefix().getName();
                boolean full = prefix.startsWith("crateGt64");
                String form = prefix.replace("crateGt64", "").replace("crateGt", "");
                var itemPrefix = switch (form) {
                    case "Raw" -> MaterialPrefix.oreRaw;
                    case "Dust" -> MaterialPrefix.dust;
                    case "Gem" -> MaterialPrefix.gem;
                    case "Ingot" -> MaterialPrefix.ingot;
                    case "Plate" -> MaterialPrefix.plate;
                    case "PlateGem" -> MaterialPrefix.plateGem;
                    default -> throw new IllegalStateException(prefix);
                };
                var material = materialBlock.material();
                if (material.has(gregtech.api.material.MaterialProperty.ANTIMATTER)) continue;
                var contents = GTItems.getStack(itemPrefix, material, full ? 64 : 16);
                if (contents.isEmpty())
                    continue; // Prefix registration may precede an unavailable external material form.
                String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
                add(
                        "unpack/" + path,
                        MachineRecipeMaps.Unboxinator,
                        16,
                        16,
                        items(new ItemStack(block)),
                        items(contents, item("gt6ce:crate", 1)),
                        null,
                        null);
                if (full)
                    add(
                            "pack/" + path,
                            MachineRecipeMaps.Boxinator,
                            16,
                            16,
                            items(contents, item("gt6ce:crate", 1)),
                            items(new ItemStack(block)),
                            null,
                            null);
            }
    }
}
