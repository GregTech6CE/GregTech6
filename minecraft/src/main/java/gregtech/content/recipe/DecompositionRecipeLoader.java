package gregtech.content.recipe;

import com.mojang.logging.LogUtils;
import gregtech.api.addon.material.MaterialDecompositionRule;
import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.material.GTMaterial;
import gregtech.bootstrap.InitializationStep;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTTechnological;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialStackItemHelper;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.MaterialDecompositionPlanner;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.util.OM;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import org.slf4j.Logger;

/**
 * Publishes the executable projection of each material's typed decomposition declaration.
 *
 * <p>This adapter resolves native items and fluids only. Permissions, phases, quantities, route
 * selection, and output ordering come from {@link MaterialDecompositionPlanner}.</p>
 */
public class DecompositionRecipeLoader implements InitializationStep {
    private static final Logger LOGGER = LogUtils.getLogger();

    private int added;
    private int skipped;
    private int recipes;

    @Override
    public void run() {
        for (GTMaterial entry : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = entry.resolve();
            if (material != entry || !material.isValid()) continue;
            var declared = GregTechMaterials.processes()
                    .findSingle(new MaterialKey(material.getName()), MaterialProcessKeys.DECOMPOSITION);
            if (declared.isEmpty()) continue;
            MaterialDecompositionRule rule = declared.get().value();
            boolean any = false;
            if (rule.routes().contains(MaterialDecompositionRule.Route.CENTRIFUGE))
                any |= publish(
                        MachineRecipeMaps.Centrifuge,
                        material,
                        rule,
                        MaterialDecompositionRule.Route.CENTRIFUGE);
            if (rule.routes().contains(MaterialDecompositionRule.Route.ELECTROLYSER))
                any |= publish(
                        MachineRecipeMaps.Electrolyzer,
                        material,
                        rule,
                        MaterialDecompositionRule.Route.ELECTROLYSER);
            if (any) added++;
            else skipped++;
        }
        LOGGER.info("[gregtech] Decomposition recipes: {} materials, {} recipes, {} skipped", added, recipes, skipped);
    }

    private boolean publish(
            RecipeMap map,
            GTMaterial material,
            MaterialDecompositionRule rule,
            MaterialDecompositionRule.Route route) {
        var result = MaterialDecompositionPlanner.plan(
                material, rule, route, map.mOutputItemsCount, map.mOutputFluidCount);
        if (!result.planned()) {
            LOGGER.warn(
                    "[gregtech] Decomposition {} for {}: {} ({})",
                    route,
                    material.getName(),
                    result.status(),
                    result.reason());
            return false;
        }
        var plan = result.plan();
        List<ItemStack> itemOutputs = nativeItemOutputs(plan);
        List<FluidStack> fluidOutputs = nativeFluidOutputs(plan);
        if (itemOutputs == null || fluidOutputs == null) {
            LOGGER.warn(
                    "[gregtech] Decomposition {} for {} has an unregistered native output binding",
                    route,
                    material.getName());
            return false;
        }

        boolean addedAny = false;
        boolean fluidInputAdded = false;
        if (map.mInputFluidCount > 0) {
            for (var input : plan.fluidInputs()) {
                FluidStack fluid = DecompositionNativeAdapter.fluid(
                        input.fluid(), input.millibuckets(), input.declared(), material.getName());
                if (fluid == null || fluid.isEmpty()) {
                    if (input.declared())
                        LOGGER.warn(
                                "[gregtech] Decomposition {} for {} references unregistered input fluid {}",
                                route,
                                material.getName(),
                                input.fluid());
                    continue;
                }
                fluidInputAdded = true;
                addedAny |= add(map, null, fluid, itemOutputs, fluidOutputs, plan);
            }
        }

        if ((!fluidInputAdded || rule.keepSolidInput()) && map.mInputItemsCount > 0) {
            ItemStack dust = MaterialStackItemHelper.mat(MaterialPrefix.dust, material, (int) plan.divider());
            if (!dust.isEmpty()) addedAny |= add(map, dust, null, itemOutputs, fluidOutputs, plan);
            if (plan.blockDustCount() > 0) {
                ItemStack block = GTBlocks.getStack(BlockMaterialPrefix.blockDust, material);
                if (!block.isEmpty()) {
                    block.setCount(plan.blockDustCount());
                    addedAny |= add(map, block, null, itemOutputs, fluidOutputs, plan);
                }
            }
        }
        return addedAny;
    }

    private static List<ItemStack> nativeItemOutputs(MaterialDecompositionPlanner.Plan plan) {
        List<ItemStack> outputs = new ArrayList<>();
        for (var output : plan.itemOutputs()) {
            GTMaterial target = GTMaterialRegistry.get(output.material().name());
            if (!target.isValid()) return null;
            ItemStack dust = OM.dust(target, output.units());
            if (dust.isEmpty()) return null;
            outputs.add(dust);
        }
        return outputs;
    }

    private static List<FluidStack> nativeFluidOutputs(MaterialDecompositionPlanner.Plan plan) {
        List<FluidStack> outputs = new ArrayList<>();
        for (var output : plan.fluidOutputs()) {
            FluidStack fluid = DecompositionNativeAdapter.fluid(
                    output.fluid(),
                    output.millibuckets(),
                    output.declared(),
                    output.component().name());
            if (fluid == null || fluid.isEmpty()) return null;
            outputs.add(fluid);
        }
        return outputs;
    }

    private boolean add(
            RecipeMap map,
            ItemStack itemInput,
            FluidStack fluidInput,
            List<ItemStack> itemOutputs,
            List<FluidStack> fluidOutputs,
            MaterialDecompositionPlanner.Plan plan) {
        ItemStack[] items = itemInput == null ? RecipeMap.ZL_IS : new ItemStack[] {itemInput};
        FluidStack[] fluids = fluidInput == null ? RecipeMap.ZL_FS : new FluidStack[] {fluidInput};
        Recipe recipe = RecipeMap.make(
                true,
                items,
                itemOutputs.toArray(RecipeMap.ZL_IS),
                null,
                RecipeMap.ZL_LONG,
                fluids,
                fluidOutputs.toArray(RecipeMap.ZL_FS),
                plan.duration(),
                plan.eut(),
                0);
        if (recipe == null) return false;

        if (plan.electrolyserCatalyst()) {
            Item selector = GTTechnological.selectorTag(0);
            if (selector == null) return false;
            ItemStack[] gated = new ItemStack[recipe.mInputs.length + 1];
            gated[0] = new ItemStack(selector);
            System.arraycopy(recipe.mInputs, 0, gated, 1, recipe.mInputs.length);
            recipe = new Recipe(
                    gated,
                    recipe.mOutputs,
                    recipe.mSpecialItems,
                    recipe.mChances,
                    recipe.mFluidInputs,
                    recipe.mFluidOutputs,
                    recipe.mDuration,
                    recipe.mEUt,
                    recipe.mSpecialValue)
                    .withCatalystInputs(0);
        }

        if (map.addRecipe(recipe) == null) return false;
        recipes++;
        return true;
    }
}
