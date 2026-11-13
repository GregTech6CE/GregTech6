package gregtech.content.data;

import static org.junit.jupiter.api.Assertions.*;

import gregtech.api.material.GTValues;
import gregtech.content.creative.CreativeTabCatalog;
import gregtech.content.creative.SourceCreativeCatalog;
import gregtech.content.material.MaterialTestFixture;
import gregtech.content.tool.OriginalToolShapes;
import gregtech.engine.material.GTMaterialRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Static guards for RFC #382: the coin prefix, its recipe, its creative entries and its resources are gone. */
class CoinRemovalTest {
    @BeforeAll
    static void initMaterials() {
        MaterialTestFixture.ensureInitialized();
    }

    @Test
    void coinPrefixIsNoLongerRegistered() {
        assertFalse(MaterialPrefixes.all().containsKey("coin"), "the OP registry has no coin entry");
    }

    @Test
    void plateTinyKeepsItsPredicateAndWeight() {
        assertEquals(GTValues.U9, MaterialPrefix.plateTiny.getMaterialWeight());
        assertTrue(MaterialPrefix.plateTiny.isValidFor(GTMaterialRegistry.get("Iron")));
    }

    @Test
    void coinMoldRecipeRowIsGone() {
        assertTrue(
                OriginalSurvivalRecipes.allRows().stream().noneMatch(row -> row.id().equals("coin_mold")),
                "the survival recipe table has no coin_mold row");
    }

    @Test
    void coinMoldToolShapeIsGone() {
        assertThrows(IllegalArgumentException.class, () -> OriginalToolShapes.bounds("coin_mold"));
    }

    @Test
    void coinsCreativeFamilyIsGone() {
        assertFalse(CreativeTabCatalog.FAMILIES.contains("coins"), "the creative family list has no coins");
        assertNull(SourceCreativeCatalog.entry("coin"), "the source catalog has no coin entry");
    }

    @Test
    void coinResourcesAreGone() throws Exception {
        Path assets = Path.of("src/main/resources/assets/gt6ce");
        assertTrue(Files.isDirectory(assets), "the core assets directory is present");
        try (Stream<Path> paths = Files.walk(assets)) {
            List<String> leftovers = paths.filter(Files::isRegularFile)
                    .map(assets::relativize)
                    .map(Path::toString)
                    .filter(path -> path.toLowerCase(Locale.ROOT).contains("coin") || path.contains("mold_coinage"))
                    .toList();
            assertTrue(leftovers.isEmpty(), "coin resources remain: " + leftovers);
        }
    }

    @Test
    void coinLanguageKeysAreGone() throws Exception {
        List<String> keys = List.of(
                "block.gt6ce.coin",
                "block.gt6ce.coin_mold",
                "block.gt6ce.coin_pile",
                "block.gt6ce.coins",
                "item.gt6ce.coin",
                "item.gt6ce.tab_icon_coin",
                "itemGroup.gt6ce.coin",
                "itemGroup.gt6ce.coins",
                "tooltip.gt6ce.coin_mold.1",
                "tooltip.gt6ce.coin_mold.2",
                "gt.lang.recipes.mold.coinage",
                "gt.tooltip.coins.1",
                "gt.tooltip.coins.2",
                "gt.tooltip.coins.3",
                "oredict.prefix.coin");
        for (String lang : List.of("en_us", "zh_cn")) {
            String json = Files.readString(
                    Path.of("src/main/resources/assets/gt6ce/lang/" + lang + ".json"), StandardCharsets.UTF_8);
            for (String key : keys) {
                assertFalse(json.contains("\"" + key + "\""), lang + " still defines " + key);
            }
        }
    }
}