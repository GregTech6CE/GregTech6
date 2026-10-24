package gregtech.gametest;

import gregtech.block.machine.HopperBlock;
import gregtech.block.machine.ItemPipeBlock;
import gregtech.blockentity.machine.HopperBlockEntity;
import gregtech.blockentity.machine.ItemPipeBlockEntity;
import gregtech.content.cover.PanelCover;
import gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
//?}

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class ItemPipeShutterTests {
    private static final int FLAGS = 18;

    private static IItemHandler face(GameTestHelper h, ItemPipeBlockEntity pipe, Direction side) {
//? if >=1.21.1 {
/*        return java.util.Objects.requireNonNull(h.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pipe.getBlockPos(), side));
*///?} else {
        return pipe.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side)
                .orElseThrow(IllegalStateException::new);
//?}
    }

    private static ItemPipeBlockEntity pipe(GameTestHelper h, BlockPos pos) {
        var block = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof ItemPipeBlock p && p.spec().invSize() >= 8)
                .findFirst().orElseThrow();
        h.getLevel().setBlock(pos, block.defaultBlockState(), FLAGS);
        return (ItemPipeBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static HopperBlockEntity hopper(GameTestHelper h, BlockPos pos) {
        var block = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof HopperBlock).findFirst().orElseThrow();
        h.getLevel().setBlock(pos, block.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST), FLAGS);
        return (HopperBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static int count(IItemHandler handler) {
        int count = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) count += handler.getStackInSlot(slot).getCount();
        return count;
    }

    private static void tick(GameTestHelper h, HopperBlockEntity hopper) {
        for (int i = 0; i < 4; i++) HopperBlockEntity.serverTick(h.getLevel(), hopper.getBlockPos(), hopper.getBlockState(), hopper);
    }

    private static void close(GameTestHelper h, ItemPipeBlockEntity pipe, Direction side) {
        h.assertTrue(pipe.attachCover(side, new ItemStack(GTTechnological.get(PanelCover.SHUTTER.id))), "registered shutter attaches");
        h.assertTrue(pipe.panels().configure(side, false, false), "native configuration closes inverted shutter");
        h.assertTrue(pipe.panels().shuttered(side) && !pipe.getBlockState().getValue(ItemPipeBlock.propFor(side)), "closed shutter disconnects face");
    }

    private static void open(GameTestHelper h, ItemPipeBlockEntity pipe, Direction side) {
        h.assertTrue(pipe.panels().configure(side, false, false), "native configuration reopens shutter");
        h.assertTrue(!pipe.panels().shuttered(side) && pipe.getBlockState().getValue(ItemPipeBlock.propFor(side)), "open shutter reconnects face");
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void itemPipeShutterBlocksNativeHopperPushAndPull(GameTestHelper h) {
        for (boolean pull : new boolean[]{false, true}) {
            var origin = new BlockPos(25_600_000 + (pull ? 16 : 0), 200, 25_600_000);
            try (var fixture = new Fixture(h, origin)) {
                var hopper = hopper(h, origin);
                var pipe = pipe(h, pull ? origin.above() : origin.east());
                h.getLevel().setBlock(pull ? origin.east() : origin.above(), Blocks.STONE.defaultBlockState(), FLAGS);
                var side = pull ? Direction.DOWN : Direction.WEST;
                close(h, pipe, side);
                if (pull) h.assertTrue(pipe.insertItem(0, new ItemStack(Items.DIRT, 8), false).isEmpty(), "seed source pipe");
                else hopper.setStackInSlot(0, new ItemStack(Items.DIRT, 8));
                tick(h, hopper);
                h.assertTrue(count(pipe) == (pull ? 8 : 0) && count(hopper) == (pull ? 0 : 8), "closed shutter rejects actual hopper " + (pull ? "pull" : "push"));
                open(h, pipe, side);
                // Refresh the real source inventory flag so the hopper's idle gate retries.
                hopper.setStackInSlot(0, pull ? ItemStack.EMPTY : new ItemStack(Items.DIRT, 8));
                tick(h, hopper);
                h.assertTrue(count(pipe) == (pull ? 0 : 8) && count(hopper) == (pull ? 8 : 0), "open shutter allows native transfer and conserves all eight items");
            }
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void itemPipeShutterUpdatesPreviouslyObtainedSideHandlers(GameTestHelper h) {
        var origin = new BlockPos(25_600_032, 200, 25_600_000);
        try (var fixture = new Fixture(h, origin)) {
            var pipe = pipe(h, origin);
            var retained = face(h, pipe, Direction.WEST);
            h.assertTrue(!pipe.getBlockState().getValue(ItemPipeBlock.WEST), "coverless fixture starts disconnected");
            h.assertTrue(retained.insertItem(0, new ItemStack(Items.DIRT, 4), true).isEmpty() && count(pipe) == 0, "coverless disconnected simulation remains supported");
            h.assertTrue(retained.insertItem(0, new ItemStack(Items.DIRT, 4), false).isEmpty() && count(pipe) == 4, "coverless disconnected commit remains supported");
            close(h, pipe, Direction.WEST);
            var fresh = face(h, pipe, Direction.WEST);
            for (var handler : new IItemHandler[]{retained, fresh}) {
                for (boolean simulate : new boolean[]{true, false}) {
                    h.assertTrue(handler.insertItem(0, new ItemStack(Items.DIRT, 2), simulate).getCount() == 2, "closed shutter rejects retained/fresh insertion");
                    h.assertTrue(handler.extractItem(0, 2, simulate).isEmpty() && count(pipe) == 4, "closed shutter rejects retained/fresh extraction without mutation");
                }
            }
            h.assertTrue(pipe.componentItems(Direction.WEST).extractItem(0, 1, true).getCount() == 1
                    && pipe.extractItem(0, 1, true).getCount() == 1 && count(pipe) == 4, "internal and unsided access remain unchanged");
            var unrelated = face(h, pipe, Direction.EAST);
            h.assertTrue(unrelated.extractItem(0, 1, false).getCount() == 1 && count(pipe) == 3, "unrelated face remains usable");
            h.assertTrue(unrelated.insertItem(0, new ItemStack(Items.DIRT), false).isEmpty() && count(pipe) == 4, "unrelated face restores conserved item");
            open(h, pipe, Direction.WEST);
            for (var handler : new IItemHandler[]{retained, fresh}) {
                h.assertTrue(handler.extractItem(0, 2, true).getCount() == 2 && count(pipe) == 4, "reopened extraction simulation is nonmutating");
                h.assertTrue(handler.insertItem(0, new ItemStack(Items.DIRT, 2), true).isEmpty() && count(pipe) == 4, "reopened insertion simulation is nonmutating");
                var taken = handler.extractItem(0, 2, false);
                h.assertTrue(taken.getCount() == 2 && count(pipe) == 2, "reopened retained/fresh extraction commits");
                h.assertTrue(handler.insertItem(0, taken, false).isEmpty() && count(pipe) == 4, "reopened retained/fresh insertion conserves contents");
            }
        }
        h.succeed();
    }

    /** Independent high-altitude cells: clear inputs/redstone terrain, restore native states and BEs even on failure. */
    private static final class Fixture implements AutoCloseable {
        private record Saved(net.minecraft.world.level.block.state.BlockState state, net.minecraft.nbt.CompoundTag data) {}
        private final GameTestHelper h;
        private final java.util.Map<BlockPos, Saved> saved = new java.util.LinkedHashMap<>();

        Fixture(GameTestHelper h, BlockPos origin) {
            this.h = h;
            for (var pos : BlockPos.betweenClosed(origin.offset(-3, -3, -3), origin.offset(3, 3, 3))) {
                var entity = h.getLevel().getBlockEntity(pos);
//? if >=1.21.1 {
/*                var data = entity == null ? null : entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
                var data = entity == null ? null : entity.saveWithoutMetadata();
//?}
                saved.put(pos.immutable(), new Saved(h.getLevel().getBlockState(pos), data));
            }
            for (var pos : saved.keySet()) clear(pos);
        }

        private void clear(BlockPos pos) {
            h.getLevel().removeBlockEntity(pos);
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        }

        @Override
        public void close() {
            for (var pos : saved.keySet()) clear(pos);
            for (var entry : saved.entrySet()) {
                var pos = entry.getKey(); var original = entry.getValue();
                h.getLevel().setBlock(pos, original.state(), FLAGS);
                var restored = h.getLevel().getBlockEntity(pos);
                if (restored != null && original.data() != null) {
//? if >=1.21.1 {
/*                    restored.loadWithComponents(original.data(), h.getLevel().registryAccess());
*///?} else {
                    restored.load(original.data());
//?}
                }
            }
        }
    }
}
