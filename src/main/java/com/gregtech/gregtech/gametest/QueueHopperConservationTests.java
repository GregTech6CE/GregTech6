package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.blockentity.machine.QueueHopperBlockEntity;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class QueueHopperConservationTests {
    @GameTest(template = "test_empty")
    public static void partialGroundPickupPreservesRemainder(GameTestHelper helper) {
        for (int[] example : new int[][]{{1, 0}, {16, 0}, {64, 0}, {16, 15}}) {
            int capacity = example[0], initial = example[1];
            BlockPos pos = new BlockPos(1, 1, 1);
            var state = MachineRegistry.queueHoppers().get(0).get().defaultBlockState()
                    .setValue(DirectionalBlock.FACING, Direction.UP);
            helper.setBlock(pos, state);
            var hopper = (QueueHopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            CompoundTag tag = new CompoundTag();
            tag.putByte("gt.mode", (byte) capacity);
            tag.putByte("gt.check", (byte) 0);
            hopper.load(tag);
            var absolute = helper.absolutePos(pos);
            var stack = new ItemStack(Items.COBBLESTONE, 64);
            stack.getOrCreateTag().putString("conservation", "original");
            if (initial > 0) hopper.insertItem(0, stack.copyWithCount(initial), false);
            var entity = new ItemEntity(helper.getLevel(), absolute.getX() + .5,
                    absolute.getY() + 1, absolute.getZ() + .5, stack);
            entity.setNoGravity(true);
            helper.getLevel().addFreshEntity(entity);
            QueueHopperBlockEntity.serverTick(helper.getLevel(), absolute, state, hopper);
            int stored = 0;
            for (int slot = 0; slot < hopper.getSlots(); slot++) {
                var value = hopper.getStackInSlot(slot);
                if (!value.isEmpty()) {
                    stored += value.getCount();
                    helper.assertTrue("original".equals(value.getOrCreateTag().getString("conservation")), "NBT preserved");
                }
            }
            int remainder = entity.isRemoved() ? 0 : entity.getItem().getCount();
            helper.assertTrue(stored == capacity && remainder == 64 + initial - capacity,
                    "capacity " + capacity + ": stored=" + stored + ", remainder=" + remainder);
            entity.discard();
            // Remove contents before removing the fixture so drops cannot affect the next case.
            hopper.load(new CompoundTag());
            for (int slot = 0; slot < hopper.getSlots(); slot++)
                helper.assertTrue(hopper.getStackInSlot(slot).isEmpty(), "loading an empty inventory clears old slots");
            helper.getLevel().removeBlock(absolute, false);
        }
        helper.succeed();
    }
}

