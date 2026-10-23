package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.EngineBlock;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.blockentity.machine.KineticDieselEngineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.gregtech.gregtech.api.machine.EngineType;
import com.gregtech.gregtech.data.FuelRecipeMaps;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.content.cover.PanelCoverHost;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Native registered-engine regressions; never substitutes mock recipe maps or engine subclasses. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class FuelEngineIntakeTests {
    private static java.util.List<EngineBlock> engines() {
        return BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof EngineBlock e && e.engineType()==EngineType.DIESEL)
            .map(b -> (EngineBlock)b).sorted(java.util.Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).toString())).toList();
    }
    private static KineticDieselEngineBlockEntity engine(Fixture f, EngineBlock block, Direction facing) {
        f.set(f.center,Blocks.AIR.defaultBlockState());
        f.set(f.center,block.defaultBlockState().setValue(EngineBlock.FACING,facing));
        var be=(KineticDieselEngineBlockEntity)f.h.getLevel().getBlockEntity(f.center);
        f.h.assertTrue(be!=null && be.spec()!=null,"registered engine BE/spec");
        ((PanelCoverHost)(Object)be).panels(); // initialize lazy cover state before simulation snapshots
        be.setStopped(true); // isolate transfers from combustion, not an intake veto in GT6
        return be;
    }
    private static IFluidHandler fluids(GameTestHelper h,BlockPos pos,Direction side) {
//? if >=1.21.1 {
        /*return h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,pos,side);
*///?} else {
        var be=h.getLevel().getBlockEntity(pos);
        return be==null?null:be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,side).orElse(null);
