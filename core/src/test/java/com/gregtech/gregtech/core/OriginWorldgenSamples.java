package com.gregtech.gregtech.core;

import com.gregtech.gregtech.worldgen.center.*;
import java.util.*;

/** Small source-pinned placement samples; no Minecraft world, client or server launch. */
final class OriginWorldgenSamples {
    private record Pos(int x,int y,int z) {}
    private record Placed(String id,int meta,int slabSide) {}
    private static int assertions;
    private static void equal(Object expected,Object actual,String message) {
        assertions++; if (!Objects.equals(expected,actual)) throw new AssertionError(message+": "+actual+" != "+expected);
    }
    private static final class SampleWorld implements OriginWorld {
        final int minX,minZ;
        final boolean mountain,water;
        final Map<Pos,Placed> samples = new HashMap<>();
        final Map<Pos,List<String>> signs = new HashMap<>();
        int lowestWrite = Integer.MAX_VALUE;
        Pos spawn;
        SampleWorld(int x,int z,boolean mountain,boolean water) { minX=x;minZ=z;this.mountain=mountain;this.water=water; }
        void probe(int x,int y,int z) { samples.put(new Pos(x,y,z),null); }
        void block(int x,int y,int z,String id,int meta,int side) {
            equal(new Placed(id,meta,side),samples.get(new Pos(x,y,z)),"source position "+new Pos(x,y,z));
        }
        public int minY() { return -64; }
        public boolean canWrite(int x,int y,int z) { return x>=minX && x<minX+16 && z>=minZ && z<minZ+16 && y>=-64 && y<320; }
        public Block getBlock(int x,int y,int z) {
            // The source's mode sample at y=75 remains untouched by the tunnel's y=73 roof.
            return y<=64 || mountain ? new Block("minecraft:stone") : OriginSupport.NB;
        }
        public boolean setBlock(int x,int y,int z,Block block,int meta,int flags) {
            if (!canWrite(x,y,z)) return false;
            lowestWrite=Math.min(lowestWrite,y);
            Pos pos = new Pos(x,y,z);
            if (samples.containsKey(pos)) samples.put(pos,new Placed(block.id(),meta,block.slabSide()));
            return true;
        }
        public Biome getBiomeGenForCoords(int x,int z) { return new Biome(water?"ocean":"plains",water?"Ocean":"Plains"); }
        public boolean isInfiniteWaterBiome(String name) { return name.equals("ocean"); }
        public void setSpawnLocation(int x,int y,int z) { spawn=new Pos(x,y,z); }
        public void sign(int x,int y,int z,int side,int flags,String... lines) {
            if (canWrite(x,y,z)) signs.put(new Pos(x,y,z),List.of(lines));
        }
        public void beacon(int x,int y,int z,String primary,String secondary) { throw new AssertionError("optional beacon must remain disabled"); }
        public void clearNonPlayerEntities(int x,int y,int z,int xx,int yy,int zz) {}
    }
    static int verify() {
        assertions=0;
        SampleWorld nexus=new SampleWorld(16,-48,false,false);
        for (Pos p:List.of(new Pos(20,-63,-44),new Pos(20,66,-44),new Pos(20,79,-44),
                new Pos(16,75,-47),new Pos(17,67,-46),new Pos(19,67,-45),new Pos(28,67,-46),
                new Pos(22,71,-47),new Pos(23,68,-47))) nexus.probe(p.x,p.y,p.z);
        SourceNexus layout=new SourceNexus(66,true);
        equal(false,layout.generate(nexus,new OriginWorld.Chunk(nexus,16,-48),0,0,new Random(0)),"Nexus only at original anchor");
        equal(true,layout.generate(nexus,new OriginWorld.Chunk(nexus,16,-48),16,-48,new Random(0)),"source Nexus generated");
        nexus.block(20,-63,-44,"gregtech:concrete",8,-1);
        nexus.block(20,66,-44,"minecraft:obsidian",0,-1);
        nexus.block(20,79,-44,"gregtech:cfoam",8,-1);
        nexus.block(16,75,-47,"gregtech:glass_clear",12,-1);
        nexus.block(17,67,-46,"minecraft:end_portal_frame",3,-1);
        nexus.block(19,67,-45,"minecraft:air",0,-1);
        nexus.block(28,67,-46,"minecraft:water",0,-1);
        nexus.block(22,71,-47,"minecraft:glowstone",0,-1);
        nexus.block(23,68,-47,"minecraft:air",0,-1);
        equal(new Pos(0,71,0),nexus.spawn,"original Nexus spawn");
        equal(-63,nexus.lowestWrite,"Nexus foundation preserves bottom bedrock");

        SourceStreets streets=new SourceStreets(66,false,false,true,false);
        // All 16 chunks of the source's 64x64 plaza must receive their own foundation. This
        // guards the modern adaptation against silently retaining only the four centre chunks.
        for (int x:new int[]{-32,-16,0,16}) for (int z:new int[]{-32,-16,0,16}) {
            SampleWorld plaza=new SampleWorld(x,z,false,false);
            plaza.probe(x,-63,z);
            equal(true,streets.generate(plaza,-16,-16,-1,-1,Set.of("plains")),"plaza chunk "+x+","+z);
            plaza.block(x,-63,z,"gregtech:concrete",8,-1);
            equal(-63,plaza.lowestWrite,"plaza preserves bottom bedrock");
        }
        SampleWorld edge=new SampleWorld(-32,0,false,false);
        edge.probe(-31,66,0);
        streets.generate(edge,-16,-16,-1,-1,Set.of("plains"));
        edge.block(-31,66,0,"gregtech:asphalt",15,-1);

        for (int mode=0;mode<3;mode++) {
            SampleWorld road=new SampleWorld(0,512,mode==2,mode==1);
            road.probe(3,66,520);road.probe(1,67,520);road.probe(2,67,520);
            road.probe(5,65,518);road.probe(5,73,518);
            equal(true,streets.generate(road,0,512,15,527,Set.of(mode==1?"ocean":"plains")),"road mode "+mode);
            road.block(3,66,520,"gregtech:asphalt",8,-1);
            road.block(1,67,520,"gregtech:cfoam",7,4);
            road.block(2,67,520,"gregtech:railroad",0,-1);
            // A source bridge footing overlaps this square with light gray at k+3.
            road.block(5,65,518,mode==0?"minecraft:gravel":"gregtech:concrete",mode==0?1:mode==1?7:8,-1);
            if (mode==2) road.block(5,73,518,"gregtech:concrete",15,-1);
            equal("Z: 0",road.signs.get(new Pos(11,69,519)).get(2),"sign before 512 boundary");
            equal("Z: 1",road.signs.get(new Pos(11,69,520)).get(2),"sign after 512 boundary");
        }
        equal(true,OriginBiomeNames.water("rwg_oceanIce"),"exact source mod biome alias");
        equal(false,OriginBiomeNames.water("river_valley"),"unlisted biome is not guessed from a word");
        return assertions;
    }
}
