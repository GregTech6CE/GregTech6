package gregtech.compat.jade;

import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.content.energy.BatteryBoxEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import snownee.jade.api.BlockAccessor;

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class BatteryBoxJadeRegressionTests {
    private BatteryBoxJadeRegressionTests() {}

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void fullBoxesKeepStatusAndAllIconsWithinPacketBudget(GameTestHelper helper) {
        ItemStack battery = BuiltInRegistries.ITEM.stream().map(ItemStack::new)
                .filter(BatteryBoxEnergy::accepts).findFirst().orElseThrow();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (String id : new String[]{"battery_box_mv", "energy_storage_mv"}) {
            helper.getLevel().setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(
                    ResourceLocation.fromNamespaceAndPath("gt6ce", id)).defaultBlockState());
            var box = (EnergyNodeBlockEntity) helper.getLevel().getBlockEntity(pos);
            var inventory = box.batteryInventory();
            for (int count = 0; count <= inventory.getSlots(); count++) {
                if (count > 0) inventory.setStackInSlot(count - 1, battery.copy());
                assertSnapshot(helper, box, count);
            }
            var oversized = battery.copy();
            oversized.getOrCreateTag().putString("addon_note", "x".repeat(20_000));
            inventory.setStackInSlot(0, oversized);
            var before = inventory.getStackInSlot(0).copy();
            assertSnapshot(helper, box, inventory.getSlots());
            helper.assertTrue(ItemStack.matches(before, inventory.getStackInSlot(0)), "Inspection must preserve battery NBT");
        }
        helper.succeed();
    }

    private static void assertSnapshot(GameTestHelper helper, EnergyNodeBlockEntity box, int installed) {
        var tag = new CompoundTag();
        var accessor = (BlockAccessor) java.lang.reflect.Proxy.newProxyInstance(BlockAccessor.class.getClassLoader(),
                new Class<?>[]{BlockAccessor.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getBlockEntity" -> box;
                    case "getLevel" -> box.getLevel();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        BatteryBoxJadeProvider.INSTANCE.appendServerData(tag, accessor);
        var data = tag.getCompound(BatteryBoxJadeProvider.DATA_KEY);
        helper.assertTrue(data.contains("stored") && data.contains("enabled"), "Status must survive at " + installed + " batteries");
        helper.assertTrue(data.getInt("installed") == installed, "Installed battery count must agree");
        helper.assertTrue(data.getList("items", 10).size() == installed, "All battery icons must remain visible");
        // Reserve half of Jade's 16 KiB NBT accounting limit for its own and other providers' data.
        helper.assertTrue(tag.sizeInBytes() < 8192, "Battery snapshot too large: " + tag.sizeInBytes());
        helper.assertTrue(data.getLong("capacity") == box.batteryEnergy().capacity(), "Real energy capacity must be retained");
    }
}
