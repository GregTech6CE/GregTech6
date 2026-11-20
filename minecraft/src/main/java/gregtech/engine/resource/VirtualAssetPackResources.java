package gregtech.engine.resource;

import gregtech.content.registry.GTHoppers;
import gregtech.content.registry.GTSmeltery;
import gregtech.content.registry.RegisteredBlock;
import gregtech.engine.mod.GregTechIdentity;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
/*import net.minecraft.server.packs.PackLocationInfo;
*///?}
import org.jetbrains.annotations.Nullable;

/**
 * Client-free resource routing shared by every pack that serves GregTech's virtual assets.
 *
 * <p>The class owns the single copy of the {@code models/}, {@code blockstates/} and
 * {@code stack_groups/} lookups, the placeholder payloads of the blocks drawn by generated geometry,
 * the {@code pack.mcmeta} payload and the directory enumeration rules, so the client pack and a
 * catalog-only pack answer resource-manager queries identically. No client-only type is referenced,
 * which keeps the class loadable on a dedicated server and in {@code runGameTestServer}.
 *
 * <p>Subclasses supply the model map and the client-only sprite hooks; {@link #isBuilding()} lets a
 * subclass short-circuit reads while it builds that map.
 */
public abstract class VirtualAssetPackResources extends AbstractPackResources {
    /** Loader id of the composite model geometry that virtual models reference. */
    public static final String COMPOSITE_LOADER =
//? if >=1.21.1 {
            /*"neoforge:composite";
*///?} else {
            "forge:composite";
//?}

    /** Resource-pack format the active version's pack loader accepts. */
//? if >=1.21.1 {
    /*private static final int PACK_FORMAT = 34;
*///?} else {
    private static final int PACK_FORMAT = 15;
//?}

    /** Facing order of the six-way hopper blockstates. */
    private static final String[] SIX_WAY_FACINGS = {"down", "up", "north", "south", "west", "east"};

    /** Stone blockstate handed to the bakery for a block drawn by generated geometry. */
    public static final byte[] STONE_BLOCKSTATE =
            encode("{\"variants\":{\"\":{\"model\":\"minecraft:block/stone\"}}}");

    /** Stone inventory parent paired with {@link #STONE_BLOCKSTATE}. */
    public static final byte[] STONE_ITEM_MODEL = encode("{\"parent\":\"minecraft:block/stone\"}");

    /** Iron cube model that gives every generated block a resolvable {@code models/block/blocks/} parent. */
    public static final byte[] IRON_BLOCK_MODEL = encode(
            "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"minecraft:block/iron_block\"}}");

    /** Six-way blockstate of the plain hoppers, keyed by {@code facing}. */
    public static final byte[] HOPPER_SIX_WAY_BLOCKSTATE = sixWayBlockstate("hopper");

    /** Six-way blockstate of the queue hoppers, keyed by {@code facing}. */
    public static final byte[] QUEUE_HOPPER_SIX_WAY_BLOCKSTATE = sixWayBlockstate("queuehopper");

    /** Inventory parent of the plain hoppers. */
    public static final byte[] HOPPER_ITEM_MODEL =
            encode("{\"parent\":\"" + GregTechIdentity.REGISTRY_NAMESPACE + ":block/machine/hopper\"}");

    /** Inventory parent of the queue hoppers; the nozzle points north in the inventory. */
    public static final byte[] QUEUE_HOPPER_ITEM_MODEL = encode(
            "{\"parent\":\"" + GregTechIdentity.REGISTRY_NAMESPACE + ":block/machine/queuehopper_north\"}");

    /** Hopper ids that keep a six-way blockstate and their own inventory parent. */
    private static final Map<String, SixWayHopper> SIX_WAY_HOPPERS = Map.of(
            "hopper_bronze", new SixWayHopper(HOPPER_SIX_WAY_BLOCKSTATE, HOPPER_ITEM_MODEL),
            "hopper_steel", new SixWayHopper(HOPPER_SIX_WAY_BLOCKSTATE, HOPPER_ITEM_MODEL),
            "hopper_tungsten", new SixWayHopper(HOPPER_SIX_WAY_BLOCKSTATE, HOPPER_ITEM_MODEL),
            "queue_hopper_bronze", new SixWayHopper(QUEUE_HOPPER_SIX_WAY_BLOCKSTATE, QUEUE_HOPPER_ITEM_MODEL),
            "queue_hopper_steel", new SixWayHopper(QUEUE_HOPPER_SIX_WAY_BLOCKSTATE, QUEUE_HOPPER_ITEM_MODEL),
            "queue_hopper_tungsten", new SixWayHopper(QUEUE_HOPPER_SIX_WAY_BLOCKSTATE, QUEUE_HOPPER_ITEM_MODEL));

