package gregtech.client;

//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
//?}
public final class ConfiguredToolSounds {
    private ConfiguredToolSounds() {}

    @SubscribeEvent public static void play(PlaySoundEvent event) {
        if (gregtech.GregTechConfig.Rule.TOOL_SOUNDS.enabled() || event.getSound() == null) return;
        var id = event.getSound().getLocation();
        if (id.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE) && java.util.Set.of("wrench", "screwdriver").contains(id.getPath()))
            event.setSound(null);
    }
}
