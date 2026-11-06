package gregtech.engine.recipe.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Removal conditions are a set assignment; listing order must not decide the outcome. */
final class MachineRecipeMatchTest {
    private static final MachineRecipeMatch.TagQuery TAGS = new MachineRecipeMatch.TagQuery() {
        @Override
        public boolean itemInTag(String itemId, String tagId) {
            return tagId.equals("test:any");
        }
    };

    @Test
    void aBroadConditionDoesNotConsumeTheSpecificInputsOnlySlot() {
        var spec = MachineRecipeDocuments.removal("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"tag":"test:any"},{"item":"minecraft:dirt"}]}
                """);
        // Greedy first-match would give the tag the dirt slot and then fail on the specific need.
        assertTrue(MachineRecipeMatch.matches(spec, view(List.of("minecraft:dirt", "minecraft:stone")), TAGS));
    }

    @Test
    void conditionOrderIsIrrelevant() {
        var first = MachineRecipeDocuments.removal("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"tag":"test:any"},{"item":"minecraft:dirt"}]}
                """);
        var second = MachineRecipeDocuments.removal("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:dirt"},{"tag":"test:any"}]}
                """);
        var recipe = view(List.of("minecraft:dirt", "minecraft:stone"));
        assertTrue(MachineRecipeMatch.matches(first, recipe, TAGS));
        assertTrue(MachineRecipeMatch.matches(second, recipe, TAGS));
    }

    @Test
    void repeatedConditionsNeedDistinctInputs() {
        var spec = MachineRecipeDocuments.removal("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:dirt"},{"item":"minecraft:dirt"}]}
                """);
        assertFalse(MachineRecipeMatch.matches(spec, view(List.of("minecraft:dirt")), TAGS));
        assertTrue(MachineRecipeMatch.matches(spec, view(List.of("minecraft:dirt", "minecraft:dirt")), TAGS));
    }

    @Test
    void unmatchedConditionStillRejectsTheRecipe() {
        var spec = MachineRecipeDocuments.removal("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:stone"}]}
                """);
        assertFalse(MachineRecipeMatch.matches(spec, view(List.of("minecraft:dirt")), TAGS));
    }

    private static MachineRecipeMatch.RecipeView view(List<String> inputItems) {
        return new MachineRecipeMatch.RecipeView() {
            public int inputItems() {
                return inputItems.size();
            }

            public String inputItem(int index) {
                return inputItems.get(index);
            }

            public int inputItemCount(int index) {
                return 1;
            }

            public int inputFluids() {
                return 0;
            }

            public String inputFluid(int index) {
                return "";
            }

            public long inputFluidAmount(int index) {
                return 0;
            }

            public int outputItems() {
                return 0;
            }

            public String outputItem(int index) {
                return "";
            }

            public int outputItemCount(int index) {
                return 0;
            }

            public int outputItemChance(int index) {
                return 0;
            }

            public int outputFluids() {
                return 0;
            }

            public String outputFluid(int index) {
                return "";
            }

            public long outputFluidAmount(int index) {
                return 0;
            }
        };
    }
}
