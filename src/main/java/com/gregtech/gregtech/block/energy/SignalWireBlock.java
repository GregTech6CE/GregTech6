package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.blockentity.energy.SignalWireBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.List;

/** Six independent ports; the common cutter spec also drives the client face overlay. */
public final class SignalWireBlock extends Block implements EntityBlock, ToolInteractionTarget {
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
    private final GTMaterial material;
    private final int range;
    private final boolean insulated;
    public SignalWireBlock(GTMaterial material, int range, boolean insulated, boolean luminous, Properties properties) {
        super(properties.lightLevel(state -> luminous ? state.getValue(POWER) : 0));
        this.material=material; this.range=range; this.insulated=insulated;
        var state=defaultBlockState().setValue(POWER,0);
        for(var property:ElectricWireBlock.CONNECTIONS) state=state.setValue(property,false);
        registerDefaultState(state);
    }
    public GTMaterial material(){return material;}
    public int range(){return range;}
    public boolean insulated(){return insulated;}
    public int halfThickness(){return insulated?2:1;}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){
        return defaultBlockState().setValue(ElectricWireBlock.propFor(context.getClickedFace().getOpposite()),true);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ElectricWireBlock.CONNECTIONS).add(POWER);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SignalWireBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return !level.isClientSide && type==GTBlockEntities.SIGNAL_WIRE.get() ? (l,p,s,be)->((SignalWireBlockEntity)be).tickNetwork():null;
    }
    @Override public ToolInteractionSpec toolInteraction(BlockState state,ItemStack tool){
        return GTToolHelper.isWireCutter(tool)?ToolInteractionSpec.connections(ToolInteractionSpec.ConnectionKind.REDSTONE):null;
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(ToolInteractions.use(state,level,pos,player,hand,hit))return InteractionResult.sidedSuccess(level.isClientSide);
        if(GTToolHelper.isScrewdriver(player.getItemInHand(hand))){
            if(!level.isClientSide && player.mayBuild() && level.mayInteract(player,pos) && level.getBlockEntity(pos) instanceof SignalWireBlockEntity wire){
                wire.setMode((wire.mode()+(player.isShiftKeyDown()?15:1))&15);
                player.displayClientMessage(Component.translatable("message.gregtech.signal_wire.mode",wire.mode()),true);
                GTToolHelper.damageForUse(player.getItemInHand(hand),1,player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(level.isClientSide||old.is(this))return;
        var next=state;
        for(var side:Direction.values()){
            var neighbor=pos.relative(side);
            if(!level.hasChunkAt(neighbor))continue;
            var other=level.getBlockState(neighbor);
            if(other.getBlock() instanceof SignalWireBlock){
                next=next.setValue(ElectricWireBlock.propFor(side),true);
                level.setBlockAndUpdate(neighbor,other.setValue(ElectricWireBlock.propFor(side.getOpposite()),true));
            } else if(!other.isAir() && (other.isSignalSource() || other.isRedstoneConductor(level,neighbor))) {
                next=next.setValue(ElectricWireBlock.propFor(side),true);
            }
        }
        level.setBlockAndUpdate(pos,next);
    }
    @Override public boolean isSignalSource(BlockState state){return true;}
    @Override public boolean canConnectRedstone(BlockState state,BlockGetter level,BlockPos pos,Direction side){
        return side!=null&&state.getValue(ElectricWireBlock.propFor(side.getOpposite()));
    }
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction query){
        return level.getBlockEntity(pos) instanceof SignalWireBlockEntity wire?wire.output(query.getOpposite()):0;
    }
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction query){return getSignal(state,level,pos,query);}
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){
        return level.getBlockEntity(pos) instanceof SignalWireBlockEntity wire?wire.comparator():0;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        super.onRemove(state,level,pos,next,moving);
        if(!state.is(next.getBlock()))level.updateNeighborsAt(pos,this);
    }
    @Override public List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){return List.of(new ItemStack(this));}
    @Override public void appendHoverText(ItemStack stack,BlockGetter level,List<Component> text,TooltipFlag flag){
        text.add(Component.translatable("tooltip.gregtech.signal_wire.range",range));
        text.add(Component.translatable("tooltip.gregtech.signal_wire.controls"));
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        if(context instanceof EntityCollisionContext e && e.getEntity() instanceof Player p && GTToolHelper.isWireCutter(p.getMainHandItem()))return Shapes.block();
        int a=8-halfThickness(),b=8+halfThickness();
        var shape=Block.box(a,a,a,b,b,b);
        for(var side:Direction.values())if(state.getValue(ElectricWireBlock.propFor(side)))shape=Shapes.or(shape,
                Block.box(side==Direction.WEST?0:a,side==Direction.DOWN?0:a,side==Direction.NORTH?0:a,
                        side==Direction.EAST?16:b,side==Direction.UP?16:b,side==Direction.SOUTH?16:b));
        return shape;
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return getShape(state,level,pos,CollisionContext.empty());}
}
