/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original Loader_MultiTileEntities.sensors identities and getSensorDescription keys. */
package com.gregtech.gregtech.api.sensor;

import java.util.List;

public final class SensorCatalog {
    private SensorCatalog() {}
    public record Entry(String id, String kind, int originalId, String description) {
        public String descriptionKey() { return "gt.tooltip.sensor." + description; }
    }
    public static final List<Entry> ALL = List.of(
            new Entry("sensor_fluidometer", "FLUID", 31006, "fluidometer"),
            new Entry("sensor_itemometer", "ITEM", 31004, "itemometer"),
            new Entry("sensor_electrometer", "ENERGY", 31015, "electrometer"),
            new Entry("sensor_progressmeter", "PROGRESS", 31018, "progressmeter"),
            new Entry("sensor_thermometer", "THERMOMETER", 31000, "thermometer"),
            new Entry("sensor_tachometer", "TACHOMETER", 31019, "tachometer"),
            new Entry("sensor_weightometric", "WEIGHTOMETRIC", 31012, "heavyweightometer"),
            new Entry("sensor_weightometer_light", "WEIGHTOMETRIC_LIGHT", 31010, "lightweightometer"),
            new Entry("sensor_weightometer_medium", "WEIGHTOMETRIC_MEDIUM", 31011, "mediumweightometer"),
            new Entry("sensor_weightometer_super_heavy", "WEIGHTOMETRIC_SUPER_HEAVY", 31013, "superheavyweightometer"),
            new Entry("sensor_bucketometer", "BUCKETOMETER", 31007, "bucketometer"),
            new Entry("sensor_kilobucketometer", "KILOBUCKETOMETER", 31022, "kilobucketometer"),
            new Entry("sensor_gibblometer", "GIBBLOMETER", 31001, "gibblometer"),
            new Entry("sensor_stackometer", "STACKOMETER", 31005, "stackometer"),
            new Entry("sensor_luminometer", "LUMINOMETER", 31002, "luminometer"),
            new Entry("sensor_playercounter", "PLAYERCOUNTER", 31017, "playercounter"),
            new Entry("sensor_chronometer", "CHRONOMETER", 31003, "chronometer"),
            new Entry("sensor_geiger", "GEIGER", 31020, "geigercounter"),
            new Entry("sensor_laserometer", "LASEROMETER", 31021, "laserometer"),
            new Entry("sensor_tpsmeter", "TPS", 31016, "tpsmeter"),
            new Entry("sensor_kilogibblometer", "KILOGIBBLOMETER", 31023, "kilogibblometer"));

    public static Entry byKind(String kind) {
        return ALL.stream().filter(entry -> entry.kind().equals(kind)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sensor kind: " + kind));
    }
}
