package gregtech.content.data;

import com.google.gson.Gson;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

/** Required vanilla overrides, loaded through the native data-pack reload path. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?}
public final class VanillaCraftingRecipePack extends AbstractPackResources {
    /** Replaced by {@code VanillaWoodCraftingRows}: GT6 NERFED_WOOD log/stripped-log counts. */
    private static final String[] REPLACED_PLANK_RECIPES = {
            "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks",
            "minecraft:jungle_planks", "minecraft:acacia_planks", "minecraft:dark_oak_planks",
            "minecraft:mangrove_planks", "minecraft:cherry_planks", "minecraft:crimson_planks",
            "minecraft:warped_planks",
    };
    private Map<ResourceLocation,byte[]> resources;

//? if >=1.21.1 {
    /*public VanillaCraftingRecipePack(net.minecraft.server.packs.PackLocationInfo info) {
        super(info);
*///?} else {
    public VanillaCraftingRecipePack(String id) {
        super(id, true);
//?}
    }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
//? if >=1.21.1 {
            /*var location=new net.minecraft.server.packs.PackLocationInfo("gt6ce:vanilla_replacements",Component.literal("GregTech original vanilla replacements"),PackSource.BUILT_IN,java.util.Optional.empty());
            Pack pack=Pack.readMetaAndCreate(location,new Pack.ResourcesSupplier(){
                public PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info){return new VanillaCraftingRecipePack(info);}
                public PackResources openFull(net.minecraft.server.packs.PackLocationInfo info,Pack.Metadata metadata){return new VanillaCraftingRecipePack(info);}
            },PackType.SERVER_DATA,new net.minecraft.server.packs.PackSelectionConfig(true,Pack.Position.TOP,true));
*///?} else {
            Pack pack = Pack.readMetaAndCreate("gt6ce:vanilla_replacements",
                    Component.literal("GregTech original vanilla replacements"), true,
                    VanillaCraftingRecipePack::new, PackType.SERVER_DATA, Pack.Position.TOP,
                    PackSource.BUILT_IN);
//?}
            if (pack != null) output.accept(pack);
        });
    }

    private synchronized Map<ResourceLocation,byte[]> data() {
        if(resources!=null) return resources;
        var generated=new HashMap<ResourceLocation,byte[]>();
        for(var row:gregtech.content.recipe.VanillaCraftingReplacements.rows()) {
            var keys=new LinkedHashMap<String,Object>();
            for(var entry:row.keys().entrySet()) if(row.pattern()==null||row.pattern().indexOf(entry.getKey())>=0)
                keys.put(entry.getKey().toString(),ingredient(entry.getValue()));
            var json=new LinkedHashMap<String,Object>();
            json.put("type",row.pattern()==null?"minecraft:crafting_shapeless":"gt6ce:tool_shaped");
            json.put("group","gt.vanilla");json.put("category","misc");
            if(row.pattern()==null) json.put("ingredients",new ArrayList<>(keys.values()));
            else {json.put("pattern",row.pattern().split("/",-1));json.put("key",keys);json.put("allow_mirror",true);}
            var result=(Map<?,?>)ingredient(row.result().startsWith("material:")?row.result():"item:"+row.result());
//? if >=1.21.1 {
            /*json.put("result",Map.of("id",result.get("item"),"count",row.count()));
            var id=ResourceLocation.parse(row.id());
            generated.put(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"recipe/"+id.getPath()+".json"),new Gson().toJson(json).getBytes(StandardCharsets.UTF_8));
*///?} else {
            json.put("result",Map.of("item",result.get("item"),"count",row.count()));
            var id=ResourceLocation.parse(row.id());
            generated.put(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"recipes/"+id.getPath()+".json"),new Gson().toJson(json).getBytes(StandardCharsets.UTF_8));
//?}
        }
        for(var recipeId:REPLACED_PLANK_RECIPES) disablePlanks(generated,recipeId);
        resources=Map.copyOf(generated);
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original vanilla overrides: {} rows",resources.size());
        return resources;
    }

    /** A failing condition disables the vanilla recipe; the TOP pack makes this file win by id. */
    private static void disablePlanks(Map<ResourceLocation,byte[]> generated,String recipeId) {
        var id=ResourceLocation.parse(recipeId);
        var json=new LinkedHashMap<String,Object>();
        json.put("type","minecraft:crafting_shapeless");
//? if >=1.21.1 {
        /*json.put("neoforge:conditions",List.of(Map.of("type","neoforge:false")));
*///?} else {
        json.put("conditions",List.of(Map.of("type","forge:false")));
//?}
        json.put("ingredients",List.of(Map.of("item","minecraft:barrier")));
//? if >=1.21.1 {
        /*json.put("result",Map.of("id","minecraft:barrier","count",1));
        generated.put(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"recipe/"+id.getPath()+".json"),
                new Gson().toJson(json).getBytes(StandardCharsets.UTF_8));
*///?} else {
        json.put("result",Map.of("item","minecraft:barrier"));
        generated.put(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"recipes/"+id.getPath()+".json"),
                new Gson().toJson(json).getBytes(StandardCharsets.UTF_8));
//?}
    }
    private static Object ingredient(String spec) {
        if(spec.startsWith("tag:")) {
            // Vanilla has no wooden_chests tag; preserve GT6's ordinary chest ingredient.
            return spec.equals("tag:minecraft:wooden_chests")?Map.of("item","minecraft:chest"):Map.of("tag",spec.substring(4));
        }
        if(spec.startsWith("item:")) return Map.of("item",spec.substring(5));
        // GT6 uses OD.craftingFirestarter, including the two early wooden fire starters.
        if(spec.equals("firestarter")) return Map.of("tag","gt6ce:crafting_firestarters");
        if(spec.startsWith("tool:")) return Map.of("item","gt6ce:tool_"+gregtech.engine.tool.GTToolType.valueOf(spec.substring(5).toUpperCase(java.util.Locale.ROOT)).id());
        String[] parts=spec.split(":",3);
        var stack=gregtech.content.registry.GTItems.getStack(gregtech.engine.prefix.PrefixRegistry.byName(parts[1]),
                gregtech.engine.material.GTMaterialRegistry.get(parts[2]),1);
        if(stack.isEmpty()) throw new IllegalStateException("Unbound original vanilla ingredient "+spec);
        return Map.of("item",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
//? if >=1.21.1 {
        /*return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":48,\"description\":"
*///?} else {
        return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":15,\"description\":"
//?}
                + "\"GregTech original vanilla replacements\"}}").getBytes(StandardCharsets.UTF_8));
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
        return type == PackType.SERVER_DATA ? Set.of("gt6ce", "minecraft") : Set.of();
    }

    @Override
    public void close() {
        resources = null;
    }
}
