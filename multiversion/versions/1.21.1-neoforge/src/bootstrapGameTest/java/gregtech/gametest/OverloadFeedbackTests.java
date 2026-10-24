package gregtech.gametest;

import gregtech.GregTechConfig;
import gregtech.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.blockentity.machine.BasicMachineBlockEntity;
import gregtech.blockentity.machine.BoilerTankBlockEntity;
import gregtech.data.GregTechTags;
import gregtech.registry.GTFluids;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.*;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

/** One synchronous fixture isolates config changes and captures every area explosion. */
@GameTestHolder("gt6ce_overload_feedback")
@PrefixGameTestTemplate(false)
public final class OverloadFeedbackTests {
    @GameTest(template="test_empty", timeoutTicks=40)
    public static void originalDefaultsSurviveOldConfigOversizedPacketsAndSteamBursts(GameTestHelper h) {
        h.assertTrue(!GregTechConfig.explodeByOverload() && !GregTechConfig.breakByOverload(),
                "original overload defaults stay disabled, including after legacy true config is loaded");
        h.assertTrue(!GregTechConfig.SPEC.getValues().contains("machineOvervoltageExplosions"),
                "the obsolete generated true setting is not an active config key");
        ModConfigSpec.BooleanValue explosions = GregTechConfig.SPEC.getValues().get("machines.explode_by_overload");
        ModConfigSpec.BooleanValue breaking = GregTechConfig.SPEC.getValues().get("machines.break_by_overload");
        var count = new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<ExplosionEvent.Start> listener = event -> { count.incrementAndGet(); event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            for (String id : new String[]{"sluice_bronze", "crusher_bronze"}) {
                var pos = h.absolutePos(new BlockPos(id.startsWith("sluice") ? 1 : 5, 2, 1));
                var machine = basic(h, pos, id);
                var type = id.startsWith("sluice") ? GregTechTags.Energy.RU : GregTechTags.Energy.KU;
                long normal = machine.getEnergySizeInputRecommended(type, null);
                h.assertTrue(machine.doInject(type, null, normal, 1, true) == 1, "normal packet accepted");
                long stored = machine.getEnergyStored(type, null);
                long tooLarge = machine.getEnergySizeInputMax(type, null) + 1;
                h.assertTrue(machine.doInject(type, null, tooLarge, 7, false) == 7
                        && machine.getEnergyStored(type, null) == stored, "simulation reports consumed packets without changing storage");
                h.assertTrue(machine.doInject(type, null, tooLarge, 7, true) == 7
                        && machine.getEnergyStored(type, null) == stored && h.getLevel().getBlockEntity(pos) == machine,
                        "oversized " + type + " is discarded without erasing valid stored energy or breaking the machine");
            }
            var pos = h.absolutePos(new BlockPos(1, 2, 5));
            var boilerBlock = block("strong_steam_boiler_bronze");
            var turbineBlock = block("steam_turbine_invar");
            h.getLevel().setBlockAndUpdate(pos, boilerBlock.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos.above(), turbineBlock.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP));
            var boiler = (BoilerTankBlockEntity)h.getLevel().getBlockEntity(pos);
            var turbine = (EnergyNodeBlockEntity)h.getLevel().getBlockEntity(pos.above());
            var washer = basic(h, pos.above(2), "sluice_bronze");
            for (int tick = 0; tick < 360; tick++) {
                boiler.steamTank().setFluid(new FluidStack(GTFluids.still("Steam").get(), 1), boiler.steamTank().capacity() * 3 / 4 + 19200);
                BoilerTankBlockEntity.serverTick(h.getLevel(), pos, boiler.getBlockState(), boiler);
                EnergyNodeBlockEntity.serverTick(h.getLevel(), pos.above(), turbine.getBlockState(), turbine);
                h.assertTrue(h.getLevel().getBlockEntity(pos) == boiler && h.getLevel().getBlockEntity(pos.above()) == turbine
                        && h.getLevel().getBlockEntity(pos.above(2)) == washer, "192 L/t high-pressure boiler chain remains intact at tick " + tick);
            }
            // Force actual turbine conversion overloads past the first 100-event protection window.
            var inlet = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos.above(), Direction.DOWN);
            for (int tick = 0; tick < 240; tick++) {
                inlet.fill(new FluidStack(GTFluids.still("Steam").get(), inlet.getTankCapacity(0)), FluidAction.EXECUTE);
                EnergyNodeBlockEntity.serverTick(h.getLevel(), pos.above(), turbine.getBlockState(), turbine);
                h.assertTrue(h.getLevel().getBlockEntity(pos.above()) == turbine, "repeated full-tank overload never destroys turbine by default");
            }
            h.assertTrue(count.get() == 0, "disabled overload policy never creates an area explosion");

            var hammerPos = h.absolutePos(new BlockPos(8, 2, 8));
            var hammerBlock = gregtech.registry.GTRelaysFilters.HAMMERS.get(0).get();
            h.getLevel().setBlockAndUpdate(hammerPos, hammerBlock.defaultBlockState());
            var hammer = (gregtech.block.misc.AutoHammerBlockEntity)h.getLevel().getBlockEntity(hammerPos);
            hammer.doInject(GregTechTags.Energy.KU, null, hammer.input() * 2 + 1, 1, true);
            h.assertTrue(count.get() == 1 && h.getLevel().getBlockEntity(hammerPos) == null,
                    "automatic hammer retains the original direct-explosion exception with overload config disabled");
            count.set(0);

            var optionPos = h.absolutePos(new BlockPos(8, 2, 5));
            h.getLevel().setBlockAndUpdate(optionPos.east(), Blocks.STONE.defaultBlockState());
            var target = basic(h, optionPos, "sluice_bronze");
            breaking.set(true);
            target.doInject(GregTechTags.Energy.RU, null, 128, 1, true);
            h.assertTrue(h.getLevel().getBlockEntity(optionPos) == null && h.getLevel().getBlockState(optionPos.east()).is(Blocks.STONE)
                    && count.get() == 0, "break-only option removes the target without an area explosion");
            breaking.set(false); explosions.set(true);
            h.assertTrue(GregTechConfig.breakByOverload(), "explosion option implies machine removal, as in GT6");
            target = basic(h, optionPos, "sluice_bronze");
            target.doInject(GregTechTags.Energy.RU, null, 128, 1, false);
            h.assertTrue(count.get() == 0 && h.getLevel().getBlockEntity(optionPos) == target, "enabled damage still has no simulation side effects");
            target.doInject(GregTechTags.Energy.RU, null, 128, 1, true);
            h.assertTrue(count.get() == 1 && h.getLevel().getBlockEntity(optionPos) == null,
                    "explicit explosion option alone produces the area explosion event");
        } finally {
            explosions.set(false); breaking.set(false);
            NeoForge.EVENT_BUS.unregister(listener);
        }
        h.succeed();
    }

    private static Block block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:" + id)); }
    private static BasicMachineBlockEntity basic(GameTestHelper h, BlockPos pos, String id) {
        h.getLevel().setBlockAndUpdate(pos, block(id).defaultBlockState());
        return (BasicMachineBlockEntity)h.getLevel().getBlockEntity(pos);
    }
}
