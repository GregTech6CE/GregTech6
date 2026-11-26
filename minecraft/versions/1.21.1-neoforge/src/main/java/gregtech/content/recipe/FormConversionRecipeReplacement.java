package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.recipe.OriginalFormConversions.Conversion;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialForm;
import gregtech.engine.material.ItemMaterialForms;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/** NeoForge 1.21.1 runtime pass that removes replaced plain recipes and rebinds tag-backed form conversions. */
final class FormConversionRecipeReplacement {
    private static final String FORM_CONVERSION_ID_PREFIX = GregTechIdentity.REGISTRY_NAMESPACE + ":form_conversion/";

    private FormConversionRecipeReplacement() {}

    static void replacePlain(RecipeManager manager, RegistryAccess access) {
        var recipes = new ArrayList<>(manager.getRecipes());
        int before = recipes.size();
        recipes.removeIf(holder -> holder.value() instanceof CraftingRecipe crafting
                && !ConfiguredRecipeRules.keepVanillaStorageBlockRecipe(holder.id())
                && FormConversionCraftingRecipeInjector.disallowsPlainRecipe(holder.id(), crafting, access));
        int removed = before - recipes.size();
        // Rebind existing generated rows too: their JSON ingredient aliases were indexed before
        // this reload's tags existed. Only this loader's own form-conversion rows are rebuilt.
        int beforeRebind = recipes.size();
        recipes.removeIf(holder -> holder.id().getNamespace().equals(GregTechIdentity.REGISTRY_NAMESPACE)
                && holder.id().getPath().startsWith("form_conversion/")
                && holder.value() instanceof ToolShapelessRecipe);
        int rebound = beforeRebind - recipes.size();
        var ids = new HashSet<ResourceLocation>();
        for (var holder : recipes) ids.add(holder.id());
        int added = 0;
        synchronized (FormConversionCraftingRecipeInjector.class) {
            FormConversionCraftingRecipeInjector.removeRegisteredWithPrefix(FORM_CONVERSION_ID_PREFIX);
            // Generated JSON is built before this reload's tags exist. Bind all source forms now,
            // including Harder Ores raw chunks, without creating a GT-owned item for the prefix.
            for (var material : GTMaterialRegistry.allMaterials()) {
                if (!material.isValid()) continue;
                for (var conversion : OriginalFormConversions.FIXED) {
                    added += addBoundConversion(recipes, ids, conversion, material);
                }
            }
            for (var entry : ItemMaterialForms.aliases().keySet()) {
                int count = ItemMaterialForm.STANDARD_ORES.contains(entry.prefix())
                        ? 1
                        : ItemMaterialForm.DENSE_ORES.contains(entry.prefix()) ? 2 : 0;
                if (count > 0) {
                    added += addBoundConversion(
                            recipes,
                            ids,
                            new Conversion(entry.prefix(), 1, "oreRaw", count, true),
                            GTMaterialRegistry.get(entry.material()).resolve());
                }
            }
        }
        if (removed > 0 || rebound > 0 || added > 0) manager.replaceRecipes(recipes);
        Mc.logger()
                .info(
                        "[gregtech] Replaced {} plain source-equivalent crafting rows; rebuilt {} tag-bound conversions ({} prior rows)",
                        removed,
                        added,
                        rebound);
    }

    private static int addBoundConversion(
            List<RecipeHolder<?>> recipes, Set<ResourceLocation> ids, Conversion conversion, GTMaterial material) {
        var id = FormConversionCraftingRecipeInjector.conversionId(conversion, material);
        if (ids.contains(id)) return 0;
        var aliases = ItemMaterialForms.aliases();
        var input = FormConversionCraftingRecipeInjector.item(aliases, conversion.input(), material, 1);
        var output = FormConversionCraftingRecipeInjector.item(
                aliases, conversion.output(), material, conversion.outputCount());
        if (input.isEmpty() || output.isEmpty()) return 0;
        var ingredients =
                NonNullList.withSize(conversion.inputCount(), CraftingMaterialForms.ingredient(input, aliases));
        var base = new ShapelessRecipe(
                "gt.form_conversion",
                CraftingBookCategory.MISC,
                ItemMaterialForms.canonical(output.copy()),
                ingredients);
        var recipe = new ToolShapelessRecipe(
                base,
                false,
                OriginalFormConversions.selector(conversion),
                conversion.input(),
                material.resolve().getName(),
                false,
                FormConversionPlacement.FIXED);
        recipes.add(new RecipeHolder<>(id, recipe));
        ids.add(id);
        FormConversionCraftingRecipeInjector.recordRegistered(id.toString());
        return 1;
    }
}
