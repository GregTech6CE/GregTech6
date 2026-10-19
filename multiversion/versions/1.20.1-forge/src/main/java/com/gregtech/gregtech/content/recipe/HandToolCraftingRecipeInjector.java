package com.gregtech.gregtech.content.recipe;
import com.google.gson.JsonParser;
import com.gregtech.gregtech.data.OriginalHandCraftingRows;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Forge reload adapter for the shared original hand-crafting rows. */
public final class HandToolCraftingRecipeInjector {
    private HandToolCraftingRecipeInjector(){}
    private static final List<String> REGISTERED=new ArrayList<>();
    public static List<String> registeredIds(){return List.copyOf(REGISTERED);}
    public static void apply(RecipeManager manager,net.minecraft.core.RegistryAccess access){
        Map<ResourceLocation,byte[]> rows=new LinkedHashMap<>();
        OriginalHandCraftingRows.add(rows);
        List<Recipe<?>> recipes=new ArrayList<>(manager.getRecipes());
        REGISTERED.clear();
        for(var entry:rows.entrySet()){
            var path=entry.getKey().getPath();
            if(!path.startsWith("recipe/")||!path.endsWith(".json"))throw new IllegalStateException("Invalid hand recipe resource "+entry.getKey());
            var id=ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(),path.substring(7,path.length()-5));
            var json=JsonParser.parseString(new String(entry.getValue(),StandardCharsets.UTF_8)).getAsJsonObject();
            var type=ResourceLocation.parse(json.get("type").getAsString());
            var serializer=BuiltInRegistries.RECIPE_SERIALIZER.get(type);
            if(serializer==null)throw new IllegalStateException("Unknown hand recipe serializer "+type);
            recipes.add(serializer.fromJson(id,json));REGISTERED.add(id.toString());
        }
        if(!REGISTERED.isEmpty())com.gregtech.gregtech.recipe.RuntimeRecipeLifecycle.replaceGenerated(manager,recipes);
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Shared original hand recipes: {}",REGISTERED.size());
    }
}
