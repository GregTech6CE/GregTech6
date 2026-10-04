package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.GregTech;
import net.minecraft.core.Direction;
import net.minecraft.tags.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** GT6 Behavior_Place_Sapling followed by Behavior_Place_Workbench. */
public final class BehaviorPlaceWoodworkingSupplies {
    private static final TagKey<Item> GT_SAPLINGS=ItemTags.create(GregTech.id("saplings"));
    private static final TagKey<Item> WORKBENCHES=ItemTags.create(GregTech.id("crafting_workbenches"));
    private BehaviorPlaceWoodworkingSupplies() {}
    public static InteractionResult use(UseOnContext context) {
        var player=context.getPlayer();var level=context.getLevel();
        if(level.isClientSide||player==null||!ItemBehaviors.mayEdit(level,player,context.getClickedPos())) return InteractionResult.PASS;
        if(context.getClickedFace()==Direction.UP) {
            var planted=attempt(context,true);
            if(planted.consumesAction())return planted;
        }
        return excludesWorkbench(level.getBlockState(context.getClickedPos())) ? InteractionResult.PASS : attempt(context,false);
    }
    private static boolean excludesWorkbench(BlockState state) {
        var block=state.getBlock();
        return state.is(BlockTags.MINEABLE_WITH_AXE)||state.is(BlockTags.LEAVES)||state.is(BlockTags.LOGS)
                ||block instanceof BushBlock||block instanceof GrowingPlantBlock||block instanceof VineBlock
                ||block instanceof CactusBlock||block instanceof LeavesBlock||block instanceof PumpkinBlock||block==Blocks.MELON;
    }
    private static InteractionResult attempt(UseOnContext context,boolean sapling) {
        var player=context.getPlayer();var level=context.getLevel();
        for(int slot=player.getInventory().items.size()-1;slot>=0;slot--) {
            var source=player.getInventory().items.get(slot);
            if(source.isEmpty()||!(source.getItem() instanceof BlockItem))continue;
            if(sapling ? !(source.is(ItemTags.SAPLINGS)||source.is(GT_SAPLINGS)) : !source.is(WORKBENCHES))continue;
            var copy=source.copy();
            var hit=new BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside());
            var placement=new UseOnContext(level,player,context.getHand(),copy,hit);
            var target=new BlockPlaceContext(placement).getClickedPos();
            if(!level.hasChunkAt(target)||!ItemBehaviors.mayEdit(level,player,target))return InteractionResult.PASS;
            // ItemStack.useOn retains Forge snapshots, placement events and cancellation rollback.
            var result=copy.useOn(placement);
            if(!result.consumesAction())return InteractionResult.PASS;
            if(!player.getAbilities().instabuild)source.shrink(Math.max(0,source.getCount()-copy.getCount()));
            player.getInventory().setChanged();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
    public static net.minecraft.world.InteractionResult placeTorch(net.minecraft.world.item.context.UseOnContext context,boolean usable){var player=context.getPlayer();if(player==null||!usable)return net.minecraft.world.InteractionResult.PASS;for(int slot=player.getInventory().getContainerSize()-1;slot>=0;slot--){var item=player.getInventory().getItem(slot);if(!item.is(net.minecraft.world.item.Items.TORCH))continue;var copy=item.copyWithCount(1);var placement=new net.minecraft.world.item.context.BlockPlaceContext(context.getLevel(),player,context.getHand(),copy,new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside()));var result=((net.minecraft.world.item.BlockItem)item.getItem()).place(placement);if(result.consumesAction()&&!context.getLevel().isClientSide&&!player.getAbilities().instabuild)item.shrink(1);return result;}return net.minecraft.world.InteractionResult.PASS;}
}
