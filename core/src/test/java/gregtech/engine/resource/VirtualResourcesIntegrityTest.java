package gregtech.engine.resource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.block.stone.StoneType;
import gregtech.content.material.MaterialTestFixture;
import gregtech.content.transport.HopperCatalog;
import gregtech.engine.definition.TexturePaletteTable;
import gregtech.engine.machine.CrucibleSpec;
import gregtech.engine.machine.OriginalSmelteryDefinitions;
import gregtech.engine.machine.OrthogonalMachineModels;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.recipe.data.JsonMaps;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Verifies integrity, parser strictness, reference closure, and static asset budgets for the
 * virtual model, blockstate, and stack-group catalogs across all resource roots.
 */
final class VirtualResourcesIntegrityTest {
    private static final String NAMESPACE_PREFIX = GregTechIdentity.REGISTRY_NAMESPACE + ":";
    private static final String FORGE_COMPOSITE = "forge:composite";
    private static final String NEOFORGE_COMPOSITE = "neoforge:composite";
    private static final int MAX_STATIC_JSON = 450;
    private static final int EXPECTED_STATIC_MODELS = 319;
    private static final int EXPECTED_STATIC_BLOCKSTATES = 100;
    private static final int EXPECTED_VIRTUAL_MODELS = 6608;
    private static final int EXPECTED_MODEL_TSV_ROWS = 4378;
    private static final int EXPECTED_VIRTUAL_BLOCKSTATES = 2180;
    private static final int EXPECTED_BLOCKSTATE_TSV_ROWS = 1245;
    private static final int EXPECTED_BLOCKSTATE_KINDS = 6;
    private static final int EXPECTED_FAMILY_BLOCKSTATES = 289;
    private static final int EXPECTED_STACK_GROUPS = 464;
    private static final int EXPECTED_REGEX_GROUPS = 45;
    private static final int EXPECTED_TAG_GROUPS = 111;
    private static final int EXPECTED_ITEM_GROUPS = 308;
    private static final int EXPECTED_SMELTERY_CRUCIBLES = 29;
    private static final int EXPECTED_SMELTERY_COMPONENTS = 145;
    private static final int EXPECTED_HOPPER_ENTRIES = 88;
    private static final List<String> SMELTERY_COMPONENT_PREFIXES =
            List.of("smelting_crucible_", "mold_", "mold_basin_", "crucible_crossing_", "crucible_faucet_");
    private static final List<String> HOPPER_FAMILIES = List.of("hopper", "queuehopper");
    private static final List<String> HOPPER_DIRECTIONS = List.of("down", "up", "north", "south", "west", "east");

    @Test
    void stoneSpecsMatchStoneTypeEnum() {
        MaterialTestFixture.ensureInitialized();
        StoneType[] types = StoneType.values();
        List<ProceduralModels.StoneSpec> specs = ProceduralModels.stoneSpecs();
        assertEquals(types.length, specs.size(), "Procedural stone spec count must match StoneType");
        for (int i = 0; i < types.length; i++) {
            StoneType type = types[i];
            ProceduralModels.StoneSpec spec = specs.get(i);
            assertEquals(type.registryId(), spec.registryId(), "StoneSpec registryId mismatch at index " + i);
            assertEquals(type.textureFolder(), spec.textureFolder(), "StoneSpec textureFolder mismatch at index " + i);
        }
    }

