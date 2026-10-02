package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.content.cover.CoverUtilityBehaviors;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Server block-use dispatch with real item capabilities and a mock survival player inventory. */
@GameTestHolder("gregtech_fluid_channels")
@PrefixGameTestTemplate(false)
public final class FluidPipeContainerTests {
    private static BlockPos site(int x) { return new BlockPos(113000 + x, 180, 113000); }

    private static BlockState place(GameTestHelper h, BlockPos pos, String id) {
        h.getLevel().getChunkAt(pos);
        if (h.getLevel().getBlockEntity(pos) instanceof FluidPipeBlockEntity previous) {
            for (int i = 0; i < previous.getTanks(); i++) previous.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        }
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", id));
        h.assertTrue(block != Blocks.AIR, "registered block " + id);
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        // Clear auto-connections inherited from any previous fixture before opening the requested face.
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        return block.defaultBlockState();
    }

    private static FluidPipeBlockEntity pipe(GameTestHelper h, int x, String size) {
        var pos = site(x);
        var state = place(h, pos, "pipe_" + size + "_steel");
        h.getLevel().setBlockAndUpdate(pos, state.setValue(FluidPipeBlock.EAST, true));
        return (FluidPipeBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static Player player(GameTestHelper h, BlockPos pos) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[gt-fluid]"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setPos(pos.getX() + 2, pos.getY(), pos.getZ() + .5);
        return player;
    }

    private static void click(GameTestHelper h, BlockPos pos, Player player, InteractionHand hand, Direction side) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(side, .5), side, pos, false);
        var state = h.getLevel().getBlockState(pos);
        state.useItemOn(player.getItemInHand(hand), h.getLevel(), player, hand, hit);
    }

    private static int count(Player player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void survivalBucketRoundTripUsesClickedHand(GameTestHelper h) {
        var p = pipe(h, 0, "huge");
        var player = player(h, p.getBlockPos());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
        click(h, p.getBlockPos(), player, InteractionHand.OFF_HAND, Direction.EAST);
        h.assertTrue(player.getOffhandItem().is(Items.BUCKET) && p.getFluidInTank(0).getAmount() == 1000,
                "pouring replaces offhand water bucket with empty bucket and adds exactly 1000 mB");
        h.assertTrue(player.getMainHandItem().is(Items.STICK), "unrelated main hand unchanged");
        click(h, p.getBlockPos(), player, InteractionHand.OFF_HAND, Direction.EAST);
        h.assertTrue(player.getOffhandItem().is(Items.WATER_BUCKET) && p.getFluidInTank(0).isEmpty(),
                "drawing replaces offhand bucket and removes exactly 1000 mB");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void closedClickedFaceBlocksBothDirections(GameTestHelper h) {
        var p = pipe(h, 20, "huge");
        var player = player(h, p.getBlockPos());
        p.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.WEST);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET) && p.getFluidInTank(0).getAmount() == 1000,
                "closed west cannot drain although east is open");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.WEST);
        h.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET) && p.getFluidInTank(0).getAmount() == 1000,
                "closed west cannot fill although east is open");
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET) && p.getFluidInTank(0).getAmount() == 2000,
                "the same bucket can fill through the open face");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void filterConfigurationThenContainerTransfer(GameTestHelper h) {
        var p = pipe(h, 40, "quadruple");
        var player = player(h, p.getBlockPos());
        h.assertTrue(p.attachCover(Direction.EAST, new ItemStack(GTTechnological.get(CoverUtilityBehaviors.FILTER_FLUID))),
                "fluid filter attaches");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(p.getFluidInTank(0).isEmpty() && player.getMainHandItem().is(Items.WATER_BUCKET),
                "first bucket click configures empty filter without consuming contents");
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(p.getFluidInTank(0).getAmount() == 1000 && player.getMainHandItem().is(Items.BUCKET),
                "configured filter admits matching bucket");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(p.getFluidInTank(0).getAmount() == 1000 && p.getFluidInTank(1).isEmpty()
                && player.getMainHandItem().is(Items.LAVA_BUCKET),
                "configured water filter rejects lava without changing held container");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(p.getFluidInTank(0).isEmpty() && player.getMainHandItem().is(Items.WATER_BUCKET),
                "matching drain through filter returns full water bucket");
        p.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(p.getFluidInTank(0).getAmount() == 1000 && player.getMainHandItem().is(Items.BUCKET),
                "draining disallowed fluid is filtered too");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void incompleteBucketTransferChangesNeitherSide(GameTestHelper h) {
        var p = pipe(h, 60, "huge");
        var player = player(h, p.getBlockPos());
        p.fill(new FluidStack(Fluids.WATER, 4300), FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(p.getFluidInTank(0).getAmount() == 4300 && player.getMainHandItem().is(Items.WATER_BUCKET),
                "500 mB remaining capacity cannot consume a 1000 mB bucket");
        p.drain(3800, FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(p.getFluidInTank(0).getAmount() == 500 && player.getMainHandItem().is(Items.BUCKET),
                "500 mB in pipe cannot fill or consume an empty bucket");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void stackedBucketsStoreExactlyOneFilledResult(GameTestHelper h) {
        var p = pipe(h, 80, "huge");
        var player = player(h, p.getBlockPos());
        p.fill(new FluidStack(Fluids.WATER, 1500), FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET, 3));
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET) && player.getMainHandItem().getCount() == 2
                && count(player, Items.WATER_BUCKET) == 1 && p.getFluidInTank(0).getAmount() == 500,
                "one container processed, remaining empty stack stays in hand, filled result stored once");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fullInventoryDropsExactlyOneFilledResult(GameTestHelper h) {
        // Entity queries only see active sections; use the framework's ticking arena for the drop.
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        var state = place(h, pos, "pipe_huge_steel");
        h.getLevel().setBlockAndUpdate(pos, state.setValue(FluidPipeBlock.EAST, true));
        var p = (FluidPipeBlockEntity) h.getLevel().getBlockEntity(pos);
        var player = player(h, p.getBlockPos());
        var area = new AABB(p.getBlockPos()).inflate(6);
        for (var entity : h.getLevel().getEntitiesOfClass(ItemEntity.class, area)) entity.discard();
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COBBLESTONE, 64));
        p.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET, 3));
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET) && player.getMainHandItem().getCount() == 2
                && count(player, Items.WATER_BUCKET) == 0 && p.getFluidInTank(0).isEmpty(),
                "full inventory transfer consumes one empty bucket and exactly 1000 mB");
        h.runAfterDelay(1, () -> {
            int dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream()
                    .filter(e -> e.getItem().is(Items.WATER_BUCKET)).mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(dropped == 1, "full inventory drops exactly one water bucket, got " + dropped);
            h.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void creativeKeepsContainerButTransfersFluid(GameTestHelper h) {
        var p = pipe(h, 120, "huge");
        var player = player(h, p.getBlockPos());
        player.getAbilities().instabuild = true;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET) && p.getFluidInTank(0).getAmount() == 1000,
                "creative pouring retains water bucket and adds its contents");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        click(h, p.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET) && p.getFluidInTank(0).isEmpty(),
                "platform creative drain removes fluid but retains original empty container");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void storageDrumUsesReplacementContainerTransaction(GameTestHelper h) {
        var pos = site(140);
        place(h, pos, "drum_stainless_steel");
        var tank = (TankBlockEntity) h.getLevel().getBlockEntity(pos);
        var player = player(h, pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        click(h, pos, player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(tank.getFluidInTank(0).getAmount() == 1000 && player.getMainHandItem().is(Items.BUCKET),
                "drum pouring writes replacement bucket back too");
        click(h, pos, player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(tank.getFluidInTank(0).isEmpty() && player.getMainHandItem().is(Items.WATER_BUCKET),
                "drum round trip conserves one bucket");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void bucketPourMarksTheClickedFaceForOneTick(GameTestHelper h) {
        // Reset the receiving pipe before opening the source, so a reused world cannot back-dump old fluid.
        var b = pipe(h, 161, "huge");
        var a = pipe(h, 160, "huge");
        h.getLevel().setBlockAndUpdate(b.getBlockPos(), b.getBlockState().setValue(FluidPipeBlock.WEST, true));
        var player = player(h, a.getBlockPos());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        click(h, a.getBlockPos(), player, InteractionHand.MAIN_HAND, Direction.EAST);
        FluidPipeBlockEntity.serverTick(h.getLevel(), a.getBlockPos(), a.getBlockState(), a);
        h.assertTrue(a.getFluidInTank(0).getAmount() == 1000 && b.getFluidInTank(0).isEmpty(),
                "bucket insertion does not immediately flow back through its entry face");
        FluidPipeBlockEntity.serverTick(h.getLevel(), a.getBlockPos(), a.getBlockState(), a);
        h.assertTrue(a.getFluidInTank(0).getAmount() == 500 && b.getFluidInTank(0).getAmount() == 500,
                "bucket backflow marker expires after one distribution tick");
        h.succeed();
    }
}
