package gregtech.content.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.api.mod.ModData;
import gregtech.content.fluid.FluidDefinitions;
import gregtech.content.recipe.ClayMoldCatalog;
import gregtech.content.recipe.EquipmentCraftingCatalog;
import gregtech.content.recipe.SourceRecyclingFacts;
import gregtech.content.recipe.SourceWaxMaterials;
import gregtech.content.storage.ItemBarrelCatalog;
import gregtech.content.tool.AnvilVariantCatalog;
import gregtech.content.tool.PortableCraftingCatalog;
import gregtech.engine.material.GTMaterialRegistry;
import org.junit.jupiter.api.Test;

class MaterialCatalogTest {
    @Test
    void portableCraftingIndexOnlyReferencesPackagedRecipes() {
        for (String file : EquipmentCraftingCatalog.FILES) assertRecipeResource(file);
        for (String file : ClayMoldCatalog.FILES) assertRecipeResource(file);
        for (String file : PortableCraftingCatalog.FILES) assertRecipeResource("fluid_tools/" + file);
    }

    private static void assertRecipeResource(String file) {
        assertNotNull(MaterialCatalogTest.class.getResource("/data/gt6ce/recipes/" + file), file);
    }

    @Test
    void standaloneMaterialsAndDeviceCatalogsResolveWithoutLegacyMods() {
        ModData.bindPresence(id -> false);
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        FluidDefinitions.prepare();
        // Building the thermal facts also verifies every phase binding and target has an identity.
        assertTrue(!SourceRecyclingFacts.all().isEmpty());
        assertNotNull(SourceRecyclingFacts.find(260, "Iron"));
        assertNotNull(SourceRecyclingFacts.find(790, "Gold"));
        assertNull(SourceRecyclingFacts.find(1850, "Abyssalnite"));
        assertNull(SourceRecyclingFacts.find(8703, "Manyullyn"));
        assertTrue(gregtech.content.energy.WireCatalog.specifications().stream()
                .anyMatch(wire -> wire.material() == Materials.Gold && wire.insulated() && wire.size() == 4));
        for (int id : new int[] {
            1190, 1200, 2390, 8004, 8005, 8106, 8198, 8199, 8202, 8227, 8228, 8243, 8244, 8279, 8317, 8350, 8351, 8352,
            8353, 8354, 8355, 8356, 8357, 8358, 8362, 8363, 8364, 8373, 8388, 8394, 8411, 8414, 8463, 8466, 8469, 8509,
            8512, 8513, 8523, 8525, 8526, 8527, 8606, 8613, 8637, 8639, 8642, 8649, 8650, 8654, 8678, 8683, 8688, 8690,
            8691, 8692, 8693, 8694, 8695, 8696, 8701, 8711, 8718, 8725, 8728, 8731, 8733, 8736, 8753, 8754, 8755, 8757,
            8766, 8769, 8770, 8771, 8776, 8794, 8806, 8807, 8808, 8809, 8810, 8811, 8812, 9172, 9173, 9178, 9181, 9182,
            9183, 9191, 9244, 9245, 9249, 9250, 9251, 9253, 9254, 9255, 9256, 9257, 9258, 9259, 9260, 9262, 9263, 9264,
            9265, 9266, 9267, 9268, 9269, 9270, 9320, 9324, 9325, 9326, 9327, 9328, 9331, 9332, 9333, 9337, 9338, 9339,
            9340, 9341, 9358, 9371, 9406, 9704, 9706, 9707, 9708, 9714, 9716, 9717, 9718, 9719, 9778, 9786, 9787, 9788,
            9789, 9795, 9797, 9798, 9799, 9887
        }) assertTrue(!GTMaterialRegistry.get(id).isValid(), "Retired material " + id);
        for (int i = 0; i < 16; i++) {
            var dye = gregtech.content.data.ImportedMaterialData.DATA.DYE_MATERIALS[i];
            assertEquals(8250 + i, dye.getId());
            assertTrue(dye.getName().startsWith("Dye"));
            assertTrue(!GTMaterialRegistry.get(9500 + i).isValid());
            assertNotNull(SourceRecyclingFacts.find(dye.getId(), dye.getName()));
        }
        for (String retained :
                java.util.List.of("Greatwood", "DominicanAmber", "ElvenQuartz", "Adamantine", "TitaniumIridium"))
            assertTrue(GTMaterialRegistry.get(retained).isValid(), retained);
        var waxes = SourceWaxMaterials.materials();
        assertEquals(
                java.util.List.of(8235, 8236, 8238, 8239),
                waxes.stream().map(material -> material.getId()).toList());
        assertEquals(
                java.util.List.of("Wax", "WaxBee", "WaxParaffin", "WaxPlant"),
                waxes.stream().map(material -> material.getName()).toList());
        for (String name : new String[] {
            "Iron",
            "Amethyst",
            "Apatite",
            "Honey",
            "Ethanol",
            "Maple",
            "Willow",
            "BlueMahoe",
            "WoodDead",
            "Cherrywood",
            "WoodTreated"
        }) assertTrue(GTMaterialRegistry.get(name).isValid(), name);
        for (var barrel : ItemBarrelCatalog.ALL) {
            assertTrue(GTMaterialRegistry.get(barrel.material()).isValid(), barrel.id());
        }
        for (var anvil : AnvilVariantCatalog.ALL)
            assertTrue(GTMaterialRegistry.get(anvil.material()).isValid(), anvil.id());
    }
}