    @Test
    void parsersFailFastOnMalformedRowsAndDuplicateKeys() {
        assertThrows(IOException.class, () -> VirtualModelCatalog.parse(utf8Stream("bad\tonlytwo\n")));
        assertThrows(
                IOException.class,
                () -> VirtualModelCatalog.parse(utf8Stream("item/x\tminecraft:item/generated\t\tbadpair\n")));
        assertParseFailure(
                () -> VirtualModelCatalog.parse(
                        utf8Stream("item/x\tminecraft:item/generated\t\tlayer0=gt6ce:item/a;layer0=gt6ce:item/b\n")),
                1,
                "layer0");
        assertParseFailure(
                () -> VirtualModelCatalog.parse(utf8Stream("item/dup\tminecraft:item/generated\t\tlayer0=gt6ce:item/a\n"
                        + "item/dup\tminecraft:item/generated\t\tlayer0=gt6ce:item/a\n")),
                2,
                "item/dup");
        assertThrows(
                IOException.class,
                () -> VirtualModelCatalog.parse(
                        utf8Stream("block/tool/scaffold_x\t@scaffold1\t\tparticle=gt6ce:block/iconsets/plate\n")));

        assertThrows(IOException.class, () -> VirtualBlockstateCatalog.parse(utf8Stream("bad\tonly_two_columns\n")));
        assertThrows(
                IOException.class,
                () -> VirtualBlockstateCatalog.parse(utf8Stream("b\tUNKNOWN_KIND\tgt6ce:block/x\n")));
        assertThrows(
                IOException.class,
                () -> VirtualBlockstateCatalog.parse(utf8Stream("b\tFACING_4_MODELS\tgt6ce:block/only_one\n")));
        assertThrows(
                IOException.class,
                () -> VirtualBlockstateCatalog.parse(utf8Stream("b\tSINGLETON\tgt6ce:block/a;gt6ce:block/b\n")));
        assertParseFailure(
                () -> VirtualBlockstateCatalog.parse(
                        utf8Stream("dup\tSINGLETON\tgt6ce:block/a\ndup\tSINGLETON\tgt6ce:block/b\n")),
                2,
                "dup");
        for (String retired : List.of("WOOD_SLAB", "BUSH_STAGES")) {
            assertThrows(
                    IOException.class,
                    () -> VirtualBlockstateCatalog.parse(utf8Stream("b\t" + retired + "\tgt6ce:block/a\n")),
                    "Retired blockstate kind must be rejected: " + retired);
        }

        assertThrows(IOException.class, () -> StackGroupCatalog.parseItems(utf8Stream("bad\n")));
        assertThrows(
                IOException.class, () -> StackGroupCatalog.parseItems(utf8Stream("g\tnot_int\tName\tgt6ce:ingot\t\n")));
        assertThrows(IOException.class, () -> StackGroupCatalog.parseItems(utf8Stream("g\t0\tName\t\t\n")));
        assertThrows(
                IOException.class, () -> StackGroupCatalog.parseItems(utf8Stream("g\t0\tName\tgt6ce:a;;gt6ce:b\t\n")));
        assertThrows(IOException.class, () -> StackGroupCatalog.parseItems(utf8Stream("g\t0\tName\tgt6ce:a\tBOGUS\n")));
        assertParseFailure(
                () -> StackGroupCatalog.parseItems(utf8Stream("dup\t0\tA\tgt6ce:a\t\ndup\t0\tB\tgt6ce:b\t\n")),
                2,
                "dup");

        assertThrows(IOException.class, () -> StackGroupCatalog.parseRegex(utf8Stream("bad\n")));
        assertThrows(IOException.class, () -> StackGroupCatalog.parseRegex(utf8Stream("g\t0\tName\t\t\n")));
        assertThrows(IOException.class, () -> StackGroupCatalog.parseRegex(utf8Stream("g\t0\tName\t[unclosed\t\n")));
        assertThrows(
                IOException.class, () -> StackGroupCatalog.parseRegex(utf8Stream("g\t0\tName\t^gt6ce:a$\tBOGUS\n")));
        assertParseFailure(
                () -> StackGroupCatalog.parseRegex(utf8Stream("dup\t0\tA\t^gt6ce:a$\t\ndup\t0\tB\t^gt6ce:b$\t\n")),
                2,
                "dup");
    }

    /**
     * Verifies that the smeltery component and hopper placeholder ids stay disjoint from the virtual
     * catalogs and that the shared hopper templates their fallback blockstates reference are shipped as
     * static JSON.
     */
    @Test
    void placeholderModelsAreDisjointAndResolveToSharedTemplates() throws IOException {
        MaterialTestFixture.ensureInitialized();
        Map<String, String> staticModels = collectStaticJson("models");

        Set<String> smelteryIds = new HashSet<>();
        for (CrucibleSpec spec : OriginalSmelteryDefinitions.all()) {
            smelteryIds.add(spec.id());
        }
        List<CrucibleSpec> crucibles = OriginalSmelteryDefinitions.crucibles();
        assertEquals(EXPECTED_SMELTERY_CRUCIBLES, crucibles.size(), "Smeltery crucible count");
        assertEquals(EXPECTED_SMELTERY_COMPONENTS, smelteryIds.size(), "Smeltery component id count");
        for (CrucibleSpec crucible : crucibles) {
            String tier = crucible.id().substring("smelting_crucible_".length());
            for (String prefix : SMELTERY_COMPONENT_PREFIXES) {
                String id = prefix + tier;
                assertTrue(smelteryIds.contains(id), "Missing smeltery component " + id);
            }
        }
        assertTrue(smelteryIds.contains("crucible_crossing_steel"), "Missing crucible_crossing_steel");

        Set<String> hopperIds = new HashSet<>();
        for (HopperCatalog.Entry entry : HopperCatalog.ALL) {
            hopperIds.add(entry.spec().id());
        }
        assertEquals(EXPECTED_HOPPER_ENTRIES, hopperIds.size(), "Hopper catalog entry count");
        assertTrue(hopperIds.contains("hopper_bronze"), "Missing hopper_bronze");

        Set<String> placeholders = new HashSet<>(smelteryIds);
        placeholders.addAll(hopperIds);
        for (String id : placeholders) {
            assertFalse(VirtualModelCatalog.contains("item/" + id), "Virtual item model shadows placeholder " + id);
            assertFalse(
                    VirtualModelCatalog.contains("block/blocks/" + id),
                    "Virtual block model shadows placeholder " + id);
            assertFalse(VirtualBlockstateCatalog.contains(id), "Virtual blockstate shadows placeholder " + id);
        }

        assertTrue(
                staticModels.containsKey("block/machine/hopper"), "Missing shared hopper model: block/machine/hopper");
        for (String family : HOPPER_FAMILIES) {
            for (String direction : HOPPER_DIRECTIONS) {
                String path = "block/machine/" + family + "_" + direction;
                assertTrue(staticModels.containsKey(path), "Missing shared hopper model: " + path);
            }
        }
    }

