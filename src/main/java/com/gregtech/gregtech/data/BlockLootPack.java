package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.block.MaterialBlockLike;
import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.*;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Registry-driven built-in datapack: avoids thousands of tiny processResources files. */
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class BlockLootPack extends AbstractPackResources {
    private Map<ResourceLocation,byte[]> resources;
    public BlockLootPack(String id) { super(id,true); }
    @SubscribeEvent public static void packs(AddPackFindersEvent event) {
        if(event.getPackType()!=PackType.SERVER_DATA) return;
        event.addRepositorySource(output->{
            var pack=Pack.readMetaAndCreate("gregtech:block_loot",Component.literal("GregTech block loot and harvest tags"),true,
                    BlockLootPack::new,PackType.SERVER_DATA,Pack.Position.BOTTOM,PackSource.BUILT_IN);
            if(pack!=null) output.accept(pack);
        });
    }
    private synchronized Map<ResourceLocation,byte[]> data() {
        if(resources!=null) return resources;
        var gson=new com.google.gson.Gson();
        var result=new HashMap<ResourceLocation,byte[]>();
        var tags=new TreeMap<String,Set<String>>();
        for(var block:ForgeRegistries.BLOCKS.getValues()) {
            var id=ForgeRegistries.BLOCKS.getKey(block);
            if(id==null || !id.getNamespace().equals("gregtech") || block instanceof net.minecraft.world.level.block.LiquidBlock) continue;
            String tag=BlockHarvestPolicy.tag(BlockHarvestPolicy.tool(block));
            tags.computeIfAbsent(tag,k->new TreeSet<>()).add(id.toString());
            int level=BlockHarvestPolicy.level(block);
            if(level>0) tags.computeIfAbsent("minecraft:needs_"+(level>=3?"diamond":level==2?"iron":"stone")+"_tool",k->new TreeSet<>()).add(id.toString());
            if(block.asItem()==Items.AIR)continue;
            // Supply vanilla loot only. Blocks overriding getDrops retain their specialized NBT/drop behavior.
            // GT6 spike metadata distinguished the two materials and wall/omni/falling mode. Copy
            // those properties into BlockStateTag so mining a configured spike does not erase them.
            Map<String,Object> entry=block instanceof com.gregtech.gregtech.block.misc.SpikeBlock
                    ? Map.of("type","minecraft:item", "name",ForgeRegistries.ITEMS.getKey(block.asItem()).toString(),
                            "functions",List.of(Map.of("function","minecraft:copy_state", "block",id.toString(),
                                    "properties",List.of("mode","secondary"))))
                    : Map.of("type","minecraft:item","name",ForgeRegistries.ITEMS.getKey(block.asItem()).toString());
            var pool=Map.of("rolls",1,"entries",List.of(entry),"conditions",List.of(Map.of("condition","minecraft:survives_explosion")));
            result.put(new ResourceLocation(id.getNamespace(),"loot_tables/blocks/"+id.getPath()+".json"),
                    gson.toJson(Map.of("type","minecraft:block","pools",List.of(pool))).getBytes(StandardCharsets.UTF_8));
        }
        tags.forEach((tag,values)->{
            var id=ResourceLocation.parse(tag);
            result.put(new ResourceLocation(id.getNamespace(),"tags/blocks/"+id.getPath()+".json"),
                    gson.toJson(Map.of("replace",false,"values",values)).getBytes(StandardCharsets.UTF_8));
        });
        resources=Collections.unmodifiableMap(result); return resources;
    }
    public IoSupplier<InputStream> getRootResource(String... path) {
        if(path.length!=1||!path[0].equals("pack.mcmeta"))return null;
        return ()->new ByteArrayInputStream("{\"pack\":{\"pack_format\":15,\"description\":\"GregTech block loot and harvest tags\"}}".getBytes(StandardCharsets.UTF_8));
    }
    public IoSupplier<InputStream> getResource(PackType type,ResourceLocation id) {
        if(type!=PackType.SERVER_DATA)return null; var bytes=data().get(id);
        return bytes==null?null:()->new ByteArrayInputStream(bytes);
    }
    public void listResources(PackType type,String ns,String path,ResourceOutput out) {
        if(type==PackType.SERVER_DATA)data().forEach((id,bytes)->{if(id.getNamespace().equals(ns)&&id.getPath().startsWith(path+"/"))out.accept(id,()->new ByteArrayInputStream(bytes));});
    }
    public Set<String> getNamespaces(PackType type) { return type==PackType.SERVER_DATA?Set.of("minecraft","gregtech"):Set.of(); }
    public void close() { resources=null; }
}
