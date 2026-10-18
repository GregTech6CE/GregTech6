package com.gregtech.gregtech.data;

import com.google.gson.Gson;
import com.gregtech.gregtech.data.MaterialPrefix;
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

/** Original wood dictionary patterns over shared catalogs; native data-pack reload and codecs. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class TrackCraftingRecipePack extends AbstractPackResources {
    private static final Gson GSON=new Gson();

    private Map<ResourceLocation, byte[]> resources;

    public TrackCraftingRecipePack(net.minecraft.server.packs.PackLocationInfo info) {
        super(info);
    }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
            var location=new net.minecraft.server.packs.PackLocationInfo("gregtech:track_crafting_recipes",Component.literal("GregTech original track crafting recipes"),PackSource.BUILT_IN,java.util.Optional.empty());
            Pack pack=Pack.readMetaAndCreate(location,new Pack.ResourcesSupplier(){
                public PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info){return new TrackCraftingRecipePack(info);}
                public PackResources openFull(net.minecraft.server.packs.PackLocationInfo info,Pack.Metadata metadata){return new TrackCraftingRecipePack(info);}
            },PackType.SERVER_DATA,new net.minecraft.server.packs.PackSelectionConfig(true,Pack.Position.BOTTOM,true));
            if (pack != null) output.accept(pack);
        });
    }

    private synchronized Map<ResourceLocation,byte[]> data(){
        if(resources!=null)return resources;Map<ResourceLocation,byte[]> generated=new HashMap<>();int skipped=0;
        for(var row:com.gregtech.gregtech.content.transport.TrackRecipeCatalog.rows()){
            var output=new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(row.output())));
            Map<String,Object> keys=new HashMap<>();boolean missing=output.isEmpty();
            for(var key:row.ingredients().entrySet()){
                var stack=resolve(key.getValue());if(stack.isEmpty()){missing=true;break;}
                keys.put(key.getKey(),Map.of("item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
            }
            if(missing){skipped++;com.mojang.logging.LogUtils.getLogger().warn("[gregtech] Missing source rail crafting identity {}",row.id());continue;}
            var json=Map.of("type","minecraft:crafting_shaped","category","misc","group","gt.tracks","pattern",row.pattern().split("\\|"),"key",keys,"result",Map.of("id",row.output(),"count",row.count()));
            generated.put(ResourceLocation.fromNamespaceAndPath("gregtech","recipe/"+row.id()+".json"),GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Native original track crafting rows: {}; missing={}",generated.size(),skipped);
        resources=Map.copyOf(generated);return resources;
    }
    private static net.minecraft.world.item.ItemStack resolve(String value){
        if(value.startsWith("item:"))return new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(value.substring(5))));
        int colon=value.indexOf(':');var material=com.gregtech.gregtech.api.material.GTMaterialRegistry.get(value.substring(colon+1));
        if(material==null||!material.isValid())return net.minecraft.world.item.ItemStack.EMPTY;
        return com.gregtech.gregtech.registry.GTItems.getStack(value.startsWith("rail:")?MaterialPrefix.railGt:MaterialPrefix.stick,material.resolve(),1);
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
        return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":48,\"description\":"
                + "\"GregTech original track crafting recipes\"}}").getBytes(StandardCharsets.UTF_8));
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
