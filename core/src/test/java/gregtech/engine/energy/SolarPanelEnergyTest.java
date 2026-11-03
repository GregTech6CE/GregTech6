package gregtech.engine.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class SolarPanelEnergyTest {
    private static final SolarPanelEnergy.Conditions CLEAR_DAY =
            new SolarPanelEnergy.Conditions(true, false, false, false, 0);

    @Test
    void distantRoofChangesRefreshWithinOneSecondWithoutNeighbourUpdates() {
        for (int offset : new int[] {-100, 0, 1, 19, 100}) {
            var solar = new SolarPanelEnergy(32, offset);
            solar.tick(() -> false, CLEAR_DAY, energy -> 0);
            assertFalse(solar.active());

            for (int i = 0; i < 20; i++) solar.tick(() -> true, CLEAR_DAY, energy -> 0);
            assertTrue(solar.active());
            assertEquals(32, solar.energy());

            for (int i = 0; i < 20; i++) solar.tick(() -> false, CLEAR_DAY, energy -> 0);
            assertFalse(solar.active());
            assertEquals(0, solar.energy());
        }
    }

    @Test
    void periodicChecksAreStaggeredAndDoNotRunEveryTick() {
        var panels = new SolarPanelEnergy[20];
        for (int i = 0; i < panels.length; i++) {
            panels[i] = new SolarPanelEnergy(32, i);
            panels[i].tick(() -> true, CLEAR_DAY, energy -> 0);
        }
        for (int tick = 0; tick < 40; tick++) {
            var checks = new AtomicInteger();
            for (var panel : panels)
                panel.tick(
                        () -> {
                            checks.incrementAndGet();
                            return true;
                        },
                        CLEAR_DAY,
                        energy -> 0);
            assertEquals(1, checks.get(), "Only one of twenty offsets should refresh on each tick");
        }
    }

    @Test
    void neighbourChangesAndReenablingRefreshImmediately() {
        var solar = new SolarPanelEnergy(32);
        solar.tick(() -> false, CLEAR_DAY, energy -> 0);
        solar.checkSky();
        solar.tick(() -> true, CLEAR_DAY, energy -> 0);
        assertTrue(solar.active());

        solar.enabled(false);
        solar.tick(
                () -> {
                    throw new AssertionError("Disabled panels should not query the world");
                },
                CLEAR_DAY,
                energy -> 0);
        solar.enabled(true);
        solar.tick(() -> false, CLEAR_DAY, energy -> 0);
        assertFalse(solar.active());
    }
}
