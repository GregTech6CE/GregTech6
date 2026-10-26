package gregtech.engine.sensor;

import gregtech.content.blockentity.sensor.SensorBlockEntity;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/** Only sensors that actually ticked are sampled; never wakes unloaded chunks. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class SensorSampling {
    private static final java.util.Map<Level, java.util.Set<SensorBlockEntity>> PENDING = new java.util.IdentityHashMap<>();
    private SensorSampling() {}

    public static void enqueue(SensorBlockEntity sensor) {
        var level = sensor.getLevel();
        if (level != null && !level.isClientSide)
            PENDING.computeIfAbsent(level, key -> new java.util.LinkedHashSet<>()).add(sensor);
    }

//? if >=1.21.1 {
    /*@SubscribeEvent(priority = EventPriority.LOWEST)
    public static void end(LevelTickEvent.Post event) {
        finish(event.getLevel());
    }
*///?} else {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void end(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END) finish(event.level);
    }
//?}
    private static void finish(Level level) {
        if (level.isClientSide) return;
        var sensors = PENDING.remove(level);
        if (sensors == null) return;
        for (var sensor : sensors) {
            var pos = sensor.getBlockPos();
            if (!sensor.isRemoved() && level.hasChunkAt(pos) && level.getBlockEntity(pos) == sensor)
                SensorBlockEntity.measure(level, pos, sensor.getBlockState(), sensor);
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) PENDING.remove(event.getLevel());
    }
}
