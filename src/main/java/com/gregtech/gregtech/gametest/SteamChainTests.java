package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class SteamChainTests {
    private static KineticSteamEngineBlockEntity engine(GameTestHelper h){
        var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech","engine_steam_bronze"));
        var pos=new BlockPos(1,2,1);h.setBlock(pos,block);
        return (KineticSteamEngineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    @GameTest(template="test_empty") public static void steamEngineRejectsNonSteam(GameTestHelper h){
        var e=engine(h);var inlet=e.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
        h.assertTrue(inlet.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),FluidAction.EXECUTE)==0,"water must not power steam engine");
        h.assertTrue(inlet.fill(new FluidStack(GTFluids.still("Steam").get(),200),FluidAction.EXECUTE)==200,"actual steam accepted");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void steamEngineOriginalCondensationRatio(GameTestHelper h){
        var e=engine(h);e.steamTank().setFluid(new FluidStack(GTFluids.still("Steam").get(),199));
        KineticSteamEngineBlockEntity.serverTick(h.getLevel(),e.getBlockPos(),e.getBlockState(),e);
        h.assertTrue(e.steamTank().getAmount()==199&&e.getKuEnergy()==0,"GT6 requires 200 L steam per conversion, not boiler expansion of 160");
        e.steamTank().setFluid(new FluidStack(GTFluids.still("Steam").get(),200));
        KineticSteamEngineBlockEntity.serverTick(h.getLevel(),e.getBlockPos(),e.getBlockState(),e);
        h.assertTrue(e.steamTank().isEmpty()&&e.getKuEnergy()==e.spec().efficiency()/100,"200 L steam yields 100 KU times efficiency");
        e.setEnabled(false);var n=e.saveWithoutMetadata();n.putLong("kuEnergy",1000);e.load(n);
        KineticSteamEngineBlockEntity.serverTick(h.getLevel(),e.getBlockPos(),e.getBlockState(),e);
        h.assertTrue(e.getKuEnergy()<1000,"soft-hammer shutdown releases stored heat");h.succeed();
    }
    @GameTest(template="test_empty") public static void boilerOriginalBuffersAndHeatAcceptance(GameTestHelper h){
        var block=GTBoilers.all().get(0).get();var pos=new BlockPos(1,2,1);h.setBlock(pos,block);
        var b=(BoilerTankBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        h.assertTrue(b.waterTank().getCapacity()==4000,"original boiler water capacity is 4000 L");
        h.assertTrue(b.spec().heatCapacity()==b.spec().steamOutput()*10000&&b.spec().steamCapacity()==b.spec().steamOutput()*10000,"source default heat and steam capacities");
        long packets=b.doInject(GregTechTags.Energy.HU,Direction.DOWN,80,b.spec().heatCapacity()/80+1,true);
        h.assertTrue(packets>0&&b.storedHeat()>b.spec().heatCapacity(),"heat is accepted past capacity so dry-overheat protection is reachable");
        h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);h.succeed();
    }

    @GameTest(template="test_empty") public static void boilerConversionAndCooldown(GameTestHelper h){
        var pos=new BlockPos(1,2,1);h.setBlock(pos,GTBoilers.all().get(0).get());
        var b=(BoilerTankBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        var water=new FluidStack(GTFluids.still("DistW").get(),100);
        var side=b.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
        var top=b.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
        h.assertTrue(top.fill(water,FluidAction.EXECUTE)==0&&side.fill(water,FluidAction.EXECUTE)==100,"water enters sides/bottom, not steam outlet");
        b.doInject(GregTechTags.Energy.HU,Direction.DOWN,80,1,true);
        BoilerTankBlockEntity.serverTick(h.getLevel(),b.getBlockPos(),b.getBlockState(),b);
        h.assertTrue(b.waterTank().getAmount()==99&&b.steamTank().getAmount()==160&&b.storedHeat()==0,"one water and 80 HU produce exactly 160 steam, without immediate one-L decay");
        h.assertTrue(side.drain(100,FluidAction.EXECUTE).isEmpty(),"automation cannot bypass boiler pressure threshold by pulling steam");
        var n=b.saveWithoutMetadata();n.putInt("gt.cooldown",0);b.load(n);
        BoilerTankBlockEntity.serverTick(h.getLevel(),b.getBlockPos(),b.getBlockState(),b);
        h.assertTrue(b.steamTank().isEmpty(),"cooldown vents at original output times 64 rate");h.succeed();
    }
    @GameTest(template="test_empty") public static void steamEngineRatedOutputAndBlockedStroke(GameTestHelper h){
        var e=engine(h);h.assertTrue(e.spec().outputRate()==12&&e.energyCapacity()==24000&&e.steamTank().getCapacity()==4800,"bronze source registers 24/2 KU and separate 24000 KU/4800 L buffers");
        for(var d:Direction.values())h.getLevel().setBlockAndUpdate(e.getBlockPos().relative(d),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var n=e.saveWithoutMetadata();n.putLong("kuEnergy",12000);e.load(n);
        KineticSteamEngineBlockEntity.serverTick(h.getLevel(),e.getBlockPos(),e.getBlockState(),e);
        h.assertTrue(e.getKuEnergy()<12000,"a blocked piston still spends its stroke, as in source");
        h.assertTrue(e.doExtract(GregTechTags.Energy.KU,null,1,9999,true)==0,"cannot pull a second output after the stroke");h.succeed();
    }
}
