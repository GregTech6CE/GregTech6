package com.gregtech.gregtech.blockentity.tool;

import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.block.tool.MaterialAnvilBlock;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Two workpieces and persisted GT6 durability units. Failed recipes never consume either. */
public final class MaterialAnvilBlockEntity extends BlockEntity implements com.gregtech.gregtech.api.inventory.BlockContents, PoweredToolTarget {
    private final ItemStack[] work = {ItemStack.EMPTY, ItemStack.EMPTY};
    private long durability;
    private final net.minecraftforge.items.IItemHandler input = new net.minecraftforge.items.IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) { return workpiece(slot); }
        @Override public int getSlotLimit(int slot) { return slot >= 0 && slot < 2 ? 64 : 0; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 0 && slot < 2 && !stack.isEmpty()
                    && !com.gregtech.gregtech.content.tool.AnvilHammerDisplay.isHammer(work[0])
                    && !com.gregtech.gregtech.content.tool.AnvilHammerDisplay.isHammer(work[1])
                    && (MachineRecipeMaps.Anvil.containsInput(stack) || MachineRecipeMaps.AnvilBendSmall.containsInput(stack)
                        || MachineRecipeMaps.AnvilBendBig.containsInput(stack));
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot,stack) || !work[slot].isEmpty() && !ItemStack.isSameItemSameTags(stack,work[slot])) return stack.copy();
            int count=Math.min(stack.getCount(),Math.min(64,stack.getMaxStackSize())-work[slot].getCount());
            if(count<=0)return stack.copy();
            if(!simulate) {
                if(work[slot].isEmpty())work[slot]=stack.copyWithCount(count);else work[slot].grow(count);
                markUpdated();
            }
            return stack.copyWithCount(stack.getCount()-count);
        }
        // GT6 allows automated insertion from every side, but never automated extraction.
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate) { return ItemStack.EMPTY; }
    };
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> itemCapability = net.minecraftforge.common.util.LazyOptional.of(()->input);
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, Direction side) {
        return cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER ? itemCapability.cast() : super.getCapability(cap,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); itemCapability=net.minecraftforge.common.util.LazyOptional.of(()->input); }

    public MaterialAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.MATERIAL_ANVIL.get(), pos, state);
        durability = ((MaterialAnvilBlock) state.getBlock()).durability();
    }

    /** Place the GT6 workshop's initial hammer without bypassing the two-slot anvil rules. */
    public boolean setDungeonHammer(ItemStack hammer) {
        if (!com.gregtech.gregtech.content.tool.AnvilHammerDisplay.isHammer(hammer)
                || !work[0].isEmpty() || !work[1].isEmpty()) return false;
        work[0] = hammer.copyWithCount(1);
        markUpdated();
        return true;
    }
    public void interact(Player player, InteractionHand hand, BlockHitResult hit) {
        if (level == null || level.isClientSide) return;
        ItemStack held = player.getItemInHand(hand);
        Direction facing = getBlockState().getValue(MaterialAnvilBlock.FACING);
        double half = facing.getAxis() == Direction.Axis.Z ? hit.getLocation().x - worldPosition.getX() : hit.getLocation().z - worldPosition.getZ();
        if (hit.getLocation().y - worldPosition.getY() < .25) return;
        boolean storedHammer = com.gregtech.gregtech.content.tool.AnvilHammerDisplay.isHammer(work[0])
                || com.gregtech.gregtech.content.tool.AnvilHammerDisplay.isHammer(work[1]);
        if (storedHammer && hit.getDirection() == Direction.UP) {
            int occupied = work[0].isEmpty() ? 1 : 0;
            give(player, work[occupied]); work[occupied] = ItemStack.EMPTY; markUpdated(); return;
        }
        if (GTToolHelper.matchesTool(held, GTToolType.HARD_HAMMER)) {
            if (hit.getDirection() == Direction.UP && work[0].isEmpty() && work[1].isEmpty()) {
                work[half < .5 ? 0 : 1] = held.copy();
                if (!player.getAbilities().instabuild) held.setCount(0);
                markUpdated();
            } else hammer(player, held, facing, hit.getDirection(), half, Long.MAX_VALUE);
            return;
        }
        if (hit.getDirection() != Direction.UP) { splitWork(player); return; }
        if (!held.isEmpty() && !MachineRecipeMaps.Anvil.containsInput(held)
                && !MachineRecipeMaps.AnvilBendSmall.containsInput(held)
                && !MachineRecipeMaps.AnvilBendBig.containsInput(held)) return;
        int slot = half < .5 ? 0 : 1;
        if (held.isEmpty()) { give(player, work[slot]); work[slot] = ItemStack.EMPTY; }
        else if (work[slot].isEmpty()) {
            work[slot] = held.copy();
            if (!player.getAbilities().instabuild) held.setCount(0);
        } else if (ItemStack.isSameItemSameTags(held, work[slot])) {
            int amount = Math.min(held.getCount(), work[slot].getMaxStackSize() - work[slot].getCount());
            work[slot].grow(amount);
            if (!player.getAbilities().instabuild) held.shrink(amount);
        }
        markUpdated();
    }
    /** GT6 side interaction: divide the single occupied slot evenly, returning an odd remainder. */
    private void splitWork(Player player) {
        if (work[0].isEmpty() == work[1].isEmpty()) return;
        int source = work[0].isEmpty() ? 1 : 0;
        if ((work[source].getCount() & 1) != 0) give(player, work[source].split(1));
        if (work[source].getCount() > 1) {
            work[source].setCount(work[source].getCount() / 2);
            work[1-source] = work[source].copy();
        }
        if (work[source].isEmpty()) work[source] = ItemStack.EMPTY;
        markUpdated();
    }
    @Override public long usePoweredHammer(Direction side,long budget,int quality) {
        if(level==null || level.isClientSide) return 0;
        return hammer(null,ItemStack.EMPTY,getBlockState().getValue(MaterialAnvilBlock.FACING),side,.5,budget);
    }
    private long hammer(Player player, ItemStack hammer, Direction facing, Direction side, double half, long budget) {
        if (durability == 0) return 0;
        RecipeMap map = switch (com.gregtech.gregtech.content.tool.AnvilRules.surface(facing, side, half, player==null)) {
            case TOP -> MachineRecipeMaps.Anvil; case SMALL -> MachineRecipeMaps.AnvilBendSmall; case BIG -> MachineRecipeMaps.AnvilBendBig;
        };
        for (Recipe recipe : map.mRecipeList) {
            if (!recipe.mEnabled || recipe.mFakeRecipe || recipe.mFluidInputs.length != 0 || recipe.mFluidOutputs.length != 0) continue;
            if(com.gregtech.gregtech.content.tool.AnvilRules.wear(recipe.mEUt,recipe.mDuration,0,0)>budget) continue;
            ItemStack[] remaining = consume(recipe);
            if (remaining == null) continue;
            work[0] = remaining[0]; work[1] = remaining[1];
            for (int i = 0; i < recipe.mOutputs.length; i++) {
                long count = recipe.rollOutputCount(i, 1, level.random::nextInt);
                if (count == 0) continue;
                ItemStack output = recipe.getOutput(i);
                output.setCount((int) count);
                give(player, output);
            }
            var fatigue = player==null?null:player.getEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN);
            var haste = player==null?null:player.getEffect(net.minecraft.world.effect.MobEffects.DIG_SPEED);
            double power = Math.abs((double) recipe.mEUt) * recipe.mDuration;
            long wear = com.gregtech.gregtech.content.tool.AnvilRules.wear(recipe.mEUt, recipe.mDuration,
                    fatigue == null ? 0 : fatigue.getAmplifier() + 1, haste == null ? 0 : haste.getAmplifier() + 1);
            durability = Math.max(0, durability - Math.min(durability, wear));
            if(player!=null) GTToolHelper.damageForUse(hammer, (int)Math.min(Integer.MAX_VALUE, Math.max(1, (wear + 9999.0) / 10000)), player);
            if(player!=null) player.causeFoodExhaustion((float) Math.min(Float.MAX_VALUE, power / 5000));
            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
            markUpdated();
            if (durability == 0) {
                for (int i=0;i<work.length;i++) { give(player,work[i]);work[i]=ItemStack.EMPTY; }
                var material = ((MaterialAnvilBlock)getBlockState().getBlock()).material();
                give(player, com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.scrapGt, material, 48 + level.random.nextInt(16)));
                level.removeBlock(worldPosition, false);
            }
            return wear;
        }
        return 0;
    }
    public ItemStack workpiece(int slot) { return slot >= 0 && slot < 2 ? work[slot].copy() : ItemStack.EMPTY; }
    private void markUpdated() {
        super.setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(net.minecraft.network.Connection connection, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }
    private ItemStack[] consume(Recipe recipe) {
        return com.gregtech.gregtech.content.tool.AnvilWorkInputs.consume(work,recipe.mInputs);
    }
    private void give(Player player, ItemStack item) { if(player==null) com.gregtech.gregtech.api.inventory.BlockContents.drop(this,item); else if (!item.isEmpty() && !player.addItem(item)) player.drop(item, false); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putLong("gt.durability", durability);
        for (int i = 0; i < 2; i++) tag.put("gt.work" + i, work[i].save(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("gt.durability")) durability = Math.max(0, Math.min(((MaterialAnvilBlock)getBlockState().getBlock()).durability(), tag.getLong("gt.durability")));
        for (int i = 0; i < 2; i++) work[i] = ItemStack.of(tag.getCompound("gt.work" + i));
    }
    @Override public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < work.length; i++) {
            com.gregtech.gregtech.api.inventory.BlockContents.drop(this, work[i]);
            work[i] = ItemStack.EMPTY;
        }
        setChanged();
    }
}