    @Test
    void virtualAndStaticModelsAreValidJsonNonOverlappingAndWithinBudget() throws IOException {
        Map<String, String> staticModels = collectStaticJson("models");
        Map<String, String> staticBlockstates = collectStaticJson("blockstates");
        assertEquals(EXPECTED_STATIC_MODELS, staticModels.size(), "Static model JSON count");
        assertEquals(EXPECTED_STATIC_BLOCKSTATES, staticBlockstates.size(), "Static blockstate JSON count");
        int totalStaticJson = staticModels.size() + staticBlockstates.size();
        assertTrue(
                totalStaticJson <= MAX_STATIC_JSON,
                "Static models (" + staticModels.size() + ") + blockstates (" + staticBlockstates.size()
                        + ") exceed budget " + MAX_STATIC_JSON);
        for (Path root : assetRoots()) {
            Path stackGroupsDir = root.resolve("stack_groups");
            assertFalse(Files.exists(stackGroupsDir), "Static stack_groups must not exist: " + stackGroupsDir);
        }
        assertTrue(
                files(forgeAssets(), ".json").isEmpty(),
                "1.20.1-forge assets directory must not carry static JSON files");

        assertEquals(EXPECTED_MODEL_TSV_ROWS, VirtualModelCatalog.entries().size());
        Map<String, String> forgeVirtual = new LinkedHashMap<>();
        VirtualModelCatalog.emitAll(
                FORGE_COMPOSITE, (path, bytes) -> forgeVirtual.put(path, new String(bytes, StandardCharsets.UTF_8)));
        Map<String, String> neoforgeVirtual = new LinkedHashMap<>();
        VirtualModelCatalog.emitAll(
                NEOFORGE_COMPOSITE,
                (path, bytes) -> neoforgeVirtual.put(path, new String(bytes, StandardCharsets.UTF_8)));

        assertEquals(EXPECTED_VIRTUAL_MODELS, neoforgeVirtual.size());
        assertEquals(neoforgeVirtual.keySet(), forgeVirtual.keySet());

        int compositeCount = 0;
        for (String path : neoforgeVirtual.keySet()) {
            String forgeJson = forgeVirtual.get(path);
            String neoJson = neoforgeVirtual.get(path);
            Map<String, Object> forgeObj = JsonMaps.object(forgeJson);
            Map<String, Object> neoObj = JsonMaps.object(neoJson);
            if (NEOFORGE_COMPOSITE.equals(neoObj.get("loader"))) {
                assertEquals(FORGE_COMPOSITE, forgeObj.get("loader"), "Forge composite loader mismatch on " + path);
                assertNotEquals(forgeJson, neoJson);
                compositeCount++;
            } else {
                assertEquals(forgeJson, neoJson, "Non-composite model differs between loaders: " + path);
            }
            assertFalse(staticModels.containsKey(path), "Static model shadows VirtualModelCatalog entry: " + path);
            assertFalse(
                    OrthogonalMachineModels.contains(path), "Virtual model overlaps OrthogonalMachineModels: " + path);
        }
        assertTrue(compositeCount > 0, "Expected composite models in virtual catalog");

        for (String machinePath : OrthogonalMachineModels.all().keySet()) {
            assertFalse(
                    staticModels.containsKey(machinePath),
                    "Static model shadows OrthogonalMachineModels entry: " + machinePath);
        }

        Set<String> allKnownModels = new HashSet<>(staticModels.keySet());
        allKnownModels.addAll(neoforgeVirtual.keySet());
        allKnownModels.addAll(OrthogonalMachineModels.all().keySet());
        addRuntimeSynthesizedModels(allKnownModels);

        Set<String> shippedTextures = collectShippedTextures();
        TexturePaletteTable paletteTable = TexturePaletteTable.load();
        List<String> unresolved = new ArrayList<>();

        for (var entry : staticModels.entrySet()) {
            verifyModelReferences(
                    "static:" + entry.getKey(),
                    JsonMaps.object(entry.getValue()),
                    allKnownModels,
                    shippedTextures,
                    paletteTable,
                    unresolved);
        }
        for (var entry : neoforgeVirtual.entrySet()) {
            verifyModelReferences(
                    "virtual:" + entry.getKey(),
                    JsonMaps.object(entry.getValue()),
                    allKnownModels,
                    shippedTextures,
                    paletteTable,
                    unresolved);
        }
        assertTrue(unresolved.isEmpty(), "Unresolved model/texture references: " + unresolved);
    }

