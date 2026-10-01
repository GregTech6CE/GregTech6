package com.gregtech.gregtech.content.food;

import com.gregtech.gregtech.content.nuclear.PlayerRadiation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The two entry points GT6 gives its food tracker: {@code GT_API_Proxy.onItemUseFinish} (eating or drinking
 * anything whose item is in {@code FoodsGT}) and the once-per-50-ticks tick in
 * {@code EntityFoodTracker.tick()}.
 *
 * <p>GT6 subscribes with {@code EventPriority.LOWEST} to the 1.7.10 {@code PlayerUseItemEvent.Finish}; in
 * 1.20.1 that event is {@link LivingEntityUseItemEvent.Finish} (the same fire point: an item finished being
 * used, which is what eating a piece of food does), and the priority is kept so another mod can still
 * replace the item before the statistics are read.
 */
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class FoodStatEvents {
    private FoodStatEvents() {}

    /** GT6 {@code GT_API_Proxy:995-1010}. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem();
        int[] stats = GTFoodStats.stats(stack);
        if (stats == null) {
            return;
        }
        for (int stat = 0; stat < GTFoodStats.TRACKED; stat++) {
            if (stats[stat] != 0) {
                PlayerFoodStats.change(player, stats[stat], stat);
            }
        }
        if (stats[GTFoodStats.RADIATION] != 0) {
            // GT6 routes this into the tracker's sixth value (mRadiation); the port's radiation tracker is
            // PlayerRadiation, whose persistent key is the same kind of 7-bit dose.
            PlayerRadiation.change(player, stats[GTFoodStats.RADIATION]);
        }
    }

    /** GT6 ticks the whole tracker list every 50 ticks; the port ticks each player's own statistics. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        PlayerFoodStats.tick(event.player);
    }

    /**
     * GT6's tracker is a 1.7.10 extended property that is <em>not</em> copied on respawn, so dying clears
     * every statistic; a non-death clone (dimension change) keeps them, which is what the port's persistent
     * data already does - only the death case has to be made explicit, exactly like
     * {@link PlayerRadiation} does for the dose.
     */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            PlayerFoodStats.clear(event.getEntity());
        }
    }
}
