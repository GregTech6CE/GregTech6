package gregtech.compat.jade;

import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

/** Compact battery snapshots: Jade drops whole sections when its 16 KiB NBT budget is exceeded. */
public enum BatteryBoxJadeProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    INSTANCE;

    static final String DATA_KEY = "gt_battery";
    private static final int MAX_SLOTS = 16;
    private static final int MAX_ICON_BYTES = 512;

    @Override public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "battery_box");
    }

    @Override public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof EnergyNodeBlockEntity node) || !node.isBatteryBox()) return;
        var data = new CompoundTag();
        var spec = node.spec();
        var battery = node.batteryEnergy();
        var control = node.machineControl(null);
        data.putBoolean("enabled", control.enabled());
        data.putBoolean("running", control.running());
        data.putString("unit", spec.outType().getShortName());
        data.putLongArray("input", new long[]{node.getEnergySizeInputRecommended(spec.inType(), null),
                node.getEnergySizeInputMin(spec.inType(), null), node.getEnergySizeInputMax(spec.inType(), null)});
        data.putLongArray("output", new long[]{node.getEnergySizeOutputRecommended(spec.outType(), null),
                node.getEnergySizeOutputMin(spec.outType(), null), node.getEnergySizeOutputMax(spec.outType(), null)});
        data.putLong("buffer", battery.buffer());
        data.putLong("bufferMax", battery.bufferCapacity());
        data.putLong("stored", battery.stored());
        data.putLong("capacity", battery.capacity());
        data.putLong("offered", battery.offered());
        data.putInt("mode", control.mode());
        var icons = new ListTag();
        var inventory = node.batteryInventory();
        for (int slot = 0; slot < Math.min(MAX_SLOTS, inventory.getSlots()); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            var icon = saveIcon(stack, accessor);
            // Arbitrary third-party battery data must not evict the entire status panel.
            // Only the display copy loses oversized metadata; the real stack is never modified.
            if (icon.sizeInBytes() > MAX_ICON_BYTES) icon = saveIcon(new ItemStack(stack.getItem(), stack.getCount()), accessor);
            icons.add(icon);
        }
        data.put("items", icons);
        tag.put(DATA_KEY, data);
    }

    private static CompoundTag saveIcon(ItemStack stack, BlockAccessor accessor) {
//? if >=1.21.1 {
        /*return (CompoundTag) stack.save(accessor.getLevel().registryAccess());
*///?} else {
        return stack.save(new CompoundTag());
//?}
    }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var tag = accessor.getServerData();
        if (!tag.contains(DATA_KEY)) return;
//? if >=1.21.1 {
        /*tooltip.remove(snownee.jade.api.JadeIds.UNIVERSAL_ITEM_STORAGE);
*///?} else {
        tooltip.remove(snownee.jade.api.Identifiers.UNIVERSAL_ITEM_STORAGE);
//?}
        var data = tag.getCompound(DATA_KEY);
        var unit = JadeText.unit(data.getString("unit"));
        tooltip.add(label(!data.getBoolean("enabled") ? "disabled" : data.getBoolean("running") ? "working" : "waiting"));
        for (String direction : new String[]{"input", "output"}) {
            var rates = data.getLongArray(direction);
            if (rates.length != 3) continue;
            String value = direction.equals("output")
                    ? EnergyRateText.offered(data.getLong("offered"), rates[0], rates[1], rates[2], data.getString("unit"))
                    : EnergyRateText.rate(rates[0], rates[1], rates[2], data.getString("unit"));
            line(tooltip, "energy_rate", label(direction), value);
        }
        line(tooltip, "buffer", data.getLong("buffer"), data.getLong("bufferMax"), unit);
        line(tooltip, "mode", data.getInt("mode"));
        line(tooltip, "battery_total", data.getLong("stored"), data.getLong("capacity"), unit);
        var items = data.getList("items", 10);
        if (items.isEmpty()) return;
        for (int i = 0; i < Math.min(MAX_SLOTS, items.size()); i++) {
//? if >=1.21.1 {
            /*var stack = ItemStack.parseOptional(accessor.getLevel().registryAccess(), items.getCompound(i));
*///?} else {
            var stack = ItemStack.of(items.getCompound(i));
//?}
            var icon = IElementHelper.get().item(stack);
            icon.message(Component.translatable("jade.gt6ce.omni.item_count", stack.getHoverName(), stack.getCount()).getString());
            if (i % 8 == 0) tooltip.add(icon); else tooltip.append(icon);
        }
    }

    private static Component label(String key) { return JadeText.status("jade.gt6ce.omni." + key); }
    private static void line(ITooltip tooltip, String key, Object... args) {
        JadeText.detail(tooltip, "jade.gt6ce.omni." + key, args);
    }

    @Override public int getDefaultPriority() { return 1002; }
}
