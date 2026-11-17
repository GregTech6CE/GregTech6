package gregtech.gametest;

import com.google.gson.JsonParser;
import gregtech.api.material.MaterialTextureSet;
import gregtech.client.CrucibleBowlIcons;
import gregtech.client.CrucibleCrossingIcons;
import gregtech.client.CrucibleFaucetIcons;
import gregtech.client.CrucibleMoldIcons;
import gregtech.client.MoldBasinIcons;
import gregtech.content.block.inventory.BottleCrateBlock;
import gregtech.content.book.BookShelfVariants;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.MaterialIconDefinitions;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.prefix.BlockMaterialPrefix;
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
}
