package gregtech.engine.recipe.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gregtech.api.addon.recipe.MachineRecipeDocument;
import gregtech.api.addon.recipe.MachineRecipeDocuments;
import gregtech.api.addon.recipe.MachineRecipeRemovalDocument;
import org.junit.jupiter.api.Test;

/** Numeric fields are narrowed only after their full range is validated. */
final class MachineRecipeDocumentsTest {
    @Test
    void negativeValuesOutsideTheIntRangeAreRejectedInsteadOfWrapping() {
        assertThrows(IllegalArgumentException.class, () -> removal("""
                        {"map":"gt.recipe.crusher",
                         "item_inputs":[{"item":"minecraft:stone","count":-4294967295}]}
                        """));
        assertThrows(IllegalArgumentException.class, () -> removal("""
                        {"map":"gt.recipe.crusher",
                         "item_outputs":[{"item":"minecraft:stone","count":2147483648}]}
                        """));
    }

    @Test
    void boundaryCountsAreAccepted() {
        var spec = removal("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:stone","count":2147483647}]}
                """);
        assertEquals(2147483647, spec.itemInputs().get(0).count());
    }

    @Test
    void nonIntegralValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> removal("""
                        {"map":"gt.recipe.crusher",
                         "item_inputs":[{"item":"minecraft:stone","count":1.5}]}
                        """));
    }

    @Test
    void removalConditionsDoNotTakePriority() {
        // Removal matches inputs/outputs; a priority filter would silently change an old datapack's
        // meaning, so the field stays an addition-only annotation and is rejected on removal.
        assertThrows(IllegalArgumentException.class, () -> removal("""
                        {"map":"gt.recipe.crusher","priority":5,
                         "item_inputs":[{"item":"minecraft:stone"}]}
                        """));
    }

    @Test
    void priorityIsOptionalAndRoundTripsOnlyWhenSet() {
        var ordinary = addition("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:stone"}],
                 "item_outputs":[{"item":"minecraft:gravel"}],
                 "eu_per_tick":16,"duration":20}
                """);
        assertEquals(0, ordinary.priority());
        assertEquals("""
                {"map":"gt.recipe.crusher","item_inputs":[{"item":"minecraft:stone","count":1}],\
                "fluid_inputs":[],"item_outputs":[{"item":"minecraft:gravel","count":1,"chance":10000}],\
                "fluid_outputs":[],"eu_per_tick":16,"duration":20,"special":0}""", MachineRecipeDocuments.canonical(ordinary));

        var prioritized = addition("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:stone"}],
                 "item_outputs":[{"item":"minecraft:gravel"}],
                 "eu_per_tick":16,"duration":20,"priority":7}
                """);
        assertEquals(7, prioritized.priority());
        assertEquals(prioritized, addition(MachineRecipeDocuments.canonical(prioritized)));
    }

    @Test
    void itemOutputsTakeTags() {
        var spec = addition("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:stone"}],
                 "item_outputs":[{"tag":"forge:gravel"}],
                 "eu_per_tick":16,"duration":20}
                """);
        assertEquals("forge:gravel", spec.itemOutputs().get(0).tag());
        assertEquals(
                "{\"tag\":\"forge:gravel\",\"count\":1,\"chance\":10000}",
                itemJson(MachineRecipeDocuments.canonical(spec)));
        assertEquals(spec, addition(MachineRecipeDocuments.canonical(spec)));
    }

    @Test
    void fluidOutputsTakeTags() {
        var spec = addition("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:stone"}],
                 "fluid_outputs":[{"tag":"forge:steam","amount":100}],
                 "eu_per_tick":16,"duration":20}
                """);
        assertEquals("forge:steam", spec.fluidOutputs().get(0).tag());
        assertEquals(spec, addition(MachineRecipeDocuments.canonical(spec)));
    }

    @Test
    void itemOutputsTakeMaterialForms() {
        var spec = addition("""
                {"map":"gt.recipe.crusher",
                 "item_inputs":[{"item":"minecraft:stone"}],
                 "item_outputs":[{"material":"Steel","form":"ingot"}],
                 "eu_per_tick":16,"duration":20}
                """);
        assertEquals("Steel", spec.itemOutputs().get(0).material());
        assertEquals("ingot", spec.itemOutputs().get(0).form());
        assertEquals(spec, addition(MachineRecipeDocuments.canonical(spec)));
    }

    @Test
    void itemEntriesNeedExactlyOneIdentity() {
        assertThrows(IllegalArgumentException.class, () -> addition("""
                        {"map":"gt.recipe.crusher",
                         "item_inputs":[{"item":"minecraft:stone","tag":"forge:stone"}],
                         "item_outputs":[{"item":"minecraft:gravel"}],
                         "eu_per_tick":16,"duration":20}
                        """));
        assertThrows(IllegalArgumentException.class, () -> addition("""
                        {"map":"gt.recipe.crusher",
                         "item_inputs":[{"material":"Steel"}],
                         "item_outputs":[{"item":"minecraft:gravel"}],
                         "eu_per_tick":16,"duration":20}
                        """));
    }

    /** Extracts one serialized item entry so the test does not depend on key order elsewhere. */
    private static String itemJson(String document) {
        int start = document.indexOf("\"item_outputs\":[") + "\"item_outputs\":[".length();
        return document.substring(start, document.indexOf(']', start));
    }

    private static MachineRecipeDocument addition(String json) {
        return MachineRecipeDocuments.addition(JsonMaps.object(json));
    }

    private static MachineRecipeRemovalDocument removal(String json) {
        return MachineRecipeDocuments.removal(JsonMaps.object(json));
    }
}
