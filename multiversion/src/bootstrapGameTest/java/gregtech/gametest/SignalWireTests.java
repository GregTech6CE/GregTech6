package gregtech.gametest;

import gregtech.api.machine.MachineControl;
import gregtech.internal.tool.*;
import gregtech.content.block.energy.*;
import gregtech.content.blockentity.energy.SignalWireBlockEntity;
import gregtech.content.material.Materials;
import gregtech.content.recipe.WireProcessingRecipes;
import gregtech.content.item.GTToolItem;
import gregtech.content.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.*;
//?}

//? if >=1.21.1 {
/*/^* NeoForge counterpart of the Forge signal-wire behavior tests. ^/
@GameTestHolder("gregtech_signal_wire") @PrefixGameTestTemplate(false)
*///?} else {
@GameTestHolder("gt6ce") @PrefixGameTestTemplate(false)
//?}
public final class SignalWireTests {
    private static SignalWireBlock block(String material,boolean insulated){
        return GTSignalWires.all().stream().map(r->r.get()).filter(b->b.material().getName().equalsIgnoreCase(material)&&b.insulated()==insulated).findFirst().orElseThrow();
    }
    private static SignalWireBlockEntity place(GameTestHelper h,BlockPos pos,SignalWireBlock block){
        var state=block.defaultBlockState();
        for(var property:ElectricWireBlock.CONNECTIONS)state=state.setValue(property,true);
        h.getLevel().setBlockAndUpdate(pos,state);
        return (SignalWireBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void solve(SignalWireBlockEntity wire){wire.setMode(wire.mode());wire.tickNetwork();}
//? if >=1.21.1 {
/*    @GameTest(template="test_empty") public static void signalSourceDirectionsComparatorAndLight(GameTestHelper h){
*///?} else {
    @GameTest(template="test_blueprint_empty") public static void signalSourceDirectionsComparatorAndLight(GameTestHelper h){
//?}
        var pos=h.absolutePos(new BlockPos(4,4,4));
        h.getLevel().setBlockAndUpdate(pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.north(),Blocks.STONE.defaultBlockState());
        var wire=place(h,pos,block("Lumium",false));solve(wire);
        h.assertTrue(wire.output(Direction.WEST)==0,"No feedback into receiving face");
        h.assertTrue(wire.output(Direction.SOUTH)==15,"Full weak output into non-solid consumer");
        h.assertTrue(wire.output(Direction.NORTH)==14,"One lower output into solid block");
        h.assertTrue(wire.comparator()==14,"GT6 comparator floors fractional signal");
        h.assertTrue(h.getLevel().getBlockState(pos).getLightEmission(h.getLevel(),pos)==15,"Lumium bare wire is a powered lamp");
        h.getLevel().setBlockAndUpdate(pos.west(),Blocks.AIR.defaultBlockState());solve(wire);
        h.assertTrue(wire.signal()==0&&h.getLevel().getBlockState(pos).getValue(SignalWireBlock.POWER)==0,"Source removal clears state and lamp");
        h.succeed();
    }
//? if >=1.21.1 {
/*    @GameTest(template="test_empty") public static void originalFractionalRangeAndMixedCable(GameTestHelper h){
*///?} else {
    @GameTest(template="test_blueprint_empty") public static void originalFractionalRangeAndMixedCable(GameTestHelper h){
//?}
        var origin=h.absolutePos(new BlockPos(2,2,2));
        // Isolate long runs above the shared test fixtures, without force-loading any chunk.
        var base=new BlockPos(origin.getX(),h.getLevel().getMaxBuildHeight()-40,origin.getZ());
        var placed=new java.util.ArrayList<BlockPos>();
        try {
            for(String material:new String[]{"RedAlloy","Signalum"}){
                int range=material.equals("Signalum")?64:16;
                var start=base.offset(0,0,material.equals("Signalum")?3:0);
                SignalWireBlockEntity first=null,last=null;
                for(int i=0;i<=range;i++){
                    var pos=start.east(i);
                    h.assertTrue(h.getLevel().hasChunkAt(pos),"Test run must stay in loaded chunks");
                    placed.add(pos);last=place(h,pos,block(material,(i&1)==1));if(i==0)first=last;
                }
                first.setMode(15);first.tickNetwork();
                h.assertTrue(last.signal()==15*SignalWireBlockEntity.UNIT-(range+1)*(SignalWireBlockEntity.UNIT/range),"Original integer loss after "+range+" hops");
                h.assertTrue(last.output(Direction.SOUTH)==14,"Range drops exactly one signal level");
                first.setMode(0);first.tickNetwork();h.assertTrue(last.signal()==0,"Long cable clears without decay tail");
            }
        } finally {for(var pos:placed)h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());}
        h.succeed();
    }
//? if >=1.21.1 {
/*    @GameTest(template="test_empty") public static void loopUnlatchesAndCutSeparatesNetwork(GameTestHelper h){
*///?} else {
    @GameTest(template="test_blueprint_empty") public static void loopUnlatchesAndCutSeparatesNetwork(GameTestHelper h){
//?}
        var pos=h.absolutePos(new BlockPos(4,4,4));var type=block("RedAlloy",false);
        var a=place(h,pos,type);var b=place(h,pos.east(),type);
        var c=place(h,pos.east().south(),type);var d=place(h,pos.south(),type);
        a.setMode(12);a.tickNetwork();h.assertTrue(c.output(Direction.UP)==12,"Loop transmits signal");
        a.setMode(0);a.tickNetwork();h.assertTrue(a.signal()==0&&b.signal()==0&&c.signal()==0&&d.signal()==0,"Unpowered ring cannot self-latch");
        a.setMode(9);a.tickNetwork();
        h.getLevel().setBlockAndUpdate(pos,a.getBlockState().setValue(ElectricWireBlock.EAST,false).setValue(ElectricWireBlock.SOUTH,false));
        solve(b);h.assertTrue(b.signal()==0&&c.signal()==0&&d.signal()==0,"Both ends must agree to connect");
        h.succeed();
    }
//? if >=1.21.1 {
/*    @GameTest(template="test_empty") public static void modesPersistAndControlCannotMutateRemovedWire(GameTestHelper h){
*///?} else {
    @GameTest(template="test_blueprint_empty") public static void modesPersistAndControlCannotMutateRemovedWire(GameTestHelper h){
//?}
        var pos=h.absolutePos(new BlockPos(4,4,4));var wire=place(h,pos,block("Lumium",true));
        var control=MachineControl.find(wire,Direction.UP);control.setMode(99);wire.tickNetwork();
        h.assertTrue(control.mode()==15&&control.progress()>14000&&control.progressMax()==16000,"Relay exposes original mode and progress scale");
        h.assertTrue(wire.getBlockState().getLightEmission(h.getLevel(),pos)==0,"Insulated Lumium is not a world lamp");
//? if >=1.21.1 {
/*        var saved=wire.saveWithoutMetadata(h.getLevel().registryAccess());
        var copy=new SignalWireBlockEntity(pos,wire.getBlockState());
        copy.loadWithComponents(saved,h.getLevel().registryAccess());
*///?} else {
        var copy=new SignalWireBlockEntity(pos,wire.getBlockState());copy.load(wire.saveWithoutMetadata());
//?}
        h.assertTrue(copy.mode()==15&&copy.signal()==0,"Persist source mode, recompute transient network power");
        wire.setRemoved();control.setMode(2);h.assertTrue(!control.available()&&wire.mode()==15,"Cached control cannot edit removed wire");
        h.succeed();
    }
//? if >=1.21.1 {
/*    @GameTest(template="test_empty") public static void reloadedLampClearsStaleBlockState(GameTestHelper h){
*///?} else {
    @GameTest(template="test_blueprint_empty") public static void reloadedLampClearsStaleBlockState(GameTestHelper h){
//?}
        var pos=h.absolutePos(new BlockPos(4,4,4));var wire=place(h,pos,block("Lumium",false));
        h.getLevel().setBlockAndUpdate(pos,wire.getBlockState().setValue(SignalWireBlock.POWER,15));
//? if >=1.21.1 {
/*        wire.loadWithComponents(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());solve(wire);
*///?} else {
        wire.load(new net.minecraft.nbt.CompoundTag());solve(wire);
//?}
        h.assertTrue(wire.signal()==0&&wire.getBlockState().getValue(SignalWireBlock.POWER)==0,"Unpowered reloaded lamp clears persisted light");
        h.succeed();
    }
//? if >=1.21.1 {
/*    @GameTest(template="test_empty") public static void allSixCutterFacesAndRecipeRoutes(GameTestHelper h){
*///?} else {
    @GameTest(template="test_blueprint_empty") public static void allSixCutterFacesAndRecipeRoutes(GameTestHelper h){
//?}
        var pos=h.absolutePos(new BlockPos(4,4,4));var wire=place(h,pos,block("RedAlloy",false));
//? if >=1.21.1 {
/*        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        var player=h.makeMockPlayer();
//?}
        var tool=GTToolItem.create(gregtech.internal.tool.GTToolType.WIRE_CUTTER,Materials.Steel,gregtech.internal.material.GTMaterialRegistry.get("Wood"));
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        for(var side:Direction.values()){
            var other=place(h,pos.relative(side),block("Signalum",true));
            var hit=new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(side.getNormal()).scale(.5)),side,pos,false);
//? if >=1.21.1 {
/*            wire.getBlockState().useItemOn(player.getItemInHand(InteractionHand.MAIN_HAND),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
*///?} else {
            wire.getBlockState().use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
//?}
            h.assertTrue(!wire.connected(side)&&!other.connected(side.getOpposite()),"Cutter disconnects both ports: "+side);
//? if >=1.21.1 {
/*            wire.getBlockState().useItemOn(player.getItemInHand(InteractionHand.MAIN_HAND),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
*///?} else {
            wire.getBlockState().use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
//?}
            h.assertTrue(wire.connected(side)&&other.connected(side.getOpposite()),"Cutter reconnects both ports: "+side);
        }
        for(var entry:GTSignalWires.all()){
            long recipes=WireProcessingRecipes.entries().stream().filter(e->e.recipe().mOutputs[0].is(entry.get().asItem())).count();
            h.assertTrue(recipes==(entry.get().insulated()?2:1),"Wiremill and plate/foil laminator routes: "+entry.getId());
        }
        h.succeed();
    }
}
