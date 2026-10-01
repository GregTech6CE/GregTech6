package com.gregtech.gregtech.data;

import java.util.LinkedHashMap;
import java.util.Map;

/** Non-OreDict GT item ids from GT6 IL.java. Auto-transpiled skeleton. */
public class ItemReferences {
    protected ItemReferences() {}

    public record ItemEntry(String id) {}

    private static final Map<String, ItemEntry> REGISTRY = new LinkedHashMap<>();

    private static ItemEntry item(String field) {
        ItemEntry entry = new ItemEntry(field);
        REGISTRY.put(field, entry);
        return entry;
    }

    public static final ItemEntry
            Wrench = item("Wrench"),
            Screwdriver = item("Screwdriver"),
            HardHammer = item("HardHammer"),
            SoftHammer = item("SoftHammer"),
            Crowbar = item("Crowbar"),
            SolderingTool = item("SolderingTool");

    public static Map<String, ItemEntry> all() {
        return Map.copyOf(REGISTRY);
    }

    public static void bootstrap() {
        if (REGISTRY.isEmpty()) {
            throw new IllegalStateException("IL failed to initialize");
        }
    }
}
