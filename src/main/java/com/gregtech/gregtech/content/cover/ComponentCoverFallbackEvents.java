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
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class ComponentCoverFallbackEvents {
    private ComponentCoverFallbackEvents() {}
    private record Initial(net.minecraft.server.level.ServerPlayer player,ServerLevel level,net.minecraft.world.level.ChunkPos chunk,long tick) {}
    private static final java.util.List<Initial> INITIAL=new java.util.ArrayList<>();
    @SubscribeEvent public static void watched(ChunkWatchEvent.Watch event){INITIAL.add(new Initial(event.getPlayer(),event.getLevel(),event.getPos(),event.getLevel().getGameTime()+1));}
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level))return;
        ComponentCoverFallback.tick(level);
        var iterator=INITIAL.iterator();while(iterator.hasNext()){
            var pending=iterator.next();if(pending.level()!=level)continue;
            if(pending.player().isRemoved()||pending.player().serverLevel()!=level){iterator.remove();continue;}
            if(pending.tick()>level.getGameTime())continue;
            iterator.remove();if(!pending.player().isRemoved())ComponentCoverFallback.syncChunk(pending.player(),pending.chunk());
        }
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){INITIAL.clear();}
}
