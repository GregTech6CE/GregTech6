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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
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
public final class DecompositionRecipeLoader implements InitializationStep {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final java.util.Map<RecipeMap, Set<Recipe>> OWNED = new IdentityHashMap<>();

    private static int added;
    private static int skipped;
    private static int recipes;
    private static int bindingIssues;

    @Override
    public void run() {
        rebuild();
    }

    /** Recipes owned by the decomposition producer in the current overlay generation. */
    public static synchronized Set<Recipe> owned() {
        var result = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        for (var rows : OWNED.values()) result.addAll(rows);
        return Set.copyOf(result);
    }

    /** Removes this producer's rows and diagnostics before the next generation is built. */
    public static synchronized void clear() {
        for (var entry : OWNED.entrySet()) entry.getKey().removeAll(entry.getValue());
        OWNED.clear();
        added = 0;
        skipped = 0;
        recipes = 0;
        bindingIssues = 0;
    }

    /** Rebuilds all decomposition rows at a recipe-overlay transaction boundary. */
    public static synchronized int rebuild() {
        clear();
        try {
            new DecompositionRecipeLoader().build();
            LOGGER.info(
                    "[gregtech] Decomposition recipes: {} materials, {} recipes, {} skipped, {} binding issues",
                    added,
                    recipes,
                    skipped,
                    bindingIssues);
            return recipes;
        } catch (RuntimeException | Error failure) {
            clear();
            throw failure;
        }
    }

    private void build() {
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
        if (plan.droppedItemOutputs() > 0 || plan.droppedFluidOutputs() > 0)
            LOGGER.warn(
                    "[gregtech] Decomposition {} for {} truncated {} item and {} fluid outputs to map capacity",
                    route,
                    material.getName(),
                    plan.droppedItemOutputs(),
                    plan.droppedFluidOutputs());
        List<ItemStack> itemOutputs = nativeItemOutputs(plan);
        List<FluidStack> fluidOutputs = nativeFluidOutputs(plan);
        if (itemOutputs == null || fluidOutputs == null) {
            bindingIssues++;
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
                    if (input.declared()) bindingIssues++;
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
            if (!target.isValid()) {
                LOGGER.warn(
                        "[gregtech] Decomposition {} for {} cannot resolve item output {}",
                        plan.route(),
                        plan.material().name(),
                        output.material().name());
                return null;
            }
            ItemStack dust = OM.dust(target, output.units());
            if (dust.isEmpty()) {
                LOGGER.warn(
                        "[gregtech] Decomposition {} for {} cannot bind {}U of {}",
                        plan.route(),
                        plan.material().name(),
                        output.units(),
                        output.material().name());
                return null;
            }
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
            if (fluid == null || fluid.isEmpty()) {
                LOGGER.warn(
                        "[gregtech] Decomposition {} for {} cannot resolve {} output fluid {} ({} mB)",
                        plan.route(),
                        plan.material().name(),
                        output.phase(),
                        output.fluid(),
                        output.millibuckets());
                return null;
            }
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
        OWNED.computeIfAbsent(map, ignored -> Collections.newSetFromMap(new IdentityHashMap<>()))
                .add(recipe);
        recipes++;
        return true;
    }
}
