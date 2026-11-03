package gregtech.content.material;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.generated.WoodMaterials;
import gregtech.content.recipe.TechnologyIngredients;
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
}
