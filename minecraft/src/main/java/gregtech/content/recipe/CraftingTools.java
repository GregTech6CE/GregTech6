package gregtech.content.recipe;

import gregtech.content.tool.CraftingToolDefinitions;
import gregtech.content.item.GTToolItem;
import gregtech.platform.Mc;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.*;

/** GT6 crafting-only identities. World/tool-click dispatch remains with real hand tools. */
public final class CraftingTools {
    private CraftingTools() {}
    private static String id(ItemStack stack) {
        var id = Mc.itemId(stack.getItem());
        return id != null && id.getNamespace().equals("gt6ce") ? id.getPath() : "";
    }
    public static boolean matches(ItemStack stack, gregtech.engine.tool.GTToolType kind) {
        return CraftingToolDefinitions.matches(id(stack),kind.id())
            || gregtech.engine.tool.GTToolHelper.matchesTool(stack,kind);
    }
    public static boolean infinite(ItemStack stack) { return CraftingToolDefinitions.infinite(id(stack)); }
    private static boolean hasToolItem(com.google.gson.JsonElement json) {
        if(json.isJsonArray()) {
            for(var child : json.getAsJsonArray()) if(hasToolItem(child)) return true;
        } else if(json.isJsonObject()) {
            var object = json.getAsJsonObject();
            return object.has("item") && object.get("item").getAsString().startsWith("gt6ce:tool_");
        }
        return false;
    }
    /** The kind of a {@code forge:tools/<kind>} tag ingredient, or null for any other ingredient. */
    private static String forgeToolKind(com.google.gson.JsonElement json) {
        if (!json.isJsonObject()) return null;
        var object = json.getAsJsonObject();
        if (!object.has("tag")) return null;
        String tag = object.get("tag").getAsString();
        return tag.startsWith("forge:tools/") ? tag.substring("forge:tools/".length()) : null;
    }
    private static boolean isGtItem(net.minecraft.world.item.Item item) {
        var key = BuiltInRegistries.ITEM.getKey(item);
        return key != null && key.getNamespace().equals("gt6ce");
    }
    public static Ingredient expand(Ingredient ingredient) {
//? if >=1.21.1 {
        /*if(ingredient.getCustomIngredient() != null) return ingredient;
        var json = Ingredient.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,ingredient).getOrThrow();
*///?} else {
        if(ingredient.getClass() != Ingredient.class) return ingredient;
        var json = ingredient.toJson();
//?}
        // GT6 recipes consume GT tools through their own wear rules. Other mods' tools carrying the
        // same forge identity would be matched and then consumed by vanilla, so expand tool tags to
        // GT items only; machine interaction keeps the full forge tag.
        String toolTag = forgeToolKind(json);
        if (toolTag != null) {
            var stacks = new ArrayList<ItemStack>();
            for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(
                    ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "tools/" + toolTag)))) {
                var item = holder.value();
                if (isGtItem(item)) stacks.add(new ItemStack(item));
            }
            return stacks.isEmpty() ? ingredient : Ingredient.of(stacks.stream());
        }
        // Tag ingredients already contain crafting-only tools; do not enumerate large material tags.
        if(!hasToolItem(json)) return ingredient;
        var kinds = new HashSet<String>();
        for(var stack : ingredient.getItems())
            if(stack.getItem() instanceof GTToolItem tool) kinds.add(tool.toolType().id());
        if(kinds.isEmpty()) return ingredient;
        var stacks = new ArrayList<ItemStack>(Arrays.asList(ingredient.getItems()));
        for(var kind : kinds) {
            var tag = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge","tools/"+kind));
            for(var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                var item = holder.value();
                if(isGtItem(item) && stacks.stream().noneMatch(s -> s.is(item))) stacks.add(new ItemStack(item));
            }
        }
        for(var tool : CraftingToolDefinitions.ALL) {
            if(tool.kinds().stream().noneMatch(kinds::contains)) continue;
            for(String name : List.of(tool.tip(),tool.token())) {
                var item = Mc.item(ResourceLocation.fromNamespaceAndPath("gt6ce",name));
                if(item == null || item == Items.AIR) throw new IllegalStateException("Missing crafting tool " + name);
                if(stacks.stream().noneMatch(s -> s.is(item))) stacks.add(new ItemStack(item));
            }
        }
        // Network ingredients are already expanded. Keep re-expansion idempotent for every tool family.
        for(var holder:gregtech.content.registry.GTToolItems.all().values())if(holder.get() instanceof gregtech.content.item.PocketToolItem pocket&&kinds.contains(pocket.activeType().id())&&stacks.stream().noneMatch(s->s.is(pocket)))stacks.add(new ItemStack(pocket));
        for(var spec:gregtech.content.tool.ElectricToolCatalog.ALL){var item=gregtech.content.registry.GTElectricItems.get(spec.id());var sample=new ItemStack(item);var active=item.interactionType(sample);if(active!=null&&kinds.contains(active.id())&&stacks.stream().noneMatch(s->s.is(item)))stacks.add(sample);}
        return Ingredient.of(stacks.stream());
    }
}
