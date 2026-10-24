package gregtech.content.nuclear;

//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.blockentity.energy.ReactorCoreBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}
import java.util.*;

/** Two-phase world-end exchange: every emission reads last cycle, independent of BE tick order. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID)
//?}
public final class ReactorNetwork {

    /**
     * Cores enqueued during the current level tick, drained by {@link #end} at the end of that tick.
     *
     * <p>The normal handler removes the entry before exchange and reaction. Weak keys alone do not
     * guarantee cleanup if a level stops ticking before that drain: each value strongly holds cores,
     * whose level field can refer back to the key. This class currently has no unload/stop cleanup
     * hook, so an entry can remain if the same level never receives another end-of-tick event.</p>
     */
    private static final Map<Level, Set<ReactorCoreBlockEntity>> PENDING = new WeakHashMap<>();

    private ReactorNetwork() {
    }

    public static void enqueue(ReactorCoreBlockEntity core) {
        if (core.getLevel() != null && !core.getLevel().isClientSide) PENDING.computeIfAbsent(core.getLevel(), levelKey -> new HashSet<>()).add(core);
    }
//? if >=1.21.1 {
    /*@SubscribeEvent
    public static void end(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide) return;
        finishTick(event.getLevel());
    }
*///?} else {
    @SubscribeEvent
    public static void end(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        finishTick(event.level);
    }
//?}
    private static void finishTick(Level level) {
        var pending = PENDING.remove(level);
        if (pending == null) return;
        var cores = pending.stream().filter(coreEntry -> !coreEntry.isRemoved()
                && level.hasChunkAt(coreEntry.getBlockPos())).sorted(Comparator.comparingLong(
                coreEntry -> coreEntry.getBlockPos().asLong())).toList();
        if (level.getGameTime() % 20 == 19) exchange(cores);

        for (var core : cores) core.react();

    }

    /** Only the supplied, ticking cores participate; never loads a neighboring chunk. */
    public static void exchange(List<ReactorCoreBlockEntity> cores) {
        Map<BlockPos, ReactorCoreBlockEntity> positions = new HashMap<>();
        Map<ReactorCoreBlockEntity, int[]> incoming = new IdentityHashMap<>();
        Map<ReactorCoreBlockEntity, int[]> emissions = new IdentityHashMap<>();
        Map<ReactorCoreBlockEntity, boolean[]> moderated = new IdentityHashMap<>();
        for (var coreEntry : cores) {
            positions.put(coreEntry.getBlockPos(), coreEntry);
            int[] neutronCounts = new int[coreEntry.rods.length], neutronEmissions = new int[coreEntry.rods.length];
            boolean[] rodModeration = new boolean[coreEntry.rods.length];
            for (int rodSlot = 0; rodSlot < neutronCounts.length; rodSlot++) if (coreEntry.slotActive(rodSlot)) {
                neutronCounts[rodSlot] = RodPhysics.self(coreEntry.rods[rodSlot], coreEntry.coolant());
                neutronEmissions[rodSlot] = RodPhysics.emission(coreEntry.rods[rodSlot], coreEntry.neutrons[rodSlot], coreEntry.coolant());
                rodModeration[rodSlot] = RodPhysics.moderated(coreEntry.rods[rodSlot]);
            }
            incoming.put(coreEntry, neutronCounts);
            emissions.put(coreEntry, neutronEmissions);
            moderated.put(coreEntry, rodModeration);
        }
        for (var coreEntry : cores) for (int rodSlot = 0; rodSlot < coreEntry.rods.length; rodSlot++) if (coreEntry.slotActive(rodSlot)) {
            int count = emissions.get(coreEntry)[rodSlot];
            boolean isModerated = moderated.get(coreEntry)[rodSlot];
            if (count == 0 && !isModerated) continue;
            if (coreEntry.rods.length == 1) {
                int half = (int) RodPhysics.ceil(count, 2);
                for (var side : Direction.Plane.HORIZONTAL) {
                    var next = positions.get(coreEntry.getBlockPos().relative(side));
                    if (next == null) continue;
                    for (int edge = 0; edge < 2; edge++) send(coreEntry, rodSlot, next, edgeSlot(side.getOpposite(), edge, next.rods.length), half,
                            isModerated, incoming);
                }
            } else {
                int localX = rodSlot / 2, localZ = rodSlot % 2;
                for (var side : Direction.Plane.HORIZONTAL) {
                    int neighborX = localX + side.getStepX(), neighborZ = localZ + side.getStepZ();
                    if (neighborX >= 0 && neighborX < 2 && neighborZ >= 0 && neighborZ < 2) send(coreEntry, rodSlot, coreEntry, neighborX * 2 + neighborZ,
                            count, isModerated, incoming);
                    else {
                        var next = positions.get(coreEntry.getBlockPos().relative(side));
                        if (next != null) send(coreEntry, rodSlot, next, edgeSlot(side.getOpposite(),
                                side.getAxis() == Direction.Axis.X ? localZ : localX, next.rods.length), count, isModerated, incoming);
                    }
                }
            }
        }
        for (var coreEntry : cores) {
            System.arraycopy(incoming.get(coreEntry), 0, coreEntry.neutrons, 0, coreEntry.neutrons.length);
            for (var rod : coreEntry.rods) RodPhysics.finishCycle(rod);
            coreEntry.setChanged();
        }
    }

    private static int edgeSlot(Direction side, int edge, int slots) {
        if (slots == 1) return 0;
        return switch (side) {
            case NORTH -> edge * 2;
            case SOUTH -> edge * 2 + 1;
            case WEST -> edge;
            case EAST -> 2 + edge;
            default -> throw new IllegalArgumentException();
        };
    }

    private static void send(ReactorCoreBlockEntity source, int slot, ReactorCoreBlockEntity target, int targetSlot, int amount, boolean moderated,
            Map<ReactorCoreBlockEntity, int[]> incoming) {
        if (!target.slotActive(targetSlot)) return;
        var result = RodPhysics.receive(target.rods[targetSlot], amount, moderated);
        incoming.get(target)[targetSlot] = RodPhysics.bound((long) incoming.get(target)[targetSlot] + result.absorbed());
        incoming.get(source)[slot] = RodPhysics.bound((long) incoming.get(source)[slot] + result.reflected());
        if (result.moderates()) RodPhysics.moderate(source.rods[slot]);
    }
}
