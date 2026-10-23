package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.block.energy.PowerCellBlock;
import com.gregtech.gregtech.content.energy.PowerCellSpec;
import com.gregtech.gregtech.item.PowerCellItem;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.HolderLookup;
*///?} else {
import com.gregtech.gregtech.registry.GTBlockEntities;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** No energy-net emission: GT6's placed power cells are passive, not battery boxes. */
public final class PowerCellBlockEntity extends BlockEntity {
    private long charge;
//? if >=1.21.1 {
    /*public PowerCellBlockEntity(BlockPos pos,BlockState state){super(GTBlockEntities.POWER_CELL.get(),pos,state);charge=spec().capacity();}
*///?} else {
    public PowerCellBlockEntity(BlockPos pos,BlockState state){super(GTBlockEntities.POWER_CELL.get(),pos,state);charge=spec().capacity();}
//?}
    public PowerCellSpec spec(){return ((PowerCellBlock)getBlockState().getBlock()).spec();}
    public long stored(){return charge;}
    public void setCharge(long value){
        charge=spec().usableRemainder(Math.max(0,value));setChanged();
        if(level!=null&&!level.isClientSide) {
            var state = getBlockState().setValue(PowerCellBlock.CHARGE_LEVEL, spec().display(charge));
            level.setBlock(worldPosition, state, 3);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
    public ItemStack asItem(){
        var stack=new ItemStack(getBlockState().getBlock());
        ((PowerCellItem)stack.getItem()).setCharge(stack,charge);return ((PowerCellItem)stack.getItem()).afterEnergyTransfer(stack);
    }
//? if >=1.21.1 {
    /*@Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);tag.putLong(PowerCellItem.CHARGE,charge);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);charge=Math.max(0,Math.min(spec().capacity(),tag.getLong(PowerCellItem.CHARGE)));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
*///?} else {
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putLong(PowerCellItem.CHARGE,charge);}
    @Override public void load(CompoundTag tag){super.load(tag);charge=Math.max(0,Math.min(spec().capacity(),tag.getLong(PowerCellItem.CHARGE)));}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
//?}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
