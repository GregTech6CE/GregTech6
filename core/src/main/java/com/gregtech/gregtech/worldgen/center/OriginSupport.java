package com.gregtech.gregtech.worldgen.center;

/** Source constants and WD helpers required by the two origin generators; no game dependency. */
public final class OriginSupport {
    private OriginSupport() {}
    public static final boolean T = true, F = false;
    public static final int SIDE_Y_NEG=0, SIDE_Y_POS=1, SIDE_Z_NEG=2, SIDE_Z_POS=3, SIDE_X_NEG=4, SIDE_X_POS=5;
    public static final int DYE_INDEX_Black=0,DYE_INDEX_Red=1,DYE_INDEX_Blue=4,DYE_INDEX_LightGray=7,
            DYE_INDEX_Gray=8,DYE_INDEX_LightBlue=12,DYE_INDEX_White=15,DYE_INDEX_Yellow=11,DYE_INDEX_Green=2;
    public static final OriginWorld.Block NB = new OriginWorld.Block("minecraft:air",-1,false,false,false,false,false);
    public static final class Blocks {
        private Blocks() {}
        public static final OriginWorld.Block coarse_dirt=new OriginWorld.Block("minecraft:coarse_dirt"),
                ice=new OriginWorld.Block("minecraft:ice"), packed_ice=new OriginWorld.Block("minecraft:packed_ice"),
                clay=new OriginWorld.Block("minecraft:clay"), snow_layer=new OriginWorld.Block("minecraft:snow"),
                pumpkin=new OriginWorld.Block("minecraft:pumpkin"), mossy_cobblestone=new OriginWorld.Block("minecraft:mossy_cobblestone"),
                tallgrass=new OriginWorld.Block("minecraft:short_grass"), red_flower=new OriginWorld.Block("source:flower"),
                sand=new OriginWorld.Block("minecraft:sand"), hardened_clay=new OriginWorld.Block("minecraft:terracotta"),
                cactus=new OriginWorld.Block("minecraft:cactus"), melon_block=new OriginWorld.Block("minecraft:melon"),
                waterlily=new OriginWorld.Block("minecraft:lily_pad"), stone=new OriginWorld.Block("minecraft:stone"),
                crafting_table=new OriginWorld.Block("minecraft:crafting_table"), cauldron=new OriginWorld.Block("minecraft:cauldron"),
                ender_chest=new OriginWorld.Block("minecraft:ender_chest"), brewing_stand=new OriginWorld.Block("minecraft:brewing_stand"),
                anvil=new OriginWorld.Block("minecraft:anvil");
        public static final OriginWorld.Block obsidian=new OriginWorld.Block("minecraft:obsidian"),
                glowstone=new OriginWorld.Block("minecraft:glowstone"),
                end_portal_frame=new OriginWorld.Block("minecraft:end_portal_frame"),
                grass=new OriginWorld.Block("minecraft:grass_block"), dirt=new OriginWorld.Block("minecraft:dirt"),
                water=new OriginWorld.Block("minecraft:water",-1,false,true,false,false,true),
                yellow_flower=new OriginWorld.Block("minecraft:dandelion",-1,false,false,false,false,false),
                sandstone=new OriginWorld.Block("minecraft:sandstone"),
                stained_hardened_clay=new OriginWorld.Block("source:stained_terracotta"),
                glass_pane=new OriginWorld.Block("minecraft:glass_pane",-1,false,false,false,false,false),
                cobblestone=new OriginWorld.Block("minecraft:cobblestone"),gravel=new OriginWorld.Block("minecraft:gravel"),
                iron_block=new OriginWorld.Block("minecraft:iron_block"),beacon=new OriginWorld.Block("minecraft:beacon");
    }
    public static final class BlocksGT {
        private BlocksGT() {}
        public static final OriginWorld.Block Concrete=new OriginWorld.Block("gregtech:concrete"),
                CFoam=new OriginWorld.Block("gregtech:cfoam"),Asphalt=new OriginWorld.Block("gregtech:asphalt"),
                Glass=new OriginWorld.Block("gregtech:glass_clear",-1,false,false,false,false,false),
                RailRoad=new OriginWorld.Block("gregtech:railroad",-1,false,false,false,false,false);
        public static final OriginWorld.Block GlowGlass=new OriginWorld.Block("gregtech:glass_glow"),
                River=new OriginWorld.Block("source:river"), Sands=new OriginWorld.Block("source:sands"),
                Diggables=new OriginWorld.Block("source:diggables"), Glowtus=new OriginWorld.Block("source:glowtus"),
                SchistGreen=new OriginWorld.Block("gregtech:stone_greenschist"), SchistBlue=new OriginWorld.Block("gregtech:stone_blueschist"),
                Kimberlite=new OriginWorld.Block("gregtech:stone_kimberlite"), Quartzite=new OriginWorld.Block("gregtech:stone_quartzite"),
                Limestone=new OriginWorld.Block("gregtech:stone_limestone"), Marble=new OriginWorld.Block("gregtech:stone_marble"),
                GraniteRed=new OriginWorld.Block("gregtech:stone_granite_red"), GraniteBlack=new OriginWorld.Block("gregtech:stone_granite_black"),
                Komatiite=new OriginWorld.Block("gregtech:stone_komatiite"), Basalt=new OriginWorld.Block("gregtech:stone_basalt");
        public static final OriginWorld.Block[] GLOW_GLASS_SLABS={GlowGlass.slab(0),GlowGlass.slab(1),GlowGlass.slab(2),GlowGlass.slab(3),GlowGlass.slab(4),GlowGlass.slab(5)};
        public static final OriginWorld.Block[] FOAM_SLABS={CFoam.slab(0),CFoam.slab(1),CFoam.slab(2),CFoam.slab(3),CFoam.slab(4),CFoam.slab(5)};
        public static final OriginWorld.Block[] stones=java.util.Arrays.stream(new String[]{"granite_black","granite_red","basalt","marble","limestone","granite","diorite","andesite","komatiite","greenschist","blueschist","kimberlite","quartzite","prismarine_light","prismarine_dark","slate","shale"}).map(name->new OriginWorld.Block("gregtech:stone_"+name)).toArray(OriginWorld.Block[]::new);
    }
    public static final class BlockStones {private BlockStones(){}public static final int TILES=10,CHISL=6,STILE=11;}
    public static boolean set(OriginWorld world,int x,int y,int z,OriginWorld.Block block,int meta,int flags){return set(world,x,y,z,block,meta,flags,block.opaque());}
    public static boolean set(OriginWorld world,int x,int y,int z,OriginWorld.Block block,int meta,int flags,boolean removeGrass){
        if(!world.canWrite(x,y,z))return false;
        if(removeGrass){var below=world.getBlock(x,y-1,z);if(below.id().equals("minecraft:grass_block")||below.id().equals("minecraft:mycelium"))world.setBlock(x,y-1,z,Blocks.dirt,0,flags);}
        return world.setBlock(x,y,z,block,meta&15,flags);
    }
    public static boolean set(OriginWorld.Chunk chunk,int x,int y,int z,OriginWorld.Block block,int meta){return chunk.world().setBlock(chunk.minX()+x,y,chunk.minZ()+z,block,meta&15,0);}
    public static boolean opq(OriginWorld world,int x,int y,int z,boolean load,boolean fallback){return opq(world.getBlock(x,y,z));}
    public static boolean opq(OriginWorld.Block block){return block.opaque()&&!block.leaves();}
    public static boolean anywater(OriginWorld.Block block){return block.water();}
    public static OriginWorld.Block block(OriginWorld world,int x,int y,int z){return world.getBlock(x,y,z);}
    public static OriginWorld.Block block(OriginWorld world,int x,int y,int z,boolean load){return world.getBlock(x,y,z);}
    public static boolean even(int x,int y,int z){int count=0;for(int coordinate:new int[]{x,y,z})if(coordinate%2==0)count++;return count%2==0;}
    public static boolean inside(int min,int max,int value){return value>=min&&value<=max;}
    public static String text(OriginWorld.Biome biome){return biome==null?"":biome.displayName();}
}
