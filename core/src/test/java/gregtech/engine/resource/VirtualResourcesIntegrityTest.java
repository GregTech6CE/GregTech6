package gregtech.engine.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.block.stone.StoneType;
import gregtech.content.material.MaterialTestFixture;
import gregtech.engine.definition.TexturePaletteTable;
import gregtech.engine.machine.OrthogonalMachineModels;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.recipe.data.JsonMaps;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Verifies integrity, parser strictness, reference closure, and static asset budgets for the
 * virtual model, blockstate, and stack-group catalogs across all resource roots.
 */
final class VirtualResourcesIntegrityTest {
    private static final String NAMESPACE_PREFIX = GregTechIdentity.REGISTRY_NAMESPACE + ":";
    private static final String FORGE_COMPOSITE = "forge:composite";
    private static final String NEOFORGE_COMPOSITE = "neoforge:composite";
    private static final int MAX_STATIC_JSON = 450;
    private static final int EXPECTED_VIRTUAL_MODELS = 6624;
    private static final int EXPECTED_MODEL_TSV_ROWS = 4438;
    private static final int EXPECTED_VIRTUAL_BLOCKSTATES = 2276;
    private static final int EXPECTED_BLOCKSTATE_TSV_ROWS = 1630;
    private static final int EXPECTED_STACK_GROUPS = 464;
    private static final int EXPECTED_REGEX_GROUPS = 45;
    private static final int EXPECTED_TAG_GROUPS = 111;
    private static final int EXPECTED_ITEM_GROUPS = 308;

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
        assertThrows(
                IOException.class,
                () -> VirtualModelCatalog.parse(utf8Stream("path\tparent\trender_type\tao\ttextures\nbad\tonlytwo\n")));
        assertThrows(
                IOException.class,
                () -> VirtualModelCatalog.parse(utf8Stream("item/x\tminecraft:item/generated\t\tmaybe\t\n")));
        assertThrows(
                IOException.class,
                () -> VirtualModelCatalog.parse(utf8Stream("item/x\tminecraft:item/generated\t\t\tbadpair\n")));
        assertThrows(
                IOException.class,
                () -> VirtualModelCatalog.parse(utf8Stream(
                        "item/dup\tminecraft:item/generated\t\t\t\n" + "item/dup\tminecraft:item/generated\t\t\t\n")));

        assertThrows(
                IOException.class,
                () -> VirtualBlockstateCatalog.parse(utf8Stream("block\tkind\tmodels\nbad\tSINGLE\n")));
        assertThrows(
                IOException.class,
                () -> VirtualBlockstateCatalog.parse(utf8Stream("b\tUNKNOWN_KIND\tgt6ce:block/x\n")));
        assertThrows(
                IOException.class,
                () -> VirtualBlockstateCatalog.parse(utf8Stream("b\tFACING_4_MODELS\tgt6ce:block/only_one\n")));
        assertThrows(
                IOException.class,
                () -> VirtualBlockstateCatalog.parse(utf8Stream("b\tSINGLE\tgt6ce:block/a;gt6ce:block/b\n")));
        assertThrows(
                IOException.class,
                () -> VirtualBlockstateCatalog.parse(
                        utf8Stream("dup\tSINGLE\tgt6ce:block/a\ndup\tSINGLE\tgt6ce:block/b\n")));

        assertThrows(
                IOException.class,
                () -> StackGroupCatalog.parse(utf8Stream("stem\tpriority\tname\tkind\tpayload\texclusions\nbad\n")));
        assertThrows(
                IOException.class,
                () -> StackGroupCatalog.parse(utf8Stream("g\tnot_int\tName\tTAG\tgt6ce:ingots\t\n")));
        assertThrows(IOException.class, () -> StackGroupCatalog.parse(utf8Stream("g\t0\tName\tBOGUS\tpayload\t\n")));
        assertThrows(IOException.class, () -> StackGroupCatalog.parse(utf8Stream("g\t0\tName\tREGEX\t[unclosed\t\n")));
        assertThrows(
                IOException.class,
                () -> StackGroupCatalog.parse(utf8Stream("dup\t0\tA\tTAG\tgt6ce:a\t\ndup\t0\tB\tTAG\tgt6ce:b\t\n")));
    }

    @Test
    void virtualAndStaticModelsAreValidJsonNonOverlappingAndWithinBudget() throws IOException {
        Map<String, String> staticModels = collectStaticJson("models");
        Map<String, String> staticBlockstates = collectStaticJson("blockstates");
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

    @Test
    void stackGroupsAreCleanValidAndComplete() {
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
