package gregtech.content.cover;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Covers drop through their host's removal path, separately from the host item. */
public final class CoverDrops {
    private record Collector(ServerPlayer player, ServerLevel level, BlockPos pos) {
        private static final Collector NONE = new Collector(null, null, null);
    }

    private static final ThreadLocal<Deque<Collector>> COLLECTORS = ThreadLocal.withInitial(ArrayDeque::new);

    private CoverDrops() {}

    /** Pushes a balanced breaker-collection scope around block removal; unmatched breaks pass {@code null}. */
    public static void pushCollector(ServerPlayer player, ServerLevel level, BlockPos pos) {
        COLLECTORS
                .get()
                .push(
                        player != null && level != null && pos != null
                                ? new Collector(player, level, pos.immutable())
                                : Collector.NONE);
    }

    public static void popCollector() {
        var stack = COLLECTORS.get();
        if (!stack.isEmpty()) stack.pop();
        if (stack.isEmpty()) COLLECTORS.remove();
    }

    /** Drops a detached cover, inserting into the wrench breaker's inventory first when active. */
    public static void dropCover(BlockEntity owner, ItemStack cover) {
        if (cover == null || cover.isEmpty() || owner == null) return;
        var level = owner.getLevel();
        if (level == null || level.isClientSide) return;
        var collector = COLLECTORS.get().peek();
        if (collector != null
                && collector != Collector.NONE
                && collector.level() == level
                && collector.pos().equals(owner.getBlockPos())
                && collector.player().isAlive()
                && collector.player().getInventory().add(cover)) {
            level.playSound(
                    null,
                    owner.getBlockPos(),
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS,
                    0.2F,
                    (collector.player().getRandom().nextFloat()
                                            - collector.player().getRandom().nextFloat())
                                    * 0.7F
                            + 1.0F);
            return;
        }
        if (!cover.isEmpty()) Block.popResource(level, owner.getBlockPos(), cover);
    }

    public static void saveRuntime(BlockEntity owner, CompoundTag tag) {
        if (!ComponentCoverFallback.ownClass(owner) || !(owner instanceof PanelCoverHost host)) return;
        if (host.hasAttachedCovers())
            tag.putBoolean("gt.cover.stopped", host.panels().stopped());
    }

    public static void loadRuntime(BlockEntity owner, CompoundTag tag) {
        if (ComponentCoverFallback.ownClass(owner)
                && owner instanceof PanelCoverHost host
                && tag.contains("gt.cover.stopped")) host.panels().restoreStopped(tag.getBoolean("gt.cover.stopped"));
    }

    private static boolean coverKey(String key) {
        return key.startsWith("gt_cover_")
                || key.startsWith("gt.battery_cover_")
                || key.startsWith("gt.logistics.cover.")
                || key.equals("gt.component_covers")
                || key.equals("gt.cover.stopped");
    }

    public static CompoundTag withoutCovers(CompoundTag tag) {
        for (String key : new ArrayList<>(tag.getAllKeys())) if (coverKey(key)) tag.remove(key);
        return tag;
    }

    /**
     * Machine items keep only block configuration that placement restores, never runtime contents.
     * Add a key here only when the block's own drop path writes it and its load path reads it back.
     */
    private static final java.util.Set<String> MACHINE_ITEM_KEYS = java.util.Set.of("gt.mold.shape");

    private static void keepMachineItemData(CompoundTag tag) {
        for (String key : new ArrayList<>(tag.getAllKeys()))
            if (!MACHINE_ITEM_KEYS.contains(key) && !key.equals("id")) tag.remove(key);
    }

    /** Called on block replacement, not chunk unload; clearing slots makes native drop paths idempotent. */
    public static void dropAll(BlockEntity owner) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide) return;
        if (owner instanceof PanelCoverHost host)
            for (var side : net.minecraft.core.Direction.values()) {
                var cover = host.removeCover(side);
                if (!cover.isEmpty()) dropCover(owner, cover);
            }
        if (owner instanceof gregtech.content.logistics.LogisticsCoverHost host)
            host.logisticsCovers().dropAll();
    }

    public static List<ItemStack> capture(
            BlockState state, ServerLevel level, BlockEntity owner, List<ItemStack> drops) {
        if (owner == null
                || !net.minecraft.core.registries.BuiltInRegistries.BLOCK
                        .getKey(state.getBlock())
                        .getNamespace()
                        .equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) return drops;
        boolean machine = owner instanceof gregtech.content.blockentity.machine.HopperBlockEntity
                || owner instanceof gregtech.content.blockentity.tool.ProcessingToolBlockEntity
                || owner instanceof gregtech.content.blockentity.GTEnergyBlockEntity
                        && !(owner instanceof gregtech.content.blockentity.machine.TankBlockEntity)
                        && !(owner instanceof gregtech.content.blockentity.machine.LogisticsTankBlockEntity)
                        && !(owner instanceof gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity);
        for (var stack : drops) {
            if (!stack.is(state.getBlock().asItem())) continue;
            var tag = gregtech.engine.stack.BlockEntityData.read(stack);
            if (tag == null) continue;
            // Preserve intentional contents of portable tanks, but never duplicate their covers.
            withoutCovers(tag);
            // Machines drop empty shells; only their portable configuration survives.
            if (machine) keepMachineItemData(tag);
            boolean empty = tag.isEmpty() || tag.size() == 1 && tag.contains("id");
            if (empty) gregtech.engine.stack.BlockEntityData.remove(stack);
            else gregtech.engine.stack.BlockEntityData.write(stack, tag);
            // Engraving is part of the mold item; heat, contents and attached covers are not.
            if (owner instanceof gregtech.content.blockentity.machine.MoldBlockEntity mold)
                gregtech.content.block.machine.MoldItemData.withShape(stack, mold.getMoldShape());
        }
        return drops;
    }
}
