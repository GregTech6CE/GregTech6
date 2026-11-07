package gregtech.content.recipe;

import gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding;
import gregtech.content.data.*;
import gregtech.content.multiblock.LargeMachineParts;
import gregtech.content.registry.*;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.MaterialUnitRecipe;
import gregtech.engine.recipe.Recipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Welding recipes from GT6 Loader_MultiTileEntities, metalwall / metalwalldense registrations. */
public final class StructuralPartRecipes {
    private StructuralPartRecipes() {}

    public static int register() {
        int count = 0;
        count += wall(GTMultiblocks.TANK_WALL.get(), "StainlessSteel", false);
        count += wall(GTMultiblocks.TANK_WALL_DENSE.get(), "StainlessSteel", true);
        count += wall(GTMultiblocks.IMPLOSION_COMPRESSOR_WALL.get(), "TungstenSteel", true);
        // The original 18004 Tungsten Wall has the four-plate welder route. The older
        // lightning_rod_wall alias is crafted from it; registering both from the same
        // plates and selector makes the recipe map silently discard the original wall.
        count += wall(LargeMachineParts.block(18008), "SteelGalvanized", false);
        count += wall(LargeMachineParts.block(18031), "Lead", true);
        count += wall(LargeMachineParts.block(18003), "TungstenSteel", false);
        count += wall(LargeMachineParts.block(18004), "Tungsten", false);
        count += wall(LargeMachineParts.block(18005), "Adamantium", false);
        count += wall(LargeMachineParts.block(18006), "Titanium", false);
        count += wall(LargeMachineParts.block(18007), "Invar", false);
        count += wall(LargeMachineParts.block(18009), "Steel", false);
        count += wall(LargeMachineParts.block(18010), "Bronze", false);
        count += wall(LargeMachineParts.block(18011), "Lead", false);
        count += wall(LargeMachineParts.block(18012), "Ta4HfC5", false);
        count += wall(LargeMachineParts.block(18026), "Titanium", true);
        count += wall(LargeMachineParts.block(18024), "Tungsten", true);
        count += wall(LargeMachineParts.block(18025), "Adamantium", true);
        count += wall(LargeMachineParts.block(18027), "Invar", true);
        count += wall(LargeMachineParts.block(18028), "SteelGalvanized", true);
        count += wall(LargeMachineParts.block(18029), "Steel", true);
        count += wall(LargeMachineParts.block(18030), "Bronze", true);
        count += wall(LargeMachineParts.block(18032), "Ta4HfC5", true);
        // MT.Ad has neither FURNACE nor SOFT. Original handler: 9 plates, 16 GU/t,
        // 256 ticks per material unit per (quality + 1), not the easy-workable 144 ticks.
        var adamantium = GTMaterialRegistry.get("Adamantium");
        var plates = GTItems.getStack(MaterialPrefix.plate, adamantium, 9);
        var dense = GTItems.getStack(MaterialPrefix.plateDense, adamantium, 1);
        if (!plates.isEmpty() && !dense.isEmpty())
            MachineRecipeMaps.Compressor.addRecipe1(
                    false, 16, 9L * 256 * (adamantium.getToolQuality() + 1), plates, dense);
        return count;
    }

    private static int wall(Block result, String material, boolean dense) {
        var materialObject = GTMaterialRegistry.get(material);
        MaterialPrefix prefix = dense ? MaterialPrefix.plateDense : MaterialPrefix.plate;
        ItemStack plate = GTItems.getStack(prefix, materialObject, 4);
        if (plate.isEmpty()) return 0;
        var template = new Recipe(
                new ItemStack[] {plate, new ItemStack(GTTechnological.selectorTag(10))},
                new ItemStack[] {new ItemStack(result)},
                null,
                null,
                null,
                null,
                dense ? 512 : 256,
                dense ? 64 : 16,
                0);
        long required = Math.multiplyExact(prefix.getMaterialWeight(), 4);
        var rule = new MaterialUnitRecipe(template, 0, materialObject, required, Rounding.WHOLE_OUTPUTS);
        rule.source(plate, prefix.getMaterialWeight());
        rule.publish(MachineRecipeMaps.Welder);
        return 1;
    }
}
