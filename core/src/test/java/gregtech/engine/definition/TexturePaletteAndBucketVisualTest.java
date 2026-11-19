package gregtech.engine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.data.FluidCatalog;
import gregtech.content.fluid.FluidDefinitions;
import gregtech.content.material.MaterialTestFixture;
import gregtech.content.tool.WoodenBucketCatalog;
import gregtech.engine.recipe.data.JsonMaps;
import gregtech.engine.tool.WoodenBucketVisuals;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link TexturePaletteTable} rules, pixel CRC32 hashes against pre-compression outputs,
 * model texture resolution, retired fluid exclusions, and layered {@link WoodenBucketVisuals}.
 */
final class TexturePaletteAndBucketVisualTest {
    private static final String HASH_RESOURCE = "/gregtech/texture-palette-hashes.tsv";
    private static final Set<String> RETIRED_FLUID_TEXTURES = Set.of(
            "beer",
            "binnie.juiceapricot",
            "binnie.juicecherry",
            "binnie.juiceelderberry",
            "binnie.juicegrapefruit",
            "binnie.juicelime",
            "binnie.juiceorange",
            "binnie.juicepeach",
            "binnie.juicepear",
            "binnie.juiceplum",
            "binnie.juiceredgrape",
            "binnie.juicewhitegrape",
            "binnie.mashcorn",
            "binnie.mashgrain",
            "binnie.mashrye",
            "binnie.mashwheat",
            "binnie.whiskeywheat",
            "binnie.winered",
            "binnie.winewhite",
            "chocolatemilk",
            "ethanol",
            "for.honey",
            "for.honeydew",
            "glenmckenner",
            "grc.honey",
            "grcmilk.milk",
            "heliumplasma",
            "ic2biomass",
            "ic2uumatter",
            "indigo",
            "kerosene",
            "lithium",
            "molten.latex",
            "nitrogenplasma",
            "peanutbutter",
            "petrotheum",
            "plastic",
            "potion",
            "potion.saltywater",
            "pyrotheum",
            "ricevinegar",
            "ricewater",
            "uuamplifier",
            "whaleoil",
            "wheathopsmash");

    private static Path coreAssets() {
        String configured = System.getProperty("gt6ce.coreAssets");
        if (configured != null && !configured.isBlank()) return Path.of(configured);
        return Path.of("src/main/resources/assets/gt6ce");
    }

    @Test
    void texturePaletteTemplatesAndRulesAreValid() throws IOException {
        TexturePaletteTable table = TexturePaletteTable.load();
        assertFalse(table.entries().isEmpty(), "texture_palettes.tsv must define entries");

        Path assets = coreAssets();
        Set<String> synthesizedPaths = table.entries().stream()
                .map(TexturePaletteTable.Entry::assetPath)
                .collect(Collectors.toSet());
        Set<String> checkedTemplates = new HashSet<>();
        for (var entry : table.entries()) {
            assertFalse(
                    synthesizedPaths.contains(entry.templatePath()),
                    "Template must not itself be synthesized: " + entry.templatePath());
            Path targetOnDisk = assets.resolve(entry.assetPath());
            assertFalse(Files.exists(targetOnDisk), "Synthesized PNG should not ship on disk: " + targetOnDisk);

            if (checkedTemplates.add(entry.templatePath())) {
                Path templateOnDisk = assets.resolve(entry.templatePath());
                assertTrue(Files.isRegularFile(templateOnDisk), "Missing template PNG: " + templateOnDisk);
                try (InputStream in = Files.newInputStream(templateOnDisk)) {
                    BufferedImage img = ImageIO.read(in);
                    assertNotNull(img, "Unreadable template PNG: " + templateOnDisk);
                    assertTrue(img.getWidth() > 0 && img.getHeight() > 0);
                }
            }
            if (entry.mcmetaId() != null) {
                byte[] mcmeta = table.mcmetaBytes(entry.assetPath() + ".mcmeta");
                assertNotNull(mcmeta, "Missing mcmeta bytes for " + entry.assetPath());
                String json = new String(mcmeta, StandardCharsets.UTF_8);
                assertTrue(json.contains("\"animation\""), "Fluid mcmeta must declare animation: " + entry.assetPath());
            }
        }
    }

