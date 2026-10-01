package com.gregtech.gregtech.integration.jade;

import com.gregtech.gregtech.block.machine.SolidBurningBoxBlock;
import com.gregtech.gregtech.blockentity.machine.SolidBurningBoxBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/** Stored heat and front inventory for solid burning boxes. */
public enum BurningBoxJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gregtech", "burning_box");
    private static final String NBT_HEAT = "gt_heat";
    private static final String NBT_FUEL = "gt_fuel";
    private static final String NBT_ASH = "gt_ash";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof SolidBurningBoxBlockEntity burningBox)) {
            return;
        }
        tag.putLong(NBT_HEAT, burningBox.getStoredHeat());
        ItemStack fuel = burningBox.getFuelStack();
        if (!fuel.isEmpty()) {
            tag.put(NBT_FUEL, fuel.save(accessor.getLevel().registryAccess()));
        }
        ItemStack ash = burningBox.getAshStack();
        if (!ash.isEmpty()) {
            tag.put(NBT_ASH, ash.save(accessor.getLevel().registryAccess()));
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(NBT_HEAT, CompoundTag.TAG_LONG)) {
            return;
        }
        tooltip.add(Component.translatable("jade.gregtech.burning_box.heat", format(data.getLong(NBT_HEAT))));
        if (data.contains(NBT_FUEL, CompoundTag.TAG_COMPOUND)) {
            ItemStack fuel = ItemStack.parseOptional(accessor.getLevel().registryAccess(),data.getCompound(NBT_FUEL));
            tooltip.add(Component.translatable("jade.gregtech.burning_box.fuel",
                    fuel.getHoverName(), fuel.getCount()));
        }
        if (data.contains(NBT_ASH, CompoundTag.TAG_COMPOUND)) {
            ItemStack ash = ItemStack.parseOptional(accessor.getLevel().registryAccess(),data.getCompound(NBT_ASH));
            tooltip.add(Component.translatable("jade.gregtech.burning_box.ash",
                    ash.getHoverName(), ash.getCount()));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }
}
