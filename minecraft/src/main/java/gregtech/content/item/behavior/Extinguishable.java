package gregtech.content.item.behavior;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A block (or block entity) that reacts to GT6's {@code TOOL_extinguisher} ({@code CS}):
 * the port's half of {@code IBlockToolable.onToolClick}.
 *
 * <p>GT6's implementors answer {@code 10000} — one full unit — when they defused themselves or
 * stopped burning; the extinguisher turns that into {@link BehaviorSprayExtinguisher#HOOK_MAX_COST} uses
 * ({@code Behavior_Spray_Extinguisher}).</p>
 */
public interface Extinguishable {
    /**
     * @return GT6's tool-click return value in 1/10000 units, {@code 0} when nothing was
     *         extinguished
     */
    long onExtinguish(
            Level level,
            BlockPos pos,
            Direction side,
            @Nullable Player player,
            ItemStack can,
            boolean sneaking,
            float hitX,
            float hitY,
            float hitZ);
}
