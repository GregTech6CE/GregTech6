package gregtech.engine.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.engine.mod.GregTechIdentity;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Guards {@code machine_models.tsv} and the merged machine art.
 *
 * <p>The table replaced one static model per machine state, and the cleanup merged duplicated
 * sprites into {@code casing/} and {@code shared_overlay/}. Both are easy to break silently: a
 * missing row leaves a machine untextured, a stale sprite path points at a file the merge moved,
 * and a leftover static model shadows the row the loader generates.
 */
class MachineModelIntegrityTest {
    /** Rows in the shipped table: one per live orthogonal machine model. */
    private static final int TABLE_ROWS = 842;
    /**
     * The machine art budget. The client builds some sprite paths itself, so those files keep
     * their own names and cannot be merged; with that pinned set (including {@code endergarbage/bin/overlay/}
     * and the 45 restored original cover textures) the merged folder stays inside the 995 target.
     */
    private static final int MAX_MACHINE_PNGS = 995;

    private static final int TABLE_COLUMNS = 15;
    private static final String TABLE = "machine_models.tsv";
    private static final String MACHINE_TEXTURES = "textures/block/machines";
    /** Prefixes the client resolves by concatenation or by enumerating the directory. */
    private static final List<String> PINNED_PREFIXES = List.of(
            "covers/",
            "barometer/",
            "batteries/",
            "metalchest/",
            "woodchest/",
            "lootchest/",
            "multiblockmains/turbine",
            "multiblockmains/crucible/");

    private static Map<String, Sprite> sprites;

    @Test
    void tableLoadsEveryRowInAFixedShape() throws IOException {
        Map<String, OrthogonalMachineModels.Entry> rows = OrthogonalMachineModels.all();
        assertFalse(rows.isEmpty(), "machine_models.tsv is missing from the core resources");
        assertEquals(TABLE_ROWS, rows.size());

        List<String> problems = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int number = 0;
        for (String line : Files.readAllLines(coreAssets().resolve(TABLE))) {
            number++;
            if (line.isBlank() || line.strip().startsWith("#")) {
                continue;
            }
            String[] columns = line.split("\t", -1);
            if (columns.length != TABLE_COLUMNS) {
                problems.add("line " + number + " has " + columns.length + " columns");
                continue;
            }
            String modelPath = columns[0];
            if (!seen.add(modelPath)) {
                problems.add("line " + number + " repeats " + modelPath);
            }
            if (!columns[1].equals("0") && !columns[1].equals("1")) {
                problems.add("line " + number + " has solidBase=" + columns[1]);
            }
            OrthogonalMachineModels.Entry row = rows.get(modelPath);
            if (row == null) {
                problems.add("line " + number + " names an unloaded row " + modelPath);
                continue;
            }
            if (!row.hasOverlay() && row.baseFace(0) == null) {
                problems.add(modelPath + " draws nothing");
            }
        }
        assertTrue(problems.isEmpty(), "Malformed machine_models.tsv: " + problems);
        assertEquals(seen, rows.keySet(), "Rows the parser skipped or invented instead of reading the file");
    }

    @Test
    void legacyMachineTexturesRespectPackPriority() {
        var autoclave = OrthogonalMachineModels.get("block/machine/basic/autoclave");
        assertNotNull(autoclave);
        assertNull(autoclave.overlayFace(1));
        assertSame(autoclave, OrthogonalMachineModels.resolveLegacyTextures(autoclave, path -> -1));

        String oldFront = "basicmachines/autoclave/colored/front";
        String oldTop = "basicmachines/autoclave/overlay/top";
        Map<String, Integer> packs = Map.of("casing/hull", 1, oldFront, 1, oldTop, 0);
        var compatible = OrthogonalMachineModels.resolveLegacyTextures(autoclave, path -> packs.getOrDefault(path, -1));
        assertEquals(oldFront, compatible.baseFace(2));
        assertEquals("casing/hull", compatible.baseFace(1));
        assertEquals(oldTop, compatible.overlayFace(1));

        Map<String, Integer> newerPack = Map.of("casing/hull", 2, oldFront, 1);
        assertSame(
                autoclave,
                OrthogonalMachineModels.resolveLegacyTextures(autoclave, path -> newerPack.getOrDefault(path, -1)));
    }

