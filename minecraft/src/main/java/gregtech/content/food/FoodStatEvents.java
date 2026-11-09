package gregtech.content.food;

import gregtech.content.nuclear.PlayerRadiation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/**
 * GT6 {@code GT_API_Proxy.onItemUseFinish}'s food branch, reduced to the one statistic the port keeps:
 * radiation. Eating a food whose item has a radiation value routes it into {@link PlayerRadiation}.
 *
 * <p>GT6 subscribes with {@code EventPriority.LOWEST} to the 1.7.10 {@code PlayerUseItemEvent.Finish}; in
 * 1.20.1 that event is {@link LivingEntityUseItemEvent.Finish} (the same fire point: an item finished being
 * used, which is what eating a piece of food does), and the priority is kept so another mod can still
 * replace the item before the radiation is read.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class FoodStatEvents {
    private FoodStatEvents() {}

    /** Applies GT6's FoodStat radiation value when a food item finishes ({@code GT_API_Proxy}). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem();
        Integer radiation = GTFoodStats.radiation(stack);
        if (radiation == null || radiation == 0) {
            return;
        }
        // GT6 routes this into the tracker's sixth value (mRadiation); the port's radiation tracker is
        // PlayerRadiation, whose persistent key is the same kind of 7-bit dose.
        PlayerRadiation.change(player, radiation);
    }
}
