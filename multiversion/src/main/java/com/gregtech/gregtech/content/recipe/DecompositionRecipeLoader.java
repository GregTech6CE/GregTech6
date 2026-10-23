package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.machine.crucible.CrucibleMath;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialComponent;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.bootstrap.InitializationStep;
import com.gregtech.gregtech.content.recipe.generated.GeneratedRecipeResolver;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.util.OM;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.gregtech.gregtech.api.material.GTValues.U;

/**
 * GT6 {@code Loader_Recipes_Decomp}: generic decomposition of a material into its components.
 *
 * <p>Admission and map membership come from the {@link GregTechTags} contracts carried from
 * {@code MT.java}, not from a composition-shape heuristic: a material needs
 * {@link GregTechTags.Compounds#DECOMPOSABLE} plus {@link GregTechTags.Processing#CENTRIFUGE} or
 * {@link GregTechTags.Processing#ELECTROLYSER} to receive a generic recipe. Components at or below
 * ambient temperature are emitted through their registered fluid, everything else through the
 * component's pulverization target. Inputs follow the original fallback: a resolvable fluid form
 * suppresses the dust/block-dust entries for that map.</p>
 */
public class DecompositionRecipeLoader implements InitializationStep {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** GT6 {@code CS.DEF_ENV_TEMP} = C + 20. */
    private static final long ENV_TEMP = 293;

    private int added;
    private int skipped;

    @Override
    public void run() {
        for (GTMaterial entry : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = entry.resolve();
            if (material != entry || !material.isValid()) continue;
            if (!material.hasComposition()) continue;
            if (!material.hasTag(GregTechTags.Compounds.DECOMPOSABLE)) continue;

            long divider = material.getCompositionDivider();
            if (divider <= 0 || divider > 64) continue;

            List<RecipeMap> maps = mapsFor(material);
            if (maps.isEmpty()) continue;

            long totalAmount = 0;
            List<ItemStack> itemOutputs = new ArrayList<>();
            List<FluidStack> fluidOutputs = new ArrayList<>();
            for (MaterialComponent component : material.getCompositionComponents()) {
                totalAmount += component.amount();
                GTMaterial part = component.material().resolve();
                if (!part.isValid()) continue;
                if (part.getMeltingPoint() <= ENV_TEMP) {
                    FluidStack fluid = fluidOf(part, component.amount());
                    if (fluid != null) {
                        fluidOutputs.add(fluid);
                        continue;
                    }
                }
                GTMaterial pulver = part.getTargetPulverMaterial();
                long pulverAmount = pulver.getTargetPulverAmount();
                if (pulverAmount <= 0) continue;
                ItemStack dust = OM.dust(pulver, CrucibleMath.units(component.amount(), U, pulverAmount, false));
                if (!dust.isEmpty()) itemOutputs.add(dust);
            }
            if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
                skipped++;
                continue;
            }

            long eut = Math.max(16L, CrucibleMath.units(totalAmount, U, 14, false));
            long duration = Math.max(1L, CrucibleMath.units(totalAmount, U, 292, true));

            boolean any = false;
            for (RecipeMap map : maps) {
                any |= addMapRecipes(map, material, divider, eut, duration, itemOutputs, fluidOutputs);
            }
            if (any) added++; else skipped++;
        }
        LOGGER.info("[gregtech] Decomposition recipes: {} materials added, {} skipped", added, skipped);
    }

    private static List<RecipeMap> mapsFor(GTMaterial material) {
        List<RecipeMap> maps = new ArrayList<>(2);
        if (material.hasTag(GregTechTags.Processing.CENTRIFUGE)) maps.add(MachineRecipeMaps.Centrifuge);
        if (material.hasTag(GregTechTags.Processing.ELECTROLYSER)) maps.add(MachineRecipeMaps.Electrolyzer);
        return maps;
    }

    private static boolean addMapRecipes(RecipeMap map, GTMaterial material, long divider, long eut, long duration,
                                         List<ItemStack> itemOutputs, List<FluidStack> fluidOutputs) {
        boolean added = false;
        boolean fluidInputAdded = false;

        if (map.mInputFluidCount > 0) {
            FluidStack input = fluidOf(material, divider * U);
            if (input != null) {
                fluidInputAdded = true;
                added |= submit(map, null, new FluidStack[]{input}, fluidOutputs, itemOutputs, eut, duration);
            }
        }

        // GT6 suppresses the solid fallback once a fluid form was accepted; Glowstone keeps both.
        if ((!fluidInputAdded || hasGlowstoneReRegistration(material)) && map.mInputItemsCount > 0) {
            ItemStack dust = MaterialStackItemHelper.mat(MaterialPrefix.dust, material, (int) divider);
            if (!dust.isEmpty()) {
                added |= submit(map, dust, RecipeMap.ZL_FS, fluidOutputs, itemOutputs, eut, duration);
            }
            if (divider % 9 == 0) {
                ItemStack block = GTBlocks.getStack(BlockMaterialPrefix.blockDust, material);
                if (!block.isEmpty()) {
                    block.setCount((int) (divider / 9));
                    added |= submit(map, block, RecipeMap.ZL_FS, fluidOutputs, itemOutputs, eut, duration);
                }
            }
        }
        return added;
    }

    private static boolean submit(RecipeMap map, ItemStack itemInput, FluidStack[] fluidInputs,
                                  List<FluidStack> fluidOutputs, List<ItemStack> itemOutputs,
                                  long eut, long duration) {
        FluidStack[] fluidsOut = fluidOutputs.toArray(RecipeMap.ZL_FS);
        ItemStack[] itemsOut = itemOutputs.toArray(RecipeMap.ZL_IS);
        Recipe recipe = RecipeMap.make(true,
                itemInput == null ? RecipeMap.ZL_IS : new ItemStack[]{itemInput},
                itemsOut, null, RecipeMap.ZL_LONG, fluidInputs, fluidsOut,
                duration, eut, 0);
        // GT6 Loader_Recipes_Decomp passes optimize/checkCollisions = true, fake/hidden/logErrors = false.
        return map.addRecipe(recipe, true, false, false, false) != null;
    }

    /**
     * Component phase contract: GT6 asks the material for its fluid form at ambient temperature.
     * The port keeps one registered fluid binding per material, so this resolves gas, liquid,
     * named, or molten forms (with the original 1000 mB/U and 144 mB/U scales).
     */
    private static FluidStack fluidOf(GTMaterial material, long amountU) {
        long mb = amountU * 1000L / U;
        if (mb <= 0 || mb > Integer.MAX_VALUE) return null;
        FluidStack stack = GeneratedRecipeResolver.materialFluid(material.getName(), (int) mb);
        return stack == null || stack.isEmpty() ? null : stack;
    }

    private static boolean hasGlowstoneReRegistration(GTMaterial material) {
        for (GTMaterial reRegistration : material.getReRegistrations()) {
            if ("Glowstone".equals(reRegistration.getName())) return true;
        }
        return false;
    }
}
