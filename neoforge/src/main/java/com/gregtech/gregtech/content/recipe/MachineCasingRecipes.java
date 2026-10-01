package com.gregtech.gregtech.content.recipe;


import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code Loader_Recipes_Handlers:349-358} — machine casings welded from six plates plus two long
 * rods (or four rods) in the Welder, in four grades
 * ({@code casingMachine}, {@code ...Double}, {@code ...Quadruple}, {@code ...Dense}).
 * <p>
 * Conditions are GT6's: {@code SMITHABLE} and {@code FLAMMABLE.NOT}, with the fixed easy-heatable
 * duration (16 ticks per material unit) for {@code tEasyHeatable = Or(FURNACE)} materials and the
 * quality-scaled multiplier 64 for the rest. The flags come from
 * {@code tools/extract_gt6_workability.py}. The previous implementation hard-coded twenty material
 * names with a hand-picked easy/hard split, so every other smithable material had no casing recipe.
 * </p>
 */
public final class MachineCasingRecipes {
    /** GT6's four casing grades with their plate prefix and material units (:349-358). */
    private record Grade(MaterialPrefix plates, BlockMaterialPrefix casing, long units) {}

    private static final Grade[] GRADES = {
            new Grade(MaterialPrefix.plate, BlockMaterialPrefix.casingMachine, 8),
            new Grade(MaterialPrefix.plateDouble, BlockMaterialPrefix.casingMachineDouble, 14),
            new Grade(MaterialPrefix.plateQuadruple, BlockMaterialPrefix.casingMachineQuadruple, 26),
            new Grade(MaterialPrefix.plateDense, BlockMaterialPrefix.casingMachineDense, 56)};

    private MachineCasingRecipes() {}

    public static int register() {
        int count = 0;
        int materials = 0;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.has(MaterialProperty.ANTIMATTER) || material.has(MaterialProperty.FLAMMABLE)) continue;
            if (!material.has(MaterialProperty.SMITHABLE)) continue;
            int before = count;
            for (Grade grade : GRADES) {
                count += casing(material, grade, true);
                count += casing(material, grade, false);
            }
            if (count > before) materials++;
        }
        com.mojang.logging.LogUtils.getLogger().info("Registered {} GT6 machine casing welds for {} materials", count, materials);
        return count;
    }

    private static int casing(GTMaterial material, Grade grade, boolean longRod) {
        ItemStack plates = GTItems.getStack(grade.plates(), material, 6);
        ItemStack rod = GTItems.getStack(longRod ? MaterialPrefix.stickLong : MaterialPrefix.stick, material,
                longRod ? 2 : 4);
        ItemStack output = GTBlocks.getStack(grade.casing(), material);
        if (plates.isEmpty() || rod.isEmpty() || output.isEmpty()) return 0;
        long ticks = MaterialWorkability.isFurnace(material)
                ? 16L * grade.units()                                        // easy pass: fixed duration
                : 64L * grade.units() * (material.getToolQuality() + 1);     // hard pass: multiplier 64
        Recipe recipe = MachineRecipeMaps.Welder.addRecipe(
                new Recipe(new ItemStack[]{plates, rod}, new ItemStack[]{output},
                        null, null, null, null, ticks, 16, 0));
        return recipe != null ? 1 : 0;
    }
}
