package gregtech.api.multiblock.tracking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.WeakHashMap;

/** Formed bit carried in the block-entity update packet and nowhere in the saved chunk. */
public final class ClientFormedSync {
    public static final String KEY = "gt.formed_sync";
    private static final Map<BlockEntity, Boolean> CLIENT = new WeakHashMap<>();

    private ClientFormedSync() {}

    public static boolean get(BlockEntity entity) {
        return Boolean.TRUE.equals(CLIENT.get(entity));
    }

    public static void write(BlockEntity entity, CompoundTag tag) {
        if (!(entity instanceof TrackedMultiblock)) return;
        Level level = entity.getLevel();
        if (level != null && level.isClientSide) return;
        StructureTracker tracker = StructureTracking.tracker(entity);
        tag.putBoolean(KEY, tracker != null && tracker.formed());
    }

    public static void read(BlockEntity entity, CompoundTag tag) {
        if (tag != null && tag.contains(KEY)) CLIENT.put(entity, tag.getBoolean(KEY));
    }

    /** Pushes the update packet without marking the chunk for save. */
    public static void send(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null || level.isClientSide) return;
        BlockState state = entity.getBlockState();
        level.sendBlockUpdated(entity.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
    }
}
