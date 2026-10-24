package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import com.gregtech.gregtech.block.machine.BehaviorMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BehaviorMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.content.multiblock.PlainStructureBlocks;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.Fluids;
import java.util.List;
import java.util.Map;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class BehaviorStructureSafetyTests {
    private static void set(GameTestHelper h, BlockPos pos, Block block) {
        h.getLevel().setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
    private static BehaviorMachineBlockEntity machine(GameTestHelper h, BlockPos pos) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("addon_sample", "irrigator"));
        h.assertTrue(block instanceof BehaviorMachineBlock, "registered sample behavior machine exists");
        set(h, pos, Blocks.AIR);
        h.getLevel().setBlock(pos, block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        return (BehaviorMachineBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static MultiblockPortBlockEntity part(GameTestHelper h, BlockPos pos) {
        set(h, pos, LargeMachineParts.block(18100));
        return (MultiblockPortBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    @SuppressWarnings("unchecked")
    private static <V> Map<String,V> cache(Class<?> type, String name) {
        try {
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            return (Map<String,V>) field.get(null);
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Fixture cache unavailable", failure); }
    }
    /** Isolated post-bootstrap layout/resolution injection, restored even on failure; actual matching remains untouched. */
    private static void withLayout(LargeMachineLayouts.Layout layout, Runnable body) {
        Map<String,LargeMachineLayouts.Layout> layouts = cache(LargeMachineLayouts.class, "LAYOUTS");
        Map<String,Block> blocks = cache(PlainStructureBlocks.class, "RESOLVED");
        var oldLayout = layouts.put("irrigator", layout);
        var oldStone = blocks.put("minecraft:stone", Blocks.STONE);
        try { body.run(); }
        finally {
            if (oldLayout == null) layouts.remove("irrigator"); else layouts.put("irrigator", oldLayout);
            if (oldStone == null) blocks.remove("minecraft:stone"); else blocks.put("minecraft:stone", oldStone);
        }
    }
    private static LargeMachineLayouts.Cell mixed(boolean plainFirst) {
        return new LargeMachineLayouts.Cell(1,0,0,plainFirst ? 0 : 18100,Role.CASING,
                plainFirst ? "minecraft:stone" : null,-1,List.of(new SharedLargeMachineLayouts.Choice(
                plainFirst ? 18100 : 0,plainFirst ? null : "minecraft:stone")));
    }
    private static LargeMachineLayouts.Cell input() {
        return new LargeMachineLayouts.Cell(-1,0,0,18100,Role.ITEM_FLUID_INPUT);
    }

    @GameTest(template="test_empty")
    public static void behaviorMixedAlternativesBindActualFixedAndRepeatedParts(GameTestHelper h) {
        for (boolean plainFirst : new boolean[]{false,true}) for (boolean repeated : new boolean[]{false,true}) {
            var layout = new LargeMachineLayouts.Layout(repeated ? List.of(input()) : List.of(input(),mixed(plainFirst)),
                    repeated ? List.of(new LargeMachineLayouts.Repeat("layers",0,1,1,1,1,List.of(mixed(plainFirst)))) : List.of());
            withLayout(layout, () -> {
                var pos = h.absolutePos(new BlockPos(3,3,3));
                var machine = machine(h,pos);
                var input = part(h,pos.west());
                for (boolean usePart : new boolean[]{false,true,false,true}) {
                    set(h,pos.east(),Blocks.AIR);
                    var casing = usePart ? part(h,pos.east()) : null;
                    if (!usePart) set(h,pos.east(),Blocks.STONE);
                    h.assertTrue(StructureTestSupport.ok(machine) && StructureTestSupport.ok(machine), "actual selected branch forms on repeated checks");
                    h.assertTrue(input.isBoundTo(pos), "input role retained");
                    if (casing != null) h.assertTrue(casing.isBoundTo(pos) && casing.getEnergyTypes(null).isEmpty(),
                            "selected casing part binds without gaining energy permission");
                }
                set(h,pos,Blocks.AIR); set(h,pos.west(),Blocks.AIR); set(h,pos.east(),Blocks.AIR);
            });
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void behaviorRetainedPortRejectsBrokenStructureBeforeTickAndRebinds(GameTestHelper h) {
        withLayout(new LargeMachineLayouts.Layout(List.of(input(),mixed(false)),List.of()), () -> {
            var pos = h.absolutePos(new BlockPos(3,3,3));
            var machine = machine(h,pos);
            var originalPort = part(h,pos.west());
            int[] callbacks = {0};
            var port = new MultiblockPortBlockEntity(originalPort.getBlockPos(), originalPort.getBlockState()) {
                @Override public void bind(BlockPos owner, Role role) {
                    callbacks[0]++;
                    h.assertTrue(!StructureTestSupport.ok(machine), "reentrant request during bind fails closed, never trusts stale formed");
                    super.bind(owner,role);
                }
            };
            h.getLevel().removeBlockEntity(port.getBlockPos());
            h.getLevel().setBlockEntity(port);
            set(h,pos.east(),Blocks.STONE);
            h.assertTrue(StructureTestSupport.ok(machine) && callbacks[0]>0, "fixture forms through actual guarded bind callback without controller tick");
//? if >=1.21.1 {
/*            IItemHandler items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,port.getBlockPos(),Direction.DOWN);
            IFluidHandler fluids = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,port.getBlockPos(),Direction.DOWN);
*///?} else {
            IItemHandler items = port.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(IllegalStateException::new);
            IFluidHandler fluids = port.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.DOWN).orElseThrow(IllegalStateException::new);
//?}
            h.assertTrue(items != null && fluids != null, "real retained port handlers exist");
            h.assertTrue(items.insertItem(0,new ItemStack(Items.BONE_MEAL),false).isEmpty()
                    && fluids.fill(new FluidStack(Fluids.WATER,100),IFluidHandler.FluidAction.EXECUTE)==100, "formed port accepts both resources");
            set(h,pos.east(),Blocks.DIRT);
            h.assertTrue(items.insertItem(0,new ItemStack(Items.BONE_MEAL),false).getCount()==1
                    && fluids.fill(new FluidStack(Fluids.WATER,100),IFluidHandler.FluidAction.EXECUTE)==0,
                    "retained handlers reject removed required cell before another controller tick");
            h.assertTrue(machine.inventory().getStackInSlot(0).getCount()==1
                    && machine.fluids().getFluidInTank(0).getAmount()==100, "rejected transfers preserve controller stock");
            set(h,pos.east(),Blocks.STONE);
            h.assertTrue(StructureTestSupport.ok(machine) && port.isBoundTo(pos), "restoration revalidates and rebinds");
            h.assertTrue(items.insertItem(0,new ItemStack(Items.BONE_MEAL),false).isEmpty()
                    && fluids.fill(new FluidStack(Fluids.WATER,100),IFluidHandler.FluidAction.EXECUTE)==100,
                    "same retained relay resumes only after valid reformation");
            machine.setRemoved();
            h.assertTrue(!StructureTestSupport.ok(machine) && fluids.fill(new FluidStack(Fluids.WATER,1),IFluidHandler.FluidAction.EXECUTE)==0,
                    "removed controller cannot validate or receive through retained port");
            set(h,pos,Blocks.AIR); set(h,pos.west(),Blocks.AIR); set(h,pos.east(),Blocks.AIR);
        });
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=200)
    public static void behaviorEnergyPollingDoesNotLoadRemoteBorderNeighbor(GameTestHelper h) {
        var pos = new BlockPos(18000008,80,-18000001); // z%16=15; BACK/south crosses into the next chunk.
        var outside = pos.south();
        h.assertTrue(!h.getLevel().hasChunkAt(outside), "remote behavior neighbor starts unloaded");
        var machine = machine(h,pos);
        h.assertTrue(!h.getLevel().hasChunkAt(outside), "known-shape fixture writes preserve unloaded exterior");
        h.assertTrue(machine.isEnergyAcceptingFrom("EU",Direction.SOUTH,false),
                "selected exterior is a genuine energy input");
        BehaviorMachineBlockEntity.serverTick(h.getLevel(),pos,machine.getBlockState(),machine);
        h.assertTrue(!h.getLevel().hasChunkAt(outside) && machine.storedEnergy()==0,
                "actual behavior tick neither loads absent energy neighbor nor fabricates energy");
        set(h,pos,Blocks.AIR);
        h.succeed();
    }
}
