package gregtech.content.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Structural invariants for the migrated RecipePublishing rule tables in core. */
final class GeneratedRecipeRulesTest {
    @Test
    void clayMoldRulesAreCompleteAndUnique() {
        var recipes = ClayMoldRecipeRules.recipes();
        assertEquals(422, recipes.size());
        Set<String> paths = new HashSet<>();
        for (var recipe : recipes) {
            assertTrue(paths.add(recipe.path()), "duplicate clay mold path " + recipe.path());
        }
    }

    @Test
    void extruderShapeRulesAreCompleteAndUnique() {
        var recipes = ExtruderShapeRecipeRules.recipes();
        assertEquals(78, recipes.size());
        Set<String> paths = new HashSet<>();
        for (var recipe : recipes) {
            assertTrue(paths.add(recipe.path()), "duplicate extruder shape path " + recipe.path());
        }
    }

    @Test
    void energyEquipmentRulesAreCompleteAndUnique() {
        assertEquals(126, EnergyEquipmentRecipeRules.ROWS.size());
        Set<String> paths = new HashSet<>();
        for (var row : EnergyEquipmentRecipeRules.ROWS) {
            assertTrue(paths.add(row.jsonPath()), "duplicate energy equipment path " + row.jsonPath());
        }
    }

    @Test
    void componentAndSmelteryRulesAreCompleteAndUnique() {
        var rows = ComponentAndSmelteryRecipeRules.all();
        assertEquals(215, rows.size());
        Set<String> paths = new HashSet<>();
        for (var row : rows) {
            assertTrue(paths.add(row.path()), "duplicate component/smeltery path " + row.path());
        }
    }
}
