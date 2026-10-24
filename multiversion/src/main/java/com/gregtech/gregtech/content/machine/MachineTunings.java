package com.gregtech.gregtech.content.machine;

import com.gregtech.gregtech.api.addon.MachineTuning;
import com.gregtech.gregtech.api.addon.MachineTuningContributor;
import com.gregtech.gregtech.api.addon.MachineTuningRules;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/** Built-in machine tunings loaded before the catalog is converted into specs. */
public final class MachineTunings {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static Map<MachineTuningRules.Key, MachineTuningRules.Merged> applied = Map.of();
    private static boolean collected;

    private MachineTunings() {}

    public static synchronized void collect() {
        if (collected) return;
        collected = true;
        var declared = new ArrayList<MachineTuningRules.Declared>();
        ClassLoader loader = MachineTuningContributor.class.getClassLoader();
        for (MachineTuningContributor contributor : ServiceLoader.load(MachineTuningContributor.class, loader)) {
            String addon;
            try {
                addon = contributor.addonId();
            } catch (RuntimeException | Error failure) {
                throw com.gregtech.gregtech.api.addon.AddonFailures.of("Machine tuning contributor failed: "
                        + contributor.getClass().getName(), failure);
            }
            List<MachineTuning> tunings;
            try {
                tunings = contributor.tunings();
            } catch (RuntimeException | Error failure) {
                throw com.gregtech.gregtech.api.addon.AddonFailures.of(
                        "Addon '" + addon + "' machine tunings failed", failure);
            }
            if (tunings == null) continue;
            for (MachineTuning tuning : tunings) declared.add(new MachineTuningRules.Declared(addon, tuning));
        }
        applied = MachineTuningRules.merge(declared);
        for (String line : MachineTuningRules.describe(applied)) LOGGER.info("[gt6ce] tuning: {}", line);
        if (!applied.isEmpty()) writeReport();
    }

    @Nullable
    public static MachineTuningRules.Merged find(String machineName, int tier) {
        return applied.get(new MachineTuningRules.Key(machineName, tier));
    }

    public static Set<MachineTuningRules.Key> keys() { return applied.keySet(); }

    /** Fails startup when a declared key never hit a built-in spec. */
    public static void requireMatched(Set<MachineTuningRules.Key> matched) {
        for (MachineTuningRules.Key key : applied.keySet())
            if (!matched.contains(key))
                throw new IllegalStateException("Machine tuning target not found: " + key);
    }

    public static List<String> reportLines() { return MachineTuningRules.describe(applied); }

    private static void writeReport() {
        try {
            Path directory = configDirectory();
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("gt6ce-tuning.txt"),
                    "# GregTech 6 Community Edition machine tuning\n"
                            + String.join(System.lineSeparator(), reportLines()) + System.lineSeparator());
        } catch (IOException failure) {
            LOGGER.warn("[gt6ce] could not write machine tuning report", failure);
        }
    }

    private static Path configDirectory() {
//? if >=1.21.1 {
        /*return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
*///?} else {
        return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
//?}
    }
}
