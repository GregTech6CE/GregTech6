package gregtech.content.food;

import net.minecraft.world.entity.player.Player;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/**
 * GT6 {@code EntityFoodTracker.tick()}'s AFK hunger branch: every 2400 player ticks, non-creative,
 * non-spectator, living players gain exhaustion scaled by the weight of their inventory and armor.
 *
 * <p>The branch used {@code player.tickCount}, not the level's game time, so it keeps the per-entity
 * tick phase; {@code general.AFK_Hunger} still gates the whole pass.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class AfkHungerEvents {
    private AfkHungerEvents() {}

    @SubscribeEvent
//? if >=1.21.1 {
    /*public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
*///?} else {
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
//?}
            return;
        }
//? if >=1.21.1 {
        /*apply(event.getEntity());
*///?} else {
        apply(event.player);
//?}
    }

    /** GT6's AFK hunger pass; kept public so GameTest can drive it without a world tick. */
    public static void apply(Player player) {
        if (player.isCreative() || player.isSpectator() || !player.isAlive()
                || !gregtech.GregTechConfig.Rule.AFK_HUNGER.enabled()
                || player.tickCount % 2400 != 1200) {
            return;
        }
        int count = 64;
        for (var stack : player.getInventory().items) if (!stack.isEmpty())
            count += stack.getCount() * 64 / Math.max(1, stack.getMaxStackSize());
        for (var stack : player.getInventory().armor) if (!stack.isEmpty()) count += 256;
        player.causeFoodExhaustion(Math.max(1.0F, count / 666F));
    }
}
