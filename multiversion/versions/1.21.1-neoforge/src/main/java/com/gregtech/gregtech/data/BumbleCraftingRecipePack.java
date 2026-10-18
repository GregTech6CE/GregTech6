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

/** Original portable crafting dictionary patterns over shared catalogs; native data-pack reload and codecs. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class BumbleCraftingRecipePack extends AbstractPackResources {
    private static final Gson GSON=new Gson();

    private Map<ResourceLocation, byte[]> resources;

    public BumbleCraftingRecipePack(net.minecraft.server.packs.PackLocationInfo info) {
        super(info);
    }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
            var location=new net.minecraft.server.packs.PackLocationInfo("gregtech:bumble_crafting_recipes",Component.literal("GregTech original bumble crafting recipes"),PackSource.BUILT_IN,java.util.Optional.empty());
            Pack pack=Pack.readMetaAndCreate(location,new Pack.ResourcesSupplier(){
                public PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info){return new BumbleCraftingRecipePack(info);}
                public PackResources openFull(net.minecraft.server.packs.PackLocationInfo info,Pack.Metadata metadata){return new BumbleCraftingRecipePack(info);}
            },PackType.SERVER_DATA,new net.minecraft.server.packs.PackSelectionConfig(true,Pack.Position.BOTTOM,true));
            if (pack != null) output.accept(pack);
        });
    }

    private synchronized Map<ResourceLocation,byte[]> data(){
        if(resources!=null)return resources;
        Map<ResourceLocation,byte[]> generated=new HashMap<>();
        for(String file:com.gregtech.gregtech.content.bumble.BumbleCraftingCatalog.FILES){String path="data/gregtech/recipes/tool_blocks/"+file;try(var stream=BumbleCraftingRecipePack.class.getClassLoader().getResourceAsStream(path)){if(stream==null)throw new IllegalStateException("Missing shared portable recipe "+path);var recipe=com.google.gson.JsonParser.parseString(new String(stream.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();var result=recipe.get("result");if(result.isJsonPrimitive()){var obj=new com.google.gson.JsonObject();obj.addProperty("id",result.getAsString());recipe.add("result",obj);}else{var obj=result.getAsJsonObject();if(obj.has("item")){obj.add("id",obj.remove("item"));}}
            generated.put(ResourceLocation.fromNamespaceAndPath("gregtech","recipe/tool_blocks/"+file),GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8));}catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}}
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original portable crafting datapack: {} rows",generated.size());resources=Map.copyOf(generated);return resources;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
        return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":48,\"description\":"
                + "\"GregTech original bumble crafting recipes\"}}").getBytes(StandardCharsets.UTF_8));
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
