package com.gregtech.gregtech.core;

import com.gregtech.gregtech.content.energy.SolarPanelEnergy;
import com.gregtech.gregtech.content.energy.SolarPanelEnergy.Conditions;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Fixed samples from GT6 MultiTileEntitySolarPanelElectric, without a game client. */
public final class SolarPanelEnergyContracts {
    private static int assertions;
    private static final Conditions DAY = new Conditions(true, false, false, false, 0.8f);
    private static final Conditions NIGHT = new Conditions(false, false, false, false, 0.8f);
    private static final Conditions WET_NIGHT = new Conditions(false, true, false, false, 0.8f);

    public static void main(String[] args) {
        System.out.println("Solar panel contracts passed: " + verify() + " checks");
    }

    public static int verify() {
        assertions = 0;
        weatherGoldens();
        oneTickOutput();
        cachedSkyAndSwitch();
        packetExtractionAndMigration();
        return assertions;
    }

    private static void weatherGoldens() {
        // Source generateEnergy: sky -> thunder -> Twilight Forest -> day/night -> biome rain.
        Conditions[] weather = {
                DAY, new Conditions(true, true, false, false, 0.8f), NIGHT, WET_NIGHT,
                new Conditions(true, true, false, false, 0),
                new Conditions(false, true, false, false, 0),
                new Conditions(true, true, true, false, 0.8f),
                new Conditions(false, false, true, false, 0),
                new Conditions(true, true, false, true, 0.8f),
                new Conditions(false, true, false, true, 0.8f),
                new Conditions(true, false, true, true, 0.8f)};
        long[] silicon = {8, 1, 1, 0, 8, 1, 0, 0, 4, 4, 0};
        long[] germanium = {16, 2, 2, 0, 16, 2, 0, 0, 8, 8, 0};
        for (int i = 0; i < weather.length; i++) {
            equal(silicon[i], SolarPanelEnergy.generation(8, true, weather[i]), "Silicon source weather " + i);
            equal(germanium[i], SolarPanelEnergy.generation(16, true, weather[i]), "Germanium source weather " + i);
        }
        equal(0, SolarPanelEnergy.generation(8, false, DAY), "Obstructed sky prevents daytime generation");
        equal(0, SolarPanelEnergy.generation(16, false, weather[9]), "Twilight Forest still requires sky");
    }

    private static void oneTickOutput() {
        var solar = new SolarPanelEnergy(8);
        for (int i = 0; i < 500; i++) solar.tick(() -> true, DAY, energy -> 0);
        equal(8, solar.energy(), "Rejected energy never accumulates across ticks");
        check(solar.active() && !solar.emitting(), "Work texture follows generation even without a receiver");
        var calls = new AtomicInteger();
        var packet = new AtomicLong();
        solar.tick(() -> true, NIGHT, energy -> { calls.incrementAndGet(); packet.set(energy); return 1; });
        equal(1, packet.get(), "Clear night emits one actual 1-EU packet, not an 8-EU packet");
        equal(1, calls.get(), "One current-tick emission");
        equal(0, solar.energy(), "Successful emission clears current output");
        check(solar.active() && solar.emitting(), "Generating and emitting are independent running flags");
        solar.tick(() -> true, WET_NIGHT, energy -> { calls.incrementAndGet(); return 1; });
        equal(1, calls.get(), "Rainy night does not call the network emitter");
        equal(0, solar.energy(), "A non-generating tick replaces prior energy");
        check(!solar.active() && !solar.emitting(), "Rainy-night work flags clear");
        var germanium = new SolarPanelEnergy(16);
        germanium.tick(() -> true, new Conditions(true, true, false, false, 0.8f), energy -> { packet.set(energy); return 0; });
        equal(2, packet.get(), "Rainy day supplies the actual 2-EU Germanium packet");
        equal(2, germanium.energy(), "Rejected low-output packet remains available for this tick");
    }

