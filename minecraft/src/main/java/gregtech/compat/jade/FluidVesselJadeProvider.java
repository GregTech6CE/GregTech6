package gregtech.compat.jade;

import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import gregtech.content.blockentity.machine.TankBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;
//? if >=1.21.1 {
/*import static snownee.jade.api.JadeIds.UNIVERSAL_FLUID_STORAGE;
*///?} else {
import static snownee.jade.api.Identifiers.UNIVERSAL_FLUID_STORAGE;
//?}

/** Separate pipe fluid bars and barrel modes; no full save data or inventory mutation. */
public enum FluidVesselJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override public ResourceLocation getUid() { return ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_vessel"); }

    @Override public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        var data = new CompoundTag();
        if (accessor.getBlockEntity() instanceof TankBlockEntity tank) {
            data.putString("kind", "tank");
            var stored = tank.getFluidTank();
            data.put("fluid", FluidView.writeDefault(JadeFluidObject.of(stored.getFluidLong().getFluid(),
                    stored.getAmount()), stored.capacity()));
            data.putBoolean("auto", tank.isAutoOutput());
            data.putBoolean("sealed", tank.getSoftHammerState());
            data.putLong("time", tank.getSealedTime());
            data.putLong("duration", tank.getMaxSealedTime());
        } else if (accessor.getBlockEntity() instanceof FluidPipeBlockEntity pipe) {
            data.putString("kind", "pipe");
            var contents = new ListTag();
            for (int i = 0; i < Math.min(4, pipe.getTanks()); i++) {
                var fluid = pipe.getFluidInTank(i);
                contents.add(FluidView.writeDefault(JadeFluidObject.of(fluid.getFluid(), fluid.getAmount()),
                        pipe.getTankCapacity(i)));
            }
            data.put("contents", contents);
        } else return;
        tag.put("gt_vessel", data);
    }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains("gt_vessel")) return;
        var data = accessor.getServerData().getCompound("gt_vessel");
        if (data.getString("kind").equals("tank")) {
            tooltip.remove(UNIVERSAL_FLUID_STORAGE);
            var view = JadeBars.readFluid(data.getCompound("fluid"));
            if (view != null) tooltip.add(JadeBars.fluid(view, view.overrideText != null ? view.overrideText
                    : Component.translatable("jade.fluid", view.fluidName, view.current)));
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.vessel.auto", Component.translatable(
                    "jade.gt6ce.vessel." + (data.getBoolean("auto") ? "enabled" : "disabled"))));
            JadeText.add(tooltip, Component.translatable("jade.gt6ce.vessel." + (data.getBoolean("sealed") ? "sealed" : "unsealed")));
            if (data.getBoolean("sealed") && data.getLong("duration") > 0) {
                tooltip.add(JadeBars.time(data.getLong("time"), data.getLong("duration"), 1));
            }
        } else {
            // Replace Jade's combined storage view so each physical tank has just one bar.
            tooltip.remove(UNIVERSAL_FLUID_STORAGE);
            var contents = data.getList("contents", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(4, contents.size()); i++) {
                var view = JadeBars.readFluid(contents.getCompound(i));
                if (view == null) continue;
                Component label = view.overrideText != null ? view.overrideText
                        : Component.translatable("jade.fluid", view.fluidName, view.current);
                tooltip.add(JadeBars.fluid(view, label));
            }
        }
    }

    @Override public int getDefaultPriority() { return 1001; }
}
