/* Gregorius Techneticies / GregTech-6 Team source rules, LGPL-3.0-or-later. */
package gregtech.event;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class OriginalItemFuelEvents {
    private OriginalItemFuelEvents() {}
    @SubscribeEvent
    public static void fuel(FurnaceFuelBurnTimeEvent event) {
        Integer time = gregtech.engine.machine.FurnaceFuelValue.originalBurnTime(event.getItemStack());
        if (time != null) event.setBurnTime(Math.max(time, event.getBurnTime()));
    }
}
