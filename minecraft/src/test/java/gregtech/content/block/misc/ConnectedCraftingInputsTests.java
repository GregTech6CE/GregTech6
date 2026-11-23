package gregtech.content.block.misc;

import gregtech.content.blockentity.machine.TankBlockEntity;
import gregtech.engine.stack.NbtCompat;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ConnectedCraftingInputsTests {
    private static final String BATCH = "connected_crafting";

    @GameTest(template = "test_empty", batch = BATCH)
    public static void aliasedReservationsReturnExtractOnlyItems(GameTestHelper helper) {
        var storage = new ItemStackHandler(1) {
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        };
        var iron = new ItemStack(Items.IRON_INGOT);
        storage.setStackInSlot(0, iron.copy());
        var inputs = new ConnectedCraftingInputs(List.of(new RangedWrapper(storage, 0, 1), new RangedWrapper(storage, 0, 1)), List.of());
        helper.assertTrue(!inputs.take(iron).isEmpty() && !inputs.take(iron).isEmpty(), "two views both report the same iron");
        var refunds = new ArrayList<ItemStack>();
        boolean crafted = inputs.commit(() -> true, refunds::add, fluid -> helper.fail("unexpected fluid"));
        helper.assertTrue(!crafted && storage.getStackInSlot(0).isEmpty()
                && refunds.size() == 1 && refunds.get(0).is(Items.IRON_INGOT) && refunds.get(0).getCount() == 1,
                "cancel without awarding a result and return the single extracted item");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH)
    public static void aliasedReservationsUseIndependentStock(GameTestHelper helper) {
        var first = new ItemStackHandler(1);
        var second = new ItemStackHandler(1);
        var iron = new ItemStack(Items.IRON_INGOT);
        first.setStackInSlot(0, iron.copy());
        second.setStackInSlot(0, iron.copy());
        var inputs = new ConnectedCraftingInputs(List.of(first, new RangedWrapper(first, 0, 1), second), List.of());
        inputs.take(iron);
        inputs.take(iron);
        boolean crafted = inputs.commit(() -> true, item -> helper.fail("unexpected item"), fluid -> helper.fail("unexpected fluid"));
        helper.assertTrue(crafted && first.getStackInSlot(0).isEmpty() && second.getStackInSlot(0).isEmpty(),
                "use the independent source when the second view is exhausted");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH)
    public static void unrelatedSavedRefundAllowsRefill(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, Mc.block(Mc.gregtechId("advanced_crafting_table")).defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Mc.block(Mc.gregtechId("drum_steel")).defaultBlockState());
        var table = (AdvancedCraftingTableBlockEntity) level.getBlockEntity(pos);
        var drum = (TankBlockEntity) level.getBlockEntity(pos.east());
        drum.getFluidTank().setFluid(new FluidStack(Fluids.LAVA, 1000));

        var lookup = level.registryAccess();
        var saved = new CompoundTag();
        table.saveData(saved, lookup);
        var credits = new ListTag();
        credits.add(NbtCompat.saveFluid(new FluidStack(Fluids.WATER, 1), lookup));
        saved.put("gt.crafting.refunds", credits);
        table.loadData(saved, lookup);

        var inputs = table.connectedInputs();
        helper.assertTrue(inputs.refill(new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.BUCKET)),
                "a saved water credit must allow a bucket to refill from the adjacent lava drum");
        helper.assertTrue(inputs.commit(() -> true, item -> helper.fail("unexpected item"), fluid -> helper.fail("unexpected refund"))
                && drum.getFluidTank().isEmpty(), "the confirmed refill must debit exactly one bucket from the drum");
        var after = new CompoundTag();
        table.saveData(after, lookup);
        var retained = after.getList("gt.crafting.refunds", 10);
        var water = NbtCompat.readFluid(retained.getCompound(0), lookup);
        helper.assertTrue(retained.size() == 1 && water.getFluid() == Fluids.WATER && water.getAmount() == 1,
                "saving the table after the lava refill must retain its one millibucket of water");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH)
    public static void shortFluidDrainRecoversActualTransfers(GameTestHelper helper) {
        var tank = new FluidTank(1000) {
            @Override public FluidStack drain(FluidStack request, FluidAction action) {
                var offer = request.copy();
                if (action.execute()) offer.setAmount(Math.min(360, offer.getAmount()));
                return super.drain(offer, action);
            }
            @Override public int fill(FluidStack fluid, FluidAction action) {
                var offer = fluid.copy();
                offer.setAmount(Math.min(80, offer.getAmount()));
                return super.fill(offer, action);
            }
        };
        tank.setFluid(new FluidStack(Fluids.WATER, 1000));
        var inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        var inputs = new ConnectedCraftingInputs(List.of(inventory), List.of(tank));
        inputs.take(new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(inputs.refill(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.BUCKET)), "bucket refill previews successfully");
        var refunds = new ArrayList<FluidStack>();
        boolean crafted = inputs.commit(() -> true, item -> helper.fail("item should return to source"), refunds::add);
        helper.assertTrue(!crafted && inventory.getStackInSlot(0).getCount() == 1, "failed refill also returns earlier item debits");
        helper.assertTrue(tank.getFluidAmount() == 720 && refunds.size() == 1 && refunds.get(0).getFluid() == Fluids.WATER
                && refunds.get(0).getAmount() == 280, "refund exactly what the source did not take back");
        helper.succeed();
    }
}
