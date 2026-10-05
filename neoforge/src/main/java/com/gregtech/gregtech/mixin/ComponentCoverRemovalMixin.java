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

package com.gregtech.gregtech.mixin;

import com.gregtech.gregtech.content.cover.ComponentCoverFallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Break covers using chunk-local state; unloading must never request a world chunk. */
@Mixin(LevelChunk.class)
public abstract class ComponentCoverRemovalMixin {
    @Inject(method="setBlockState",at=@At("HEAD"),require=1)
    private void gregtech$break(BlockPos pos,BlockState next,boolean moved,CallbackInfoReturnable<BlockState> cir){
        var owner=((LevelChunk)(Object)this).getBlockEntities().get(pos);
        if(owner!=null&&!next.is(owner.getBlockState().getBlock()))ComponentCoverFallback.breaking(owner);
    }
}
