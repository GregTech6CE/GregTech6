package com.gregtech.gregtech.content.recipe;

import com.google.gson.*;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.content.transport.TransportCraftingCatalog;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.*;

/** Native form/registry lookup for the shared original registration-pattern catalog. */
public final class TransportCraftingInputs {
    private TransportCraftingInputs() {}
    public static Ingredient resolve(TransportCraftingCatalog.Input input){
        if(input.kind().equals("tag"))return Ingredient.of(net.minecraft.tags.ItemTags.create(ResourceLocation.parse(input.name())));
        if(input.kind().equals("item")){
            var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(input.name()));
            return item==Items.AIR?Ingredient.EMPTY:Ingredient.of(item);
        }
        var prefix=PrefixRegistry.byName(input.name());
        if(prefix==null)return Ingredient.EMPTY;
        var material=input.material();
        var materials=material.getId()<0?material.getReRegistrations():Set.of(material);
        var stacks=materials.stream().map(m->GTItems.getStack(prefix,m)).filter(s->!s.isEmpty()).toList();
        return stacks.isEmpty()?Ingredient.EMPTY:Ingredient.of(stacks.stream());
    }
    public static Optional<JsonObject> json(TransportCraftingCatalog.Row row){
        var key=new JsonObject();
        var ingredients=new JsonArray();
        for(var entry:row.key().entrySet()){
            if(!row.unpack()&&row.pattern().stream().noneMatch(p->p.indexOf(entry.getKey())>=0))continue;
            var ingredient=resolve(entry.getValue());
            if(ingredient==Ingredient.EMPTY)return Optional.empty();
            var encoded=ingredient.toJson();
            if(row.unpack())ingredients.add(encoded);else key.add(entry.getKey().toString(),encoded);
        }
        var result=new JsonObject(); result.addProperty("item",row.output()); result.addProperty("count",row.count());
        var json=new JsonObject(); json.addProperty("type",row.unpack()?"gregtech:tool_shapeless":"gregtech:tool_shaped");
        json.addProperty("group","gt.hand");json.addProperty("allow_mirror",false);
        json.addProperty("require_empty_fluid_containers",row.empty());
        if(row.unpack())json.add("ingredients",ingredients);else {var pattern=new JsonArray();row.pattern().forEach(pattern::add);json.add("pattern",pattern);json.add("key",key);}
        json.add("result",result);return Optional.of(json);
    }
}
