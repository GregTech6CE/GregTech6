package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.item.GTDungeonKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;


/**
 * Port of GT6's miniature portals as far as the dungeon needs them
 * ({@code gregtech/tileentity/portals/MultiTileEntityMiniPortal.java} and its two vanilla-dimension
 * subclasses {@code MultiTileEntityMiniPortalNether.java} / {@code MultiTileEntityMiniPortalEnd.java}):
 * the block that remembers which dungeon key opens it, which dimension it leads to, and that moves an
 * entity standing inside it across.
 *
 * <p>GT6's portal is a multi-tile with its own networking, rendering and relay system
 * ({@code MultiTileEntityMiniPortal} alone is 520 lines: 13 render passes, item/fluid/redstone/energy
 * relays into the target portal, a comparator, an inventory delegate). This port keeps only what the
 * dungeon rooms need:</p>
 * <ul>
 *   <li>the key id GT6 stores under {@code NBT_KEY} ({@code MultiTileEntitySafeKeyLocked:56}) and the
 *       active flag of {@code NBT_ACTIVE} ({@code MultiTileEntityMiniPortal:73-79}),</li>
 *   <li>GT6's key rule ({@code Behavior_Key:44-64} with
 *       {@code MultiTileEntitySafeKeyLocked.useKey:79-91}): an unkeyed portal adopts the id of the key
 *       used on it, a keyed one only opens for a key with the same id,</li>
 *   <li>GT6's igniter toggle ({@code MultiTileEntityMiniPortalNether:116-128}, {@code TOOL_igniter}),</li>
 *   <li>GT6's portal lists and target search ({@code MultiTileEntityMiniPortalNether:42-113}): the
 *       nearest portal of the same kind on the other side, within GT6's margin of error and with
 *       GT6's distance factor,</li>
 *   <li>and a cross dimension move, which GT6's portal does <b>not</b> have: the original relays items,
 *       fluids, redstone and energy through the target portal and never teleports an entity. The port
 *       adds it because a dungeon portal without it would be a decoration - see
 *       {@link #teleport(ServerLevel, BlockPos, Entity)}.</li>
 * </ul>
 *
 * <p>Skipped from GT6 (with the reason): the twelve frame render passes and the portal texture
 * animation of {@code MultiTileEntityMiniPortal:272-323} (the port's block model carries GT6's frame
 * box from {@code sBlockBounds} and the portal plane as static quads), the item/fluid/redstone/energy
 * relaying and the comparator of {@code :325-519} (they need the multi-tile's delegates), and the
 * client side activation sound packet of {@code :244-257}. GT6 disables a portal when its chunk
 * unloads ({@code onChunkUnload:203-207}); the port keeps the active flag, so a portal still works
 * after its chunk is reloaded.</p>
 */
public class DungeonPortalBlockEntity extends BlockEntity {

    /** GT6's {@code NBT_KEY} ({@code gregapi/data/CS.java:1169}), the id the matching key must carry. */
    public static final String NBT_KEY = GTDungeonKeyItem.NBT_KEY;
    /** GT6's {@code NBT_ACTIVE} ({@code CS.java:1172}). */
    public static final String NBT_ACTIVE = "gt.active";

    /**
     * GT6's {@code sListWorldSide} / {@code sListNetherSide} / {@code sListEndSide}
     * ({@code MultiTileEntityMiniPortalNether:42-47}, {@code MultiTileEntityMiniPortalEnd:42-47}):
     * the active portals of each kind, so a portal can find its counterpart on the other side. GT6
     * keeps live tile entities; the port keeps positions and validates them while scanning, which
     * survives a chunk unload ({@code MultiTileEntityMiniPortal:203-207} cleared GT6's lists there).
     */
    private static final Map<Block, Set<GlobalPos>> ACTIVE = new LinkedHashMap<>();

    private long keyId;
    private boolean active;

