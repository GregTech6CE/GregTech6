package com.gregtech.gregtech.integration.jade;

import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock;
import com.gregtech.gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity;
import com.gregtech.gregtech.api.material.GTMaterial;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Temperature, buffered HU, and internal material content for smelting crucibles. */
public enum CrucibleJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gregtech", "crucible");
    private static final String NBT_TEMPERATURE = "gt_crucible_temp";
    private static final String NBT_BUFFER = "gt_crucible_buffer";
    private static final String NBT_CONTENT = "gt_crucible_content";
    private static final String NBT_CACHE = "gt_crucible_cache";
    private static final String NBT_FILL = "gt_crucible_fill";
    private static final String NBT_MOLTEN = "gt_crucible_molten";
    private static final String NBT_WARNING = "gt_crucible_warning";
    private static final String NBT_MELT_LIMIT = "gt_crucible_melt_limit";
    private static final String NBT_DISPLAY_MAT = "gt_crucible_display_mat";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        var entity = accessor.getBlockEntity();
        SmeltingCrucibleEntity crucible = entity instanceof SmeltingCrucibleEntity direct ? direct
                : entity instanceof com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity port
                    ? (port.crucibleController() instanceof SmeltingCrucibleEntity owner ? owner : null) : null;
        if (crucible == null) return;
        appendCrucibleData(tag, crucible);
    }

    public static void appendCrucibleData(CompoundTag tag, SmeltingCrucibleEntity crucible) {
        tag.putLong(NBT_TEMPERATURE, crucible.getTemperature());
        tag.putLong(NBT_BUFFER, crucible.getEnergyBuffer());
        tag.putString(NBT_CONTENT, crucible.formatContentSummary());
        tag.putInt(NBT_FILL, crucible.getDisplayFillPercent());
        tag.putBoolean(NBT_MOLTEN, crucible.isDisplayedMolten());
        tag.putBoolean(NBT_WARNING, crucible.isMeltDownWarning());
        tag.putLong(NBT_MELT_LIMIT, crucible.getMeltDownLimitK());
        GTMaterial displayed = crucible.getDisplayedMaterial();
        if (displayed != null && displayed.isValid()) {
            tag.putString(NBT_DISPLAY_MAT, displayed.getLocalName());
        }
        ItemStack cache = crucible.getCacheStack();
        if (!cache.isEmpty()) {
            tag.put(NBT_CACHE, cache.save(crucible.getLevel().registryAccess()));
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(NBT_TEMPERATURE, CompoundTag.TAG_LONG)) {
            return;
        }
        tooltip.add(Component.translatable("jade.gregtech.crucible.temperature", data.getLong(NBT_TEMPERATURE)));
        tooltip.add(Component.translatable("jade.gregtech.crucible.buffer", format(data.getLong(NBT_BUFFER))));
        if (data.contains(NBT_MELT_LIMIT)) {
            tooltip.add(Component.translatable("jade.gregtech.crucible.melt_limit", data.getLong(NBT_MELT_LIMIT)));
        }
        if (data.contains(NBT_FILL)) {
            tooltip.add(Component.translatable("jade.gregtech.crucible.fill", data.getInt(NBT_FILL)));
        }
        if (data.contains(NBT_DISPLAY_MAT)) {
            String stateKey = data.getBoolean(NBT_MOLTEN) ? "molten" : "solid";
            tooltip.add(Component.translatable("jade.gregtech.crucible.display", data.getString(NBT_DISPLAY_MAT),
                    Component.translatable("jade.gregtech.crucible.state." + stateKey)));
        }
        if (data.getBoolean(NBT_WARNING)) {
            tooltip.add(Component.translatable("jade.gregtech.crucible.warning"));
        }
        if (data.contains(NBT_CONTENT)) {
            tooltip.add(Component.translatable("jade.gregtech.crucible.content", data.getString(NBT_CONTENT)));
        }
        if (data.contains(NBT_CACHE, CompoundTag.TAG_COMPOUND)) {
            ItemStack cache = ItemStack.parseOptional(accessor.getLevel().registryAccess(),data.getCompound(NBT_CACHE));
            tooltip.add(Component.translatable("jade.gregtech.crucible.cache",
                    cache.getHoverName(), cache.getCount()));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static String format(long value) {
        return Long.toString(value);
    }
}
