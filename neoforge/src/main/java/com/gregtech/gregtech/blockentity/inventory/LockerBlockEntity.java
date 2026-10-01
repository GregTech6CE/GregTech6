package com.gregtech.gregtech.blockentity.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** GT6 Locker block entity: stores one armor set (head/chest/legs/feet). */
public class LockerBlockEntity extends BlockEntity {

    private static final EquipmentSlot[] SLOTS = com.gregtech.gregtech.content.storage.ContainerStorageRules.ARMOR_ORDER.stream().map(EquipmentSlot::valueOf).toArray(EquipmentSlot[]::new);

    private final ItemStack[] armor = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

    public LockerBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.LOCKER.get(), pos, state);
    }

    /** Swap the player's worn armor with the stored set (GT6 locker behavior). */
    public void swapArmor(Player player) {
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack worn = player.getItemBySlot(SLOTS[i]);
            player.setItemSlot(SLOTS[i], armor[i]);
            armor[i] = worn;
        }
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ARMOR_EQUIP_IRON.value(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        setChanged();
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < armor.length; i++) {
            if (!armor[i].isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, armor[i]);
                armor[i] = ItemStack.EMPTY;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        ListTag list = new ListTag();
        for (ItemStack stack : armor) {
            list.add(stack.saveOptional(lookup));
        }
        tag.put("gt.armor", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        if (tag.contains("gt.armor")) {
            ListTag list = tag.getList("gt.armor", 10);
            for (int i = 0; i < armor.length && i < list.size(); i++) {
                armor[i] = ItemStack.parseOptional(lookup,list.getCompound(i));
            }
        }
    }
}
