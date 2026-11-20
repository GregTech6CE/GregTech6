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
import gregtech.content.registry.GTHoppers;
import gregtech.content.registry.GTSmeltery;
import gregtech.content.registry.RegisteredBlock;
import gregtech.engine.material.MaterialIconDefinitions;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.resource.VirtualAssetPackResources;
import gregtech.engine.resource.VirtualBlockstateCatalog;
import gregtech.engine.resource.VirtualModelCatalog;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import net.minecraft.server.packs.resources.Resource;
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
 * handling for material sprites, bottle-crate frames, the virtual asset catalogs and the smeltery
 * and hopper placeholders on the dedicated server.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ResourceFilterPackTests {
    private static final String BATCH = "resource_filter_pack";
    private static final String VIRTUAL_PACK_ID = "test_virtual_catalog";
    private static final String HIGH_PACK_ID = "test_virtual_override_filter";
    private static final String PLACEHOLDER_PACK_ID = "test_placeholder_catalog";
    private static final String PLACEHOLDER_CHILD_PACK_ID = "test_placeholder_child";
    private static final byte[] DUMMY_PNG = new byte[] {1};

    /** Stone placeholder documents the generated-geometry blocks resolve to. */
    private static final String STONE_BLOCKSTATE_JSON = "{\"variants\":{\"\":{\"model\":\"minecraft:block/stone\"}}}";

    private static final String STONE_ITEM_JSON = "{\"parent\":\"minecraft:block/stone\"}";
    private static final String IRON_BLOCK_MODEL_JSON =
            "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"minecraft:block/iron_block\"}}";

    /** Facing order of the six-way hopper blockstates. */
    private static final String[] SIX_WAY_FACINGS = {"down", "up", "north", "south", "west", "east"};

    /** Hoppers that keep a six-way blockstate and their own inventory parent. */
    private static final List<String> SIX_WAY_HOPPER_IDS = List.of(
            "hopper_bronze",
            "hopper_steel",
            "hopper_tungsten",
            "queue_hopper_bronze",
            "queue_hopper_steel",
            "queue_hopper_tungsten");

    private static final int SMELTERY_BLOCK_COUNT = 145;
    private static final int HOPPER_COUNT = 88;

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

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void virtualCatalogPackSupportsOverrideAndFilter(GameTestHelper helper) throws IOException {
        PackResources virtualPack = virtualCatalogPack();
        byte[] overrideModel = "{\"parent\":\"minecraft:block/stone\"}".getBytes(StandardCharsets.UTF_8);
        byte[] overrideBlockstate =
                "{\"variants\":{\"\":{\"model\":\"minecraft:block/stone\"}}}".getBytes(StandardCharsets.UTF_8);
        byte[] overrideGroup =
                "{\"type\":\"remi:group\",\"name\":\"override\",\"priority\":1}".getBytes(StandardCharsets.UTF_8);
        PackResources highPack = memoryPack(
                HIGH_PACK_ID,
                filterMcmeta(
                        "^models/item/compass_center\\.json$",
                        "^blockstates/stone_granite_bricks\\.json$",
                        "^stack_groups/material_forms/functional_drinks\\.json$"),
                Map.of(
                        "models/item/cryo_distillation_main.json", overrideModel,
                        "blockstates/stone_basalt_bricks.json", overrideBlockstate,
                        "stack_groups/material_forms/functional_co2_laser.json", overrideGroup));

        try (var manager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of(virtualPack, highPack))) {
            Predicate<ResourceLocation> gtNamespace =
                    id -> GregTechIdentity.REGISTRY_NAMESPACE.equals(id.getNamespace());

            // Models: a procedural catalog model, its composite sibling, one override and one
            // filtered path.
            var itemModels = manager.listResources("models/item", gtNamespace);
            ResourceLocation plainModel = gtResource("models/item/compass_north.json");
            String plainModelJson = readResource(helper, manager, plainModel);
            helper.assertTrue(
                    JsonParser.parseString(plainModelJson).isJsonObject(), "Invalid virtual model JSON");
            helper.assertTrue(
                    VIRTUAL_PACK_ID.equals(listedPackId(itemModels, plainModel)),
                    "Virtual model compass_north must come from the catalog pack");
            assertEnumeratedMatches(helper, itemModels, plainModel, plainModelJson);

            var machineModels = manager.listResources("models/block/machine/energy", gtNamespace);
            ResourceLocation compositeModel = gtResource("models/block/machine/energy/reactor_core.json");
            String compositeJson = readResource(helper, manager, compositeModel);
            var compositeObject = JsonParser.parseString(compositeJson).getAsJsonObject();
            helper.assertTrue(
                    compositeObject.has("loader")
                            && VirtualAssetPackResources.COMPOSITE_LOADER.equals(
                                    compositeObject.get("loader").getAsString()),
                    "Composite model must carry the active version's loader, got: " + compositeJson);
            helper.assertTrue(
                    VIRTUAL_PACK_ID.equals(listedPackId(machineModels, compositeModel)),
                    "Composite model must come from the catalog pack");
            assertEnumeratedMatches(helper, machineModels, compositeModel, compositeJson);

            ResourceLocation overriddenModel = gtResource("models/item/cryo_distillation_main.json");
            String overriddenModelJson = readResource(helper, manager, overriddenModel);
            helper.assertTrue(
                    overriddenModelJson.contains("minecraft:block/stone"),
                    "Higher pack override must win over the virtual model, got: " + overriddenModelJson);
            helper.assertTrue(
                    HIGH_PACK_ID.equals(listedPackId(itemModels, overriddenModel)),
                    "Enumerated model override must report the higher pack as its source");
            assertEnumeratedMatches(helper, itemModels, overriddenModel, overriddenModelJson);

            ResourceLocation filteredModel = gtResource("models/item/compass_center.json");
            helper.assertTrue(
                    manager.getResource(filteredModel).isEmpty(),
                    "Filter rule must block virtual models/item/compass_center.json");
            helper.assertTrue(
                    listedPackId(itemModels, filteredModel) == null,
                    "Filter rule must keep models/item/compass_center.json out of the listing");

            // Blockstates: a procedural catalog state, one override and one filtered path.
            var blockstates = manager.listResources("blockstates", gtNamespace);
            ResourceLocation keptState = gtResource("blockstates/stone_andesite_bricks.json");
            String keptStateJson = readResource(helper, manager, keptState);
            helper.assertTrue(
                    JsonParser.parseString(keptStateJson).isJsonObject(), "Invalid virtual blockstate JSON");
            helper.assertTrue(
                    VIRTUAL_PACK_ID.equals(listedPackId(blockstates, keptState)),
                    "Virtual blockstate must come from the catalog pack");
            assertEnumeratedMatches(helper, blockstates, keptState, keptStateJson);

            ResourceLocation overriddenState = gtResource("blockstates/stone_basalt_bricks.json");
            String overriddenStateJson = readResource(helper, manager, overriddenState);
            helper.assertTrue(
                    overriddenStateJson.contains("minecraft:block/stone"),
                    "Higher pack override must win over the virtual blockstate, got: " + overriddenStateJson);
            helper.assertTrue(
                    HIGH_PACK_ID.equals(listedPackId(blockstates, overriddenState)),
                    "Enumerated blockstate override must report the higher pack as its source");
            assertEnumeratedMatches(helper, blockstates, overriddenState, overriddenStateJson);

            ResourceLocation filteredState = gtResource("blockstates/stone_granite_bricks.json");
            helper.assertTrue(
                    manager.getResource(filteredState).isEmpty(),
                    "Filter rule must block virtual blockstates/stone_granite_bricks.json");
            helper.assertTrue(
                    listedPackId(blockstates, filteredState) == null,
                    "Filter rule must keep blockstates/stone_granite_bricks.json out of the listing");

            // Stack groups: one kept definition, one override and one filtered path, listed from the
            // family directory and from the stack_groups root alike.
            var groups = manager.listResources("stack_groups/material_forms", gtNamespace);
            var allGroups = manager.listResources("stack_groups", gtNamespace);
            ResourceLocation keptGroup =
                    gtResource("stack_groups/material_forms/functional_chemical_bottles.json");
            String keptGroupJson = readResource(helper, manager, keptGroup);
            helper.assertTrue(
                    JsonParser.parseString(keptGroupJson).isJsonObject(), "Invalid virtual stack group JSON");
            helper.assertTrue(
                    VIRTUAL_PACK_ID.equals(listedPackId(groups, keptGroup))
                            && VIRTUAL_PACK_ID.equals(listedPackId(allGroups, keptGroup)),
                    "Virtual stack group must come from the catalog pack in both listings");
            assertEnumeratedMatches(helper, groups, keptGroup, keptGroupJson);
            assertEnumeratedMatches(helper, allGroups, keptGroup, keptGroupJson);

            ResourceLocation overriddenGroup =
                    gtResource("stack_groups/material_forms/functional_co2_laser.json");
            String overriddenGroupJson = readResource(helper, manager, overriddenGroup);
            helper.assertTrue(
                    overriddenGroupJson.contains("\"name\":\"override\""),
                    "Higher pack override must win over the virtual stack group, got: " + overriddenGroupJson);
            helper.assertTrue(
                    HIGH_PACK_ID.equals(listedPackId(groups, overriddenGroup))
                            && HIGH_PACK_ID.equals(listedPackId(allGroups, overriddenGroup)),
                    "Enumerated stack group override must report the higher pack as its source");
            assertEnumeratedMatches(helper, groups, overriddenGroup, overriddenGroupJson);
            assertEnumeratedMatches(helper, allGroups, overriddenGroup, overriddenGroupJson);

            ResourceLocation filteredGroup =
                    gtResource("stack_groups/material_forms/functional_drinks.json");
            helper.assertTrue(
                    manager.getResource(filteredGroup).isEmpty(),
                    "Filter rule must block virtual stack_groups functional_drinks");
            helper.assertTrue(
                    listedPackId(groups, filteredGroup) == null
                            && listedPackId(allGroups, filteredGroup) == null,
                    "Filter rule must keep functional_drinks out of both stack group listings");
        }
        helper.succeed();
    }

    /**
     * The smeltery and hopper placeholders are the production routing for every block drawn by
     * generated geometry, so the dedicated server verifies the documents an addon pack resolves and
     * the six-way hopper blockstates the client pack serves.
     */
    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 40)
    public static void placeholderPackServesSmelteryHopperModelsAndSixWayStates(GameTestHelper helper)
            throws IOException {
        List<String> hulls = smelteryBlockIds();
        List<String> hoppers = hopperBlockIds();
        List<String> plainHoppers = new ArrayList<>(hoppers);
        plainHoppers.removeAll(SIX_WAY_HOPPER_IDS);
        List<String> blockModels = new ArrayList<>(hulls);
        blockModels.addAll(plainHoppers);
        helper.assertTrue(
                hulls.size() == SMELTERY_BLOCK_COUNT,
                "Expected " + SMELTERY_BLOCK_COUNT + " smeltery blocks, got " + hulls.size());
        helper.assertTrue(
                hoppers.size() == HOPPER_COUNT, "Expected " + HOPPER_COUNT + " hoppers, got " + hoppers.size());
        helper.assertTrue(
                blockModels.size() == SMELTERY_BLOCK_COUNT + HOPPER_COUNT - SIX_WAY_HOPPER_IDS.size(),
                "Expected " + (SMELTERY_BLOCK_COUNT + HOPPER_COUNT - SIX_WAY_HOPPER_IDS.size())
                        + " generated block models, got " + blockModels.size());

        Predicate<ResourceLocation> gtNamespace = id -> GregTechIdentity.REGISTRY_NAMESPACE.equals(id.getNamespace());
        byte[] childModel = "{\"parent\":\"gt6ce:block/blocks/crucible_faucet_bronze\"}"
                .getBytes(StandardCharsets.UTF_8);
        PackResources placeholderPack = placeholderCatalogPack();
        PackResources childPack = memoryPack(
                PLACEHOLDER_CHILD_PACK_ID, null, Map.of("models/block/custom_faucet_child.json", childModel));

        try (var manager = new MultiPackResourceManager(
                PackType.CLIENT_RESOURCES, List.of(placeholderPack, childPack))) {
            // Every generated block resolves the one shared iron-block parent model.
            var blockModelsListed = manager.listResources("models/block/blocks", gtNamespace);
            helper.assertTrue(
                    blockModelsListed.size() == blockModels.size(),
                    "Expected " + blockModels.size() + " models/block/blocks documents, got "
                            + blockModelsListed.size());
            for (String id : blockModels) {
                ResourceLocation model = gtResource("models/block/blocks/" + id + ".json");
                helper.assertTrue(blockModelsListed.containsKey(model), "Missing generated block model " + model);
                helper.assertTrue(
                        PLACEHOLDER_PACK_ID.equals(listedPackId(blockModelsListed, model)),
                        "Generated block model " + model + " must come from the placeholder pack");
            }
            ResourceLocation faucetParent = gtResource("models/block/blocks/crucible_faucet_bronze.json");
            helper.assertTrue(
                    IRON_BLOCK_MODEL_JSON.equals(readResource(helper, manager, faucetParent)),
                    "Sampled faucet parent must be the iron-block model");
            assertEnumeratedMatches(helper, blockModelsListed, faucetParent, IRON_BLOCK_MODEL_JSON);

            // An addon child model reaches the placeholder pack through its parent reference.
            ResourceLocation child = gtResource("models/block/custom_faucet_child.json");
            String childJson = readResource(helper, manager, child);
            String parent = JsonParser.parseString(childJson).getAsJsonObject().get("parent").getAsString();
            helper.assertTrue(
                    "gt6ce:block/blocks/crucible_faucet_bronze".equals(parent),
                    "Child model must reference the generated faucet parent, got: " + parent);
            ResourceLocation parentId = ResourceLocation.tryParse(parent);
            helper.assertTrue(parentId != null, "Child model parent must parse, got: " + parent);
            if (parentId != null) {
                ResourceLocation parentResource = ResourceLocation.fromNamespaceAndPath(
                        parentId.getNamespace(), "models/" + parentId.getPath() + ".json");
                helper.assertTrue(
                        IRON_BLOCK_MODEL_JSON.equals(readResource(helper, manager, parentResource)),
                        "Child model parent chain must resolve to the iron-block model");
            }
            var blockModelsOfChild = manager.listResources("models/block", gtNamespace);
            helper.assertTrue(
                    blockModelsOfChild.containsKey(child), "Listing must contain the addon child model " + child);
            assertEnumeratedMatches(helper, blockModelsOfChild, child, childJson);

            // Six-way hoppers keep their directional blockstate and their own inventory parent.
            for (String id : SIX_WAY_HOPPER_IDS) {
                boolean queue = id.startsWith("queue_");
                String blockstate = readResource(helper, manager, gtResource("blockstates/" + id + ".json"));
                helper.assertTrue(
                        sixWayBlockstate(queue ? "queuehopper" : "hopper").equals(blockstate),
                        "Six-way blockstate mismatch for " + id + ", got: " + blockstate);
                String item = readResource(helper, manager, gtResource("models/item/" + id + ".json"));
                String expectedItem = queue
                        ? "{\"parent\":\"gt6ce:block/machine/queuehopper_north\"}"
                        : "{\"parent\":\"gt6ce:block/machine/hopper\"}";
                helper.assertTrue(
                        expectedItem.equals(item), "Inventory parent mismatch for " + id + ", got: " + item);
                ResourceLocation blocksModel = gtResource("models/block/blocks/" + id + ".json");
                helper.assertTrue(
                        manager.getResource(blocksModel).isEmpty(),
                        "Six-way hopper " + id + " must not emit " + blocksModel);
                helper.assertTrue(
                        !blockModelsListed.containsKey(blocksModel),
                        "Six-way hopper " + id + " must stay out of the models/block/blocks listing");
            }

            // Every other hopper keeps the stone placeholders plus the iron-block parent model.
            for (String id : List.of("hopper_lead", "queue_hopper_lead")) {
                helper.assertTrue(hoppers.contains(id), "Expected a registered hopper " + id);
                String blockstate = readResource(helper, manager, gtResource("blockstates/" + id + ".json"));
                helper.assertTrue(
                        STONE_BLOCKSTATE_JSON.equals(blockstate),
                        "Plain hopper " + id + " must keep the stone blockstate");
                String item = readResource(helper, manager, gtResource("models/item/" + id + ".json"));
                helper.assertTrue(
                        STONE_ITEM_JSON.equals(item),
                        "Plain hopper " + id + " must keep the stone inventory parent");
                helper.assertTrue(
                        IRON_BLOCK_MODEL_JSON.equals(
                                readResource(helper, manager, gtResource("models/block/blocks/" + id + ".json"))),
                        "Plain hopper " + id + " must resolve the iron-block parent model");
            }

            // Exact placeholder counts: one blockstate and one inventory parent per block, and one
            // parent model per smeltery hull and plain hopper.
            List<String> placeholders = new ArrayList<>(hulls);
            placeholders.addAll(hoppers);
            var blockstates = manager.listResources("blockstates", gtNamespace);
            var itemModels = manager.listResources("models/item", gtNamespace);
            int placeholderStates = 0;
            int placeholderItems = 0;
            for (String id : placeholders) {
                if (blockstates.containsKey(gtResource("blockstates/" + id + ".json"))) placeholderStates++;
                if (itemModels.containsKey(gtResource("models/item/" + id + ".json"))) placeholderItems++;
                helper.assertTrue(
                        !VirtualBlockstateCatalog.contains(id),
                        "Placeholder blockstate " + id + " must not overlap the virtual blockstate catalog");
            }
            helper.assertTrue(
                    placeholderStates == SMELTERY_BLOCK_COUNT + HOPPER_COUNT,
                    "Expected " + (SMELTERY_BLOCK_COUNT + HOPPER_COUNT) + " placeholder blockstates, got "
                            + placeholderStates);
            helper.assertTrue(
                    placeholderItems == SMELTERY_BLOCK_COUNT + HOPPER_COUNT,
                    "Expected " + (SMELTERY_BLOCK_COUNT + HOPPER_COUNT) + " placeholder inventory parents, got "
                            + placeholderItems);
            for (String id : blockModels) {
                helper.assertTrue(
                        !VirtualModelCatalog.contains("block/blocks/" + id),
                        "Generated block model block/blocks/" + id + " must not overlap the virtual model catalog");
            }
        }
        helper.succeed();
    }

    /** The six-way {@code facing} blockstate of {@code modelPrefix} hoppers. */
    private static String sixWayBlockstate(String modelPrefix) {
        StringBuilder json = new StringBuilder("{\"variants\":{");
        for (int i = 0; i < SIX_WAY_FACINGS.length; i++) {
            if (i > 0) json.append(',');
            json.append("\"facing=")
                    .append(SIX_WAY_FACINGS[i])
                    .append("\":{\"model\":\"gt6ce:block/machine/")
                    .append(modelPrefix)
                    .append('_')
                    .append(SIX_WAY_FACINGS[i])
                    .append("\"}");
        }
        return json.append("}}").toString();
    }

    /** Registered smeltery hull, mold, basin, crossing and faucet paths. */
    private static List<String> smelteryBlockIds() {
        List<String> ids = new ArrayList<>();
        addBlockIds(ids, GTSmeltery.crucibles());
        addBlockIds(ids, GTSmeltery.basins());
        addBlockIds(ids, GTSmeltery.molds());
        addBlockIds(ids, GTSmeltery.crossings());
        addBlockIds(ids, GTSmeltery.faucets());
        return ids;
    }

    /** Registered hopper and queue hopper paths. */
    private static List<String> hopperBlockIds() {
        List<String> ids = new ArrayList<>();
        addBlockIds(ids, GTHoppers.hoppers());
        addBlockIds(ids, GTHoppers.queues());
        return ids;
    }

    private static <T extends Block> void addBlockIds(List<String> ids, List<RegisteredBlock<T>> entries) {
        for (RegisteredBlock<T> entry : entries) {
            if (entry.isPresent()) ids.add(entry.getId().getPath());
        }
    }

    /** Resource location in the GregTech asset namespace. */
    private static ResourceLocation gtResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, path);
    }

    /** Pack id that serves {@code id} in a listing, or {@code null} when the listing omits it. */
    @Nullable
    private static String listedPackId(Map<ResourceLocation, Resource> listed, ResourceLocation id) {
        Resource resource = listed.get(id);
        return resource == null ? null : resource.sourcePackId();
    }

    /** Reads {@code id} through the manager, failing when no pack in the chain serves it. */
    private static String readResource(GameTestHelper helper, ResourceManager manager, ResourceLocation id)
            throws IOException {
        var resource = manager.getResource(id);
        helper.assertTrue(resource.isPresent(), "Missing resource: " + id);
        try (InputStream in = resource.get().open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Asserts that the enumerated body of {@code id} matches the body read directly. */
    private static void assertEnumeratedMatches(
            GameTestHelper helper, Map<ResourceLocation, Resource> listed, ResourceLocation id, String expected)
            throws IOException {
        Resource resource = listed.get(id);
        helper.assertTrue(resource != null, "Listing must contain " + id);
        if (resource == null) return;
        try (InputStream in = resource.open()) {
            helper.assertTrue(
                    expected.equals(new String(in.readAllBytes(), StandardCharsets.UTF_8)),
                    "Enumerated " + id + " must match the direct read");
        }
    }

    private static PackResources virtualCatalogPack() {
//? if >=1.21.1 {
        /*return VirtualAssetPackResources.catalogOnly(new PackLocationInfo(
                VIRTUAL_PACK_ID,
                Component.literal(VIRTUAL_PACK_ID),
                PackSource.BUILT_IN,
                Optional.empty()));
*///?} else {
        return VirtualAssetPackResources.catalogOnly(VIRTUAL_PACK_ID);
//?}
    }

    /** The production placeholder routing, mounted without any client wiring. */
    private static PackResources placeholderCatalogPack() {
//? if >=1.21.1 {
        /*return VirtualAssetPackResources.catalogWithPlaceholders(new PackLocationInfo(
                PLACEHOLDER_PACK_ID,
                Component.literal(PLACEHOLDER_PACK_ID),
                PackSource.BUILT_IN,
                Optional.empty()));
*///?} else {
        return VirtualAssetPackResources.catalogWithPlaceholders(PLACEHOLDER_PACK_ID);
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
}
