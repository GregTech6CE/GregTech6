package gregtech.client;

import gregtech.engine.material.MaterialIconDefinitions;
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.block.OreBlock;
import gregtech.content.book.BookShelfVariants;
import gregtech.content.item.CreativeTabIconItem;
import gregtech.content.registry.AddonPipeRegistration;
import gregtech.content.registry.GTAxles;
import gregtech.content.registry.GTFluidPipes;
import gregtech.content.registry.GTItemPipes;
import gregtech.content.registry.GTSignalWires;
import gregtech.content.registry.GTWires;
import gregtech.engine.machine.OrthogonalMachineModels;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.prefix.BlockMaterialPrefix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

/**
 * Virtual client pack: the item references, shared material models, machine blockstates and machine
 * item models are generated at runtime instead of shipped as static JSON.
 *
 * <p>The pack is hidden and sits at the bottom position, so an installed resource pack overrides any
 * file it serves, blockstates, table row models and item models included. Machine geometry comes
 * from {@code machine_models.tsv} through {@link OrthogonalMachineModelLoader}, whose loader
 * delegates to ModelBaker's existing cache instead of baking 56,000 copies.
 *
 * <p>Material icons are resolved against the packs the resource manager currently mounts, so an
 * external icon set is honoured by the first bake rather than after the next reload.
 */
//? if >=1.21.1 {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class SharedInventoryModelPack extends AbstractPackResources {
    /** Pack id shared with the loader wiring and the item-override probe. */
    public static final String PACK_ID = "gt6ce:shared_inventory";

    /** Ores are replaced by OreBakedModel; their blockstate only needs a valid stone placeholder. */
    private static final ResourceLocation STONE_TEMPLATE = ResourceLocation.withDefaultNamespace("block/stone");

    /** Material sprites, relative to {@code assets/<namespace>/}. */
    private static final String[] MATERIAL_ICON_DIRECTORIES = {
            "textures/item/material_icons", "textures/block/material_icons"};
    private Map<ResourceLocation, byte[]> resources;
    private Set<String> namespaces;

    /** Internal model references remain loaded without appearing as a user-selectable pack. */
    @Override
    public boolean isHidden() {
        return true;
    }

//? if >=1.21.1 {
    /*public SharedInventoryModelPack(PackLocationInfo info) { super(info); }
*///?} else {
    public SharedInventoryModelPack(String id) { super(id, true); }
