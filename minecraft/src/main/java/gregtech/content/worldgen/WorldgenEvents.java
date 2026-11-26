package gregtech.content.worldgen;

import static net.minecraft.commands.Commands.literal;

import gregtech.engine.mod.GregTechIdentity;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
*///?} else {
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
//?}

@EventBusSubscriber(modid = GregTechIdentity.MOD_ID)
public final class WorldgenEvents {
    private WorldgenEvents() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("gt6ce")
                .requires(source -> source.hasPermission(2))
                .then(literal("worldgen").then(WorldgenLocate.command("locate"))));
        event.getDispatcher().register(literal("locate")
                .requires(source -> source.hasPermission(2))
                .then(WorldgenLocate.command("gt6ce")));
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        VeinTerrain.clear();
    }
}
