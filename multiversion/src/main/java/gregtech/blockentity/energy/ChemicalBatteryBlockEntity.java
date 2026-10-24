package gregtech.blockentity.energy;

import gregtech.block.energy.ChemicalBatteryBlock;
import gregtech.content.energy.PortableBatterySpec;
import gregtech.item.ChemicalBatteryItem;
//? if >=1.21.1 {
/*import gregtech.registry.GTChemicalBatteries;
import net.minecraft.core.HolderLookup;
*///?} else {
import gregtech.registry.GTBlockEntities;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** No energy-net emission: GT6's placed chemical batteries are passive, not battery boxes. */
public final class ChemicalBatteryBlockEntity extends BlockEntity {
    private long charge;
//? if >=1.21.1 {
    /*public ChemicalBatteryBlockEntity(BlockPos pos,BlockState state){super(GTChemicalBatteries.CHEMICAL_BATTERY.get(),pos,state);}
*///?} else {
    public ChemicalBatteryBlockEntity(BlockPos pos,BlockState state){super(GTBlockEntities.CHEMICAL_BATTERY.get(),pos,state);}
//?}
    public PortableBatterySpec spec(){return ((ChemicalBatteryBlock)getBlockState().getBlock()).spec();}
    public long stored(){return charge;}
    public void setCharge(long value){
        charge=Math.max(0,Math.min(spec().capacity(),value));setChanged();
        if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public ItemStack asItem(){
        var stack=new ItemStack(getBlockState().getBlock());
        ((ChemicalBatteryItem)stack.getItem()).setCharge(stack,charge);return stack;
    }
//? if >=1.21.1 {
    /*@Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);tag.putLong(ChemicalBatteryItem.CHARGE,charge);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);charge=Math.max(0,Math.min(spec().capacity(),tag.getLong(ChemicalBatteryItem.CHARGE)));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
*///?} else {
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putLong(ChemicalBatteryItem.CHARGE,charge);}
    @Override public void load(CompoundTag tag){super.load(tag);charge=Math.max(0,Math.min(spec().capacity(),tag.getLong(ChemicalBatteryItem.CHARGE)));}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
//?}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
