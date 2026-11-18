package gregtech.content.machine;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.data.ImportedMaterialData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.content.data.BasicMachineCraftingRecipes.Form;
import gregtech.content.data.BasicMachineCraftingRecipes.Recipe;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.MaterialTestFixture;
import gregtech.engine.material.ItemComposition;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class BasicMachineRecyclingTest {
    @BeforeAll
    static void initializeMaterials() {
        MaterialTestFixture.ensureInitialized();
    }

    @Test
    void crusherUsesTheSameTierMaterialForRegistrationAndRecovery() {
        for (int tier = 1; tier <= 2; tier++) {
            var casing = tier == 1 ? Bronze : Steel;
            var harvest = BasicMachineCatalog.harvest("crusher", tier);
            var composition =
                    MachineConstructionMaterials.machine("crusher", tier).orElseThrow();
            assertEquals(casing, harvest.material());
            assertEquals(Map.of(casing, 15 * U, Diamond, 2 * U), amounts(composition));
        }
    }

    @Test
    void changingARecipeIngredientChangesEveryMatchingSlot() {
        var crusher = BasicMachineCatalog.crafting("crusher", 1);
        var keys = new HashMap<>(crusher.keys());
        keys.replaceAll((symbol, ingredient) -> ingredient instanceof Form form && form.material() == Diamond
                ? new Form(MaterialPrefix.dustSmall, form.material(), form.group())
                : ingredient);
        var changed = BasicMachineRecycling.derive(
                new Recipe(crusher.rows(), keys), Bronze, BasicMachineCatalog.sourceId("crusher", 1));
        assertEquals(Map.of(Bronze, 15 * U, Diamond, U / 2), amounts(changed));
    }

    @Test
    void compactComponentsAndKnownTagDataContributeTheirOriginalMaterials() {
        var mixer = MachineConstructionMaterials.machine("electricmixer", 2).orElseThrow();
        assertEquals(
                Map.of(
                        StainlessSteel, 49 * U / 4,
                        Al, 19 * U / 2,
                        Cu, 5 * U,
                        Rubber, 2 * U,
                        SteelMagnetic, U / 2),
                amounts(mixer));

        var melter = MachineConstructionMaterials.machine("melter", 1).orElseThrow();
        assertEquals(Map.of(Fe, 14 * U, Ceramic, 7 * U, Brick, 8 * U, Cu, 2 * U), amounts(melter));
    }

    @Test
    void sourceOreFormContributesItsMaterial() {
        var cracker =
                MachineConstructionMaterials.machine("catalyticcracker", 1).orElseThrow();
        assertEquals(Map.of(Steel, 26 * U, Invar, 8 * U, Cu, 2 * U, OREMATS.Zeolite, 2 * U), amounts(cracker));
    }

    private static Map<GTMaterial, Long> amounts(ItemComposition composition) {
        return composition.components().stream()
                .collect(Collectors.toMap(MaterialComponent::material, MaterialComponent::amount));
    }
}
