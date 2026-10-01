package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.block.energy.*;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.content.cover.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class BatteryBoxPanelTests {
    private static EnergyNodeBlockEntity box(GameTestHelper h){
        var block=GTEnergyNodes.all().stream().map(net.minecraftforge.registries.RegistryObject::get)
                .filter(b->b.spec().id().equals("battery_box_lv")).findFirst().orElseThrow();
        h.setBlock(new BlockPos(2,2,2),block);return (EnergyNodeBlockEntity)h.getBlockEntity(new BlockPos(2,2,2));
    }
    private static ItemStack panel(PanelCover p){return new ItemStack(GTTechnological.get(p.id));}
    private static void buffer(EnergyNodeBlockEntity be,long energy){var tag=be.saveWithoutMetadata();tag.putLong("gt.buffer",energy);be.load(tag);}
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void threeBufferStatesReachWorldBlockState(GameTestHelper h){
        var be=box(h);var prop=BatteryBoxBlock.CHARGE_STATE;
        h.assertTrue(be.getBlockState().getBlock().getStateDefinition().getPossibleStates().size()==36,"six directions x three charge states x waterlogging");
        h.assertTrue(be.getBlockState().getValue(prop)==0,"initial empty visual");
        buffer(be,32*300*4);
        h.startSequence().thenExecuteAfter(21,()->{
            h.assertTrue(be.getBlockState().getValue(prop)==1,"full working buffer uses active overlay");buffer(be,32);
        }).thenExecuteAfter(21,()->{
            h.assertTrue(be.getBlockState().getValue(prop)==2,"intermediate buffer uses blinking overlay");buffer(be,31);
        }).thenExecuteAfter(21,()->h.assertTrue(be.getBlockState().getValue(prop)==0,"less than one output packet uses idle overlay")).thenSucceed();
    }
    @GameTest(template="test_empty")
    public static void blockClicksControlModesAndStatusAndPersistPanels(GameTestHelper h){
        var be=box(h);var player=h.makeMockPlayer();var pos=be.getBlockPos();
        player.setItemInHand(InteractionHand.MAIN_HAND,panel(PanelCover.BUTTONS));
        var local=CoverFaceCoordinates.to(Direction.NORTH,.3,.3,0);
        var hit=new BlockHitResult(local.add(pos.getX(),pos.getY(),pos.getZ()),Direction.NORTH,pos,false);
        h.assertTrue(be.getBlockState().use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit).consumesAction()&&!be.getCover(Direction.NORTH).isEmpty(),"real block interaction attaches selector");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        h.assertTrue(be.getBlockState().use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit).consumesAction()&&be.batteryEnergy().mode()==5,"render coordinates and real click choose mode five");
        h.assertTrue(be.attachCover(Direction.SOUTH,panel(PanelCover.STATUS)),"status display attaches");
        h.assertTrue(be.panels().click(Direction.SOUTH,.8,.9)&&!be.batteryEnergy().enabled(),"status button toggles machine output");
        var copy=new EnergyNodeBlockEntity(pos,be.getBlockState());copy.load(be.getUpdateTag());
        h.assertTrue(PanelCover.of(copy.getCover(Direction.NORTH))==PanelCover.BUTTONS&&PanelCoverRuntime.value(copy.getCover(Direction.NORTH))==5,"client/update NBT retains cover value");
        h.assertTrue(!copy.batteryEnergy().enabled()&&copy.batteryEnergy().mode()==5,"control state saved with covers");
        be.removeCover(Direction.NORTH);h.assertTrue(be.batteryEnergy().mode()==0,"removing selector resets mode");
        h.getLevel().destroyBlock(pos,true);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(.8));
        h.assertTrue(drops.stream().filter(e->PanelCover.of(e.getItem())==PanelCover.STATUS).count()==1,"remaining cover drops exactly once");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void emptyCapacitorAndEnergyPanelsExposePhysicalRedstone(GameTestHelper h){
        var be=box(h);
        h.assertTrue(be.attachCover(Direction.NORTH,panel(PanelCover.ENERGY)),"empty battery box still implements EU capacitor");
        be.panels().afterTick();h.assertTrue(be.panels().signal(Direction.NORTH)==0,"empty emits zero");
        var battery=GTChemicalBatteries.all().stream().map(net.minecraftforge.registries.RegistryObject::get)
                .filter(b->b.spec().id().equals("battery_lead_acid_lv")).findFirst().orElseThrow();
        var stack=new ItemStack(battery);((com.gregtech.gregtech.item.ChemicalBatteryItem)stack.getItem()).setCharge(stack,64000);
        be.installBattery(stack);be.panels().afterTick();
        var state=be.getBlockState();var pos=be.getBlockPos();
        h.assertTrue(state.getSignal(h.getLevel(),pos,Direction.SOUTH)==15,"full battery signal reaches block redstone query on physical north face");
        be.panels().configure(Direction.NORTH,true,false);
        h.assertTrue(state.getDirectSignal(h.getLevel(),pos,Direction.SOUTH)==15,"cutter selects strong power");
        h.assertTrue(be.attachCover(Direction.DOWN,panel(PanelCover.SHUTTER)),"inventory shutter attaches now that sided automation is gated");
        h.succeed();
    }
}
