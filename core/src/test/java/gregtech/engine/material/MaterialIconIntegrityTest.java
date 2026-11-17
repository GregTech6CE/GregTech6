package gregtech.engine.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.cover.MaterialCoverRules;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.MaterialTestFixture;
import gregtech.content.material.Materials;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards the material icon tree: every material resolves to a modeled texture set, the set
 * inheritance tree stays a forest, and every shared model agrees with the sprites its chain ships.
 */
class MaterialIconIntegrityTest {
    private static final List<String> COVER_PREFIXES = List.of(
            "plate",
            "sheetGt",
            "plateDouble",
            "plateTriple",
            "plateQuadruple",
            "plateQuintuple",
            "plateDense",
            "plateCurved",
            "plateGem",
            "foil");
    /** Sets that keep a constant so addons compile, yet own no materials, models or art. */
    private static final Set<String> UNMODELED_SETS = Set.of("fluid", "gas", "hex", "netherstar", "plasma", "rad");

    private static final List<String> ICON_KINDS = List.of("item", "block");
    private static final String PNG_SUFFIX = ".png";
    /** Sprite ids a static model names directly: {@code gt6ce:(block|item)/material_icons/<set>/<name>}. */
    private static final Pattern STATIC_ICON_REFERENCE =
            Pattern.compile("\"gt6ce:(block|item)/material_icons/([a-z0-9_]+)/([a-z0-9_]+)\"");
    /** Static hull trees the shared pack now generates; only their live models may stay on disk. */
    private static final List<String> GENERATED_MODEL_DIRECTORIES =
            List.of("crucible_bowl", "mold_basin", "crucible_mold", "crucible_crossing", "crucible_faucet");

    private static final Predicate<String> EXISTS = MaterialIconDefinitions::classpathResourceExists;
    /** Ore templates the creative-tab entries point at directly, without a material of their own. */
    private static final List<String> ORE_ICONS = List.of("ore", "oresmall");
    /** Sprite ids carry no suffixes, so the classpath probe rebuilds the resource path around them. */
    private static final Predicate<String> SPRITE_EXISTS =
            spritePath -> MaterialIconDefinitions.classpathResourceExists("textures/" + spritePath + ".png");

    @BeforeAll
    static void init() {
        MaterialTestFixture.ensureInitialized();
    }