//?}

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        event.addRepositorySource(output -> {
            Pack pack = ClientPackFacade.createSharedInventoryPack();
            if (pack != null) output.accept(pack);
        });
    }

    public static void loaders(ModelEvent.RegisterGeometryLoaders event) {
        ClientPackFacade.registerGeometryLoader(event,
                ResourceLocation.fromNamespaceAndPath(gregtech.engine.mod.GregTechIdentity.MOD_ID, "shared_inventory"),
                (json, context) -> new Reference(ResourceLocation.parse(json.get("parent").getAsString())));
    }

    /**
     * Nesting depth of {@link #data()} on the current thread so pack reads during resource building
     * short-circuit instead of re-entering {@link #buildResources()}.
     */
    private static final ThreadLocal<Integer> BUILD_DEPTH = ThreadLocal.withInitial(() -> 0);

    static boolean isBuilding() {
        return BUILD_DEPTH.get() > 0;
    }

    static boolean isServedAssetPath(String path) {
        return path.startsWith("models/") || path.startsWith("blockstates/");
    }

    static boolean servesAssetDirectory(String directory) {
        return directory.isEmpty()
                || "models".equals(directory)
                || directory.startsWith("models/")
                || "blockstates".equals(directory)
                || directory.startsWith("blockstates/");
    }

    private synchronized Map<ResourceLocation, byte[]> data() {
        if (resources != null) return resources;
        BUILD_DEPTH.set(BUILD_DEPTH.get() + 1);
        try {
            resources = buildResources();
        } finally {
            int depth = BUILD_DEPTH.get() - 1;
            // Drop the entry rather than store a zero, so a finished build leaves nothing behind.
            if (depth == 0) BUILD_DEPTH.remove();
            else BUILD_DEPTH.set(depth);
        }
        return resources;
    }

    private Map<ResourceLocation, byte[]> buildResources() {
        Map<ResourceLocation, byte[]> result = new HashMap<>();
        Map<ResourceLocation, byte[]> references = new HashMap<>();
        // Many material items refer to the same template. Cache each alias/classpath lookup per parent.
        Map<ResourceLocation, Boolean> templates = new HashMap<>();
        emitStructureParts(result);
        emitCompressedTanks(result);
        emitAddonBlockPrefixes(result);
        // Shared material models live only in this pack; blockstates and item references below
        // resolve their templates from the same map. Material sprites come from the mounted
        // resource manager once the registry namespace is mounted, or from the classpath before the
        // first client reload.
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager manager = minecraft == null ? null : minecraft.getResourceManager();
        Set<String> mountedIcons = collectMountedMaterialIcons(manager);
        Predicate<String> materialExists =
                mountedIcons != null ? mountedIcons::contains : MaterialIconDefinitions::classpathResourceExists;
        int sharedModels = emitSharedMaterialModels(result, materialExists);
        int smelteryModels = emitSmelteryModels(result, materialExists);
        int metalBookshelves = emitMetalBookshelfModels(result, materialExists);
        for (var item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (item instanceof gregtech.engine.material.MaterialFormItem material && material.getPrefix().isAddon()) {
                result.put(itemModel(id), MaterialIconDefinitions.addonItemModel(material.getPrefix()).getBytes(StandardCharsets.UTF_8));
                continue;
            }
            if (id.getNamespace().equals(GregTechIdentity.REGISTRY_NAMESPACE) && !id.getPath().equals("bottle_crate")
                    && item instanceof BlockItem crateItem
                    && crateItem.getBlock() instanceof gregtech.content.block.inventory.BottleCrateBlock crate) {
                String model = "block/inventory/" + id.getPath();
                String blockJson = "{\"parent\":\"gt6ce:block/inventory/bottle_crate_tinted\",\"textures\":{\"wood\":\""
                        + crate.frameTexture(materialExists) + "\"}}";
                result.put(location("models/" + model + ".json"), blockJson.getBytes(StandardCharsets.UTF_8));
                result.put(location("blockstates/" + id.getPath() + ".json"),
                        ("{\"variants\":{\"\":{\"model\":\"gt6ce:" + model + "\"}}}").getBytes(StandardCharsets.UTF_8));
                result.put(location("models/item/" + id.getPath() + ".json"),
                        ("{\"parent\":\"gt6ce:" + model + "\"}").getBytes(StandardCharsets.UTF_8));
                continue;
            }
            boolean materialForm = item instanceof gregtech.engine.material.MaterialFormItem material
                    && !material.getPrefix().isAddon();
//? if >=1.21.1 {
            /*boolean fluidDisplay = item instanceof gregtech.platform.neoforge.fluid.FluidDisplayItem;
*///?} else {
            boolean fluidDisplay = item instanceof gregtech.content.item.FluidItem;
//?}
            if (!id.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE) && !materialForm && !fluidDisplay) continue;
            if (JsonResourceAliases.contains("/assets/" + id.getNamespace()
                    + "/models/item/" + id.getPath() + ".json")) continue;
            ResourceLocation parent = null;
            if (item instanceof gregtech.engine.material.MaterialFormItem material) {
                parent = MaterialIcons.sharedModelLocation(MaterialIcons.resolveTextureSet(material.getMaterial()), material.getPrefix());
//? if >=1.21.1 {
            /*} else if (item instanceof gregtech.platform.neoforge.fluid.FluidDisplayItem) {
*///?} else {
            } else if (item instanceof gregtech.content.item.FluidItem) {
//?}
                parent = location("item/fluid_item");
            } else if (item instanceof CreativeTabIconItem icon) {
                if (icon.materialPrefix() != null) {
                    parent = MaterialIcons.sharedModelLocation(MaterialTextureSet.NONE, icon.materialPrefix());
                } else if (icon.blockPrefix() != null) {
                    parent = BlockMaterialIcons.sharedModelLocation(MaterialTextureSet.NONE, icon.blockPrefix());
                }
            } else if (item instanceof BlockItem blockItem) {
                if (OrthogonalMachineFamilies.usesTable(blockItem.getBlock())) {
                    // The machine pass writes the real item model for these blocks.
                    continue;
                } else if (blockItem.getBlock() instanceof OreBlock) {
                    parent = STONE_TEMPLATE;
                } else if (blockItem.getBlock() instanceof MaterialBlockLike material) {
                    parent = material.prefix().isAddon()
                            ? BlockMaterialIcons.addonModel(material.prefix())
                            : BlockMaterialIcons.sharedModelLocation(MaterialIcons.resolveTextureSet(material.material()), material.prefix());
                }
            }
            // Do not invent a missing template or hide it behind a dummy model. Unsupported
            // references remain visible in the usual bakery warning and final model checks.
            if (parent == null) continue;
            if (!hasTemplate(result, templates, parent)) continue;
            byte[] json = references.computeIfAbsent(parent, key ->
                    MaterialIconDefinitions.sharedInventoryJson(key.toString()).getBytes(StandardCharsets.UTF_8));
            result.put(named(id.getNamespace(), "models/item/" + id.getPath() + ".json"), json);
        }
        // Registration gained material forms after the original static data generation.
        // Supply only absent states, using the same real template as the existing alias hook.
        int states = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof MaterialBlockLike material)) continue;
            if (material.prefix().isAddon()) continue;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) continue;
            String statePath = "blockstates/" + id.getPath() + ".json";
            if (JsonResourceAliases.contains("/assets/gt6ce/" + statePath) || result.containsKey(location(statePath))) continue;
            ResourceLocation parent = block instanceof OreBlock
                    ? STONE_TEMPLATE
                    : BlockMaterialIcons.sharedModelLocation(MaterialIcons.resolveTextureSet(material.material()), material.prefix());
            if (!hasTemplate(result, templates, parent)) continue;
            byte[] json = ("{\"variants\":{\"\":{\"model\":\"" + parent + "\"}}}").getBytes(StandardCharsets.UTF_8);
            result.put(location(statePath), json);
            states++;
        }
        // Keep aliases at the same low priority as the original mod assets. Each path
        // serves its original bytes, so a user pack can override aliases independently.
        JsonResourceAliases.resources().forEach((path, bytes) ->
                result.put(location(path.substring("/assets/gt6ce/".length())), bytes));
        // Spec models come last: a row is the generated replacement for the static file the
        // aliases above still carry, so the generated one has to win inside this pack.
        int specs = emitMachineModelSpecs(result);
        // Emitted after the aliases so a generated template can never shadow a transport placeholder.
        int beforePlaceholders = result.size();
        emitTransportPlaceholders(result);
        int placeholders = result.size() - beforePlaceholders;
        int machines = emitMachineBlockstatesAndItems(result);
        com.mojang.logging.LogUtils.getLogger()
                .info("[gregtech] Shared inventory resources: {} files, {} templates, {} shared material models,"
                        + " {} smeltery hull models, {} metal bookshelf models,"
                        + " {} missing static blockstates, {} machine model specs, {} machines,"
                        + " {} transport placeholders",
                        result.size(), references.size(), sharedModels, smelteryModels, metalBookshelves,
                        states, specs, machines, placeholders);
        return Collections.unmodifiableMap(result);
    }

    /**
     * One-line spec model per {@code machine_models.tsv} row. Any remaining blockstate, item or
     * parent reference to a migrated machine resolves here instead of a static JSON tree.
     */
    private static int emitMachineModelSpecs(Map<ResourceLocation, byte[]> result) {
        for (String modelPath : OrthogonalMachineModels.all().keySet()) {
            String json = "{\"loader\":\"" + gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE
                    + ":orthogonal_machine\",\"spec\":\"" + modelPath + "\"}";
            result.put(location("models/" + modelPath + ".json"), json.getBytes(StandardCharsets.UTF_8));
        }
        return OrthogonalMachineModels.all().size();
    }

    /**
     * Real blockstate and item model for every block the table draws. The blockstate carries the
     * rotation each state needs, so the vanilla bakery renders the world from the row itself; the
     * item model references the row the inventory shows. Both are written with {@code putIfAbsent}:
     * a resource pack that ships its own file keeps it, and this pack only fills the gaps.
     */
    private static int emitMachineBlockstatesAndItems(Map<ResourceLocation, byte[]> result) {
        int machines = 0;
        StringBuilder variants = new StringBuilder();
        for (Block block : OrthogonalMachineFamilies.blocks()) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null) continue;
            variants.setLength(0);
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                OrthogonalMachineFamilies.Variant variant = OrthogonalMachineFamilies.world(state);
                if (variant == null || !OrthogonalMachineFamilies.usesTable(variant.modelId())) continue;
                if (variants.length() > 0) variants.append(',');
                variants.append('"')
                        .append(BlockModelShaper.statePropertiesToString(state.getValues()))
                        .append("\":");
                appendVariant(variants, variant);
            }
            if (variants.length() == 0) continue;
            result.putIfAbsent(named(id.getNamespace(), "blockstates/" + id.getPath() + ".json"),
                    ("{\"variants\":{" + variants + "}}").getBytes(StandardCharsets.UTF_8));
            ResourceLocation itemRow = itemRow(block);
            if (itemRow != null) {
                result.putIfAbsent(named(id.getNamespace(), "models/item/" + id.getPath() + ".json"),
                        MaterialIconDefinitions.sharedInventoryJson(itemRow.toString())
                                .getBytes(StandardCharsets.UTF_8));
            }
            machines++;
        }
        return machines;
    }

    /** One blockstate variant: the table row plus the rotation the static blockstate used to carry. */
    private static void appendVariant(StringBuilder json, OrthogonalMachineFamilies.Variant variant) {
        json.append("{\"model\":\"").append(variant.modelId()).append('"');
        if (variant.xRot() != 0) json.append(",\"x\":").append(variant.xRot());
        if (variant.yRot() != 0) json.append(",\"y\":").append(variant.yRot());
        if (variant.uvLock()) json.append(",\"uvlock\":true");
        json.append('}');
    }

    /** The first inventory row of the block that the table serves; null when it has none. */
    private static ResourceLocation itemRow(Block block) {
        for (OrthogonalMachineFamilies.Variant variant : OrthogonalMachineFamilies.itemVariants(block)) {
            if (OrthogonalMachineFamilies.usesTable(variant.modelId())) return variant.modelId();
        }
        return null;
    }

    /**
     * Shared {@code models/{item,block}/material/...} JSON, generated from the icon inheritance
     * chain instead of shipped as one file per texture set and prefix.
     */
    private static int emitSharedMaterialModels(Map<ResourceLocation, byte[]> result,
                                                Predicate<String> materialExists) {
        int emitted = 0;
        Map<String, byte[]> encoded = new HashMap<>();
        for (MaterialSharedModelIndex.ItemModel model : MaterialSharedModelIndex.items(materialExists)) {
            String json = MaterialIconDefinitions.sharedModelJson(
                    model.set(), model.prefix(), materialExists);
            if (json == null) continue;
            putModel(result, encoded, MaterialIconDefinitions.modelPath(model.set(), model.prefix()), json);
            emitted++;
        }
        for (MaterialSharedModelIndex.BlockModel model : MaterialSharedModelIndex.blocks(materialExists)) {
            String json = BlockMaterialIcons.sharedModelJson(
                    model.set(), model.prefix(), materialExists);
            if (json == null) continue;
            ResourceLocation id = BlockMaterialIcons.sharedModelLocation(model.set(), model.prefix());
            putModel(result, encoded, id.getPath(), json);
            emitted++;
        }
        return emitted;
    }

    /**
     * One model under {@code models/<path>.json}. Sets that resolve to the same sprite ship
     * byte-identical JSON, so the payloads are shared rather than copied per set.
     */
    private static void putModel(Map<ResourceLocation, byte[]> result, Map<String, byte[]> encoded,
                                 String path, String json) {
        result.put(location("models/" + path + ".json"),
                encoded.computeIfAbsent(json, value -> value.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * The five smeltery hull families, generated per modeled texture set: bowl, basin, mold,
     * crossing and the four faucet facings. Their static JSON tree is gone, so these models are the
     * only place the hull geometry lives.
     */
    private static int emitSmelteryModels(Map<ResourceLocation, byte[]> result,
                                          Predicate<String> materialExists) {
        int before = result.size();
        Map<String, byte[]> encoded = new HashMap<>();
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            putModel(result, encoded, CrucibleBowlIcons.sharedModelLocation(set).getPath(),
                    CrucibleBowlIcons.sharedModelJson(set, materialExists));
            putModel(result, encoded, MoldBasinIcons.sharedModelLocation(set).getPath(),
                    MoldBasinIcons.sharedModelJson(set, materialExists));
            putModel(result, encoded, CrucibleMoldIcons.sharedModelLocation(set).getPath(),
                    CrucibleMoldIcons.sharedModelJson(set, materialExists));
            putModel(result, encoded, CrucibleCrossingIcons.sharedModelLocation(set).getPath(),
                    CrucibleCrossingIcons.sharedModelJson(set, materialExists));
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                putModel(result, encoded, CrucibleFaucetIcons.sharedModelLocation(set, facing).getPath(),
                        CrucibleFaucetIcons.sharedModelJson(set, facing, materialExists));
            }
        }
        return result.size() - before;
    }

    /**
     * Metal shelves point the shared shelf geometry at the casing sprite their own material
     * resolves to, which replaces the per-material model files with one static template.
     */
    private static int emitMetalBookshelfModels(Map<ResourceLocation, byte[]> result,
                                                Predicate<String> materialExists) {
        int before = result.size();
        String casing = BlockMaterialPrefix.casingMachine.getTextureFileName();
        for (BookShelfVariants.Variant variant : BookShelfVariants.all()) {
            if (!variant.metal()) continue;
            MaterialTextureSet owner = MaterialIconDefinitions.resolveBlockTextureOwner(
                    MaterialIcons.resolveTextureSet(variant.material()), casing, materialExists);
            String sprite = "gt6ce:"
                    + MaterialIconDefinitions.blockIconPath(owner == null ? MaterialTextureSet.DULL : owner, casing);
            String json = "{\"parent\":\"gt6ce:block/bookshelf_metal_tinted\",\"textures\":{\"wood\":\""
                    + sprite + "\"}}";
            result.put(location("models/block/" + variant.path() + ".json"),
                    json.getBytes(StandardCharsets.UTF_8));
        }
        return result.size() - before;
    }

    /**
     * Material sprites visible through {@code manager} for the registry namespace, as paths relative
     * to {@code assets/<namespace>/}, or {@code null} before that namespace is mounted. Only the
     * resource keys are collected so no stream is opened while this pack builds.
     */
    @Nullable
    static Set<String> collectMountedMaterialIcons(@Nullable ResourceManager manager) {
        if (manager == null || !MaterialIconDefinitions.isMaterialNamespaceMounted(manager.getNamespaces())) {
            return null;
        }
        Set<String> icons = new HashSet<>();
        for (String directory : MATERIAL_ICON_DIRECTORIES) {
            manager.listResources(
                            directory,
                            id -> GregTechIdentity.REGISTRY_NAMESPACE.equals(id.getNamespace())
                                    && id.getPath().endsWith(".png"))
                    .keySet()
                    .forEach(id -> icons.add(id.getPath()));
        }
        return icons;
    }

    /** A model reference resolves when this pack serves it, as an alias, or as vanilla stone. */
    private static boolean hasTemplate(
            Map<ResourceLocation, byte[]> result, Map<ResourceLocation, Boolean> aliases, ResourceLocation parent) {
        if (parent.equals(STONE_TEMPLATE)) return true;
        if (result.containsKey(named(parent.getNamespace(), "models/" + parent.getPath() + ".json"))) return true;
        return aliases.computeIfAbsent(
                parent,
                key -> JsonResourceAliases.contains(
                        "/assets/" + key.getNamespace() + "/models/" + key.getPath() + ".json"));
    }

    private static void emitStructureParts(Map<ResourceLocation, byte[]> result) {
        for (var part : gregtech.content.registry.GTMultiblocks.addonParts()) {
            ResourceLocation id = part.block().getId();
            String texture = part.texture() == null ? "" : part.texture();
            boolean energy = part.block().get() instanceof gregtech.content.block.machine.AddonEnergyPartBlock;
            boolean functional = part.block().get() instanceof gregtech.content.block.machine.AddonFunctionalPartBlock;
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
            if (!(block instanceof gregtech.content.block.machine.CompressedGasTankBlock)) continue;
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

    /**
     * Transport blocks, wires, cables and axles are baked into generated geometry, so their static
     * blockstate and inventory JSON only has to hand the bakery a loaded model. The stone template is
     * already registered as a shared model and needs no per-block file on disk.
     */
    private static void emitTransportPlaceholders(Map<ResourceLocation, byte[]> result) {
        byte[] blockstate = ("{\"variants\":{\"\":{\"model\":\"" + STONE_TEMPLATE + "\"}}}")
                .getBytes(StandardCharsets.UTF_8);
        byte[] item = ("{\"parent\":\"" + STONE_TEMPLATE + "\"}").getBytes(StandardCharsets.UTF_8);
        for (var entry : GTFluidPipes.all()) {
            if (entry.isPresent()) emitPlaceholder(result, entry.get(), blockstate, item);
        }
        for (var block : AddonPipeRegistration.fluidBlocks()) emitPlaceholder(result, block, blockstate, item);
        for (var entry : GTItemPipes.all()) {
            if (entry.isPresent()) emitPlaceholder(result, entry.get(), blockstate, item);
        }
        for (var block : AddonPipeRegistration.itemBlocks()) emitPlaceholder(result, block, blockstate, item);
        for (var entry : GTWires.all()) {
            if (entry.isPresent()) emitPlaceholder(result, entry.get(), blockstate, item);
        }
        for (var entry : GTSignalWires.all()) {
            if (entry.isPresent()) emitPlaceholder(result, entry.get(), blockstate, item);
        }
        for (var entry : GTAxles.all()) {
            if (entry.isPresent()) emitPlaceholder(result, entry.get(), blockstate, item);
        }
    }

    /**
     * Written unconditionally: the pack is registered at the bottom position, so an installed
     * resource pack still wins, and the bakery replaces both entries before anything renders.
     */
    private static void emitPlaceholder(Map<ResourceLocation, byte[]> result, Block block,
                                        byte[] blockstate, byte[] item) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        result.put(named(id.getNamespace(), "blockstates/" + id.getPath() + ".json"), blockstate);
        result.put(named(id.getNamespace(), "models/item/" + id.getPath() + ".json"), item);
    }

    private static ResourceLocation location(String path) { return ResourceLocation.fromNamespaceAndPath("gt6ce", path); }
    private static ResourceLocation itemModel(ResourceLocation item) {
        return ResourceLocation.fromNamespaceAndPath(item.getNamespace(), "models/item/" + item.getPath() + ".json");
    }
    private static ResourceLocation named(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

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
            if (id == null || !gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE.equals(id.getNamespace())) continue;
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
        if (type != PackType.CLIENT_RESOURCES || !isServedAssetPath(id.getPath()) || isBuilding()) return null;
        byte[] json = data().get(id);
        return json == null ? null : () -> new ByteArrayInputStream(json);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !servesAssetDirectory(path) || isBuilding()) return;
        String prefix = path.isEmpty() ? "" : path + "/";
        data().forEach((id, json) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix))
                output.accept(id, () -> new ByteArrayInputStream(json));
        });
    }

    @Override
    public synchronized Set<String> getNamespaces(PackType type) {
        if (type != PackType.CLIENT_RESOURCES) return Set.of();
        if (namespaces != null) return namespaces;
        var names = new HashSet<String>();
        names.add("gt6ce");
        for (var prefix : gregtech.engine.prefix.PrefixRegistry.addonPrefixes())
            names.add(prefix.itemNamespace());
        for (var part : gregtech.content.registry.GTMultiblocks.addonParts())
            names.add(part.block().getId().getNamespace());
        for (var block : BuiltInRegistries.BLOCK)
            if (block instanceof gregtech.content.block.machine.CompressedGasTankBlock)
                names.add(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
        names.addAll(gregtech.content.registry.AddonPipeRegistration.namespaces());
        for (var material : gregtech.engine.material.GTMaterialRegistry.allMaterials())
            names.addAll(gregtech.engine.addon.AddonMaterials.owners(material));
        namespaces = Set.copyOf(names);
        return namespaces;
    }

    @Override
    public synchronized void close() { resources = null; namespaces = null; }

    record Reference(ResourceLocation parent) implements IUnbakedGeometry<Reference> {
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
            return BlockItemModelHelper.applyContext(
                    baker.bake(parent, state),
                    context.getTransforms(),
                    overrides,
                    context.useAmbientOcclusion(),
                    context.useBlockLight());
        }
    }
}
