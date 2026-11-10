package gregtech.engine.material;

import gregtech.api.material.MaterialComponent;
import gregtech.content.material.MaterialTestFixture;
import gregtech.content.material.Materials;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemCompositionResolverTest {
    @BeforeAll
    static void init() {
        MaterialTestFixture.ensureInitialized();
    }

    @Test
    void candidateOrderDoesNotChangeTheEffectiveComposition() {
        var explicit = candidate("explicit", ItemCompositionSource.Kind.EXPLICIT, Materials.Iron, 4, true);
        var tag = candidate("tag", ItemCompositionSource.Kind.FORM_TAG, Materials.Copper, 1, true);
        var expected = ItemCompositionResolver.resolve(Map.of("test:item", List.of(tag, explicit)));
        var reversed = new ArrayList<>(List.of(explicit, tag));
        Collections.reverse(reversed);
        assertEquals(expected, ItemCompositionResolver.resolve(Map.of("test:item", reversed)));
        assertEquals(Materials.Iron.resolve(), expected.get("test:item").material());
    }

    @Test
    void equivalentExplicitDeclarationsMergeTheirSources() {
        var first = candidate("first", ItemCompositionSource.Kind.EXPLICIT, Materials.Iron, 4, true);
        var second = candidate("second", ItemCompositionSource.Kind.VANILLA_JSON, Materials.Iron, 4, true);
        var data = ItemCompositionResolver.resolve(Map.of("test:item", List.of(second, first)))
                .get("test:item");
        assertEquals(
                List.of("first", "second"),
                data.sources().stream()
                        .map(ItemCompositionSource::declarationId)
                        .toList());
    }

    @Test
    void conflictingSamePriorityCandidatesFailWithBothDeclarations() {
        var iron = candidate("iron", ItemCompositionSource.Kind.EXPLICIT, Materials.Iron, 4, true);
        var copper = candidate("copper", ItemCompositionSource.Kind.EXPLICIT, Materials.Copper, 4, true);
        var error = assertThrows(
                IllegalStateException.class,
                () -> ItemCompositionResolver.resolve(Map.of("test:item", List.of(iron, copper))));
        assertTrue(error.getMessage().contains("iron"));
        assertTrue(error.getMessage().contains("copper"));
    }

    @Test
    void tagCandidateIsEffectiveWhenNoExplicitLayerExists() {
        var tag = candidate("tag", ItemCompositionSource.Kind.FORM_TAG, Materials.Copper, 1, true);
        var resolved = ItemCompositionResolver.resolve(Map.of("test:item", List.of(tag)))
                .get("test:item");
        assertEquals(Materials.Copper.resolve(), resolved.material());
        assertEquals(
                ItemCompositionSource.Kind.FORM_TAG, resolved.sources().get(0).kind());
    }

    @Test
    void explicitOverrideIsValidatedAndTracked() {
        var json = candidate("json", ItemCompositionSource.Kind.VANILLA_JSON, Materials.Iron, 4, false);
        var correction = new ItemCompositionCandidate(
                composition("correction", ItemCompositionSource.Kind.CORRECTION, Materials.Copper, 4, true), "json");
        var resolved = ItemCompositionResolver.resolve(Map.of("test:item", List.of(json, correction)));
        assertEquals(Materials.Copper.resolve(), resolved.get("test:item").material());
        assertEquals(List.of("json"), resolved.get("test:item").sources().get(0).replacedSourceIds());
        assertThrows(
                IllegalStateException.class,
                () -> ItemCompositionResolver.resolve(Map.of("test:item", List.of(correction))));

        var cycleA = new ItemCompositionCandidate(
                composition("a", ItemCompositionSource.Kind.EXPLICIT, Materials.Iron, 4, true), "b");
        var cycleB = new ItemCompositionCandidate(
                composition("b", ItemCompositionSource.Kind.EXPLICIT, Materials.Copper, 4, true), "a");
        assertThrows(
                IllegalStateException.class,
                () -> ItemCompositionResolver.resolve(Map.of("test:item", List.of(cycleA, cycleB))));
    }

    @Test
    void oneDeclarationCannotBeReplacedByMultipleCandidates() {
        var json = candidate("json", ItemCompositionSource.Kind.VANILLA_JSON, Materials.Iron, 4, true);
        var first = new ItemCompositionCandidate(
                composition("first", ItemCompositionSource.Kind.CORRECTION, Materials.Iron, 4, true), "json");
        var second = new ItemCompositionCandidate(
                composition("second", ItemCompositionSource.Kind.CORRECTION, Materials.Iron, 4, true), "json");

        var error = assertThrows(
                IllegalStateException.class,
                () -> ItemCompositionResolver.resolve(Map.of("test:item", List.of(json, first, second))));
        assertTrue(error.getMessage().contains("replaced by both"));
    }

    @Test
    void replacementTraceRetainsTheCompleteOverrideChain() {
        var base = candidate("base", ItemCompositionSource.Kind.VANILLA_JSON, Materials.Iron, 4, true);
        var middle = new ItemCompositionCandidate(
                composition("middle", ItemCompositionSource.Kind.EXPLICIT, Materials.Iron, 4, true), "base");
        var winner = new ItemCompositionCandidate(
                composition("winner", ItemCompositionSource.Kind.CORRECTION, Materials.Iron, 4, true), "middle");

        var resolved = ItemCompositionResolver.resolve(Map.of("test:item", List.of(base, middle, winner)))
                .get("test:item");

        assertEquals(List.of("middle", "base"), resolved.sources().get(0).replacedSourceIds());
    }

    @Test
    void replacementTargetMayBeRegisteredAfterTheOverride() {
        var correction = new ItemCompositionCandidate(
                composition("correction", ItemCompositionSource.Kind.CORRECTION, Materials.Copper, 4, true), "json");
        var pending = ItemCompositionResolver.resolveLenient(Map.of("test:item", List.of(correction)));
        assertEquals(Materials.Copper.resolve(), pending.get("test:item").material());

        var json = candidate("json", ItemCompositionSource.Kind.VANILLA_JSON, Materials.Iron, 4, false);
        var resolved = ItemCompositionResolver.resolve(Map.of("test:item", List.of(json, correction)));
        assertEquals(Materials.Copper.resolve(), resolved.get("test:item").material());
        assertEquals(
                List.of("correction"),
                resolved.get("test:item").sources().stream()
                        .map(ItemCompositionSource::declarationId)
                        .toList());
        assertEquals(List.of("json"), resolved.get("test:item").sources().get(0).replacedSourceIds());
    }

    @Test
    void damagedScalingUsesExactFloorWithoutMultiplicationOverflow() {
        assertEquals(Long.MAX_VALUE / 3, ItemCompositionScaling.scale(Long.MAX_VALUE, 1, 3));
        assertEquals(2, ItemCompositionScaling.scale(5, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> ItemCompositionScaling.scale(10, 2, 1));
    }

    @Test
    void damagedScalingPreservesAnInvalidPrimaryWhenOnlySecondaryMaterialRemains() {
        var composition = new ItemComposition(
                null,
                List.of(MaterialComponent.of(Materials.Iron, 1), MaterialComponent.of(Materials.Copper, 100)),
                Materials.Iron,
                List.of(new ItemCompositionSource(ItemCompositionSource.Kind.EXPLICIT, "probe", "test", "", "")),
                true);

        var scaled = ItemCompositionScaling.scale(composition, 1, 100);

        assertEquals(List.of(MaterialComponent.of(Materials.Copper, 1)), scaled.components());
        assertFalse(scaled.material().isValid());
    }

    @Test
    void legacySingleComponentDeclarationsStillInferTheirPrimary() {
        var composition =
                new ItemComposition(null, List.of(MaterialComponent.of(Materials.Iron, 4)), "legacy probe", true);

        assertEquals(Materials.Iron.resolve(), composition.material());
    }

    private static ItemCompositionCandidate candidate(
            String id,
            ItemCompositionSource.Kind kind,
            gregtech.api.material.GTMaterial material,
            long amount,
            boolean bulkRecovery) {
        return new ItemCompositionCandidate(composition(id, kind, material, amount, bulkRecovery), null);
    }

    private static ItemComposition composition(
            String id,
            ItemCompositionSource.Kind kind,
            gregtech.api.material.GTMaterial material,
            long amount,
            boolean bulkRecovery) {
        return new ItemComposition(
                null,
                List.of(MaterialComponent.of(material, amount)),
                material,
                List.of(new ItemCompositionSource(kind, id, "test", "", "")),
                bulkRecovery);
    }
}
