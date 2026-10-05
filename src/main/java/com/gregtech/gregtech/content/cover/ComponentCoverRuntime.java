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

/* Adapted from Gregorius Techneticies' component covers, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.cover;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

/** Native storage/capability boundary for the shared original component-cover rules. */
public final class ComponentCoverRuntime {
    public static final String VISUAL = "gt.cover.visual", SLOT = "gt.cover.slot";
    private ComponentCoverRuntime() {}
    public static ComponentCoverRules.Kind kind(ItemStack stack) { return ComponentCoverRules.kind(CoverItems.behavior(stack)); }
    public static int visual(ItemStack stack) { return CoverStackData.readOrEmpty(stack).getInt(VISUAL); }
    public static int slot(ItemStack stack) { return CoverStackData.readOrEmpty(stack).getInt(SLOT); }
    public static boolean pipe(PanelCoverHost host) {
        return host.coverOwner() instanceof com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity
                || host.coverOwner() instanceof com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity;
    }
    public static boolean canAttach(PanelCoverHost host, Direction side, ItemStack stack) {
        var kind = kind(stack);
        if (kind == null) return true;
        var fluid = host.componentFluids(side);
        var items = host.componentItems(side);
        return ComponentCoverRules.canAttach(kind, host.componentTicks(),
                fluid != null && fluid.getTanks() > 0, items != null && items.getSlots() > 0);
    }
    public static void attached(PanelCoverHost host, Direction side) {
        var stack = host.getCover(side);
        var kind = kind(stack);
        if (kind != null && pipe(host)) {
            CoverStackData.putInt(stack, VISUAL, 1);
            if (kind == ComponentCoverRules.Kind.ROBOT_ARM && slot(stack) >= 0)
                CoverStackData.putInt(stack, SLOT, -1 - slot(stack));
        }
    }
    public static boolean allowsItem(ItemStack stack, boolean insert) {
        var kind = kind(stack);
        return kind != ComponentCoverRules.Kind.CONVEYOR && kind != ComponentCoverRules.Kind.ROBOT_ARM
                || (insert ? ComponentCoverRules.allowsInsert(visual(stack)) : ComponentCoverRules.allowsExtract(visual(stack)));
    }
    public static boolean allowsFluid(ItemStack stack, boolean fill) {
        return kind(stack) != ComponentCoverRules.Kind.PUMP
                || (fill ? ComponentCoverRules.allowsInsert(visual(stack)) : ComponentCoverRules.allowsExtract(visual(stack)));
    }
    public static void tick(PanelCoverHost host, Direction side, long serverTime) {
        var stack = host.getCover(side);
        var kind = kind(stack);
        if (kind == null || !ComponentCoverRules.due(kind, CoverItems.tierIndex(stack), serverTime, host.panels().stopped())) return;
        var owner = host.coverOwner();var level = owner.getLevel();var next = owner.getBlockPos().relative(side);
        if (level == null || level.isClientSide || owner.isRemoved() || !level.hasChunkAt(next)) return;
        var neighbor = level.getBlockEntity(next);
        if (neighbor instanceof com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity part
                && part.isBoundTo(owner.getBlockPos())) return;
        boolean input = visual(stack) != 0;
        if (kind == ComponentCoverRules.Kind.PUMP) {
            var own = host.componentFluids(side);
            var adjacent = fluid(neighbor, side.getOpposite());
            if (own != null && adjacent != null)
                moveFluid(input ? adjacent : own, input ? own : adjacent, CoverItems.pumpThroughput(stack));
        } else {
            var own = host.componentItems(side);
            var adjacent = items(neighbor, side.getOpposite());
            if (own != null && adjacent != null) {
                int source = kind == ComponentCoverRules.Kind.ROBOT_ARM ? ComponentCoverRules.sourceSlot(slot(stack)) : -1;
                int target = kind == ComponentCoverRules.Kind.ROBOT_ARM ? ComponentCoverRules.targetSlot(slot(stack)) : -1;
                moveItem(owner, input ? adjacent : own, input ? own : adjacent, source, target);
            }
        }
    }
    private static IFluidHandler fluid(BlockEntity entity, Direction side) {
        return entity == null ? null : entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
    }
    private static IItemHandler items(BlockEntity entity, Direction side) {
        return entity == null ? null : entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
    }
    private static void moveFluid(IFluidHandler from, IFluidHandler to, int budget) {
        // A cover operation may move different channels, sharing one throughput budget.
        for (int tank = 0; tank < from.getTanks() && budget > 0; tank++) {
            var stored = from.getFluidInTank(tank);
            if (stored.isEmpty()) continue;
            var offer = stored.copy();offer.setAmount(Math.min(budget, stored.getAmount()));
            var available = from.drain(offer, IFluidHandler.FluidAction.SIMULATE);
            int accepted = to.fill(available, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            available.setAmount(Math.min(accepted, available.getAmount()));
            var moved = from.drain(available, IFluidHandler.FluidAction.EXECUTE);
            int filled = to.fill(moved, IFluidHandler.FluidAction.EXECUTE);
            if (filled < moved.getAmount()) {
                var rest = moved.copy();rest.setAmount(moved.getAmount() - filled);
                from.fill(rest, IFluidHandler.FluidAction.EXECUTE);
            }
            budget -= filled;
        }
    }
    private static ItemStack insert(IItemHandler to, ItemStack stack, int slot, boolean simulate) {
        if (slot >= 0) return slot < to.getSlots() ? to.insertItem(slot, stack, simulate) : stack;
        for (int i = 0; i < to.getSlots() && !stack.isEmpty(); i++) stack = to.insertItem(i, stack, simulate);
        return stack;
    }
    private static void moveItem(BlockEntity owner, IItemHandler from, IItemHandler to, int source, int target) {
        if (source >= from.getSlots() || target >= to.getSlots()) return;
        int start = source < 0 ? 0 : source, end = source < 0 ? from.getSlots() : source + 1;
        for (int slot = start; slot < end; slot++) {
            var available = from.extractItem(slot, 64, true);
            if (available.isEmpty()) continue;
            var rest = insert(to, available.copy(), target, true);
            int accepted = available.getCount() - rest.getCount();
            if (accepted <= 0) continue;
            var extracted = from.extractItem(slot, accepted, false);
            if (extracted.isEmpty()) continue;
            rest = insert(to, extracted, target, false);
            if (!rest.isEmpty()) {
                rest = insert(from, rest, slot, false);
                if (!rest.isEmpty()) com.gregtech.gregtech.api.inventory.BlockContents.drop(owner, rest);
            }
            return; // Original conveyor/arm moves one group per operation.
        }
    }
}
