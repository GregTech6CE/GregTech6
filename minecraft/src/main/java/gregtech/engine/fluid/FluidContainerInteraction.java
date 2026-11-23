package gregtech.engine.fluid;

import gregtech.content.item.FluidDisplayItemLike;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** Filled-first original interaction priority, with survival-equivalent creative container exchange. */
public final class FluidContainerInteraction {
    private FluidContainerInteraction() {}

    /** Client-safe container check; does not rely on unsynced client tank contents. */
    public static boolean canInteract(Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        if (held.getItem() instanceof FluidDisplayItemLike && !player.getAbilities().instabuild) return false;
        return !held.isEmpty() && FluidUtil.getFluidHandler(held.copyWithCount(1)).isPresent();
    }

    /** Client-side prediction result for block interactions backed by {@link #use}. */
    public static InteractionResult clientResult(Player player, InteractionHand hand) {
        return canInteract(player, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    public static boolean use(Player player, InteractionHand hand, IFluidHandler tank) {
        var held = player.getItemInHand(hand);
        if (held.isEmpty()) return false;
        // Display items are a creative-only editing tool and never drain a tank in survival.
        boolean display = held.getItem() instanceof FluidDisplayItemLike;
        if (display && !player.getAbilities().instabuild) return false;
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
        // returning anything. Use the non-stow transfer and apply unified container delivery. A display item
        // only ever pours out; filling it would silently replace the fluid it stands for.
        boolean emptying = filled || display;
        var result = emptying
                ? FluidUtil.tryEmptyContainer(held, tank, Integer.MAX_VALUE, player, true)
                : FluidUtil.tryFillContainer(held, tank, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess() && emptying && !display) {
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
     * semantics without exposing reverse draining.
     */
    public static boolean pour(Player player, InteractionHand hand, IFluidHandler tank) {
        var held = player.getItemInHand(hand);
        if (held.isEmpty()) return false;
        if (held.getItem() instanceof FluidDisplayItemLike && !player.getAbilities().instabuild) return false;
        var result = FluidUtil.tryEmptyContainer(held, tank, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess()) return false;
        applyResult(player, hand, held, result.getResult(), true);
        return true;
    }

    /**
     * Swaps one held container for {@code result}. The replacement lands in the hand while the stack
     * lasts, otherwise in the inventory or on the ground, exactly as a survival bucket would behave.
     */
    public static ItemStack exchangeContainer(ItemStack held, Player player, ItemStack result) {
        if (held.isEmpty()) return result.copy();
        held.shrink(1);
        if (held.isEmpty()) return result.copy();
        if (!result.isEmpty()) {
            var delivered = result.copy();
            if (!player.getInventory().add(delivered)) player.drop(delivered, false);
        }
        return held;
    }

    /**
     * Applies replacement container delivery and syncs {@link ServerPlayer} inventory slots immediately.
     * Every real container exchanges like a survival bucket. A display item is a fluid stand-in rather
     * than a vessel, so creative mode keeps it untouched and survival mode never reaches this point.
     */
    public static void applyResult(
            Player player, InteractionHand hand, ItemStack held, ItemStack result, boolean emptyingContainer) {
        if (held.getItem() instanceof FluidDisplayItemLike) {
            if (player.getAbilities().instabuild) syncInventory(player);
            return;
        }
        player.setItemInHand(hand, exchangeContainer(held, player, result));
        syncInventory(player);
    }

    public static void syncInventory(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
            serverPlayer.inventoryMenu.sendAllDataToRemote();
        }
    }
}
