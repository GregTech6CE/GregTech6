package com.gregtech.gregtech.compat;

import com.google.gson.Gson;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.content.compat.CompatSpecs;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Adds compat crafting rows and disables audited foreign recipes by id. Loaded above mod datapacks. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class CompatCraftingPack extends AbstractPackResources {
    private static final Gson GSON = new Gson();
    private Map<ResourceLocation, byte[]> resources;

    public CompatCraftingPack(net.minecraft.server.packs.PackLocationInfo info) { super(info); }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
            var location = new net.minecraft.server.packs.PackLocationInfo("gregtech:compat_crafting",
                    Component.literal("GregTech mod compat crafting"), PackSource.BUILT_IN, java.util.Optional.empty());
            Pack pack = Pack.readMetaAndCreate(location, new Pack.ResourcesSupplier() {
                public PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info) { return new CompatCraftingPack(info); }
                public PackResources openFull(net.minecraft.server.packs.PackLocationInfo info, Pack.Metadata metadata) { return new CompatCraftingPack(info); }
            }, PackType.SERVER_DATA, new net.minecraft.server.packs.PackSelectionConfig(true, Pack.Position.TOP, true));
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
        json.put("result", Map.of("id", result.get("item"), "count", row.output().count()));
        put(generated, ResourceLocation.fromNamespaceAndPath("gregtech", "recipe/" + row.id() + ".json"), json);
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
        json.put("result", Map.of("id", result.get("item"), "count", row.output().count()));
        put(generated, ResourceLocation.fromNamespaceAndPath("gregtech", "recipe/" + row.id() + ".json"), json);
    }

    private static void disable(Map<ResourceLocation, byte[]> generated, CompatSpecs.Removal removal) {
        var id = ResourceLocation.parse(removal.recipeId());
        var json = new LinkedHashMap<String, Object>();
        json.put("type", "minecraft:crafting_shapeless");
        json.put("neoforge:conditions", List.of(Map.of("type", "neoforge:false")));
        json.put("ingredients", List.of(Map.of("item", "minecraft:barrier")));
        json.put("result", Map.of("id", "minecraft:barrier", "count", 1));
        put(generated, ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "recipe/" + id.getPath() + ".json"), json);
    }

    private static Object ingredient(CompatSpecs.Stack stack) {
        return switch (stack) {
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
    }

    private static void put(Map<ResourceLocation, byte[]> generated, ResourceLocation file, Map<String, Object> json) {
        generated.put(file, GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
        return () -> new ByteArrayInputStream("{\"pack\":{\"pack_format\":48,\"description\":\"GregTech mod compat crafting\"}}"
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
}