//?}
    }
    private static CompoundTag snapshot(GameTestHelper h,net.minecraft.world.level.block.entity.BlockEntity be) {
//? if >=1.21.1 {
        /*return be.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return be.saveWithoutMetadata();
//?}
    }
    private static void restore(GameTestHelper h,net.minecraft.world.level.block.entity.BlockEntity be,CompoundTag tag) {
//? if >=1.21.1 {
        /*be.loadWithComponents(tag,h.getLevel().registryAccess());
*///?} else {
        be.load(tag);
//?}
    }
    private static FluidStack fuel(GameTestHelper h,String field,int amount) {
        var fluid=GTFluids.stack(field,amount);
        h.assertTrue(fluid!=null&&!fluid.isEmpty(),"real registered fuel field "+field);
        return fluid;
    }

    @GameTest(template="test_empty",timeoutTicks=200)
    public static void allEightDieselsAcceptKnownFuelOnAllSixFacesWithoutSimulationMutation(GameTestHelper h) {
        h.assertTrue(engines().size()==8,"all eight registered diesel variants, not a fabricated fixture");
        var diesel=fuel(h,"Diesel",17);
        h.assertTrue(FuelRecipeMaps.Engine.containsInput(diesel),"bootstrapped real Engine recipe map has diesel");
        for(var block:engines())for(var facing:Direction.values())try(var f=new Fixture(h,0)) {
            var be=engine(f,block,facing);
            for(var side:Direction.values()) {
                be.fuelTank().setEmpty();
                var handler=fluids(h,f.center,side);
                h.assertTrue(handler!=null&&handler.getTanks()==2,"native capability all original faces "+side);
                h.assertTrue(handler.getTankCapacity(0)==be.spec().outputRate()*10,"source mRate*10 tank capacity");
                var before=snapshot(h,be);
                h.assertTrue(handler.fill(diesel,FluidAction.SIMULATE)==17,"valid fuel simulation "+side);
                h.assertTrue(snapshot(h,be).equals(before)&&diesel.getAmount()==17,"simulation leaves NBT and offered stack unchanged");
                var water=new FluidStack(net.minecraft.world.level.material.Fluids.WATER,17);
                h.assertTrue(!handler.isFluidValid(0,water)&&handler.fill(water,FluidAction.SIMULATE)==0&&handler.fill(water,FluidAction.EXECUTE)==0,"water is not engine fuel");
                h.assertTrue(snapshot(h,be).equals(before),"invalid fill conserves both tanks/control state");
                h.assertTrue(handler.fill(diesel,FluidAction.EXECUTE)==17&&be.fuelTank().getAmount()==17&&be.exhaustTank().isEmpty(),"commit moves exactly accepted fuel only");
                h.assertTrue(handler.drain(17,FluidAction.EXECUTE).isEmpty()&&be.fuelTank().getAmount()==17,"normal capability cannot extract input fuel");
                int left=handler.getTankCapacity(0)-17;
                var excess=fuel(h,"Diesel",handler.getTankCapacity(0)+1);
                h.assertTrue(handler.fill(excess,FluidAction.SIMULATE)==left&&be.fuelTank().getAmount()==17,"partial simulation uses remaining capacity");
                h.assertTrue(handler.fill(excess,FluidAction.EXECUTE)==left&&be.fuelTank().getAmount()==handler.getTankCapacity(0),"partial commit cannot overflow");
            }
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void sourceFuelsHaveRegisteredRecipesAndNativeIntake(GameTestHelper h) {
        var missing=new java.util.ArrayList<String>();
        for(String key:java.util.List.of("Kerosine","JetFuel","BioDiesel","BioFuel","BioEthanol","Reikanol","NitroFuel","GenGas_Ethanol")) {
            var input=fuel(h,key,1);
            if(!FuelRecipeMaps.Engine.containsInput(input))missing.add(key+"="+BuiltInRegistries.FLUID.getKey(input.getFluid()));
        }
        h.assertTrue(missing.isEmpty(),"original valid fuels missing from real Engine map: "+missing);
        for(var block:engines())try(var f=new Fixture(h,16)) {
            var be=engine(f,block,Direction.NORTH);
            for(String key:java.util.List.of("Kerosine","JetFuel","BioDiesel","BioFuel","BioEthanol","Reikanol","NitroFuel","GenGas_Ethanol"))for(var side:Direction.values()) {
                be.fuelTank().setEmpty();var input=fuel(h,key,1);var cap=fluids(h,f.center,side);
                var before=snapshot(h,be);
                h.assertTrue(cap.fill(input,FluidAction.SIMULATE)==1&&snapshot(h,be).equals(before),"source fuel simulation "+key+" "+side);
                h.assertTrue(cap.fill(input,FluidAction.EXECUTE)==1&&be.fuelTank().getAmount()==1,"source fuel commit "+key+" "+side);
            }
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void nativePipePlacementAndTransferReachEachEngineFace(GameTestHelper h) {
        var pipe=(FluidPipeBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","pipe_small_steel"));
        for(var side:Direction.values())try(var f=new Fixture(h,32)) {
            var be=engine(f,engines().get(0),Direction.NORTH);
            place(h,pipe,f.center,side);
            var pos=f.center.relative(side);var state=h.getLevel().getBlockState(pos);
            h.assertTrue(state.getValue(FluidPipeBlock.propFor(side.getOpposite())),"real placement connects clicked diesel face "+side);
            var source=(FluidPipeBlockEntity)h.getLevel().getBlockEntity(pos);
            h.assertTrue(source.fill(fuel(h,"Diesel",30),FluidAction.EXECUTE)==30,"real steel pipe holds test fuel");
            for(int i=0;i<20;i++)FluidPipeBlockEntity.serverTick(h.getLevel(),pos,source.getBlockState(),source);
            long remaining=0;for(int i=0;i<source.getTanks();i++)remaining+=source.getFluidInTank(i).getAmount();
            h.assertTrue(be.fuelTank().getAmount()>0&&remaining+be.fuelTank().getAmount()==30,"native pipe transfer accepts and conserves fuel "+side);
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void stoppedModeNbtAndSidedCoverRemainIndependentOfFuelAdmission(GameTestHelper h) {
        try(var f=new Fixture(h,48)) {
            var be=engine(f,engines().get(0),Direction.NORTH);
            var east=fluids(h,f.center,Direction.EAST);var west=fluids(h,f.center,Direction.WEST);
            h.assertTrue(east.fill(fuel(h,"Diesel",7),FluidAction.EXECUTE)==7,"stopped machine can refuel");
            var saved=snapshot(h,be);be.setStopped(false);be.fuelTank().setEmpty();restore(h,be,saved);
            h.assertTrue(be.isStopped()&&be.fuelTank().getAmount()==7&&snapshot(h,be).equals(saved),"native NBT round trip retains fuel/stopped/config");
            var host=(PanelCoverHost)(Object)be;
            var shutter=new ItemStack(com.gregtech.gregtech.registry.GTTechnological.get(com.gregtech.gregtech.content.cover.PanelCover.SHUTTER.id));
            h.assertTrue(host.attachCover(Direction.EAST,shutter),"actual component cover attaches");
            host.panels().configure(Direction.EAST,false,false);
            h.assertTrue(host.panels().shuttered(Direction.EAST),"closed real shutter fixture");
            h.assertTrue(east.fill(fuel(h,"Diesel",1),FluidAction.SIMULATE)==0&&east.fill(fuel(h,"Diesel",1),FluidAction.EXECUTE)==0,"cached sided handler obeys new shutter");
            h.assertTrue(west.fill(fuel(h,"Diesel",1),FluidAction.EXECUTE)==1,"other face remains open despite first-handler cache");
            host.removeCover(Direction.EAST);
            h.assertTrue(fluids(h,f.center,Direction.EAST).fill(fuel(h,"Diesel",1),FluidAction.EXECUTE)==1,"cover removal restores intake");
            be.setStopped(false);h.assertTrue(west.fill(fuel(h,"Diesel",1),FluidAction.EXECUTE)==1,"running mode also admits fuel");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void finitePortableContainerUsesItsNativeItemFirstTransfer(GameTestHelper h) {
        try(var f=new Fixture(h,64)) {
            var be=engine(f,engines().get(0),Direction.NORTH);
            var container=BuiltInRegistries.ITEM.stream().filter(i->i instanceof com.gregtech.gregtech.item.PortableFluidContainerItem)
                .map(ItemStack::new).filter(s->FluidUtil.getFluidHandler(s).map(c->c.fill(fuel(h,"Diesel",100),FluidAction.SIMULATE)==100).orElse(false)).findFirst().orElseThrow();
            var expectedEmptyItem=container.getItem();
            var itemCap=FluidUtil.getFluidHandler(container).orElseThrow(() -> new IllegalStateException("Missing finite container fluid capability"));
            h.assertTrue(itemCap.fill(fuel(h,"Diesel",100),FluidAction.EXECUTE)==100,"finite portable fixture has 100mB");
            var player=actor(h,f.center);
            player.setItemInHand(InteractionHand.MAIN_HAND,itemCap.getContainer());
            var hit=new BlockHitResult(Vec3.atCenterOf(f.center),Direction.UP,f.center,false);
            int inventoryBefore=inventoryCount(player);
            var held=player.getMainHandItem();
            var result=player.gameMode.useItemOn(player,h.getLevel(),held,InteractionHand.MAIN_HAND,hit);
            int remaining=FluidUtil.getFluidContained(player.getMainHandItem()).orElse(FluidStack.EMPTY).getAmount();
            h.assertTrue(result.consumesAction()&&be.fuelTank().getAmount()==100&&remaining==0,"actual item-first transfer conserves finite 100mB");
            h.assertTrue(player.getMainHandItem().getItem()==expectedEmptyItem&&player.getMainHandItem().getCount()==1
                &&inventoryCount(player)==inventoryBefore,"finite empty vessel identity/count retained without duplication or deletion");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void creativeDisplayFuelCarrierFullDispatchCanRefuelEngine(GameTestHelper h) {
        try(var f=new Fixture(h,80)) {
            var be=engine(f,engines().get(0),Direction.NORTH);
            var carrier=new ItemStack(com.gregtech.gregtech.registry.GTFluidItems.get("Diesel"));
            h.assertTrue(FluidUtil.getFluidHandler(carrier).map(c->!c.drain(1,FluidAction.SIMULATE).isEmpty()).orElse(false),"real carrier exposes diesel item capability");
            var player=actor(h,f.center);
            player.setItemInHand(InteractionHand.MAIN_HAND,carrier);var hit=new BlockHitResult(Vec3.atCenterOf(f.center),Direction.UP,f.center,false);
            var before=carrier.copy();
            player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
            h.assertTrue(be.fuelTank().isEmpty()&&player.getMainHandItem().getCount()==before.getCount(),"source display carrier does not fuel engines in survival");
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
            h.assertTrue(be.fuelTank().getAmount()==be.fuelTank().getCapacity(),"original creative-only carrier first-use fills engine through full native dispatch");
            h.assertTrue(player.getMainHandItem().getItem()==before.getItem()&&player.getMainHandItem().getCount()==before.getCount(),"display carrier is intentionally inexhaustible, not a finite-container conservation claim");
        }
        h.succeed();
    }
    private static int inventoryCount(net.minecraft.world.entity.player.Player player) {
        int total=0;for(int i=0;i<player.getInventory().getContainerSize();i++)total+=player.getInventory().getItem(i).getCount();return total;
    }

    private static ItemStack waterCarrier(GameTestHelper h) {
//? if >=1.21.1 {
        /*return com.gregtech.gregtech.api.fluid.FluidDisplayBinding.display(h.getLevel().registryAccess(),new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000));
*///?} else {
        return com.gregtech.gregtech.api.fluid.FluidDisplayBinding.display(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000));
//?}
    }

    private static net.minecraft.server.level.ServerPlayer actor(GameTestHelper h,BlockPos pos) {
//? if >=1.21.1 {
        /*var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"[gt-fuel]"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().clearContent();player.setPos(pos.getX()+2,pos.getY(),pos.getZ()+.5);
        return player;
    }

    @GameTest(template="test_empty")
    public static void unrelatedWoodenBucketAndWaterCarrierDoNotConsumeOrFill(GameTestHelper h) {
        try(var f=new Fixture(h,96)) {
            var be=engine(f,engines().get(0),Direction.NORTH);var player=actor(h,f.center);
            var bucket=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","wooden_bucket_copper_water"));
            h.assertTrue(bucket instanceof com.gregtech.gregtech.item.WoodenBucketItem,"registered wooden water bucket, not AIR");
            var water=waterCarrier(h);
            h.assertTrue(!water.isEmpty(),"actual water display carrier registered");
            for(var mode:java.util.List.of(net.minecraft.world.level.GameType.SURVIVAL,net.minecraft.world.level.GameType.CREATIVE))
            for(var offered:java.util.List.of(new ItemStack(bucket),water)) {
                player.setGameMode(mode);var stack=offered.copy();
                h.assertTrue(FluidUtil.getFluidContained(stack).map(fluid->fluid.getFluid()==net.minecraft.world.level.material.Fluids.WATER).orElse(false),"invalid fixture really contains water");
                player.setItemInHand(InteractionHand.OFF_HAND,stack);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(net.minecraft.world.item.Items.STICK));
                var before=stack.copy();var state=snapshot(h,be);
                player.gameMode.useItemOn(player,h.getLevel(),stack,InteractionHand.OFF_HAND,new BlockHitResult(Vec3.atCenterOf(f.center),Direction.UP,f.center,false));
                h.assertTrue(player.getOffhandItem().getItem()==before.getItem()&&player.getOffhandItem().getCount()==before.getCount()
                    &&player.getMainHandItem().is(net.minecraft.world.item.Items.STICK)&&snapshot(h,be).equals(state),"invalid water container leaves held stacks and engine unchanged");
            }
        }
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=200)
    public static void originalEngineFuelRowsProduceExactEnergyAndBufferedCarbonDioxide(GameTestHelper h) {
        // Independent source oracle, not a circular assertion against the production row list.
        Object[][] sourceRows={
            {"JetFuel",128L,12L},
            {"Kerosine",64L,7L},
            {"Petrol",64L,7L},
            {"Diesel",64L,7L},
            {"Fuel",64L,8L},
            {"NitroFuel",64L,12L},
            {"Rum_White",16L,9L},
            {"Rum_Dark",16L,9L},
            {"Pina_Colada",16L,9L},
            {"Whiskey",16L,9L},
            {"Whiskey_Rye",16L,9L},
            {"Whiskey_Corn",16L,9L},
            {"Whiskey_Scotch",16L,9L},
            {"Whiskey_GlenMcKenner",16L,9L},
            {"Vinegar_Grape",16L,9L},
            {"Vinegar_Apple",16L,9L},
            {"Vinegar_Cane",16L,9L},
            {"Vinegar_Rice",16L,9L},
            {"Vodka",16L,9L},
            {"Beer_Dragonblood",16L,18L},
            {"BioEthanol",16L,12L},
            {"Reikanol",16L,12L},
            {"BioDiesel",64L,8L},
            {"BioFuel",64L,12L},
            {"GenGas_Ethanol",16L,12L}
        };
        for(var row:sourceRows) {
            String key=(String)row[0];long rate=(Long)row[1],ticks=(Long)row[2];
            var recipe=FuelRecipeMaps.Engine.findRecipe(java.util.List.of(),java.util.List.of(fuel(h,key,1)),false,0,0);
            h.assertTrue(recipe!=null&&recipe.mEUt==-rate&&recipe.mDuration==ticks,"source exact per-mB EUt/duration "+key);
            h.assertTrue(recipe.mFluidInputs.length==1&&recipe.mFluidInputs[0].getAmount()==1
                &&recipe.mFluidOutputs.length==1&&recipe.mFluidOutputs[0].getAmount()==1
                &&recipe.mFluidOutputs[0].getFluid()==fuel(h,"CarbonDioxide",1).getFluid(),"source deterministic1mB fuel->1mBCO2 "+key);
            try(var f=new Fixture(h,112)) {
                var be=engine(f,engines().get(0),Direction.NORTH);
                f.set(f.center.south(),Blocks.STONE.defaultBlockState()); // prevent rear transfer/vent; inspect stored product
                h.assertTrue(fluids(h,f.center,Direction.UP).fill(fuel(h,key,1),FluidAction.EXECUTE)==1,"real one-charge admission");
                be.setStopped(false);
                KineticDieselEngineBlockEntity.serverTick(h.getLevel(),f.center,be.getBlockState(),be);
                h.assertTrue(be.fuelTank().isEmpty()&&be.getKuEnergy()==rate*ticks&&be.exhaustTank().getAmount()==1,
                    "actual registered engine converts one mB to exact cycle credit and retained CO2 "+key);
            }
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void exhaustVentsByGasPhaseAndEmptyCollisionNotBuoyancyOrAirOnly(GameTestHelper h) {
        try(var f=new Fixture(h,128)) {
            var be=engine(f,engines().get(0),Direction.NORTH);var rear=f.center.south();
            var co2=fuel(h,"CarbonDioxide",1);
            h.assertTrue(GTFluids.isGas(co2),"actual registered CO2 is gas by phase");
            com.mojang.logging.LogUtils.getLogger().info("[fuel-audit] CO2 native id={} gas={} density={} lighterThanAir={}",
                BuiltInRegistries.FLUID.getKey(co2.getFluid()),GTFluids.isGas(co2),co2.getFluid().getFluidType().getDensity(co2),co2.getFluid().getFluidType().isLighterThanAir());
            // Original WD.hasCollide: opaque OR any collision bounding box. A narrow
            // glass pane is still a barrier, not an excuse to vent through non-full blocks.
            f.set(rear,Blocks.GLASS_PANE.defaultBlockState());
            h.assertTrue(!h.getLevel().getBlockState(rear).getCollisionShape(h.getLevel(),rear).isEmpty(),"non-full-shape collision barrier fixture");
            be.exhaustTank().fill(co2,FluidAction.EXECUTE);
            KineticDieselEngineBlockEntity.serverTick(h.getLevel(),f.center,be.getBlockState(),be);
            h.assertTrue(be.exhaustTank().getAmount()==1,"any collision shape retains exhaust");
            f.set(rear,Blocks.AIR.defaultBlockState());
            KineticDieselEngineBlockEntity.serverTick(h.getLevel(),f.center,be.getBlockState(),be);
            h.assertTrue(be.exhaustTank().isEmpty(),"gas exhaust vents to open air regardless of buoyancy");
            f.set(rear.below(),Blocks.DIRT.defaultBlockState());f.set(rear,Blocks.DANDELION.defaultBlockState());
            h.assertTrue(!h.getLevel().getBlockState(rear).isAir()&&h.getLevel().getBlockState(rear).getCollisionShape(h.getLevel(),rear).isEmpty(),"non-air empty-collision flower fixture");
            be.exhaustTank().fill(co2,FluidAction.EXECUTE);
            KineticDieselEngineBlockEntity.serverTick(h.getLevel(),f.center,be.getBlockState(),be);
            h.assertTrue(be.exhaustTank().isEmpty(),"source also vents past non-air empty-collision targets");
        }
        h.succeed();
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
            h=helper; center=new BlockPos(28700008+offset,200,28700008);
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
