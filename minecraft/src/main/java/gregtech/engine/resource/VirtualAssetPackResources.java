package gregtech.engine.resource;

import gregtech.engine.mod.GregTechIdentity;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
//? if >=1.21.1 {
/*import net.minecraft.server.packs.PackLocationInfo;
*///?}
import org.jetbrains.annotations.Nullable;

/**
 * Client-free resource routing shared by every pack that serves GregTech's virtual assets.
 *
 * <p>The class owns the single copy of the {@code models/}, {@code blockstates/} and
 * {@code stack_groups/} lookups, the {@code pack.mcmeta} payload and the directory enumeration
 * rules, so the client pack and a catalog-only pack answer resource-manager queries identically. No
 * client-only type is referenced, which keeps the class loadable on a dedicated server and in
 * {@code runGameTestServer}.
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
        return new CatalogOnlyPack(info);
    }
*///?} else {
    public static VirtualAssetPackResources catalogOnly(String id) {
        return new CatalogOnlyPack(id);
    }
//?}

    /** Serves the virtual catalogs alone; the model tree and the sprite hooks stay empty. */
    private static final class CatalogOnlyPack extends VirtualAssetPackResources {
        private Map<ResourceLocation, byte[]> catalogs;

//? if >=1.21.1 {
        /*private CatalogOnlyPack(PackLocationInfo info) {
            super(info);
        }
*///?} else {
        private CatalogOnlyPack(String id) {
            super(id);
        }
//?}

        @Override
        protected synchronized Map<ResourceLocation, byte[]> modelResources() {
            if (catalogs != null) return catalogs;
            Map<ResourceLocation, byte[]> built = new HashMap<>();
            emitVirtualModels(built);
            emitVirtualBlockstates(built);
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
