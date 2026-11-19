package gregtech.gametest;

import com.google.gson.JsonParser;
import gregtech.api.material.MaterialTextureSet;
import gregtech.client.CrucibleBowlIcons;
import gregtech.client.CrucibleCrossingIcons;
import gregtech.client.CrucibleFaucetIcons;
import gregtech.client.CrucibleMoldIcons;
import gregtech.client.LegacyPartDesignModels;
import gregtech.client.MoldBasinIcons;
import gregtech.content.block.inventory.BottleCrateBlock;
import gregtech.content.book.BookShelfVariants;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.MaterialIconDefinitions;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.resource.StackGroupCatalog;
import gregtech.engine.resource.VirtualBlockstateCatalog;
import gregtech.engine.resource.VirtualModelCatalog;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceFilterSection;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
/*import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}
import org.jetbrains.annotations.Nullable;

/**
 * Verifies resource-manager mounting, pack overrides, and {@code pack.mcmeta} {@code filter}
 * handling for material sprites and bottle-crate frames on the dedicated server.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ResourceFilterPackTests {
    private static final String BATCH = "resource_filter_pack";
    private static final byte[] DUMMY_PNG = new byte[] {1};

    private ResourceFilterPackTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void resourceFilterBlocksLowerAndBuiltinMaterialSprites(GameTestHelper helper)
            throws IOException {
        PackResources lowPack = memoryPack(
                "test_low_sprites",
                null,
                Map.of(
                        "textures/item/material_icons/metallic/ingot.png", DUMMY_PNG,
                        "textures/item/material_icons/metallic/ingot_overlay.png", DUMMY_PNG,
                        "textures/item/material_icons/magnetic/ingot.png", DUMMY_PNG,
                        "textures/item/material_icons/magnetic/ingot_overlay.png", DUMMY_PNG));
        PackResources highFilterPack = memoryPack(
                "test_high_filter",
                filterMcmeta(
                        "^textures/item/material_icons/magnetic/.*$",
                        "^textures/item/material_icons/metallic/ingot_overlay\\.png$"),
                Map.of());
        helper.assertTrue(
                highFilterPack.getMetadataSection(ResourceFilterSection.TYPE) != null,
                "filter metadata must parse into ResourceFilterSection");

        try (var manager = new MultiPackResourceManager(
                PackType.CLIENT_RESOURCES, List.of(lowPack, highFilterPack))) {
            helper.assertTrue(
                    MaterialIconDefinitions.isMaterialNamespaceMounted(manager.getNamespaces()),
                    "gt6ce namespace must be mounted");
            Set<String> icons = collectPngPaths(manager, "textures/item/material_icons");
            Predicate<String> probe =
                    MaterialIconDefinitions.selectMaterialProbe(manager.getNamespaces(), icons::contains);
            String json = MaterialIconDefinitions.sharedModelJson(
                    MaterialTextureSet.MAGNETIC, MaterialPrefix.ingot, probe);
            helper.assertTrue(json != null, "MAGNETIC ingot must resolve via METALLIC fallback");
            helper.assertTrue(
                    json.contains("\"gt6ce:item/material_icons/metallic/ingot\""),
                    "filtered MAGNETIC ingot must fall back to metallic/ingot, got: " + json);
            helper.assertTrue(
                    !json.contains("\"layer1\""),
                    "filtered overlay sprites must not emit layer1, got: " + json);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void bottleCrateUsesMountedPackOverrideInsteadOfClasspathOnly(GameTestHelper helper) {
        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, "bottle_crate_steel"));
        helper.assertTrue(block instanceof BottleCrateBlock, "bottle_crate_steel must be a BottleCrateBlock");
        BottleCrateBlock steelCrate = (BottleCrateBlock) block;

        PackResources basePack = memoryPack(
                "test_crate_base",
                null,
                Map.of("textures/block/material_icons/dull/casingmachine.png", DUMMY_PNG));
        PackResources overridePack = memoryPack(
                "test_crate_override",
                null,
                Map.of("textures/block/material_icons/metallic/casingmachine.png", DUMMY_PNG));
        PackResources filterPack = memoryPack(
                "test_crate_filter",
                filterMcmeta("^textures/block/material_icons/metallic/casingmachine\\.png$"),
                Map.of());

        try (var overriddenManager = new MultiPackResourceManager(
                PackType.CLIENT_RESOURCES, List.of(basePack, overridePack))) {
            Set<String> icons = collectPngPaths(overriddenManager, "textures/block/material_icons");
            Predicate<String> probe = MaterialIconDefinitions.selectMaterialProbe(
                    overriddenManager.getNamespaces(), icons::contains);
            String frame = steelCrate.frameTexture(probe);
            helper.assertTrue(
                    "gt6ce:block/material_icons/metallic/casingmachine".equals(frame),
                    "overridden steel bottle crate must use metallic/casingmachine, got: " + frame);
        }

        try (var filteredManager = new MultiPackResourceManager(
                PackType.CLIENT_RESOURCES, List.of(basePack, overridePack, filterPack))) {
            Set<String> icons = collectPngPaths(filteredManager, "textures/block/material_icons");
            Predicate<String> probe = MaterialIconDefinitions.selectMaterialProbe(
                    filteredManager.getNamespaces(), icons::contains);
            String frame = steelCrate.frameTexture(probe);
            helper.assertTrue(
                    "gt6ce:block/material_icons/dull/casingmachine".equals(frame),
                    "filtered steel bottle crate must fall back to dull/casingmachine, got: " + frame);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void smelteryAndMetalBookshelfHonorMountedPackOverrideAndFilter(GameTestHelper helper) {
        BookShelfVariants.Variant steelShelf = BookShelfVariants.byPath("bookshelf_metal_steel");
        helper.assertTrue(steelShelf != null && steelShelf.metal(), "bookshelf_metal_steel variant missing");
        MaterialTextureSet steelSet = MaterialIconDefinitions.resolveTextureSet(steelShelf.material());
        String casing = BlockMaterialPrefix.casingMachine.getTextureFileName();

        PackResources basePack = memoryPack(
                "test_smeltery_base",
                null,
                Map.of(
                        "textures/block/material_icons/dull/blocksolid.png", DUMMY_PNG,
                        "textures/block/material_icons/dull/casingmachine.png", DUMMY_PNG));
        PackResources overridePack = memoryPack(
                "test_smeltery_override",
                null,
                Map.of(
                        "textures/block/material_icons/rough/blocksolid.png", DUMMY_PNG,
                        "textures/block/material_icons/metallic/blocksolid.png", DUMMY_PNG,
                        "textures/block/material_icons/metallic/casingmachine.png", DUMMY_PNG));
        PackResources filterPack = memoryPack(
                "test_smeltery_filter",
                filterMcmeta(
                        "^textures/block/material_icons/rough/blocksolid\\.png$",
                        "^textures/block/material_icons/metallic/casingmachine\\.png$"),
                Map.of());

        try (var overriddenManager = new MultiPackResourceManager(
                PackType.CLIENT_RESOURCES, List.of(basePack, overridePack))) {
            Set<String> icons = collectPngPaths(overriddenManager, "textures/block/material_icons");
            Predicate<String> probe = MaterialIconDefinitions.selectMaterialProbe(
                    overriddenManager.getNamespaces(), icons::contains);
            String bowl = CrucibleBowlIcons.sharedModelJson(MaterialTextureSet.ROUGH, probe);
            String basin = MoldBasinIcons.sharedModelJson(MaterialTextureSet.ROUGH, probe);
            String mold = CrucibleMoldIcons.sharedModelJson(MaterialTextureSet.ROUGH, probe);
            String crossing = CrucibleCrossingIcons.sharedModelJson(MaterialTextureSet.ROUGH, probe);
            helper.assertTrue(JsonParser.parseString(bowl).isJsonObject(), "invalid bowl json");
            helper.assertTrue(JsonParser.parseString(basin).isJsonObject(), "invalid basin json");
            helper.assertTrue(JsonParser.parseString(mold).isJsonObject(), "invalid mold json");
            helper.assertTrue(JsonParser.parseString(crossing).isJsonObject(), "invalid crossing json");
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                String faucet = CrucibleFaucetIcons.sharedModelJson(MaterialTextureSet.METALLIC, facing, probe);
                helper.assertTrue(
                        JsonParser.parseString(faucet).isJsonObject(), "invalid faucet json for " + facing);
                helper.assertTrue(
                        faucet.contains("\"gt6ce:block/material_icons/metallic/blocksolid\""),
                        "overridden metallic faucet must resolve metallic/blocksolid, got: " + faucet);
            }
            MaterialTextureSet shelfOwner = MaterialIconDefinitions.resolveBlockTextureOwner(steelSet, casing, probe);
            helper.assertTrue(
                    bowl.contains("\"gt6ce:block/material_icons/rough/blocksolid\""),
                    "overridden rough crucible bowl must resolve rough/blocksolid, got: " + bowl);
            helper.assertTrue(
                    basin.contains("\"gt6ce:block/material_icons/rough/blocksolid\""),
                    "overridden rough mold basin must resolve rough/blocksolid, got: " + basin);
            helper.assertTrue(
                    shelfOwner == MaterialTextureSet.METALLIC,
                    "overridden bookshelf_metal_steel must resolve METALLIC, got: " + shelfOwner);
        }

        try (var filteredManager = new MultiPackResourceManager(
                PackType.CLIENT_RESOURCES, List.of(basePack, overridePack, filterPack))) {
            Set<String> icons = collectPngPaths(filteredManager, "textures/block/material_icons");
            Predicate<String> probe = MaterialIconDefinitions.selectMaterialProbe(
                    filteredManager.getNamespaces(), icons::contains);
            String bowl = CrucibleBowlIcons.sharedModelJson(MaterialTextureSet.ROUGH, probe);
            MaterialTextureSet shelfOwner = MaterialIconDefinitions.resolveBlockTextureOwner(steelSet, casing, probe);
            helper.assertTrue(
                    bowl.contains("\"gt6ce:block/material_icons/dull/blocksolid\""),
                    "filtered rough crucible bowl must fall back to dull/blocksolid, got: " + bowl);
            helper.assertTrue(
                    shelfOwner == MaterialTextureSet.DULL,
                    "filtered bookshelf_metal_steel must fall back to DULL, got: " + shelfOwner);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void legacyPartDesignUsesOnlyMountedFaces(GameTestHelper helper) {
        String prefix = "block/machines/multiblockparts/metalwall/4/";
        PackResources art = memoryPack(
                "test_legacy_design_art",
                null,
                Map.of(
                        "textures/" + prefix + "colored/side.png", DUMMY_PNG,
                        "textures/" + prefix + "colored/top.png", DUMMY_PNG,
                        "textures/" + prefix + "overlay/bottom.png", DUMMY_PNG));
        PackResources filter = memoryPack(
                "test_legacy_design_filter",
                filterMcmeta("^textures/" + prefix + "colored/top\\.png$"),
                Map.of());

        try (var manager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of(art, filter))) {
            helper.assertTrue(
                    LegacyPartDesignModels.createModel(manager, "metalwalldense") == null,
                    "The dense wall must not borrow the regular wall's design");
            String json = LegacyPartDesignModels.createModel(manager, "metalwall");
            helper.assertTrue(json != null, "Legacy PNGs must make design 4 available");
            var children = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("children");
            var colored = children.getAsJsonObject("colored").getAsJsonArray("elements")
                    .get(0).getAsJsonObject().getAsJsonObject("faces");
            helper.assertTrue(
                    colored.size() == 4 && colored.getAsJsonObject("north").get("tintindex").getAsInt() == 0,
                    "Only the four colored sides should be generated and they must be tinted");
            var overlay = children.getAsJsonObject("overlay").getAsJsonArray("elements")
                    .get(0).getAsJsonObject().getAsJsonObject("faces");
            helper.assertTrue(
                    overlay.size() == 1 && overlay.has("down") && !overlay.getAsJsonObject("down").has("tintindex"),
                    "The legacy bottom overlay must remain untinted");
            helper.assertTrue(
                    !json.contains("colored/top"), "A filtered sprite must not be referenced");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void virtualCatalogPackSupportsOverrideAndFilter(GameTestHelper helper) throws IOException {
        PackResources virtualPack = virtualCatalogPack();
        byte[] overrideModel = "{\"parent\":\"minecraft:block/stone\"}".getBytes(StandardCharsets.UTF_8);
        PackResources highPack = memoryPack(
                "test_virtual_override_filter",
                filterMcmeta(
                        "^stack_groups/material_forms/functional_drinks\\.json$",
                        "^models/item/compass_center\\.json$"),
                Map.of("models/item/cryo_distillation_main.json", overrideModel));

        try (var manager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of(virtualPack, highPack))) {
            ResourceLocation bsId = ResourceLocation.fromNamespaceAndPath(
                    GregTechIdentity.REGISTRY_NAMESPACE, "blockstates/stone_andesite_bricks.json");
            var bsRes = manager.getResource(bsId);
            helper.assertTrue(bsRes.isPresent(), "Virtual blockstate stone_andesite_bricks must resolve");
            try (InputStream in = bsRes.get().open()) {
                String bsJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                helper.assertTrue(JsonParser.parseString(bsJson).isJsonObject(), "Invalid virtual blockstate JSON");
            }

            ResourceLocation overrideId = ResourceLocation.fromNamespaceAndPath(
                    GregTechIdentity.REGISTRY_NAMESPACE, "models/item/cryo_distillation_main.json");
            var overrideRes = manager.getResource(overrideId);
            helper.assertTrue(overrideRes.isPresent(), "Overridden item/cryo_distillation_main must resolve");
            try (InputStream in = overrideRes.get().open()) {
                String actual = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                helper.assertTrue(
                        actual.contains("minecraft:block/stone"),
                        "Higher pack override must win over virtual catalog, got: " + actual);
            }

            ResourceLocation filteredModelId = ResourceLocation.fromNamespaceAndPath(
                    GregTechIdentity.REGISTRY_NAMESPACE, "models/item/compass_center.json");
            helper.assertTrue(
                    manager.getResource(filteredModelId).isEmpty(),
                    "Filter rule must block virtual models/item/compass_center.json");

            ResourceLocation blockedGroup = ResourceLocation.fromNamespaceAndPath(
                    GregTechIdentity.REGISTRY_NAMESPACE, "stack_groups/material_forms/functional_drinks.json");
            ResourceLocation visibleGroup = ResourceLocation.fromNamespaceAndPath(
                    GregTechIdentity.REGISTRY_NAMESPACE,
                    "stack_groups/material_forms/functional_chemical_bottles.json");
            helper.assertTrue(
                    manager.getResource(blockedGroup).isEmpty(),
                    "Filter rule must block virtual stack_groups functional_drinks");
            helper.assertTrue(
                    manager.getResource(visibleGroup).isPresent(),
                    "Sibling virtual stack_group functional_chemical_bottles must remain readable");
            try (InputStream in = visibleGroupJson(manager, visibleGroup)) {
                String groupJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                helper.assertTrue(JsonParser.parseString(groupJson).isJsonObject(), "Invalid virtual stack_group JSON");
            }

            var listedGroups = manager.listResources(
                    "stack_groups/material_forms",
                    id -> GregTechIdentity.REGISTRY_NAMESPACE.equals(id.getNamespace()));
            helper.assertTrue(
                    !listedGroups.containsKey(blockedGroup) && listedGroups.containsKey(visibleGroup),
                    "listResources must omit filtered stack groups and retain unfiltered ones");
        }
        helper.succeed();
    }

    private static InputStream visibleGroupJson(ResourceManager manager, ResourceLocation id) throws IOException {
        return manager.getResourceOrThrow(id).open();
    }

    private static PackResources virtualCatalogPack() {
//? if >=1.21.1 {
        /*return new VirtualCatalogPack(new PackLocationInfo(
                "test_virtual_catalog",
                Component.literal("test_virtual_catalog"),
                PackSource.BUILT_IN,
                Optional.empty()));
*///?} else {
        return new VirtualCatalogPack("test_virtual_catalog");
