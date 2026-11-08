package gregtech.engine.material;

import static org.junit.jupiter.api.Assertions.*;

import gregtech.api.addon.material.*;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.MaterialTestFixture;
import gregtech.engine.prefix.BlockMaterialPrefix;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        for (var prefix :
                Set.of(MaterialPrefix.gem, MaterialPrefix.plateGem, MaterialPrefix.stick, MaterialPrefix.arrowGtWood)) {
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
        assertFalse(MaterialViews.catalog()
                .forms()
                .forms(new MaterialKey("Iron"))
                .contains(new FormKey(BlockMaterialPrefix.blockIngot.getName())));
        assertFalse(MaterialViews.catalog()
                .forms()
                .allows(new MaterialKey("Iron"), new FormKey(BlockMaterialPrefix.blockIngot.getName())));
    }

    @Test
    void tantalumKeepsItsReviewedForcedGemRule() {
        var gem = rule(rules("Tantalum"), MaterialPrefix.gem.getName());
        assertEquals(MaterialFormKind.ITEM, gem.kind());
        assertEquals(MaterialPrefix.gem.getMaterialWeight(), gem.materialUnits());
    }

    @Test
    void bindingKindMustMatchAnExistingDefinitionRule() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MaterialViews.publishContent(List.of(new MaterialFormBinding(
                        new MaterialKey("Iron"),
                        new FormKey(MaterialPrefix.ingot.getName()),
                        MaterialFormKind.BLOCK,
                        "gt6ce:invalid_ingot_block",
                        1))));
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
