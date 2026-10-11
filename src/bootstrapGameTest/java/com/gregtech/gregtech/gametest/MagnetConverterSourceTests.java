package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.block.energy.*;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity;
import com.gregtech.gregtech.content.energy.OriginalMagnetCrafting;
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
public final class MagnetConverterSourceTests {
    @GameTest(template="test_empty", timeoutTicks=100)
    public static void nativeBipolarMuReceiversStoppedResidualBufferAndLegacyControls(GameTestHelper h) {
        var polarizer = java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(), false)
                .filter(b -> b instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock)
                .map(b -> (com.gregtech.gregtech.block.machine.BasicMachineBlock)b)
                .filter(b -> b.basicSpec().machineName().equals("polarizer") && b.basicSpec().tier() == 1).findFirst().orElseThrow();
        int x = 3;
        for (String id : List.of("electromagnet_lv", "flux_magnet_lv")) {
            var pos = new BlockPos(x,4,4);
            var block = (MagnetMachineBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:" + id));
            h.setBlock(pos, block.defaultBlockState().setValue(EnergyNodeBlock.FACING, Direction.UP));
            var tile = (EnergyNodeBlockEntity) h.getBlockEntity(pos);
            h.setBlock(pos.above(),polarizer.defaultBlockState()); h.setBlock(pos.below(),polarizer.defaultBlockState());
            var front = (BasicMachineBlockEntity) h.getBlockEntity(pos.above());
            var back = (BasicMachineBlockEntity) h.getBlockEntity(pos.below());
            long rate = tile.spec().inputRate();
            h.assertTrue(tile.doEnergyInjection(tile.spec().inType(),Direction.EAST,-rate*2,1,true) == 1,"actual signed source input accepted");
            tile.machineControl(null).setEnabled(false);
            EnergyNodeBlockEntity.serverTick(h.getLevel(),tile.getBlockPos(),tile.getBlockState(),tile);
            h.assertTrue(front.getEnergyTick() == 32 && back.getEnergyTick() == 32 && tile.stored() == 0,"stopped residual buffer sends both32MU poles with one waste tick to actual polarizers");
            h.assertTrue(tile.machineControl(null).running() && tile.machineControl(null).active() && !tile.getBlockState().getValue(MagnetMachineBlock.ACTIVE),"source possible/emitted flags differ from stopped visuals");
            var old = new net.minecraft.nbt.CompoundTag(); old.putLong("gt.buffer",rate*2);
            old.putBoolean("gt.magnet_stopped",true); old.putByte("gt.magnet_mode",(byte)1); old.putBoolean("gt.converter_stopped",false);
            tile.load(old);
            h.assertTrue(!tile.machineControl(null).enabled() && tile.machineControl(null).mode() == 1 && tile.stored() == rate*2,"old magnet keys override unrelated old converter flag");
            EnergyNodeBlockEntity.serverTick(h.getLevel(),tile.getBlockPos(),tile.getBlockState(),tile);
            h.assertTrue(front.getEnergyTick() == 62 && back.getEnergyTick() == 62 && tile.stored() == rate/8,"original mode sends30MU per pole and consumes15/16 of maximum input once");
            var saved = tile.saveWithoutMetadata();
            h.assertTrue(saved.getBoolean("gt.magnet_stopped") == saved.getBoolean("gt.converter_stopped") && saved.getByte("gt.magnet_mode") == saved.getByte("gt.mode"),"legacy aliases mirror single canonical state");
            var restored = new EnergyNodeBlockEntity(pos,tile.getBlockState()); restored.load(saved);
            h.assertTrue(restored.stored() == rate/8 && restored.machineControl(null).mode() == 1 && !restored.machineControl(null).enabled(),"native controls and buffer method roundtrip, not a world restart");
            if (id.startsWith("flux_")) {
                var fe = tile.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY,Direction.EAST).orElseThrow(IllegalStateException::new);
                h.assertTrue(!fe.canReceive() && fe.receiveEnergy(256,false) == 0,"cached FE wrapper respects source stopped state");
                tile.machineControl(null).setEnabled(true);
                h.assertTrue(fe.canReceive(),"source FE input restored by switch");
                h.setBlock(pos,tile.getBlockState().setValue(EnergyNodeBlock.FACING,Direction.EAST));
                h.assertTrue(!fe.canReceive() && fe.receiveEnergy(256,false) == 0,"cached FE face follows wrench rotation into a MU output pole");
            }
            x += 4;
        }
        h.succeed();
    }
    @GameTest(template="test_empty", timeoutTicks=100)
    public static void tenMagnetCraftingRowsToolsSynchronizationMaterialRecoveryAndStoredProtection(GameTestHelper h) {
        int count = 0;
        for (var description : OriginalMagnetCrafting.rows()) {
            var recipe = row(h,description.path()); var stacks = inputs(description);
            h.assertTrue(recipe.matches(grid(stacks),h.getLevel()),"actual source magnet grid " + description.path());
            var result = recipe.assemble(grid(stacks),h.getLevel().registryAccess());
            h.assertTrue(result.is(item(description.output()).getItem()) && result.getCount() == 1,"actual source magnet result");
            var decoded = wire(h,recipe);
            h.assertTrue(decoded.matches(grid(stacks),h.getLevel()),"native recipe synchronization retains magnet inputs");
            var remains = recipe.getRemainingItems(grid(stacks));
            for (int i=0;i<stacks.size();i++) if (stacks.get(i).getItem() instanceof GTToolItem tool)
                h.assertTrue(remains.get(i).is(tool) && remains.get(i).getDamageValue() == tool.toolType().damagePerCraft(),"source magnet tools incur crafting wear");
            var recovery = VanillaRecoveryRecipes.recipes().stream().filter(r -> r.mInputs[0].is(result.getItem())).findFirst().orElseThrow();
            h.assertTrue(RecipeInputs.consume(recovery,List.of(result),List.of(),1) != null,"clean magnet shell enters actual recovery predicate");
            var available = new HashMap<GTMaterial,Long>();
            for (var component : ItemMaterialRegistry.get(result).orElseThrow().components())
                available.merge(component.material().getTargetPulverMaterial().resolve(),MaterialRecoveryRules.pulverizedAmount(component.material(),component.amount()),Math::addExact);
            for (var out : recovery.mOutputs) {
                var form = MaterialEquivalence.form(out); h.assertTrue(form != null,"native magnet dust output");
                available.merge(form.material(),-form.prefix().getMaterialWeight()*out.getCount(),Long::sum);
            }
            h.assertTrue(available.values().stream().allMatch(v -> v>=0 && v<GTValues.U/72),"magnet recovery conserves each registered material");
            checkCrucible(h,result);
            var unsafe = result.copy(); stored(unsafe);
            h.assertTrue(RecipeInputs.consume(recovery,List.of(unsafe),List.of(),1) == null && com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(unsafe).isEmpty(),"stored magnet machine survives recycling guard");
            if (description.output().contains("flux_")) {
                stored(stacks.get(4));
                h.assertTrue(!recipe.matches(grid(stacks),h.getLevel()) && !decoded.matches(grid(stacks),h.getLevel()),"charged or configured base converter cannot be consumed by RF upgrade");
            }
            count++;
        }
        h.assertTrue(count == 10,"all original magnet crafting registrations"); h.succeed();
    }
}