    @Test
    void legacyShellKeepsItsCutoutLayer() {
        var laser = OrthogonalMachineModels.get("block/machine/energy/co2_laser");
        assertNotNull(laser);
        assertTrue(laser.solidBase());
        String oldFront = "lasers/laser_electric/colored/front";
        var compatible = OrthogonalMachineModels.resolveLegacyTextures(laser, path -> path.equals(oldFront) ? 1 : -1);
        assertEquals(oldFront, compatible.baseFace(2));
        assertFalse(compatible.solidBase());
    }

    @Test
    void everyRowSpriteExistsAndStaysOpaque() {
        List<String> missing = new ArrayList<>();
        List<String> invisible = new ArrayList<>();
        List<String> foreign = new ArrayList<>();
        for (OrthogonalMachineModels.Entry row : OrthogonalMachineModels.all().values()) {
            List<String> specs = new ArrayList<>();
            specs.add(row.particle());
            for (int face = 0; face < 6; face++) {
                specs.add(row.baseFace(face));
                specs.add(row.overlayFace(face));
            }
            for (String spec : specs) {
                if (spec == null) {
                    continue;
                }
                String relative = relative(spec);
                if (relative == null) {
                    // The merged art lives in this repository's namespace, so a row cannot borrow
                    // another pack's folder: those files ship with that pack, not here.
                    foreign.add(row.modelPath() + " -> " + spec);
                    continue;
                }
                Sprite sprite = sprites().get(relative);
                if (sprite == null) {
                    missing.add(row.modelPath() + " -> " + spec);
                } else if (sprite.transparent()) {
                    invisible.add(row.modelPath() + " -> " + spec);
                }
            }
        }
        assertTrue(
                foreign.isEmpty(),
                "Rows naming sprites outside " + GregTechIdentity.REGISTRY_NAMESPACE + ": " + foreign);
        assertTrue(missing.isEmpty(), "Rows naming sprites the folder does not ship: " + missing);
        assertTrue(invisible.isEmpty(), "Rows naming fully transparent sprites: " + invisible);
    }

    @Test
    void machineArtStaysInsideItsFileBudget() {
        List<String> transparent = new ArrayList<>();
        for (Sprite sprite : sprites().values()) {
            if (sprite.transparent()) {
                transparent.add(sprite.relative());
            }
        }
        assertTrue(
                sprites().size() <= MAX_MACHINE_PNGS,
                "Machine art grew to " + sprites().size() + " files, over the " + MAX_MACHINE_PNGS + " budget");
        assertTrue(transparent.isEmpty(), "Fully transparent machine sprites: " + transparent);
    }

    @Test
    void everyMcmetaBelongsToASprite() throws IOException {
        List<String> orphans = new ArrayList<>();
        for (Path file : files(textureRoot(), ".mcmeta")) {
            String name = file.getFileName().toString();
            String png = name.substring(0, name.length() - ".mcmeta".length());
            if (!Files.isRegularFile(file.resolveSibling(png))) {
                orphans.add(label(file));
            }
        }
        assertTrue(orphans.isEmpty(), "Metadata without its sprite: " + orphans);
    }

    @Test
    void noStaticModelShadowsATableRow() throws IOException {
        List<String> shadows = new ArrayList<>();
        for (Path root : assetRoots()) {
            Path machine = root.resolve("models/block/machine");
            if (!Files.isDirectory(machine)) {
                continue;
            }
            for (Path file : files(machine, ".json")) {
                String modelPath =
                        "block/machine/" + machine.relativize(file).toString().replace('\\', '/');
                modelPath = modelPath.substring(0, modelPath.length() - ".json".length());
                if (OrthogonalMachineModels.contains(modelPath)) {
                    shadows.add(label(file));
                }
            }
        }
        assertTrue(shadows.isEmpty(), "Static models the table already generates: " + shadows);
    }

