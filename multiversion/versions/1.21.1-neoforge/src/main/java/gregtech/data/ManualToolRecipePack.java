package gregtech.data;

import com.google.gson.Gson;
import gregtech.api.material.GTMaterial;
import gregtech.data.MaterialPrefix;
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
@EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
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
            var location=new net.minecraft.server.packs.PackLocationInfo("gt6ce:manual_tool_recipes",Component.literal("GregTech original manual tool recipes"),PackSource.BUILT_IN,java.util.Optional.empty());
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
        for(var type:gregtech.api.tool.GTToolType.values()){
          var patterns=gregtech.recipe.GTToolRecipes.shaped(type);
          for(int i=0;i<patterns.size();i++){if(!gregtech.recipe.GTToolPatternRecipe.hasMaterials(type,patterns.get(i)))continue;add(generated,"tools/"+type.id()+(i==0?"":"_"+i),"tool_crafting",type.id(),i);shaped++;}
          boolean assembly=type.requiresHeadAssembly()||type==gregtech.api.tool.GTToolType.MAGNIFYING_GLASS;
          if(assembly){add(generated,"tools/"+type.id()+"_assembly","tool_assembly",type.id(),0);assemblies++;}
          if(patterns.isEmpty()&&!assembly)without.add(type.id());
          var headPatterns=gregtech.recipe.GTToolRecipes.heads(type);
          for(int i=0;i<headPatterns.size();i++){if(!gregtech.recipe.GTToolPatternRecipe.hasMaterials(type,headPatterns.get(i)))continue;add(generated,"tool_heads/"+type.id()+(i==0?"":"_"+i),"tool_head",type.id(),i);heads++;}
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original manual tool datapack: {} shaped tools, {} head+handle assemblies, {} heads; original table without manual row: {}",shaped,assemblies,heads,without);
        int powered=0;
        for(var material:gregtech.api.material.GTMaterialRegistry.allMaterials())for(var spec:gregtech.content.tool.ElectricToolAssembly.values()){
         java.util.List<net.minecraft.world.item.ItemStack> batteries=new ArrayList<>();
         for(var entry:gregtech.registry.GTChemicalBatteries.all())if(entry.get().spec().tier()==spec.definition().tier())batteries.add(new net.minecraft.world.item.ItemStack(entry.get().asItem()));
         for(var battery:batteries){if(spec.recipe(material,battery,"")==null)continue;
          var batteryId=BuiltInRegistries.ITEM.getKey(battery.getItem());String suffix="/"+batteryId.getPath();
          var id=ResourceLocation.fromNamespaceAndPath("gt6ce","recipe/electric_tools/"+spec.id+"/"+material.getName().toLowerCase(java.util.Locale.ROOT)+suffix+".json");
          var bytes=GSON.toJson(Map.of("type","gt6ce:electric_tool_assembly","tool",spec.id,"material",material.getName(),"battery",batteryId.toString())).getBytes(StandardCharsets.UTF_8);
          if(generated.putIfAbsent(id,bytes)!=null)throw new IllegalStateException("Duplicate powered assembly "+id);powered++;
         }
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original powered tool datapack: {} concrete material/battery assemblies",powered);
        OriginalHandCraftingRows.add(generated);
        gregtech.content.recipe.FormConversionCraftingRecipeInjector.add(generated);
        gregtech.content.recipe.StoneCraftingRecipeInjector.add(generated);
        gregtech.content.recipe.BedrockFlowerCraftingRecipeInjector.add(generated);
        resources=Map.copyOf(generated);return resources;
    }
    private static void add(Map<ResourceLocation,byte[]> data,String id,String serializer,String tool,int pattern){
        var key=ResourceLocation.fromNamespaceAndPath("gt6ce","recipe/"+id+".json");
        byte[] value=GSON.toJson(Map.of("type","gt6ce:"+serializer,"tool",tool,"pattern",pattern)).getBytes(StandardCharsets.UTF_8);
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
        return type == PackType.SERVER_DATA ? Set.of("gt6ce") : Set.of();
    }

    @Override
    public void close() {
        resources = null;
    }
}