    @Test
    void virtualBlockstatesAreValidJsonWithCompleteModelReferences() throws IOException {
        Map<String, String> staticModels = collectStaticJson("models");
        Map<String, String> staticBlockstates = collectStaticJson("blockstates");
        assertEquals(EXPECTED_BLOCKSTATE_KINDS, VirtualBlockstateCatalog.Kind.values().length, "Blockstate kind count");
        assertEquals(
                EXPECTED_FAMILY_BLOCKSTATES, FamilyBlockstateGenerators.stems().size(), "Family blockstate stem count");
        assertEquals(
                EXPECTED_BLOCKSTATE_TSV_ROWS, VirtualBlockstateCatalog.entries().size());

        Map<String, String> virtualBlockstates = new LinkedHashMap<>();
        VirtualBlockstateCatalog.emitAll(
                (stem, bytes) -> virtualBlockstates.put(stem, new String(bytes, StandardCharsets.UTF_8)));
        assertEquals(EXPECTED_VIRTUAL_BLOCKSTATES, virtualBlockstates.size());

        Set<String> allKnownModels = new HashSet<>(staticModels.keySet());
        VirtualModelCatalog.emitAll(NEOFORGE_COMPOSITE, (path, bytes) -> allKnownModels.add(path));
        allKnownModels.addAll(OrthogonalMachineModels.all().keySet());
        addRuntimeSynthesizedModels(allKnownModels);

        List<String> missingModels = new ArrayList<>();
        for (var entry : virtualBlockstates.entrySet()) {
            String stem = entry.getKey();
            assertFalse(
                    staticBlockstates.containsKey(stem),
                    "Static blockstate shadows VirtualBlockstateCatalog entry: " + stem);
            assertTrue(VirtualBlockstateCatalog.contains(stem));
            Map<String, Object> doc = JsonMaps.object(entry.getValue());
            verifyBlockstateModels("virtual_bs:" + stem, doc, allKnownModels, missingModels);
        }
        for (var entry : staticBlockstates.entrySet()) {
            Map<String, Object> doc = JsonMaps.object(entry.getValue());
            verifyBlockstateModels("static_bs:" + entry.getKey(), doc, allKnownModels, missingModels);
        }
        assertTrue(missingModels.isEmpty(), "Blockstates reference missing models: " + missingModels);
    }

