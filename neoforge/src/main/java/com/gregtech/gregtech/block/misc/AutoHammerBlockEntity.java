package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.api.tool.PoweredToolTarget;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.*;
import net.minecraft.world.level.block.state.BlockState;

/** KU forward packets store work; a reverse packet triggers the hammer stroke. */
public class AutoHammerBlockEntity extends AutoToolBlockEntity {
    private boolean pullingBack;
    public AutoHammerBlockEntity(BlockPos pos,BlockState state) { super(com.gregtech.gregtech.registry.GTBlockEntities.AUTO_HAMMER.get(),pos,state); }
    @Override protected GregTechTags.Tag energyType() { return GregTechTags.Energy.KU; }
    @Override protected boolean acceptsSide(Direction side) { return side==null || side==facing().getOpposite(); }
    @Override public long getEnergySizeInputMin(GregTechTags.Tag type,Direction side) { return Math.max(1,input()/8); }
    @Override public long doInject(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute) {
        if(!isEnergyAcceptingFrom(type,side,false) || size==0 || size==Long.MIN_VALUE || amount<=0) return 0;
        if(execute) {
            if(size>0) {
                if(overvoltage(size)) return amount;
                pullingBack=false;
                energy=com.gregtech.gregtech.content.tool.AutomaticToolRules.hammerStored(energy,size,amount);
            } else pullingBack=true;
            setChanged();
        }
        return amount;
    }
    @Override public void tick() {
        if(level==null || level.isClientSide || stopped || !pullingBack || energy<=0) return;
        var target=worldPosition.relative(facing());
        var entity=level.getBlockEntity(target);
        boolean success=entity instanceof PoweredToolTarget tool && tool.usePoweredHammer(facing().getOpposite(),com.gregtech.gregtech.content.tool.AutomaticToolRules.hammerBudget(energy),quality())>0;
        var state=level.getBlockState(target);
        if(!success && entity==null && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            float hardness=state.getDestroySpeed(level,target);
            int needed=state.is(BlockTags.NEEDS_DIAMOND_TOOL)?3:state.is(BlockTags.NEEDS_IRON_TOOL)?2:state.is(BlockTags.NEEDS_STONE_TOOL)?1:0;
            if(hardness>=0 && hardness*50<=energy && quality()>=needed) {
                var drops=net.minecraft.world.level.block.Block.getDrops(state,(net.minecraft.server.level.ServerLevel)level,target,null);
                if(level.destroyBlock(target,false)) {
                    for(var drop:drops) net.minecraft.world.level.block.Block.popResource(level,target,drop);
                    success=true;
                }
            }
        }
        if(success) level.playSound(null,target,SoundEvents.ANVIL_USE,SoundSource.BLOCKS,1,1);
        energy=0;setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) { super.saveAdditional(tag,lookup);tag.putBoolean("gt.pulling_back",pullingBack); }
    @Override protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) { super.loadAdditional(tag,lookup);energy=com.gregtech.gregtech.content.tool.AutomaticToolRules.hammerSaved(energy);pullingBack=tag.getBoolean("gt.pulling_back"); }
}
