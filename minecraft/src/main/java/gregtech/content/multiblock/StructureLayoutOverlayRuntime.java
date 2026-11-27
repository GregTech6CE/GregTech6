package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.LayoutCell;
import gregtech.api.multiblock.StructureLayout;
import gregtech.content.registry.MultiblockRegistration;
import gregtech.engine.addon.AddonMachines;
import gregtech.engine.kubejs.StructureLayoutDocuments;
import gregtech.platform.Mc;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/** Reloads addon structure layouts from {@code gt6ce_layouts} and keeps the text sent to clients. */
public final class StructureLayoutOverlayRuntime {
    private static final Logger LOGGER = Mc.logger();
    private static volatile Map<String, String> documents = Map.of();
    private static volatile Set<String> lastAffected = Set.of();
    private static int logged;

    private StructureLayoutOverlayRuntime() {}

    public static Map<String, String> documents() {
        return documents;
    }

    /** Machine names whose layout changed in the most recent install. */
    public static Set<String> lastAffected() {
        return lastAffected;
    }

    /** Server datapack pass. Restores baselines, then applies the files that remain, in id order. */
    public static void apply(MinecraftServer server) {
        var found = new TreeMap<String, String>();
        var listed = new ArrayList<>(server.getResourceManager()
                .listResources("gt6ce_layouts", id -> id.getPath().endsWith(".json"))
                .entrySet());
        listed.sort(Comparator.comparing(entry -> entry.getKey().toString()));
        for (var entry : listed) {
            String machine = machine(entry.getKey());
            if (machine == null) {
                warn(entry.getKey() + " is not one machine type");
                continue;
            }
            try (var in = entry.getValue().open()) {
                var layout =
                        StructureLayoutDocuments.parse(machine, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                if (!accept(entry.getKey().toString(), layout)) continue;
                found.put(machine, StructureLayoutDocuments.canonical(layout));
            } catch (RuntimeException | IOException failure) {
                warn(entry.getKey() + " " + failure.getMessage());
            }
        }
        install(found);
        LOGGER.info("[gregtech] Structure layout overlay replaced {}", documents.size());
    }

    /** Client copy of the server overlay. Unknown files are dropped instead of crashing the game. */
    public static void applySynced(Map<String, String> synced) {
        install(synced == null ? Map.of() : synced);
    }

    private static void install(Map<String, String> next) {
        var layouts = new LinkedHashMap<String, SharedLargeMachineLayouts.Layout>();
        var accepted = new TreeMap<String, String>();
        for (var entry : next.entrySet()) {
            try {
                var layout = StructureLayoutDocuments.parse(entry.getKey(), entry.getValue());
                if (!accept(entry.getKey(), layout)) continue;
                layouts.put(entry.getKey(), AddonMachines.sharedLayout(entry.getKey(), layout));
                accepted.put(entry.getKey(), StructureLayoutDocuments.canonical(layout));
            } catch (RuntimeException failure) {
                warn(entry.getKey() + " " + failure.getMessage());
            }
        }
        synchronized (ControllerStructureLayouts.class) {
            var affected = new HashSet<>(SharedLargeMachineLayouts.overlaid());
            SharedLargeMachineLayouts.overlayLayouts(layouts);
            affected.addAll(accepted.keySet());
            lastAffected = Set.copyOf(affected);
            LargeMachineLayouts.forget(affected);
            ControllerStructureLayouts.invalidate();
            documents = Map.copyOf(accepted);
        }
    }

    private static boolean accept(String source, StructureLayout layout) {
        if (!SharedLargeMachineLayouts.replaceable(layout.name())) {
            warn(source + " does not replace an addon layout");
            return false;
        }
        for (var cell : allCells(layout)) {
            if (!known(cell.match())) {
                warn(source + " uses an unknown block in " + cell.match());
                return false;
            }
        }
        MultiblockRegistration.validatePorts(layout.name(), AddonMachines.sharedLayout(source, layout));
        return true;
    }

    private static List<LayoutCell> allCells(StructureLayout layout) {
        var out = new ArrayList<>(layout.cells());
        for (var repeat : layout.repeats()) out.addAll(repeat.cells());
        return out;
    }

    private static boolean known(CellMatch match) {
        if (match instanceof CellMatch.Block block) return Mc.hasBlock(ResourceLocation.parse(block.id()));
        if (match instanceof CellMatch.OneOf one) {
            for (CellMatch member : one.members()) if (!known(member)) return false;
        }
        return true;
    }

    private static String machine(ResourceLocation id) {
        String path = id.getPath();
        String prefix = "gt6ce_layouts/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) return null;
        String machine = path.substring(prefix.length(), path.length() - ".json".length());
        if (machine.isEmpty() || machine.indexOf('/') >= 0) return null;
        return machine;
    }

    private static void warn(String message) {
        if (logged++ < 20) LOGGER.warn("[gregtech] Structure layout overlay {}", message);
    }
}