//?}
    }

    private static Set<String> collectPngPaths(ResourceManager manager, String directory) {
        Set<String> icons = new HashSet<>();
        manager.listResources(
                        directory,
                        id -> GregTechIdentity.REGISTRY_NAMESPACE.equals(id.getNamespace())
                                && id.getPath().endsWith(".png"))
                .keySet()
                .forEach(id -> icons.add(id.getPath()));
        return icons;
    }

    private static byte[] filterMcmeta(String... blockedPaths) {
        StringBuilder blocks = new StringBuilder();
        for (int i = 0; i < blockedPaths.length; i++) {
            if (i > 0) blocks.append(',');
            blocks.append("{\"namespace\":\"")
                    .append(GregTechIdentity.REGISTRY_NAMESPACE)
                    .append("\",\"path\":\"")
                    .append(blockedPaths[i].replace("\\", "\\\\"))
                    .append("\"}");
        }
        String json = "{\"pack\":{\"pack_format\":15,\"description\":\"filter test\"},"
                + "\"filter\":{\"block\":[" + blocks + "]}}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static PackResources memoryPack(
            String id, @Nullable byte[] packMcmeta, Map<String, byte[]> files) {
//? if >=1.21.1 {
        /*return new InMemoryPack(
                new PackLocationInfo(id, Component.literal(id), PackSource.BUILT_IN, Optional.empty()),
                packMcmeta,
                files);
*///?} else {
        return new InMemoryPack(id, packMcmeta, files);
//?}
    }

    private static final class InMemoryPack extends AbstractPackResources {
        @Nullable
        private final byte[] packMcmeta;
        private final Map<String, byte[]> files;

//? if >=1.21.1 {
        /*private InMemoryPack(PackLocationInfo info, @Nullable byte[] packMcmeta, Map<String, byte[]> files) {
            super(info);
            this.packMcmeta = packMcmeta;
            this.files = Map.copyOf(files);
        }
*///?} else {
        private InMemoryPack(String id, @Nullable byte[] packMcmeta, Map<String, byte[]> files) {
            super(id, true);
            this.packMcmeta = packMcmeta;
            this.files = Map.copyOf(files);
        }
//?}

        @Override
        @Nullable
        public IoSupplier<InputStream> getRootResource(String... paths) {
            if (paths.length == 1 && "pack.mcmeta".equals(paths[0]) && packMcmeta != null) {
                return () -> new ByteArrayInputStream(packMcmeta);
            }
            return null;
        }

        @Override
        @Nullable
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
            if (type != PackType.CLIENT_RESOURCES
                    || !GregTechIdentity.REGISTRY_NAMESPACE.equals(id.getNamespace())) {
                return null;
            }
            byte[] bytes = files.get(id.getPath());
            return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
        }

        @Override
        public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
            if (type != PackType.CLIENT_RESOURCES
                    || !GregTechIdentity.REGISTRY_NAMESPACE.equals(namespace)) {
                return;
            }
            String prefix = path.isEmpty() ? "" : path + "/";
            files.forEach((rel, bytes) -> {
                if (rel.startsWith(prefix)) {
                    output.accept(
                            ResourceLocation.fromNamespaceAndPath(namespace, rel),
                            () -> new ByteArrayInputStream(bytes));
                }
            });
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            if (type == PackType.CLIENT_RESOURCES && !files.isEmpty()) {
                return Set.of(GregTechIdentity.REGISTRY_NAMESPACE);
            }
            return Set.of();
        }

        @Override
        public void close() {}
    }

    /** Dedicated-server-safe virtual pack mirror wrapping core's client-free resource catalogs. */
    private static final class VirtualCatalogPack extends AbstractPackResources {
        private static final String MODELS_PREFIX = "models/";
        private static final String BLOCKSTATES_PREFIX = "blockstates/";
        private static final String STACK_GROUPS_PREFIX = "stack_groups/material_forms/";
        private static final String JSON_SUFFIX = ".json";
        private static final String COMPOSITE_LOADER =
//? if >=1.21.1 {
                /*"neoforge:composite";
*///?} else {
                "forge:composite";
//?}

//? if >=1.21.1 {
        /*private VirtualCatalogPack(PackLocationInfo info) {
            super(info);
        }
*///?} else {
        private VirtualCatalogPack(String id) {
            super(id, true);
        }
//?}

        @Override
        @Nullable
        public IoSupplier<InputStream> getRootResource(String... paths) {
            return null;
        }

        @Override
        @Nullable
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
            if (type != PackType.CLIENT_RESOURCES
                    || !GregTechIdentity.REGISTRY_NAMESPACE.equals(id.getNamespace())) {
                return null;
            }
            String rel = id.getPath();
            if (!rel.endsWith(JSON_SUFFIX)) {
                return null;
            }
            String stem = rel.substring(0, rel.length() - JSON_SUFFIX.length());
            if (stem.startsWith(MODELS_PREFIX)) {
                String json = VirtualModelCatalog.renderJson(
                        stem.substring(MODELS_PREFIX.length()), COMPOSITE_LOADER);
                return json == null ? null : utf8Supplier(json);
            }
            if (stem.startsWith(BLOCKSTATES_PREFIX)) {
                String json = VirtualBlockstateCatalog.renderJson(stem.substring(BLOCKSTATES_PREFIX.length()));
                return json == null ? null : utf8Supplier(json);
            }
            if (stem.startsWith(STACK_GROUPS_PREFIX)) {
                StackGroupCatalog.Entry entry = StackGroupCatalog.entry(stem.substring(STACK_GROUPS_PREFIX.length()));
                return entry == null ? null : utf8Supplier(StackGroupCatalog.renderJson(entry));
            }
            return null;
        }

        @Override
        public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
            if (type != PackType.CLIENT_RESOURCES
                    || !GregTechIdentity.REGISTRY_NAMESPACE.equals(namespace)) {
                return;
            }
            if (path.isEmpty() || "models".equals(path) || path.startsWith(MODELS_PREFIX)) {
                String prefix = path.isEmpty() ? "" : path + "/";
                VirtualModelCatalog.emitAll(COMPOSITE_LOADER, (modelPath, bytes) -> {
                    String rel = MODELS_PREFIX + modelPath + JSON_SUFFIX;
                    if (rel.startsWith(prefix)) {
                        output.accept(
                                ResourceLocation.fromNamespaceAndPath(namespace, rel),
                                () -> new ByteArrayInputStream(bytes));
                    }
                });
            }
            if (path.isEmpty() || "blockstates".equals(path) || path.startsWith(BLOCKSTATES_PREFIX)) {
                String prefix = path.isEmpty() ? "" : path + "/";
                VirtualBlockstateCatalog.emitAll((stem, bytes) -> {
                    String rel = BLOCKSTATES_PREFIX + stem + JSON_SUFFIX;
                    if (rel.startsWith(prefix)) {
                        output.accept(
                                ResourceLocation.fromNamespaceAndPath(namespace, rel),
                                () -> new ByteArrayInputStream(bytes));
                    }
                });
            }
            if (path.isEmpty()
                    || "stack_groups".equals(path)
                    || "stack_groups/material_forms".equals(path)
                    || path.startsWith(STACK_GROUPS_PREFIX)) {
                String prefix = path.isEmpty() ? "" : path + "/";
                StackGroupCatalog.buildAllResources().forEach((rel, bytes) -> {
                    if (rel.startsWith(prefix)) {
                        output.accept(
                                ResourceLocation.fromNamespaceAndPath(namespace, rel),
                                () -> new ByteArrayInputStream(bytes));
                    }
                });
            }
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            return type == PackType.CLIENT_RESOURCES
                    ? Set.of(GregTechIdentity.REGISTRY_NAMESPACE)
                    : Set.of();
        }

        @Override
        public void close() {}

        private static IoSupplier<InputStream> utf8Supplier(String json) {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            return () -> new ByteArrayInputStream(bytes);
        }
    }
}
