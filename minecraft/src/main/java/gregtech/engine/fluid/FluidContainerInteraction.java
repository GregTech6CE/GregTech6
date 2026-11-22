package gregtech.engine.fluid;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** Filled-first original interaction priority, with unconditional creative delivery and immediate inventory sync. */
public final class FluidContainerInteraction {
    private FluidContainerInteraction() {}

    /** Client-safe container check; does not rely on unsynced client tank contents. */
    public static boolean canInteract(Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        return !held.isEmpty() && FluidUtil.getFluidHandler(held.copyWithCount(1)).isPresent();
    }

    /** Client-side prediction result for block interactions backed by {@link #use}. */
    public static InteractionResult clientResult(Player player, InteractionHand hand) {
        return canInteract(player, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

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
        // returning anything. Use the non-stow transfer and apply unified creative/survival container delivery.
        boolean emptying = filled;
        var result = filled
                ? FluidUtil.tryEmptyContainer(held, tank, Integer.MAX_VALUE, player, true)
                : FluidUtil.tryFillContainer(held, tank, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess() && filled) {
            // Partially filled containers against output-only endpoints or full tanks fall back to filling.
            emptying = false;
            result = FluidUtil.tryFillContainer(held, tank, Integer.MAX_VALUE, player, true);
        }
        if (!result.isSuccess()) return false;
        applyResult(player, hand, held, result.getResult(), emptying);
        return true;
    }

    /**
     * One-way pour from a held fluid container into {@code tank}, keeping stacked {@code copyWithCount(1)}
     * and creative preservation semantics without exposing reverse draining.
     */
    public static boolean pour(Player player, InteractionHand hand, IFluidHandler tank) {
        var held = player.getItemInHand(hand);
        if (held.isEmpty()) return false;
        var result = FluidUtil.tryEmptyContainer(held, tank, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess()) return false;
        applyResult(player, hand, held, result.getResult(), true);
        return true;
    }

    /**
     * Applies replacement container delivery and syncs {@link ServerPlayer} inventory slots immediately.
     * Creative mode preserves filled containers and keeps empty containers while unconditionally adding filled output.
     */
    public static void applyResult(
            Player player, InteractionHand hand, ItemStack held, ItemStack result, boolean emptyingContainer) {
        if (!emptyingContainer || !player.getAbilities().instabuild) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result.copy(), false));
        }
        syncInventory(player);
    }

    public static void syncInventory(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
            serverPlayer.inventoryMenu.sendAllDataToRemote();
        }
    }
}
