package com.gregtech.gregtech.integration.jade;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialPresentation;
import com.gregtech.gregtech.blockentity.tool.DustFunnelBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Reads the authoritative mode and fractional reservoir from the server. */
public enum DustFunnelJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gt6ce", "dust_funnel");
    private static final String MODE = "gt_dust_funnel_mode";
    private static final String MATERIAL = "gt_dust_funnel_material";
    private static final String AMOUNT = "gt_dust_funnel_amount";
    private static final String NEEDS_MORE = "gt_dust_funnel_needs_more";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof DustFunnelBlockEntity funnel)) return;
        tag.putInt(MODE, funnel.mode());
        if (funnel.retainedMaterial() != null && funnel.retainedAmount() > 0) {
            tag.putString(MATERIAL, funnel.retainedMaterial().getName());
            tag.putLong(AMOUNT, funnel.retainedAmount());
            tag.putBoolean(NEEDS_MORE, funnel.needsMoreDust());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(MODE, CompoundTag.TAG_INT)) return;
        int mode = Math.floorMod(data.getInt(MODE), 5);
        tooltip.add(Component.translatable("jade.gt6ce.dust_funnel.mode",
                Component.translatable("jade.gt6ce.dust_funnel.size." + mode)));
        if (data.getLong(AMOUNT) > 0 && data.contains(MATERIAL, CompoundTag.TAG_STRING)) {
            var material = GTMaterialRegistry.get(data.getString(MATERIAL));
            tooltip.add(Component.translatable("jade.gt6ce.dust_funnel.fraction",
                    data.getLong(AMOUNT) / GTValues.U72, MaterialPresentation.name(material)));
            if (data.getBoolean(NEEDS_MORE))
                tooltip.add(Component.translatable("jade.gt6ce.dust_funnel.fraction_hint"));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
