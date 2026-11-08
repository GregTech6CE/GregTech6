package gregtech.content.recipe;

import gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.generated.MaterialWorkability;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.MaterialUnitRecipe;
import gregtech.engine.recipe.Recipe;
import gregtech.platform.Mc;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code Loader_Recipes_Handlers} — machine casings welded from six plates plus two long
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
    /** GT6's four casing grades with their plate prefix and material units. */
    private record Grade(MaterialPrefix plates, BlockMaterialPrefix casing, long units) {}

    private static final Grade[] GRADES = {
        new Grade(MaterialPrefix.plate, BlockMaterialPrefix.casingMachine, 8),
        new Grade(MaterialPrefix.plateDouble, BlockMaterialPrefix.casingMachineDouble, 14),
        new Grade(MaterialPrefix.plateQuadruple, BlockMaterialPrefix.casingMachineQuadruple, 26),
        new Grade(MaterialPrefix.plateDense, BlockMaterialPrefix.casingMachineDense, 56)
    };

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
        Mc.logger().info("Registered {} GT6 machine casing welds for {} materials", count, materials);
        return count;
    }

    private static int casing(GTMaterial material, Grade grade, boolean longRod) {
        ItemStack plates = GTItems.getStack(grade.plates(), material, 6);
        ItemStack rod =
                GTItems.getStack(longRod ? MaterialPrefix.stickLong : MaterialPrefix.stick, material, longRod ? 2 : 4);
        ItemStack output = GTBlocks.getStack(grade.casing(), material);
        if (plates.isEmpty() || rod.isEmpty() || output.isEmpty()) return 0;
        long ticks = MaterialWorkability.isFurnace(material)
                ? 16L * grade.units() // easy pass: fixed duration
                : 64L * grade.units() * (material.getToolQuality() + 1); // hard pass: multiplier 64
        // The source handler scales the plate batch by the form's material units; the rod input
        // stays a template slot (equivalent rods are admitted by the ordinary matcher).
        var template = new Recipe(
                new ItemStack[] {plates, rod}, new ItemStack[] {output}, null, null, null, null, ticks, 16, 0);
        long required = Math.multiplyExact(grade.plates().getMaterialWeight(), 6);
        var rule = new MaterialUnitRecipe(template, 0, material, required, Rounding.WHOLE_OUTPUTS);
        rule.source(plates, grade.plates().getMaterialWeight());
        rule.publish(MachineRecipeMaps.Welder);
        return 1;
    }
}
