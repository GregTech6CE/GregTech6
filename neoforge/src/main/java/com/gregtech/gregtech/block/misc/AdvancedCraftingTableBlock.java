package com.gregtech.gregtech.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;

/** GT6 Advanced Crafting Table — crafting with inventory cache and recipe memory. */
public class AdvancedCraftingTableBlock extends HorizontalDirectionalBlock implements EntityBlock, com.gregtech.gregtech.api.tool.ToolInteractionTarget {
    @Override protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    private final com.gregtech.gregtech.api.material.GTMaterial material;
    public com.gregtech.gregtech.api.material.GTMaterial material(){return material;}
    public AdvancedCraftingTableBlock(com.gregtech.gregtech.api.material.GTMaterial material,Properties properties) {
        super(properties);
        this.material=material;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AdvancedCraftingTableBlockEntity(pos, state); }

    @Override public com.gregtech.gregtech.api.tool.ToolInteractionSpec toolInteraction(BlockState state,net.minecraft.world.item.ItemStack tool) {
        return com.gregtech.gregtech.api.tool.GTToolHelper.isMachineWrench(tool)?com.gregtech.gregtech.api.tool.ToolInteractionSpec.facing(FACING,com.gregtech.gregtech.block.machine.MachineRotationType.HORIZONTAL):null;
    }
    public net.minecraft.world.InteractionResult interact(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,
            net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        if(hand!=net.minecraft.world.InteractionHand.MAIN_HAND||!(level.getBlockEntity(pos) instanceof AdvancedCraftingTableBlockEntity table))return net.minecraft.world.InteractionResult.PASS;
        var tool=player.getItemInHand(hand);
        if(com.gregtech.gregtech.api.tool.GTToolHelper.isMonkeyWrench(tool)||com.gregtech.gregtech.api.tool.GTToolHelper.isScrewdriver(tool)) {
            if(!level.isClientSide&&player.mayBuild()&&level.mayInteract(player,pos)) {
                boolean upper=hit.getDirection()==Direction.UP;
                boolean filter=com.gregtech.gregtech.api.tool.GTToolHelper.isScrewdriver(tool);
                table.toggleMode(upper,filter);com.gregtech.gregtech.api.tool.GTToolHelper.damageForToolClickReturn(tool,10000,player);
                boolean enabled=(table.modes()&(filter?(upper?4:8):(upper?1:2)))!=0;
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(filter?"message.gregtech.crafting.filter":"message.gregtech.crafting.access",upper?"4×4":"9×4",
                        net.minecraft.network.chat.Component.translatable((filter?enabled:!enabled)?"options.on":"options.off")),true);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(com.gregtech.gregtech.api.tool.ToolInteractions.use(state,level,pos,player,hand,hit))return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        boolean upper=hit.getDirection()==Direction.UP;
        if(!upper&&hit.getDirection().getAxis()!=state.getValue(FACING).getAxis())return net.minecraft.world.InteractionResult.PASS;
        if(!level.isClientSide)player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id,inv,p)->upper?new com.gregtech.gregtech.client.gui.AdvancedCraftingMenu(id,inv,table):table.storageMenu(id,inv),net.minecraft.network.chat.Component.translatable(getDescriptionId())));
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held,BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override public net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
    @Override public void onRemove(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof AdvancedCraftingTableBlockEntity table)table.dropContents();
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip,net.minecraft.world.item.TooltipFlag flag) {
        com.gregtech.gregtech.client.StorageBlockTooltips.craftingTable(tooltip);
    }
}
