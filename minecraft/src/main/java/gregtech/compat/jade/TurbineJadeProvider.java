package gregtech.compat.jade;

import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Steam tank and real rated ranges; spent input energy is not displayed as a stored RU buffer. */
public enum TurbineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "steam_turbine");
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof EnergyNodeBlockEntity node)) return;
        var steam = node.steamStatus();
        if (steam == null) return;
        tag.putLong("gt.turbine.steam", steam.amount());
        tag.putLong("gt.turbine.capacity", steam.capacity());
        tag.putLong("gt.turbine.input", node.spec().inputRate());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (!data.contains("gt.turbine.input")) return;
        long amount = data.getLong("gt.turbine.steam"), capacity = data.getLong("gt.turbine.capacity");
        int percent = capacity == 0 ? 0 : (int) (amount * 100 / capacity);
        JadeText.add(
                tooltip, gregtech.engine.fluid.FluidUnits.text("jade.gt6ce.boiler.steam", amount, capacity, percent));
    }
}
