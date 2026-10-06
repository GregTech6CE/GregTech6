package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.block.energy.*;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity;
import com.gregtech.gregtech.content.energy.OriginalThermalCrafting;
import com.gregtech.gregtech.content.recipe.*;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import static com.gregtech.gregtech.gametest.TransportMaterialPortTests.*;

/** Finite native fixtures for the next pooled acceptance; ordinary jars exclude these. */
@net.minecraftforge.gametest.GameTestHolder("gregtech_thermal_converters") @net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class ThermalConverterSourceTests {
    private static EnergyNodeBlockEntity place(GameTestHelper h, BlockPos pos, String id, Direction front) {
        var block = (EnergyNodeBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:" + id));
        h.setBlock(pos, block.defaultBlockState().setValue(EnergyNodeBlock.FACING, front));
        return (EnergyNodeBlockEntity) h.getBlockEntity(pos);
    }
    private static void tick(GameTestHelper h, EnergyNodeBlockEntity tile) {
        EnergyNodeBlockEntity.serverTick(h.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile);
    }
    @GameTest(template="test_empty", timeoutTicks=100)
    public static void thermalTwinOutputsNativeReceiversStoppedFeAndHeaterContact(GameTestHelper h) {
        var freezer = java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(), false)
                .filter(b -> b instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock)
                .map(b -> (com.gregtech.gregtech.block.machine.BasicMachineBlock) b)
                .filter(b -> b.basicSpec().machineName().equals("freezer") && b.basicSpec().tier() == 1).findFirst().orElseThrow();
        var boiler = com.gregtech.gregtech.registry.GTBoilers.all().get(0).get();
        int z = 2;
        for (String id : List.of("electric_cooler_lv", "flux_cooler_lv")) {
            var pos = new BlockPos(4,4,z);
            var tile = place(h, pos, id, Direction.EAST);
            h.setBlock(pos.east(), freezer.defaultBlockState().setValue(com.gregtech.gregtech.block.machine.BasicMachineBlock.FACING, Direction.EAST));
            h.setBlock(pos.west(), boiler.defaultBlockState());
            var cold = (BasicMachineBlockEntity) h.getBlockEntity(pos.east());
            var heat = (BoilerTankBlockEntity) h.getBlockEntity(pos.west());
            h.assertTrue(cold.getEnergyTick() == 0 && heat.storedHeat() == 0, "actual receivers start empty");
            if (id.startsWith("flux_")) {
                var fe = tile.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, Direction.UP).orElseThrow(IllegalStateException::new);
                h.assertTrue(fe.receiveEnergy(256, true) == 256 && tile.stored() == 0, "real FE capability simulation is pure");
                h.assertTrue(!tile.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, Direction.WEST).isPresent(), "cooler back is HU output, not RF input");
                h.assertTrue(fe.receiveEnergy(256, false) == 256, "source RF capacity and actual FE insertion");
            } else h.assertTrue(tile.doEnergyInjection(GregTechTags.Energy.EU, Direction.UP,64,1,true) == 1, "source EU max packet accepts");
            tick(h,tile);
            h.assertTrue(tile.stored() == 0 && cold.getEnergyTick() == 16 && heat.storedHeat() == 16, "source one waste tick sends16CU front and16HU back to actual machines");
            h.assertTrue(tile.machineControl(null).active() && tile.getBlockState().getValue(RotaryConverterBlock.ACTIVITY) == 2, "two output network updates actual activity state");
            boolean selectable = !id.startsWith("flux_");
            h.assertTrue(tile.machineControl(null).supportsMode() == selectable, "original RF cooler omits selector interface");
            tile.machineControl(null).setMode(1);
            tile.doEnergyInjection(tile.spec().inType(),Direction.UP,tile.spec().inputRate(),2,true);
            tick(h,tile);
            h.assertTrue(cold.getEnergyTick() == (selectable ? 31 : 32) && heat.storedHeat() == (selectable ? 31 : 32) && tile.stored() == (selectable ? tile.spec().inputRate()/8 : 0),
                    "selectable EU cooler mode emits15; RF cooler remains16; both waste input once");
            tile.machineControl(null).setEnabled(false);
            var saved = tile.saveWithoutMetadata();
            var restored = new EnergyNodeBlockEntity(tile.getBlockPos(), tile.getBlockState()); restored.load(saved);
            h.assertTrue(restored.stored() == (selectable ? tile.spec().inputRate()/8 : 0) && restored.machineControl(null).mode() == (selectable ? 1 : 0) && !restored.machineControl(null).enabled(), "native saved controls and buffer method roundtrip; not a restart");
            if (id.startsWith("flux_")) {
                var fe = tile.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, Direction.UP).orElseThrow(IllegalStateException::new);
                h.assertTrue(!fe.canReceive() && fe.receiveEnergy(256,false) == 0, "cached native FE view respects stopped control");
            }
            tick(h,tile);
            h.assertTrue(tile.stored() == 0 && tile.getBlockState().getValue(RotaryConverterBlock.ACTIVITY) == 0, "stopped source drains remainder and hides activity");
            z += 3;
        }
        var heater = place(h,new BlockPos(4,4,8),"electric_heater_lv",Direction.UP);
        heater.doEnergyInjection(GregTechTags.Energy.EU,Direction.DOWN,64,1,true); tick(h,heater);
        var block = (OriginalHeaterBlock) heater.getBlockState().getBlock();
        var cow = h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(4,5,8));
        float health = cow.getHealth();
        block.entityInside(heater.getBlockState(),h.getLevel(),heater.getBlockPos(),cow);
        h.assertTrue(Math.abs(cow.getHealth()-(health-1.6F)) < .01F, "native source LV contact damage1.6 uses registered heat damage");
        var protectedCow = h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(5,5,8));
        protectedCow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,100));
        health = protectedCow.getHealth(); block.entityInside(heater.getBlockState(),h.getLevel(),heater.getBlockPos(),protectedCow);
        h.assertTrue(protectedCow.getHealth() == health, "native heat protection remains effective");
        heater.machineControl(null).setEnabled(false); tick(h,heater);
        var idleCow = h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(6,5,8));
        health = idleCow.getHealth(); block.entityInside(heater.getBlockState(),h.getLevel(),heater.getBlockPos(),idleCow);
        h.assertTrue(idleCow.getHealth() == health, "inactive source heater does not burn contacts");
        h.succeed();
    }
    @GameTest(template="test_empty", timeoutTicks=100)
    public static void twentyThermalCraftingRowsToolsSynchronizationMaterialRecoveryAndStoredProtection(GameTestHelper h) {
        int count = 0;
        for (var description : OriginalThermalCrafting.rows()) {
            var recipe = row(h,description.path()); var stacks = inputs(description);
            h.assertTrue(recipe.matches(grid(stacks),h.getLevel()),"actual source thermal grid " + description.path());
            var result = recipe.assemble(grid(stacks),h.getLevel().registryAccess());
            h.assertTrue(result.is(item(description.output()).getItem()) && result.getCount() == 1,"actual source thermal result");
            var decoded = wire(h,recipe);
            h.assertTrue(decoded.matches(grid(stacks),h.getLevel()),"native recipe synchronization retains thermal inputs");
            var remains = recipe.getRemainingItems(grid(stacks));
            for (int i=0;i<stacks.size();i++) if (stacks.get(i).getItem() instanceof GTToolItem tool)
                h.assertTrue(remains.get(i).is(tool) && remains.get(i).getDamageValue() == tool.toolType().damagePerCraft(),"source thermal tools incur crafting wear");
            var recovery = VanillaRecoveryRecipes.recipes().stream().filter(r -> r.mInputs[0].is(result.getItem())).findFirst().orElseThrow();
            h.assertTrue(RecipeInputs.consume(recovery,List.of(result),List.of(),1) != null,"clean thermal shell enters actual recovery predicate");
            var available = new HashMap<GTMaterial,Long>();
            for (var component : ItemMaterialRegistry.get(result).orElseThrow().components())
                available.merge(component.material().getTargetPulverMaterial().resolve(),MaterialRecoveryRules.pulverizedAmount(component.material(),component.amount()),Math::addExact);
            for (var out : recovery.mOutputs) {
                var form = MaterialEquivalence.form(out); h.assertTrue(form != null,"native thermal dust output");
                available.merge(form.material(),-form.prefix().getMaterialWeight()*out.getCount(),Long::sum);
            }
            h.assertTrue(available.values().stream().allMatch(v -> v>=0 && v<GTValues.U/72),"thermal recovery conserves each registered material");
            checkCrucible(h,result);
            var unsafe = result.copy(); stored(unsafe);
            h.assertTrue(RecipeInputs.consume(recovery,List.of(unsafe),List.of(),1) == null && com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(unsafe).isEmpty(),"stored thermal machine survives recycling guard");
            if (description.output().contains("flux_")) {
                stored(stacks.get(4));
                h.assertTrue(!recipe.matches(grid(stacks),h.getLevel()) && !decoded.matches(grid(stacks),h.getLevel()),"charged or configured base converter cannot be consumed by RF upgrade");
            }
            count++;
        }
        h.assertTrue(count == 20,"all original thermal crafting registrations"); h.succeed();
    }
}
