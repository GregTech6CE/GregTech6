package gregtech.engine.recipe;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import gregtech.api.addon.recipe.RecipeKeptInputs;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Plans consumption on copies, including repeated ingredients and split stacks/tanks. */
public final class RecipeInputs {
    private RecipeInputs() {}
    public record Remaining(List<ItemStack> items, List<FluidStack> fluids) {}
    public static Remaining consume(Recipe recipe, List<ItemStack> items, List<FluidStack> fluids, int parallel) {
        var matcher = new Matcher(items, fluids);
        if (!matcher.matches(recipe, parallel)) return null;
        var remainingItems = new ArrayList<ItemStack>(items.size());
        for (int slot = 0; slot < items.size(); slot++) {
            remainingItems.add(items.get(slot).copyWithCount(matcher.itemCounts[slot] + matcher.retainedItems[slot]));
        }
        var remainingFluids = new ArrayList<FluidStack>(fluids.size());
        for (int tank = 0; tank < fluids.size(); tank++) {
            var copy = fluids.get(tank).copy();
            if (!copy.isEmpty()) copy.setAmount(matcher.fluidCounts[tank] + matcher.retainedFluids[tank]);
            remainingFluids.add(copy);
        }
        return new Remaining(remainingItems, remainingFluids);
    }

    /** Reusable count workspace: failed candidates never copy stacks or their NBT/components. */
    public static final class Matcher {
        private final List<ItemStack> items;
        private final List<FluidStack> fluids;
        private final int[] itemCounts, fluidCounts, retainedItems, retainedFluids;

        public Matcher(List<ItemStack> items, List<FluidStack> fluids) {
            this.items = items;
            this.fluids = fluids;
            itemCounts = new int[items.size()];
            fluidCounts = new int[fluids.size()];
            retainedItems = new int[items.size()];
            retainedFluids = new int[fluids.size()];
        }

        public boolean matches(Recipe recipe, int parallel) {
            if (parallel < 1) return false;
            for (int i = 0; i < itemCounts.length; i++) itemCounts[i] = items.get(i).getCount();
            for (int i = 0; i < fluidCounts.length; i++) fluidCounts[i] = fluids.get(i).getAmount();
            Arrays.fill(retainedItems, 0);
            Arrays.fill(retainedFluids, 0);
            // Reserve catalysts alongside consumables; restore them only in the final consumption plan.
            for (int input = 0; input < recipe.mInputs.length; input++) {
                var required = recipe.mInputs[input];
                if (required == null || required.isEmpty()) continue;
                boolean catalyst = recipe.isCatalystInput(input);
                long needed = (long) required.getCount() * (catalyst ? 1 : parallel);
                for (int slot = 0; slot < items.size(); slot++) {
                    var available = items.get(slot);
                    if (itemCounts[slot] <= 0 || !RecipeInputs.matches(required, available)) continue;
//? if >=1.21.1 {
                    /*if (recipe.mExactItemInputs && !ItemStack.isSameItemSameComponents(required, available)) continue;
*///?} else {
                    if (recipe.mExactItemInputs && !ItemStack.isSameItemSameTags(required, available)) continue;
//?}
                    if (recipe.mRequiresEmptyContainerInputs && gregtech.engine.material.ItemMaterialRegistry.hasStoredContents(available)) continue;
                    if (recipe.mMaterialRecovery && (available.isDamaged() || !gregtech.engine.material.ItemMaterialRegistry.canRecover(available))) continue;
                    int take = (int) Math.min(needed, itemCounts[slot]);
                    itemCounts[slot] -= take;
                    if (catalyst) retainedItems[slot] += take;
                    needed -= take;
                    if (needed == 0) break;
                }
                if (needed != 0) return false;
            }
            for (int input = 0; input < recipe.mFluidInputs.length; input++) {
                var required = recipe.mFluidInputs[input];
                if (required == null) continue;
                // Presence tests the original tanks, including when a preceding input consumes all fluid.
                if (recipe.isPresenceFluidInput(input)) {
                    if (required.isEmpty()) return false;
                    boolean present = false;
                    for (var available : fluids) {
                        if (!available.isEmpty() && sameFluid(available, required)) { present = true; break; }
                    }
                    if (!present) return false;
                    continue;
                }
                if (required.isEmpty()) continue;
                boolean catalyst = recipe.isCatalystFluidInput(input);
                long needed = RecipeKeptInputs.fluidDemand(catalyst, required.getAmount(), parallel);
                for (int tank = 0; tank < fluids.size(); tank++) {
                    if (fluidCounts[tank] <= 0 || !sameFluid(fluids.get(tank), required)) continue;
                    int take = (int) Math.min(needed, fluidCounts[tank]);
                    fluidCounts[tank] -= take;
                    if (catalyst) retainedFluids[tank] += take;
                    needed -= take;
                    if (needed == 0) break;
                }
                if (needed != 0) return false;
            }
            return true;
        }
    }
    public static boolean sameFluid(FluidStack available, FluidStack required) {
//? if >=1.21.1 {
        /*return FluidStack.isSameFluidSameComponents(available, required);
*///?} else {
        return available.isFluidEqual(required);
//?}
    }

    public static boolean matches(ItemStack required, ItemStack available) {
        if(available.isEmpty())return false;
        // Legacy palette items and their fixed-color replacements denote the same input.
        // Normalize the offered identity too; expanding only the requirement rejects its bare alias.
        var canonical = gregtech.content.block.misc.LegacyConstructionBlockItem.canonical(available);
        if (canonical != available) return matches(required, canonical);
        var colors = gregtech.content.block.misc.LegacyConstructionBlockItem.ingredientAlternatives(required);
        if (!colors.isEmpty()) return colors.stream().anyMatch(color -> matches(color, available));
        if(!required.is(available.getItem())) return gregtech.engine.material.MaterialEquivalence.matches(required,available);
        // An unfilled vessel in a recipe denotes its empty shell. Never silently
        // destroy stored fluid in recycling/crafting by treating absent NBT as a wildcard.
//? if >=1.21.1 {
        /*if(required.getItem() instanceof gregtech.engine.inventory.ContainerShapeLike
                &&net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(required).isEmpty()
                &&net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(available).isPresent())return false;
        return required.getComponentsPatch().isEmpty() || ItemStack.isSameItemSameComponents(required, available);
*///?} else {
        if(required.getItem() instanceof gregtech.content.item.PortableFluidContainerItem
                &&net.minecraftforge.fluids.FluidUtil.getFluidContained(required).isEmpty()
                &&net.minecraftforge.fluids.FluidUtil.getFluidContained(available).isPresent())return false;
        return !required.hasTag() || ItemStack.isSameItemSameTags(required, available);
//?}
    }
}
