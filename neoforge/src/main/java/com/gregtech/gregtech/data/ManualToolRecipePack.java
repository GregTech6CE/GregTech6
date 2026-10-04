package com.gregtech.gregtech.data;

import com.google.gson.Gson;
import com.gregtech.gregtech.api.material.GTMaterial;
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

/** Full original shaped/head/assembly recipes over shared tool patterns; reloadable and network synchronized. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ManualToolRecipePack extends AbstractPackResources {
    private static final Gson GSON=new Gson();

    private Map<ResourceLocation, byte[]> resources;

    public ManualToolRecipePack(net.minecraft.server.packs.PackLocationInfo info) {
        super(info);
    }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
            var location=new net.minecraft.server.packs.PackLocationInfo("gregtech:manual_tool_recipes",Component.literal("GregTech original manual tool recipes"),PackSource.BUILT_IN,java.util.Optional.empty());
            Pack pack=Pack.readMetaAndCreate(location,new Pack.ResourcesSupplier(){
                public PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info){return new ManualToolRecipePack(info);}
                public PackResources openFull(net.minecraft.server.packs.PackLocationInfo info,Pack.Metadata metadata){return new ManualToolRecipePack(info);}
            },PackType.SERVER_DATA,new net.minecraft.server.packs.PackSelectionConfig(true,Pack.Position.BOTTOM,true));
            if (pack != null) output.accept(pack);
        });
    }

    private synchronized Map<ResourceLocation,byte[]> data(){
        if(resources!=null)return resources;
        Map<ResourceLocation,byte[]> generated=new HashMap<>();int shaped=0,heads=0,assemblies=0;List<String> without=new ArrayList<>();
        for(var type:com.gregtech.gregtech.api.tool.GTToolType.values()){
          var patterns=com.gregtech.gregtech.recipe.GTToolRecipes.shaped(type);
          for(int i=0;i<patterns.size();i++){if(!com.gregtech.gregtech.recipe.GTToolPatternRecipe.hasMaterials(type,patterns.get(i)))continue;add(generated,"tools/"+type.id()+(i==0?"":"_"+i),"tool_crafting",type.id(),i);shaped++;}
          boolean assembly=type.requiresHeadAssembly()||type==com.gregtech.gregtech.api.tool.GTToolType.MAGNIFYING_GLASS;
          if(assembly){add(generated,"tools/"+type.id()+"_assembly","tool_assembly",type.id(),0);assemblies++;}
          if(patterns.isEmpty()&&!assembly)without.add(type.id());
          var headPatterns=com.gregtech.gregtech.recipe.GTToolRecipes.heads(type);
          for(int i=0;i<headPatterns.size();i++){if(!com.gregtech.gregtech.recipe.GTToolPatternRecipe.hasMaterials(type,headPatterns.get(i)))continue;add(generated,"tool_heads/"+type.id()+(i==0?"":"_"+i),"tool_head",type.id(),i);heads++;}
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original manual tool datapack: {} shaped tools, {} head+handle assemblies, {} heads; original table without manual row: {}",shaped,assemblies,heads,without);
        int powered=0;
        for(var material:com.gregtech.gregtech.api.material.GTMaterialRegistry.allMaterials())for(var spec:com.gregtech.gregtech.content.tool.ElectricToolAssembly.values()){
         java.util.List<net.minecraft.world.item.ItemStack> batteries=new ArrayList<>();
         for(var entry:com.gregtech.gregtech.registry.GTChemicalBatteries.all())if(entry.get().spec().tier()==1)batteries.add(new net.minecraft.world.item.ItemStack(entry.get().asItem()));
         for(var battery:batteries){if(spec.recipe(material,battery,"")==null)continue;
          var batteryId=BuiltInRegistries.ITEM.getKey(battery.getItem());String suffix="/"+batteryId.getPath();
          var id=ResourceLocation.fromNamespaceAndPath("gregtech","recipe/electric_tools/"+spec.id+"/"+material.getName().toLowerCase(java.util.Locale.ROOT)+suffix+".json");
          var bytes=GSON.toJson(Map.of("type","gregtech:electric_tool_assembly","tool",spec.id,"material",material.getName(),"battery",batteryId.toString())).getBytes(StandardCharsets.UTF_8);
          if(generated.putIfAbsent(id,bytes)!=null)throw new IllegalStateException("Duplicate powered assembly "+id);powered++;
         }
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original powered tool datapack: {} concrete material/battery assemblies",powered);
        addAxles(generated);
        OriginalHandCraftingRows.add(generated);
        com.gregtech.gregtech.loaders.Loader_FormConversionCraftingRecipes.add(generated);
        com.gregtech.gregtech.loaders.Loader_StoneCraftingRecipes.add(generated);
        com.gregtech.gregtech.loaders.Loader_BedrockFlowerCraftingRecipes.add(generated);
        resources=Map.copyOf(generated);return resources;
    }
    /** Full Loader_HandToolCraftingRecipes.axles patterns; same ids, forms and tool costs. */
    private static void addAxles(Map<ResourceLocation,byte[]> data) {
        var axles = com.gregtech.gregtech.registry.GTAxles.all();
        if (axles.size() != 52) throw new IllegalStateException("Expected 52 original axle variants");
        for (var holder : axles) {
            var spec = holder.get().spec();
            boolean wood = spec.material() == com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated;
            boolean creosote = wood && spec.size() == 4;
            var keys = new java.util.LinkedHashMap<String, Object>();
            keys.put("f", itemIngredient(new net.minecraft.world.item.ItemStack(com.gregtech.gregtech.registry.GTToolItems.get(com.gregtech.gregtech.api.tool.GTToolType.FILE))));
            var hammer = wood ? com.gregtech.gregtech.api.tool.GTToolType.SOFT_HAMMER : com.gregtech.gregtech.api.tool.GTToolType.HARD_HAMMER;
            keys.put(wood ? "r" : "h", itemIngredient(new net.minecraft.world.item.ItemStack(com.gregtech.gregtech.registry.GTToolItems.get(hammer))));
            String[] pattern;
            if (creosote) {
                pattern = new String[]{"rS", "Bf"};
                keys.put("S", Map.of("tag", "gregtech:wooden_beams"));
                keys.put("B", Map.of("type", "gregtech:creosote_container_1000"));
            } else {
                var form = switch (spec.size()) {
                    case 1 -> MaterialPrefix.stick;
                    case 2 -> MaterialPrefix.stickLong;
                    case 3 -> wood ? MaterialPrefix.stickLong : MaterialPrefix.ingotDouble;
                    case 4 -> MaterialPrefix.ingotQuadruple;
                    default -> throw new IllegalStateException("Unknown axle size");
                };
                var stack = com.gregtech.gregtech.registry.GTItems.getStack(form, spec.material(), 1);
                Object piece = itemIngredient(stack);
                if (form == MaterialPrefix.stick || form == MaterialPrefix.stickLong) {
                    var tag = (form == MaterialPrefix.stick ? "rods/" : "long_rods/")
                            + com.gregtech.gregtech.api.material.MaterialEquivalence.materialName(spec.material());
                    piece = List.of(piece, Map.of("tag", "forge:" + tag), Map.of("tag", "c:" + tag));
                }
                keys.put("S", piece);
                pattern = wood && spec.size() == 3 ? new String[]{"  S", "SrS", "S f"}
                        : new String[]{"  S", wood ? " r " : " h ", "S f"};
            }
            var json = new java.util.LinkedHashMap<String, Object>();
            json.put("type", creosote ? "gregtech:creosote_axle" : "gregtech:tool_shaped");
            json.put("category", "misc"); json.put("group", "gt.hand");
            json.put("pattern", pattern); json.put("key", keys); json.put("allow_mirror", false);
            json.put("result", Map.of("id", holder.getId().toString(), "count", 1));
            var id = ResourceLocation.fromNamespaceAndPath("gregtech", "recipe/hand/axle/" + spec.id() + ".json");
            if (data.putIfAbsent(id, GSON.toJson(json).getBytes(StandardCharsets.UTF_8)) != null)
                throw new IllegalStateException("Duplicate axle recipe " + id);
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original native axle crafting recipes: {}", axles.size());
    }
    private static Map<String, Object> itemIngredient(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalStateException("Missing original axle material/tool");
        return Map.of("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private static void add(Map<ResourceLocation,byte[]> data,String id,String serializer,String tool,int pattern){
        var key=ResourceLocation.fromNamespaceAndPath("gregtech","recipe/"+id+".json");
        byte[] value=GSON.toJson(Map.of("type","gregtech:"+serializer,"tool",tool,"pattern",pattern)).getBytes(StandardCharsets.UTF_8);
        if(data.putIfAbsent(key,value)!=null)throw new IllegalStateException("Duplicate manual tool recipe "+id);
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
        return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":48,\"description\":"
                + "\"GregTech original manual tool recipes\"}}").getBytes(StandardCharsets.UTF_8));
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
