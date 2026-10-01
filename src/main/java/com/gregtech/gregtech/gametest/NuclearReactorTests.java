package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.content.nuclear.*;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.blockentity.energy.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import java.util.*;
@GameTestHolder("gregtech")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class NuclearReactorTests {
    private static ReactorCoreBlockEntity core(GameTestHelper h,int x,boolean four){
        var pos=new BlockPos(x,2,1);h.setBlock(pos,four?GTEnergyNodes.REACTOR_CORE_2X2.get():GTEnergyNodes.REACTOR_CORE_BLOCK.get());
        var c=(ReactorCoreBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));return c;
    }
    @GameTest(template="test_empty") public static void nuclearCatalogAndCoolants(GameTestHelper h){
        h.assertTrue(ReactorRodCatalog.ALL.size()==46,"46 original rod registrations");
        for(var d:ReactorRodCatalog.ALL){h.assertTrue(!GTFuelRods.stack(d.originalId()).isEmpty(),"registered rod "+d.id());h.assertTrue(com.gregtech.gregtech.api.material.GTMaterialRegistry.get(d.material()).isValid(),"rod material "+d.material());if(d.product()>0)h.assertTrue(ReactorRodCatalog.byOriginal(d.product())!=null,"rod product exists");}
        for(var c:ReactorCoolants.values())h.assertTrue(c.inputFluid()!=null&&c.outputFluid()!=null,"coolant phases exist: "+c.name()+" "+c.input+" -> "+c.output);
        h.assertTrue(ReactorRodRecipes.count()>=40,"canning and recovery routes loaded");h.succeed();
    }
    @GameTest(template="test_empty") public static void nuclearExchangeAndOrder(GameTestHelper h){
        var a=core(h,1,false);var b=core(h,2,false);a.insertRod(GTFuelRods.stack(9221));b.insertRod(GTFuelRods.stack(9221));a.setStopped(false);b.setStopped(false);
        ReactorNetwork.exchange(List.of(a,b));h.assertTrue(a.neutrons[0]==64&&b.neutrons[0]==64,"two half-width contacts send 32 to each adjacent one-rod core");
        a.neutrons[0]=b.neutrons[0]=0;ReactorNetwork.exchange(List.of(b,a));h.assertTrue(a.neutrons[0]==64&&b.neutrons[0]==64,"exchange independent of tick order");
        b.setStopped(true);ReactorNetwork.exchange(List.of(a,b));h.assertTrue(a.neutrons[0]==32&&b.neutrons[0]==0,"stopped core no emission or reception");h.succeed();
    }
    @GameTest(template="test_empty") public static void nuclearInternalSlotsAndReflector(GameTestHelper h){
        var c=core(h,1,true);h.assertTrue(c.rods.length==4,"2x2 is four rods, no casing multiblock");
        for(int i=0;i<4;i++)c.insertRod(i,GTFuelRods.stack(9221));c.setStopped(false);
        ReactorNetwork.exchange(List.of(c));h.assertTrue(Arrays.stream(c.neutrons).allMatch(n->n==96),"four fuel rods have two internal neighbors each");
        ReactorNetwork.exchange(List.of(c));h.assertTrue(Arrays.stream(c.neutrons).allMatch(n->n==128),"feedback uses previous cycle counts");
        for(int i=0;i<4;i++)c.removeRod(i);c.insertRod(0,GTFuelRods.stack(9221));c.insertRod(1,GTFuelRods.stack(9203));ReactorNetwork.exchange(List.of(c));
        h.assertTrue(c.neutrons[0]==64&&c.neutrons[1]==0,"reflector returns neutrons without heating itself");h.succeed();
    }
    @GameTest(template="test_empty") public static void nuclearBreedingAndLongLife(GameTestHelper h){
        var thorium=GTFuelRods.stack(9210);h.assertTrue(RodPhysics.life(thorium)==12000000000L,"fuel life exceeds signed int");
        var breeder=GTFuelRods.stack(9410);
        h.assertTrue(RodPhysics.receive(breeder,2000,false).absorbed()==1000&&RodPhysics.receive(breeder,1000,false).absorbed()==0&&RodPhysics.receive(breeder,2000,true).absorbed()==0,"breeding loss applies per entry and moderated neutrons cannot breed");
        breeder.getOrCreateTag().putLong("gt.reactor_life",50);var result=RodPhysics.react(breeder,100,null);
        h.assertTrue(RodPhysics.definition(result.stack()).originalId()==9411&&result.heat()==50,"breeding produces enriched rod and half heat");
        var fuel=GTFuelRods.stack(9221);fuel.getOrCreateTag().putLong("gt.reactor_life",100);var spent=RodPhysics.react(fuel,32,null);h.assertTrue(RodPhysics.definition(spent.stack()).originalId()==9321,"depleted fuel becomes recoverable rod");
        var moderator=GTFuelRods.stack(9204);h.assertTrue(RodPhysics.receive(moderator,32,false).reflected()==0,"moderator uses preceding contact count");RodPhysics.finishCycle(moderator);h.assertTrue(RodPhysics.receive(moderator,32,false).reflected()==32,"moderator reflects per touching fuel rod");
        h.assertTrue(RodPhysics.react(GTFuelRods.stack(9202),100,null).heat()==200,"absorber doubles heat");h.succeed();
    }
    @GameTest(template="test_empty") public static void nuclearCoolantTransactions(GameTestHelper h){
        var c=core(h,1,false);
        h.assertTrue(c.fluids.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,100),FluidAction.EXECUTE)==0,"plain water is not GT6 distilled coolant");
        for(var coolant:ReactorCoolants.values()){
            c.input.setEmpty();c.output.setEmpty();c.storedHeat=coolant.heat*3L+1;
            h.assertTrue(c.fluids.fill(new FluidStack(coolant.inputFluid(),10),FluidAction.SIMULATE)==10&&c.input.isEmpty(),"fill simulation does not mutate");
            c.fluids.fill(new FluidStack(coolant.inputFluid(),10),FluidAction.EXECUTE);h.assertTrue(c.coolHeat(),"coolant reacts: "+coolant);
            h.assertTrue(c.input.getAmount()==7&&c.output.getAmount()==3L*coolant.expansion&&c.storedHeat==1,"heat/volume conservation: "+coolant);
            c.output.setFluid(new FluidStack(coolant.outputFluid(),1),c.output.capacity(new FluidStack(coolant.outputFluid(),1)));c.storedHeat=coolant.heat;
            h.assertTrue(!c.coolHeat()&&c.input.getAmount()==7&&c.storedHeat==coolant.heat,"blocked hot output preserves cold coolant");
        }
        h.assertTrue(c.fluids.drain(1,FluidAction.SIMULATE).getAmount()==1,"automation reads output");h.succeed();
    }
    @GameTest(template="test_empty") public static void nuclearLegacyStorageAndSave(GameTestHelper h){
        var c=core(h,1,true);var old=new CompoundTag();var list=new ListTag();for(int i=0;i<8;i++)list.add(GTFuelRods.stack(9221).save(new CompoundTag()));old.put("gt.rods",list);c.load(old);
        h.assertTrue(c.rodCount()==4&&c.overflowCount()==4&&c.stopped,"legacy eight-slot save preserves excess rods and loads stopped");
        var saved=c.saveWithoutMetadata();c.load(saved);for(int i=0;i<8;i++)h.assertTrue(!c.removeRod().isEmpty(),"each saved rod can be recovered once");h.assertTrue(c.removeRod().isEmpty(),"no duplicated rods");
        c.input.setFluid(new FluidStack(ReactorCoolants.INDUSTRIAL.inputFluid(),1234));c.storedHeat=17;c.load(c.saveWithoutMetadata());h.assertTrue(c.input.getAmount()==1234&&c.storedHeat==17,"coolant and fractional heat persist");h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=60) public static void nuclearActualWorldTick(GameTestHelper h){
        var c=core(h,1,false);c.insertRod(GTFuelRods.stack(9221));c.fluids.fill(new FluidStack(ReactorCoolants.DISTILLED.inputFluid(),64000),FluidAction.EXECUTE);c.setStopped(false);
        h.runAfterDelay(25,()->{h.assertTrue(c.neutronTotal()==32&&c.output.getAmount()>0&&!c.failed,"world-end scheduler heats coolant");h.assertTrue(RodPhysics.life(c.rods[0])<1200000000L,"fuel wears every tick");c.setStopped(true);h.succeed();});
    }
    @GameTest(template="test_empty") public static void nuclearRadiationFallback(GameTestHelper h){
        var pig=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(1,2,1));
        h.assertTrue(ReactorRadiation.apply(pig,1,2),"living unprotected entity receives radiation fallback");
        h.assertTrue(pig.getEffect(net.minecraft.world.effect.MobEffects.WITHER).getDuration()==260,"original wither fallback duration");
        ReactorRadiation.apply(pig,1,2);h.assertTrue(pig.getEffect(net.minecraft.world.effect.MobEffects.WITHER).getDuration()==520,"repeated exposure accumulates duration");
        var zombie=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new BlockPos(2,2,1));h.assertTrue(!ReactorRadiation.apply(zombie,1,2),"undead exempt as in GT6");h.succeed();
    }
    @GameTest(template="test_empty") public static void nuclearDryFailureAndCoolingModifiers(GameTestHelper h){
        var c=core(h,1,false);c.insertRod(GTFuelRods.stack(9221));c.setStopped(false);ReactorNetwork.exchange(List.of(c));c.react();
        h.assertTrue(c.failed&&c.stopped&&c.rodCount()==0,"dry active reactor destroys installed rods and stops without destroying core block");
        h.assertTrue(h.getLevel().getBlockEntity(c.getBlockPos())==c,"GT6 terrain explosion remains disabled");
        var fuel=GTFuelRods.stack(9221);long life=RodPhysics.life(fuel);RodPhysics.react(fuel,32,ReactorCoolants.DISTILLED);
        h.assertTrue(RodPhysics.life(fuel)==life-400,"water moderation quadruples ordinary fuel wear");
        var d=RodPhysics.definition(fuel);
        h.assertTrue(ReactorCoolants.HEAVY.maximum(d)==256&&ReactorCoolants.TRITIATED.maximum(d)==128,"heavy water neutron limits");
        h.assertTrue(ReactorCoolants.INDUSTRIAL.self(d)==128&&ReactorCoolants.SODIUM.divisor(d)==3&&ReactorCoolants.LITHIUM_CHLORIDE.self(d)==160,"original coolant neutron modifiers");h.succeed();
    }

    @GameTest(template="test_empty") public static void nuclearAutomationAndSlotControl(GameTestHelper h){
        var c=core(h,1,true);var stack=GTFuelRods.stack(9221).copyWithCount(2);
        var handler=c.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
        h.assertTrue(handler.insertItem(0,stack,true).getCount()==1&&c.rodCount()==0,"simulation preserves reactor and caller stack");
        h.assertTrue(handler.insertItem(0,stack,false).getCount()==1&&stack.getCount()==2,"automation inserts exactly one rod");
        c.setStopped(false);
        h.assertTrue(handler.extractItem(0,1,false).isEmpty()&&handler.insertItem(1,stack,false).getCount()==2,"running slots reject automation");
        c.setDisabledSlots(2);
        h.assertTrue(handler.insertItem(1,stack,false).getCount()==1,"disabled slot accepts automation while other slots run");
        ReactorNetwork.exchange(List.of(c));h.assertTrue(c.neutrons[0]==32&&c.neutrons[1]==0,"disabled rod neither emits nor receives");
        c.load(c.saveWithoutMetadata());h.assertTrue(c.disabledSlots()==2&&!c.slotActive(1),"slot control persists");
        var copy=handler.getStackInSlot(0);copy.setCount(0);h.assertTrue(c.rodCount()==2,"inventory inspection cannot mutate rods");
        h.assertTrue(c.insertHandRod(2,stack)&&c.stopped,"hand loading stops whole reactor as in GT6");
        h.assertTrue(!handler.extractItem(0,1,true).isEmpty()&&c.rodCount()==3,"extraction simulation preserves rod");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void nuclearIndependentFluidOutlets(GameTestHelper h){
        var a=core(h,1,false);var b=core(h,2,false);
        var level=h.getLevel();
        level.setBlockAndUpdate(a.getBlockPos(),a.getBlockState().setValue(ReactorPorts.COLD,net.minecraft.core.Direction.EAST).setValue(ReactorPorts.HOT,net.minecraft.core.Direction.DOWN));
        a.input.setFluid(new FluidStack(ReactorCoolants.DISTILLED.inputFluid(),50000));
        a.pushFluids();h.assertTrue(a.input.getAmount()==32000&&b.input.getAmount()==18000,"excess cold coolant moves to independent secondary side, preserving half tank");
        a.input.setFluid(new FluidStack(ReactorCoolants.DISTILLED.inputFluid(),50000));
        b.input.setFluid(new FluidStack(ReactorCoolants.DISTILLED.inputFluid(),63000));
        a.pushFluids();h.assertTrue(a.input.getAmount()==49000&&b.input.getAmount()==64000,"partial destination capacity conserves fluid");
        a.pushFluids();h.assertTrue(a.input.getAmount()==49000,"full destination does not delete fluid");
        b.input.setEmpty();level.setBlockAndUpdate(a.getBlockPos(),a.getBlockState().setValue(ReactorPorts.HOT,net.minecraft.core.Direction.EAST));
        a.pushFluids();h.assertTrue(b.input.isEmpty(),"identical primary and secondary faces disable cold transfer");
        a.output.setFluid(new FluidStack(ReactorCoolants.DISTILLED.inputFluid(),200));a.pushFluids();
        h.assertTrue(a.output.isEmpty()&&b.input.getAmount()==200,"primary side sends output independently of half-tank rule");
        var item=new ItemStack(a.getBlockState().getBlock());ReactorPorts.saveItemState(item,a.getBlockState());
        h.assertTrue(item.getTagElement("BlockStateTag").getString("cold_outlet").equals("east"),"portable block state preserves configured ports");
        h.succeed();
    }
}
