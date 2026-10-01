package com.gregtech.gregtech.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** GT6 Filter block — pipe-insertable item/fluid filter. */
public class FilterBlock extends net.minecraft.world.level.block.DirectionalBlock implements net.minecraft.world.level.block.EntityBlock,com.gregtech.gregtech.api.tool.ToolInteractionTarget {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty SECONDARY=net.minecraft.world.level.block.state.properties.DirectionProperty.create("secondary");
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty SECONDARY_SET=net.minecraft.world.level.block.state.properties.BooleanProperty.create("secondary_set");
    private final String filterType;
    public static Direction secondary(BlockState state){return state.getValue(SECONDARY_SET)?state.getValue(SECONDARY):state.getValue(FACING).getOpposite();}

    public FilterBlock(String filterType, Properties properties) {
        super(properties);
        this.filterType = filterType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SECONDARY,Direction.SOUTH).setValue(SECONDARY_SET,false));
    }

    public String filterType() { return filterType; }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.BlockGetter level,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag){
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.filter.routing"));
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.filter.tools"));
        if(filterType.equals("oredict"))lines.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.filter.prefix"));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING,SECONDARY,SECONDARY_SET); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { var front=ctx.getNearestLookingDirection();return defaultBlockState().setValue(FACING,front).setValue(SECONDARY,front.getOpposite()).setValue(SECONDARY_SET,true); }

    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new FilterBlockEntity(pos,state); }
    @Override public net.minecraft.world.InteractionResult use(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        if(com.gregtech.gregtech.api.tool.ToolInteractions.use(state,level,pos,player,hand,hit)) return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        var tool=player.getItemInHand(hand);
        if(com.gregtech.gregtech.api.tool.GTToolHelper.isScrewdriver(tool)||com.gregtech.gregtech.api.tool.GTToolHelper.isSoftHammer(tool)){
            if(!level.isClientSide&&level.getBlockEntity(pos) instanceof FilterBlockEntity filter){
                if(com.gregtech.gregtech.api.tool.GTToolHelper.isSoftHammer(tool))filter.clearFilter();else filter.toggleMode();
                com.gregtech.gregtech.api.tool.GTToolHelper.damageForUse(tool,1,player);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(filter.blacklist()?"gregtech.filter.blacklist":"gregtech.filter.whitelist"),true);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer server && level.getBlockEntity(pos) instanceof FilterBlockEntity filter)
            net.minecraftforge.network.NetworkHooks.openScreen(server,filter,buffer->buffer.writeBoolean(filter.prefixMode()));
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public com.gregtech.gregtech.api.tool.ToolInteractionSpec toolInteraction(BlockState state,net.minecraft.world.item.ItemStack tool){
        var property=com.gregtech.gregtech.api.tool.GTToolHelper.isMonkeyWrench(tool)?SECONDARY:com.gregtech.gregtech.api.tool.GTToolHelper.isMachineWrench(tool)?FACING:null;
        return property==null?null:com.gregtech.gregtech.api.tool.ToolInteractionSpec.facing(property,com.gregtech.gregtech.block.machine.MachineRotationType.ALL);
    }
    @Override public void toolStateChanged(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,com.gregtech.gregtech.api.tool.ToolInteractionSpec operation){
        if(operation.facing()==SECONDARY&&!state.getValue(SECONDARY_SET))level.setBlockAndUpdate(pos,state.setValue(SECONDARY_SET,true));
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var stack=new net.minecraft.world.item.ItemStack(this);
        var entity=builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if(entity instanceof FilterBlockEntity) stack.getOrCreateTag().put("BlockEntityTag",entity.saveWithoutMetadata());
        return java.util.List.of(stack);
    }
}
