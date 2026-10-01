package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.util.OM;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * GT6's crucible smelting recipes (original {@code RecipeMapCrucible#getRecipeFor} and
 * {@code #getNEIRecipes}).
 * <p>
 * In GT6 {@code RM.CrucibleSmelting} is a {@code RecipeMapCrucible}: instead of stored recipes it
 * derives, for any item carrying material data,
 * <pre>
 *   input form of material M  →  OM.ingotOrDust(M.mTargetSmelting, converted amount)
 *   special value (temperature) = M.mMeltingPoint
 * </pre>
 * and {@code getNEIRecipes} lists those forms in the recipe viewer. The smelting crucible machine
 * is registered for both crucible maps
 * ({@code RM.CrucibleSmelting.mRecipeMachineList.addAll(RM.CrucibleAlloying.mRecipeMachineList)},
 * {@code Loader_MultiTileEntities:294}), so its tab is what tells a player at which temperature a
 * material melts and what it turns into — including the iron chain, where
 * {@code Pig Iron → Wrought Iron} needs 2011 K.
 * </p>
 * <p>
 * The entries are display-only: the crucible block entity performs the same conversion locally from
 * {@link GTMaterial#getTargetSmeltingMaterial()} (see {@code SmeltingCrucibleBlockEntity}), exactly
 * like the original tile entity, which never consults the map either. They must not be executable,
 * or a machine would "melt" items without any heat.
 * </p>
 */
public final class CrucibleSmeltingRecipes {
    private CrucibleSmeltingRecipes() {}

    /**
     * Item forms GT6's {@code getNEIRecipes} lists. GT6 knows more of them (blockIngot, blockGem,
     * blockDust, chunk, rubble, pebbles, cluster, cleanGravel, dirtyGravel, crystalline, reduced);
     * this port has no items for those, so only the forms that exist are emitted.
     */
    private static final MaterialPrefix[] FORMS = {
            MaterialPrefix.dust, MaterialPrefix.crushed, MaterialPrefix.crushedPurified,
            MaterialPrefix.crushedCentrifuged, MaterialPrefix.oreRaw,
    };
    /** Forms GT6 only lists when the input material is not its own smelting target. */
    private static final MaterialPrefix[] SOLID_FORMS = {
            MaterialPrefix.ingot, MaterialPrefix.gem,
    };

    private static final TreeSet<String> MISSING = new TreeSet<>();

    /** Forms that could not be resolved to an item (recorded, never invented). */
    public static TreeSet<String> missingContent() { return new TreeSet<>(MISSING); }

    /** Registers the display recipes and returns how many were added. */
    public static int register() {
        MISSING.clear();
        int count = 0;
        for (GTMaterial raw : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = raw.resolve();
            if (!material.isValid() || !material.has(MaterialProperty.MELTING)) continue;
            GTMaterial target = material.getTargetSmeltingMaterial();
            if (target == null || !target.isValid()) continue;
            long targetAmount = material.getTargetSmeltingAmount();
            if (targetAmount <= 0) continue;

            long temperature = material.getMeltingPoint();
            if (temperature <= 0) continue;

            // GT6 lists the processing forms always and the solid forms only for the materials that
            // are actually transformed into their smelting target (Pig Iron → Wrought Iron, ores, …).
            boolean transformed = target != material;
            List<MaterialPrefix> forms = new ArrayList<>(List.of(FORMS));
            if (transformed) forms.addAll(List.of(SOLID_FORMS));

            for (MaterialPrefix prefix : forms) {
                ItemStack input = GTItems.getStack(prefix, material, 1);
                if (input.isEmpty()) continue;
                // GT6 UT.Code.units(inputAmount, U, targetAmount, false).
                long amount = Math.max(1, prefix.getMaterialWeight() * targetAmount / GTValues.U);
                ItemStack output = OM.ingotOrDust(target, amount);
                if (output.isEmpty()) {
                    MISSING.add(prefix.getName() + " of " + target.getName());
                    continue;
                }
                if (MachineRecipeMaps.CrucibleSmelting.addFakeRecipe(false,
                        new ItemStack[]{input}, new ItemStack[]{output}, null, null, null, null,
                        0, 0, temperature) != null) {
                    count++;
                }
            }
        }
        return count;
    }
}
