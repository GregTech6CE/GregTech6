package gregtech.content.recipe;

import gregtech.internal.machine.crucible.CrucibleMath;
import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.material.MaterialComponent;
import gregtech.internal.material.MaterialProperty;
import gregtech.internal.material.MaterialStackItemHelper;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeMap;
import gregtech.bootstrap.InitializationStep;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTTechnological;
import gregtech.util.OM;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static gregtech.api.material.GTValues.U;

/**
 * GT6 {@code Loader_Recipes_Decomp}: generic decomposition of a material into its components.
 *
 * <p>Admission requires the original {@link MaterialProperty#DECOMPOSABLE} flag plus an explicit
 * {@link MaterialProperty#CENTRIFUGE} and/or {@link MaterialProperty#ELECTROLYSER} permission;
 * composition shape never grants a machine route. Components at or below ambient temperature are
 * emitted through their registered fluid, everything else through the component's pulverization
 * target. A resolvable fluid form suppresses the dust/block-dust entries for that map, and
 * electrolyzer rows retain the original selector-zero gate.</p>
 */
public class DecompositionRecipeLoader implements InitializationStep {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** GT6 {@code CS.DEF_ENV_TEMP} = C + 20. */
    private static final long ENV_TEMP = 293;

    private int added;
    private int skipped;
    private int recipes;

    @Override
    public void run() {
        for (GTMaterial entry : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = entry.resolve();
            if (material != entry || !material.isValid()) continue;
            if (!material.hasComposition() || !material.has(MaterialProperty.DECOMPOSABLE)) continue;
            boolean centrifuge = material.has(MaterialProperty.CENTRIFUGE);
            boolean electrolyzer = material.has(MaterialProperty.ELECTROLYSER);
            if (!centrifuge && !electrolyzer) continue;
            long divider = material.getCompositionDivider();
            if (divider <= 0 || divider > 64) continue;
            if (material.has(MaterialProperty.ANTIMATTER)) continue;

            long totalAmount = 0;
            List<ItemStack> dustOutputs = new ArrayList<>();
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
                if (!dust.isEmpty()) dustOutputs.add(dust);
            }
            if (dustOutputs.isEmpty() && fluidOutputs.isEmpty()) {
                skipped++;
                continue;
            }

            long eut = Math.max(16L, CrucibleMath.units(totalAmount, U, 14, false));
            long duration = Math.max(1L, CrucibleMath.units(totalAmount, U, 292, true));
            boolean any = false;
            if (centrifuge) any |= addMap(MachineRecipeMaps.Centrifuge, material, divider, eut, duration, dustOutputs, fluidOutputs);
            if (electrolyzer) any |= addMap(MachineRecipeMaps.Electrolyzer, material, divider, eut, duration, dustOutputs, fluidOutputs);
            if (any) added++; else {
                skipped++;
                LOGGER.info("[gregtech] Decomposition candidate {} ({}) has no resolvable input",
                        material.getName(), divider);
            }
        }
        LOGGER.info("[gregtech] Decomposition recipes: {} materials, {} recipes, {} skipped", added, recipes, skipped);
    }

    private boolean addMap(RecipeMap map, GTMaterial material, long divider, long eut, long duration,
                           List<ItemStack> dustOutputs, List<FluidStack> fluidOutputs) {
        boolean addedAny = false;
        boolean fluidInputAdded = false;
        if (map.mInputFluidCount > 0) {
            FluidStack input = fluidOf(material, divider * U);
            if (input != null) {
                fluidInputAdded = true;
                addedAny |= add(map, null, input, dustOutputs, fluidOutputs, eut, duration);
            }
        }

        // GT6 suppresses the solid fallback once a fluid form was accepted; Glowstone keeps both.
        if ((!fluidInputAdded || hasGlowstoneReRegistration(material)) && map.mInputItemsCount > 0) {
            ItemStack dust = MaterialStackItemHelper.mat(MaterialPrefix.dust, material, (int) divider);
            if (!dust.isEmpty()) addedAny |= add(map, dust, null, dustOutputs, fluidOutputs, eut, duration);
            if (divider % 9 == 0) {
                ItemStack block = GTBlocks.getStack(BlockMaterialPrefix.blockDust, material);
                if (!block.isEmpty()) {
                    block.setCount((int) (divider / 9));
                    addedAny |= add(map, block, null, dustOutputs, fluidOutputs, eut, duration);
                }
            }
        }
        return addedAny;
    }

    private boolean add(RecipeMap map, ItemStack itemInput, FluidStack fluidInput,
                        List<ItemStack> dustOutputs, List<FluidStack> fluidOutputs, long eut, long duration) {
        ItemStack[] items = itemInput == null ? RecipeMap.ZL_IS : new ItemStack[]{itemInput};
        FluidStack[] fluids = fluidInput == null ? RecipeMap.ZL_FS : new FluidStack[]{fluidInput};
        // Original GT6 uses map-sized arrays: preserve the ordered prefix and truncate the tail.
        // Apply this before make() optimization, for both decomposition maps and all input forms.
        ItemStack[] outputs = Arrays.copyOf(dustOutputs.toArray(RecipeMap.ZL_IS), map.mOutputItemsCount);
        FluidStack[] fluidResults = Arrays.copyOf(fluidOutputs.toArray(RecipeMap.ZL_FS), map.mOutputFluidCount);
        Recipe recipe = RecipeMap.make(true, items, outputs,
                null, RecipeMap.ZL_LONG, fluids, fluidResults, duration, eut, 0);
        if (recipe == null) return false;

        if (map == MachineRecipeMaps.Electrolyzer) {
            var selector = GTTechnological.selectorTag(0);
            if (selector == null) return false;
            // Apply the presence-only selector after make() so common-factor reduction keeps working.
            ItemStack[] gated = new ItemStack[recipe.mInputs.length + 1];
            gated[0] = new ItemStack(selector);
            System.arraycopy(recipe.mInputs, 0, gated, 1, recipe.mInputs.length);
            recipe = new Recipe(gated, recipe.mOutputs, recipe.mSpecialItems, recipe.mChances,
                    recipe.mFluidInputs, recipe.mFluidOutputs, recipe.mDuration, recipe.mEUt,
                    recipe.mSpecialValue).withCatalystInputs(0);
        }

        if (map.addRecipe(recipe) == null) return false;
        recipes++;
        return true;
    }

    /**
     * Component phase contract: GT6 asks the material for its fluid form at ambient temperature.
     * This resolves the port's registered gas, liquid, named, and molten forms with the original
     * 1000 mB/U and 144 mB/U scales, including vanilla water.
     */
    private static FluidStack fluidOf(GTMaterial material, long amountU) {
        long mb = amountU * 1000L / U;
        if (mb <= 0 || mb > Integer.MAX_VALUE) return null;
        // Prefer the material's dedicated registered fluid so a gas-family alias such as Saltwater
        // does not resolve to GenGas while the actual binding is the dedicated liquid.
        FluidStack dedicated = GeneratedRecipeResolver.namedFluid(material.getName(), (int) mb);
        if (dedicated != null && !dedicated.isEmpty()) return dedicated;
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