    private static void cachedSkyAndSwitch() {
        var solar = new SolarPanelEnergy(8);
        var skyChecks = new AtomicInteger();
        java.util.function.BooleanSupplier sky = () -> { skyChecks.incrementAndGet(); return true; };
        for (int i = 1; i <= 604; i++) solar.tick(sky, DAY, energy -> 0);
        equal(2, skyChecks.get(), "Source checks sky first tick and at tick 5, then caches it");
        solar.tick(sky, DAY, energy -> 0);
        equal(3, skyChecks.get(), "Source 600-tick refresh at tick 605");
        solar.checkSky();
        solar.tick(() -> { skyChecks.incrementAndGet(); return false; }, DAY, energy -> 0);
        equal(4, skyChecks.get(), "Neighbor update checks sky on the next tick");
        equal(0, solar.energy(), "A newly obstructed neighbor invalidates generation");

        var stopped = new SolarPanelEnergy(8);
        stopped.tick(() -> true, DAY, energy -> 0);
        check(!stopped.enabled(false) && !stopped.enabled(), "Switch disables generation");
        stopped.tick(() -> { throw new AssertionError("Stopped sky was queried"); }, WET_NIGHT,
                energy -> { throw new AssertionError("Stopped panel emitted"); });
        equal(8, stopped.energy(), "Source retains its current output while stopped");
        check(stopped.active() && !stopped.emitting(), "Source retains running flags while stopped");
        check(stopped.enabled(true), "Switch re-enables the panel");
        var resumedChecks = new AtomicInteger();
        stopped.tick(() -> { resumedChecks.incrementAndGet(); return false; }, DAY, energy -> 0);
        equal(1, resumedChecks.get(), "Re-enabling immediately requests a new sky check");
        equal(0, stopped.energy(), "Re-enabled blocked panel cannot keep old energy");
        check(!stopped.active(), "Blocked re-enabled panel clears work texture");
    }

    private static void packetExtractionAndMigration() {
        var solar = new SolarPanelEnergy(8);
        solar.tick(() -> true, DAY, energy -> 0);
        equal(2, solar.extract(4, 2, false), "Source whole-request simulation");
        equal(8, solar.energy(), "Simulation keeps current energy");
        equal(0, solar.extract(4, 3, true), "Oversized request does not partially extract");
        equal(8, solar.energy(), "Rejected extraction keeps energy");
        equal(0, solar.extract(0, 1, true), "Zero-sized request rejected");
        equal(0, solar.extract(Long.MIN_VALUE, 1, true), "Unrepresentable absolute packet rejected");
        equal(0, solar.extract(2, Long.MAX_VALUE, true), "Multiplication overflow cannot create energy");
        equal(0, solar.extract(1, -1, true), "Negative packet count rejected");
        equal(0, solar.extract(1, 0, true), "Zero packet count rejected");
        equal(2, solar.extract(-4, 2, true), "Source takes the magnitude of a negative packet");
        equal(0, solar.energy(), "Exact requested extraction consumes the whole output");
        check(solar.active() && !solar.emitting(), "Pulling energy does not rewrite source push-emission flags");
        solar.restore(256, true, false, true);
        equal(8, solar.energy(), "Old port accumulator migrates to one rated tick");
        check(!solar.enabled() && solar.active() && !solar.emitting(), "Saved source flags restored independently");
        equal(0, solar.extract(1, 9, true), "Old phantom buffer cannot be extracted");
        equal(8, solar.extract(1, 8, true), "Source retains pull access to residual output while stopped");
        solar.restore(-1, false, false, false);
        equal(0, solar.energy(), "Corrupt negative saved energy clamps to zero");
        var germanium = new SolarPanelEnergy(16);
        germanium.restore(512, true, false, false);
        equal(16, germanium.energy(), "Old Germanium buffer migrates to its own rated tick");
    }

    private static void equal(long expected, long actual, String message) {
        check(expected == actual, message + ": expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
