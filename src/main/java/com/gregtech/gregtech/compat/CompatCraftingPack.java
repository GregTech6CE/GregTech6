package com.gregtech.gregtech.compat;

import com.google.gson.Gson;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.content.compat.CompatSpecs;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

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
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CompatCraftingPack extends AbstractPackResources {
    private static final Gson GSON = new Gson();
    private Map<ResourceLocation, byte[]> resources;

    public CompatCraftingPack(String id) { super(id, true); }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
            Pack pack = Pack.readMetaAndCreate("gregtech:compat_crafting",
                    Component.literal("GregTech mod compat crafting"), true,
                    CompatCraftingPack::new, PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
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
        json.put("result", Map.of("item", result.get("item"), "count", row.output().count()));
        put(generated, new ResourceLocation("gregtech", "recipes/" + row.id() + ".json"), json);
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
        json.put("type", "minecraft:crafting_shaped");
        json.put("group", "gt.compat");
        json.put("pattern", row.pattern());
        json.put("key", key);
        json.put("result", Map.of("item", result.get("item"), "count", row.output().count()));
        put(generated, new ResourceLocation("gregtech", "recipes/" + row.id() + ".json"), json);
    }

    private static void disable(Map<ResourceLocation, byte[]> generated, CompatSpecs.Removal removal) {
        var id = new ResourceLocation(removal.recipeId());
        var json = new LinkedHashMap<String, Object>();
        json.put("type", "minecraft:crafting_shapeless");
        json.put("conditions", List.of(Map.of("type", "forge:false")));
        json.put("ingredients", List.of(Map.of("item", "minecraft:barrier")));
        json.put("result", Map.of("item", "minecraft:barrier"));
        put(generated, new ResourceLocation(id.getNamespace(), "recipes/" + id.getPath() + ".json"), json);
    }

    private static Object ingredient(CompatSpecs.Stack stack) {
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
    }

    private static void put(Map<ResourceLocation, byte[]> generated, ResourceLocation file, Map<String, Object> json) {
        generated.put(file, GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
        return () -> new ByteArrayInputStream("{\"pack\":{\"pack_format\":15,\"description\":\"GregTech mod compat crafting\"}}"
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
        if (names.isEmpty()) names.add("gregtech");
        return names;
    }

    @Override
    public void close() { resources = null; }

    /** Game bus. Runs after the recipe manager because Forge appends these listeners to the reload. */
    @Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
                for (var removal : module.removals()) removed.add(new ResourceLocation(removal.recipeId()));
            }
            if (removed.isEmpty()) return;
            var kept = new ArrayList<Recipe<?>>();
            int dropped = 0;
            for (var recipe : manager.getRecipes()) {
                if (removed.contains(recipe.getId())) dropped++;
                else kept.add(recipe);
            }
            if (dropped == 0) return;
            manager.replaceRecipes(kept);
            com.mojang.logging.LogUtils.getLogger().info("[gregtech] Removed {} compat recipes still provided by a later datapack", dropped);
        }
    }
}
