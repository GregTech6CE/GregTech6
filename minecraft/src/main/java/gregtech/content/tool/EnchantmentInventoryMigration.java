package gregtech.content.tool;

import gregtech.engine.tool.MaterialEnchantmentStacks;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class EnchantmentInventoryMigration {
    private EnchantmentInventoryMigration() {}

//? if >=1.21.1 {
    /*@SubscribeEvent public static void tick(PlayerTickEvent.Post event) { var player = event.getEntity();
*///?} else {
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var player = event.player;
//?}
        if (player.level().isClientSide || player.tickCount % 20 != 0) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            MaterialEnchantmentStacks.migrateLegacy(player.getInventory().getItem(i), player.level());
    }
}
