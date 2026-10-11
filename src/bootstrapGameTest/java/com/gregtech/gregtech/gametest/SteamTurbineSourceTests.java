package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.block.energy.*;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity;
import com.gregtech.gregtech.content.energy.OriginalSteamTurbines;
import com.gregtech.gregtech.content.recipe.*;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import static com.gregtech.gregtech.gametest.TransportMaterialPortTests.*;

/** Finite native fixtures for the next pooled acceptance; ordinary jars exclude these. */
@net.minecraftforge.gametest.GameTestHolder("gregtech_thermal_converters") @net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class SteamTurbineSourceTests {
    private static EnergyNodeBlockEntity place(GameTestHelper h, BlockPos pos, String id) {
        var block = (EnergyNodeBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:" + id));
        h.setBlock(pos,block.defaultBlockState().setValue(EnergyNodeBlock.FACING,Direction.EAST));
        return (EnergyNodeBlockEntity) h.getBlockEntity(pos);
    }
    private static void tick(GameTestHelper h, EnergyNodeBlockEntity tile) {
        EnergyNodeBlockEntity.serverTick(h.getLevel(),tile.getBlockPos(),tile.getBlockState(),tile);
    }
    @GameTest(template="test_empty", timeoutTicks=100)
    public static void nativeSteamBatchCondensateCachedFacingPendingReverseAndStoppedDynamo(GameTestHelper h) {
        var pos = new BlockPos(3,3,3);
        var turbine = place(h,pos,"steam_turbine_steel");
        var dynamo = place(h,pos.east(),"electric_dynamo_lv");
        var battery = place(h,pos.east(2),"battery_box_lv");
        h.assertTrue(battery.installBattery(item("gregtech:battery_lead_acid_lv")),"actual complete battery installed");
        battery.batteryEnergy().tick(2,null,null);
        h.setBlock(pos.south(),BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:drum_stainless_steel")).defaultBlockState());
        var drum = (com.gregtech.gregtech.blockentity.machine.TankBlockEntity) h.getBlockEntity(pos.south());
        var steam = com.gregtech.gregtech.registry.GTFluids.stack("Steam",384);
        var inlet = turbine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
        var side = turbine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
        h.assertTrue(inlet.getTankCapacity(0) == 1536 && inlet.fill(steam,FluidAction.SIMULATE) == 384
                && inlet.getFluidInTank(0).isEmpty(),"source1536L steam tank and pure insertion simulation");
        h.assertTrue(side.getTanks() == 1 && side.fill(steam,FluidAction.EXECUTE) == 0
                && inlet.fill(com.gregtech.gregtech.registry.GTFluids.stack("DistW",384),FluidAction.EXECUTE) == 0,"only steam at current back can fill visible tank");
        h.setBlock(pos,turbine.getBlockState().setValue(EnergyNodeBlock.FACING,Direction.SOUTH));
        h.assertTrue(inlet.fill(steam,FluidAction.EXECUTE) == 0,"cached former back capability cannot bypass rotation");
        h.setBlock(pos,turbine.getBlockState().setValue(EnergyNodeBlock.FACING,Direction.EAST));
        h.assertTrue(inlet.fill(steam,FluidAction.EXECUTE) == 384,"rotated back inlet resumes");
        turbine.machineControl(null).setEnabled(false);
        h.assertTrue(inlet.fill(steam,FluidAction.EXECUTE) == 0,"cached steam capability follows stopped control");
        tick(h,turbine);
        h.assertTrue(dynamo.stored() == 64 && turbine.stored() == 0 && inlet.getFluidInTank(0).isEmpty(),"stopped source converts full batch first half into64RU without runtime rotor");
        h.assertTrue(drum.getFluidInTank(0).getAmount() == 1 && drum.getFluidInTank(0).getFluid() == steamWater(),"384L consumes local200L condensate boundary once");
        var saved = turbine.saveWithoutMetadata();
        h.assertTrue(saved.getLong("gt.turbine_pending") == 192 && saved.getLong("gt.steam_remainder") == 184,"native source pending half and200L remainder saved");
        var restored = new EnergyNodeBlockEntity(pos,turbine.getBlockState()); restored.load(saved);
        h.assertTrue(!restored.machineControl(null).enabled() && restored.spec().id().equals("steam_turbine_steel"),"native source stopped state method roundtrip; no restart claim");
        tick(h,dynamo);
        h.assertTrue(battery.batteryEnergy().buffer() == 44,"actual RU generator converts to44EU in complete battery");
        turbine.reverseMotor(); tick(h,turbine); tick(h,dynamo);
        h.assertTrue(battery.batteryEnergy().buffer() == 88 && drum.getFluidInTank(0).getAmount() == 1,
                "reverse preserves pending steam half and signed RU/EU; no second condensation");
        turbine.machineControl(null).setEnabled(true);
        h.assertTrue(inlet.fill(com.gregtech.gregtech.registry.GTFluids.stack("Steam",192),FluidAction.EXECUTE) == 192 && turbine.purgeTurbineSteam() == 192
                && inlet.getFluidInTank(0).isEmpty(),"source plunger trashes whole steam tank");
        var afterPurge = turbine.saveWithoutMetadata();
        h.assertTrue(afterPurge.getLong("gt.steam_remainder") == 184 && afterPurge.getLong("gt.turbine_pending") == 0,
                "plunger retains condensate counter and independent energy state");
        h.succeed();
    }
    private static net.minecraft.world.level.material.Fluid steamWater() {
        return com.gregtech.gregtech.registry.GTFluids.still("DistW").get();
    }
    @GameTest(template="test_empty", timeoutTicks=100)
    public static void fifteenTurbineCraftingRowsToolsSynchronizationMaterialRecoveryAndStoredProtection(GameTestHelper h) {
        int count = 0;
        for (var description : OriginalSteamTurbines.rows()) {
            var recipe = row(h,description.path()); var stacks = inputs(description);
            h.assertTrue(recipe.matches(grid(stacks),h.getLevel()),"actual source turbine grid " + description.path());
            var result = recipe.assemble(grid(stacks),h.getLevel().registryAccess());
            h.assertTrue(result.is(item(description.output()).getItem()) && result.getCount() == 1,"actual source turbine result");
            var decoded = wire(h,recipe);
            h.assertTrue(decoded.matches(grid(stacks),h.getLevel()),"native recipe synchronization retains turbine inputs");
            var remains = recipe.getRemainingItems(grid(stacks));
            for (int i=0;i<stacks.size();i++) if (stacks.get(i).getItem() instanceof GTToolItem tool)
                h.assertTrue(remains.get(i).is(tool) && remains.get(i).getDamageValue() == tool.toolType().damagePerCraft(),"source turbine tools incur crafting wear");
            var recovery = VanillaRecoveryRecipes.recipes().stream().filter(r -> r.mInputs[0].is(result.getItem())).findFirst().orElseThrow();
            h.assertTrue(RecipeInputs.consume(recovery,List.of(result),List.of(),1) != null,"clean turbine shell enters actual recovery predicate");
            var available = new HashMap<GTMaterial,Long>();
            for (var component : ItemMaterialRegistry.get(result).orElseThrow().components())
                available.merge(component.material().getTargetPulverMaterial().resolve(),MaterialRecoveryRules.pulverizedAmount(component.material(),component.amount()),Math::addExact);
            for (var out : recovery.mOutputs) {
                var form = MaterialEquivalence.form(out); h.assertTrue(form != null,"native turbine dust output");
                available.merge(form.material(),-form.prefix().getMaterialWeight()*out.getCount(),Long::sum);
            }
            h.assertTrue(available.values().stream().allMatch(v -> v>=0 && v<GTValues.U/72),"turbine recovery conserves each registered material");
            checkCrucible(h,result);
            var unsafe = result.copy(); stored(unsafe);
            h.assertTrue(RecipeInputs.consume(recovery,List.of(unsafe),List.of(),1) == null && com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(unsafe).isEmpty(),"stored turbine machine survives recycling guard");
            count++;
        }
        h.assertTrue(count == 15,"all original turbine crafting registrations"); h.succeed();
    }
}
