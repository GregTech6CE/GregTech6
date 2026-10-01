package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.content.logistics.ExtenderSpec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** GT6 double-facing extender; bridges independently forward each face straight across. */
public final class SourceExtenderBlock extends DirectionalBlock implements EntityBlock,ToolInteractionTarget {
    public static final DirectionProperty SECONDARY=DirectionProperty.create("secondary");
    private final ExtenderSpec spec;
    public SourceExtenderBlock(ExtenderSpec spec,Properties properties){
        super(properties);this.spec=spec;
        registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(SECONDARY,Direction.SOUTH));
    }
    public ExtenderSpec spec(){return spec;}
    @Override protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,SECONDARY);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var front=context.getNearestLookingDirection();
        return defaultBlockState().setValue(FACING,front).setValue(SECONDARY,front.getOpposite());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ExtenderBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        return !level.isClientSide&&type==com.gregtech.gregtech.registry.GTBlockEntities.EXTENDER.get()?(world,pos,current,be)->((ExtenderBlockEntity)be).tickSignals():null;
    }
    @Override public boolean isSignalSource(BlockState state){return true;}
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction query){return level.getBlockEntity(pos) instanceof ExtenderBlockEntity relay?relay.signal(query):0;}
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction query){return level.getBlockEntity(pos) instanceof ExtenderBlockEntity relay?relay.coverStrongSignal(query):0;}
    @Override public boolean hasAnalogOutputSignal(BlockState state){return spec.universal();}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof ExtenderBlockEntity relay?relay.comparator(null):0;}
    @Override public ToolInteractionSpec toolInteraction(BlockState state,ItemStack tool){
        if(spec.bridge)return null;
        if(GTToolHelper.isMonkeyWrench(tool))return ToolInteractionSpec.facing(SECONDARY,com.gregtech.gregtech.block.machine.MachineRotationType.ALL);
        return GTToolHelper.isMachineWrench(tool)?ToolInteractionSpec.facing(FACING,com.gregtech.gregtech.block.machine.MachineRotationType.ALL):null;
    }
    public InteractionResult interact(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var tool=player.getItemInHand(hand);
        if(level.getBlockEntity(pos) instanceof ExtenderBlockEntity host){
            var result=com.gregtech.gregtech.content.cover.PanelCoverInteraction.use(host,player,hand,hit,true);
            if(result!=InteractionResult.PASS)return result;
        }
        if(spec.universal()&&(GTToolHelper.isSoftHammer(tool)||GTToolHelper.isScrewdriver(tool))&&level.getBlockEntity(pos) instanceof ExtenderBlockEntity relay){
            if(!level.isClientSide){
                var control=relay.machineControl(spec.bridge?hit.getDirection():null);
                if(!control.available()||(GTToolHelper.isScrewdriver(tool)&&!control.supportsMode())){
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.gregtech.extender.unavailable"),true);
                    return InteractionResult.CONSUME;
                }
                if(GTToolHelper.isScrewdriver(tool)){
                    int mode=control.setMode((control.mode()+1)&15);
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.gregtech.extender.mode",mode),true);
                }else{
                    boolean enabled=control.setEnabled(!control.enabled());
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.gregtech.extender."+(enabled?"enabled":"disabled")),true);
                }
                GTToolHelper.damageForUse(tool,1,player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return ToolInteractions.use(state,level,pos,player,hand,hit)?InteractionResult.sidedSuccess(level.isClientSide):InteractionResult.PASS;
    }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.extender."+(spec.bridge?"bridge":"routing")));
        if(!spec.bridge)lines.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.extender.tools"));
        if(spec.universal())lines.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.extender.control"));
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof ExtenderBlockEntity host){host.dropContents();level.updateNeighborsAt(pos,this);}
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){return java.util.List.of(new ItemStack(this));}
    @Override public net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held,BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override public net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}

}
