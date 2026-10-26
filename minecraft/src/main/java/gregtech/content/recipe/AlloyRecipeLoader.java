package gregtech.content.recipe;

import gregtech.engine.recipe.RecipeMap;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.bootstrap.InitializationStep;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTTechnological;
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

/** Registers the original alloy table using literal source fluid names and millibuckets. */
public class AlloyRecipeLoader implements InitializationStep {
    private static final Logger LOGGER = LogUtils.getLogger();
    private int added, skipped;

    @Override
    public void run() {
        added = skipped = 0;
        for (var row : SourceAlloyRecipes.smelter()) register(MachineRecipeMaps.Smelter, row);
        for (var row : SourceAlloyRecipes.mixer()) register(MachineRecipeMaps.Mixer, row);

        crucibleAlloyingDisplay();

        LOGGER.info("[gregtech] Alloy recipes: {} added, {} skipped (unresolved or rejected)", added, skipped);
    }

    /**
     * Informational recipes for the Combination Smelting (crucible alloying) JEI tab:
     * one fake recipe per ALLOY material with composition data, shown with physical ingots or dust where available.
     * The actual conversion happens inside {@code SmeltingCrucibleBlockEntity}.
     */
    private void crucibleAlloyingDisplay() {
        int shown = 0;
        for (var reaction : gregtech.engine.machine.crucible.CrucibleReactions.allRecipes()) {
            List<net.minecraft.world.item.ItemStack> inputs = new ArrayList<>();
            boolean complete = reaction.yield() > 0 && reaction.yield() <= 64;
            for (var part : reaction.parts()) {
                if (part.ratio() > 64) { complete = false; break; }
                var stack = CrucibleDisplayItems.units(part.material(), (int)part.ratio());
                if (stack.isEmpty()) { complete = false; break; }
                inputs.add(stack);
            }
            if (!complete || inputs.isEmpty()) continue;
            var output = CrucibleDisplayItems.units(reaction.output(), (int)reaction.yield());
            if (output.isEmpty()) continue;
            MachineRecipeMaps.CrucibleAlloying.addFakeRecipe(false, inputs.toArray(RecipeMap.ZL_IS),
                    new net.minecraft.world.item.ItemStack[]{output}, null, null, null, null, 0, 0, reaction.output().getMeltingPoint());
            shown++;
        }
        for (var item : new net.minecraft.world.item.Item[]{net.minecraft.world.item.Items.COAL, net.minecraft.world.item.Items.CHARCOAL}) {
            var carbon = CrucibleDisplayItems.units(gregtech.content.material.Materials.Carbon, 1);
            MachineRecipeMaps.CrucibleAlloying.addFakeRecipe(false,
                    new net.minecraft.world.item.ItemStack[]{new net.minecraft.world.item.ItemStack(item, 2)},
                    new net.minecraft.world.item.ItemStack[]{carbon}, null, null, null, null, 0, 0, 1700);
        }
        LOGGER.info("[gregtech] Crucible alloying display recipes: {}", shown);
    }

    private void register(RecipeMap map, SourceAlloyRecipes.Row row) {
        FluidStack result = sourceFluid(row.output());
        if (result == null) { skipped++; return; }
        FluidStack[] inputs = new FluidStack[row.inputs().size()];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = sourceFluid(row.inputs().get(i));
            if (inputs[i] == null) { skipped++; return; }
        }
        ItemStack[] itemInputs = RecipeMap.ZL_IS;
        if (row.selector() != 0) {
            var selector = GTTechnological.selectorTag(row.selector());
            if (selector == null) { skipped++; return; }
            itemInputs = new ItemStack[]{new ItemStack(selector, 1)};
        }
        // Native count-one selectors retain their identity. Every fixed source row has fluid gcd 1,
        // so this representation does not change the source's optimize=true coefficients or ticks.
        var recipe = RecipeMap.make(true, itemInputs, RecipeMap.ZL_IS, null, null,
                inputs, new FluidStack[]{result}, row.ticks(), SourceAlloyRecipes.POWER, 0);
        if (recipe == null) { skipped++; return; }
        if (row.selector() != 0) recipe.withCatalystInputs(0);
        // Keep normal collision rejection; never replace another producer's recipe.
        if (map.addRecipe(recipe) != null) added++;
        else skipped++;
    }

    /** Resolve the declared source fluid, not a material alias or a guessed GenMolten field. */
    private static FluidStack sourceFluid(SourceAlloyRecipes.Amount amount) {
        String path = RegisteredFluids.sanitizePath(amount.fluidName());
        FluidStack resolved = null;
        for (var field : RegisteredFluids.all().entrySet()) {
            if (!RegisteredFluids.sanitizePath(field.getValue().registryName()).equals(path)) continue;
            FluidStack candidate = GTFluids.stack(field.getKey(), amount.millibuckets());
            if (candidate == null || candidate.isEmpty()) continue;
            // Multiple catalog fields are safe only when they bind to the same native fluid.
            if (resolved != null && resolved.getFluid() != candidate.getFluid()) return null;
            resolved = candidate;
        }
        return resolved;
    }
}
