/**
 * Copyright (c) 2021 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package com.gregtech.gregtech.block.misc;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;

public class LongDistEndpointBlock extends DirectionalBlock implements EntityBlock,com.gregtech.gregtech.api.tool.ToolInteractionTarget,com.gregtech.gregtech.api.machine.GTMachineBlock {
    private final boolean fluid; public boolean isFluid(){return fluid;}

    public LongDistEndpointBlock(boolean fluid,Properties properties){super(properties);this.fluid=fluid;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));}
    public com.gregtech.gregtech.api.material.GTMaterial material(){return com.gregtech.gregtech.api.material.GTMaterialRegistry.get((fluid?"Tungsten":"Platinum"));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new LongDistEndpointBlockEntity(pos,state);}


    @Override public com.gregtech.gregtech.api.tool.ToolInteractionSpec toolInteraction(BlockState state,net.minecraft.world.item.ItemStack tool) {
        return com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(tool,com.gregtech.gregtech.api.tool.GTToolType.WRENCH)
                ? com.gregtech.gregtech.api.tool.ToolInteractionSpec.facing(FACING,com.gregtech.gregtech.block.machine.MachineRotationType.ALL) : null;
    }
    @Override public void toolStateChanged(net.minecraft.world.level.Level level,BlockPos pos,BlockState state) {
        LongDistanceTopology.changed(level);
        if(level.getBlockEntity(pos) instanceof LongDistEndpointBlockEntity endpoint) endpoint.invalidateRoute();
        if(level.getBlockEntity(pos) instanceof LongDistanceTransformerBlockEntity endpoint) endpoint.invalidateRoute();
    }
    public net.minecraft.world.InteractionResult interact(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,
            net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        var tool=player.getItemInHand(hand);
        if(!player.mayBuild() || !level.mayInteract(player,pos)) return net.minecraft.world.InteractionResult.PASS;
        if(com.gregtech.gregtech.api.tool.GTToolHelper.isSoftHammer(tool) || com.gregtech.gregtech.api.tool.GTToolHelper.isMagnifyingGlass(tool)) {
            if(!level.isClientSide) {
                boolean reset=com.gregtech.gregtech.api.tool.GTToolHelper.isSoftHammer(tool);
                if(level.getBlockEntity(pos) instanceof LongDistEndpointBlockEntity endpoint) {if(reset)endpoint.rescan();else endpoint.describe(player);}
                if(level.getBlockEntity(pos) instanceof LongDistanceTransformerBlockEntity endpoint) {if(reset)endpoint.rescan();else endpoint.describe(player);}
                com.gregtech.gregtech.api.tool.GTToolHelper.damageForToolClickReturn(tool,reset?10000:1,player);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        var spec=toolInteraction(state,tool);
        boolean same=spec!=null&&state.getValue(FACING)==com.gregtech.gregtech.api.tool.ToolInteractions.selectedFace(hit);
        boolean allowed=spec!=null&&com.gregtech.gregtech.api.tool.ToolInteractions.canApply(spec,state,level,pos,player,tool,com.gregtech.gregtech.api.tool.ToolInteractions.selectedFace(hit));
        if(com.gregtech.gregtech.api.tool.ToolInteractions.use(state,level,pos,player,hand,hit)) {
            if(allowed&&!level.isClientSide&&!(tool.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem))
                com.gregtech.gregtech.api.tool.GTToolHelper.damageForToolClickReturn(tool,same?10000:9900,player);
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.InteractionResult.PASS;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var player=context.getPlayer();
        int side=com.gregtech.gregtech.content.logistics.LongDistanceNetwork.placement(player==null?0:player.getXRot(),context.getHorizontalDirection().getOpposite().ordinal());
        return defaultBlockState().setValue(FACING,Direction.values()[side]);
    }

    @Override public net.minecraft.world.InteractionResult use(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,hand,hit);}


    @Override public void onPlace(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState previous,boolean moving){
        if(!state.is(previous.getBlock()) || state.getValue(FACING)!=previous.getValue(FACING))LongDistanceTopology.changed(level);super.onPlace(state,level,pos,previous,moving);
    }
    @Override public void onRemove(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState next,boolean moving){
        if(!state.is(next.getBlock()) || state.getValue(FACING)!=next.getValue(FACING))LongDistanceTopology.changed(level);super.onRemove(state,level,pos,next,moving);
    }
}