    /**
     * Verifies that {@code contains} and {@code renderJson} agree for every catalog entry and for the
     * malformed paths a caller may probe, and that every variant of every blockstate kind stays inside
     * the model list of its row.
     */
    @Test
    void virtualCatalogRenderJsonMatchesContainsAndRejectsUnknownPaths() {
        List<String> unknownModels = Arrays.asList(
                null,
                "",
                "item/",
                "item/compass",
                "item/compassX",
                "item/compassXnorth",
                "item/compassXdeath",
                "item/compass/",
                "item/compass/north",
                "item/compass/north_",
                "item/compass/north_0",
                "item/compass/north_01",
                "item/compass/north_16",
                "item/compass_north/",
                "item//compass_north",
                "ITEM/COMPASS_NORTH",
                "block/stones/",
                "item/stone_",
                "block/decoration/pile/",
                "block/decoration/pile/ingot_",
                "block/decoration/pile/ingot_007",
                "block/decoration/pile/ingot_65",
                "block/logistics/",
                "block/logistics/filter_slots/",
                "block/machine/energy/reactor_core_",
                "block/machine/energy/reactor_coreX",
                "block/machine/energy/reactor_core_down_down",
                "block/machine/energy/reactor_core_2x2_down_down",
                "block/chemical_battery/",
                "block/chemical_battery/standard_8_00",
                "block/chemical_battery/standard_8_5",
                "block/tool/scaffold_steel_1",
                "block/tool/scaffold_bogus_1",
                "item/battery_");
        for (String loader : List.of(FORGE_COMPOSITE, NEOFORGE_COMPOSITE)) {
            for (String path : unknownModels) {
                assertDoesNotThrow(() -> VirtualModelCatalog.contains(path), "contains must not throw on " + path);
                assertDoesNotThrow(
                        () -> VirtualModelCatalog.renderJson(path, loader), "renderJson must not throw on " + path);
                assertFalse(VirtualModelCatalog.contains(path), "Unknown model must not be contained: " + path);
                assertNull(VirtualModelCatalog.renderJson(path, loader), "Unknown model must not render: " + path);
            }
        }

        for (String loader : List.of(FORGE_COMPOSITE, NEOFORGE_COMPOSITE)) {
            Map<String, String> emitted = new LinkedHashMap<>();
            VirtualModelCatalog.emitAll(
                    loader, (path, bytes) -> emitted.put(path, new String(bytes, StandardCharsets.UTF_8)));
            assertFalse(emitted.isEmpty(), "No models emitted for " + loader);
            for (var entry : emitted.entrySet()) {
                assertTrue(
                        VirtualModelCatalog.contains(entry.getKey()),
                        "Emitted model must be contained: " + entry.getKey());
                assertEquals(
                        entry.getValue(),
                        VirtualModelCatalog.renderJson(entry.getKey(), loader),
                        "Rendered model differs from the emitted document: " + entry.getKey());
            }
        }

        for (String path : List.of("block/tool/scaffold_1", "block/tool/scaffold_steel_galvanized_1")) {
            assertTrue(VirtualModelCatalog.contains(path), "Procedural scaffold model must be contained: " + path);
            for (String loader : List.of(FORGE_COMPOSITE, NEOFORGE_COMPOSITE)) {
                assertNotNull(
                        VirtualModelCatalog.renderJson(path, loader),
                        "Procedural scaffold model must render for " + loader + ": " + path);
            }
        }

        Map<String, String> emittedBlockstates = new LinkedHashMap<>();
        VirtualBlockstateCatalog.emitAll(
                (stem, bytes) -> emittedBlockstates.put(stem, new String(bytes, StandardCharsets.UTF_8)));
        assertFalse(emittedBlockstates.isEmpty(), "No blockstates emitted");
        for (var entry : emittedBlockstates.entrySet()) {
            assertTrue(
                    VirtualBlockstateCatalog.contains(entry.getKey()),
                    "Emitted blockstate must be contained: " + entry.getKey());
            assertEquals(
                    entry.getValue(),
                    VirtualBlockstateCatalog.renderJson(entry.getKey()),
                    "Rendered blockstate differs from the emitted document: " + entry.getKey());
        }
        for (String stem : Arrays.asList(
                null,
                "",
                "stone_",
                "stone_granite_black_stone_slab_side",
                "ingot_piles",
                "filter_other",
                "battery_unknown",
                "battery_lit_ulv",
                "reactor_core_3x3",
                "reactor_core_down_down",
                "safe_steel",
                "safe_bogus",
                "scaffold_steel",
                "scaffold_bogus",
                "key_safe_bogus",
                "engine_electric_bogus",
                "sensor_bogus",
                "co2_laser_uv",
                "quantum_energizer_hv",
                "track_booster_bogus",
                "slab_bogus")) {
            assertFalse(VirtualBlockstateCatalog.contains(stem), "Unknown blockstate must not be contained: " + stem);
            assertNull(VirtualBlockstateCatalog.renderJson(stem), "Unknown blockstate must not render: " + stem);
        }

        Set<String> familyStems = FamilyBlockstateGenerators.stems();
        assertEquals(EXPECTED_FAMILY_BLOCKSTATES, familyStems.size(), "Family blockstate stem count");
        for (String stem : familyStems) {
            assertTrue(VirtualBlockstateCatalog.contains(stem), "Family blockstate must be contained: " + stem);
            assertEquals(
                    FamilyBlockstateGenerators.renderJson(stem),
                    VirtualBlockstateCatalog.renderJson(stem),
                    "Catalog render differs from the family render: " + stem);
        }

        for (VirtualBlockstateCatalog.Kind kind : VirtualBlockstateCatalog.Kind.values()) {
            for (Object variant : variantsOf(kind)) {
                for (Object reference : modelsOf(variant)) {
                    int model = modelIndexOf(reference);
                    assertTrue(
                            model >= 0 && model < kind.modelCount(),
                            "Model index " + model + " out of range for " + kind + " with " + kind.modelCount()
                                    + " models");
                }
            }
        }
    }

