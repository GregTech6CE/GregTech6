package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.material.MaterialIconDefinitions;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.block.MaterialBlockLike;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.item.CreativeTabIconItem;
import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Registry item references available before baking; geometry still comes from the original templates.
 * The low-priority built-in pack permits ordinary resource packs to override its JSON.
 * The geometry loader delegates to ModelBaker's existing cache instead of baking 56,000 copies.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SharedInventoryModelPack extends AbstractPackResources {
    private Map<ResourceLocation, byte[]> resources;
    public SharedInventoryModelPack(String id) { super(id, true); }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        event.addRepositorySource(output -> {
            var pack = Pack.readMetaAndCreate("gregtech:shared_inventory", Component.literal("GregTech shared inventory models"), true,
                    SharedInventoryModelPack::new, PackType.CLIENT_RESOURCES, Pack.Position.BOTTOM, PackSource.BUILT_IN);
            if (pack != null) output.accept(pack);
        });
    }

    @SubscribeEvent
    public static void loaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("shared_inventory", (net.minecraftforge.client.model.geometry.IGeometryLoader<Reference>)
                (json, context) -> new Reference(ResourceLocation.tryParse(json.get("parent").getAsString())));
    }

    private synchronized Map<ResourceLocation, byte[]> data() {
        if (resources != null) return resources;
        Map<ResourceLocation, byte[]> result = new HashMap<>();
        Map<ResourceLocation, byte[]> references = new HashMap<>();
        for (var item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id.getNamespace().equals("gregtech") && !id.getPath().equals("bottle_crate")
                    && item instanceof BlockItem crateItem
                    && crateItem.getBlock() instanceof com.gregtech.gregtech.block.inventory.BottleCrateBlock crate) {
                String model = "block/inventory/" + id.getPath();
                String blockJson = "{\"parent\":\"gregtech:block/inventory/bottle_crate_tinted\",\"textures\":{\"wood\":\"" + crate.frameTexture() + "\"}}";
                result.put(location("models/" + model + ".json"), blockJson.getBytes(StandardCharsets.UTF_8));
                result.put(location("blockstates/" + id.getPath() + ".json"),
                        ("{\"variants\":{\"\":{\"model\":\"gregtech:" + model + "\"}}}").getBytes(StandardCharsets.UTF_8));
                result.put(location("models/item/" + id.getPath() + ".json"),
                        ("{\"parent\":\"gregtech:" + model + "\"}").getBytes(StandardCharsets.UTF_8));
                continue;
            }
            if (!id.getNamespace().equals("gregtech") || SharedInventoryModelPack.class.getResource(
                    "/assets/gregtech/models/item/" + id.getPath() + ".json") != null) continue;
            ResourceLocation parent = null;
            if (item instanceof MaterialItem material) {
                parent = MaterialIcons.sharedModelLocation(MaterialIcons.resolveTextureSet(material.getMaterial()), material.getPrefix());
            } else if (item instanceof com.gregtech.gregtech.item.FluidItem) {
                parent = location("item/fluid_item");
            } else if (item instanceof CreativeTabIconItem icon) {
                if (icon.materialPrefix() != null) {
                    parent = MaterialIcons.sharedModelLocation(MaterialTextureSet.NONE, icon.materialPrefix());
                } else if (icon.blockPrefix() != null) {
                    parent = BlockMaterialIcons.sharedModelLocation(MaterialTextureSet.NONE, icon.blockPrefix());
                }
            } else if (item instanceof BlockItem blockItem) {
                if (blockItem.getBlock() instanceof BasicMachineBlock machine) {
                    parent = location("block/machine/basic/" + machine.basicSpec().machineName());
                } else if (blockItem.getBlock() instanceof MaterialBlockLike material) {
                    parent = BlockMaterialIcons.sharedModelLocation(MaterialIcons.resolveTextureSet(material.material()), material.prefix());
                }
            }
            // Do not invent a missing template or hide it behind a dummy model. Unsupported
            // references remain visible in the usual bakery warning and final model checks.
            if (parent == null || SharedInventoryModelPack.class.getResource("/assets/" + parent.getNamespace()
                    + "/models/" + parent.getPath() + ".json") == null) continue;
            byte[] json = references.computeIfAbsent(parent, key ->
                    MaterialIconDefinitions.sharedInventoryJson(key.toString()).getBytes(StandardCharsets.UTF_8));
            result.put(location("models/item/" + id.getPath() + ".json"), json);
        }
        // Registration gained material forms after the original static data generation.
        // Supply only absent states, using the same real template as the existing alias hook.
        int states = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof MaterialBlockLike material)) continue;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("gregtech") || SharedInventoryModelPack.class.getResource(
                    "/assets/gregtech/blockstates/" + id.getPath() + ".json") != null) continue;
            var parent = BlockMaterialIcons.sharedModelLocation(MaterialIcons.resolveTextureSet(material.material()), material.prefix());
            if (SharedInventoryModelPack.class.getResource("/assets/gregtech/models/" + parent.getPath() + ".json") == null) continue;
            byte[] json = ("{\"variants\":{\"\":{\"model\":\"" + parent + "\"}}}").getBytes(StandardCharsets.UTF_8);
            result.put(location("blockstates/" + id.getPath() + ".json"), json);
            states++;
        }
        resources = Collections.unmodifiableMap(result);
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Shared inventory resources: {} item references, {} templates, {} missing static blockstates",
                result.size() - states, references.size(), states);
        return resources;
    }

    private static ResourceLocation location(String path) { return new ResourceLocation("gregtech", path); }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
        return () -> new ByteArrayInputStream("{\"pack\":{\"pack_format\":15,\"description\":\"GregTech shared inventory models\"}}".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type != PackType.CLIENT_RESOURCES) return null;
        byte[] json = data().get(id);
        return json == null ? null : () -> new ByteArrayInputStream(json);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) return;
        data().forEach((id, json) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(path + "/"))
                output.accept(id, () -> new ByteArrayInputStream(json));
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of("gregtech") : Set.of();
    }

    @Override
    public void close() { resources = null; }

    private record Reference(ResourceLocation parent) implements IUnbakedGeometry<Reference> {
        @Override
        public net.minecraft.client.resources.model.BakedModel bake(IGeometryBakingContext context,
                net.minecraft.client.resources.model.ModelBaker baker,
                java.util.function.Function<net.minecraft.client.resources.model.Material,
                        net.minecraft.client.renderer.texture.TextureAtlasSprite> sprites,
                net.minecraft.client.resources.model.ModelState state,
                net.minecraft.client.renderer.block.model.ItemOverrides overrides, ResourceLocation modelLocation) {
            return baker.bake(parent, state);
        }
    }
}
