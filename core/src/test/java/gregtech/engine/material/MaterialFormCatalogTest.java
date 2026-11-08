package gregtech.engine.material;

import gregtech.api.addon.material.FormKey;
import gregtech.api.addon.material.MaterialFormKind;
import gregtech.api.addon.material.MaterialFormRule;
import gregtech.api.addon.material.MaterialKey;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.MaterialTestFixture;
import gregtech.engine.prefix.BlockMaterialPrefix;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialFormCatalogTest {
    @BeforeAll
    static void initMaterials() {
        MaterialTestFixture.ensureInitialized();
    }

    @Test
    void paperKeepsEverySourceMultiplateRule() {
        var rules = rules("Paper");
        for (var prefix : Set.of(
                MaterialPrefix.plateDouble,
                MaterialPrefix.plateTriple,
                MaterialPrefix.plateQuadruple,
                MaterialPrefix.plateQuintuple)) {
            var rule = rule(rules, prefix.getName());
            assertEquals(MaterialFormKind.ITEM, rule.kind());
            assertEquals(prefix.getMaterialWeight(), rule.materialUnits());
        }
        assertEquals(
                MaterialPrefix.plateTiny.getMaterialWeight(),
                rule(rules, MaterialPrefix.plateTiny.getName()).materialUnits());
    }

    @Test
    void quartzFamilyKeepsGemPlateAndStickRules() {
        var rules = rules("NetherQuartz");
        for (var prefix : Set.of(
                MaterialPrefix.gem, MaterialPrefix.plateGem, MaterialPrefix.stick, MaterialPrefix.arrowGtWood)) {
            var rule = rule(rules, prefix.getName());
            assertEquals(MaterialFormKind.ITEM, rule.kind());
            assertEquals(prefix.getMaterialWeight(), rule.materialUnits());
        }
    }

    @Test
    void metalRulesKeepItemAndBlockKindsSeparate() {
        var rules = rules("Iron");
        var ingot = rule(rules, MaterialPrefix.ingot.getName());
        assertEquals(MaterialFormKind.ITEM, ingot.kind());
        assertEquals(MaterialPrefix.ingot.getMaterialWeight(), ingot.materialUnits());

        var block = rule(rules, BlockMaterialPrefix.blockIngot.getName());
        assertEquals(MaterialFormKind.BLOCK, block.kind());
        assertEquals(BlockMaterialPrefix.blockIngot.getMaterialWeight(), block.materialUnits());
    }

    @Test
    void tantalumKeepsItsReviewedForcedGemRule() {
        var gem = rule(rules("Tantalum"), MaterialPrefix.gem.getName());
        assertEquals(MaterialFormKind.ITEM, gem.kind());
        assertEquals(MaterialPrefix.gem.getMaterialWeight(), gem.materialUnits());
    }

    private static Set<MaterialFormRule> rules(String material) {
        var rules = MaterialViews.catalog().forms().rules(new MaterialKey(material));
        assertTrue(!rules.isEmpty(), material + " has no definition-time form rules");
        return rules;
    }

    private static MaterialFormRule rule(Set<MaterialFormRule> rules, String form) {
        return rules.stream()
                .filter(rule -> rule.form().equals(new FormKey(form)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing form rule: " + form));
    }
}