    @Test
    void allSynthesizedTexturesMatchBaselinePixelHashes() throws IOException {
        TexturePaletteTable table = TexturePaletteTable.load();
        Map<String, String> expected = new HashMap<>();
        try (InputStream in = TexturePaletteAndBucketVisualTest.class.getResourceAsStream(HASH_RESOURCE)) {
            assertNotNull(in, "Missing baseline hash table: " + HASH_RESOURCE);
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\t");
                assertEquals(3, parts.length, "Malformed hash row: " + line);
                expected.put(parts[0], parts[1] + "\t" + parts[2]);
            }
        }
        assertEquals(expected.size(), table.entries().size(), "Baseline hash count must match palette table");

        Path assets = coreAssets();
        Map<String, BufferedImage> templateCache = new HashMap<>();
        for (var entry : table.entries()) {
            BufferedImage tpl = templateCache.get(entry.templatePath());
            if (tpl == null) {
                try (InputStream in = Files.newInputStream(assets.resolve(entry.templatePath()))) {
                    tpl = ImageIO.read(in);
                }
                assertNotNull(tpl);
                templateCache.put(entry.templatePath(), tpl);
            }
            int w = tpl.getWidth();
            int h = tpl.getHeight();
            ByteBuffer buf = ByteBuffer.allocate(8 + w * h * 4);
            buf.putInt(w).putInt(h);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = tpl.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    int r = (argb >>> 16) & 0xFF;
                    int g = (argb >>> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                    int mapped = entry.mapAbgr(abgr);
                    if ((mapped >>> 24) == 0) mapped = 0;
                    buf.putInt(mapped);
                }
            }
            CRC32 crc = new CRC32();
            crc.update(buf.array());
            String actual = w + "x" + h + "\t" + String.format("%08x", crc.getValue());
            assertEquals(expected.get(entry.assetPath()), actual, "Pixel hash mismatch for " + entry.assetPath());
        }
    }

    @Test
    void bottleCanAndFluidModelTexturesResolve() throws IOException {
        TexturePaletteTable table = TexturePaletteTable.load();
        Path assets = coreAssets();
        try (var stream = Files.walk(assets.resolve("models"))) {
            for (Path modelFile :
                    stream.filter(p -> p.toString().endsWith(".json")).toList()) {
                Map<String, Object> root = JsonMaps.object(Files.readString(modelFile, StandardCharsets.UTF_8));
                Object texObj = root.get("textures");
                if (!(texObj instanceof Map<?, ?> textures)) continue;
                for (Object rawVal : textures.values()) {
                    if (!(rawVal instanceof String val) || !val.startsWith("gt6ce:")) continue;
                    String rel = val.substring("gt6ce:".length());
                    if (rel.startsWith("item/bottles/")
                            || rel.startsWith("item/cans/")
                            || rel.startsWith("block/fluids/")) {
                        String texPath = "textures/" + rel + ".png";
                        assertTrue(
                                Files.isRegularFile(assets.resolve(texPath)) || table.entry(texPath) != null,
                                "Unresolved container/fluid texture " + texPath + " referenced in " + modelFile);
                    }
                }
            }
        }
        for (String group : List.of(
                "stack_groups/material_forms/functional_drinks.json",
                "stack_groups/material_forms/functional_chemical_bottles.json")) {
            Map<String, Object> root = JsonMaps.object(Files.readString(assets.resolve(group), StandardCharsets.UTF_8));
            assertTrue(root.get("contents") instanceof List<?>);
            for (Object itemObj : (List<?>) root.get("contents")) {
                String id = ((String) itemObj).replace("item:gt6ce:", "");
                assertTrue(
                        Files.isRegularFile(assets.resolve("models/item/" + id + ".json")),
                        "Missing item model for stack group entry " + id);
            }
        }
    }

    @Test
    void texturePaletteTableParseSupportsAnyRowOrderAndRejectsBadHex() throws IOException {
        String reversed = """
                tex\ttextures/block/fluids/test.png\ttextures/block/fluids/steam.png\tm2\tff000000:ff112233
                mcmeta\tm2\t{"animation":{"frametime":2}}
                """;
        TexturePaletteTable parsed =
                TexturePaletteTable.parse(new ByteArrayInputStream(reversed.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(parsed.mcmetaBytes("textures/block/fluids/test.png.mcmeta"));
        assertEquals(0xFF112233, parsed.entry("textures/block/fluids/test.png").mapAbgr(0xFF000000));

        String badHex = "tex\ttextures/block/fluids/bad.png\ttextures/block/fluids/steam.png\t-\tzzzzzzzz:ff000000\n";
        assertThrows(
                IOException.class,
                () -> TexturePaletteTable.parse(new ByteArrayInputStream(badHex.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void retiredFluidTexturesDoNotMatchRegisteredFluids() {
        MaterialTestFixture.ensureInitialized();
        FluidDefinitions.prepare();
        Set<String> liveTextureIds = new HashSet<>();
        for (var entry : FluidCatalog.all().values()) {
            liveTextureIds.add(FluidCatalog.sanitizeTextureId(entry.registryName()));
        }
        for (String retired : RETIRED_FLUID_TEXTURES) {
            assertFalse(
                    liveTextureIds.contains(retired), "Retired fluid texture id is still in FluidCatalog: " + retired);
        }
    }

    @Test
    void woodenBucketLayeredModelsAndAssetsAreConsistent() throws IOException {
        MaterialTestFixture.ensureInitialized();
        Path assets = coreAssets();
        Path bucketTextures = assets.resolve("textures/item/wooden_buckets");
        List<String> expectedPngs = List.of(
                "wooden_bucket_base.png",
                "wooden_bucket_band_overlay.png",
                "wooden_bucket_liquid_overlay.png",
                "wooden_bucket_liquid_rainbow_sap.png");
        for (String name : expectedPngs) {
            Path p = bucketTextures.resolve(name);
            assertTrue(Files.isRegularFile(p), "Missing layered bucket texture: " + p);
            try (InputStream in = Files.newInputStream(p)) {
                BufferedImage img = ImageIO.read(in);
                assertNotNull(img);
                assertEquals(16, img.getWidth());
                assertEquals(16, img.getHeight());
            }
        }
        try (var stream = Files.list(bucketTextures)) {
            long count = stream.filter(p -> p.getFileName().toString().endsWith(".png"))
                    .count();
            assertEquals(expectedPngs.size(), count, "Only 4 layered wooden bucket PNGs should remain");
        }
        for (var band : WoodenBucketCatalog.BANDS) {
            int bandColor = WoodenBucketVisuals.bandRgb(band);
            assertNotEquals(
                    0xFFFFFF, bandColor, "Bucket band material must resolve a concrete non-white color: " + band.id());
            for (var content : WoodenBucketCatalog.CONTENTS) {
                if (!content.empty() && !"rainbow_sap".equals(content.id())) {
                    assertNotEquals(
                            0xFFFFFF,
                            WoodenBucketVisuals.liquidRgb(content),
                            "Filled wooden bucket content must define an explicit non-white tint: " + content.id());
                }
                String id = WoodenBucketCatalog.itemId(band, content);
                Path staticModel = assets.resolve("models/item/" + id + ".json");
                assertFalse(
                        Files.exists(staticModel), "Static wooden bucket model should be virtualized: " + staticModel);
                String json = WoodenBucketVisuals.modelJson(content);
                assertTrue(json.contains("\"layer0\":\"" + WoodenBucketVisuals.BASE_TEXTURE + "\""));
                assertTrue(json.contains("\"layer1\":\"" + WoodenBucketVisuals.BAND_OVERLAY_TEXTURE + "\""));
                if (content.empty()) {
                    assertFalse(json.contains("\"layer2\""));
                } else if ("rainbow_sap".equals(content.id())) {
                    assertTrue(json.contains("\"layer2\":\"" + WoodenBucketVisuals.RAINBOW_SAP_OVERLAY_TEXTURE + "\""));
                    assertEquals(0xFFFFFF, WoodenBucketVisuals.tintRgb(band, content, 2));
                } else {
                    assertTrue(json.contains("\"layer2\":\"" + WoodenBucketVisuals.LIQUID_OVERLAY_TEXTURE + "\""));
                    assertEquals(content.liquidRgb(), WoodenBucketVisuals.tintRgb(band, content, 2));
                }
                assertEquals(0xFFFFFF, WoodenBucketVisuals.tintRgb(band, content, 0));
                assertEquals(bandColor, WoodenBucketVisuals.tintRgb(band, content, 1));
            }
        }
        var chocolate = TexturePaletteTable.load().entry("textures/block/fluids/potion.chocolatemilk.png");
        assertNotNull(chocolate);
        assertEquals(0, chocolate.mapAbgr(0x00123456));
    }
}
