package gregtech.compat.jade;

import gregtech.content.block.misc.DungeonPortalBlock;
import gregtech.content.block.misc.DungeonPortalBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** GT6 only reports a mini portal's link through the magnifying glass; Jade shows the same state.
 * Like the source's {@code mTarget}, a counterpart in an unloaded chunk reads as not connected. */
public enum PortalJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "portal");
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof DungeonPortalBlockEntity portal)) return;
        tag.putBoolean("gt_portal_active", portal.isActive());
        tag.putBoolean("gt_portal_supported", portal.getBlockState().getBlock() instanceof DungeonPortalBlock block
                && portal.getLevel() != null && block.targetLevel(portal.getLevel()) != null);
        var remote = portal.linkedPortal();
        if (remote == null || remote.getLevel() == null) return;
        tag.putString("gt_portal_dimension", remote.getLevel().dimension().location().toString());
        tag.putInt("gt_portal_x", remote.getBlockPos().getX());
        tag.putInt("gt_portal_y", remote.getBlockPos().getY());
        tag.putInt("gt_portal_z", remote.getBlockPos().getZ());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (!data.contains("gt_portal_active")) return;
        boolean active = data.getBoolean("gt_portal_active");
        tooltip.add(Component.translatable(active ? "jade.gt6ce.portal.active" : "jade.gt6ce.portal.inactive"));
        if (!data.getBoolean("gt_portal_supported")) {
            tooltip.add(Component.translatable("jade.gt6ce.portal.wrong_dimension"));
            return;
        }
        if (!active) return;
        if (data.contains("gt_portal_x")) {
            tooltip.add(Component.translatable("jade.gt6ce.portal.linked",
                    DungeonPortalBlockEntity.dimensionName(data.getString("gt_portal_dimension")),
                    data.getInt("gt_portal_x"), data.getInt("gt_portal_y"), data.getInt("gt_portal_z")));
        } else {
            tooltip.add(Component.translatable("jade.gt6ce.portal.unlinked"));
        }
    }

    @Override
    public int getDefaultPriority() {
        return 1001;
    }
}
