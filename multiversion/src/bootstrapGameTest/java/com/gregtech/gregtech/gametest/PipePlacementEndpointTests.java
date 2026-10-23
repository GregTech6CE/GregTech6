package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import com.gregtech.gregtech.block.machine.TankBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Ignored draft: native BlockItem placement, not a manually invoked placement hook. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class PipePlacementEndpointTests {
    @GameTest(template="test_empty")
    public static void itemPipePlacementConnectsClickedChestOnly(GameTestHelper h) {
        try (var f = new Fixture(h, 0)) {
            var pipe = registered(ItemPipeBlock.class);
            f.set(f.center, Blocks.CHEST.defaultBlockState());
            BlockPos target = f.center.above();
            f.set(target.east(), Blocks.CHEST.defaultBlockState());
            var be = h.getLevel().getBlockEntity(f.center);
//? if >=1.21.1 {
            /*var endpoint = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, f.center, Direction.UP);
*///?} else {
            var endpoint = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
//?}
            h.assertTrue(endpoint != null && endpoint.getSlots() > 0, "clicked native chest has a sided inventory");
            ((net.minecraft.world.Container) be).setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 7));
            place(h, pipe, f.center, Direction.UP);
            h.assertTrue(((net.minecraft.world.Container) be).getItem(0).getCount() == 7, "connection probe never extracts endpoint items");
            var state = h.getLevel().getBlockState(target);
            h.assertTrue(state.getValue(ItemPipeBlock.propFor(Direction.DOWN)), "actual item placement connects clicked chest");
            h.assertTrue(!state.getValue(ItemPipeBlock.propFor(Direction.EAST)), "unrelated adjacent chest is not an all-face connection request");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void fluidPipePlacementConnectsClickedNativeTankOnly(GameTestHelper h) {
        try (var f = new Fixture(h, 16)) {
            var pipe = registered(FluidPipeBlock.class);
            var tank = registered(TankBlock.class);
            f.set(f.center, tank.defaultBlockState());
            BlockPos target = f.center.above();
            f.set(target.east(), tank.defaultBlockState());
            var be = h.getLevel().getBlockEntity(f.center);
//? if >=1.21.1 {
            /*var endpoint = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, f.center, Direction.UP);
*///?} else {
            var endpoint = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElse(null);
//?}
            h.assertTrue(endpoint != null && endpoint.getTanks() > 0, "clicked registered tank has sided fluid storage");
//? if >=1.21.1 {
            /*int filled = endpoint.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 100), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
*///?} else {
            int filled = endpoint.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 100), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
//?}
            h.assertTrue(filled == 100, "real tank fixture accepts water");
            place(h, pipe, f.center, Direction.UP);
            var state = h.getLevel().getBlockState(target);
            h.assertTrue(endpoint.getFluidInTank(0).getAmount() == 100, "connection probe does not move fluid");
            h.assertTrue(state.getValue(FluidPipeBlock.propFor(Direction.DOWN)), "actual fluid placement connects clicked tank");
            h.assertTrue(!state.getValue(FluidPipeBlock.propFor(Direction.EAST)), "other native tanks do not auto-open every face");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void electricWireNativePlacementConnectsSupportReciprocally(GameTestHelper h) {
        try (var f = new Fixture(h, 32)) {
            var wire = registered(ElectricWireBlock.class);
            f.set(f.center, wire.defaultBlockState());
            var target = f.center.above();
            f.set(target.east(), wire.defaultBlockState());
            place(h, wire, f.center, Direction.UP);
            h.assertTrue(h.getLevel().getBlockState(target).getValue(ElectricWireBlock.propFor(Direction.DOWN)), "new wire connects clicked support after its BE exists");
            h.assertTrue(h.getLevel().getBlockState(f.center).getValue(ElectricWireBlock.propFor(Direction.UP)), "support wire receives reciprocal connection");
            h.assertTrue(!h.getLevel().getBlockState(target).getValue(ElectricWireBlock.propFor(Direction.EAST)), "closed unrelated neighboring wire stays closed");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void electricWirePlacementRespectsNativeEuFaces(GameTestHelper h) {
        try (var f = new Fixture(h, 48)) {
            var wire = registered(ElectricWireBlock.class);
            var laser = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", "co2_laser_lv"));
            h.assertTrue(laser instanceof com.gregtech.gregtech.block.energy.LaserConverterBlock, "registered EU laser endpoint");
            f.set(f.center, laser.defaultBlockState().setValue(com.gregtech.gregtech.block.energy.LaserConverterBlock.FACING, Direction.NORTH));
            var endpoint = com.gregtech.gregtech.api.energy.EnergyBlockViews.view(h.getLevel().getBlockEntity(f.center));
            var eu = com.gregtech.gregtech.data.GregTechTags.Energy.EU;
            h.assertTrue(endpoint != null && endpoint.isEnergyAcceptingFrom(eu, Direction.SOUTH, true)
                    && !endpoint.isEnergyAcceptingFrom(eu, Direction.NORTH, true)
                    && !endpoint.isEnergyEmittingTo(eu, Direction.NORTH, true), "real native EU back input / wrong front face");
            long packet = endpoint.getEnergySizeInputRecommended(eu, Direction.SOUTH);
            h.assertTrue(packet > 0 && endpoint.doEnergyInjection(eu, Direction.SOUTH, packet, 1, true) == 1, "legal native energy fixture");
            long demandBefore = endpoint.getEnergyDemanded(eu, Direction.SOUTH, packet);
            place(h, wire, f.center, Direction.NORTH);
            h.assertTrue(!h.getLevel().getBlockState(f.center.north()).getValue(ElectricWireBlock.propFor(Direction.SOUTH)), "wrong EU face remains closed");
            place(h, wire, f.center, Direction.SOUTH);
            h.assertTrue(h.getLevel().getBlockState(f.center.south()).getValue(ElectricWireBlock.propFor(Direction.NORTH)), "clicked compatible EU machine connects");
            h.assertTrue(endpoint.getEnergyDemanded(eu, Direction.SOUTH, packet) == demandBefore, "theoretical connection probe consumes no endpoint energy");
            f.set(f.center.north(), Blocks.AIR.defaultBlockState());
            f.set(f.center.south(), Blocks.AIR.defaultBlockState());
            f.set(f.center, Blocks.STONE.defaultBlockState());
            place(h, wire, f.center, Direction.UP);
            h.assertTrue(!h.getLevel().getBlockState(f.center.above()).getValue(ElectricWireBlock.propFor(Direction.DOWN)), "incompatible solid support stays closed");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void fluidPlacementPreservesShuttersAndManualDisconnects(GameTestHelper h) {
        try (var f = new Fixture(h, 64)) {
            var pipe = registered(FluidPipeBlock.class);
            f.set(f.center, pipe.defaultBlockState());
            var support = (com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity) h.getLevel().getBlockEntity(f.center);
            var shutter = new ItemStack(com.gregtech.gregtech.registry.GTTechnological.get(com.gregtech.gregtech.content.cover.PanelCover.SHUTTER.id));
            h.assertTrue(support.attachCover(Direction.UP, shutter), "support accepts real shutter");
            support.panels().configure(Direction.UP, false, false);
            h.assertTrue(support.panels().shuttered(Direction.UP), "support shutter is actually closed");
            place(h, pipe, f.center, Direction.UP);
            h.assertTrue(!h.getLevel().getBlockState(f.center.above()).getValue(FluidPipeBlock.propFor(Direction.DOWN)), "neighbor shutter blocks clicked connection");
            f.set(f.center.above(), Blocks.AIR.defaultBlockState());
            f.set(f.center, Blocks.AIR.defaultBlockState());
            f.set(f.center, pipe.defaultBlockState());
            support = (com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity) h.getLevel().getBlockEntity(f.center);
            support.setFaceManuallyDisconnected(Direction.UP, true);
            place(h, pipe, f.center, Direction.UP);
            h.assertTrue(!h.getLevel().getBlockState(f.center.above()).getValue(FluidPipeBlock.propFor(Direction.DOWN))
                    && support.isFaceManuallyDisconnected(Direction.UP), "placement does not erase neighbor manual veto");
            var placed = (com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity) h.getLevel().getBlockEntity(f.center.above());
            placed.setFaceManuallyDisconnected(Direction.DOWN, true);
            support.setFaceManuallyDisconnected(Direction.UP, false);
            placed.autoConnectOnPlace(Direction.UP);
            h.assertTrue(!placed.getBlockState().getValue(FluidPipeBlock.propFor(Direction.DOWN)), "own manual veto survives repeated placement attempt");
            placed.setFaceManuallyDisconnected(Direction.DOWN, false);
            h.assertTrue(placed.attachCover(Direction.DOWN, shutter.copy()), "new pipe accepts own shutter");
            placed.panels().configure(Direction.DOWN, false, false);
            h.assertTrue(placed.panels().shuttered(Direction.DOWN), "own shutter actually closed");
            placed.autoConnectOnPlace(Direction.UP);
            h.assertTrue(!placed.getBlockState().getValue(FluidPipeBlock.propFor(Direction.DOWN)), "own shutter blocks repeated placement connection");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void itemPlacementPreservesShuttersAndManualDisconnects(GameTestHelper h) {
        try (var f = new Fixture(h, 80)) {
            var pipe = registered(ItemPipeBlock.class);
            f.set(f.center, pipe.defaultBlockState());
            var support = (com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity) h.getLevel().getBlockEntity(f.center);
            var shutter = new ItemStack(com.gregtech.gregtech.registry.GTTechnological.get(com.gregtech.gregtech.content.cover.PanelCover.SHUTTER.id));
            h.assertTrue(support.attachCover(Direction.UP, shutter), "support accepts real shutter");
            support.panels().configure(Direction.UP, false, false);
            h.assertTrue(support.panels().shuttered(Direction.UP), "support shutter is actually closed");
            place(h, pipe, f.center, Direction.UP);
            h.assertTrue(!h.getLevel().getBlockState(f.center.above()).getValue(ItemPipeBlock.propFor(Direction.DOWN)), "neighbor shutter blocks clicked item connection");
            f.set(f.center.above(), Blocks.AIR.defaultBlockState());
            f.set(f.center, Blocks.AIR.defaultBlockState());
            f.set(f.center, pipe.defaultBlockState());
            support = (com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity) h.getLevel().getBlockEntity(f.center);
            support.setFaceManuallyDisconnected(Direction.UP, true);
            place(h, pipe, f.center, Direction.UP);
            h.assertTrue(!h.getLevel().getBlockState(f.center.above()).getValue(ItemPipeBlock.propFor(Direction.DOWN))
                    && support.isFaceManuallyDisconnected(Direction.UP), "item placement preserves neighbor manual veto");
            var placed = (com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity) h.getLevel().getBlockEntity(f.center.above());
            placed.setFaceManuallyDisconnected(Direction.DOWN, true);
            support.setFaceManuallyDisconnected(Direction.UP, false);
            placed.autoConnectOnPlace(Direction.UP);
            h.assertTrue(!placed.getBlockState().getValue(ItemPipeBlock.propFor(Direction.DOWN)), "own item manual veto survives repeated attempt");
            placed.setFaceManuallyDisconnected(Direction.DOWN, false);
            h.assertTrue(placed.attachCover(Direction.DOWN, shutter.copy()), "new pipe accepts own shutter");
            placed.panels().configure(Direction.DOWN, false, false);
            h.assertTrue(placed.panels().shuttered(Direction.DOWN), "own shutter actually closed");
            placed.autoConnectOnPlace(Direction.UP);
            h.assertTrue(!placed.getBlockState().getValue(ItemPipeBlock.propFor(Direction.DOWN)), "own shutter blocks repeated placement connection");
        }
        h.succeed();
    }

    private static <T extends Block> T registered(Class<T> type) {
        return BuiltInRegistries.BLOCK.stream().filter(type::isInstance)
                .sorted(java.util.Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).toString()))
                .map(type::cast).findFirst().orElseThrow();
    }

    private static void place(GameTestHelper h, Block block, BlockPos support, Direction face) {
//? if >=1.21.1 {
        /*var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        var player = h.makeMockPlayer();
//?}
        player.setPos(support.getX()+3.5, support.getY()+1, support.getZ()+3.5);
        var stack = new ItemStack(block, 2);
        h.assertTrue(stack.getItem() instanceof BlockItem, "registered native block item exists");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)), face, support, false);
        var result = ((BlockItem) stack.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
        h.assertTrue(result.consumesAction() && h.getLevel().getBlockState(support.relative(face)).is(block), "actual native BlockItem placement succeeded");
        h.assertTrue(stack.getCount() == 1, "survival placement consumed exactly one item");
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final BlockPos center;
        final java.util.Map<BlockPos,BlockState> states = new java.util.LinkedHashMap<>();
        final java.util.Map<BlockPos,CompoundTag> entities = new java.util.LinkedHashMap<>();
        Fixture(GameTestHelper helper, int offset) {
            h=helper; center=new BlockPos(29000008+offset,200,29000008);
            for (var mutable:BlockPos.betweenClosed(center.offset(-2,-1,-2),center.offset(2,3,2))) {
                var pos=mutable.immutable(); states.put(pos,h.getLevel().getBlockState(pos));
                var be=h.getLevel().getBlockEntity(pos);
                if(be!=null) {
//? if >=1.21.1 {
                    /*entities.put(pos,be.saveWithoutMetadata(h.getLevel().registryAccess()));
*///?} else {
                    entities.put(pos,be.saveWithoutMetadata());
//?}
                }
            }
            states.keySet().forEach(p -> set(p,Blocks.AIR.defaultBlockState()));
        }
        void set(BlockPos p,BlockState s) { h.getLevel().removeBlockEntity(p); h.getLevel().setBlock(p,s,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE); }
        public void close() {
            states.keySet().forEach(p -> set(p,Blocks.AIR.defaultBlockState()));
            states.forEach(this::set);
            entities.forEach((p,tag) -> { var be=h.getLevel().getBlockEntity(p); if(be!=null) {
//? if >=1.21.1 {
                /*be.loadWithComponents(tag,h.getLevel().registryAccess());
*///?} else {
                be.load(tag);
//?}
            }});
        }
    }
}
