package gregtech.gametest;

import gregtech.block.energy.EnergyNodeBlock;
import gregtech.blockentity.energy.ChemicalBatteryBlockEntity;
import gregtech.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.blockentity.inventory.MassStorageBlockEntity;
import gregtech.content.energy.*;
import gregtech.data.GregTechTags;
import gregtech.data.MachineRecipeMaps;
import gregtech.item.EnergyCrystalItem;
import gregtech.registry.GTItems;
import gregtech.registry.GTItemBarrels;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

//? if >=1.21.1 {
/*@net.neoforged.neoforge.gametest.GameTestHolder("gt6ce_crystal_barrel")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
@net.minecraftforge.gametest.GameTestHolder("gt6ce_crystal_barrel")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
public final class CrystalBarrelTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void canonical_crystals_and_survival_recipes(GameTestHelper h) {
        for (var spec : EnergyCrystalSpec.all()) {
            var stack = GTItems.getStack(spec.prefix(), spec.material());
            h.assertTrue(stack.getItem() instanceof EnergyCrystalItem, "Material recipe target is the functional crystal: " + spec.id());
            var item = (EnergyCrystalItem) stack.getItem();
            h.assertTrue(item.getBlock().asItem() == item && BuiltInRegistries.ITEM.getKey(item).equals(id(spec.id())), "One canonical item/block identity");
            h.assertTrue(item.stored(stack) == 0 && item.getEnergyCapacity(stack, GregTechTags.Energy.LU) == spec.capacity(), "Original empty crystal capacity");
            h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.EU, stack, spec.voltage(), 1, h.getLevel(), BlockPos.ZERO, true) == 0, "Reject EU");
            h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.LU, stack, spec.voltage(), 1, h.getLevel(), BlockPos.ZERO, false) == 1 && item.stored(stack) == 0, "Simulation does not charge");
            item.doEnergyInjection(GregTechTags.Energy.LU, stack, spec.voltage(), 2, h.getLevel(), BlockPos.ZERO, true);
            h.assertTrue(item.stored(stack) == spec.voltage() * 2 && stack.getMaxStackSize() == 1, "Charged stacks cannot duplicate charge by stacking");
            var pos = h.absolutePos(new BlockPos(2, 2, 2));
            h.getLevel().setBlockAndUpdate(pos, item.getBlock().defaultBlockState());
            var cell = (ChemicalBatteryBlockEntity) h.getLevel().getBlockEntity(pos);
            cell.setCharge(item.stored(stack));
            h.assertTrue(item.stored(cell.asItem()) == item.stored(stack), "Existing battery block retains crystal charge");
            h.assertTrue(MachineRecipeMaps.Autoclave.mRecipeList.stream().anyMatch(r -> Arrays.stream(r.mOutputs).anyMatch(out -> out.is(item))),
                    "Autoclave grows functional crystal: " + spec.id());
        }
        for (var spec : CrystalChargerDefinitions.specifications()) {
            h.assertTrue(BuiltInRegistries.BLOCK.get(id(spec.id())) instanceof EnergyNodeBlock, "Existing energy node architecture: " + spec.id());
            recipe(h, "energy/" + spec.id());
        }
        for (var barrel : GTItemBarrels.ALL) recipe(h, "storage/" + barrel.get().spec().id());
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void charger_uses_shared_lu_buffer_and_faces(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        var spec = new EnergyCrystalSpec(false, 1);
        var item = (EnergyCrystalItem) GTItems.getStack(spec.prefix(), spec.material()).getItem();
        var stack = new ItemStack(item); item.setCharge(stack, 32 * 80);
        var block = (EnergyNodeBlock) BuiltInRegistries.BLOCK.get(id("crystal_charger_lv"));
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var node = (EnergyNodeBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(node.batteryInventory().getSlots() == 4 && node.installBattery(stack), "Four real inventory slots accept LU cells");
        var euCell = new ItemStack(BuiltInRegistries.ITEM.get(id("battery_lead_acid_lv")));
        h.assertTrue(!node.installBattery(euCell), "LU box rejects EU batteries");
        for (Direction side : Direction.values()) {
            h.assertTrue(node.isEnergyAcceptingFrom(GregTechTags.Energy.LU, side, true) == (side != node.getBlockState().getValue(EnergyNodeBlock.FACING)), "Five LU input faces");
            h.assertTrue(node.isEnergyEmittingTo(GregTechTags.Energy.LU, side, true) == (side == node.getBlockState().getValue(EnergyNodeBlock.FACING)), "Front LU output");
            h.assertTrue(!node.isEnergyAcceptingFrom(GregTechTags.Energy.EU, side, true), "No EU conversion");
        }
        node.batteryEnergy().tick(1, h.getLevel(), pos);
        // The box requests 40 packets, but TileEntityBase08Battery caps one call at mSizeRec (32 here).
        h.assertTrue(node.batteryEnergy().buffer() == 1024 && node.batteryEnergy().offered() == 1, "Original item packet cap and box output count");
        h.assertTrue(node.getEnergyCapacity(GregTechTags.Energy.LU, null) == spec.capacity(), "Jade capacitor reports LU capacity");
        var inventory = new ArrayList<ItemStack>(List.of(new ItemStack(item), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        var box = new BatteryBoxEnergy(inventory, 32, GregTechTags.Energy.LU);
        long injected = 0;
        for (int tick = 0; tick < 900; tick++) {
            box.tick(tick, h.getLevel(), pos);
            injected += 32 * box.inject(32, 2, true);
        }
        h.assertTrue(item.stored(inventory.get(0)) > 0 && item.stored(inventory.get(0)) + box.buffer() == injected,
                "Shared charging thresholds preserve total energy");
        var saved = new CompoundTag(); box.save(saved);
        var restored = new BatteryBoxEnergy(inventory, 32, GregTechTags.Energy.LU); restored.load(saved);
        h.assertTrue(restored.buffer() == box.buffer(), "Shared buffer persistence");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void barrels_use_actual_small_capacity_and_persistence(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 2, 4));
        for (var holder : GTItemBarrels.ALL) {
            var block = holder.get();
            h.assertTrue(block.material().isValid(), "Valid original wood material: " + block.spec().id());
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var storage = (MassStorageBlockEntity) h.getLevel().getBlockEntity(pos);
            long cap = block.spec().capacity();
            h.assertTrue(storage.getType().isValid(block.defaultBlockState()), "Shared mass-storage entity supports barrel");
            h.assertTrue(storage.baseCapacity() == cap && storage.maximumItems() == cap, "Original 5000/10000 capacity");
            for (int count = 0; count < cap; count += 64) storage.insert(new ItemStack(Items.APPLE, (int)Math.min(64, cap - count)));
            h.assertTrue(storage.stored() == cap && storage.insert(new ItemStack(Items.APPLE)) == 0, "Real capacity limit, not just tooltip");
            h.assertTrue(storage.insert(new ItemStack(Items.CARROT)) == 0, "Single item identity");
            storage.toggleAutoOutput();
//? if >=1.21.1 {
            /*var saved = storage.saveWithId(h.getLevel().registryAccess());
            var restored = (MassStorageBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, block.defaultBlockState(), saved, h.getLevel().registryAccess());
*///?} else {
            var saved = storage.saveWithFullMetadata();
            var restored = (MassStorageBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, block.defaultBlockState(), saved);
//?}
            // Both native loaders restore through the shared mass-storage entity registration.
            h.assertTrue(storage.autoOutput(), "Existing adjustable-wrench output mode");
            h.assertTrue(restored != null && restored.stored() == cap && restored.autoOutput() && restored.baseCapacity() == cap, "Count and mode persistence");
        }
        h.succeed();
    }

    private static void recipe(GameTestHelper h, String path) {
        var entry = h.getLevel().getRecipeManager().byKey(id("hand/" + path));
        h.assertTrue(entry.isPresent(), "Crafting recipe: " + path);
//? if >=1.21.1 {
        /*var recipe = entry.orElseThrow().value();
*///?} else {
        var recipe = entry.orElseThrow();
//?}
        h.assertTrue(recipe.getIngredients().stream().allMatch(i -> i.getItems().length > 0), "All crafting ingredients resolve: " + path);
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("gt6ce", path); }
}
