package com.gregtech.gregtech.item;

import com.gregtech.gregtech.api.energy.item.IItemEnergy;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Retained pre-chemistry battery IDs. Original capacity, GT6 battery packet rules. */
public class BatteryItem extends Item implements IItemEnergy {
    private final long capacity;
    private final int tier;
    private final String name;

    public BatteryItem(String name, long capacity, int tier, Properties properties) {
        super(properties);
        if (tier < 1 || tier > 5 || capacity <= 0) throw new IllegalArgumentException("Legacy battery must be LV..IV with positive capacity");
        this.name = name;
        this.capacity = capacity;
        this.tier = tier;
    }

    public String batteryName() { return name; }
    public int tier() { return tier; }
    public long voltage() { return 8L << (tier * 2); }
    public long minimumPacket() { return voltage() / 2; }
    public long maximumPacket() { return voltage() * 2; }

    @Override
    public boolean canEnergyInjection(ItemStack stack, GregTechTags.Tag type, long size) {
        return isEnergyType(stack, type) && stack.getCount() == 1
                && size >= minimumPacket() && size <= maximumPacket();
    }

    @Override
    public boolean canEnergyExtraction(ItemStack stack, GregTechTags.Tag type, long size) {
        return canEnergyInjection(stack, type, size);
    }

    private long packet(ItemStack stack, GregTechTags.Tag type, long size, long amount) {
        if (amount <= 0 || size == Long.MIN_VALUE) return 0;
        long packet = Math.abs(size);
        return canEnergyInjection(stack, type, packet) ? packet : 0;
    }

    // ── IItemEnergy ──────────────────────────────────────────────────────────

    @Override
    public boolean isEnergyType(ItemStack stack, GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.EU; }

    @Override
    public long getEnergyCapacity(ItemStack stack, GregTechTags.Tag energyType) {
        return energyType == GregTechTags.Energy.EU ? capacity : 0;
    }

    @Override
    public long getEnergyStored(ItemStack stack, GregTechTags.Tag energyType) {
        if (energyType != GregTechTags.Energy.EU) return 0;
        CompoundTag tag = stack.getTag();
        return tag != null ? Math.max(0, Math.min(capacity, tag.getLong("gt.charge"))) : 0;
    }

    @Override
    public long doEnergyInjection(GregTechTags.Tag energyType, ItemStack stack, long size, long amount,
                                   Level level, BlockPos pos, boolean doInject) {
        size = packet(stack, energyType, size, amount);
        if (size == 0) return 0;
        long space = capacity - getEnergyStored(stack, energyType);
        if (space <= 0) return 0;
        // TileEntityBase08Battery:155-173: cap packets per call; permit the last partial packet.
        long accepted = Math.min(Math.min(voltage(), amount), Math.max(1, space / size));
        if (doInject && accepted > 0) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putLong("gt.charge", capacity - Math.max(0, space - accepted * size));
        }
        return accepted;
    }

    @Override
    public long doEnergyExtraction(GregTechTags.Tag energyType, ItemStack stack, long size, long amount,
                                    Level level, BlockPos pos, boolean doExtract) {
        size = packet(stack, energyType, size, amount);
        if (size == 0) return 0;
        long stored = getEnergyStored(stack, energyType);
        long available = Math.min(Math.min(voltage(), amount), stored / size);
        if (doExtract && available > 0) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putLong("gt.charge", stored - available * size);
        }
        return available;
    }

    // ── Tooltip ──────────────────────────────────────────────────────────────

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getEnergyStored(stack, GregTechTags.Energy.EU) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long stored = getEnergyStored(stack, GregTechTags.Energy.EU);
        return capacity > 0 ? (int) (stored * 13 / capacity) : 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00CC00;
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable Level level,
                                java.util.List<net.minecraft.network.chat.Component> tip,
                                net.minecraft.world.item.TooltipFlag flags) {
        super.appendHoverText(stack, level, tip, flags);
        tip.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.electric_tool.energy",
                getEnergyStored(stack, GregTechTags.Energy.EU), capacity));
        tip.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.chemical_battery.packet",
                minimumPacket(), maximumPacket()));
    }
}
