package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTechConfig;
import com.gregtech.gregtech.api.energy.MachineOverload;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.block.energy.*;
import com.gregtech.gregtech.block.misc.*;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.content.energy.BoilerHazards;
import com.gregtech.gregtech.content.logistics.LogisticsCoreStructure;
import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.*;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

/** Isolated native boundaries; blast starts are cancelled to keep every fixture self-contained. */
@GameTestHolder("gt6ce_overload_feedback") @PrefixGameTestTemplate(false)
public final class MachineExplosionReviewTests {
    private static Block block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:" + id)); }
    private static final class Blasts implements AutoCloseable {
        int count;
        final java.util.function.Consumer<ExplosionEvent.Start> listener = event -> { count++; event.setCanceled(true); };
        Blasts() { NeoForge.EVENT_BUS.addListener(listener); }
        public void close() { NeoForge.EVENT_BUS.unregister(listener); }
    }

    @GameTest(template="test_empty", timeoutTicks=40)
    public static void bronzeTurbineWikiRatesCondensateAndWalkRotation(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 2, 3));
        var world = h.getLevel();
        world.setBlockAndUpdate(p, block("steam_turbine_bronze").defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP));
        world.setBlockAndUpdate(p.above(), block("electric_dynamo_lv").defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP));
        world.setBlockAndUpdate(p.above(2), block("battery_box_lv").defaultBlockState());
        world.setBlockAndUpdate(p.east(), block("drum_stainless_steel").defaultBlockState());
        var turbine = (EnergyNodeBlockEntity)world.getBlockEntity(p);
        var dynamo = (EnergyNodeBlockEntity)world.getBlockEntity(p.above());
        var battery = (EnergyNodeBlockEntity)world.getBlockEntity(p.above(2));
        battery.installBattery(new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gt6ce:battery_lead_acid_lv"))));
        var inlet = world.getCapability(Capabilities.FluidHandler.BLOCK, p, Direction.DOWN);
        try (var blasts = new Blasts()) {
            for (int i = 0; i < 40; i++) {
                inlet.fill(new FluidStack(GTFluids.still("Steam").get(), 48), FluidAction.EXECUTE);
                EnergyNodeBlockEntity.serverTick(world, p, turbine.getBlockState(), turbine);
                if (i >= 2) h.assertTrue(dynamo.stored() == 16, "48 SU/t produces 16 RU/t after the original two-tick startup");
                battery.batteryEnergy().tick(i + 10, null, null);
                EnergyNodeBlockEntity.serverTick(world, p.above(), dynamo.getBlockState(), dynamo);
            }
            var drum = (TankBlockEntity)world.getBlockEntity(p.east());
            long consumed = 40 * 48 - turbine.turbineSteamAmount();
            h.assertTrue(drum.getFluidInTank(0).getAmount() == consumed / 200, "every 200 consumed SU yields one distilled water");
            var pig = EntityType.PIG.create(world);
            float initialYaw = pig.getYRot(), initialHead = pig.yHeadRot;
            turbine.getBlockState().getBlock().stepOn(world, p, turbine.getBlockState(), pig);
            h.assertTrue(pig.getYRot() == initialYaw + 5 && pig.yHeadRot == initialHead + 5, "normal upward turbine rotates living entities by five degrees");
            for (int i = 0; i < 8; i++) {
                inlet.fill(new FluidStack(GTFluids.still("Steam").get(), 96), FluidAction.EXECUTE);
                EnergyNodeBlockEntity.serverTick(world, p, turbine.getBlockState(), turbine);
                battery.batteryEnergy().tick(i + 100, null, null);
                EnergyNodeBlockEntity.serverTick(world, p.above(), dynamo.getBlockState(), dynamo);
            }
            h.assertTrue(turbine.getBlockState().getValue(OriginalMotorBlock.FAST), "above-nominal output selects fast rotation");
            float normalYaw = pig.getYRot(), normalHead = pig.yHeadRot;
            turbine.getBlockState().getBlock().stepOn(world, p, turbine.getBlockState(), pig);
            h.assertTrue(pig.getYRot() == normalYaw + 10 && pig.yHeadRot == normalHead + 10 && blasts.count == 0, "fast turbine adds ten degrees without requesting any blast");
        }
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=40)
    public static void boilerChecksCurrentPressureAndRetainsTrueHazards(GameTestHelper h) {
        var world = h.getLevel();
        var p = h.absolutePos(new BlockPos(3, 2, 3));
        var boilerBlock = block("stainless_steel_boiler_main_barometer");
        world.setBlockAndUpdate(p, boilerBlock.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        var boiler = (OriginalLargeBoilerControllerBlockEntity)world.getBlockEntity(p);
        for (var cell : OriginalLargeBoilerSpecs.LAYOUT.cells()) {
            var at = cell.at(p, Direction.NORTH);
            if (at.equals(p)) continue;
            var b = cell.role() == com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.AIR ? Blocks.AIR
                    : cell.role() == com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.HEAT_INPUT ? GTMultiblocks.HEAT_TRANSMITTER.get() : boiler.variant().wall();
            world.setBlockAndUpdate(at, b.defaultBlockState());
        }
        h.assertTrue(StructureTestSupport.ok(boiler), "actual large boiler shell forms");
        var saved = boiler.saveWithoutMetadata(world.registryAccess());
        var steam = new FluidTankGT(boiler.variant().steamCapacity());
        steam.setFluid(new FluidStack(GTFluids.still("Steam").get(), 1), boiler.variant().steamCapacity() * 4 / 30 + 1);
        var tank = new net.minecraft.nbt.CompoundTag(); steam.writeToNBT(tank, world.registryAccess()); saved.put("gt.steam", tank); saved.putInt("gt.cooldown", 0);
        boiler.loadWithComponents(saved, world.registryAccess());
        h.assertTrue(boiler.barometerValue() == 5 && BoilerHazards.pressure(boiler.variant().steamCapacity() / 2, boiler.variant().steamCapacity()) == 16,
                "barometer uses original positive-first-step scale");
        world.setBlockAndUpdate(p.east(), Blocks.AIR.defaultBlockState());
        try (var blasts = new Blasts()) {
            OriginalLargeBoilerControllerBlockEntity.serverTick(world, p, boiler.getBlockState(), boiler);
            h.assertTrue(blasts.count == 0 && world.getBlockEntity(p) == boiler && boiler.barometerValue() == 4,
                    "cooling pressure from five to four prevents a stale-pressure structural blast");
            saved = boiler.saveWithoutMetadata(world.registryAccess());
            steam.setFluid(new FluidStack(GTFluids.still("Steam").get(), 1), boiler.variant().steamCapacity() / 4);
            steam.writeToNBT(tank, world.registryAccess()); saved.put("gt.steam", tank); saved.putInt("gt.cooldown", 128);
            boiler.loadWithComponents(saved, world.registryAccess());
            OriginalLargeBoilerControllerBlockEntity.serverTick(world, p, boiler.getBlockState(), boiler);
            h.assertTrue(blasts.count == 1 && world.getBlockEntity(p) == null, "genuinely pressurised damaged boiler still explodes");
            boiler.explode();
            h.assertTrue(blasts.count == 1, "removed large boiler cannot explode again");

            var singlePos = h.absolutePos(new BlockPos(10, 2, 3));
            world.setBlockAndUpdate(singlePos, block("steam_boiler_bronze").defaultBlockState());
            var single = (BoilerTankBlockEntity)world.getBlockEntity(singlePos);
            saved = single.saveWithoutMetadata(world.registryAccess()); saved.putLong("gt.heat", single.spec().heatCapacity());
            single.loadWithComponents(saved, world.registryAccess());
            BoilerTankBlockEntity.serverTick(world, singlePos, single.getBlockState(), single);
            h.assertTrue(world.getBlockEntity(singlePos) == single && blasts.count == 1, "heat at the original limit is safe");
            single.doInject(GregTechTags.Energy.HU, Direction.DOWN, 1, 1, true);
            BoilerTankBlockEntity.serverTick(world, singlePos, single.getBlockState(), single);
            h.assertTrue(world.getBlockEntity(singlePos) == null && blasts.count == 2, "heat above the original limit still explodes");
            single.explode();
            h.assertTrue(blasts.count == 2, "removed single boiler cannot explode again");
        }
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=40)
    public static void distantTransformersHonorOriginalDefaultAndBreakOption(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 3, 2));
        var world = h.getLevel();
        var breaking = (ModConfigSpec.BooleanValue)GregTechConfig.SPEC.getValues().get("machines.break_by_overload");
        try (var blasts = new Blasts()) {
            var sender = route(h, p, "long_dist_transformer_ulv");
            var target = world.getBlockEntity(p.south(4));
            h.assertTrue(sender.doEnergyInjection(GregTechTags.Energy.EU, Direction.NORTH, 4097, 2, false) == 2, "oversized route simulation reports the packet count");
            h.assertTrue(sender.doEnergyInjection(GregTechTags.Energy.EU, Direction.NORTH, 4097, 2, true) == 2
                    && world.getBlockEntity(p) == sender && world.getBlockEntity(p.south(4)) == target, "sender overcharge does not destroy the default-config route");
            sender = route(h, p, "long_dist_transformer_lv"); target = world.getBlockEntity(p.south(4));
            h.assertTrue(sender.doEnergyInjection(GregTechTags.Energy.EU, Direction.NORTH, 8192, 2, true) == 2
                    && world.getBlockEntity(p.south(4)) == target && blasts.count == 0, "receiver overcharge also honors original disabled defaults");
            breaking.set(true);
            sender.doEnergyInjection(GregTechTags.Energy.EU, Direction.NORTH, 8192, 2, true);
            h.assertTrue(world.getBlockEntity(p) == sender && world.getBlockEntity(p.south(4)) == null && blasts.count == 0,
                    "break-only option removes the overloaded receiver without a range blast");
        } finally { breaking.set(false); }
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=40)
    public static void directControllersKeepBufferGuardAndExplodeOnlyOnce(GameTestHelper h) {
        var world = h.getLevel();
        var p = h.absolutePos(new BlockPos(3, 6, 3));
        var drillBlock = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof com.gregtech.gregtech.block.machine.OriginalBedrockDrillControllerBlock).findFirst().orElseThrow();
        world.setBlockAndUpdate(p, drillBlock.defaultBlockState());
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
            world.setBlockAndUpdate(p.offset(x,-5,z), Blocks.BEDROCK.defaultBlockState());
            for (int y=-4;y<=0;y++) if (x!=0 || y!=0 || z!=0)
                world.setBlockAndUpdate(p.offset(x,y,z), LargeMachineParts.block(y==-4 ? 18103 : 18026).defaultBlockState());
        }
        var drill = (BedrockDrillControllerBlockEntity)world.getBlockEntity(p);
        h.assertTrue(StructureTestSupport.ok(drill), "actual bedrock drill shell forms");
        try (var blasts = new Blasts()) {
            h.assertTrue(drill.doInject(GregTechTags.Energy.RU, null, 4096, 10, true) == 10
                    && drill.getEnergyStored(GregTechTags.Energy.RU,null) == 40960, "original last-batch buffer overshoot is retained");
            var saved = drill.saveWithoutMetadata(world.registryAccess()); drill.loadWithComponents(saved, world.registryAccess());
            h.assertTrue(drill.getEnergyStored(GregTechTags.Energy.RU,null) == 40960
                    && drill.doInject(GregTechTags.Energy.RU,null,8192,1,true) == 0 && blasts.count == 0,
                    "loaded overfull source buffer rejects a new oversized packet before the explosion test");
            saved.putLong("gt.drill_energy",0); drill.loadWithComponents(saved, world.registryAccess());
            h.assertTrue(drill.doInject(GregTechTags.Energy.RU,null,4097,1,false) == 1 && blasts.count == 0, "drill query is inert");
            drill.doInject(GregTechTags.Energy.RU,null,4097,1,true);
            h.assertTrue(blasts.count == 1 && world.getBlockEntity(p) == null, "real drill overspeed keeps source strength-six explosion and removes main");
            MachineOverload.explode(drill,6);
            h.assertTrue(blasts.count == 1, "stale drill callbacks cannot repeat the explosion");

            var corePos = h.absolutePos(new BlockPos(10,3,8));
            world.setBlockAndUpdate(corePos, block("logistics_core").defaultBlockState().setValue(com.gregtech.gregtech.block.machine.LogisticsCoreControllerBlock.FACING, Direction.NORTH));
            for (var cell : LogisticsCoreStructure.cells())
                world.setBlockAndUpdate(cell.at(corePos,Direction.NORTH), LogisticsCoreStructure.block(cell.kind()).defaultBlockState());
            var core = (LogisticsCoreControllerBlockEntity)world.getBlockEntity(corePos);
            h.assertTrue(StructureTestSupport.ok(core), "actual logistics core with processors forms");
            core.doInject(GregTechTags.Energy.EU,null,1025,1,false);
            h.assertTrue(blasts.count == 1, "logistics overvoltage simulation is inert");
            core.doInject(GregTechTags.Energy.EU,null,1025,1,true);
            h.assertTrue(blasts.count == 2 && world.getBlockEntity(corePos) == null, "logistics real source overvoltage removes main before blast");
            MachineOverload.explode(core,6);
            h.assertTrue(blasts.count == 2, "stale logistics callback cannot explode again");
        }
        h.succeed();
    }

    private static LongDistanceTransformerBlockEntity route(GameTestHelper h, BlockPos p, String sender) {
        var world = h.getLevel();
        world.removeBlock(p,false); world.removeBlock(p.south(4),false);
        world.setBlockAndUpdate(p, block(sender).defaultBlockState().setValue(LongDistanceTransformerBlock.FACING,Direction.NORTH));
        world.setBlockAndUpdate(p.south(4), block("long_dist_transformer_ulv").defaultBlockState().setValue(LongDistanceTransformerBlock.FACING,Direction.NORTH));
        var wire = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof LongDistPipeBlock line && line.isWire() && line.maximumVoltage() >= 16384).findFirst().orElseThrow();
        for (int i=1;i<4;i++) world.setBlockAndUpdate(p.south(i),wire.defaultBlockState());
        return (LongDistanceTransformerBlockEntity)world.getBlockEntity(p);
    }
}
