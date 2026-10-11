package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.registry.GTEnergyNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Inventory invariants independent of the pending battery-box energy rewrite. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BatteryBoxInventoryTests {
    private static EnergyNodeBlockEntity box(GameTestHelper h) {
        var block = GTEnergyNodes.all().stream().map(net.minecraftforge.registries.RegistryObject::get)
                .filter(b -> b.spec().id().equals("battery_box_lv")).findFirst().orElseThrow();
        h.setBlock(new BlockPos(1, 2, 1), block);
        return (EnergyNodeBlockEntity) h.getBlockEntity(new BlockPos(1, 2, 1));
    }
    private static ItemStack cell(String name, int count) {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("gregtech", "battery_" + (name.equals("alkaline_button") ? "alkaline" : name) + "_lv"));
        if (item == null || item == Items.AIR) throw new IllegalStateException(name);
        return new ItemStack(item, count);
    }
    @GameTest(template="test_empty")
    public static void addressedSlotsSimulationAndRemovalRemainStable(GameTestHelper h) {
        var be = box(h); var slots = be.batteryInventory();
        var lead = cell("lead_acid", 3); var lithium = cell("lithium_cobalt", 1);
        h.assertTrue(slots.insertItem(3, lead, true).getCount() == 2 && slots.getStackInSlot(3).isEmpty(), "simulation must not install a cell");
        h.assertTrue(slots.insertItem(3, lead, false).getCount() == 2 && lead.getCount() == 3, "insert consumes exactly one in returned remainder");
        h.assertTrue(slots.getStackInSlot(0).isEmpty() && slots.getStackInSlot(3).is(lead.getItem()), "slot 3 must not become slot 0");
        h.assertTrue(slots.insertItem(3, lithium, false).getCount() == 1, "occupied slot rejects overwrite");
        slots.insertItem(1, lithium, false);
        var simulated = slots.extractItem(1, 1, true); simulated.setCount(0);
        h.assertTrue(slots.getStackInSlot(1).getCount() == 1, "simulated extraction returns detached copy");
        slots.extractItem(1, 64, false);
        h.assertTrue(slots.getStackInSlot(1).isEmpty() && slots.getStackInSlot(3).is(lead.getItem()), "removal cannot shift later slots");
        slots.setStackInSlot(0, lead);
        h.assertTrue(slots.getStackInSlot(0).getCount() == 1, "menu assignment enforces one item per slot");
        h.assertTrue(be.removeBattery().is(lead.getItem()) && slots.getStackInSlot(0).getCount() == 1 && slots.getStackInSlot(3).isEmpty(), "crowbar removes last occupied slot without moving others");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void sparseSlotsPersistAndAutomationUsesTheMenuInventory(GameTestHelper h) {
        var be = box(h); var item = cell("nickel_cadmium", 1);
        item.getOrCreateTag().putString("test.marker", "preserved");
        be.batteryInventory().setStackInSlot(2, item);
        var saved = be.saveWithoutMetadata();
        var restored = new EnergyNodeBlockEntity(be.getBlockPos(), be.getBlockState());
        restored.load(saved);
        h.assertTrue(restored.batteryInventory().getStackInSlot(0).isEmpty() && restored.batteryInventory().getStackInSlot(1).isEmpty(), "NBT must preserve leading empty slots");
        h.assertTrue("preserved".equals(restored.batteryInventory().getStackInSlot(2).getOrCreateTag().getString("test.marker")), "NBT must preserve exact slot and item metadata");
        for (var side : Direction.values()) {
            var handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElseThrow(() -> new AssertionError("missing sided inventory"));
            h.assertTrue(handler.getStackInSlot(2) == be.batteryInventory().getStackInSlot(2), "sided wrapper and GUI share the exact stored stack on " + side);
        }
        var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER);
        be.invalidateCaps();
        h.assertTrue(!cap.isPresent(), "removed block invalidates item capability");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void actualBreakDropsSparseInventoryOnce(GameTestHelper h) {
        var be = box(h); var cell = cell("alkaline_button", 1);
        cell.getOrCreateTag().putString("test.marker", "sparse-drop");
        be.batteryInventory().setStackInSlot(3, cell);
        var pos = be.getBlockPos();
        h.getLevel().destroyBlock(pos, true);
        var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(0.8));
        int total = drops.stream().map(net.minecraft.world.entity.item.ItemEntity::getItem)
                .filter(s -> s.is(cell.getItem()) && s.hasTag() && "sparse-drop".equals(s.getTag().getString("test.marker")))
                .mapToInt(ItemStack::getCount).sum();
        h.assertTrue(total == 1, "actual block break must drop slot 3 exactly once");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void machineCircuitRequirementsOnlyAcceptSameOrHigherTier(GameTestHelper h) {
        String[] names = {"basic", "good", "advanced", "elite", "master", "ultimate"};
        for (int required = 1; required <= names.length; required++) {
            Object resolved = com.gregtech.gregtech.data.MachineRecipeIngredients.resolve(
                    "circuit:" + required, com.gregtech.gregtech.content.material.Materials.Steel, 1);
            var ingredient = net.minecraft.world.item.crafting.Ingredient.fromJson(new com.google.gson.Gson().toJsonTree(resolved));
            for (int actual = 1; actual <= names.length; actual++) {
                var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("gregtech", "circuit_" + names[actual - 1]));
                h.assertTrue(ingredient.test(new ItemStack(item)) == (actual >= required),
                        "machine circuit requirement " + required + " actual tier " + actual);
            }
            h.assertTrue(!ingredient.test(new ItemStack(Items.REDSTONE)), "redstone cannot substitute a circuit");
        }
        h.succeed();
    }

}
