package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.api.tool.PoweredToolTarget;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Original ten-tick trigger and hundred-tick recharge pause. */
public class AutoIgniterBlockEntity extends AutoToolBlockEntity {
    private int cooldown;
    public AutoIgniterBlockEntity(BlockPos pos,BlockState state) { super(com.gregtech.gregtech.registry.GTBlockEntities.AUTO_IGNITER.get(),pos,state); }
    @Override protected GregTechTags.Tag energyType() { return GregTechTags.Energy.EU; }
    @Override protected boolean acceptsSide(Direction side) { return side!=facing(); }
    @Override public long doInject(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute) {
        if (!isEnergyAcceptingFrom(type,side,false) || size<=0 || amount<=0) return 0;
        if (execute) {
            if(overvoltage(size)) return 1;
            if(cooldown==0 && energy<input()*10) energy+=size;
            setChanged();
        }
        return 1;
    }
    @Override public void tick() {
        if(level==null || level.isClientSide || level.getGameTime()%10!=0) return;
        if(cooldown>0) { cooldown--;setChanged(); }
        if(stopped || energy==0) return;
        var target=worldPosition.relative(facing());
        boolean success=level.getBlockEntity(target) instanceof PoweredToolTarget tool && tool.usePoweredIgniter(facing().getOpposite(),energy*20,quality());
        var state=level.getBlockState(target);
        if(!success && (CampfireBlock.canLight(state) || CandleBlock.canLight(state) || CandleCakeBlock.canLight(state))) {
            level.setBlockAndUpdate(target,state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,true)); success=true;
        }
        if(!success && state.isAir() && BaseFireBlock.canBePlacedAt(level,target,facing())) {
            level.setBlockAndUpdate(target,BaseFireBlock.getState(level,target));success=true;
        }
        if(success) level.playSound(null,target,SoundEvents.FLINTANDSTEEL_USE,SoundSource.BLOCKS,1,1);
        energy=0;cooldown=10;setChanged();
    }
    public int cooldown() { return cooldown; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);tag.putInt("gt.cooldown",cooldown); }
    @Override public void load(CompoundTag tag) { super.load(tag);cooldown=Math.max(0,Math.min(10,tag.getInt("gt.cooldown"))); }
}
