package com.gregtech.gregtech.item;

import com.gregtech.gregtech.api.energy.item.IItemEnergy;
import com.gregtech.gregtech.block.energy.ChemicalBatteryBlock;
import com.gregtech.gregtech.content.energy.ChemicalBatterySpec;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import java.util.List;

/** A passive placed battery and the very same charged inventory item (TileEntityBase08Battery). */
public final class ChemicalBatteryItem extends BlockItem implements IItemEnergy {
    public static final String CHARGE="gt.charge";
    public ChemicalBatteryItem(ChemicalBatteryBlock block){super(block,new Properties().stacksTo(16));}
    public ChemicalBatterySpec spec(){return ((ChemicalBatteryBlock)getBlock()).spec();}
    public long stored(ItemStack stack){return stack.hasTag()?Math.max(0,Math.min(spec().capacity(),stack.getTag().getLong(CHARGE))):0;}
    public void setCharge(ItemStack stack,long energy){
        long charge=Math.max(0,Math.min(spec().capacity(),energy));
        if(charge==0) {if(stack.hasTag()) {stack.getTag().remove(CHARGE);if(stack.getTag().isEmpty())stack.setTag(null);}}
        else stack.getOrCreateTag().putLong(CHARGE,charge);
    }
    @Override public int getMaxStackSize(ItemStack stack){return stored(stack)>0?1:16;}
    @Override public InteractionResult place(BlockPlaceContext ctx){
        return ctx.getPlayer()!=null&&ctx.getPlayer().isShiftKeyDown()?super.place(ctx):InteractionResult.PASS;
    }
    @Override public boolean isEnergyType(ItemStack s,GregTechTags.Tag type){return type==GregTechTags.Energy.EU;}
    @Override public long getEnergyCapacity(ItemStack s,GregTechTags.Tag type){return isEnergyType(s,type)?spec().capacity():0;}
    @Override public long getEnergyStored(ItemStack s,GregTechTags.Tag type){return isEnergyType(s,type)?stored(s):0;}
    @Override public boolean canEnergyInjection(ItemStack stack,GregTechTags.Tag type,long size){
        return isEnergyType(stack,type)&&stack.getCount()==1&&size>=spec().minimumPacket()&&size<=spec().maximumPacket();
    }
    @Override public boolean canEnergyExtraction(ItemStack stack,GregTechTags.Tag type,long size){
        return canEnergyInjection(stack,type,size);
    }
    private long packet(ItemStack s,GregTechTags.Tag type,long size,long amount){
        if(!isEnergyType(s,type)||s.getCount()!=1||amount<1||size==Long.MIN_VALUE)return 0;
        size=Math.abs(size);
        return size>=spec().minimumPacket()&&size<=spec().maximumPacket()?size:0;
    }
    @Override public long doEnergyInjection(GregTechTags.Tag type,ItemStack stack,long size,long amount,Level level,BlockPos pos,boolean execute){
        long packet=packet(stack,type,size,amount),stored=stored(stack);
        if(packet==0||stored>=spec().capacity())return 0;
        // GT6 accepts the final packet even when it does not completely fit; excess is discarded.
        long accepted=com.gregtech.gregtech.content.energy.ItemBatteryRules.injectionPackets(spec().capacity(),stored,spec().voltage(),packet,amount);
        if(execute)setCharge(stack,stored+accepted*packet);
        return accepted;
    }
    @Override public long doEnergyExtraction(GregTechTags.Tag type,ItemStack stack,long size,long amount,Level level,BlockPos pos,boolean execute){
        long packet=packet(stack,type,size,amount);
        if(packet==0)return 0;
        long accepted=com.gregtech.gregtech.content.energy.ItemBatteryRules.extractionPackets(stored(stack),spec().voltage(),packet,amount);
        if(execute&&accepted>0)setCharge(stack,stored(stack)-accepted*packet);
        return accepted;
    }
    @Override public boolean isBarVisible(ItemStack stack){return stored(stack)>0;}
    @Override public int getBarWidth(ItemStack stack){return (int)(13*stored(stack)/spec().capacity());}
    @Override public int getBarColor(ItemStack stack){return spec().chemistry().color;}
    @Override public void appendHoverText(ItemStack stack,@javax.annotation.Nullable Level level,List<Component> tip,TooltipFlag flags){
        super.appendHoverText(stack,level,tip,flags);
        tip.add(Component.translatable("tooltip.gregtech.electric_tool.energy",stored(stack),spec().capacity()));
        tip.add(Component.translatable("tooltip.gregtech.chemical_battery.packet",spec().minimumPacket(),spec().maximumPacket()));
        tip.add(Component.translatable("tooltip.gregtech.chemical_battery.place"));
    }
}
