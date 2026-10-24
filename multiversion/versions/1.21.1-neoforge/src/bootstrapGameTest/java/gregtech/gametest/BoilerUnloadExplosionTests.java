package gregtech.gametest;

import gregtech.internal.fluid.FluidTankGT;
import gregtech.content.blockentity.machine.OriginalLargeBoilerControllerBlockEntity;
import gregtech.content.multiblock.OriginalLargeBoilerSpecs;
import gregtech.content.registry.GTFluids;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;

/** A remote chunk boundary in this disposable world, never a user save or the forced test area. */
@GameTestHolder("gt6ce_boiler_unload_feedback") @PrefixGameTestTemplate(false)
public final class BoilerUnloadExplosionTests {
    @GameTest(template="test_empty", timeoutTicks=100)
    public static void unloadedShellPreservesCachedStructureUntilRealDamageCanBeChecked(GameTestHelper h) {
        var world=h.getLevel();
        var p=new BlockPos(1_048_575,h.absolutePos(new BlockPos(0,3,0)).getY(),1_048_575);
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:stainless_steel_boiler_main_barometer"));
        world.setBlock(p,block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,Direction.NORTH),
                net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE | net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        var boiler=(OriginalLargeBoilerControllerBlockEntity)world.getBlockEntity(p);
        var blasts=new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<ExplosionEvent.Start> listener=event->{blasts.incrementAndGet();event.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            h.assertTrue(OriginalLargeBoilerSpecs.LAYOUT.cells().stream().anyMatch(c->!world.hasChunkAt(c.at(p,Direction.NORTH))),
                    "actual adjacent shell chunks are not loaded");
            var saved=boiler.saveWithoutMetadata(world.registryAccess());
            saved.putBoolean("gt.state.str",true);
            var steam=new FluidTankGT(boiler.variant().steamCapacity());
            steam.setFluid(new FluidStack(GTFluids.still("Steam").get(),1),boiler.variant().steamCapacity()/4);
            var tank=new net.minecraft.nbt.CompoundTag();steam.writeToNBT(tank,world.registryAccess());saved.put("gt.steam",tank);
            boiler.loadWithComponents(saved,world.registryAccess());
            h.assertFalse(StructureTestSupport.ok(boiler),"tracked query is unformed while a shell chunk is unavailable");
            OriginalLargeBoilerControllerBlockEntity.serverTick(world,p,boiler.getBlockState(),boiler);
            h.assertTrue(world.getBlockEntity(p)==boiler&&blasts.get()==0,"partial loading alone never counts as pressure-vessel damage");
            for(var cell:OriginalLargeBoilerSpecs.LAYOUT.cells())world.getChunkAt(cell.at(p,Direction.NORTH));
            h.assertTrue(!StructureTestSupport.ok(boiler),"after real chunks load, a genuinely missing shell invalidates the cached state");
            OriginalLargeBoilerControllerBlockEntity.serverTick(world,p,boiler.getBlockState(),boiler);
            h.assertTrue(blasts.get()==1&&world.getBlockEntity(p)==null,"confirmed missing pressurised shell still retains original explosion");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            world.removeBlock(p,false);
        }
        h.succeed();
    }
}
