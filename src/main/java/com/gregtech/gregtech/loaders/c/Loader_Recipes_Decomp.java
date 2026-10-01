package com.gregtech.gregtech.loaders.c;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialComponent;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.util.OM;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.gregtech.gregtech.api.material.GTValues.U;

/**
 * Decomposition recipes from material composition data, ported from GT6
 * {@code Loader_Recipes_Decomp}.
 *
 * <p>Every material with composition data and a common divider ≤ 64 gets a
 * dust-decomposition recipe: Electrolyzer when all components are chemical elements
 * (GT6 {@code TD.Processing.ELECTROLYSER}), Centrifuge otherwise (mixtures,
 * {@code TD.Processing.CENTRIFUGE}). Components that are room-temperature
 * gases/liquids with a registered named fluid come out as fluids (1 U = 1000 mB);
 * everything else comes out as dust. Components with neither form abort the recipe
 * so no material is silently destroyed.</p>
 */
public class Loader_Recipes_Decomp implements IGTLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private int added, skipped;

    @Override
    public void run() {
        for (GTMaterial entry : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = entry.resolve();
            if (material != entry || !material.isValid()) continue;
            if (!material.hasComposition()) continue;
            long divider = material.getCompositionDivider();
            if (divider <= 0 || divider > 64) continue;
            if (material.has(MaterialProperty.ANTIMATTER)) continue;

            ItemStack input = MaterialStackItemHelper.mat(MaterialPrefix.dust, material, (int) divider);
            if (input.isEmpty()) continue;

            List<ItemStack> dustOutputs = new ArrayList<>();
            List<FluidStack> fluidOutputs = new ArrayList<>();
            long totalAmount = 0;
            boolean allElements = true;
            boolean complete = true;

            for (MaterialComponent component : material.getCompositionComponents()) {
                GTMaterial part = component.material().resolve();
                if (!part.isValid()) { complete = false; break; }
                totalAmount += component.amount();
                if (!part.has(MaterialProperty.ELEMENT)) allElements = false;

                FluidStack fluid = namedFluid(part, component.amount());
                if (fluid != null) {
                    fluidOutputs.add(fluid);
                    continue;
                }
                ItemStack dust = OM.dust(part, component.amount());
                if (!dust.isEmpty()) {
                    dustOutputs.add(dust);
                    continue;
                }
                complete = false;
                break;
            }

            if (!complete || (dustOutputs.isEmpty() && fluidOutputs.isEmpty())) { skipped++; continue; }

            RecipeMap map = allElements ? MachineRecipeMaps.Electrolyzer : MachineRecipeMaps.Centrifuge;
            long duration = Math.max(16, totalAmount * 14 / U);
            long eut = allElements ? 64 : 16;
            if (fluidOutputs.isEmpty()) {
                map.addRecipe1(true, eut, duration, input, dustOutputs.toArray(RecipeMap.ZL_IS));
            } else {
                map.addRecipe1(true, eut, duration, input,
                        RecipeMap.ZL_FS, fluidOutputs.toArray(RecipeMap.ZL_FS),
                        dustOutputs.toArray(RecipeMap.ZL_IS));
            }
            added++;
        }
        LOGGER.info("[gregtech] Decomposition recipes: {} added, {} skipped (incomplete outputs)", added, skipped);
    }

    /** Named FL fluid for gas/liquid components (1 U = 1000 mB), or null. */
    private static FluidStack namedFluid(GTMaterial material, long amountU) {
        if (!material.has(MaterialProperty.GAS) && !material.has(MaterialProperty.LIQUID)) return null;
        String key = material.getName();
        if (RegisteredFluids.get(key) == null) return null;
        RegistryObject<Fluid> fluid = GTFluids.still(key);
        if (fluid == null || !fluid.isPresent()) return null;
        long mb = amountU * 1000 / U;
        if (mb <= 0) return null;
        return new FluidStack(fluid.get(), (int) Math.min(Integer.MAX_VALUE, mb));
    }
}