    @Test
    void stackGroupsAreCleanValidAndComplete() throws IOException {
        Map<String, StackGroupCatalog.Entry> entries = StackGroupCatalog.entries();
        assertEquals(EXPECTED_STACK_GROUPS, entries.size());

        int regexCount = 0;
        int tagCount = 0;
        int itemsCount = 0;
        for (StackGroupCatalog.Entry entry : entries.values()) {
            switch (entry.kind()) {
                case REGEX -> {
                    regexCount++;
                    assertFalse(entry.payload().isBlank(), "Empty regex payload on " + entry.stem());
                    Pattern.compile(entry.payload());
                }
                case TAG -> {
                    tagCount++;
                    assertFalse(entry.payload().isBlank(), "Empty tag payload on " + entry.stem());
                    verifyTagEntry(entry);
                }
                case ITEMS -> {
                    itemsCount++;
                    assertFalse(entry.contents().isEmpty(), "Empty ITEMS contents on " + entry.stem());
                    Set<String> uniqueContents = new HashSet<>(entry.contents());
                    assertEquals(
                            uniqueContents.size(),
                            entry.contents().size(),
                            "Duplicate item entries in stack group " + entry.stem());
                }
            }
            Map<String, Object> json = JsonMaps.object(StackGroupCatalog.renderJson(entry));
            assertNotNull(json.get("name"), "Missing name on stack group " + entry.stem());
            if (entry.kind() == StackGroupCatalog.Kind.REGEX) {
                assertTrue(json.get("regex") instanceof String, "Missing regex string on " + entry.stem());
                assertFalse(json.containsKey("contents"), "Regex stack group must not carry contents: " + entry.stem());
            } else {
                assertTrue(json.get("contents") instanceof List<?>, "Missing contents array on " + entry.stem());
            }
        }
        assertEquals(EXPECTED_REGEX_GROUPS, regexCount);
        assertEquals(EXPECTED_TAG_GROUPS, tagCount);
        assertEquals(EXPECTED_ITEM_GROUPS, itemsCount);

        assertEquals(
                EXPECTED_ITEM_GROUPS,
                parseItemsResource().size(),
                "Row count of " + StackGroupCatalog.ITEMS_RESOURCE_PATH);
        assertEquals(
                EXPECTED_REGEX_GROUPS,
                parseRegexResource().size(),
                "Row count of " + StackGroupCatalog.REGEX_RESOURCE_PATH);

        StackGroupCatalog.Entry ores = StackGroupCatalog.entry("ores");
        assertNotNull(ores);
        assertFalse(ores.payload().contains("ore_bedrock_"), "Stale ore_bedrock_ alternation in ores");

        StackGroupCatalog.Entry cfoam = StackGroupCatalog.entry("decor_cfoam");
        assertNotNull(cfoam);
        assertFalse(cfoam.contents().contains("item:gt6ce:cfoam_hardened"));
        assertFalse(cfoam.contents().contains("item:gt6ce:cfoam_hardened_owned"));

        StackGroupCatalog.Entry cfoamFresh = StackGroupCatalog.entry("decor_cfoam_fresh");
        assertNotNull(cfoamFresh);
        assertFalse(cfoamFresh.contents().contains("item:gt6ce:cfoam_fresh_owned"));

        StackGroupCatalog.Entry emitters = StackGroupCatalog.entry("functional_laser_emitters");
        assertNotNull(emitters);
        assertFalse(emitters.contents().isEmpty());
        assertTrue(
                emitters.contents().stream().noneMatch(item -> item.contains(":co2_laser_")),
                "co2_laser_* entries belong to the functional_co2_laser group rather than functional_laser_emitters");
        StackGroupCatalog.Entry co2Lasers = StackGroupCatalog.entry("functional_co2_laser");
        assertNotNull(co2Lasers);
        for (String tier : List.of("lv", "mv", "hv", "ev", "iv")) {
            assertTrue(
                    co2Lasers.contents().contains("item:gt6ce:co2_laser_" + tier),
                    "functional_co2_laser must keep item:gt6ce:co2_laser_" + tier);
        }

        Map<String, byte[]> built = StackGroupCatalog.buildAllResources();
        assertEquals(EXPECTED_STACK_GROUPS, built.size());
        for (String stem : entries.keySet()) {
            String key = "stack_groups/material_forms/" + stem + ".json";
            assertTrue(built.containsKey(key), "Missing stack_groups path: " + key);
        }
    }

