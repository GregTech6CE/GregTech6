package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.content.tool.CraftingToolDefinitions;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.*;

/** GT6 crafting-only identities. World/tool-click dispatch remains with real hand tools. */
public final class CraftingTools {
    private CraftingTools() {}
    private static String id(ItemStack stack) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("gregtech") ? id.getPath() : "";
    }
    public static boolean matches(ItemStack stack, com.gregtech.gregtech.api.tool.GTToolType kind) {
        return CraftingToolDefinitions.matches(id(stack),kind.id())
            || com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(stack,kind);
    }
    public static boolean infinite(ItemStack stack) { return CraftingToolDefinitions.infinite(id(stack)); }
    private static boolean hasToolItem(com.google.gson.JsonElement json) {
        if(json.isJsonArray()) {
            for(var child : json.getAsJsonArray()) if(hasToolItem(child)) return true;
        } else if(json.isJsonObject()) {
            var object = json.getAsJsonObject();
            return object.has("item") && object.get("item").getAsString().startsWith("gregtech:tool_");
        }
        return false;
    }
    public static Ingredient expand(Ingredient ingredient) {
        if(ingredient.getCustomIngredient() != null) return ingredient;
        // Tag ingredients already contain crafting-only tools; do not enumerate large material tags.
        if(!hasToolItem(Ingredient.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,ingredient).getOrThrow())) return ingredient;
        var kinds = new HashSet<String>();
        for(var stack : ingredient.getItems())
            if(stack.getItem() instanceof GTToolItem tool) kinds.add(tool.toolType().id());
        if(kinds.isEmpty()) return ingredient;
        var stacks = new ArrayList<ItemStack>(Arrays.asList(ingredient.getItems()));
        for(var tool : CraftingToolDefinitions.ALL) {
            if(tool.kinds().stream().noneMatch(kinds::contains)) continue;
            for(String name : List.of(tool.tip(),tool.token())) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",name));
                if(item == null || item == Items.AIR) throw new IllegalStateException("Missing crafting tool " + name);
                if(stacks.stream().noneMatch(s -> s.is(item))) stacks.add(new ItemStack(item));
            }
        }
        return Ingredient.of(stacks.stream());
    }
}
