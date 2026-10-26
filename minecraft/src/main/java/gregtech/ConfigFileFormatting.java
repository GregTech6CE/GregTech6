package gregtech;

import java.nio.file.Files;
//? if >=1.21.1 {
/*import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
*///?} else {
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?}
public final class ConfigFileFormatting {
    private ConfigFileFormatting() {}

    @SubscribeEvent public static void loaded(ModConfigEvent.Loading event) { format(event); }
    @SubscribeEvent public static void reloaded(ModConfigEvent.Reloading event) { format(event); }

    private static void format(ModConfigEvent event) {
        if (!event.getConfig().getModId().equals("gt6ce")) return;
        var path = event.getConfig().getFullPath();
        try {
            if (!Files.isRegularFile(path)) return;
            String before = Files.readString(path);
            String after = gregtech.util.ConfigSpacing.format(before);
            if (!before.equals(after)) Files.writeString(path, after);
        } catch (java.io.IOException failure) {
            com.mojang.logging.LogUtils.getLogger().warn("Cannot format configuration {}", path, failure);
        }
    }
}
