package gregtech.integration.jade;
import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
/** Steam tank and real rated ranges; spent input energy is not displayed as a stored RU buffer. */
public enum TurbineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;
    @Override public ResourceLocation getUid() { return ResourceLocation.fromNamespaceAndPath("gt6ce", "steam_turbine"); }
    @Override public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof EnergyNodeBlockEntity turbine) || !turbine.isSteamConverter()) return;
        tag.putLong("gt.turbine.steam", turbine.turbineSteamAmount());
        tag.putLong("gt.turbine.capacity", turbine.turbineSteamCapacity());
        tag.putLong("gt.turbine.input", turbine.spec().inputRate());
        tag.putLong("gt.turbine.output", turbine.spec().outputRate());
        tag.putBoolean("gt.turbine.stopped", turbine.converterStopped());
        tag.putBoolean("gt.turbine.emitted", turbine.converterEmitted());
    }
    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (!data.contains("gt.turbine.input")) return;
        long input = data.getLong("gt.turbine.input"), output = data.getLong("gt.turbine.output");
        long amount = data.getLong("gt.turbine.steam"), capacity = data.getLong("gt.turbine.capacity");
        int percent = capacity == 0 ? 0 : (int)(amount * 100 / capacity);
        tooltip.add(Component.translatable("jade.gt6ce.boiler.steam", amount, capacity, percent));
        tooltip.add(Component.translatable("gt.lang.energy.input").append(": " + (input <= 16 ? 1 : input / 2) + "–" + input * 2 + " L/t"));
        tooltip.add(Component.translatable("gt.lang.energy.output").append(": " + output / 2 + "–" + output * 2 + " RU/t"));
        tooltip.add(data.getBoolean("gt.turbine.stopped") ? Component.translatable("jade.gt6ce.engine.status.off")
                : data.getBoolean("gt.turbine.emitted") ? Component.translatable("jade.gt6ce.engine.status.running", percent)
                : Component.translatable("jade.gt6ce.engine.status.preheating", percent));
    }
}
