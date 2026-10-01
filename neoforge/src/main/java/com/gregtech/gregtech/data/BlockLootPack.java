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
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Registry-driven built-in datapack: avoids thousands of tiny processResources files. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class BlockLootPack extends AbstractPackResources {
    private Map<ResourceLocation,byte[]> resources;
    public BlockLootPack(PackLocationInfo info) { super(info); }
    @SubscribeEvent public static void packs(AddPackFindersEvent event) {
        if(event.getPackType()!=PackType.SERVER_DATA) return;
        event.addRepositorySource(output->{
            var location=new PackLocationInfo("gregtech:block_loot",Component.literal("GregTech block loot and harvest tags"),PackSource.BUILT_IN,Optional.empty());
            var pack=Pack.readMetaAndCreate(location,new Pack.ResourcesSupplier(){public PackResources openPrimary(PackLocationInfo info){return new BlockLootPack(info);}public PackResources openFull(PackLocationInfo info,Pack.Metadata metadata){return new BlockLootPack(info);}},PackType.SERVER_DATA,new PackSelectionConfig(true,Pack.Position.BOTTOM,true));
            if(pack!=null) output.accept(pack);
        });
    }
    private synchronized Map<ResourceLocation,byte[]> data() {
        if(resources!=null) return resources;
        var gson=new com.google.gson.Gson();
        var result=new HashMap<ResourceLocation,byte[]>();
        var tags=new TreeMap<String,Set<String>>();
        for(var block:BuiltInRegistries.BLOCK) {
            var id=BuiltInRegistries.BLOCK.getKey(block);
            if(id==null || !id.getNamespace().equals("gregtech") || block instanceof net.minecraft.world.level.block.LiquidBlock) continue;
            String tag=BlockHarvestPolicy.tag(BlockHarvestPolicy.tool(block));
            tags.computeIfAbsent(tag,k->new TreeSet<>()).add(id.toString());
            int level=BlockHarvestPolicy.level(block);
            if(level>0) tags.computeIfAbsent("minecraft:needs_"+(level>=3?"diamond":level==2?"iron":"stone")+"_tool",k->new TreeSet<>()).add(id.toString());
            if(block.asItem()==Items.AIR)continue;
            // Supply vanilla loot only. Blocks overriding getDrops retain their specialized NBT/drop behavior.
            // GT6 spike metadata distinguished the two materials and wall/omni/falling mode. Copy
            // those properties into BlockStateTag so mining a configured spike does not erase them.
            Map<String,Object> entry=block instanceof com.gregtech.gregtech.api.block.StatefulBlockLoot stateful
                    ? Map.of("type","minecraft:item", "name",BuiltInRegistries.ITEM.getKey(block.asItem()).toString(),
                            "functions",List.of(Map.of("function","minecraft:copy_state", "block",id.toString(),
                                    "properties",stateful.lootStateProperties())))
                    : Map.of("type","minecraft:item","name",BuiltInRegistries.ITEM.getKey(block.asItem()).toString());
            var pool=Map.of("rolls",1,"entries",List.of(entry),"conditions",List.of(Map.of("condition","minecraft:survives_explosion")));
            result.put(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"loot_table/blocks/"+id.getPath()+".json"),
                    gson.toJson(Map.of("type","minecraft:block","pools",List.of(pool))).getBytes(StandardCharsets.UTF_8));
        }
        tags.forEach((tag,values)->{
            var id=ResourceLocation.parse(tag);
            result.put(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"tags/block/"+id.getPath()+".json"),
                    gson.toJson(Map.of("replace",false,"values",values)).getBytes(StandardCharsets.UTF_8));
        });
        resources=Collections.unmodifiableMap(result); return resources;
    }
    public IoSupplier<InputStream> getRootResource(String... path) {
        if(path.length!=1||!path[0].equals("pack.mcmeta"))return null;
        return ()->new ByteArrayInputStream("{\"pack\":{\"pack_format\":48,\"description\":\"GregTech block loot and harvest tags\"}}".getBytes(StandardCharsets.UTF_8));
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
