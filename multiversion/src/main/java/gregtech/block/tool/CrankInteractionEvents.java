/* Derived from GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. */
package gregtech.block.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID)
*///?} else {
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID)
//?}
public final class CrankInteractionEvents {
    private CrankInteractionEvents() {}
    // Weak keys avoid retaining disconnected players/worlds. Targets are transient, as in GT6.
    private static final Map<Player, BlockPos> LAST_TARGET = new WeakHashMap<>();

    static void target(Player player, BlockPos pos) {
        if (player.level().isClientSide) return;
        if (player.level().getBlockState(pos).getBlock() instanceof CrankBlock) {
            LAST_TARGET.put(player, pos.immutable());
        } else {
            LAST_TARGET.remove(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockClick(PlayerInteractEvent.RightClickBlock event) {
        target(event.getEntity(), event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockAttack(PlayerInteractEvent.LeftClickBlock event) {
        target(event.getEntity(), event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void airUse(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide) LAST_TARGET.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void logout(
//? if >=1.21.1 {
            /*net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
*///?} else {
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
//?}
        LAST_TARGET.remove(event.getEntity());
    }

    static List<Player> operators(ServerLevel level, BlockPos pos) {
        List<Player> result = new ArrayList<>();
        LAST_TARGET.forEach((player, target) -> {
            if (player.isAlive() && !player.isRemoved() && player.level() == level && pos.equals(target)
                    && (player.isCreative() || player.distanceToSqr(pos.getX() + .5,
                            pos.getY() + .5, pos.getZ() + .5) <= 36)) result.add(player);
        });
        return result;
    }
}
