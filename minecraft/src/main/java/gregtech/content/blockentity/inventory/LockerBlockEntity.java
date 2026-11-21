package gregtech.content.blockentity.inventory;

import gregtech.content.blockentity.GTBlockEntity;
import gregtech.engine.stack.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** GT6 Locker block entity: stores one armor set (head/chest/legs/feet). */
public class LockerBlockEntity extends GTBlockEntity {

    private static final EquipmentSlot[] SLOTS =
            gregtech.content.storage.ContainerStorageRules.ARMOR_ORDER.stream()
                    .map(EquipmentSlot::valueOf)
                    .toArray(EquipmentSlot[]::new);

    private final ItemStack[] armor = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

    public LockerBlockEntity(BlockPos pos, BlockState state) {
        super(gregtech.content.registry.GTBlockEntities.LOCKER.get(), pos, state);
    }

    /** Swap eligible equipment slots in order; the block enforces front-face activation. */
    public void swapArmor(Player player) {
        boolean swapped = false;
        for (int i = 0; i < SLOTS.length; i++) {
            // Keep an incompatible stored stack in its original slot; other slots still swap.
            if (!armor[i].isEmpty() && !armor[i].canEquip(SLOTS[i], player)) continue;
            ItemStack worn = player.getItemBySlot(SLOTS[i]);
            player.setItemSlot(SLOTS[i], armor[i]);
            armor[i] = worn;
            swapped = true;
        }
        if (!swapped) return;
        if (level != null) {
//? if >=1.21.1 {
            /*level.playSound(null, worldPosition, SoundEvents.ARMOR_EQUIP_IRON.value(),
*///?} else {
            level.playSound(null, worldPosition, SoundEvents.ARMOR_EQUIP_IRON,
//?}
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        setChanged();
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < armor.length; i++) {
            if (!armor[i].isEmpty()) {
                gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, armor[i]);
                armor[i] = ItemStack.EMPTY;
            }
        }
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        ListTag list = new ListTag();
        for (ItemStack stack : armor) {
            list.add(NbtCompat.saveItem(stack, lookup));
        }
        tag.put("gt.armor", list);
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        if (tag.contains("gt.armor")) {
            ListTag list = tag.getList("gt.armor", 10);
            for (int i = 0; i < armor.length && i < list.size(); i++) {
                armor[i] = NbtCompat.readItem(list.getCompound(i), lookup);
            }
        }
    }
}
