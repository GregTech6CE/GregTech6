package gregtech.compat.common;

import com.google.gson.Gson;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.compat.CompatSpecs;
import gregtech.content.registry.GTItems;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.RecipeHolder;
*///?} else {
import net.minecraft.world.item.crafting.Recipe;
//?}
import net.minecraft.world.item.crafting.RecipeManager;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.AddReloadListenerEvent;
*///?} else {
import net.minecraftforge.event.AddReloadListenerEvent;
//?}
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
*///?} else {
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
//?}

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds compat crafting rows and disables audited foreign recipes by id.
 * The in-memory pack is not always the last datapack, so a reload listener also drops those ids
 * after the recipe manager has applied every pack.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?}
public final class CompatCraftingPack extends AbstractPackResources {
    private static final Gson GSON = new Gson();
    private Map<ResourceLocation, byte[]> resources;

//? if >=1.21.1 {
    /*public CompatCraftingPack(net.minecraft.server.packs.PackLocationInfo info) { super(info); }
*///?} else {
    public CompatCraftingPack(String id) { super(id, true); }
//?}

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
//? if >=1.21.1 {
            /*var location = new net.minecraft.server.packs.PackLocationInfo("gt6ce:compat_crafting",
                    Component.literal("GregTech mod compat crafting"), PackSource.BUILT_IN, java.util.Optional.empty());
            Pack pack = Pack.readMetaAndCreate(location, new Pack.ResourcesSupplier() {
                public PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info) { return new CompatCraftingPack(info); }
                public PackResources openFull(net.minecraft.server.packs.PackLocationInfo info, Pack.Metadata metadata) { return new CompatCraftingPack(info); }
            }, PackType.SERVER_DATA, new net.minecraft.server.packs.PackSelectionConfig(true, Pack.Position.TOP, true));
*///?} else {
            Pack pack = Pack.readMetaAndCreate("gt6ce:compat_crafting",
                    Component.literal("GregTech mod compat crafting"), true,
                    CompatCraftingPack::new, PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
//?}
            if (pack != null) output.accept(pack);
        });
    }

    private synchronized Map<ResourceLocation, byte[]> data() {
        if (resources != null) return resources;
        var generated = new HashMap<ResourceLocation, byte[]>();
        for (var module : CompatSpecs.modules()) {
            if (!ModList.get().isLoaded(module.modernId())) continue;
            for (var row : module.crafting()) add(generated, row);
            for (var row : module.shaped()) addShaped(generated, row);
            for (var removal : module.removals()) disable(generated, removal);
        }
        resources = Map.copyOf(generated);
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Compat crafting pack: {} files", resources.size());
        return resources;
    }

    private static void add(Map<ResourceLocation, byte[]> generated, CompatSpecs.CraftingRow row) {
        var ingredients = new ArrayList<Object>();
        for (var input : row.inputs()) {
            var ingredient = ingredient(input);
            if (ingredient == null) return;
            ingredients.add(ingredient);
        }
        var output = ingredient(row.output());
        if (!(output instanceof Map<?, ?> result)) return;
        var json = new LinkedHashMap<String, Object>();
        json.put("type", "minecraft:crafting_shapeless");
        json.put("group", "gt.compat");
        json.put("ingredients", ingredients);
//? if >=1.21.1 {
        /*json.put("result", Map.of("id", result.get("item"), "count", row.output().count()));
        put(generated, ResourceLocation.fromNamespaceAndPath("gt6ce", "recipe/" + row.id() + ".json"), json);
*///?} else {
        json.put("result", Map.of("item", result.get("item"), "count", row.output().count()));
        put(generated, ResourceLocation.fromNamespaceAndPath("gt6ce", "recipes/" + row.id() + ".json"), json);
//?}
    }

    private static void addShaped(Map<ResourceLocation, byte[]> generated, CompatSpecs.ShapedRow row) {
        var key = new LinkedHashMap<String, Object>();
        for (var entry : row.keys()) {
            var ingredient = ingredient(entry.stack());
            if (ingredient == null || entry.symbol().length() != 1) return;
            key.put(entry.symbol(), ingredient);
        }
        var output = ingredient(row.output());
        if (!(output instanceof Map<?, ?> result)) return;
        var json = new LinkedHashMap<String, Object>();
        json.put("type", row.toolRecipe() ? "gt6ce:tool_shaped" : "minecraft:crafting_shaped");
        if (row.toolRecipe()) {
            json.put("allow_mirror", false);
        }
        json.put("group", "gt.compat");
        json.put("pattern", row.pattern());
        json.put("key", key);
//? if >=1.21.1 {
        /*json.put("result", Map.of("id", result.get("item"), "count", row.output().count()));
        put(generated, ResourceLocation.fromNamespaceAndPath("gt6ce", "recipe/" + row.id() + ".json"), json);
*///?} else {
        json.put("result", Map.of("item", result.get("item"), "count", row.output().count()));
        put(generated, ResourceLocation.fromNamespaceAndPath("gt6ce", "recipes/" + row.id() + ".json"), json);
//?}
    }

    private static void disable(Map<ResourceLocation, byte[]> generated, CompatSpecs.Removal removal) {
//? if >=1.21.1 {
        /*var id = ResourceLocation.parse(removal.recipeId());
*///?} else {
        var id = ResourceLocation.parse(removal.recipeId());
//?}
        var json = new LinkedHashMap<String, Object>();
        json.put("type", "minecraft:crafting_shapeless");
//? if >=1.21.1 {
        /*json.put("neoforge:conditions", List.of(Map.of("type", "neoforge:false")));
*///?} else {
        json.put("conditions", List.of(Map.of("type", "forge:false")));
//?}
        json.put("ingredients", List.of(Map.of("item", "minecraft:barrier")));
//? if >=1.21.1 {
        /*json.put("result", Map.of("id", "minecraft:barrier", "count", 1));
        put(generated, ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "recipe/" + id.getPath() + ".json"), json);
*///?} else {
        json.put("result", Map.of("item", "minecraft:barrier"));
        put(generated, ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "recipes/" + id.getPath() + ".json"), json);
