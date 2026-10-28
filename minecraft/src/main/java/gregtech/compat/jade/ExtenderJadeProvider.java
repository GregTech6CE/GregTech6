package gregtech.compat.jade;

import gregtech.content.block.misc.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

public enum ExtenderJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "extender");
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof ExtenderBlockEntity relay) || !relay.universal()) return;
        var sourceBlock = (SourceExtenderBlock) relay.getBlockState().getBlock();
        var control = relay.machineControl(
                sourceBlock.spec().bridge ? accessor.getHitResult().getDirection() : null);
        var controlData = new CompoundTag();
        controlData.putBoolean("available", control != null && control.available());
        if (control != null && control.available()) {
            controlData.putBoolean("enabled", control.enabled());
            controlData.putBoolean("running", control.running());
            controlData.putBoolean("active", control.active());
            if (control.supportsMode()) controlData.putInt("mode", control.mode());
            if (control.supportsProgress()) {
                controlData.putLong("progress", control.progress());
                controlData.putLong("maximum", control.progressMax());
            }
        }
        tag.put("gt.extender.control", controlData);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains("gt.extender.control")) return;
        var controlData = accessor.getServerData().getCompound("gt.extender.control");
        if (!controlData.getBoolean("available")) {
            JadeText.add(tooltip, Component.translatable("message.gt6ce.extender.unavailable"));
            return;
        }
        JadeText.add(
                tooltip,
                Component.translatable(
                        "message.gt6ce.extender." + (controlData.getBoolean("enabled") ? "enabled" : "disabled")));
        JadeText.add(
                tooltip,
                Component.translatable("jade.gt6ce.extender."
                        + (controlData.getBoolean("active")
                                ? "active"
                                : controlData.getBoolean("running") ? "running" : "idle")));
        if (controlData.contains("mode"))
            JadeText.add(tooltip, Component.translatable("message.gt6ce.extender.mode", controlData.getInt("mode")));
        if (controlData.contains("maximum") && controlData.getLong("maximum") > 0)
            JadeText.add(
                    tooltip,
                    Component.translatable(
                            "jade.gt6ce.extender.progress",
                            controlData.getLong("progress"),
                            controlData.getLong("maximum")));
    }
}
