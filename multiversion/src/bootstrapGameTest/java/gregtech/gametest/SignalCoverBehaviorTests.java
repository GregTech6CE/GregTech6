package gregtech.gametest;

import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.tool.GTToolHelper;
import gregtech.internal.tool.GTToolType;
import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.block.energy.SignalWireBlock;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.blockentity.energy.SignalWireBlockEntity;
import gregtech.content.cover.PanelCoverHost;
import gregtech.content.registry.GTSignalWires;
import gregtech.content.registry.GTToolItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
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

/** Native registered-world attachment circuits; no direct PanelCoverRuntime.afterTick calls. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class SignalCoverBehaviorTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static SignalWireBlock wire(boolean insulated) {
        return GTSignalWires.all().stream().map(r -> r.get())
                .filter(b -> b.material().getName().equals("RedAlloy") && b.insulated() == insulated)
                .findFirst().orElseThrow();
    }
    private static int mask(BlockState state) {
        int result = 0;
        for (var side : Direction.values()) if (state.getValue(ElectricWireBlock.propFor(side))) result |= 1 << side.ordinal();
        return result;
    }
    private static BlockState ports(SignalWireBlock block, int mask) {
        var state = block.defaultBlockState();
        for (var side : Direction.values()) state = state.setValue(ElectricWireBlock.propFor(side), (mask & 1 << side.ordinal()) != 0);
        return state;
    }
    private static CompoundTag save(GameTestHelper h, BlockEntity entity) {
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }
    private static void load(GameTestHelper h, BlockEntity entity, CompoundTag data) {
//? if >=1.21.1 {
/*        entity.loadWithComponents(data, h.getLevel().registryAccess());
*///?} else {
        entity.load(data);
