package gregtech.content.worldgen.center;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/** New-world spawn agrees with the original Nexus instead of vanilla's distant terrain search. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID)
//?}
public final class OriginSpawnEvents {

    @SubscribeEvent
    public static void createSpawn(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            level.setDefaultSpawnPos(new BlockPos(0, level.getSeaLevel() + 8, 0), 0);
            event.setCanceled(true);
        }
    }
}
