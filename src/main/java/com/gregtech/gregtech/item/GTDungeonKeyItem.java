package com.gregtech.gregtech.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Port of GT6's ten dungeon keys, which are ordinary entries of its random-tools multi-item
 * ({@code gregtech/items/MultiItemRandomTools.java:589-598}, ids 30000..30009, {@code IL.KEYS} in
 * {@code gregapi/data/IL.java:516}) with GT6's key behaviour attached
 * ({@code gregtech/items/behaviors/Behavior_Key.java}).
 *
 * <p>GT6 hands these keys out from the dungeon cells: {@code WorldgenDungeonGT:169-173} builds five
 * stacks, one per dungeon, each with a <b>random key type</b> ({@code IL.KEYS[aRandom.nextInt(10)]}),
 * the custom name {@code "Key #" + (i + 1)} and one id in the {@code gt.key} long tag; the rooms then
 * put the stack into a shelf or chest inventory, and {@code Behavior_Key} compares the tag of the held
 * key with the lock it is used on.</p>
 *
 * <h2>Port structure</h2>
 * <ul>
 *   <li><b>Ten separate items.</b> GT6 has one multi-item class with ten metas; the port registers one
 *       item per key (see {@link com.gregtech.gregtech.registry.GTDungeonKeys}), which is how every
 *       material item of this port works and keeps the item models trivial.</li>
 *   <li><b>The item id is not a material prefix.</b> GT6's keys are ore-dictionary entries of
 *       {@code OD.itemKey} built from a plate of the key's material, not one of its material forms; the
 *       port has no {@code key} prefix in its form table, so the keys are registered as plain items
 *       ({@code key_brass}, {@code key_bronze}, ...) instead of adding a tenth-of-a-material form that
 *       would have to be handled by every material loop of the port.</li>
 *   <li><b>Name and id.</b> GT6's {@code getWithNameAndNBT} writes the name into the vanilla
 *       {@code display.Name} tag, which is where 1.20.1 reads a custom name from as well, so
 *       {@link #withName} writes the same place and the stack shows {@code Key #N} like the original.</li>
 *   <li><b>Stack size.</b> GT6's keys keep the multi-item default of 64
 *       ({@code MultiItem.getItemStackLimit:392-396} only forces a limit of one for energy items), so
 *       the port registers them with 64 as well.</li>
 * </ul>
 */
public class GTDungeonKeyItem extends Item {

    /** GT6's {@code NBT_KEY} ({@code gregapi/data/CS.java:1169}). */
    public static final String NBT_KEY = "gt.key";
    /** The vanilla compound GT6's {@code getWithNameAndNBT} puts the custom name into. */
    public static final String NBT_DISPLAY = "display";
    /** The field of {@link #NBT_DISPLAY} that holds the name, {@code ItemStack.getHoverName}. */
    public static final String NBT_NAME = "Name";

    public GTDungeonKeyItem(Item.Properties properties) {
        super(properties);
    }

    @Override public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack, net.minecraft.world.item.context.UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof com.gregtech.gregtech.blockentity.inventory.SafeBlockEntity safe && safe.keyLocked()) {
            if (context.getLevel().isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
            return safe.useKey(stack) ? net.minecraft.world.InteractionResult.CONSUME : net.minecraft.world.InteractionResult.PASS;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    /** GT6's {@code Behavior_Key:50}: the {@code gt.key} id of the stack, {@code 0} for a blank key. */
    public static long keyId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(NBT_KEY);
    }

    /** GT6's {@code UT.NBT.setNumber(tNBT, NBT_KEY, aKeyID)}. */
    public static void setKeyId(ItemStack stack, long keyId) {
        stack.getOrCreateTag().putLong(NBT_KEY, keyId);
    }

    /** GT6's {@code getWithNameAndNBT}'s name part: the vanilla custom name of a stack. */
    public static ItemStack withName(ItemStack stack, String name) {
        stack.getOrCreateTagElement(NBT_DISPLAY).putString(NBT_NAME,
                Component.Serializer.toJson(Component.literal(name)));
        return stack;
    }

    /** GT6's {@code IL.KEYS[...].getWithNameAndNBT(1, "Key #" + (aIndex + 1), ...)}. */
    public static ItemStack key(Item item, long keyId, int index) {
        ItemStack stack = new ItemStack(item);
        setKeyId(stack, keyId);
        return withName(stack, "Key #" + (index + 1));
    }

    /**
     * GT6's key tooltips ({@code Behavior_Key:66-74}): the behaviour line and the id of the key, or
     * {@code *BLANK*} for a key that has none.
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gregtech.key.lock").withStyle(ChatFormatting.GRAY));
        long keyId = keyId(stack);
        tooltip.add(keyId == 0
                ? Component.translatable("tooltip.gregtech.key.blank").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("tooltip.gregtech.key.id", Long.toString(keyId))
                        .withStyle(ChatFormatting.DARK_GRAY));
    }
}
