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
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.SubscribeEvent;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?}
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Registry item references available before baking; geometry still comes from the original templates.
 * The low-priority built-in pack permits ordinary resource packs to override its JSON.
 * The geometry loader delegates to ModelBaker's existing cache instead of baking 56,000 copies.
 */
//? if >=1.21.1 {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class SharedInventoryModelPack extends AbstractPackResources {
    private Map<ResourceLocation, byte[]> resources;
//? if >=1.21.1 {
    /*public SharedInventoryModelPack(PackLocationInfo info) { super(info); }
*///?} else {
    public SharedInventoryModelPack(String id) { super(id, true); }
//?}

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        event.addRepositorySource(output -> {
//? if >=1.21.1 {
            /*var location = new PackLocationInfo("gt6ce:shared_inventory", Component.literal("GregTech shared inventory models"), PackSource.BUILT_IN, Optional.empty());
            var pack = Pack.readMetaAndCreate(location, new Pack.ResourcesSupplier() {
                public PackResources openPrimary(PackLocationInfo info) { return new SharedInventoryModelPack(info); }
                public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) { return new SharedInventoryModelPack(info); }
            }, PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.BOTTOM, true));
*///?} else {
            var pack = Pack.readMetaAndCreate("gt6ce:shared_inventory", Component.literal("GregTech shared inventory models"), true,
                    SharedInventoryModelPack::new, PackType.CLIENT_RESOURCES, Pack.Position.BOTTOM, PackSource.BUILT_IN);
//?}
            if (pack != null) output.accept(pack);
        });
    }

    @SubscribeEvent
    public static void loaders(ModelEvent.RegisterGeometryLoaders event) {
//? if >=1.21.1 {
        /*event.register(ResourceLocation.fromNamespaceAndPath(com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, "shared_inventory"),
                (net.neoforged.neoforge.client.model.geometry.IGeometryLoader<Reference>)
                        (json, context) -> new Reference(ResourceLocation.parse(json.get("parent").getAsString())));
*///?} else {
        event.register("shared_inventory", (net.minecraftforge.client.model.geometry.IGeometryLoader<Reference>)
                (json, context) -> new Reference(ResourceLocation.tryParse(json.get("parent").getAsString())));
//?}
    }

    private synchronized Map<ResourceLocation, byte[]> data() {
        if (resources != null) return resources;
        Map<ResourceLocation, byte[]> result = new HashMap<>();
        Map<ResourceLocation, byte[]> references = new HashMap<>();
        emitStructureParts(result);
        emitAddonPipes(result);
        emitCompressedTanks(result);
        emitAddonBlockPrefixes(result);
        for (var item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (item instanceof com.gregtech.gregtech.api.material.MaterialFormItem material && material.getPrefix().isAddon()) {
                result.put(itemModel(id), MaterialIconDefinitions.addonItemModel(material.getPrefix()).getBytes(StandardCharsets.UTF_8));
                continue;
            }
            if (id.getNamespace().equals("gt6ce") && !id.getPath().equals("bottle_crate")
                    && item instanceof BlockItem crateItem
                    && crateItem.getBlock() instanceof com.gregtech.gregtech.block.inventory.BottleCrateBlock crate) {
                String model = "block/inventory/" + id.getPath();
                String blockJson = "{\"parent\":\"gt6ce:block/inventory/bottle_crate_tinted\",\"textures\":{\"wood\":\"" + crate.frameTexture() + "\"}}";
                result.put(location("models/" + model + ".json"), blockJson.getBytes(StandardCharsets.UTF_8));
                result.put(location("blockstates/" + id.getPath() + ".json"),
                        ("{\"variants\":{\"\":{\"model\":\"gt6ce:" + model + "\"}}}").getBytes(StandardCharsets.UTF_8));
                result.put(location("models/item/" + id.getPath() + ".json"),
                        ("{\"parent\":\"gt6ce:" + model + "\"}").getBytes(StandardCharsets.UTF_8));
                continue;
            }
            if (!id.getNamespace().equals("gt6ce") || SharedInventoryModelPack.class.getResource(
                    "/assets/gt6ce/models/item/" + id.getPath() + ".json") != null) continue;
            ResourceLocation parent = null;
            if (item instanceof com.gregtech.gregtech.api.material.MaterialFormItem material) {
                parent = MaterialIcons.sharedModelLocation(MaterialIcons.resolveTextureSet(material.getMaterial()), material.getPrefix());
//? if >=1.21.1 {
            /*} else if (item instanceof com.gregtech.gregtech.platform.neoforge.fluid.FluidDisplayItem) {
*///?} else {
            } else if (item instanceof com.gregtech.gregtech.item.FluidItem) {
//?}
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
        // Material bushes share the existing 28 geometries; resource packs can override each identity.
        byte[] bushStates=com.gregtech.gregtech.content.plant.MaterialBerryBushCatalog.blockstateJson().getBytes(StandardCharsets.UTF_8);
        byte[] bushItem=MaterialIconDefinitions.sharedInventoryJson("gt6ce:block/plants/bush_stage3").getBytes(StandardCharsets.UTF_8);
        for(var block:com.gregtech.gregtech.registry.GTBushes.allBlocks()) {
            if(!((com.gregtech.gregtech.block.plant.BushBlock)block).berryId().startsWith("gt6ce:plant_gt_berry_"))continue;
            var id=BuiltInRegistries.BLOCK.getKey(block);
            if(SharedInventoryModelPack.class.getResource("/assets/gt6ce/blockstates/"+id.getPath()+".json")==null)
                result.put(location("blockstates/"+id.getPath()+".json"),bushStates);
            if(SharedInventoryModelPack.class.getResource("/assets/gt6ce/models/item/"+id.getPath()+".json")==null)
                result.put(location("models/item/"+id.getPath()+".json"),bushItem);
        }
        // Registration gained material forms after the original static data generation.
        // Supply only absent states, using the same real template as the existing alias hook.
        int states = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof MaterialBlockLike material)) continue;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("gt6ce") || SharedInventoryModelPack.class.getResource(
                    "/assets/gt6ce/blockstates/" + id.getPath() + ".json") != null) continue;
            var parent = BlockMaterialIcons.sharedModelLocation(MaterialIcons.resolveTextureSet(material.material()), material.prefix());
            if (SharedInventoryModelPack.class.getResource("/assets/gt6ce/models/" + parent.getPath() + ".json") == null) continue;
            byte[] json = ("{\"variants\":{\"\":{\"model\":\"" + parent + "\"}}}").getBytes(StandardCharsets.UTF_8);
            result.put(location("blockstates/" + id.getPath() + ".json"), json);
            states++;
        }
        resources = Collections.unmodifiableMap(result);
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Shared inventory resources: {} item references, {} templates, {} missing static blockstates",
                result.size() - states, references.size(), states);
        return resources;
    }

    private static void emitStructureParts(Map<ResourceLocation, byte[]> result) {
        for (var part : com.gregtech.gregtech.registry.GTMultiblocks.addonParts()) {
            ResourceLocation id = part.block().getId();
            String texture = part.texture() == null ? "" : part.texture();
            boolean energy = part.block().get() instanceof com.gregtech.gregtech.block.machine.AddonEnergyPartBlock;
            boolean functional = part.block().get() instanceof com.gregtech.gregtech.block.machine.AddonFunctionalPartBlock;
            String model = texture.isEmpty()
                    ? "gt6ce:block/machine/multiblock/steel_wall"
                    : id.getNamespace() + ":block/" + id.getPath();
            if (!texture.isEmpty()) {
                String cube = "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\""
                        + id.getNamespace() + ":" + texture + "\"}}";
                result.put(named(id.getNamespace(), "models/block/" + id.getPath() + ".json"), cube.getBytes(StandardCharsets.UTF_8));
            }
            String blockstate = energy
                    ? facingBlockstate(model)
                    : functional ? facingOnlyBlockstate(model)
                    : "{\"variants\":{\"\":{\"model\":\"" + model + "\"}}}";
            result.put(named(id.getNamespace(), "blockstates/" + id.getPath() + ".json"), blockstate.getBytes(StandardCharsets.UTF_8));
            result.put(named(id.getNamespace(), "models/item/" + id.getPath() + ".json"),
                    ("{\"parent\":\"" + model + "\"}").getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String facingBlockstate(String model) {
        String variant = "{\"model\":\"" + model + "\"}";
        var json = new StringBuilder("{\"variants\":{");
        boolean first = true;
        for (String facing : new String[] {"north", "south", "west", "east"}) {
            for (String satisfied : new String[] {"false", "true"}) {
                if (!first) json.append(',');
                first = false;
                json.append("\"facing=").append(facing).append(",satisfied=").append(satisfied).append("\":").append(variant);
            }
        }
        return json.append("}}").toString();
    }

    private static String facingOnlyBlockstate(String model) {
        String variant = "{\"model\":\"" + model + "\"}";
        return "{\"variants\":{"
                + "\"facing=north\":" + variant + ","
                + "\"facing=south\":" + variant + ","
                + "\"facing=west\":" + variant + ","
                + "\"facing=east\":" + variant
                + "}}";
    }

    /** Iron cube so a drum has a model before an addon ships its own. */
    private static void emitCompressedTanks(Map<ResourceLocation, byte[]> result) {
        byte[] model = "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"minecraft:block/iron_block\"}}"
                .getBytes(StandardCharsets.UTF_8);
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof com.gregtech.gregtech.block.machine.CompressedGasTankBlock)) continue;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            String path = id.getPath();
            result.put(named(id.getNamespace(), "models/block/" + path + ".json"), model);
            result.put(named(id.getNamespace(), "blockstates/" + path + ".json"),
                    ("{\"variants\":{\"\":{\"model\":\"" + id.getNamespace() + ":block/" + path + "\"}}}")
                            .getBytes(StandardCharsets.UTF_8));
            result.put(named(id.getNamespace(), "models/item/" + path + ".json"),
                    ("{\"parent\":\"" + id.getNamespace() + ":block/" + path + "\"}").getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Placeholder so the bakery has a model to replace with the dynamic pipe shape. */
    private static void emitAddonPipes(Map<ResourceLocation, byte[]> result) {
        String blockstate = "{\"variants\":{\"\":{\"model\":\"gt6ce:block/machine/pipe_tiny_steel_core\"}}}";
        String item = "{\"parent\":\"gt6ce:block/machine/pipe_tiny_steel_core\"}";
        for (var block : com.gregtech.gregtech.registry.AddonPipeRegistration.fluidBlocks()) emitPipe(result, block, blockstate, item);
        for (var block : com.gregtech.gregtech.registry.AddonPipeRegistration.itemBlocks()) emitPipe(result, block, blockstate, item);
    }

    private static void emitPipe(Map<ResourceLocation, byte[]> result, net.minecraft.world.level.block.Block block,
                                 String blockstate, String item) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        result.put(named(id.getNamespace(), "blockstates/" + id.getPath() + ".json"), blockstate.getBytes(StandardCharsets.UTF_8));
        result.put(named(id.getNamespace(), "models/item/" + id.getPath() + ".json"), item.getBytes(StandardCharsets.UTF_8));
    }

//? if >=1.21.1 {
    /*private static ResourceLocation location(String path) { return ResourceLocation.fromNamespaceAndPath("gt6ce", path); }
    private static ResourceLocation itemModel(ResourceLocation item) {
        return ResourceLocation.fromNamespaceAndPath(item.getNamespace(), "models/item/" + item.getPath() + ".json");
    }
    private static ResourceLocation named(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
*///?} else {
    private static ResourceLocation location(String path) { return new ResourceLocation("gt6ce", path); }
    private static ResourceLocation itemModel(ResourceLocation item) {
        return new ResourceLocation(item.getNamespace(), "models/item/" + item.getPath() + ".json");
    }
    private static ResourceLocation named(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
//?}

    private static void emitAddonBlockPrefixes(Map<ResourceLocation, byte[]> result) {
        var emitted = new HashSet<String>();
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof MaterialBlockLike like) || !like.prefix().isAddon()) continue;
            var prefix = like.prefix();
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || !emitted.add(prefix.getTextureFileName())) continue;
            String texture = prefix.textureNamespace() + ":" + prefix.addonTexturePath();
            String model = "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"" + texture
                    + "\",\"particle\":\"" + texture + "\"}}";
            result.put(location("models/block/material/addon/" + prefix.getTextureFileName() + ".json"),
                    model.getBytes(StandardCharsets.UTF_8));
        }
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof MaterialBlockLike like) || !like.prefix().isAddon()) continue;
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || !"gt6ce".equals(id.getNamespace())) continue;
            String parent = "gt6ce:block/material/addon/" + like.prefix().getTextureFileName();
            result.put(location("blockstates/" + id.getPath() + ".json"),
                    ("{\"variants\":{\"\":{\"model\":\"" + parent + "\"}}}").getBytes(StandardCharsets.UTF_8));
            result.put(location("models/item/" + id.getPath() + ".json"),
                    ("{\"parent\":\"" + parent + "\"}").getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
//? if >=1.21.1 {
        /*return () -> new ByteArrayInputStream("{\"pack\":{\"pack_format\":34,\"description\":\"GregTech shared inventory models\"}}".getBytes(StandardCharsets.UTF_8));
*///?} else {
        return () -> new ByteArrayInputStream("{\"pack\":{\"pack_format\":15,\"description\":\"GregTech shared inventory models\"}}".getBytes(StandardCharsets.UTF_8));
//?}
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
        if (type != PackType.CLIENT_RESOURCES) return Set.of();
        var names = new HashSet<String>();
        names.add("gt6ce");
        for (var prefix : com.gregtech.gregtech.api.prefix.PrefixRegistry.addonPrefixes())
            names.add(prefix.itemNamespace());
        for (var part : com.gregtech.gregtech.registry.GTMultiblocks.addonParts())
            names.add(part.block().getId().getNamespace());
        for (var block : BuiltInRegistries.BLOCK)
            if (block instanceof com.gregtech.gregtech.block.machine.CompressedGasTankBlock)
                names.add(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
        names.addAll(com.gregtech.gregtech.registry.AddonPipeRegistration.namespaces());
        return Set.copyOf(names);
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
//? if >=1.21.1 {
                /*net.minecraft.client.renderer.block.model.ItemOverrides overrides) {
*///?} else {
                net.minecraft.client.renderer.block.model.ItemOverrides overrides, ResourceLocation modelLocation) {
//?}
            return baker.bake(parent, state);
        }
    }
}
