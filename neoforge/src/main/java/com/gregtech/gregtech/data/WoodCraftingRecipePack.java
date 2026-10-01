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

/** Original wood dictionary patterns over shared catalogs; native data-pack reload and codecs. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class WoodCraftingRecipePack extends AbstractPackResources {
    private static final Gson GSON=new Gson();

    private Map<ResourceLocation, byte[]> resources;

    public WoodCraftingRecipePack(net.minecraft.server.packs.PackLocationInfo info) {
        super(info);
    }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
            var location=new net.minecraft.server.packs.PackLocationInfo("gregtech:wood_crafting_recipes",Component.literal("GregTech original wood crafting recipes"),PackSource.BUILT_IN,java.util.Optional.empty());
            Pack pack=Pack.readMetaAndCreate(location,new Pack.ResourcesSupplier(){
                public PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info){return new WoodCraftingRecipePack(info);}
                public PackResources openFull(net.minecraft.server.packs.PackLocationInfo info,Pack.Metadata metadata){return new WoodCraftingRecipePack(info);}
            },PackType.SERVER_DATA,new net.minecraft.server.packs.PackSelectionConfig(true,Pack.Position.BOTTOM,true));
            if (pack != null) output.accept(pack);
        });
    }

    private synchronized Map<ResourceLocation,byte[]> data(){
        if(resources!=null)return resources;
        Map<ResourceLocation,byte[]> generated=new HashMap<>();
        for(String row:com.gregtech.gregtech.content.recipe.GTWoodRecipes.CRAFT_ROWS)addRow(generated,row);
        for(String row:com.gregtech.gregtech.content.recipe.TreatedWoodCraftingRows.ROWS)addRow(generated,row);
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original wood crafting datapack: {} rows; source-explicit skipped cinnamonwood rod rows={}",generated.size(),com.gregtech.gregtech.content.recipe.GTWoodRecipes.SKIPPED_CRAFT_ROWS.length);
        resources=Map.copyOf(generated);return resources;
    }
    private static net.minecraft.world.item.ItemStack resolve(String spec){
        if(spec.startsWith("item:"))return new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec.substring(5))));
        if(spec.startsWith("material:")){
            String[] parts=spec.split(":",3);
            return com.gregtech.gregtech.registry.GTItems.getStack(prefix(parts[1]),com.gregtech.gregtech.api.material.GTMaterialRegistry.get(parts[2]),1);
        }
        return com.gregtech.gregtech.loaders.Loader_WoodCraftingRecipes.resolve(spec);
    }
    private static Map<String,Object> ingredient(String spec){
        var stack=resolve(spec);if(stack.isEmpty())throw new IllegalStateException("Unresolved original wood ingredient "+spec);
        return Map.of("item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
    private static void addRow(Map<ResourceLocation,byte[]> data,String encoded){
        String[] parts=encoded.split("\\|",-1);if(parts.length!=7)throw new IllegalArgumentException("Malformed wood row "+encoded);
        Map<String,Object> keys=new java.util.LinkedHashMap<>();
        for(String key:parts[4].split(";")){if(key.length()<3||key.charAt(1)!='=')throw new IllegalArgumentException("Bad wood key "+key);keys.put(key.substring(0,1),ingredient(key.substring(2)));}
        int star=parts[5].lastIndexOf('*');String outputSpec=star<0?parts[5]:parts[5].substring(0,star);int count=star<0?1:Integer.parseInt(parts[5].substring(star+1));
        var output=resolve(outputSpec);if(output.isEmpty())throw new IllegalStateException("Unresolved original wood output "+outputSpec);
        Map<String,Object> recipe=new java.util.LinkedHashMap<>();boolean shaped=parts[1].equals("shaped");
        recipe.put("type",shaped?"gregtech:tool_shaped":"minecraft:crafting_shapeless");recipe.put("group","gt.wood");recipe.put("category",parts[0].startsWith("treated/")?"misc":"building");
        if(shaped){recipe.put("pattern",parts[3].split("/",-1));recipe.put("key",keys);recipe.put("allow_mirror",parts[2].equals("mirror"));}
        else recipe.put("ingredients",new ArrayList<>(keys.values()));
        recipe.put("result",Map.of("id",BuiltInRegistries.ITEM.getKey(output.getItem()).toString(),"count",count));
        var key=ResourceLocation.fromNamespaceAndPath("gregtech","recipe/wood/"+parts[0]+".json");
        if(data.putIfAbsent(key,GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8))!=null)throw new IllegalStateException("Duplicate wood recipe "+key);
    }

    private static com.gregtech.gregtech.data.MaterialPrefix prefix(String id){
        return switch(id){case "bolt"->com.gregtech.gregtech.data.MaterialPrefix.bolt;case "plate"->com.gregtech.gregtech.data.MaterialPrefix.plate;case "gearGtSmall"->com.gregtech.gregtech.data.MaterialPrefix.gearGtSmall;case "gearGt"->com.gregtech.gregtech.data.MaterialPrefix.gearGt;default->throw new IllegalArgumentException("Unknown wood prefix "+id);};
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
        return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":48,\"description\":"
                + "\"GregTech original wood crafting recipes\"}}").getBytes(StandardCharsets.UTF_8));
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
