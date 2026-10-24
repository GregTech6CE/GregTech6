/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntityPipeFluid.onTick2/onServerTickPre and WD.even. */
package gregtech.content.transport.fluid;

import gregtech.blockentity.machine.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
@EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID)
*///?} else {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
@Mod.EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID)
//?}
public final class FluidPipeTickPhases {
    private static final Map<Level, Set<FluidPipeBlockEntity>> PENDING = new WeakHashMap<>();
    private FluidPipeTickPhases() {}

    /** Native ticking enrolls only currently ticking pipes, without retaining unloaded chunks. */
    public static void enqueue(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                               FluidPipeBlockEntity pipe) {
        if (!level.isClientSide) PENDING.computeIfAbsent(level, key -> new LinkedHashSet<>()).add(pipe);
    }

//? if >=1.21.1 {
    /*@SubscribeEvent public static void start(LevelTickEvent.Pre event) { tick(event.getLevel()); }
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) { PENDING.remove(event.getLevel()); }
*///?} else {
    @SubscribeEvent public static void start(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) tick(event.level);
    }
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event) { PENDING.remove(event.getLevel()); }
//?}

    public static void tick(Level level) {
        if (level.isClientSide) return;
        var queued = PENDING.remove(level);
        if (queued == null) return;
        // Every adjacent pair has opposite parity. A rotation cannot reverse their tick ordering.
        for (int phase = 0; phase < 2; phase++) for (var pipe : queued) {
            BlockPos pos = pipe.getBlockPos();
            int parity = (pos.getX() ^ pos.getY() ^ pos.getZ()) & 1;
            // Original WD.even counts even coordinates: its first phase is odd sum parity.
            if (parity != 1 - phase || pipe.isRemoved() || !level.hasChunkAt(pos)
                    || level.getBlockEntity(pos) != pipe) continue;
            FluidPipeBlockEntity.serverTick(level, pos, pipe.getBlockState(), pipe);
        }
    }
}
