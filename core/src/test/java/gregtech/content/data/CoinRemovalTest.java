package gregtech.content.data;

import static org.junit.jupiter.api.Assertions.*;

import gregtech.api.material.GTValues;
import gregtech.content.material.MaterialTestFixture;
import gregtech.engine.material.GTMaterialRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Guards that plateTiny keeps its U9 weight and its predicate for tiny-plate materials such as Iron. */
class CoinRemovalTest {
    @BeforeAll
    static void initMaterials() {
        MaterialTestFixture.ensureInitialized();
    }

    @Test
    void plateTinyKeepsItsPredicateAndWeight() {
        assertEquals(GTValues.U9, MaterialPrefix.plateTiny.getMaterialWeight());
        assertTrue(MaterialPrefix.plateTiny.isValidFor(GTMaterialRegistry.get("Iron")));
    }
}