    public DungeonPortalBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.DUNGEON_PORTAL.get(), pos, state);
    }

    /** The key id this portal accepts, {@code 0} while it has none. */
    public long keyId() {
        return keyId;
    }

    /** GT6's {@code MultiTileEntitySafeKeyLocked:56} read of its {@code mID}. */
    public void setKeyId(long keyId) {
        this.keyId = keyId;
        setChanged();
    }

    public boolean isActive() {
        return active;
    }

    /**
     * GT6's {@code Behavior_Key:50-61} on the lock side, i.e. {@code MultiTileEntitySafeKeyLocked:79-91}:
     * a portal without an id adopts the id of the key that is used on it, and a keyed portal only
     * opens for a key that carries the same id.
     */
    public boolean useKey(long keyId) {
        if (active || keyId == 0) return false;
        if (this.keyId == 0) this.keyId = keyId;
        if (this.keyId != keyId) return false;
        return activate();
    }

    /** GT6's {@code setPortalActive} ({@code MultiTileEntityMiniPortal:183}). */
    public boolean activate() {
        if (active) return false;
        active = true;
        if (level != null && !level.isClientSide) {
            level.setBlock(worldPosition, getBlockState().setValue(DungeonPortalBlock.ACTIVE, true), 3);
            addToPortalList();
        }
        setChanged();
        return true;
    }

    /** GT6's {@code setPortalInactive} ({@code MultiTileEntityMiniPortal:184}). */
    public boolean deactivate() {
        if (!active) return false;
        active = false;
        removeFromPortalList();
        if (level != null && !level.isClientSide) {
            level.setBlock(worldPosition, getBlockState().setValue(DungeonPortalBlock.ACTIVE, false), 3);
        }
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putLong(NBT_KEY, keyId);
        tag.putBoolean(NBT_ACTIVE, active);
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        keyId = tag.getLong(NBT_KEY);
        active = tag.getBoolean(NBT_ACTIVE);
        if (active) addToPortalList();
    }

    @Override
    public void setRemoved() {
        removeFromPortalList();
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (active) addToPortalList();
    }

    private void addToPortalList() {
        if (level instanceof ServerLevel server && active) {
            ACTIVE.computeIfAbsent(getBlockState().getBlock(), block -> new LinkedHashSet<>())
                    .add(GlobalPos.of(server.dimension(), worldPosition.immutable()));
        }
    }

    private void removeFromPortalList() {
        if (level instanceof ServerLevel server) {
            Set<GlobalPos> portals = ACTIVE.get(getBlockState().getBlock());
            if (portals != null) portals.remove(GlobalPos.of(server.dimension(), worldPosition));
        }
    }

    /**
     * GT6's {@code findTargetPortal} ({@code MultiTileEntityMiniPortalNether:67-96},
     * {@code MultiTileEntityMiniPortalEnd:63-92}): the closest active portal of the same kind on the
     * other side, measured with GT6's distance factor ({@code xCoord - tTarget.xCoord * 8} from the
     * overworld and the other way round from the Nether, the same with 128 for the End) and limited by
     * GT6's margin of error (128 metres for the Nether, 512 for the End). On a tie GT6 keeps the one
     * with the smaller height difference, which is kept here too.
     *
     * <p>Entries whose block or block entity is gone are dropped while scanning; an entry in a chunk
     * that is not loaded is skipped, because its block cannot be checked (GT6 lost it entirely when the
     * chunk unloaded).</p>
     */
    @Nullable
    private static GlobalPos findTarget(BlockPos origin, boolean fromOverworld, Block block, ServerLevel target,
                                        int factor, int margin) {
        Set<GlobalPos> portals = ACTIVE.get(block);
        if (portals == null || portals.isEmpty()) return null;
        GlobalPos best = null;
        long bestDistance = (long) margin * margin;
        for (Iterator<GlobalPos> iterator = portals.iterator(); iterator.hasNext(); ) {
            GlobalPos candidate = iterator.next();
            if (candidate.dimension() != target.dimension()) continue;
            BlockPos pos = candidate.pos();
            if (!target.isLoaded(pos)) continue;
            boolean valid = target.getBlockEntity(pos) instanceof DungeonPortalBlockEntity portal
                    && portal.isActive() && target.getBlockState(pos).is(block);
            if (!valid) {
                iterator.remove();
                continue;
            }
            // GT6's own arithmetic: this tile entity's coordinates against the target's, scaled by the
            // distance factor of whichever side this portal is on.
            long dx = fromOverworld ? origin.getX() - (long) pos.getX() * factor
                    : pos.getX() - (long) origin.getX() * factor;
            long dz = fromOverworld ? origin.getZ() - (long) pos.getZ() * factor
                    : pos.getZ() - (long) origin.getZ() * factor;
            long distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            } else if (distance == bestDistance && (best == null
                    || Math.abs(pos.getY() - origin.getY()) < Math.abs(best.pos().getY() - origin.getY()))) {
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Moves an entity that stands in this portal to the other side, GT6's
     * {@code MultiTileEntityMiniPortalNether:67-96} target search included.
     *
     * <p>GT6's portal never moves an entity: it relays what is piped into it. The port's dungeon portal
     * teleports, because that is the point of a dungeon portal for a player:</p>
     * <ul>
     *   <li>an active portal of the same kind inside GT6's margin of error is the destination (the
     *       entity arrives in the middle of that portal block),</li>
     *   <li>without one the entity arrives at the scaled coordinate (nether: an eighth, End: a
     *       hundred-and-twenty-eighth) on a small obsidian platform, because a dungeon portal that
     *       leads nowhere would be useless. Vanilla builds a whole portal there; the port only lays the
     *       platform, the matching portal has to be built by the player, exactly like GT6's relay needs
     *       a counterpart.</li>
     *   <li>Vanilla's portal cooldown ({@code Entity#setPortalCooldown}) keeps an entity from bouncing
     *       back and forth between an arrival portal and the one it came from, and it is also what makes
     *       a second call in the same movement tick harmless: the moved entity is gone and the new one
     *       carries the cooldown.</li>
     * </ul>
     *
     * <p>{@code DungeonPortalBlock#entityInside} calls this directly, the way vanilla's end portal did
     * before 1.16 deferred its teleport ({@code Entity#setAsInsidePortal}); 1.20.1 has no deferred hook
     * a mod block can use. A moved entity is removed, so the block loop that called it cannot teleport
     * it twice.</p>
     *
     * @return the moved entity (a dimension change replaces it), or {@code null} when nothing happened
     */
    @Nullable
    public static Entity teleport(ServerLevel level, BlockPos pos, Entity entity) {
        if (entity.isRemoved() || entity.isPassenger() || entity.isVehicle()
                || entity.isOnPortalCooldown()) {
            return null;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof DungeonPortalBlock portal)) return null;
        if (!(level.getBlockEntity(pos) instanceof DungeonPortalBlockEntity self) || !self.isActive()) return null;
        ServerLevel target = portal.targetLevel(level);
        if (target == null || !entity.canChangeDimensions(level,target)) return null;

        GlobalPos destination = findTarget(pos, level.dimension() == Level.OVERWORLD, portal, target,
                portal.distanceFactor(), portal.margin());
        Vec3 arrival = destination != null
                ? new Vec3(destination.pos().getX() + 0.5D, destination.pos().getY(), destination.pos().getZ() + 0.5D)
                : arrivalPlatform(level, pos, target, portal);
        Entity moved = entity.changeDimension(new DimensionTransition(target,arrival,Vec3.ZERO,entity.getYRot(),entity.getXRot(),DimensionTransition.DO_NOTHING));
        if (moved != null) {
            moved.setDeltaMovement(Vec3.ZERO);
            moved.setPortalCooldown();
        }
        return moved;
    }

    /** The scaled arrival coordinate (GT6's factor) with a small platform under it. */
    private static Vec3 arrivalPlatform(ServerLevel level, BlockPos pos, ServerLevel target, DungeonPortalBlock portal) {
        int factor = portal.distanceFactor();
        boolean fromOverworld = level.dimension() == Level.OVERWORLD;
        int x = fromOverworld ? Math.floorDiv(pos.getX(), factor) : pos.getX() * factor;
        int z = fromOverworld ? Math.floorDiv(pos.getZ(), factor) : pos.getZ() * factor;
        int y = net.minecraft.util.Mth.clamp(pos.getY(), target.getMinBuildHeight() + 1, target.getMaxBuildHeight() - 2);
        BlockPos centre = new BlockPos(x, y, z);
        target.getChunkAt(centre);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                target.setBlock(centre.offset(dx, -1, dz),
                        net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
        }
        for (int dy = 0; dy <= 1; dy++) {
            target.setBlock(centre.above(dy),
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        return new Vec3(x + 0.5D, y, z + 0.5D);
    }

}
