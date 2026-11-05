package gregtech.content.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.MaterialTestFixture;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class MaterialCrateDistributionTest {
    private static final double EPSILON = 1e-9;

    @BeforeAll
    static void initializeMaterials() {
        MaterialTestFixture.ensureInitialized();
    }

    @Test
    void fiveCategoriesSumToOne() {
        var categories = MaterialCrateDistribution.categories(entry -> true);
        assertEquals(5, categories.length);
        assertEquals(
                List.of("metals", "dusts", "gems", "woods", "ores"),
                List.of(
                        categories[0].name(),
                        categories[1].name(),
                        categories[2].name(),
                        categories[3].name(),
                        categories[4].name()));
        for (var category : categories) {
            assertEquals(1.0, category.total(), EPSILON, category.name());
        }
    }

    @Test
    void metalFormsKeepTheirMultiplicity() {
        var metals = MaterialCrateDistribution.categories(entry -> true)[0];
        assertEquals(1.0 / 3, sizeWeight(metals, 64), EPSILON);
        assertEquals(2.0 / 3, sizeWeight(metals, 16), EPSILON);
        assertEquals(2.0 / 12, weight(metals, MaterialPrefix.dust, 16), EPSILON);
        assertEquals(4.0 / 12, weight(metals, MaterialPrefix.ingot, 16), EPSILON);
        assertEquals(2.0 / 12, weight(metals, MaterialPrefix.plate, 16), EPSILON);
        assertEquals(1.0 / 12, weight(metals, MaterialPrefix.dust, 64), EPSILON);
        assertEquals(1.0 / 12, weight(metals, MaterialPrefix.plate, 64), EPSILON);
        assertEquals(2.0 / 12, weight(metals, MaterialPrefix.ingot, 64), EPSILON);
    }

    @Test
    void dustAndOreUseTheRoundedFortyPercentFullOdds() {
        var categories = MaterialCrateDistribution.categories(entry -> true);
        assertEquals(0.40, sizeWeight(categories[1], 64), EPSILON);
        assertEquals(0.60, sizeWeight(categories[1], 16), EPSILON);
        assertEquals(0.40, sizeWeight(categories[4], 64), EPSILON);
        assertEquals(0.60, sizeWeight(categories[4], 16), EPSILON);
    }

    @Test
    void gemsUseTheRoundedTwentyPercentFullOdds() {
        var gems = MaterialCrateDistribution.categories(entry -> true)[2];
        assertEquals(0.20, sizeWeight(gems, 64), EPSILON);
        assertEquals(0.80, sizeWeight(gems, 16), EPSILON);
    }

    @Test
    void woodIsAlwaysSixtyFour() {
        var woods = MaterialCrateDistribution.categories(entry -> true)[3];
        assertEquals(1.0, sizeWeight(woods, 64), EPSILON);
        assertEquals(0.0, sizeWeight(woods, 16), EPSILON);
    }

    @Test
    void expectedUnitsFollowTheRoundedOdds() {
        // The exact per-crate average is 29/72 (dust/ore) and 29/144 (gems); the implementation
        // rounds those to 40%/20%, which these figures pin.
        var categories = MaterialCrateDistribution.categories(entry -> true);
        assertEquals(32.0, expectedUnits(categories[0]), EPSILON);
        assertEquals(35.2, expectedUnits(categories[1]), EPSILON);
        assertEquals(25.6, expectedUnits(categories[2]), EPSILON);
        assertEquals(64.0, expectedUnits(categories[3]), EPSILON);
        assertEquals(35.2, expectedUnits(categories[4]), EPSILON);
        assertEquals(
                38.4,
                (expectedUnits(categories[0])
                                + expectedUnits(categories[1])
                                + expectedUnits(categories[2])
                                + expectedUnits(categories[3])
                                + expectedUnits(categories[4]))
                        / 5,
                EPSILON);
    }

    @Test
    void materialMultiplicityIsTheWeight() {
        var metals = MaterialCrateDistribution.categories(entry -> true)[0];
        assertEquals(2.0 / 12, materialWeight(metals, "Copper"), EPSILON);
        assertEquals(1.0 / 12, materialWeight(metals, "Tin"), EPSILON);
        assertEquals(3.0 / 12, materialWeight(metals, "Iron"), EPSILON);
    }

    @Test
    void porcelainIsInTheDustTable() {
        var dusts = MaterialCrateDistribution.categories(entry -> true)[1];
        assertEquals(1.0 / 31, materialWeight(dusts, "Porcelain"), EPSILON);
    }

    @Test
    void unavailableEntriesAreRemovedAndTheTotalIsRecomputed() {
        var categories = MaterialCrateDistribution.categories(entry -> entry.size() == 16);
        assertEquals(0.60, categories[1].total(), EPSILON);
        assertEquals(0.0, categories[3].total(), EPSILON);

        var noCopper = MaterialCrateDistribution.categories(
                entry -> !entry.material().getName().equals("Copper"));
        assertTrue(noCopper[0].total() < 1.0);
        assertEquals(0.0, materialWeight(noCopper[0], "Copper"), EPSILON);
    }

    private static double sizeWeight(MaterialCrateDistribution.Category category, int size) {
        double sum = 0;
        for (var entry : category.entries()) {
            if (entry.size() == size) sum += entry.weight();
        }
        return sum;
    }

    private static double weight(MaterialCrateDistribution.Category category, MaterialPrefix prefix, int size) {
        double sum = 0;
        for (var entry : category.entries()) {
            if (entry.prefix() == prefix && entry.size() == size) sum += entry.weight();
        }
        return sum;
    }

    private static double materialWeight(MaterialCrateDistribution.Category category, String material) {
        double sum = 0;
        for (var entry : category.entries()) {
            if (entry.material().getName().equals(material)) sum += entry.weight();
        }
        return sum;
    }

    private static double expectedUnits(MaterialCrateDistribution.Category category) {
        double sum = 0;
        for (var entry : category.entries()) {
            sum += entry.weight() * entry.size();
        }
        return sum;
    }
}
