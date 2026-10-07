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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.*;

/** Lifecycle of cover slots on ticking GT entities which predate PanelCoverHost. */
public final class ComponentCoverFallback {
    private ComponentCoverFallback() {}
    private static final Map<BlockEntity,Boolean> ACTIVE=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<BlockEntity,CompoundTag> SENT=Collections.synchronizedMap(new WeakHashMap<>());
    private static final ClassValue<Boolean> FALLBACK=new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type){
            try{return type.getMethod("getCover",Direction.class).getDeclaringClass()==BlockEntity.class;}
            catch(NoSuchMethodException failure){return false;}
        }
    };
    public static boolean ownClass(BlockEntity owner){
        if(owner==null)return false;
        for(Class<?> type=owner.getClass();type!=BlockEntity.class&&type!=null;type=type.getSuperclass())
            if(type.getName().startsWith("com.gregtech.gregtech.blockentity."))return true;
        return false;
    }
    public static boolean fallbackClass(BlockEntity owner){return owner instanceof FallbackCoverHost&&FALLBACK.get(owner.getClass());}
    public static boolean uses(BlockEntity owner){return ownClass(owner)&&owner instanceof FallbackCoverHost&&FALLBACK.get(owner.getClass());}
    @SuppressWarnings({"unchecked","rawtypes"})
    public static boolean eligible(BlockEntity owner){
        if(!ownClass(owner)||owner.getLevel()==null||owner.isRemoved())return false;
        // The active fallback service provides cover ticks even for otherwise passive GT storage.
        return owner.getBlockState().getBlock() instanceof net.minecraft.world.level.block.EntityBlock;
    }
    public static boolean hasLogistics(BlockEntity owner){
        if(owner instanceof com.gregtech.gregtech.content.logistics.LogisticsCoverHost host)for(var side:Direction.values())if(!host.logisticsCovers().get(side).isEmpty())return true;
        return false;
    }
    public static List<BlockEntity> active(){synchronized(ACTIVE){return new ArrayList<>(ACTIVE.keySet());}}
    public static void track(BlockEntity owner){if(uses(owner))ACTIVE.put(owner,true);}
    public static void write(BlockEntity owner,CompoundTag tag){
        CoverDrops.saveRuntime(owner,tag);
        if(owner instanceof com.gregtech.gregtech.content.logistics.LogisticsCoverHost logistics)logistics.logisticsCovers().save(tag);
        if(!uses(owner))return;
        var storage=((FallbackCoverHost)owner).gregtechComponentStorage(false);
        if(storage!=null){tag.putBoolean("gt.component_covers",true);storage.save(tag);}
    }
    /** Harvested GT block items retain the native attachment data. */
    public static CompoundTag forItem(BlockEntity owner,CompoundTag tag){
        // Native harvested block data retains attached covers and their configuration.
        return tag;
    }
    public static void read(BlockEntity owner,CompoundTag tag){
        if(tag==null)return;CoverDrops.loadRuntime(owner,tag);
        if(owner instanceof com.gregtech.gregtech.content.logistics.LogisticsCoverHost logistics)logistics.logisticsCovers().load(tag);
        if(!uses(owner))return;
        boolean present=false;for(var side:Direction.values())present|=tag.contains("gt_cover_"+side.ordinal());
        if(!present&&!tag.getBoolean("gt.component_covers"))return;
        var host=(FallbackCoverHost)owner;var storage=host.gregtechComponentStorage(present);
        if(storage==null)return;storage.load(tag);
        if(storage.hasCovers()||hasLogistics(owner))track(owner);else ACTIVE.remove(owner);
    }
    public static void tick(ServerLevel level){
        for(var owner:active()){
            if(owner.getLevel()!=level)continue;
            if(owner.isRemoved()){ACTIVE.remove(owner);SENT.remove(owner);continue;}
            var pos=owner.getBlockPos();var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if(chunk==null)continue;
            if(chunk.getBlockEntities().get(pos)!=owner){removing(owner);continue;}
            var storage=((FallbackCoverHost)owner).gregtechComponentStorage(false);
            if(storage==null)continue;
            if(eligible(owner)&&level.hasChunkAt(owner.getBlockPos()))storage.tick();
            var tag=new CompoundTag();write(owner,tag);
            if(!tag.equals(SENT.get(owner))){
                SENT.put(owner,tag.copy());
                var packet=com.gregtech.gregtech.network.PacketSyncComponentCovers.of(owner,tag);
                for(var player:level.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(owner.getBlockPos()),false))
                    com.gregtech.gregtech.network.PacketSyncComponentCovers.send(player,packet);
            }
            if(!storage.hasCovers()&&!hasLogistics(owner)){ACTIVE.remove(owner);SENT.remove(owner);}
        }
    }
    public static void removing(BlockEntity owner){ACTIVE.remove(owner);SENT.remove(owner);}
    /** Called only by a chunk's block replacement path, never by chunk unloading. */
    public static void breaking(BlockEntity owner){
        if(!uses(owner)||owner.getLevel()==null||owner.getLevel().isClientSide)return;
        var storage=((FallbackCoverHost)owner).gregtechComponentStorage(false);
        if(storage!=null)storage.drop();
        removing(owner);
    }
    public static void syncChunk(net.minecraft.server.level.ServerPlayer player,net.minecraft.world.level.ChunkPos chunk){
        for(var owner:active())if(owner.getLevel()==player.serverLevel()&&!owner.isRemoved()&&new net.minecraft.world.level.ChunkPos(owner.getBlockPos()).equals(chunk)){
            var tag=new CompoundTag();write(owner,tag);
            com.gregtech.gregtech.network.PacketSyncComponentCovers.send(player,com.gregtech.gregtech.network.PacketSyncComponentCovers.of(owner,tag));
        }
    }
}
