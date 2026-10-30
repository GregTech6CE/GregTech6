package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.Recipe;
import net.minecraft.world.item.ItemStack;

/**
 * The directed {@code RM.generify} rows the source registers next to its material handler. They own
 * their input and leave every other row to {@link GenerifierRecipeFamily}.
 */
public final class GenerifierExplicitRecipes {
    private GenerifierExplicitRecipes() {}

    /** {input form, input material, output form, output material}: the source's dye dusts. */
    private static final String[] DYE_ROWS = {
        "dust", "Chocolate", "dust", "Brown",
        "dust", "Azurite", "dust", "Blue",
        "dust", "Malachite", "dust", "Green",
        "gem", "Malachite", "dust", "Green",
        "gem", "Lazurite", "dust", "Cyan",
        "dust", "Ashes", "dust", "LightGray",
        "dust", "VolcanicAshes", "dust", "Black",
        "dust", "DarkAshes", "dust", "Gray",
    };

    public static int register() {
        return dyeDusts();
    }

    private static int dyeDusts() {
        int count = 0;
        for (int i = 0; i < DYE_ROWS.length; i += 4) {
            GTMaterial source = GTMaterialRegistry.get(DYE_ROWS[i + 1]);
            GTMaterial target = GTMaterialRegistry.get(DYE_ROWS[i + 3]);
            if (!source.isValid() || !target.isValid()) continue;
            ItemStack input = GTItems.getCreativeStack(PrefixRegistry.byName(DYE_ROWS[i]), source);
            ItemStack output = GTItems.getCreativeStack(PrefixRegistry.byName(DYE_ROWS[i + 2]), target);
            if (input.isEmpty() || output.isEmpty()) continue;
            count += add(input, output);
        }
        return count;
    }

    private static int add(ItemStack input, ItemStack output) {
        Recipe recipe = new Recipe(new ItemStack[] {input}, new ItemStack[] {output}, null, null, null, null, 1, 0, 0)
                .withItemInputGuard(0, candidate -> candidate.getItem() == input.getItem());
        return MachineRecipeMaps.Generifier.addRecipe(recipe, false, false, false, false) == null ? 0 : 1;
    }
}
