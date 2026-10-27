package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialUnification;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import net.minecraft.world.item.ItemStack;

/** Convert legacy GT stacks into their preferred vanilla equivalents. */
public final class VanillaMaterialUnificationRecipes {
    private VanillaMaterialUnificationRecipes() {}

    public static int register() {
        int count = 0;
        for (String name : new String[]{"Iron", "Gold", "Copper", "Netherite", "Diamond", "Coal", "Lapis", "Redstone"}) {
            var material = GTMaterialRegistry.get(name);
            if (material == null) continue;
            for (var prefix : new BlockMaterialPrefix[]{BlockMaterialPrefix.blockIngot, BlockMaterialPrefix.blockSolid,
                    BlockMaterialPrefix.blockGem, BlockMaterialPrefix.blockDust}) {
                count += add(GTBlocks.getStack(prefix, material));
            }
        }
        for (String name : new String[]{"Iron", "Gold", "Copper", "Netherite", "Diamond", "Emerald", "Amethyst", "Coal",
                "Charcoal", "Lapis", "Redstone", "Sugar", "Gunpowder", "Glowstone"}) {
            var material = GTMaterialRegistry.get(name);
            if (material == null) continue;
            for (var prefix : new MaterialPrefix[]{MaterialPrefix.ingot, MaterialPrefix.nugget, MaterialPrefix.gem, MaterialPrefix.dust}) {
                count += add(GTItems.getStack(prefix, material));
            }
        }
        return count;
    }

    private static int add(ItemStack input) {
        if (!MaterialUnification.hiddenDuplicate(input)) return 0;
        ItemStack output = MaterialUnification.canonical(input);
        if (output.isEmpty() || output.getItem() == input.getItem()) return 0;
        var recipe = new Recipe(new ItemStack[]{input}, new ItemStack[]{output}, null, null, null, null, 1, 0, 0)
                .withItemInputGuard(0, candidate -> candidate.getItem() == input.getItem());
        return MachineRecipeMaps.Generifier.addRecipe(recipe, false, false, false, false) == null ? 0 : 1;
    }
}
