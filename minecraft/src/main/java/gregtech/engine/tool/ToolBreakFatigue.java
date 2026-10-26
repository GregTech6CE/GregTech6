package gregtech.engine.tool;

import gregtech.content.item.GTToolItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** GT_API_Proxy.onPlayerDestroyItem and ToolStats.afterBreaking. */
//? if >=1.21.1 {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = "gt6ce")
*///?} else {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6ce")
//?}
public final class ToolBreakFatigue {
    private ToolBreakFatigue() {}

//? if >=1.21.1 {
    /*@net.neoforged.bus.api.SubscribeEvent
    public static void onBreak(net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent event) {
*///?} else {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onBreak(net.minecraftforge.event.entity.player.PlayerDestroyItemEvent event) {
//?}
        apply(event.getEntity(), event.getOriginal());
    }

    public static void apply(Player player, ItemStack broken) {
        if (player.level().isClientSide || player.isCreative() || player.isSpectator()
                || !gregtech.GregTechConfig.Rule.TOOL_BREAK_FATIGUE.enabled()) return;
        if (broken.getItem() instanceof GTToolItem) {
            var type = GTToolHelper.getType(broken);
            if (type == GTToolType.FLINT_AND_TINDER || type == GTToolType.MAGNIFYING_GLASS
                    || type == GTToolType.BUILDER_WAND) return;
        } else if (!(broken.getItem() instanceof net.minecraft.world.item.DiggerItem)
                && !(broken.getItem() instanceof net.minecraft.world.item.SwordItem)) return;
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, 2, false, false));
    }
}
