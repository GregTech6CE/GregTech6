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

package gregtech.content.cover;
import net.minecraft.world.InteractionResult;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

//? if >=1.21.1 {
/*@EventBusSubscriber(modid=gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid=gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class ComponentCoverEvents {
    private ComponentCoverEvents() {}
    /** A consumed cover action cancels the native block/item callback below this event. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof PanelCoverHost host)) return;
        var held = event.getEntity().getItemInHand(event.getHand());
        boolean crowbar = gregtech.engine.tool.GTToolHelper.matchesTool(held,
                gregtech.engine.tool.GTToolType.CROWBAR);
        var hit = event.getHitVec();
        if ((event.getEntity().isShiftKeyDown() && CoverItems.isCover(held)) || crowbar)
            hit = hit.withDirection(gregtech.engine.tool.ToolInteractions.selectedFace(hit));
        boolean recoverStoredCover = (host.coverOwner() instanceof gregtech.content.blockentity.misc.PileBlockEntity
                || host.coverOwner() instanceof gregtech.content.blockentity.tool.ProcessingToolBlockEntity)
                && crowbar && !host.getCover(hit.getDirection()).isEmpty();
        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                && host.coverOwner() instanceof gregtech.content.blockentity.tool.ProcessingToolBlockEntity tool
                && tool.handlesHandInput(held) && !recoverStoredCover) {
            // Manual storage accepts items without recipes too. In particular, sneaking
            // with a BlockItem must use the vessel instead of placing an adjacent block.
//? if >=1.21.1 {
            /*if (event.getUseBlock() == net.neoforged.neoforge.common.util.TriState.FALSE) return;
            event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
*///?} else {
            if (event.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.DENY) return;
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
//?}
            return;
        }
        if (host.coverOwner() instanceof gregtech.content.blockentity.misc.PileBlockEntity pile
                && pile.canAccept(held)) {
            // Allow the normal pile callback while sneaking, without bypassing protection events.
//? if >=1.21.1 {
            /*event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
*///?} else {
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
//?}
            return;
        }
        if (ComponentCoverFallback.fallbackClass(host.coverOwner())
                && !ComponentCoverFallback.eligible(host.coverOwner()) && !recoverStoredCover) return;
        var result = host instanceof gregtech.content.logistics.LogisticsCoverHost logistics
                ? gregtech.content.logistics.LogisticsCoverInteraction.use(logistics,
                        event.getLevel(), event.getEntity(), event.getHand(), hit.getDirection())
                : InteractionResult.PASS;
        if (result == InteractionResult.PASS)
            result = ComponentCoverInteraction.use(host, event.getEntity(), event.getHand(), hit);
        if (result == InteractionResult.PASS)
            result = UtilityCoverInteraction.use(host, event.getEntity(), event.getHand(), hit);
        if (result == InteractionResult.PASS)
            result = PanelCoverInteraction.use(host, event.getEntity(), event.getHand(), hit, true);
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
