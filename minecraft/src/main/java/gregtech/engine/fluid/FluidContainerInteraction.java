package gregtech.engine.fluid;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemUtils;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** Filled-first original interaction priority, with vanilla creative and survival container handling. */
public final class FluidContainerInteraction {
    private FluidContainerInteraction() {}

    public static boolean use(Player player, InteractionHand hand, IFluidHandler tank) {
        var held = player.getItemInHand(hand);
        if (held.isEmpty()) return false;
        // Probe one item: stacked containers are processed one at a time.
        var item = FluidUtil.getFluidHandler(held.copyWithCount(1)).orElse(null);
        if (item == null) return false;
        boolean filled = false;
        for (int i = 0; i < item.getTanks(); i++) {
            if (!item.getFluidInTank(i).isEmpty()) {
                filled = true;
                break;
            }
        }
        // FluidUtil's stow variants discard the filled container in creative mode, draining the tank without
        // returning anything. Use the non-stow transfer and match vanilla BucketItem / ItemUtils creative rules.
        var result = filled
                ? FluidUtil.tryEmptyContainer(held, tank, Integer.MAX_VALUE, player, true)
                : FluidUtil.tryFillContainer(held, tank, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess()) return false;
        if (!filled || !player.getAbilities().instabuild)
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result.getResult()));
        return true;
    }
}
