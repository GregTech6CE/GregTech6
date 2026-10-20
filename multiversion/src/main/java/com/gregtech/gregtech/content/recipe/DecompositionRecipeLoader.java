package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialComponent;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.bootstrap.InitializationStep;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.util.OM;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;
//?}
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.gregtech.gregtech.api.material.GTValues.U;

/**
 * Current decomposition registration from the shared material composition catalog.
 *
 * <p>This implementation requires a dust input and chooses a machine from the direct components'
 * ELEMENT flags. Named fluid outputs require GAS/LIQUID flags and a bound registered holder;
 * otherwise it requests dust. An unresolved component skips the entire submission. These rules,
 * work parameters and submission counts are audited separately against GT6's
 * {@code Loader_Recipes_Decomp}; they do not encode all of its processing or phase contracts.</p>
 */
public class DecompositionRecipeLoader implements InitializationStep {
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
                if (!part.isValid()) {
                    complete = false;
                    break;
                }
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

            if (!complete || (dustOutputs.isEmpty() && fluidOutputs.isEmpty())) {
                skipped++;
                continue;
            }

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
//? if >=1.21.1 {
        /*DeferredHolder<Fluid, ? extends Fluid> fluid = GTFluids.still(key);
        if (fluid == null || !fluid.isBound()) return null;
*///?} else {
        RegistryObject<Fluid> fluid = GTFluids.still(key);
        if (fluid == null || !fluid.isPresent()) return null;
//?}
        long mb = amountU * 1000 / U;
        if (mb <= 0) return null;
        return new FluidStack(fluid.get(), (int) Math.min(Integer.MAX_VALUE, mb));
    }
}
