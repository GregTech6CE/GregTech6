package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Optical cable connections share the cutter interaction and overlay used by other connectors. */
public final class LaserFiberBlock extends Block implements EntityBlock,ToolInteractionTarget {
    public LaserFiberBlock(Properties properties){
        super(properties.noOcclusion());
        var state=defaultBlockState();
        for(var property:ElectricWireBlock.CONNECTIONS)state=state.setValue(property,false);
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ElectricWireBlock.CONNECTIONS);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new com.gregtech.gregtech.blockentity.energy.LaserFiberBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        return !level.isClientSide && type==com.gregtech.gregtech.registry.GTBlockEntities.LASER_FIBER.get()
                ? (world,pos,current,entity)->((com.gregtech.gregtech.blockentity.energy.LaserFiberBlockEntity)entity).rollTransferWindow()
                : null;
    }
    @Override public java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){return java.util.List.of(new ItemStack(this));}
    @Override public ToolInteractionSpec toolInteraction(BlockState state,ItemStack tool){return GTToolHelper.isWireCutter(tool)?ToolInteractionSpec.connections(ToolInteractionSpec.ConnectionKind.LASER):null;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        return ToolInteractions.use(state,level,pos,player,hand,hit)?InteractionResult.sidedSuccess(level.isClientSide):InteractionResult.PASS;
    }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(level.isClientSide||old.is(this))return;
        var next=state;
        for(var side:Direction.values()){
            var neighbor=pos.relative(side);
            if(!level.hasChunkAt(neighbor))continue;
            var entity=level.getBlockEntity(neighbor);
            if(entity instanceof IEnergyBlock energy && (energy.isEnergyAcceptingFrom(GregTechTags.Energy.LU,side.getOpposite(),true)
                    || energy.isEnergyEmittingTo(GregTechTags.Energy.LU,side.getOpposite(),true)) || level.getBlockState(neighbor).getBlock() instanceof LaserFiberBlock){
                next=next.setValue(ElectricWireBlock.propFor(side),true);
                var other=level.getBlockState(neighbor);
                if(other.getBlock() instanceof LaserFiberBlock)level.setBlockAndUpdate(neighbor,other.setValue(ElectricWireBlock.propFor(side.getOpposite()),true));
            }
        }
        if(next!=state)level.setBlockAndUpdate(pos,next);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){
        if(context instanceof EntityCollisionContext entity && entity.getEntity() instanceof Player player && GTToolHelper.isWireCutter(player.getMainHandItem()))return Shapes.block();
        var shape=Block.box(6,6,6,10,10,10);
        for(var side:Direction.values())if(state.getValue(ElectricWireBlock.propFor(side)))shape=Shapes.or(shape,
                Block.box(side==Direction.WEST?0:6,side==Direction.DOWN?0:6,side==Direction.NORTH?0:6,
                        side==Direction.EAST?16:10,side==Direction.UP?16:10,side==Direction.SOUTH?16:10));
        return shape;
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){
        return getShape(state,world,pos,CollisionContext.empty());
    }
}