    /** Blockstate and inventory JSON a six-way hopper keeps instead of the stone placeholder. */
    private record SixWayHopper(byte[] blockstate, byte[] itemModel) {}

    /** JSON payload of {@code json}; every resource serving the same document shares one array. */
    private static byte[] encode(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    /** Six-way {@code facing} blockstate pointing at {@code gt6ce:block/machine/<prefix>_<facing>}. */
    private static byte[] sixWayBlockstate(String modelPrefix) {
        StringBuilder json = new StringBuilder("{\"variants\":{");
        for (int i = 0; i < SIX_WAY_FACINGS.length; i++) {
            if (i > 0) json.append(',');
            json.append("\"facing=")
                    .append(SIX_WAY_FACINGS[i])
                    .append("\":{\"model\":\"")
                    .append(GregTechIdentity.REGISTRY_NAMESPACE)
                    .append(":block/machine/")
                    .append(modelPrefix)
                    .append('_')
                    .append(SIX_WAY_FACINGS[i])
                    .append("\"}");
        }
        return encode(json.append("}}").toString());
    }

    private Map<ResourceLocation, byte[]> stackGroups;

//? if >=1.21.1 {
    /*protected VirtualAssetPackResources(PackLocationInfo info) {
        super(info);
    }
*///?} else {
    protected VirtualAssetPackResources(String id) {
        super(id, true);
    }
//?}

    /** Internal model references remain loaded without appearing as a user-selectable pack. */
    @Override
    public boolean isHidden() {
        return true;
    }

    /** Resource location in the GregTech asset namespace, relative to {@code assets/<namespace>/}. */
    protected static ResourceLocation gtLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(GregTechIdentity.MOD_ID, path);
    }

    /** Directories whose contents this pack enumerates on request. */
    protected static boolean servesAssetDirectory(String directory) {
        return directory.isEmpty()
                || "models".equals(directory)
                || directory.startsWith("models/")
                || "blockstates".equals(directory)
                || directory.startsWith("blockstates/")
                || "stack_groups".equals(directory)
                || directory.startsWith("stack_groups/")
                || "textures".equals(directory)
                || directory.startsWith("textures/");
    }

    /**
     * Creative-tab stack definitions, built on first use. Kept apart from
     * {@link #modelResources()} so a tab listing never materializes the model tree and a model
     * request never rebuilds the definitions.
     */
    private synchronized Map<ResourceLocation, byte[]> stackGroups() {
        if (stackGroups != null) return stackGroups;
        Map<ResourceLocation, byte[]> groups = new HashMap<>();
        StackGroupCatalog.buildAllResources().forEach((path, bytes) -> groups.put(gtLocation(path), bytes));
        stackGroups = Collections.unmodifiableMap(groups);
        return stackGroups;
    }

    /**
     * Copies every virtual model into {@code result} under {@code models/}, returning how many files
     * it contributed. Catalog paths are relative to that directory and carry no extension.
     */
    protected static int emitVirtualModels(Map<ResourceLocation, byte[]> result) {
        Map<ResourceLocation, byte[]> files = new HashMap<>();
        VirtualModelCatalog.emitAll(
                COMPOSITE_LOADER, (path, bytes) -> files.put(gtLocation("models/" + path + ".json"), bytes));
        result.putAll(files);
        return files.size();
    }

    /**
     * Copies every virtual blockstate into {@code result} under {@code blockstates/}, returning how
     * many files it contributed. Catalog stems carry no extension.
     */
    protected static int emitVirtualBlockstates(Map<ResourceLocation, byte[]> result) {
        Map<ResourceLocation, byte[]> files = new HashMap<>();
        VirtualBlockstateCatalog.emitAll(
                (stem, bytes) -> files.put(gtLocation("blockstates/" + stem + ".json"), bytes));
        result.putAll(files);
        return files.size();
    }

    /**
     * Emits the stone blockstate and inventory placeholder for every present entry of
     * {@code entries}, returning how many were written.
     */
    public static <T extends Block> int emitStonePlaceholders(
            Map<ResourceLocation, byte[]> result, List<RegisteredBlock<T>> entries) {
        int emitted = 0;
        for (RegisteredBlock<T> entry : entries) {
            if (!entry.isPresent()) continue;
            emitStonePlaceholder(result, entry.getId());
            emitted++;
        }
        return emitted;
    }

    /**
     * Emits the stone blockstate and inventory placeholder for every block of {@code blocks},
     * returning how many were written.
     */
    public static int emitStonePlaceholders(Map<ResourceLocation, byte[]> result, Collection<? extends Block> blocks) {
        for (Block block : blocks) emitStonePlaceholder(result, BuiltInRegistries.BLOCK.getKey(block));
        return blocks.size();
    }

