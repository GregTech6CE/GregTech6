package gregtech.engine.recipe;

import gregtech.api.addon.recipe.MaterialUnitIngredient;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialDisplayBinding;
import gregtech.engine.material.MaterialEquivalence;
import net.minecraft.world.item.ItemStack;

/** Shared admission boundary for data packs and addons; a rule's allowed forms are explicit. */
public final class MaterialUnitRecipes {
    private MaterialUnitRecipes() {}

    public static void discardUnregistered() {
        MaterialUnitRecipeIndex.discardUnregistered();
    }

    public static ItemStack display(MaterialUnitIngredient ingredient) {
        var material = GTMaterialRegistry.get(ingredient.material());
        if (material == null || !material.isValid())
            throw new IllegalArgumentException("Unknown material " + ingredient.material());
        var display = GTItems.getStack(MaterialPrefix.unit, material.resolve(), 1);
        if (display.isEmpty()) throw new IllegalArgumentException("Missing unit item " + ingredient.material());
        return display;
    }

    public static Recipe add(RecipeMap map, Recipe template, int slot, MaterialUnitIngredient ingredient) {
        if (slot < 0 || slot >= template.mInputs.length || template.itemTag(slot) != null)
            throw new IllegalArgumentException("Material input slot");
        var display = display(ingredient);
        template.mInputs[slot] = display;
        template.materialDeclarations = java.util.Map.of(slot, ingredient);
        var material = GTMaterialRegistry.get(ingredient.material()).resolve();
        var rule = new MaterialUnitRecipe(template, slot, material, ingredient.amount(), ingredient.rounding());
        var found = new java.util.HashSet<String>();
        for (var candidate : MaterialDisplayBinding.alternatives(display)) {
            var form = MaterialEquivalence.form(candidate);
            if (form == null
                    || form.material().resolve() != material
                    || !ingredient.forms().contains(form.prefix().getName())) continue;
            long units = form.prefix().getMaterialWeight();
            if (units <= 0 || form.prefix() == MaterialPrefix.unit) continue;
            found.add(form.prefix().getName());
            rule.source(
                    candidate,
                    units,
                    stack -> {
                        var current = MaterialEquivalence.form(stack);
                        return current != null
                                && current.material().resolve() == material
                                && current.prefix() == form.prefix()
                                && !stack.isDamaged()
                                && !ItemMaterialRegistry.hasStoredContents(stack);
                    },
                    0,
                    0);
        }
        for (String name : ingredient.forms()) {
            var prefix = gregtech.content.data.MaterialPrefixes.all().get(name);
            if (prefix == null || !prefix.isBlock() || found.contains(name)) continue;
            var block = prefix.getBlockDelegate();
            long units = block.getMaterialWeight();
            if (units <= 0) continue;
            var candidate = gregtech.engine.material.MaterialUnification.vanillaFormStack(name, material);
            if (candidate.isEmpty()) candidate = gregtech.content.registry.GTBlocks.getStack(block, material);
            if (candidate.isEmpty()) continue;
            found.add(name);
            rule.source(
                    candidate,
                    units,
                    stack -> !stack.isDamaged() && !ItemMaterialRegistry.hasStoredContents(stack),
                    0,
                    0);
        }
        if (!found.containsAll(ingredient.forms()))
            throw new IllegalArgumentException(
                    "Missing material forms for " + ingredient.material() + ": " + ingredient.forms());
        return rule.publish(map);
    }
}
