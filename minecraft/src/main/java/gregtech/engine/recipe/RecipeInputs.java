package gregtech.engine.recipe;

import gregtech.engine.stack.StackCompat;

import java.util.*;
import net.minecraft.world.item.ItemStack;
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
            if (recipe.hasTaggedInputs()) return tagged(recipe, parallel);
            // Presence requirements do not consume; they are answered against the original tanks.
            for (int input = 0; input < recipe.mFluidInputs.length; input++) {
                var required = recipe.mFluidInputs[input];
                if (required == null || !recipe.isPresenceFluidInput(input)) continue;
                if (required.isEmpty()) return false;
                boolean present = false;
                for (var available : fluids)
                    if (!available.isEmpty() && sameFluid(available, required)) { present = true; break; }
                if (!present) return false;
            }
            // Reserve catalysts alongside consumables; restore them only in the final consumption plan.
            for (int input = 0; input < recipe.mInputs.length; input++) {
                var required = recipe.mInputs[input];
                if (required == null || required.isEmpty()) continue;
                boolean catalyst = recipe.isCatalystInput(input);
                long needed = (long) required.getCount() * (catalyst ? 1 : parallel);
                for (int slot = 0; slot < items.size(); slot++) {
                    if (itemCounts[slot] <= 0) continue;
                    if (!acceptsItem(recipe, input, required, items.get(slot))) continue;
                    int take = (int) Math.min(needed, itemCounts[slot]);
                    itemCounts[slot] -= take;
                    if (catalyst) retainedItems[slot] += take;
                    needed -= take;
                    if (needed == 0) break;
                }
                // An earlier, broader requirement may already have drained the only slot this one
                // accepts, so the fallback must test acceptance, not the remaining counts. A
                // requirement no slot accepts, or whose accepting slots lack the total supply,
                // still stays off the planner. The planner re-plans every item input, but the
                // fluid pass below still has to run.
                if (needed != 0) {
                    if (!couldBeSatisfied(recipe, input, required, needed)
                            || !allocateItems(recipe, parallel)) return false;
                    break;
                }
            }
            for (int input = 0; input < recipe.mFluidInputs.length; input++) {
                var required = recipe.mFluidInputs[input];
                if (required == null || required.isEmpty() || recipe.isPresenceFluidInput(input)) continue;
                boolean catalyst = recipe.isCatalystFluidInput(input);
                long needed = RecipeKeptInputs.fluidDemand(catalyst, required.getAmount(), parallel);
                boolean anyAccepted = false;
                for (int tank = 0; tank < fluids.size(); tank++) {
                    if (fluidCounts[tank] <= 0 || !sameFluid(fluids.get(tank), required)) continue;
                    anyAccepted = true;
                    int take = (int) Math.min(needed, fluidCounts[tank]);
                    fluidCounts[tank] -= take;
                    if (catalyst) retainedFluids[tank] += take;
                    needed -= take;
                    if (needed == 0) break;
                }
                // A tank holds exactly one fluid, so its acceptance does not depend on the
                // requirement: a drained accepting tank means the supply was short, not that the
                // assignment order was wrong.
                if (needed != 0) return anyAccepted && allocateFluids(recipe, parallel);
            }
            return true;
        }

        /** Flow re-assignment for a partially accepted requirement; the common path never reaches it. */
        private boolean allocateItems(Recipe recipe, int parallel) {
            resetItems();
            var demand = new long[recipe.mInputs.length];
            var accepts = new boolean[recipe.mInputs.length][items.size()];
            for (int input = 0; input < recipe.mInputs.length; input++) {
                var required = recipe.mInputs[input];
                if (required == null || required.isEmpty()) continue;
                demand[input] = (long) required.getCount() * (recipe.isCatalystInput(input) ? 1 : parallel);
                for (int slot = 0; slot < items.size(); slot++)
                    accepts[input][slot] = acceptsItem(recipe, input, required, items.get(slot));
            }
            var plan = IngredientAllocation.planMixed(itemCounts, demand, accepts);
            if (plan == null) return false;
            for (int input = 0; input < plan.length; input++)
                for (int slot = 0; slot < items.size(); slot++) {
                    itemCounts[slot] -= plan[input][slot];
                    if (recipe.isCatalystInput(input)) retainedItems[slot] += plan[input][slot];
                }
            return true;
        }

        /**
         * Whether the original slots can cover this requirement at all. Acceptance is evaluated
         * without the remaining counts, because a greedy pass may already have spent the only slot
         * this requirement accepts and the planner is what undoes that.
         */
        private boolean couldBeSatisfied(Recipe recipe, int input, ItemStack required, long needed) {
            long supply = 0;
            for (int slot = 0; slot < items.size(); slot++) {
                if (!acceptsItem(recipe, input, required, items.get(slot))) continue;
                supply += items.get(slot).getCount();
                if (supply >= needed) return true;
            }
            return false;
        }

        private boolean allocateFluids(Recipe recipe, int parallel) {
            resetFluids();
            var demand = new long[recipe.mFluidInputs.length];
            var accepts = new boolean[recipe.mFluidInputs.length][fluids.size()];
            for (int input = 0; input < recipe.mFluidInputs.length; input++) {
                var required = recipe.mFluidInputs[input];
                if (required == null || required.isEmpty() || recipe.isPresenceFluidInput(input)) continue;
                demand[input] = RecipeKeptInputs.fluidDemand(
                        recipe.isCatalystFluidInput(input), required.getAmount(), parallel);
                for (int tank = 0; tank < fluids.size(); tank++)
                    accepts[input][tank] = recipe.matchesFluidInput(input, fluids.get(tank));
            }
            var plan = IngredientAllocation.planMixed(fluidCounts, demand, accepts);
            if (plan == null) return false;
            for (int input = 0; input < plan.length; input++)
                for (int tank = 0; tank < fluids.size(); tank++) {
                    fluidCounts[tank] -= plan[input][tank];
                    if (recipe.isCatalystFluidInput(input)) retainedFluids[tank] += plan[input][tank];
                }
            return true;
        }

        private boolean acceptsItem(Recipe recipe, int input, ItemStack required, ItemStack available) {
            if (!recipe.matchesItemInput(input, available)) return false;
            if (recipe.mExactItemInputs && !sameItem(required, available)) return false;
            if (recipe.mRequiresEmptyContainerInputs
                    && gregtech.engine.material.ItemMaterialRegistry.hasStoredContents(available)) return false;
            return !recipe.mMaterialRecovery
                    || !available.isDamaged() && gregtech.engine.material.ItemMaterialRegistry.canRecover(available);
        }

        private void resetItems() {
            for (int slot = 0; slot < itemCounts.length; slot++) itemCounts[slot] = items.get(slot).getCount();
            Arrays.fill(retainedItems, 0);
        }

        private void resetFluids() {
            for (int tank = 0; tank < fluidCounts.length; tank++) fluidCounts[tank] = fluids.get(tank).getAmount();
            Arrays.fill(retainedFluids, 0);
        }

        private boolean tagged(Recipe recipe, int parallel) {
            long[] itemDemand = new long[recipe.mInputs.length];
            boolean[][] itemAccepts = new boolean[itemDemand.length][items.size()];
            boolean[] homogeneousItems = new boolean[itemDemand.length];
            Arrays.fill(homogeneousItems, true);
            for (int input = 0; input < itemDemand.length; input++) {
                ItemStack required = recipe.mInputs[input];
                if (required == null || required.isEmpty()) continue;
                itemDemand[input] = (long) required.getCount() * (recipe.isCatalystInput(input) ? 1 : parallel);
                for (int slot = 0; slot < items.size(); slot++) {
                    var available = items.get(slot);
                    itemAccepts[input][slot] = recipe.matchesItemInput(input, available)
                            && (recipe.itemTag(input) != null || !recipe.mExactItemInputs || sameItem(required, available))
                            && (!recipe.mRequiresEmptyContainerInputs || !gregtech.engine.material.ItemMaterialRegistry.hasStoredContents(available))
                            && (!recipe.mMaterialRecovery || !available.isDamaged() && gregtech.engine.material.ItemMaterialRegistry.canRecover(available));
                }
            }
            int[] itemIdentities = new int[items.size()];
            for (int slot = 0; slot < items.size(); slot++) {
                itemIdentities[slot] = slot;
                for (int previous = 0; previous < slot; previous++) if (sameItem(items.get(slot), items.get(previous))) {
                    itemIdentities[slot] = itemIdentities[previous]; break;
                }
            }
            int[][] itemPlan = IngredientAllocation.plan(itemCounts, itemDemand, itemAccepts, itemIdentities, homogeneousItems);
            if (itemPlan == null) return false;
            long[] fluidDemand = new long[recipe.mFluidInputs.length];
            boolean[][] fluidAccepts = new boolean[fluidDemand.length][fluids.size()];
            boolean[] homogeneousFluids = new boolean[fluidDemand.length];
            Arrays.fill(homogeneousFluids, true);
            for (int input = 0; input < fluidDemand.length; input++) {
                var required = recipe.mFluidInputs[input];
                if (required == null || required.isEmpty()) continue;
                boolean present = false;
                for (int tank = 0; tank < fluids.size(); tank++) {
                    fluidAccepts[input][tank] = recipe.matchesFluidInput(input, fluids.get(tank));
                    present |= fluidAccepts[input][tank];
                }
                if (recipe.isPresenceFluidInput(input)) {
                    if (!present) return false;
                } else fluidDemand[input] = RecipeKeptInputs.fluidDemand(recipe.isCatalystFluidInput(input), required.getAmount(), parallel);
            }
            int[] fluidIdentities = new int[fluids.size()];
            for (int tank = 0; tank < fluids.size(); tank++) {
                fluidIdentities[tank] = tank;
                for (int previous = 0; previous < tank; previous++) if (sameFluid(fluids.get(tank), fluids.get(previous))) {
                    fluidIdentities[tank] = fluidIdentities[previous]; break;
                }
            }
            int[][] fluidPlan = IngredientAllocation.plan(fluidCounts, fluidDemand, fluidAccepts, fluidIdentities, homogeneousFluids);
            if (fluidPlan == null) return false;
            for (int input = 0; input < itemPlan.length; input++) for (int slot = 0; slot < items.size(); slot++) {
                itemCounts[slot] -= itemPlan[input][slot];
                if (recipe.isCatalystInput(input)) retainedItems[slot] += itemPlan[input][slot];
            }
            for (int input = 0; input < fluidPlan.length; input++) for (int tank = 0; tank < fluids.size(); tank++) {
                fluidCounts[tank] -= fluidPlan[input][tank];
                if (recipe.isCatalystFluidInput(input)) retainedFluids[tank] += fluidPlan[input][tank];
            }
            return true;
        }
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return StackCompat.sameItem(a, b);
    }
    public static boolean sameFluid(FluidStack available, FluidStack required) {
        return StackCompat.sameFluid(available, required);
    }

    /**
     * Keys shared by a required stack and every stack {@link #matches(ItemStack, ItemStack)}
     * accepts for it. The recipe lookup index is only complete while these keys mirror every
     * accepting branch of that method, of {@code MaterialEquivalence.matches}, of
     * {@code MaterialUnification.canonical} and of the legacy colour aliases; change them together.
     */
    static void requireKeys(ItemStack required, Set<Object> out) {
        out.add(required.getItem());
        out.add(gregtech.engine.material.MaterialUnification.canonical(required).getItem());
        var form = gregtech.engine.material.MaterialEquivalence.form(required);
        if (form != null) {
            out.add(form);
            gregtech.engine.material.MaterialEquivalence.addTagKeys(form, out);
        }
        for (var color : gregtech.content.block.misc.LegacyConstructionBlockItem.matchingAlternatives(required))
            requireKeys(color, out);
    }

    /** Counterpart of {@link #requireKeys(ItemStack, Set)} for the offered stack. */
    static void offerKeys(ItemStack available, Set<Object> out) {
        if (available == null || available.isEmpty()) return;
        out.add(available.getItem());
        out.add(gregtech.engine.material.MaterialUnification.canonical(available).getItem());
        var form = gregtech.engine.material.MaterialEquivalence.form(available);
        // The tag branch is only reached when the offered stack has no material form.
        if (form != null) out.add(form);
        else available.getTags().forEach(out::add);
        var legacy = gregtech.content.block.misc.LegacyConstructionBlockItem.canonical(available);
        if (legacy != available) offerKeys(legacy, out);
    }

    public static boolean matches(ItemStack required, ItemStack available) {
        if(available.isEmpty())return false;
        // Legacy palette items and their fixed-color replacements denote the same input.
        // Normalize the offered identity too; expanding only the requirement rejects its bare alias.
        var canonical = gregtech.content.block.misc.LegacyConstructionBlockItem.canonical(available);
        if (canonical != available) return matches(required, canonical);
        var colors = gregtech.content.block.misc.LegacyConstructionBlockItem.matchingAlternatives(required);
        if (!colors.isEmpty()) return colors.stream().anyMatch(color -> matches(color, available));
        if(!required.is(available.getItem())) return gregtech.engine.material.MaterialEquivalence.matches(required,available);
        if (!emptyContainerMatches(required, available)) return false;
//? if >=1.21.1 {
        /*return required.getComponentsPatch().isEmpty() || ItemStack.isSameItemSameComponents(required, available);
*///?} else {
        return !required.hasTag() || ItemStack.isSameItemSameTags(required, available);
//?}
    }

    /** Empty shells must stay empty even when a recipe supplies its own stack-data predicate. */
    static boolean emptyContainerMatches(ItemStack required, ItemStack available) {
//? if >=1.21.1 {
        /*return !(required.getItem() instanceof gregtech.engine.inventory.ContainerShapeLike
                && net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(required).isEmpty()
                && net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(available).isPresent());
*///?} else {
        return !(required.getItem() instanceof gregtech.content.item.PortableFluidContainerItem
                && net.minecraftforge.fluids.FluidUtil.getFluidContained(required).isEmpty()
                && net.minecraftforge.fluids.FluidUtil.getFluidContained(available).isPresent());
//?}
    }
}