    /**
     * Smeltery hulls, molds and hoppers are drawn from the shared hull models and generated geometry,
     * so their static blockstate and inventory JSON only has to hand the bakery a loaded model. Every
     * smeltery hull and every plain hopper also gets a {@code models/block/blocks/<id>.json} parent that
     * its inventory model inherits from, so an addon pack can build on a stable path; the six-way
     * hoppers keep their directional blockstate and their own inventory parent instead.
     *
     * @return how many blocks were written
     */
    public static int emitSmelteryAndHopperPlaceholders(Map<ResourceLocation, byte[]> result) {
        int emitted = emitHullPlaceholders(result, GTSmeltery.crucibles());
        emitted += emitHullPlaceholders(result, GTSmeltery.basins());
        emitted += emitHullPlaceholders(result, GTSmeltery.molds());
        emitted += emitHullPlaceholders(result, GTSmeltery.crossings());
        emitted += emitHullPlaceholders(result, GTSmeltery.faucets());
        emitted += emitHopperPlaceholders(result, GTHoppers.hoppers());
        emitted += emitHopperPlaceholders(result, GTHoppers.queues());
        return emitted;
    }

    /** Generated-geometry placeholders whose inventory model inherits from the block's own model. */
    private static <T extends Block> int emitHullPlaceholders(
            Map<ResourceLocation, byte[]> result, List<RegisteredBlock<T>> entries) {
        int emitted = 0;
        for (RegisteredBlock<T> entry : entries) {
            if (!entry.isPresent()) continue;
            emitBlockParentPlaceholder(result, entry.getId());
            emitted++;
        }
        return emitted;
    }

    /** Six-way hoppers keep their directional blockstate; every other hopper gets a generated-geometry placeholder. */
    private static <T extends Block> int emitHopperPlaceholders(
            Map<ResourceLocation, byte[]> result, List<RegisteredBlock<T>> entries) {
        int emitted = 0;
        for (RegisteredBlock<T> entry : entries) {
            if (!entry.isPresent()) continue;
            ResourceLocation id = entry.getId();
            SixWayHopper sixWay = SIX_WAY_HOPPERS.get(id.getPath());
            if (sixWay == null) {
                emitBlockParentPlaceholder(result, id);
            } else {
                result.put(named(id.getNamespace(), "blockstates/" + id.getPath() + ".json"), sixWay.blockstate());
                result.put(named(id.getNamespace(), "models/item/" + id.getPath() + ".json"), sixWay.itemModel());
            }
            emitted++;
        }
        return emitted;
    }

    /**
     * Stone blockstate, an inventory model inheriting from the block's own
     * {@code models/block/blocks/<id>.json} parent, and the iron-block model that parent resolves to.
     */
    private static void emitBlockParentPlaceholder(Map<ResourceLocation, byte[]> result, ResourceLocation id) {
        result.put(named(id.getNamespace(), "blockstates/" + id.getPath() + ".json"), STONE_BLOCKSTATE);
        result.put(
                named(id.getNamespace(), "models/item/" + id.getPath() + ".json"),
                encode("{\"parent\":\"" + id.getNamespace() + ":block/blocks/" + id.getPath() + "\"}"));
        result.put(blockModelLocation(id), IRON_BLOCK_MODEL);
    }

    /**
     * Written unconditionally: the pack is registered at the bottom position, so an installed
     * resource pack still wins, and the bakery replaces both entries before anything renders.
     */
    private static void emitStonePlaceholder(Map<ResourceLocation, byte[]> result, ResourceLocation id) {
        result.put(named(id.getNamespace(), "blockstates/" + id.getPath() + ".json"), STONE_BLOCKSTATE);
        result.put(named(id.getNamespace(), "models/item/" + id.getPath() + ".json"), STONE_ITEM_MODEL);
    }

    /** The {@code models/block/blocks/<id>.json} parent an addon pack can reference. */
    private static ResourceLocation blockModelLocation(ResourceLocation id) {
        return named(id.getNamespace(), "models/block/blocks/" + id.getPath() + ".json");
    }

