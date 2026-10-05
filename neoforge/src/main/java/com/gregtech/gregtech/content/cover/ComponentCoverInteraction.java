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

package com.gregtech.gregtech.content.cover;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/** Called before host tools, so a cover owns the pointed face. */
public final class ComponentCoverInteraction {
    private ComponentCoverInteraction() {}
    public static InteractionResult use(PanelCoverHost host, Player player, InteractionHand hand, BlockHitResult hit) {
        var owner=host.coverOwner();var level=owner.getLevel();var side=hit.getDirection();
        if(level==null)return InteractionResult.PASS;
        var held=player.getItemInHand(hand);var stack=host.getCover(side);
        var kind=ComponentCoverRuntime.kind(stack);
        if(ComponentCoverRuntime.kind(held)!=null) {
            if(!player.mayBuild()||!level.mayInteract(player,owner.getBlockPos()))return InteractionResult.FAIL;
            if(!ComponentCoverRuntime.canAttach(host,side,held)||!stack.isEmpty())return InteractionResult.FAIL;
            if(!level.isClientSide) {
                if(!host.attachCover(side,held))return InteractionResult.FAIL;
                if(!player.getAbilities().instabuild)held.shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(kind==null)return InteractionResult.PASS;
        if(!GTToolHelper.isInteractionTool(held))return InteractionResult.PASS;
        if(!player.mayBuild()||!level.mayInteract(player,owner.getBlockPos()))return InteractionResult.FAIL;
        if(GTToolHelper.matchesTool(held,GTToolType.CROWBAR)) {
            if(!level.isClientSide) {
                var removed=host.removeCover(side);
                if(!removed.isEmpty()) {
                    if(!player.addItem(removed))player.drop(removed,false);
                    GTToolHelper.damageForUse(held,1,player);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!level.isClientSide) {
            boolean screw=GTToolHelper.matchesTool(held,GTToolType.SCREWDRIVER);
            boolean monkey=GTToolHelper.matchesTool(held,GTToolType.MONKEY_WRENCH);
            boolean glass=GTToolHelper.matchesTool(held,GTToolType.MAGNIFYING_GLASS);
            int cost=0;
            if(kind==ComponentCoverRules.Kind.ROBOT_ARM) {
                if(monkey) {
                    CoverStackData.putInt(stack,ComponentCoverRuntime.VISUAL,ComponentCoverRules.toggle(ComponentCoverRuntime.visual(stack),ComponentCoverRuntime.pipe(host)));
                    if(ComponentCoverRuntime.pipe(host)&&ComponentCoverRuntime.slot(stack)>=0)
                        CoverStackData.putInt(stack,ComponentCoverRuntime.SLOT,-1-ComponentCoverRuntime.slot(stack));
                    cost=10;
                } else if(screw) {
                    CoverStackData.putInt(stack,ComponentCoverRuntime.SLOT,ComponentCoverRules.slotStep(ComponentCoverRuntime.slot(stack),player.isShiftKeyDown(),ComponentCoverRuntime.pipe(host)));
                    cost=2;
                } else if(glass)cost=1;
                if(screw||glass) {
                    int slot=ComponentCoverRuntime.slot(stack);
                    player.displayClientMessage(Component.literal(slot<0?"Takes from Slot: "+(-1-slot):"Puts into Slot: "+slot),false);
                }
            } else if(screw) {
                CoverStackData.putInt(stack,ComponentCoverRuntime.VISUAL,ComponentCoverRules.toggle(ComponentCoverRuntime.visual(stack),ComponentCoverRuntime.pipe(host)));
                cost=10;
            }
            if(cost>0){GTToolHelper.damageForUse(held,cost,player);host.panels().changed();}
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
