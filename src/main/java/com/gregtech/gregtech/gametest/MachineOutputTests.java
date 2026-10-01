package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.recipe.MachineWorkOutputs;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * Regression tests for delivering machine products.
 * <p>
 * The port used to merge a pending output into any occupied output slot, so a machine whose
 * output section held a different item inflated that stack and destroyed the pending product.
 * These tests pin the correct behaviour: only matching stacks are merged, empty slots are used
 * next, and anything that still does not fit stays pending instead of being lost.
 * </p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class MachineOutputTests {

    /** Minimal modifiable handler over a fixed list of slots. */
    private static final class Slots implements IItemHandlerModifiable {
        private final List<ItemStack> stacks;
        private final int firstOutput;

        Slots(int firstOutput, ItemStack... initial) {
            this.firstOutput = firstOutput;
            this.stacks = new ArrayList<>(List.of(initial));
        }

        @Override public void setStackInSlot(int slot, ItemStack stack) { stacks.set(slot, stack); }
        @Override public int getSlots() { return stacks.size(); }
        @Override public ItemStack getStackInSlot(int slot) { return stacks.get(slot); }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot >= firstOutput; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
    }

    private static MachineWorkOutputs outputsOf(ItemStack... items) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack stack : items) list.add(stack.save(new CompoundTag()));
        tag.put("items", list);
        tag.put("fluids", new ListTag());
        return MachineWorkOutputs.load(tag);
    }

    /** A pending output must never be merged into a slot holding a different item. */
    @GameTest(template = "test_empty")
    public static void flushNeverMergesIntoADifferentItem(GameTestHelper helper) {
        var slots = new Slots(1,
                new ItemStack(Items.STONE),            // 0: input slot
                new ItemStack(Items.COPPER_INGOT, 1),  // 1: output, different item
                ItemStack.EMPTY);                      // 2: output, empty
        var pending = outputsOf(new ItemStack(Items.IRON_INGOT, 3));

        boolean finished = pending.flush(slots, 1, new FluidTankGT[0]);

        helper.assertTrue(finished, "the pending output fits into the empty output slot");
        helper.assertTrue(slots.getStackInSlot(1).is(Items.COPPER_INGOT)
                        && slots.getStackInSlot(1).getCount() == 1,
                "unrelated output slot untouched: " + slots.getStackInSlot(1));
        helper.assertTrue(slots.getStackInSlot(2).is(Items.IRON_INGOT)
                        && slots.getStackInSlot(2).getCount() == 3,
                "pending output delivered: " + slots.getStackInSlot(2));
        helper.succeed();
    }

    /** Output that does not fit must stay pending instead of being dropped. */
    @GameTest(template = "test_empty")
    public static void overflowStaysPending(GameTestHelper helper) {
        var slots = new Slots(1,
                new ItemStack(Items.STONE),
                new ItemStack(Items.COPPER_INGOT, 64));  // full, different item
        var pending = outputsOf(new ItemStack(Items.IRON_INGOT, 2));

        helper.assertTrue(!pending.flush(slots, 1, new FluidTankGT[0]), "flush reports no space");
        helper.assertTrue(slots.getStackInSlot(1).getCount() == 64, "full slot unchanged");
        helper.assertTrue(pending.pendingItemStacks() == 1, "output kept pending, not dropped");
        helper.succeed();
    }

    /** Matching stacks are topped up before empty slots are used. */
    @GameTest(template = "test_empty")
    public static void matchingStacksFillFirst(GameTestHelper helper) {
        var slots = new Slots(1,
                new ItemStack(Items.STONE),
                new ItemStack(Items.IRON_INGOT, 60),
                ItemStack.EMPTY);
        var pending = outputsOf(new ItemStack(Items.IRON_INGOT, 10));

        helper.assertTrue(pending.flush(slots, 1, new FluidTankGT[0]), "everything fits");
        helper.assertTrue(slots.getStackInSlot(1).getCount() == 64, "matching stack topped up first");
        helper.assertTrue(slots.getStackInSlot(2).is(Items.IRON_INGOT)
                        && slots.getStackInSlot(2).getCount() == 6,
                "remainder goes to the empty slot: " + slots.getStackInSlot(2));
        helper.succeed();
    }

    /** Fluid outputs follow the same rules. */
    @GameTest(template = "test_empty")
    public static void fluidOutputRespectsTankContents(GameTestHelper helper) {
        var water = new FluidTankGT(1000);
        water.setFluid(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000));
        var empty = new FluidTankGT(1000);

        CompoundTag tag = new CompoundTag();
        tag.put("items", new ListTag());
        ListTag fluids = new ListTag();
        fluids.add(new net.minecraftforge.fluids.FluidStack(
                net.minecraft.world.level.material.Fluids.LAVA, 500).writeToNBT(new CompoundTag()));
        tag.put("fluids", fluids);
        var pending = MachineWorkOutputs.load(tag);

        var slots = new Slots(0, new ItemStack(Items.STONE));
        helper.assertTrue(pending.flush(slots, 0, new FluidTankGT[]{water, empty}),
                "lava goes into the empty tank");
        helper.assertTrue(water.getAmount() == 1000 && water.getFluid().getFluid()
                        == net.minecraft.world.level.material.Fluids.WATER,
                "full water tank untouched");
        helper.assertTrue(empty.getAmount() == 500 && empty.getFluid().getFluid()
                        == net.minecraft.world.level.material.Fluids.LAVA,
                "lava delivered to the empty tank");
        helper.succeed();
    }
}
