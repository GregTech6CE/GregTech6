package com.gregtech.gregtech.integration.jade;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/** Shows stored / capacity for EU, HU, and Steam energy buffers. */
public enum EnergyBufferJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gregtech", "energy_buffer");
    private static final String NBT_UNIT = "gt_unit";
    private static final String NBT_STORED = "gt_stored";
    private static final String NBT_CAPACITY = "gt_capacity";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if(accessor.getBlockEntity() instanceof com.gregtech.gregtech.blockentity.energy.ChemicalBatteryBlockEntity battery) {
            tag.putString(NBT_UNIT,"EU");tag.putLong(NBT_STORED,battery.stored());tag.putLong(NBT_CAPACITY,battery.spec().capacity());
            return;
        }
        if (!(accessor.getBlockEntity() instanceof IEnergyBlock energy)) {
            return;
        }
        for (GregTechTags.Tag type : energy.getEnergyTypes(null)) {
            if (!energy.isEnergyCapacitorType(type, null)) {
                continue;
            }
            long capacity = energy.getEnergyCapacity(type, null);
            if (capacity <= 0) {
                continue;
            }
            tag.putString(NBT_UNIT, type.getShortName());
            tag.putLong(NBT_STORED, energy.getEnergyStored(type, null));
            tag.putLong(NBT_CAPACITY, capacity);
            return;
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(NBT_STORED, CompoundTag.TAG_LONG)) {
            return;
        }
        long stored = data.getLong(NBT_STORED);
        long capacity = data.getLong(NBT_CAPACITY);
        String unit = data.getString(NBT_UNIT);
        int pct = percent(stored, capacity);
        tooltip.add(Component.translatable(
                "jade.gregtech.energy.stored",
                format(stored),
                format(capacity),
                unit,
                Integer.toString(pct)));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private static int percent(long stored, long capacity) {
        if (capacity <= 0) {
            return 0;
        }
        return (int) Math.min(100, stored * 100 / capacity);
    }
}
