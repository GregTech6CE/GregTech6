package gregtech.content.recipe;

import gregtech.content.recipe.BatteryCellRecipeRows.Fill;


import gregtech.engine.recipe.Recipe;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.registry.GTTechnological;
import gregtech.platform.Mc;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6's five filled battery cells: the empty cell is canned together with its electrolyte.
 *
 * <p>GT6 declares the empty cells as ordinary shaped recipes
 * ({@code MultiItemTechnological:464,469,474,479,484}) and marks them
 * <em>"Battery Part (doesn't require Canning Machine!)"</em>, while each filled cell carries a
 * {@code FluidContainerData} which makes the Canning Machine able to
 * fill it:</p>
 *
 * <pre>
 * lead-acid H2SO4 2000 mB ({@code MT.H2SO4.liquid(2*U, T)})
 * alkaline distilled water 1000 mB ({@code FL.DistW.make(1000)})
 * nickel-cadmium distilled water 1000 mB
 * lithium-cobalt HCl gas 2000 mB ({@code MT.HCl.gas(U*2, T)})
 * lithium-manganese HF gas 2000 mB ({@code MT.HF.gas(U*2, T)})
 * </pre>
 *
 * <p>GT6 produces these rows from the fluid-container registry ({@code OreDictManager:296-300} plus
 * {@code FL.set}), so the machine work comes from the Canning Machine's generic container pass —
 * {@code RM.java:743-746} shows it as {@code Canner.addRecipe2(T, 16, 16, …)}: 16 EU/t for 16 ticks.
 * The port expresses the same conversion as explicit rows because its battery cells are plain items
 * rather than registered fluid containers.</p>
 */
public final class BatteryCellRecipes {

    /** GT6 empty cell -> filled cell plus the electrolyte its {@code FluidContainerData} carries. */


    private static final List<Fill> FILLS = BatteryCellRecipeRows.FILLS;

    private static final List<String> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = new ArrayList<>();

    private static boolean registered;

    private BatteryCellRecipes() {}

    public static List<String> entries() { return Collections.unmodifiableList(ENTRIES); }

    public static List<String> skipped() { return Collections.unmodifiableList(SKIPPED); }

    public static int register() {
        if (registered) throw new IllegalStateException("Battery cell recipes registered twice");
        registered = true;
        for (Fill fill : FILLS) {
            ItemStack empty = item(fill.empty());
            ItemStack filled = item(fill.filled());
            if (empty.isEmpty() || filled.isEmpty()) {
                SKIPPED.add(fill.source() + ": the port registers no " + fill.empty() + "/" + fill.filled());
                continue;
            }
            FluidStack fluid = electrolyte(fill.fluid(), fill.mb());
            if (fluid == null) {
                SKIPPED.add(fill.source() + ": the port registers no " + fill.fluid() + " fluid");
                continue;
            }
            // RM.java:743-746 - canning runs at 16 EU/t for 16 ticks
            Recipe recipe = MachineRecipeMaps.Canner.addRecipe(new Recipe(
                    new ItemStack[]{empty.copy()}, new ItemStack[]{filled}, null, null,
                    new FluidStack[]{fluid}, null, 16, 16, 0));
            if (recipe != null) ENTRIES.add(fill.empty() + " + " + fluid.getAmount() + " mB "
                    + fluid.getFluid().getFluidType().getDescriptionId() + " -> " + fill.filled());
        }
        Mc.logger().info("Registered {} GT6 battery cell filling recipes ({} skipped: {})",
                ENTRIES.size(), SKIPPED.size(), SKIPPED);
        return ENTRIES.size();
    }

    /** The electrolyte's registered liquid phase, then gas if that is the material's sole phase. */
    private static FluidStack electrolyte(String materialName, int mb) {
        var material = gregtech.engine.material.GTMaterialRegistry.get(materialName);
        if (material == null || !material.resolve().isValid()) return null;
        for (var phase : new gregtech.engine.recipe.chem.FluidPhase[]{
                gregtech.engine.recipe.chem.FluidPhase.LIQUID,
                gregtech.engine.recipe.chem.FluidPhase.GAS}) {
            var binding = gregtech.engine.recipe.chem.MaterialFluidBinding
                    .find(material.resolve().getName(), phase).orElse(null);
            if (binding != null) return gregtech.content.registry.GTFluids.stack(binding.field(), mb);
        }
        return null;
    }

    private static ItemStack item(String id) {
        Item item = GTTechnological.get(id);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
