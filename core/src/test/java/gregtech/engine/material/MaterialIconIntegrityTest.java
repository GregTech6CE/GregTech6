package gregtech.engine.material;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.cover.MaterialCoverRules;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.MaterialTestFixture;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards the generated material icon tree: every material resolves to a modeled texture set, and
 * every shared item model agrees with the icon sprites shipped next to it.
 */
class MaterialIconIntegrityTest {
    private static final String ITEM_ICONS = "/assets/gt6ce/textures/item/material_icons/";
    private static final String BLOCK_ICONS = "/assets/gt6ce/textures/block/material_icons/";
    private static final String ITEM_MODELS = "/assets/gt6ce/models/item/material/";
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
    void unmodeledSetsShipNoIconResources() {
        List<String> leaked = new ArrayList<>();
        for (MaterialTextureSet set : MaterialTextureSet.values()) {
            if (MaterialTextureSet.modeled().contains(set)) {
                continue;
            }
            if (exists(BLOCK_ICONS + set.folder() + "/blocksolid.png")
                    || exists(ITEM_ICONS + set.folder() + "/ingot.png")
                    || exists(ITEM_MODELS + set.folder() + "/ingot.json")) {
                leaked.add(set.name());
            }
        }
        assertTrue(leaked.isEmpty(), "Unmodeled texture sets still ship icon resources: " + leaked);
    }

    @Test
    void modeledSetsShipABlockIconFolder() {
        List<String> missing = new ArrayList<>();
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            if (!exists(BLOCK_ICONS + set.folder() + "/blocksolid.png")) {
                missing.add(set.folder());
            }
        }
        assertTrue(missing.isEmpty(), "Modeled sets without a block icon folder: " + missing);
    }

    @Test
    void sharedItemModelsMatchTheirIconSprites() throws IOException {
        List<String> missing = new ArrayList<>();
        List<String> mismatched = new ArrayList<>();
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            for (MaterialPrefix prefix : MaterialItemDefinitions.candidatePrefixes()) {
                String file = prefix.getTextureFileName();
                String modelPath = ITEM_MODELS + set.folder() + "/" + file + ".json";
                String texturePath = ITEM_ICONS + set.folder() + "/" + file + ".png";
                String model = exists(modelPath) ? read(modelPath) : null;
                if (model == null) {
                    missing.add(modelPath);
                    continue;
                }
                if (!exists(texturePath)) {
                    missing.add(texturePath);
                    continue;
                }
                boolean declaresOverlay = model.contains("\"layer1\"");
                boolean hasOverlay = exists(ITEM_ICONS + set.folder() + "/" + file + "_overlay.png");
                if (declaresOverlay != hasOverlay) {
                    mismatched.add(set.folder() + "/" + file + " layer1=" + declaresOverlay + " overlay=" + hasOverlay);
                }
            }
        }
        assertTrue(missing.isEmpty(), "Missing icon resources: " + missing);
        assertTrue(mismatched.isEmpty(), "Model and overlay sprite disagree: " + mismatched);
    }

    @Test
    void coverDesignsShipTheirIconSprites() {
        List<String> missing = new ArrayList<>();
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            for (String prefix : COVER_PREFIXES) {
                for (String icon : MaterialCoverRules.textures(prefix)) {
                    String path = BLOCK_ICONS + set.folder() + "/" + icon + ".png";
                    if (!exists(path)) {
                        missing.add(path);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Cover designs without a block icon sprite: " + missing);
    }

    private static boolean exists(String path) {
        try (InputStream stream = MaterialIconIntegrityTest.class.getResourceAsStream(path)) {
            return stream != null;
        } catch (IOException closed) {
            return false;
        }
    }

    private static String read(String path) throws IOException {
        try (InputStream stream = MaterialIconIntegrityTest.class.getResourceAsStream(path)) {
            return stream == null ? null : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
