package com.gregtech.gregtech.content.energy;

import java.util.List;

/** Migration of port-only battery IDs to GT6 Loader_MultiTileEntities 14000..14044.
 * The first four keep their voltage and fit the entire former capacity.
 * GT6 has no IV chemical battery; its port-only item becomes the highest source tier. */
public final class BatteryItemMigration {
    public record Alias(String oldId, ChemicalBatterySpec target, long oldCapacity) {}
    public static final List<Alias> ALIASES = List.of(
            nickel("battery_lv", 1, 100_000),
            nickel("battery_mv", 2, 400_000),
            nickel("battery_hv", 3, 1_600_000),
            nickel("battery_ev", 4, 6_400_000),
            new Alias("battery_iv", new ChemicalBatterySpec(ChemicalBatterySpec.Chemistry.LITHIUM_COBALT, 4), 25_600_000));

    private BatteryItemMigration() {}
    private static Alias nickel(String id, int tier, long capacity) {
        return new Alias(id, new ChemicalBatterySpec(ChemicalBatterySpec.Chemistry.NICKEL_CADMIUM, tier), capacity);
    }
    public static ChemicalBatterySpec target(String oldId) {
        return ALIASES.stream().filter(alias -> alias.oldId().equals(oldId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown old battery " + oldId)).target();
    }
}