    /**
     * Verifies a generated tag group: the shared priority, the item-group translation key, the single
     * {@code #gt6ce:} tag it selects and the exclusion list its code resolves to.
     */
    private static void verifyTagEntry(StackGroupCatalog.Entry entry) {
        String stem = entry.stem();
        boolean plate = stem.equals("plate");
        assertEquals(100, entry.priority(), "Tag group priority on " + stem);
        assertEquals("itemGroup.gt6ce." + stem, entry.name(), "Tag group name on " + stem);
        assertEquals(List.of("#gt6ce:" + stem), entry.contents(), "Tag group contents on " + stem);
        assertEquals(plate ? "STD_PLATE" : "STD", entry.exclusionsCode(), "Tag group exclusions code on " + stem);
        assertEquals(plate ? 6 : 3, entry.exclusions().size(), "Tag group exclusion count on " + stem);
    }

    /** Parses {@link StackGroupCatalog#ITEMS_RESOURCE_PATH} on its own, without the merged catalog. */
    private static Map<String, StackGroupCatalog.Entry> parseItemsResource() throws IOException {
        try (InputStream stream = openResource(StackGroupCatalog.ITEMS_RESOURCE_PATH)) {
            return StackGroupCatalog.parseItems(stream);
        }
    }

    /** Parses {@link StackGroupCatalog#REGEX_RESOURCE_PATH} on its own, without the merged catalog. */
    private static Map<String, StackGroupCatalog.Entry> parseRegexResource() throws IOException {
        try (InputStream stream = openResource(StackGroupCatalog.REGEX_RESOURCE_PATH)) {
            return StackGroupCatalog.parseRegex(stream);
        }
    }

