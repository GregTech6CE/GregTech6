package com.gregtech.gregtech.data;

import com.google.gson.Gson;
import com.gregtech.gregtech.api.material.GTMaterial;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Original bottle filling dictionary patterns over shared catalogs; native data-pack reload and codecs. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class BottleFillingRecipePack extends AbstractPackResources {
    private static final Gson GSON=new Gson();

    private Map<ResourceLocation, byte[]> resources;

    public BottleFillingRecipePack(net.minecraft.server.packs.PackLocationInfo info) {
        super(info);
    }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
            var location=new net.minecraft.server.packs.PackLocationInfo("gregtech:bottle_filling_recipes",Component.literal("GregTech original bottle filling crafting recipes"),PackSource.BUILT_IN,java.util.Optional.empty());
            Pack pack=Pack.readMetaAndCreate(location,new Pack.ResourcesSupplier(){
                public PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info){return new BottleFillingRecipePack(info);}
                public PackResources openFull(net.minecraft.server.packs.PackLocationInfo info,Pack.Metadata metadata){return new BottleFillingRecipePack(info);}
            },PackType.SERVER_DATA,new net.minecraft.server.packs.PackSelectionConfig(true,Pack.Position.BOTTOM,true));
            if (pack != null) output.accept(pack);
        });
    }

    private synchronized Map<ResourceLocation,byte[]> data(){
        if(resources!=null)return resources;
        Map<ResourceLocation,byte[]> generated=new HashMap<>();
        for(var family:com.gregtech.gregtech.content.food.BottleFillingRows.all())for(int count=4;count>=1;count--){var ingredients=new ArrayList<Map<String,Object>>();ingredients.add(Map.of("type","gregtech:finite_fluid_container_1000","fluid",family.fluidKey()));for(int i=0;i<count;i++)ingredients.add(Map.of("item",BuiltInRegistries.ITEM.getKey(com.gregtech.gregtech.item.BottleItem.emptyBottle().getItem()).toString()));var recipe=Map.of("type","gregtech:finite_bottle_filling","group","gt.bottles","category","misc","ingredients",ingredients,"result",Map.of("id","gregtech:"+family.bottleId(),"count",count));generated.put(ResourceLocation.fromNamespaceAndPath("gregtech","recipe/bottles/"+family.bottleId()+"_x"+count+".json"),GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8));}
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original finite bottle filling datapack: {} rows",generated.size());resources=Map.copyOf(generated);return resources;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
        return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":48,\"description\":"
                + "\"GregTech original bottle filling crafting recipes\"}}").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type != PackType.SERVER_DATA) return null;
        byte[] bytes = data().get(id);
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType type, String namespace, String path,
                              PackResources.ResourceOutput output) {
        if (type != PackType.SERVER_DATA) return;
        data().forEach((id, bytes) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(path + "/")) {
                output.accept(id, () -> new ByteArrayInputStream(bytes));
            }
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? Set.of("gregtech") : Set.of();
    }

    @Override
    public void close() {
        resources = null;
    }
}
