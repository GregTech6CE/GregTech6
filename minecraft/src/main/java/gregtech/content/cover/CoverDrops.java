package gregtech.content.cover;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Covers drop through their host's removal path, separately from the host item. */
public final class CoverDrops {
    private CoverDrops() {}

    public static void saveRuntime(BlockEntity owner, CompoundTag tag) {
        if (!ComponentCoverFallback.ownClass(owner) || !(owner instanceof PanelCoverHost host)) return;
        if (host.hasAttachedCovers()) tag.putBoolean("gt.cover.stopped", host.panels().stopped());
    }

    public static void loadRuntime(BlockEntity owner, CompoundTag tag) {
        if (ComponentCoverFallback.ownClass(owner) && owner instanceof PanelCoverHost host
                && tag.contains("gt.cover.stopped")) host.panels().restoreStopped(tag.getBoolean("gt.cover.stopped"));
    }

    private static boolean coverKey(String key) {
        return key.startsWith("gt_cover_") || key.startsWith("gt.battery_cover_")
                || key.startsWith("gt.logistics.cover.") || key.equals("gt.component_covers")
                || key.equals("gt.cover.stopped");
    }

    public static CompoundTag withoutCovers(CompoundTag tag) {
        for (String key : new ArrayList<>(tag.getAllKeys())) if (coverKey(key)) tag.remove(key);
        return tag;
    }

    /** Called on block replacement, not chunk unload; clearing slots makes native drop paths idempotent. */
    public static void dropAll(BlockEntity owner) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide) return;
        if (owner instanceof PanelCoverHost host) for (var side : net.minecraft.core.Direction.values()) {
            var cover = host.removeCover(side);
            if (!cover.isEmpty()) net.minecraft.world.level.block.Block.popResource(owner.getLevel(), owner.getBlockPos(), cover);
        }
        if (owner instanceof gregtech.content.logistics.LogisticsCoverHost host) host.logisticsCovers().dropAll();
    }

    public static List<ItemStack> capture(BlockState state, ServerLevel level, BlockEntity owner, List<ItemStack> drops) {
        if (owner == null || !net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("gt6ce")) return drops;
        boolean machine = owner instanceof gregtech.content.blockentity.machine.HopperBlockEntity
                || owner instanceof gregtech.content.blockentity.tool.ProcessingToolBlockEntity
                || owner instanceof gregtech.content.blockentity.GTEnergyBlockEntity
                    && !(owner instanceof gregtech.content.blockentity.machine.TankBlockEntity)
                    && !(owner instanceof gregtech.content.blockentity.machine.LogisticsTankBlockEntity)
                    && !(owner instanceof gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity);
        for (var stack : drops) {
            if (!stack.is(state.getBlock().asItem())) continue;
//? if >=1.21.1 {
            /*var data = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
            if (data == null) continue;
            var tag = data.copyTag();
*///?} else {
            var existing = stack.getTagElement("BlockEntityTag");
            if (existing == null) continue;
            var tag = existing.copy();
//?}
            // Preserve intentional contents of portable tanks, but never duplicate their covers.
            withoutCovers(tag);
            boolean empty = tag.isEmpty() || tag.size() == 1 && tag.contains("id");
//? if >=1.21.1 {
            /*if (machine || empty) stack.remove(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
            else stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag));
*///?} else {
            if (machine || empty) stack.removeTagKey("BlockEntityTag");
            else stack.addTagElement("BlockEntityTag", tag);
//?}
        }
        return drops;
    }
}