//?}
    }
    private static final class Fixture implements AutoCloseable {
        private record Saved(BlockState state, CompoundTag data) {}
        final GameTestHelper h;
        final BlockPos pos;
        final ServerPlayer player;
        final Map<BlockPos, Saved> saved = new LinkedHashMap<>();
        Fixture(GameTestHelper h, int index) {
            this.h = h;
            pos = new BlockPos(21_960_000 + index * 128, 230, 21_960_000);
//? if >=1.21.1 {
/*            player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
            player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[gt-signal-port]"));
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.setShiftKeyDown(false);
            clear();
        }
        void set(BlockPos at, BlockState state) {
            if (!saved.containsKey(at)) {
                var entity = h.getLevel().getBlockEntity(at);
                saved.put(at.immutable(), new Saved(h.getLevel().getBlockState(at), entity == null ? null : save(h, entity)));
            }
            h.getLevel().removeBlockEntity(at);
            h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(at, state, FLAGS);
        }
        void clear() {
            for (var at : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) set(at, Blocks.AIR.defaultBlockState());
        }
        SignalWireBlockEntity node(BlockPos at, boolean insulated, int initialMask) {
            set(at, ports(wire(insulated), initialMask));
            var result = h.getLevel().getBlockEntity(at);
            h.assertTrue(result instanceof SignalWireBlockEntity, "actual registered signal-wire entity");
            return (SignalWireBlockEntity) result;
        }
        void click(BlockPos at, Direction side) {
            var normal = Vec3.atLowerCornerOf(side.getNormal());
            var point = Vec3.atCenterOf(at).add(normal.scale(.5));
            var stand = Vec3.atCenterOf(at).add(normal.scale(2.5));
            player.setPos(stand.x, stand.y, stand.z);
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(point, side, at, false));
        }
        SignalWireBlockEntity place(Direction supportSide) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(wire(false), 2));
            click(pos.relative(supportSide), supportSide.getOpposite());
            h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof SignalWireBlockEntity
                    && player.getMainHandItem().getCount() == 1, "native BlockItem placement succeeds and consumes exactly one wire");
            return (SignalWireBlockEntity) h.getLevel().getBlockEntity(pos);
        }
        void cutter() {
            player.setItemInHand(InteractionHand.MAIN_HAND, GTToolHelper.write(GTToolItems.empty(GTToolType.WIRE_CUTTER),
                    GTMaterialRegistry.get("Steel"), GTMaterialRegistry.get("Wood")));
        }
        @Override public void close() {
            player.closeContainer();
            player.getInventory().clearContent();
            player.discard();
            for (var at : saved.keySet()) {
                h.getLevel().removeBlockEntity(at);
                h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
            }
            for (var entry : saved.entrySet()) {
                h.getLevel().setBlock(entry.getKey(), entry.getValue().state(), FLAGS);
                var entity = h.getLevel().getBlockEntity(entry.getKey());
                if (entity != null && entry.getValue().data() != null) load(h, entity, entry.getValue().data());
            }
        }
    }

    /** Drive the registered BE ticker and fallback component lifecycle at isolated loaded positions.
     * This is deliberately not a direct output/helper assertion: real world source, conductor,
     * receiver network and dust neighbor updates participate in every circuit below. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void tick(Fixture f, SignalWireBlockEntity node) {
        node.setMode(node.mode()); // Invalidate same-tick solution between explicit circuit phases.
        var state=node.getBlockState();
        var ticker=((SignalWireBlock)state.getBlock()).getTicker(f.h.getLevel(),state,node.getType());
        f.h.assertTrue(ticker!=null,"registered native server ticker exists");
        ((net.minecraft.world.level.block.entity.BlockEntityTicker)ticker).tick(f.h.getLevel(),node.getBlockPos(),state,node);
        var storage=((gregtech.content.cover.FallbackCoverHost)(Object)node).gregtechComponentStorage(false);
        if(storage!=null)storage.tick();
    }
    private static void attach(Fixture f, Direction side, boolean torch) {
        f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(torch?Items.REDSTONE_TORCH:Items.REPEATER,2));
        f.click(f.pos,side);
        var host=(PanelCoverHost)f.h.getLevel().getBlockEntity(f.pos);
        f.h.assertTrue(host.getCover(side).is(torch?Items.REDSTONE_TORCH:Items.REPEATER)
                &&f.player.getMainHandItem().getCount()==1,"native vanilla item-first cover attachment");
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void nativeTorchAndRepeaterStrongRelayThroughSolid(GameTestHelper h) {
        var observations=new ArrayList<String>(); boolean correct=true;
        try(var f=new Fixture(h,0)) {
            for(boolean insulated:new boolean[]{false,true})for(boolean torch:new boolean[]{false,true})for(var side:Direction.values()) {
                f.clear();
                var source=f.node(f.pos,insulated,1<<side.getOpposite().ordinal());
                f.set(f.pos.relative(side),Blocks.STONE.defaultBlockState());
                var receiver=f.node(f.pos.relative(side,2),!insulated,1<<side.getOpposite().ordinal());
                attach(f,side,torch);
                for(boolean on:new boolean[]{false,true,false,true,false}) {
                    f.set(f.pos.relative(side.getOpposite()),on?Blocks.REDSTONE_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState());
                    tick(f,source);tick(f,receiver);
                    int expected=on!=torch?15:0;
                    int weak=h.getLevel().getBlockState(f.pos).getSignal(h.getLevel(),f.pos,side.getOpposite());
                    int strong=h.getLevel().getBlockState(f.pos).getDirectSignal(h.getLevel(),f.pos,side.getOpposite());
                    int throughStone=h.getLevel().getSignal(f.pos.relative(side),side.getOpposite());
                    long expectedReceived=expected==0?0:15*SignalWireBlockEntity.UNIT-receiver.loss();
                    boolean row=weak==expected&&strong==expected&&throughStone==expected&&receiver.signal()==expectedReceived
                            &&!source.connected(side);
                    observations.add("insulated="+insulated+", torch="+torch+", side="+side+", input="+on+", weak/strong/solid="
                            +weak+"/"+strong+"/"+throughStone+", receiver="+receiver.signal()+" expected="+expectedReceived);
                    correct &= row;
                }
            }
        }
        h.assertTrue(correct,"original stored post-tick attachment strong/weak output must feed vanilla solid conductor then native wire: "+observations);
        h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void nativeAttachmentDustConnectionsAndNotifications(GameTestHelper h) {
        var observations=new ArrayList<String>();boolean correct=true;
        try(var f=new Fixture(h,1)) {
            for(boolean insulated:new boolean[]{false,true})for(boolean torch:new boolean[]{false,true})
                for(var side:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
                    f.clear();
                    var source=f.node(f.pos,insulated,1<<Direction.DOWN.ordinal());
                    var dust=f.pos.relative(side);
                    f.set(dust.below(),Blocks.STONE.defaultBlockState());
                    attach(f,side,torch);
                    // Real placement state computes vanilla wire arms using canConnectRedstone.
                    f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.REDSTONE));
                    f.click(dust.below(),Direction.UP);
                    h.assertTrue(h.getLevel().getBlockState(dust).is(Blocks.REDSTONE_WIRE),"native dust item placement");
                    for(boolean on:new boolean[]{false,true,false}) {
                        f.set(f.pos.below(),on?Blocks.REDSTONE_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState());
                        tick(f,source);
                        var state=h.getLevel().getBlockState(dust);
                        int actual=state.getValue(net.minecraft.world.level.block.RedStoneWireBlock.POWER);
                        int expected=on!=torch?15:0;
                        boolean arm=state.getValue(net.minecraft.world.level.block.RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(side.getOpposite()))
                                !=net.minecraft.world.level.block.state.properties.RedstoneSide.NONE;
                        boolean connects=((SignalWireBlock)source.getBlockState().getBlock()).canConnectRedstone(source.getBlockState(),h.getLevel(),f.pos,side.getOpposite());
                        observations.add("insulated="+insulated+", torch="+torch+", side="+side+", input="+on+", dust="+actual+", arm="+arm+", connects="+connects);
                        correct &=actual==expected&&arm&&connects&&!source.connected(side);
                    }
                }
        }
        h.assertTrue(correct,"closed cover port still exposes vanilla dust source and notifies actual dust on both edges: "+observations);
        h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void nativeUncoveredWireFeedbackAndAttachmentIntakeControls(GameTestHelper h) {
        try(var f=new Fixture(h,2)) {
            for(boolean insulated:new boolean[]{false,true}) {
                f.clear();
                var source=f.node(f.pos,insulated,(1<<Direction.WEST.ordinal())|(1<<Direction.EAST.ordinal()));
                f.set(f.pos.east(),Blocks.STONE.defaultBlockState());
                f.set(f.pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState());
                tick(f,source);
                h.assertTrue(source.signal()>0,"positive control: native redstone source powers wire");
                f.set(f.pos.west(),Blocks.AIR.defaultBlockState());tick(f,source);
                h.assertTrue(source.signal()==0,"uncovered output cannot sustain itself through its strongly powered solid neighbor");
                // Repeat with actual vanilla dust carrying the old wire output.
                f.set(f.pos.east(),Blocks.AIR.defaultBlockState());
                f.set(f.pos.east().below(),Blocks.STONE.defaultBlockState());
                f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.REDSTONE));
                f.click(f.pos.east().below(),Direction.UP);
                f.set(f.pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState());tick(f,source);
                f.set(f.pos.west(),Blocks.AIR.defaultBlockState());tick(f,source);
                // Dust may retain one update of its own stored signal; drain it with normal neighbor updates.
                for(int step=0;step<32;step++) { h.getLevel().updateNeighborsAt(f.pos,source.getBlockState().getBlock());tick(f,source); }
                h.assertTrue(source.signal()==0,"uncovered wire/dust circuit clears instead of self-sustaining");
                f.clear();
                var isolated=f.node(f.pos,insulated,1<<Direction.EAST.ordinal());
                attach(f,Direction.EAST,true);
                f.set(f.pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());tick(f,isolated);
                h.assertTrue(isolated.signal()==0&&!isolated.connected(Direction.EAST),"attachment output face cannot take input from adjacent vanilla source");
                // Original getRedstoneAtSide prioritizes wire protocol, not cover output: no direct bridge.
                var adjacent=f.node(f.pos.east(),!insulated,1<<Direction.WEST.ordinal());
                tick(f,isolated);tick(f,adjacent);
                h.assertTrue(adjacent.signal()==0,"cover must not invent direct wire-to-wire bridging across a closed port");
            }
        }
        h.succeed();
    }
}
