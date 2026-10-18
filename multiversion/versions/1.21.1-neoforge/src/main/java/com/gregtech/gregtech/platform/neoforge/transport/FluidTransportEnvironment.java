package com.gregtech.gregtech.platform.neoforge.transport;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.damage.GTDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
/** Original biome Kelvin and meltdown behavior, separate from the recipe domain. */
public final class FluidTransportEnvironment {
    private FluidTransportEnvironment() {}
    public static long environmentTemperature(net.minecraft.world.level.LevelReader level,BlockPos pos){return level==null?GregTechConstants.DEF_ENV_TEMP:GregTechConstants.C+Math.round(level.getBiome(pos).value().getBaseTemperature()*20F);}
    public static void meltdown(Level level,BlockPos pos){
        if(level==null||level.isClientSide)return;
        level.playSound(null,pos,SoundEvents.LAVA_EXTINGUISH,SoundSource.BLOCKS,.5F,2.6F);
        level.setBlock(pos,Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,4),Block.UPDATE_ALL);
        for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos).inflate(.5)))entity.hurt(GTDamageTypes.heat(level),4F);
    }
}