    @Test
    void noMergeableDuplicateSurvivesOutsidePinnedDirectories() throws IOException {
        Map<String, List<Sprite>> classes = new HashMap<>();
        for (Sprite sprite : sprites().values()) {
            classes.computeIfAbsent(sprite.fingerprint(), key -> new ArrayList<>())
                    .add(sprite);
        }
        List<String> duplicates = new ArrayList<>();
        for (List<Sprite> members : classes.values()) {
            List<String> free = new ArrayList<>();
            for (Sprite member : members) {
                if (!pinned(member.relative())) {
                    free.add(member.relative());
                }
            }
            if (free.size() > 1) {
                duplicates.add(String.join(" == ", free));
            }
        }
        assertTrue(duplicates.isEmpty(), "Identical machine sprites the merge left behind: " + duplicates);
    }

    private static boolean pinned(String relative) {
        for (String prefix : PINNED_PREFIXES) {
            if (relative.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code textures/block/machines/<spec>.png} for a table cell, or null when the cell names
     * another namespace: the merged folder only ships sprites of this repository's namespace.
     */
    private static String relative(String spec) {
        String path = OrthogonalMachineModels.fullTexturePath(spec);
        int separator = path.indexOf(':');
        if (separator >= 0 && !path.substring(0, separator).equals(GregTechIdentity.REGISTRY_NAMESPACE)) {
            return null;
        }
        return "textures/" + path.substring(separator + 1) + ".png";
    }

    /** Every sprite under the machine texture folder, decoded once per test run. */
    private static Map<String, Sprite> sprites() {
        if (sprites != null) {
            return sprites;
        }
        Map<String, Sprite> loaded = new LinkedHashMap<>();
        try {
            for (Path file : files(textureRoot(), ".png")) {
                String relative = textureRoot().relativize(file).toString().replace('\\', '/');
                loaded.put(MACHINE_TEXTURES + "/" + relative, sprite(relative, file));
            }
        } catch (IOException e) {
            throw new AssertionError("Cannot read the machine texture folder", e);
        }
        sprites = loaded;
        return sprites;
    }

    private static Sprite sprite(String relative, Path file) throws IOException {
        var image = ImageIO.read(file.toFile());
        assertNotNull(image, "unreadable sprite " + label(file));
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        boolean transparent = true;
        for (int pixel : pixels) {
            transparent &= (pixel >>> 24) == 0;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(ByteBuffer.allocate(8).putInt(width).putInt(height).array());
            ByteBuffer decoded = ByteBuffer.allocate(pixels.length * 4);
            decoded.asIntBuffer().put(pixels);
            digest.update(decoded.array());
            // An animated sprite's frames only mean something together with its metadata.
            Path meta = file.resolveSibling(file.getFileName() + ".mcmeta");
            byte[] metadata = Files.isRegularFile(meta) ? Files.readAllBytes(meta) : new byte[0];
            digest.update(metadata);
            byte[] hash = digest.digest();
            StringBuilder text = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                text.append(Character.forDigit((value >> 4) & 0xF, 16)).append(Character.forDigit(value & 0xF, 16));
            }
            return new Sprite(relative, transparent, text.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is missing from the runtime", e);
        }
    }

    /**
     * One machine sprite: its folder path, whether every pixel is invisible, and a digest of the
     * decoded pixels plus its metadata.
     */
    private record Sprite(String relative, boolean transparent, String fingerprint) {}

    private static Path textureRoot() {
        return coreAssets().resolve(MACHINE_TEXTURES);
    }

    private static List<Path> assetRoots() {
        return List.of(coreAssets(), minecraftAssets(), forgeAssets(), neoforgeAssets());
    }

    private static List<Path> files(Path directory, String suffix) throws IOException {
        List<Path> found = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return found;
        }
        try (Stream<Path> entries = Files.walk(directory)) {
            entries.filter(path -> path.toString().endsWith(suffix)).forEach(found::add);
        }
        return found;
    }

    private static String label(Path path) {
        return path.toString().replace('\\', '/');
    }

    // Asset roots come from Gradle; the module directory is the fallback for a plain IDE run.
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