    private static ResourceLocation named(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    /** Model and blockstate JSON of this pack, keyed by full resource location. */
    protected abstract Map<ResourceLocation, byte[]> modelResources();

    /** Whether this pack is currently building {@link #modelResources()} on the calling thread. */
    protected boolean isBuilding() {
        return false;
    }

    /** Opens a palette-synthesized sprite or its {@code .mcmeta}; {@code null} when none matches. */
    protected @Nullable IoSupplier<InputStream> openDynamicTexture(String path) {
        return null;
    }

    /** Lists the palette-synthesized sprites below {@code path}, which is {@code textures*} or empty. */
    protected void listDynamicTextures(String path, ResourceOutput output) {}

    /** Description written into {@code pack.mcmeta}. */
    protected String packDescription() {
        return "GregTech shared inventory models";
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
        byte[] meta = ("{\"pack\":{\"pack_format\":" + PACK_FORMAT + ",\"description\":"
                + JsonEscaper.quote(packDescription()) + "}}").getBytes(StandardCharsets.UTF_8);
        return () -> new ByteArrayInputStream(meta);
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type != PackType.CLIENT_RESOURCES || isBuilding()) return null;
        String path = id.getPath();
        if (GregTechIdentity.MOD_ID.equals(id.getNamespace())) {
            IoSupplier<InputStream> dynamic = openDynamicTexture(path);
            if (dynamic != null) return dynamic;
            if (path.startsWith("stack_groups/")) {
                byte[] group = stackGroups().get(id);
                return group == null ? null : () -> new ByteArrayInputStream(group);
            }
        }
        if (!path.startsWith("models/") && !path.startsWith("blockstates/")) return null;
        byte[] json = modelResources().get(id);
        return json == null ? null : () -> new ByteArrayInputStream(json);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !servesAssetDirectory(path) || isBuilding()) return;
        if ("textures".equals(path) || path.startsWith("textures/")) {
            if (GregTechIdentity.MOD_ID.equals(namespace)) listDynamicTextures(path, output);
            return;
        }
        if ("stack_groups".equals(path) || path.startsWith("stack_groups/")) {
            if (GregTechIdentity.MOD_ID.equals(namespace)) {
                String groupPrefix = path + "/";
                stackGroups().forEach((id, json) -> {
                    if (id.getPath().startsWith(groupPrefix))
                        output.accept(id, () -> new ByteArrayInputStream(json));
                });
            }
            return;
        }
        String prefix = path.isEmpty() ? "" : path + "/";
        modelResources().forEach((id, json) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix))
                output.accept(id, () -> new ByteArrayInputStream(json));
        });
        if (path.isEmpty() && GregTechIdentity.MOD_ID.equals(namespace)) {
            listDynamicTextures(path, output);
            stackGroups().forEach((id, json) -> output.accept(id, () -> new ByteArrayInputStream(json)));
        }
    }

    @Override
    public synchronized void close() {
        stackGroups = null;
    }

    /**
     * Catalog-only pack that serves the virtual model, blockstate and stack-group documents with no
     * client wiring, so a dedicated-server test mounts the same routing the client pack uses.
     */
//? if >=1.21.1 {
    /*public static VirtualAssetPackResources catalogOnly(PackLocationInfo info) {
        return new CatalogOnlyPack(info, false);
    }
*///?} else {
    public static VirtualAssetPackResources catalogOnly(String id) {
        return new CatalogOnlyPack(id, false);
    }
//?}

    /**
     * Catalog-only pack that also serves the smeltery and hopper placeholders, so a dedicated-server
     * test mounts the placeholder routing the client pack uses for those blocks.
     */
//? if >=1.21.1 {
    /*public static VirtualAssetPackResources catalogWithPlaceholders(PackLocationInfo info) {
        return new CatalogOnlyPack(info, true);
    }
*///?} else {
    public static VirtualAssetPackResources catalogWithPlaceholders(String id) {
        return new CatalogOnlyPack(id, true);
    }
//?}

    /**
     * Serves the virtual catalogs alone, plus the smeltery and hopper placeholders when asked; the
     * sprite hooks stay empty either way.
     */
    private static final class CatalogOnlyPack extends VirtualAssetPackResources {
        private final boolean includePlaceholders;
        private Map<ResourceLocation, byte[]> catalogs;

//? if >=1.21.1 {
        /*private CatalogOnlyPack(PackLocationInfo info, boolean includePlaceholders) {
            super(info);
            this.includePlaceholders = includePlaceholders;
        }
*///?} else {
        private CatalogOnlyPack(String id, boolean includePlaceholders) {
            super(id);
            this.includePlaceholders = includePlaceholders;
        }
//?}

        @Override
        protected synchronized Map<ResourceLocation, byte[]> modelResources() {
            if (catalogs != null) return catalogs;
            Map<ResourceLocation, byte[]> built = new HashMap<>();
            emitVirtualModels(built);
            emitVirtualBlockstates(built);
            if (includePlaceholders) emitSmelteryAndHopperPlaceholders(built);
            catalogs = Collections.unmodifiableMap(built);
            return catalogs;
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            return type == PackType.CLIENT_RESOURCES ? Set.of(GregTechIdentity.MOD_ID) : Set.of();
        }

        @Override
        public synchronized void close() {
            super.close();
            catalogs = null;
        }
    }
}
