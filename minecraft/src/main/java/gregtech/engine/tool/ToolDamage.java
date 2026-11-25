package gregtech.engine.tool;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Shared item durability and break-event facade across 1.20.1 Forge and 1.21.1 NeoForge.
 *
 * <p>1.21.1 passes an {@link EquipmentSlot} directly to {@code ItemStack#hurtAndBreak} and
 * {@code LivingEntity#onEquippedItemBroken}; 1.20.1 uses a break callback invoking
 * {@code broadcastBreakEvent}.</p>
 */
public final class ToolDamage {
    private ToolDamage() {}

    /** Maps a held interaction hand to its corresponding equipment slot. */
    public static EquipmentSlot slot(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }

    /** Damages an item held in {@code hand}, broadcasting the break animation if it breaks. */
    public static void hurtAndBreak(ItemStack stack, int amount, LivingEntity user, InteractionHand hand) {
        hurtAndBreak(stack, amount, user, slot(hand));
    }

    /** Damages an item in {@code slot}, broadcasting the break animation if it breaks. */
    public static void hurtAndBreak(ItemStack stack, int amount, LivingEntity user, EquipmentSlot slot) {
//? if >=1.21.1 {
        /*stack.hurtAndBreak(amount, user, slot);
*///?} else {
        stack.hurtAndBreak(amount, user, entity -> entity.broadcastBreakEvent(slot));
//?}
    }

    /** Notifies listeners that {@code brokenItem} broke in {@code slot} after manual damage handling. */
    public static void broadcastBreak(LivingEntity user, Item brokenItem, EquipmentSlot slot) {
//? if >=1.21.1 {
        /*user.onEquippedItemBroken(brokenItem, slot);
*///?} else {
        user.broadcastBreakEvent(slot);
//?}
    }
}
