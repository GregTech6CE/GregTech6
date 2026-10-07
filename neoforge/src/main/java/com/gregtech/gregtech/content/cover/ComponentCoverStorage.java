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
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Cover state for native GT inventories/tanks which previously lacked cover slots. */
public final class ComponentCoverStorage {
    private final PanelCoverHost host;
    private ItemStack[] covers;
    private final PanelCoverRuntime panels;
    public ComponentCoverStorage(PanelCoverHost host) { this.host=host;panels=new PanelCoverRuntime(host); }
    public PanelCoverRuntime panels() { return panels; }
    public boolean hasCovers() { if(covers!=null)for(var stack:covers)if(!stack.isEmpty())return true;return false; }
    public ItemStack get(Direction side) { return covers==null?ItemStack.EMPTY:covers[side.ordinal()]; }
    public boolean attach(Direction side, ItemStack stack) {
        if (stack.isEmpty() || !get(side).isEmpty() || !panels.canAttach(side,stack)
                || !CoverItems.isCover(stack)) return false;
        if(covers==null){covers=new ItemStack[6];java.util.Arrays.fill(covers,ItemStack.EMPTY);}
        covers[side.ordinal()]=stack.copyWithCount(1);panels.attached(side);return true;
    }
    public ItemStack remove(Direction side) {
        var stack=get(side);if(stack.isEmpty())return stack;
        covers[side.ordinal()]=ItemStack.EMPTY;return panels.removed(side,stack);
    }
    public void tick() {
        if(covers==null)return;
        panels.beforeTick();
        for(var side:Direction.values())ComponentCoverRuntime.tick(host,side,host.coverOwner().getLevel().getGameTime());
        panels.afterTick();
    }
    public void drop() {
        if(covers==null)return;
        if(CoverDrops.retained(host.coverOwner())){java.util.Arrays.fill(covers,ItemStack.EMPTY);return;}
        for(var side:Direction.values())com.gregtech.gregtech.api.inventory.BlockContents.drop(host.coverOwner(),remove(side));
    }
    public void save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        if(covers==null)return;
        for(var side:Direction.values())if(!get(side).isEmpty())tag.put("gt_cover_"+side.ordinal(),get(side).save(lookup));
    }
    public void load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        for(var side:Direction.values()) {
            var key="gt_cover_"+side.ordinal();
            if(tag.contains(key)){if(covers==null){covers=new ItemStack[6];java.util.Arrays.fill(covers,ItemStack.EMPTY);}covers[side.ordinal()]=ItemStack.parseOptional(lookup,tag.getCompound(key));}
            else if(covers!=null)covers[side.ordinal()]=ItemStack.EMPTY;
        }
        panels.loaded();
    }
}
