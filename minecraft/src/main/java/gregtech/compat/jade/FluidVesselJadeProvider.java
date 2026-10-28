package gregtech.compat.jade;

import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import gregtech.content.blockentity.machine.TankBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Omni-style channel inspection and barrel modes; no full save data or inventory mutation. */
public enum FluidVesselJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override public ResourceLocation getUid() { return ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_vessel"); }

    @Override public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        var data = new CompoundTag();
        if (accessor.getBlockEntity() instanceof TankBlockEntity tank) {
            data.putString("kind", "tank");
            data.putBoolean("auto", tank.isAutoOutput());
            data.putBoolean("sealed", tank.getSoftHammerState());
            data.putLong("time", tank.getSealedTime());
            data.putLong("duration", tank.getMaxSealedTime());
            data.putLong("limit", tank.spec().maxTemperature());
            data.putLong("amount", tank.getFluidTank().getAmount());
            data.putLong("capacity", tank.getFluidTank().capacity());
        } else if (accessor.getBlockEntity() instanceof FluidPipeBlockEntity pipe) {
            data.putString("kind", "pipe");
            data.putLong("limit", pipe.spec().maxTemperature());
            data.putInt("channels", pipe.getTanks());
            data.putLong("bandwidth", pipe.spec().bandwidthPerTank());
            data.putLong("capacity", pipe.spec().capacity());
            var contents = new ListTag();
            for (int i = 0; i < Math.min(16, pipe.getTanks()); i++) {
                var fluid = pipe.getFluidInTank(i);
                if (fluid.isEmpty()) continue;
                var row = new CompoundTag();
                row.putInt("channel", i + 1);
//? if >=1.21.1 {
                /*row.put("fluid", fluid.save(accessor.getLevel().registryAccess()));
*///?} else {
                row.put("fluid", fluid.writeToNBT(new CompoundTag()));
//?}
                contents.add(row);
            }
            data.put("contents", contents);
        } else return;
        tag.put("gt_vessel", data);
    }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains("gt_vessel")) return;
        var data = accessor.getServerData().getCompound("gt_vessel");
        if (data.getString("kind").equals("tank")) {
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.vessel.amount", data.getLong("amount"), data.getLong("capacity")));
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.vessel.auto", Component.translatable(
                    "jade.gt6ce.vessel." + (data.getBoolean("auto") ? "enabled" : "disabled"))));
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.vessel." + (data.getBoolean("sealed") ? "sealed" : "unsealed")));
            if (data.getBoolean("sealed") && data.getLong("duration") > 0) {
                tooltip.add(JadeBars.time(data.getLong("time"), data.getLong("duration"), 1));
            }
        } else {
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.vessel.channels", data.getInt("channels"), data.getLong("capacity"), data.getLong("bandwidth")));
            var contents = data.getList("contents", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < contents.size(); i++) {
                var row = contents.getCompound(i);
//? if >=1.21.1 {
                /*var fluid = net.neoforged.neoforge.fluids.FluidStack.parseOptional(accessor.getLevel().registryAccess(), row.getCompound("fluid"));
*///?} else {
                var fluid = net.minecraftforge.fluids.FluidStack.loadFluidStackFromNBT(row.getCompound("fluid"));
//?}
                if (!fluid.isEmpty()) JadeText.add(tooltip, Component.translatable("jade.gt6ce.vessel.channel", row.getInt("channel"),
                        fluid.getDisplayName(), fluid.getAmount(), data.getLong("capacity")));
            }
        }
        JadeText.add(tooltip, Component.translatable("jade.gt6ce.vessel.temperature_limit", data.getLong("limit")));
    }
}