    @Test
    void everyMaterialResolvesToAModeledTextureSet() {
        List<String> strays = new ArrayList<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            MaterialTextureSet set = MaterialIconDefinitions.resolveTextureSet(material);
            assertNotNull(set, material.getName());
            if (!MaterialTextureSet.modeled().contains(set)) {
                strays.add(material.getName() + " -> " + set);
            }
        }
        assertTrue(strays.isEmpty(), "Materials resolving to an unmodeled texture set: " + strays);
    }

    @Test
    void unmodeledSetsShipNoIconResources() throws IOException {
        assertEquals(41, MaterialTextureSet.values().length);
        assertEquals(35, MaterialTextureSet.modeled().size());
        Set<String> unmodeled = new HashSet<>();
        for (MaterialTextureSet set : MaterialTextureSet.values()) {
            if (!MaterialTextureSet.modeled().contains(set)) {
                unmodeled.add(set.folder());
            }
        }
        assertEquals(UNMODELED_SETS, unmodeled);
        List<String> leaked = new ArrayList<>();
        for (String folder : unmodeled) {
            for (String kind : ICON_KINDS) {
                Path directory = iconRoot(kind).resolve(folder);
                if (hasFiles(directory)) {
                    leaked.add(label(directory));
                }
            }
        }
        assertTrue(leaked.isEmpty(), "Unmodeled texture sets still ship icon resources: " + leaked);
    }

    @Test
    void parentTreeIsAcyclicAndModeledClosed() {
        List<String> problems = new ArrayList<>();
        Set<MaterialTextureSet> roots = EnumSet.noneOf(MaterialTextureSet.class);
        for (MaterialTextureSet set : MaterialTextureSet.values()) {
            Set<MaterialTextureSet> seen = EnumSet.noneOf(MaterialTextureSet.class);
            boolean modeled = MaterialTextureSet.modeled().contains(set);
            for (MaterialTextureSet current = set; current != null; current = current.parent()) {
                if (!seen.add(current)) {
                    problems.add(set + " revisits " + current);
                    break;
                }
                if (modeled && !MaterialTextureSet.modeled().contains(current)) {
                    problems.add(set + " inherits unmodeled " + current);
                }
            }
            if (set.parent() == null) {
                roots.add(set);
            }
        }
        assertTrue(problems.isEmpty(), "Broken icon inheritance tree: " + problems);
        assertEquals(EnumSet.of(MaterialTextureSet.DULL, MaterialTextureSet.STONE), roots);
        assertTrue(MaterialTextureSet.DULL.isRoot());
        assertTrue(MaterialTextureSet.STONE.isRoot());
        assertFalse(MaterialTextureSet.METALLIC.isRoot());
        assertEquals(MaterialTextureSet.DULL, MaterialTextureSet.METALLIC.parent());
        assertEquals(MaterialTextureSet.STONE, MaterialTextureSet.BRICK.parent());
    }

    @Test
    void modeledSetsShipNoStaticMaterialModels() throws IOException {
        List<String> leftovers = new ArrayList<>();
        leftovers.addAll(jsonFiles(coreAssets().resolve("models/item/material")));
        leftovers.addAll(jsonFiles(minecraftAssets().resolve("models/block/material")));
        assertTrue(leftovers.isEmpty(), "Static shared material models must be generated, not shipped: " + leftovers);
    }

    @Test
    void modeledSetsResolveABlockIconFolder() {
        List<String> missing = new ArrayList<>();
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            if (MaterialIconDefinitions.resolveBlockTextureOwner(set, "blocksolid", EXISTS) == null) {
                missing.add(set.folder());
            }
        }
        assertTrue(missing.isEmpty(), "Modeled sets without a resolvable block icon folder: " + missing);
    }

    @Test
    void blockIconOwnersFollowTheParentChain() {
        assertEquals(
                MaterialTextureSet.FINE, blockIconOwner(MaterialTextureSet.FINE, "molten", MaterialTextureSet.FINE));
        assertEquals(
                MaterialTextureSet.DULL,
                blockIconOwner(MaterialTextureSet.METALLIC, "molten", MaterialTextureSet.DULL));
        assertNull(blockIconOwner(MaterialTextureSet.METALLIC, "molten", null));
    }

    @Test
    void blockIconOwnersAreProbedWithSpriteIdsOnly() {
        List<String> probed = new ArrayList<>();
        String dullMolten = MaterialIconDefinitions.blockIconPath(MaterialTextureSet.DULL, "molten");
        MaterialTextureSet owner =
                MaterialIconDefinitions.resolveBlockIconOwner(MaterialTextureSet.METALLIC, "molten", spritePath -> {
                    probed.add(spritePath);
                    return spritePath.equals(dullMolten);
                });
        assertEquals(MaterialTextureSet.DULL, owner);
        // The chain is walked from the requested set towards its root, and the probe sees sprite ids.
        assertEquals(
                List.of(MaterialIconDefinitions.blockIconPath(MaterialTextureSet.METALLIC, "molten"), dullMolten),
                probed);
        for (String spritePath : probed) {
            assertFalse(spritePath.startsWith("textures/"), spritePath);
            assertFalse(spritePath.endsWith(".png"), spritePath);
        }
    }

    @Test
    void everyModeledSetResolvesEveryBlockIcon() {
        List<String> missing = new ArrayList<>();
        List<String> spriteNames = builtInBlockIconFiles();
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            for (String spriteName : spriteNames) {
                if (MaterialIconDefinitions.resolveBlockIconOwner(set, spriteName, SPRITE_EXISTS) == null) {
                    missing.add(set.folder() + "/" + spriteName);
                }
            }
        }
        assertTrue(missing.isEmpty(), "Modeled sets without a resolvable block icon sprite: " + missing);
    }

    @Test
    void sharedItemModelsResolveTheirIconSprites() {
        List<String> missing = new ArrayList<>();
        List<String> mismatched = new ArrayList<>();
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            for (MaterialPrefix prefix : MaterialItemDefinitions.candidatePrefixes()) {
                String label = set.folder() + "/" + prefix.getTextureFileName();
                MaterialTextureSet baseOwner =
                        MaterialIconDefinitions.resolveItemTextureOwner(set, prefix, false, EXISTS);
                if (baseOwner == null) {
                    missing.add(label);
                    continue;
                }
                MaterialTextureSet overlayOwner =
                        MaterialIconDefinitions.resolveItemTextureOwner(set, prefix, true, EXISTS);
                String model = MaterialIconDefinitions.sharedModelJson(set, prefix, EXISTS);
                if (model == null) {
                    missing.add(label);
                    continue;
                }
                String base = "\"gt6ce:" + MaterialIconDefinitions.texturePath(baseOwner, prefix, false) + "\"";
                if (!model.contains(base)) {
                    mismatched.add(label + " layer0 does not name " + base);
                }
                boolean declaresOverlay = model.contains("\"layer1\"");
                if (declaresOverlay != (overlayOwner != null)) {
                    mismatched.add(label + " layer1=" + declaresOverlay + " overlay=" + (overlayOwner != null));
                } else if (overlayOwner != null
                        && !model.contains(
                                "\"gt6ce:" + MaterialIconDefinitions.texturePath(overlayOwner, prefix, true) + "\"")) {
                    mismatched.add(label + " layer1 does not name its overlay sprite");
                }
                if (!model.contains("\"parent\": \"minecraft:item/generated\"")) {
                    mismatched.add(label + " lost the item/generated parent");
                }
            }
        }
        assertTrue(missing.isEmpty(), "Material icons nothing in the chain ships: " + missing);
        assertTrue(mismatched.isEmpty(), "Shared model and inherited sprites disagree: " + mismatched);
    }

    @Test
    void coverDesignsResolveTheirIconSprites() {
        List<String> missing = new ArrayList<>();
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            for (String prefix : COVER_PREFIXES) {
                for (String icon : MaterialCoverRules.textures(prefix)) {
                    if (MaterialIconDefinitions.resolveBlockTextureOwner(set, icon, EXISTS) == null) {
                        missing.add(set.folder() + "/" + icon);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Cover designs without a block icon sprite: " + missing);
    }

    @Test
    void noOrphanMcmetaOrDuplicateTexturesAlongParentChain() throws IOException {
        Map<String, String> sprites = new HashMap<>();
        Map<String, String> metadata = new HashMap<>();
        List<String> problems = new ArrayList<>();
        for (String kind : ICON_KINDS) {
            for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
                Path directory = iconRoot(kind).resolve(set.folder());
                if (!Files.isDirectory(directory)) {
                    continue;
                }
                try (Stream<Path> entries = Files.list(directory)) {
                    for (Path entry : (Iterable<Path>) entries::iterator) {
                        String fileName = entry.getFileName().toString();
                        if (fileName.endsWith(".mcmeta")) {
                            if (!Files.isRegularFile(entry.resolveSibling(fileName.replace(".mcmeta", "")))) {
                                problems.add("orphan metadata " + label(entry));
                            }
                            continue;
                        }
                        if (!fileName.endsWith(".png")) {
                            continue;
                        }
                        String key = kind + "/" + set.folder() + "/" + fileName;
                        sprites.put(key, fingerprint(entry));
                        Path mcmeta = entry.resolveSibling(fileName + ".mcmeta");
                        if (Files.isRegularFile(mcmeta)) {
                            metadata.put(key, fingerprint(mcmeta));
                        }
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), "Broken icon folders: " + problems);

        // A static model may keep its own copy of a sprite an ancestor also ships: it names that
        // file directly and every such file is checked by staticModelsOnlyNameShippedSprites.
        Set<String> overridden = staticMaterialIconReferences();
        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<String, String> sprite : sprites.entrySet()) {
            String[] key = sprite.getKey().split("/");
            if (overridden.contains(spriteId(key))) {
                continue;
            }
            MaterialTextureSet set = MaterialTextureSet.valueOf(key[1].toUpperCase(Locale.ROOT));
            for (MaterialTextureSet ancestor = set.parent(); ancestor != null; ancestor = ancestor.parent()) {
                String candidate = key[0] + "/" + ancestor.folder() + "/" + key[2];
                if (!sprites.containsKey(candidate)) {
                    continue;
                }
                if (sprites.get(candidate).equals(sprite.getValue())
                        && Objects.equals(metadata.get(candidate), metadata.get(sprite.getKey()))) {
                    duplicates.add(sprite.getKey() + " duplicates " + candidate);
                }
                break;
            }
        }
        assertTrue(duplicates.isEmpty(), "Icons repeat what an ancestor already ships: " + duplicates);
    }

    @Test
    void staticModelsOnlyNameShippedSprites() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String sprite : staticMaterialIconReferences()) {
            if (!Files.isRegularFile(spriteFile(sprite))) {
                missing.add(sprite);
            }
        }
        assertTrue(missing.isEmpty(), "Static models name sprites nothing ships: " + missing);
    }

    @Test
    void smelteryHullsAndMetalShelvesAreGeneratedNotShipped() throws IOException {
        Path template = coreAssets().resolve("models/block/bookshelf_metal_tinted.json");
        assertTrue(Files.isRegularFile(template), "Missing shared metal shelf template " + label(template));
        List<String> shelves = new ArrayList<>();
        try (Stream<Path> entries = Files.list(template.getParent())) {
            entries.forEach(path -> {
                String name = path.getFileName().toString();
                if (name.startsWith("bookshelf_metal_")
                        && !name.equals(template.getFileName().toString())) {
                    shelves.add(label(path));
                }
            });
        }
        assertTrue(shelves.isEmpty(), "Metal shelf models are generated at runtime: " + shelves);
        List<String> hulls = new ArrayList<>();
        for (String family : GENERATED_MODEL_DIRECTORIES) {
            hulls.addAll(
                    jsonFiles(minecraftAssets().resolve("models/block/machine").resolve(family)));
        }
        assertTrue(hulls.isEmpty(), "Smeltery hull models are generated at runtime: " + hulls);
    }

    @Test
    void materialProbeStopsFallingBackToClasspathOnceMounted() {
        String metallicIngot = "textures/"
                + MaterialIconDefinitions.texturePath(MaterialTextureSet.METALLIC, MaterialPrefix.ingot, false)
                + ".png";
        String dullIngot = "textures/"
                + MaterialIconDefinitions.texturePath(MaterialTextureSet.DULL, MaterialPrefix.ingot, false)
                + ".png";
        assertTrue(MaterialIconDefinitions.classpathResourceExists(dullIngot));

        Predicate<String> mountedProbe = MaterialIconDefinitions.selectMaterialProbe(
                Set.of(GregTechIdentity.REGISTRY_NAMESPACE), path -> path.equals(metallicIngot));
        assertEquals(
                MaterialTextureSet.METALLIC,
                MaterialIconDefinitions.resolveItemTextureOwner(
                        MaterialTextureSet.SHINY, MaterialPrefix.ingot, false, mountedProbe));

        Predicate<String> filteredAllProbe =
                MaterialIconDefinitions.selectMaterialProbe(Set.of(GregTechIdentity.REGISTRY_NAMESPACE), path -> false);
        assertFalse(filteredAllProbe.test(dullIngot));
        assertNull(MaterialIconDefinitions.resolveItemTextureOwner(
                MaterialTextureSet.SHINY, MaterialPrefix.ingot, false, filteredAllProbe));

        Predicate<String> unmountedProbe = MaterialIconDefinitions.selectMaterialProbe(Set.of(), path -> false);
        assertTrue(unmountedProbe.test(dullIngot));
        assertNotNull(MaterialIconDefinitions.resolveItemTextureOwner(
                MaterialTextureSet.SHINY, MaterialPrefix.ingot, false, unmountedProbe));
    }

    @Test
    void bottleCrateCasingResolutionHonorsResourcePackOverrideAndFilter() {
        MaterialTextureSet steelSet = MaterialIconDefinitions.resolveTextureSet(Materials.Steel);
        MaterialTextureSet leadSet = MaterialIconDefinitions.resolveTextureSet(Materials.Lead);
        assertEquals(MaterialTextureSet.METALLIC, steelSet);
        assertEquals(MaterialTextureSet.DULL, leadSet);

        String casingFile = BlockMaterialPrefix.casingMachine.getTextureFileName();
        String dullCasing =
                "textures/" + MaterialIconDefinitions.blockIconPath(MaterialTextureSet.DULL, casingFile) + ".png";
        String metallicCasing =
                "textures/" + MaterialIconDefinitions.blockIconPath(MaterialTextureSet.METALLIC, casingFile) + ".png";

        Set<String> dullOnly = Set.of(dullCasing);
        Predicate<String> dullOnlyProbe = MaterialIconDefinitions.selectMaterialProbe(
                Set.of(GregTechIdentity.REGISTRY_NAMESPACE), dullOnly::contains);
        assertEquals(
                MaterialTextureSet.DULL,
                MaterialIconDefinitions.resolveBlockTextureOwner(steelSet, casingFile, dullOnlyProbe));
        assertEquals(
                MaterialTextureSet.DULL,
                MaterialIconDefinitions.resolveBlockTextureOwner(leadSet, casingFile, dullOnlyProbe));

        Set<String> overridden = Set.of(dullCasing, metallicCasing);
        Predicate<String> overrideProbe = MaterialIconDefinitions.selectMaterialProbe(
                Set.of(GregTechIdentity.REGISTRY_NAMESPACE), overridden::contains);
        assertEquals(
                MaterialTextureSet.METALLIC,
                MaterialIconDefinitions.resolveBlockTextureOwner(steelSet, casingFile, overrideProbe));
        assertEquals(
                MaterialTextureSet.DULL,
                MaterialIconDefinitions.resolveBlockTextureOwner(leadSet, casingFile, overrideProbe));

        Predicate<String> filteredAllProbe =
                MaterialIconDefinitions.selectMaterialProbe(Set.of(GregTechIdentity.REGISTRY_NAMESPACE), path -> false);
        assertNull(MaterialIconDefinitions.resolveBlockTextureOwner(steelSet, casingFile, filteredAllProbe));
        assertNull(MaterialIconDefinitions.resolveBlockTextureOwner(leadSet, casingFile, filteredAllProbe));
    }

    private static Path iconRoot(String kind) {
        return coreAssets().resolve("textures/" + kind + "/material_icons");
    }

    /** File a {@code kind/set/name} sprite id resolves to in the core module. */
    private static Path spriteFile(String sprite) {
        String[] parts = sprite.split("/");
        return iconRoot(parts[0]).resolve(parts[1]).resolve(parts[2] + PNG_SUFFIX);
    }

    /** Sprite id of a {@code kind/set/name.png} texture key. */
    private static String spriteId(String[] key) {
        return key[0] + "/" + key[1] + "/" + key[2].substring(0, key[2].length() - PNG_SUFFIX.length());
    }

    /**
     * Sprite ids the static models name directly, so a file an ancestor also ships cannot simply be
     * dropped: the model would point at art nothing mounts.
     */
    private static Set<String> staticMaterialIconReferences() throws IOException {
        Set<String> references = new LinkedHashSet<>();
        for (Path root :
                List.of(coreAssets().resolve("models"), minecraftAssets().resolve("models"))) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> entries = Files.walk(root)) {
                for (Path path :
                        (Iterable<Path>) entries.filter(file -> file.toString().endsWith(".json"))::iterator) {
                    Matcher matcher = STATIC_ICON_REFERENCE.matcher(Files.readString(path));
                    while (matcher.find()) {
                        references.add(matcher.group(1) + "/" + matcher.group(2) + "/" + matcher.group(3));
                    }
                }
            }
        }
        return references;
    }

    /**
     * Every block sprite name a modeled set has to ship or inherit: the built-in prefixes,
     * plus the ore templates the creative-tab entries name directly.
     */
    private static List<String> builtInBlockIconFiles() {
        Set<String> spriteNames = new LinkedHashSet<>();
        for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
            if (!prefix.isAddon()) {
                spriteNames.add(prefix.getTextureFileName());
            }
        }
        spriteNames.addAll(ORE_ICONS);
        return List.copyOf(spriteNames);
    }

    /** Owner when only {@code shipping} ships {@code fileName}; a {@code null} shipping allows no sprite. */
    private static MaterialTextureSet blockIconOwner(
            MaterialTextureSet set, String fileName, MaterialTextureSet shipping) {
        String only = shipping == null ? null : MaterialIconDefinitions.blockIconPath(shipping, fileName);
        return MaterialIconDefinitions.resolveBlockIconOwner(set, fileName, spritePath -> spritePath.equals(only));
    }

    private static boolean hasFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.findAny().isPresent();
        }
    }

    private static List<String> jsonFiles(Path directory) throws IOException {
        List<String> found = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return found;
        }
        try (Stream<Path> entries = Files.walk(directory)) {
            entries.filter(path -> path.toString().endsWith(".json")).forEach(path -> found.add(label(path)));
        }
        return found;
    }

    private static String fingerprint(Path file) throws IOException {
        if (file.getFileName().toString().endsWith(".mcmeta")) {
            return Integer.toHexString(Arrays.hashCode(Files.readAllBytes(file)));
        }
        var image = ImageIO.read(file.toFile());
        if (image == null) {
            throw new IOException("unreadable image " + file);
        }
        int[] argb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        return image.getWidth() + "x" + image.getHeight() + ":" + Arrays.hashCode(argb);
    }

    private static String label(Path path) {
        return path.toString().replace('\\', '/');
    }

    // Resolves the core asset root from Gradle or the module directory.
    private static Path coreAssets() {
        String configured = System.getProperty("gt6ce.coreAssets");
        return Path.of(configured == null || configured.isBlank() ? "src/main/resources/assets/gt6ce" : configured);
    }

    // Resolves the shared block-model assets, which live in the client module.
    private static Path minecraftAssets() {
        String configured = System.getProperty("gt6ce.minecraftAssets");
        return Path.of(
                configured == null || configured.isBlank()
                        ? "../minecraft/src/main/resources/assets/gt6ce"
                        : configured);
    }
}
