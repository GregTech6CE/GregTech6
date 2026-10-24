package gregtech.api.addon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Merges addon machine tunings and rejects duplicate or conflicting declarations. */
public final class MachineTuningRules {
    /** Keeps a tuned batch small enough that {@code parallel * outputCount} stays far from int overflow. */
    public static final int MAX_PARALLEL = 1 << 16;
    private static final List<String> FIELDS = List.of(
            "parallelLimit", "energyIn", "energyInMin", "energyInMax",
            "processingEfficiency", "constantEnergy", "cheapOverclocking");

    private MachineTuningRules() {}

    public record Key(String machineName, int tier) {
        public Key {
            Objects.requireNonNull(machineName, "machineName");
        }
        @Override public String toString() { return machineName + "/T" + tier; }
    }

    public record Declared(String addonId, MachineTuning tuning) {}

    public record Merged(MachineTuning tuning, Map<String, String> owners) {}

    public static Map<Key, Merged> merge(List<Declared> declared) {
        if (declared == null) return Map.of();
        var builders = new LinkedHashMap<Key, Builder>();
        for (Declared row : declared) {
            if (row == null || row.tuning() == null)
                throw new IllegalStateException("Machine tuning contributor returned a null tuning");
            String addon = row.addonId();
            if (addon == null || !addon.matches("[a-z0-9_.-]+"))
                throw new IllegalStateException("Machine tuning has no valid addon id: " + addon);
            MachineTuning tuning = row.tuning();
            Key key = new Key(tuning.machineName(), tuning.tier());
            Builder builder = builders.computeIfAbsent(key, ignored -> new Builder());
            builder.put("parallelLimit", tuning.parallelLimit(), addon, key);
            builder.put("energyIn", tuning.energyIn(), addon, key);
            builder.put("energyInMin", tuning.energyInMin(), addon, key);
            builder.put("energyInMax", tuning.energyInMax(), addon, key);
            builder.put("processingEfficiency", tuning.processingEfficiency(), addon, key);
            builder.put("constantEnergy", tuning.constantEnergy(), addon, key);
            builder.put("cheapOverclocking", tuning.cheapOverclocking(), addon, key);
        }
        var merged = new LinkedHashMap<Key, Merged>();
        // Builder.build() constructs a canonical MachineTuning, so a merged row is range-checked there.
        for (var entry : builders.entrySet()) merged.put(entry.getKey(), entry.getValue().build(entry.getKey()));
        return Collections.unmodifiableMap(merged);
    }

    /** Bounds the effective values that the canonical spec constructor does not check. */
    public static void checkEffectiveValues(Key key, long energyIn, int parallel) {
        if (parallel < 1) throw new IllegalStateException("Invalid tuned parallel for " + key + ": " + parallel);
        if (parallel > MAX_PARALLEL)
            throw new IllegalStateException("Tuned parallel for " + key + " exceeds " + MAX_PARALLEL);
        if (energyIn > Long.MAX_VALUE / parallel)
            throw new IllegalStateException("Tuned energy for " + key + " overflows with parallel " + parallel);
    }

    /** One deterministic line per field, for startup logs and the config report. */
    public static List<String> describe(Map<Key, Merged> applied) {
        var lines = new ArrayList<String>();
        for (var entry : applied.entrySet()) {
            Key key = entry.getKey();
            Merged merged = entry.getValue();
            for (String field : FIELDS) {
                if (!merged.owners().containsKey(field)) continue;
                lines.add(key + " " + field + "=" + value(merged.tuning(), field)
                        + " (" + merged.owners().get(field) + ")");
            }
        }
        return lines;
    }

    private static Object value(MachineTuning tuning, String field) {
        return switch (field) {
            case "parallelLimit" -> tuning.parallelLimit();
            case "energyIn" -> tuning.energyIn();
            case "energyInMin" -> tuning.energyInMin();
            case "energyInMax" -> tuning.energyInMax();
            case "processingEfficiency" -> tuning.processingEfficiency();
            case "constantEnergy" -> tuning.constantEnergy();
            case "cheapOverclocking" -> tuning.cheapOverclocking();
            default -> throw new IllegalArgumentException("Unknown tuning field: " + field);
        };
    }

    private static final class Builder {
        private final Map<String, Object> values = new LinkedHashMap<>();
        private final Map<String, String> owners = new LinkedHashMap<>();

        private void put(String field, Object value, String addon, Key key) {
            if (value == null) return;
            if (values.containsKey(field)) {
                String owner = owners.get(field);
                Object old = values.get(field);
                if (owner.equals(addon))
                    throw new IllegalStateException("Addon '" + addon + "' declares " + key + " " + field + " twice");
                if (!old.equals(value))
                    throw new IllegalStateException("Machine tuning conflict at " + key + " " + field
                            + ": " + owner + "=" + old + ", " + addon + "=" + value);
                return;
            }
            if ("parallelLimit".equals(field) && (Integer) value > MAX_PARALLEL)
                throw new IllegalStateException("Tuned parallel for " + key + " exceeds " + MAX_PARALLEL);
            values.put(field, value);
            owners.put(field, addon);
        }

        private Merged build(Key key) {
            var tuning = new MachineTuning(key.machineName(), key.tier(),
                    (Integer) values.get("parallelLimit"),
                    (Long) values.get("energyIn"), (Long) values.get("energyInMin"), (Long) values.get("energyInMax"),
                    (Integer) values.get("processingEfficiency"),
                    (Boolean) values.get("constantEnergy"), (Boolean) values.get("cheapOverclocking"));
            return new Merged(tuning, Collections.unmodifiableMap(new LinkedHashMap<>(owners)));
        }
    }
}
