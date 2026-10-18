package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** The isolated 1.21 probe must finish asynchronous generation before initiating shutdown. */
final class WorldSaveBarrier {
    private static CompletableFuture<Integer> pending;
    static boolean ready(MinecraftServer server,JsonObject receipt){
        if(pending==null){
            pending=server.submit(()->{
                var map=server.overworld().getChunkSource().chunkMap;
                map.runGenerationTasks();
                int waiting=0;
                try { for(String name:new String[]{"updatingChunkMap","pendingUnloads"}){
                    var field=map.getClass().getDeclaredField(name);field.setAccessible(true);
                    var holders=(Map<?,?>)field.get(map);
                    for(var value:holders.values())if(!((ChunkHolder)value).isReadyForSaving())waiting++;
                }
                } catch(ReflectiveOperationException failure){throw new IllegalStateException("Native world-save barrier unavailable",failure);}
                return waiting;
            });
            return false;
        }
        if(!pending.isDone())return false;
        int waiting=pending.join();pending=null;
        receipt.addProperty("generationHoldersWaitingBeforeExit",waiting);
        return waiting==0;
    }
}