//?}
    }

    private static Object ingredient(CompatSpecs.Stack stack) {
//? if >=1.21.1 {
        /*return switch (stack) {
            case CompatSpecs.Stack.Item item -> Map.of("item", item.id());
            case CompatSpecs.Stack.Tag tag -> Map.of("tag", tag.neoTag());
            case CompatSpecs.Stack.Form form -> {
                var prefix = PrefixRegistry.byName(form.prefix());
                var material = GTMaterialRegistry.get(form.material());
                if (prefix == null || !material.isValid()) yield null;
                var resolved = GTItems.getStack(prefix, material, 1);
                if (resolved.isEmpty()) yield null;
                var key = BuiltInRegistries.ITEM.getKey(resolved.getItem());
                yield key == null ? null : Map.of("item", key.toString());
            }
        };
*///?} else {
        if (stack instanceof CompatSpecs.Stack.Item item) return Map.of("item", item.id());
        if (stack instanceof CompatSpecs.Stack.Tag tag) return Map.of("tag", tag.forgeTag());
        if (stack instanceof CompatSpecs.Stack.Form form) {
            var prefix = PrefixRegistry.byName(form.prefix());
            var material = GTMaterialRegistry.get(form.material());
            if (prefix == null || !material.isValid()) return null;
            var resolved = GTItems.getStack(prefix, material, 1);
            if (resolved.isEmpty()) return null;
            var key = ForgeRegistries.ITEMS.getKey(resolved.getItem());
            return key == null ? null : Map.of("item", key.toString());
        }
        throw new IllegalStateException(stack.getClass().getName());
//?}
    }

    private static void put(Map<ResourceLocation, byte[]> generated, ResourceLocation file, Map<String, Object> json) {
        generated.put(file, GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
//? if >=1.21.1 {
        /*return () -> new ByteArrayInputStream("{\"pack\":{\"pack_format\":48,\"description\":\"GregTech mod compat crafting\"}}"
*///?} else {
        return () -> new ByteArrayInputStream("{\"pack\":{\"pack_format\":15,\"description\":\"GregTech mod compat crafting\"}}"
//?}
                .getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type != PackType.SERVER_DATA) return null;
        byte[] bytes = data().get(id);
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, PackResources.ResourceOutput output) {
        if (type != PackType.SERVER_DATA) return;
        data().forEach((id, bytes) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(path + "/"))
                output.accept(id, () -> new ByteArrayInputStream(bytes));
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        if (type != PackType.SERVER_DATA) return Set.of();
        var names = new java.util.HashSet<String>();
        data().keySet().forEach(id -> names.add(id.getNamespace()));
        if (names.isEmpty()) names.add("gt6ce");
        return names;
    }

    @Override
    public void close() { resources = null; }

//? if >=1.21.1 {
    /*/^* Game bus. Runs after the recipe manager because NeoForge appends these listeners to the reload. ^/
    @EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
    /** Game bus. Runs after the recipe manager because Forge appends these listeners to the reload. */
    @Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?}
    static final class Removals {
        private Removals() {}

        @SubscribeEvent
        public static void reload(AddReloadListenerEvent event) {
            var manager = event.getServerResources().getRecipeManager();
            event.addListener((barrier, resources, preparations, reload, background, game) ->
                    barrier.wait(null).thenRunAsync(() -> strip(manager), game));
        }

        private static void strip(RecipeManager manager) {
            var removed = new HashSet<ResourceLocation>();
            for (var module : CompatSpecs.modules()) {
                if (!ModList.get().isLoaded(module.modernId())) continue;
//? if >=1.21.1 {
                /*for (var removal : module.removals()) removed.add(ResourceLocation.parse(removal.recipeId()));
*///?} else {
                for (var removal : module.removals()) removed.add(ResourceLocation.parse(removal.recipeId()));
//?}
            }
            if (removed.isEmpty()) return;
//? if >=1.21.1 {
            /*var kept = new ArrayList<RecipeHolder<?>>();
*///?} else {
            var kept = new ArrayList<Recipe<?>>();
//?}
            int dropped = 0;
//? if >=1.21.1 {
            /*for (var holder : manager.getRecipes()) {
                if (removed.contains(holder.id())) dropped++;
                else kept.add(holder);
*///?} else {
            for (var recipe : manager.getRecipes()) {
                if (removed.contains(recipe.getId())) dropped++;
                else kept.add(recipe);
//?}
            }
            if (dropped == 0) return;
            manager.replaceRecipes(kept);
            com.mojang.logging.LogUtils.getLogger().info("[gregtech] Removed {} compat recipes still provided by a later datapack", dropped);
        }
    }
}
