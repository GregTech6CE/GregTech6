package gregtech.engine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.data.FluidCatalog;
import gregtech.content.fluid.FluidDefinitions;
import gregtech.content.material.MaterialTestFixture;
import gregtech.content.tool.WoodenBucketCatalog;
import gregtech.engine.tool.WoodenBucketVisuals;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link TexturePaletteTable} rules, kept template PNGs, dead fluid exclusions, and
 * layered {@link WoodenBucketVisuals} integrity without Minecraft client dependencies.
 */
final class TexturePaletteAndBucketVisualTest {
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
