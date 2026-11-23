package gregtech.content.material;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.data.MaterialGroups;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.generated.WoodMaterials;
import gregtech.content.recipe.TechnologyIngredients;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class MaterialGroupsTest {
    @Test
    void anyWoodAcceptsPlainWoodBolts() {
        MaterialTestFixture.ensureInitialized();
        var family = TechnologyIngredients.FAMILIES.stream()
                .filter(candidate -> candidate.tag().equals("any_wood_bolt"))
                .findFirst()
                .orElseThrow();
        assertTrue(family.accepts(MaterialPrefix.bolt, WoodMaterials.Wood));
    }

    @Test
    void clayRecipeFamilyAcceptsEveryVariantWithoutMergingMaterials() {
        MaterialTestFixture.ensureInitialized();
        var ordinary = GTMaterialRegistry.get("Clay").resolve();
        assertTrue(MaterialGroups.contains(MaterialGroups.Clay, ordinary));
        for (String name : List.of("ClayBrown", "ClayRed", "Bentonite", "Palygorskite", "Kaolinite")) {
            var variant = GTMaterialRegistry.get(name).resolve();
            assertTrue(MaterialGroups.contains(MaterialGroups.Clay, variant), name);
            assertNotSame(ordinary, variant, name);
        }
        assertFalse(MaterialGroups.contains(MaterialGroups.Clay, GTMaterialRegistry.get("Ceramic")));
        assertFalse(MaterialGroups.contains(MaterialGroups.Clay, GTMaterialRegistry.get("Stone")));
    }
}
