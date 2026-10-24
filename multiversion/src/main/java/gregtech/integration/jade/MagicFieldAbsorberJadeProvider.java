package gregtech.integration.jade;

import gregtech.blockentity.energy.MagicFieldAbsorberBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

public enum MagicFieldAbsorberJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;
    @Override public ResourceLocation getUid() { return ResourceLocation.fromNamespaceAndPath("gt6ce", "magic_field_absorber"); }
    @Override public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof MagicFieldAbsorberBlockEntity absorber) {
            tag.putBoolean("gt.magic.active", absorber.active());
            if (absorber.active()) {
                tag.putLong("gt.magic.output", absorber.output().amount());
                tag.putString("gt.magic.type", absorber.output().type().getShortName());
                tag.putString("gt.magic.face", absorber.outputSide().getName());
            }
        }
    }
    @Override public void appendTooltip(ITooltip tip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (!data.contains("gt.magic.active")) return;
        tip.add(data.getBoolean("gt.magic.active")
                ? Component.translatable("jade.gt6ce.magic_field_absorber.output", data.getLong("gt.magic.output"), data.getString("gt.magic.type"),
                        Component.translatable("jade.gt6ce.direction." + data.getString("gt.magic.face")))
                : Component.translatable("jade.gt6ce.magic_field_absorber.idle"));
    }
}
