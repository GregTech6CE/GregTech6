package com.gregtech.gregtech.content.energy;
import java.util.*;
/** Whole signed LU packets, loaded-only BFS, cycle protection and actual accepted-path metering. */
public final class LaserPacketRouter {private LaserPacketRouter(){}public static final int MAX_VISITED=4096;
 public interface Network<N>{N neighbor(N node,int side);boolean loaded(N node);boolean fiber(N node);boolean connected(N node,int side);boolean accepts(N node,int side);long inject(N node,int side,long size,long amount,boolean execute);void record(N node,long size,long accepted);}
 public static <N> long route(N root,int inputSide,long size,long amount,boolean execute,Network<N> graph){
  if(size==0||size==Long.MIN_VALUE||amount<=0)return 0;
  Set<N> visited=new HashSet<>();ArrayDeque<N> queue=new ArrayDeque<>();Map<N,N> parents=new HashMap<>();visited.add(root);if(inputSide>=0)visited.add(graph.neighbor(root,inputSide));queue.add(root);long used=0;
  while(!queue.isEmpty()&&used<amount&&visited.size()<MAX_VISITED){N wire=queue.removeFirst();for(int output=0;output<6;output++){
   if(!graph.connected(wire,output))continue;N next=graph.neighbor(wire,output);if(!graph.loaded(next)||visited.contains(next))continue;
   if(graph.fiber(next)){if(graph.connected(next,output^1)){visited.add(next);parents.put(next,wire);queue.addLast(next);}}
   else if(graph.accepts(next,output^1)){visited.add(next);long accepted=Math.max(0,Math.min(amount-used,graph.inject(next,output^1,size,amount-used,execute)));used+=accepted;
    if(execute&&accepted>0)for(N path=wire;path!=null;path=parents.get(path))graph.record(path,size,accepted);if(used>=amount)break;}
   if(visited.size()>=MAX_VISITED)break;
  }}return used;
 }
 public static long recordedUnits(long current,long size,long packets){long magnitude=Math.abs(size);long units=packets>Long.MAX_VALUE/magnitude?Long.MAX_VALUE:packets*magnitude;return units>Long.MAX_VALUE-current?Long.MAX_VALUE:current+units;}
}
