package gregtech.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
//? if >=1.21.1 {
/*import static snownee.jade.api.JadeIds.UNIVERSAL_ITEM_STORAGE;
import static snownee.jade.api.JadeIds.UNIVERSAL_FLUID_STORAGE;
*///?} else {
import static snownee.jade.api.Identifiers.UNIVERSAL_ITEM_STORAGE;
import static snownee.jade.api.Identifiers.UNIVERSAL_FLUID_STORAGE;
//?}

/** Complements the existing detailed providers with the remaining GT6 OmniOcular observations. */
public enum OmniJadeProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    INSTANCE;

    @Override public ResourceLocation getUid() { return ResourceLocation.fromNamespaceAndPath("gt6ce", "omni_details"); }

    @Override public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        var entity = accessor.getBlockEntity();
        if (entity == null) return;
        var info = new JadeInspection(accessor);
        OmniTransportInfo.append(info, entity, accessor);
        OmniSensorInfo.append(info, entity);
        // Reading an already-bound controller never scans or rebuilds the structure.
        if (entity instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity port)
            entity = port.sensorTarget();
        // Recipe machines and their bound ports are exclusively handled by BasicMachineJadeProvider.
        OmniMachineInfo.append(info, entity, accessor);
        OmniWorldInfo.append(info, entity);
        OmniEnergyInfo.append(info, entity);
        info.write(tag);
    }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getServerData().getBoolean("gt_omni_inventory")) {
            tooltip.remove(UNIVERSAL_ITEM_STORAGE);
            tooltip.remove(UNIVERSAL_FLUID_STORAGE);
        }
        JadeInspection.render(tooltip, accessor);
    }

    @Override public int getDefaultPriority() { return 1002; }
}
