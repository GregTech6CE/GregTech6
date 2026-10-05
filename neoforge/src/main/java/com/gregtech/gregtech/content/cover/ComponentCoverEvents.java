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
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class ComponentCoverEvents {
    private ComponentCoverEvents() {}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if(!(event.getLevel().getBlockEntity(event.getPos()) instanceof PanelCoverHost host))return;
        if(ComponentCoverFallback.fallbackClass(host.coverOwner())&&!ComponentCoverFallback.eligible(host.coverOwner()))return;
        var result=ComponentCoverInteraction.use(host,event.getEntity(),event.getHand(),event.getHitVec());
        if(result==InteractionResult.PASS)result=PanelCoverInteraction.use(host,event.getEntity(),event.getHand(),event.getHitVec(),true);
        if(result!=InteractionResult.PASS){event.setCanceled(true);event.setCancellationResult(result);}
    }
}
