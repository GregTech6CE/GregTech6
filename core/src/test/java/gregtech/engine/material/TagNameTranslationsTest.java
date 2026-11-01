package gregtech.engine.material;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TagNameTranslationsTest {
    @Test
    void composesFormsAndCanonicalAliasesInBothTagDialects() {
        var names = new TagNameTranslations(Map.of(
                "material.gt6ce.iron", "铁",
                "material.gt6ce.aluminium", "铝",
                "item.gt6ce.dust", "%s粉",
                "block.gt6ce.block_raw", "%s粗矿块",
                "gt6ce.tag.part.storage_blocks", "储存块"));
        assertEquals("铁粉", names.resolve("tag.item.gt6ce.dust.iron"));
        assertEquals("铝粉", names.resolve("tag.item.forge.dusts.aluminum"));
        assertEquals("铝粉", names.resolve("tag.item.c.dusts.aluminum"));
        assertEquals("储存块 · 铁粗矿块", names.resolve("tag.c.storage_blocks.raw_iron"));
    }

    @Test
    void honorsExactNamesAndDoesNotHideUnknownTags() {
        var names = new TagNameTranslations(Map.of(
                "tag.gt6ce.dust.iron", "Pack override",
                "material.gt6ce.iron", "Iron",
                "item.gt6ce.dust", "%s Dust"));
        assertEquals("Pack override", names.resolve("tag.item.gt6ce.dust.iron"));
        assertNull(names.resolve("tag.item.othermod.dust.iron"));
        assertNull(names.resolve("tag.item.gt6ce.unknown.iron"));
        assertNull(names.resolve("item.gt6ce.dust"));
    }

    @Test
    void languageReloadDoesNotRetainOldNames() {
        var english = new TagNameTranslations(Map.of("gt6ce.tag.part.distilled_water", "Distilled Water"));
        var chinese = new TagNameTranslations(Map.of("gt6ce.tag.part.distilled_water", "蒸馏水"));
        assertEquals("Distilled Water", english.resolve("tag.fluid.gt6ce.distilled_water"));
        assertEquals("蒸馏水", chinese.resolve("tag.fluid.gt6ce.distilled_water"));
    }
}