    private static InputStream openResource(String path) throws IOException {
        InputStream stream = VirtualResourcesIntegrityTest.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Missing test resource: " + path);
        }
        return stream;
    }

    /**
     * Adds runtime-generated client pack targets referenced by virtual models or blockstates:
     * creative-tab ore icons and metal bookshelf block models built from {@code bookshelf_metal_tinted.json}.
     */
    private static void addRuntimeSynthesizedModels(Set<String> allKnownModels) {
        allKnownModels.add("block/material/none/ore");
        allKnownModels.add("block/material/none/oresmall");
        assertTrue(
                Files.isRegularFile(coreAssets().resolve("models/block/bookshelf_metal_tinted.json")),
                "Missing shared bookshelf_metal_tinted template");
        for (String stem : VirtualBlockstateCatalog.entries().keySet()) {
            if (stem.startsWith("bookshelf_metal_") && VirtualModelCatalog.contains("item/" + stem)) {
                allKnownModels.add("block/" + stem);
            }
        }
    }

    private static void verifyModelReferences(
            String label,
            Map<String, Object> obj,
            Set<String> allKnownModels,
            Set<String> shippedTextures,
            TexturePaletteTable paletteTable,
            List<String> unresolved) {
        if (obj.get("parent") instanceof String parent && parent.startsWith(NAMESPACE_PREFIX)) {
            String rel = parent.substring(NAMESPACE_PREFIX.length());
            if (!allKnownModels.contains(rel)) {
                unresolved.add(label + " -> parent " + parent);
            }
        }
        if (obj.get("textures") instanceof Map<?, ?> textures) {
            for (Object raw : textures.values()) {
                if (raw instanceof String tex && tex.startsWith(NAMESPACE_PREFIX)) {
                    String rel = tex.substring(NAMESPACE_PREFIX.length());
                    String assetPath = "textures/" + rel + ".png";
                    if (!shippedTextures.contains(assetPath) && paletteTable.entry(assetPath) == null) {
                        unresolved.add(label + " -> texture " + tex);
                    }
                }
            }
        }
        if (obj.get("overrides") instanceof List<?> overrides) {
            for (Object rawOverride : overrides) {
                if (rawOverride instanceof Map<?, ?> override
                        && override.get("model") instanceof String model
                        && model.startsWith(NAMESPACE_PREFIX)) {
                    String rel = model.substring(NAMESPACE_PREFIX.length());
                    if (!allKnownModels.contains(rel)) {
                        unresolved.add(label + " -> override " + model);
                    }
                }
            }
        }
        if (obj.get("children") instanceof Map<?, ?> children) {
            for (var child : children.entrySet()) {
                if (child.getValue() instanceof Map<?, ?> childMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> castChild = (Map<String, Object>) childMap;
                    verifyModelReferences(
                            label + "[" + child.getKey() + "]",
                            castChild,
                            allKnownModels,
                            shippedTextures,
                            paletteTable,
                            unresolved);
                }
            }
        }
    }

    private static void verifyBlockstateModels(
            String label, Map<String, Object> doc, Set<String> allKnownModels, List<String> missing) {
        if (doc.get("variants") instanceof Map<?, ?> variants) {
            for (Object raw : variants.values()) {
                collectVariantModels(label, raw, allKnownModels, missing);
            }
        }
        if (doc.get("multipart") instanceof List<?> parts) {
            for (Object rawPart : parts) {
                if (rawPart instanceof Map<?, ?> part && part.containsKey("apply")) {
                    collectVariantModels(label, part.get("apply"), allKnownModels, missing);
                }
            }
        }
    }

    private static void collectVariantModels(
            String label, Object node, Set<String> allKnownModels, List<String> missing) {
        if (node instanceof Map<?, ?> map) {
            if (map.get("model") instanceof String model && model.startsWith(NAMESPACE_PREFIX)) {
                String rel = model.substring(NAMESPACE_PREFIX.length());
                if (!allKnownModels.contains(rel)) {
                    missing.add(label + " -> " + model);
                }
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                collectVariantModels(label, item, allKnownModels, missing);
            }
        }
    }

    /**
     * Variant table of {@code kind}. The variant and model-reference records stay private to the catalog,
     * so the index invariant reads them through their accessors.
     */
    private static List<?> variantsOf(VirtualBlockstateCatalog.Kind kind) {
        try {
            Method variants = VirtualBlockstateCatalog.Kind.class.getDeclaredMethod("variants");
            variants.setAccessible(true);
            return (List<?>) variants.invoke(kind);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot read variants of " + kind, e);
        }
    }

    private static List<?> modelsOf(Object variant) {
        try {
            Method models = variant.getClass().getDeclaredMethod("models");
            models.setAccessible(true);
            return (List<?>) models.invoke(variant);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot read model references of " + variant, e);
        }
    }

    private static int modelIndexOf(Object reference) {
        try {
            Method model = reference.getClass().getDeclaredMethod("model");
            model.setAccessible(true);
            return (Integer) model.invoke(reference);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot read model index of " + reference, e);
        }
    }

    private static Map<String, String> collectStaticJson(String subdir) throws IOException {
        Map<String, String> collected = new LinkedHashMap<>();
        for (Path root : assetRoots()) {
            Path dir = root.resolve(subdir);
            for (Path file : files(dir, ".json")) {
                String rel = dir.relativize(file).toString().replace('\\', '/');
                String stem = rel.substring(0, rel.length() - ".json".length());
                assertNull(
                        collected.put(stem, Files.readString(file, StandardCharsets.UTF_8)),
                        "Duplicate static " + subdir + " across asset roots: " + stem);
            }
        }
        return collected;
    }

    private static Set<String> collectShippedTextures() throws IOException {
        Set<String> textures = new HashSet<>();
        for (Path root : assetRoots()) {
            Path texDir = root.resolve("textures");
            for (Path file : files(texDir, ".png")) {
                textures.add("textures/" + texDir.relativize(file).toString().replace('\\', '/'));
            }
        }
        return textures;
    }

    private static List<Path> files(Path directory, String suffix) throws IOException {
        List<Path> found = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return found;
        }
        try (Stream<Path> entries = Files.walk(directory)) {
            entries.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(suffix))
                    .forEach(found::add);
        }
        return found;
    }

    private static InputStream utf8Stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Asserts that {@code parse} rejects its input and that the diagnostic names the offending line and
     * duplicate key, so a collision is reported where it is found instead of at the end of the table.
     */
    private static void assertParseFailure(Executable parse, int line, String... keys) {
        IOException failure = assertThrows(IOException.class, parse);
        String message = failure.getMessage();
        assertNotNull(message, "Parser failure must carry a message");
        assertTrue(
                Pattern.compile("\\bline " + line + "\\b").matcher(message).find(),
                "Message must name line " + line + ": " + message);
        for (String key : keys) {
            assertTrue(message.contains(key), "Message must name " + key + ": " + message);
        }
    }

    private static List<Path> assetRoots() {
        return List.of(coreAssets(), minecraftAssets(), forgeAssets(), neoforgeAssets());
    }

    private static Path coreAssets() {
        return assets("gt6ce.coreAssets", "src/main/resources/assets/gt6ce");
    }

    private static Path minecraftAssets() {
        return assets("gt6ce.minecraftAssets", "../minecraft/src/main/resources/assets/gt6ce");
    }

    private static Path forgeAssets() {
        return assets("gt6ce.forgeAssets", "../minecraft/versions/1.20.1-forge/src/main/resources/assets/gt6ce");
    }

    private static Path neoforgeAssets() {
        return assets("gt6ce.neoforgeAssets", "../minecraft/versions/1.21.1-neoforge/src/main/resources/assets/gt6ce");
    }

    private static Path assets(String property, String fallback) {
        String configured = System.getProperty(property);
        return Path.of(configured == null || configured.isBlank() ? fallback : configured);
    }
}
