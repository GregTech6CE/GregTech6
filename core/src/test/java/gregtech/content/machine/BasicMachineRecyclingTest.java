package gregtech.content.machine;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.data.ImportedMaterialData.Bronze;
import static gregtech.content.data.ImportedMaterialData.StainlessSteel;
import static gregtech.content.data.ImportedMaterialData.Steel;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gregtech.api.material.MaterialComponent;
import gregtech.content.data.BasicMachineCraftingRecipes.BlockForm;
import gregtech.content.data.BasicMachineCraftingRecipes.Recipe;
import gregtech.content.data.BasicMachineCraftingRecipes.RegistryItem;
import gregtech.content.data.BasicMachineCraftingRecipes.RegistryTag;
import gregtech.content.material.MaterialTestFixture;
import gregtech.engine.prefix.BlockMaterialPrefix;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class BasicMachineRecyclingTest {
    @Test
    void recoversKnownBlockMaterialsAndSkipsUnknownRegistryInputs() {
        MaterialTestFixture.ensureInitialized();
        var recipe = new Recipe(
                List.of("BBW", "UTX"),
                Map.of(
                        'B', new BlockForm(BlockMaterialPrefix.casingMachineDouble, Steel),
                        'W', new RegistryItem("gt6ce:tank_wall"),
                        'U', new RegistryItem("gt6ce:unlisted_part"),
                        'T', new RegistryTag("gt6ce:technology/any_iron_plate"),
                        'X', new RegistryItem("example:tank_wall")));

        assertEquals(
                List.of(MaterialComponent.of(Steel, 28 * U), MaterialComponent.of(StainlessSteel, 4 * U)),
                BasicMachineRecycling.derive(recipe, Bronze, 0).components());
    }
}
