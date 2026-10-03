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
import net.minecraft.world.entity.Entity;


/** GT6 miniature cross-dimension relay (MultiTileEntityMiniPortal:325-519).
 * Items, fluids, FE and GT packets address the loaded opposite neighbour of the linked
 * portal; six sided redstone/comparator inboxes follow the original tick phases.
 * Legacy key data is retained for saves, but keys do not activate the source machine.
 */
public class DungeonPortalBlockEntity extends com.gregtech.gregtech.blockentity.EnergyRelayBlockEntity
        implements com.gregtech.gregtech.item.behavior.ManualToolBehaviorAccess.Ignitable,
        com.gregtech.gregtech.item.behavior.Extinguishable {

    /** GT6's {@code NBT_KEY} ({@code gregapi/data/CS.java:1169}), legacy key data retained for old saves. */
    public static final String NBT_KEY = GTDungeonKeyItem.NBT_KEY;
    /** GT6's {@code NBT_ACTIVE} ({@code CS.java:1172}). */
    public static final String NBT_ACTIVE = "gt.active";

    /**
     * GT6's {@code sListWorldSide} / {@code sListNetherSide} / {@code sListEndSide}
     * ({@code MultiTileEntityMiniPortalNether:42-47}, {@code MultiTileEntityMiniPortalEnd:42-47}):
     * the active portals of each kind, so a portal can find its counterpart on the other side. GT6
     * keeps live tile entities; the port keeps positions and validates them while scanning, with entries removed on chunk unload as in {@code MultiTileEntityMiniPortal:203-207}.
     */
    private static final Map<Block, Set<GlobalPos>> ACTIVE = new LinkedHashMap<>();

    private long keyId;
    private boolean active;
    private final com.gregtech.gregtech.content.logistics.MiniPortalSignals signals = new com.gregtech.gregtech.content.logistics.MiniPortalSignals();

    public DungeonPortalBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.DUNGEON_PORTAL.get(), pos, state);
    }

    /** Legacy key id, retained only for save compatibility. */
    public long keyId() {
        return keyId;
    }

    /** Preserves the superseded port's key data without granting key activation. */
    public void setKeyId(long keyId) {
        this.keyId = keyId;
        setChanged();
    }

    public boolean isActive() {
        return active;
    }

    /** Legacy key activation was a port invention; the source uses igniters/Ender Eyes. */
    @Deprecated
    public boolean useKey(long keyId) { return false; }

    /** MultiTileEntityMiniPortalNether:116-128: all GT igniters toggle; extinguishers close. */
    @Override
    public long onIgnite(Level world, BlockPos pos, net.minecraft.core.Direction side,
                         @Nullable net.minecraft.world.entity.player.Player player,
                         net.minecraft.world.item.ItemStack igniter, boolean sneaking,
                         float hitX, float hitY, float hitZ) {
        if (world.isClientSide || !(getBlockState().getBlock() instanceof DungeonPortalBlock block)
                || block.target() != DungeonPortalBlock.Target.NETHER) return 0L;
        if (active) deactivate(); else activate();
        var remote = linkedPortal();
        if (remote != null && player != null) player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "X: " + remote.worldPosition.getX() + "   Y: " + remote.worldPosition.getY()
                        + "   Z: " + remote.worldPosition.getZ()), false);
        return 10000L;
    }
    @Override
    public long onExtinguish(Level world, BlockPos pos, net.minecraft.core.Direction side,
                            @Nullable net.minecraft.world.entity.player.Player player,
                            net.minecraft.world.item.ItemStack can, boolean sneaking,
                            float hitX, float hitY, float hitZ) {
        if (world.isClientSide || !(getBlockState().getBlock() instanceof DungeonPortalBlock block)
                || block.target() != DungeonPortalBlock.Target.NETHER) return 0L;
        deactivate();
        return 10000L;
    }

    /** GT6's {@code setPortalActive} ({@code MultiTileEntityMiniPortal:183}). */
    public boolean activate() {
        if (active || !(getBlockState().getBlock() instanceof DungeonPortalBlock portal)
                || level == null || portal.targetLevel(level) == null) return false;
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
        clearLinkedSignals();
        active = false;
        signals.clear();
        removeFromPortalList();
        if (level != null && !level.isClientSide) {
            level.setBlock(worldPosition, getBlockState().setValue(DungeonPortalBlock.ACTIVE, false), 3);
        }
        setChanged();
        return true;
    }

    private void clearLinkedSignals() {
        var remote = linkedPortal();
        if (remote != null && level instanceof ServerLevel server)
            remote.signals.disconnect(server.getServer().getTickCount());
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

    /** Source MiniPortal.getDelegateTileEntity: the remote opposite neighbour faces this input side. */
    @Override
    protected Target target(net.minecraft.core.Direction side) {
        if (side == null) return null;
        var portal = linkedPortal();
        if (portal == null || portal.level == null) return null;
        return new Target(portal.worldPosition.relative(side.getOpposite()), side, portal.level);
    }
    @Override protected boolean supportsEnergy() { return true; }
    @Override protected boolean supportsItems() { return true; }
    @Override protected boolean supportsFluids() { return true; }

    @Nullable
    public DungeonPortalBlockEntity linkedPortal() {
        if (!active || isRemoved() || !(level instanceof ServerLevel server)
                || !(getBlockState().getBlock() instanceof DungeonPortalBlock block)) return null;
        ServerLevel remote = block.targetLevel(server);
        if (remote == null) return null;
        GlobalPos pos = findTarget(worldPosition, server.dimension() == Level.OVERWORLD, block, remote,
                block.distanceFactor(), block.margin());
        return pos != null && remote.getBlockEntity(pos.pos()) instanceof DungeonPortalBlockEntity portal ? portal : null;
    }

    /** Vanilla queries the opposite of the actual output face, matching source OPOS[query]. */
    public int signal(net.minecraft.core.Direction query) {
        return active ? signals.redstone(query.getOpposite().ordinal()) : 0;
    }
    public int comparator(net.minecraft.core.Direction output) {
        return !active ? 0 : output == null ? signals.maximumComparator() : signals.comparator(output.ordinal());
    }
    private int incoming(net.minecraft.core.Direction side, boolean comparator) {
        BlockPos pos = worldPosition.relative(side);
        if (level == null || !level.hasChunkAt(pos)) return 0;
        var state = level.getBlockState(pos);
        if (comparator) {
            var entity = level.getBlockEntity(pos);
            if (entity instanceof DungeonPortalBlockEntity portal) return portal.comparator(side.getOpposite());
            if (entity instanceof ExtenderBlockEntity extender) return extender.comparator(side.getOpposite());
            if (state.hasAnalogOutputSignal()) return state.getAnalogOutputSignal(level, pos);
        }
        return level.getSignal(pos, side);
    }
    public void tickRelay() {
        if (!(level instanceof ServerLevel server)) return;
        long tick = server.getServer().getTickCount();
        if (!active) signals.clear();
        else {
            signals.advance(tick);
            var remote = linkedPortal();
            if (remote != null) for (var side : net.minecraft.core.Direction.values())
                remote.signals.receive(tick, side.getOpposite().ordinal(), incoming(side, false), incoming(side, true));
        }
        if (signals.consumeChanged()) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
    @Override public void onLoad() {
        super.onLoad();
        if (active) addToPortalList();
        if (level != null && !level.isClientSide && getBlockState().getValue(DungeonPortalBlock.ACTIVE) != active)
            level.setBlock(worldPosition, getBlockState().setValue(DungeonPortalBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
    }
    @Override public void onChunkUnloaded() {
        // Source onChunkUnload disables the relay; never load chunks or change remote terrain.
        clearLinkedSignals();
        active = false;
        signals.clear();
        removeFromPortalList();
        setChanged();
        super.onChunkUnloaded();
    }
    /** Kept only for source compatibility with the superseded port API. Mini portals do not move entities. */
    @Deprecated @Nullable
    public static Entity teleport(ServerLevel level, BlockPos pos, Entity entity) { return null; }
}
