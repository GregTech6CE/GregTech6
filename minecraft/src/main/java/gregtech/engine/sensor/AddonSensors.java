package gregtech.engine.sensor;

import gregtech.api.addon.AddonFailures;
import gregtech.api.addon.AddonNames;
import gregtech.api.addon.platform.AddonSensor;
import gregtech.api.addon.platform.AddonSensorContributor;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/** Sensor kinds declared by addons. Collected before block registration. */
public final class AddonSensors {
    public record Declared(String addonId, AddonSensor sensor) {}

    private static final List<Declared> ALL = new ArrayList<>();
    private static final Map<String, AddonSensor> BY_ID = new LinkedHashMap<>();
    private static final List<Supplier<? extends Block>> BLOCKS = new ArrayList<>();
    private static boolean collected;
    private static boolean collecting;

    private AddonSensors() {}

    /**
     * Loads every sensor kind. The registry is published only after the whole pass succeeds, so a failure
     * leaves it empty and a later call can load it again instead of keeping a half-built map.
     */
    public static synchronized void collect() {
        if (collected || collecting) return;
        collecting = true;
        try {
            var all = new ArrayList<Declared>();
            var byId = new LinkedHashMap<String, AddonSensor>();
            ClassLoader loader = AddonSensorContributor.class.getClassLoader();
            var seen = new LinkedHashMap<String, String>();
            for (AddonSensorContributor contributor : ServiceLoader.load(AddonSensorContributor.class, loader)) {
                String addon;
                try {
                    addon = contributor.addonId();
                } catch (RuntimeException | Error failure) {
                    throw AddonFailures.of("Sensor contributor failed: " + contributor.getClass().getName(), failure);
                }
                if (!AddonNames.isAddonId(addon))
                    throw new IllegalStateException("Sensor contributor has no valid addon id: "
                            + contributor.getClass().getName());
                List<AddonSensor> kinds;
                try {
                    kinds = contributor.sensorKinds();
                } catch (RuntimeException | Error failure) {
                    throw AddonFailures.of("Addon '" + addon + "' sensor kinds failed", failure);
                }
                if (kinds == null) continue;
                for (AddonSensor sensor : kinds) {
                    if (sensor == null) throw new IllegalStateException("Addon '" + addon + "' declares a null sensor");
                    String id = sensor.id();
                    if (id == null || !id.matches("[a-z0-9_]+"))
                        throw new IllegalStateException("Addon '" + addon + "' has an invalid sensor id: " + id);
                    if (sensor.titleKey() == null || sensor.titleKey().isBlank())
                        throw new IllegalStateException("Addon '" + addon + "' sensor '" + id + "' has no title key");
                    if (!SensorPanelRules.hasUnitGlyph(sensor.unit()))
                        throw new IllegalStateException("Addon '" + addon + "' sensor '" + id
                                + "' has an unknown unit glyph: " + sensor.unit());
                    if (sensor.color() < 0 || sensor.color() > 0xFFFFFF)
                        throw new IllegalStateException("Addon '" + addon + "' sensor '" + id + "' has an invalid color");
                    if (sensor.maximum() < 0)
                        throw new IllegalStateException("Addon '" + addon + "' sensor '" + id
                                + "' has a negative maximum");
                    String key = addon + ":" + id;
                    String previous = seen.putIfAbsent(key, addon);
                    if (previous != null)
                        throw new IllegalStateException("Addon '" + addon + "' declares sensor '" + id + "' twice");
                    if (byId.putIfAbsent(id, sensor) != null)
                        throw new IllegalStateException("Sensor id '" + id + "' is declared by two addons");
                    all.add(new Declared(addon, sensor));
                }
            }
            ALL.addAll(all);
            BY_ID.putAll(byId);
            collected = true;
        } finally {
            collecting = false;
        }
    }

    public static List<Declared> all() { return List.copyOf(ALL); }

    @Nullable
    public static AddonSensor find(String id) { return BY_ID.get(id); }

    public static List<Supplier<? extends Block>> blocks() { return List.copyOf(BLOCKS); }

    public static void addBlock(Supplier<? extends Block> block) { BLOCKS.add(block); }
}
