package gregtech.api.addon;

import gregtech.api.machine.ItemPipeSpec;
import gregtech.api.machine.PipeSpec;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTMaterialRegistry;
import gregtech.content.transport.ItemPipeCatalog;
import gregtech.content.transport.fluid.FluidTransportDefinitions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Addon pipe rows appended after the built-in catalogs. A suffix collision fails startup. */
public final class AddonPipes {
    private static final Map<String, String> OWNERS = new LinkedHashMap<>();
    private static List<PipeSpec> FLUID = List.of();
    private static List<ItemLine> ITEM = List.of();

    /** One accepted item-pipe material, before the six sizes are expanded. */
    public record ItemLine(String addonId, String idSuffix, GTMaterial material, long stepSize, int invSize) {}

    private AddonPipes() {}

    /** Resolve the queued rows. An unknown material or a duplicate suffix fails startup. */
    public static void validate() { refresh(); }

    public static List<PipeSpec> addonFluidSpecs() {
        refresh();
        return FLUID;
    }

    public static List<ItemLine> itemLines() {
        refresh();
        return ITEM;
    }

    /** Addon id that owns this block path, or null when the path is built in or unknown. */
    public static String owner(String path) {
        refresh();
        return OWNERS.get(path);
    }

    /** Registry namespace of a pipe path. Built-in paths stay {@code gt6ce}. */
    public static String namespace(String path) {
        String owner = owner(path);
        return owner == null ? "gt6ce" : owner;
    }

    private static void refresh() {
        var owners = new LinkedHashMap<String, String>();
        var fluid = new ArrayList<PipeSpec>();
        var item = new ArrayList<ItemLine>();
        var fluidSuffix = new LinkedHashMap<String, String>();
        var itemSuffix = new LinkedHashMap<String, String>();
        var builtinFluid = new HashSet<>(FluidTransportDefinitions.builtinFluidSuffixes());
        var builtinItem = new HashSet<String>();
        for (var mat : ItemPipeCatalog.ITEM_PIPE_MATS) builtinItem.add(mat.idSuffix());
        for (var queued : AddonMachines.queuedPipes()) {
            for (var declaration : queued.fluidPipes()) {
                claim(queued.addonId(), declaration.idSuffix(), fluidSuffix, builtinFluid, "fluid");
                var material = material(queued.addonId(), declaration.idSuffix(), declaration.material(), "fluid");
                long temperature = declaration.maxTemperature() > 0 ? declaration.maxTemperature()
                        : (long) (material.getMeltingPoint() * 1.25D);
                for (var size : PipeSpec.PipeSize.values()) {
                    String id = "pipe_" + size.name().toLowerCase(Locale.ROOT) + "_" + declaration.idSuffix();
                    fluid.add(PipeSpec.of(id, material, size, declaration.baseCapacity(),
                            declaration.gasProof(), declaration.acidProof(), declaration.plasmaProof(),
                            declaration.magicProof(), temperature, declaration.flammability()));
                    owners.put(id, queued.addonId());
                }
            }
            for (var declaration : queued.itemPipes()) {
                claim(queued.addonId(), declaration.idSuffix(), itemSuffix, builtinItem, "item");
                var material = material(queued.addonId(), declaration.idSuffix(), declaration.material(), "item");
                item.add(new ItemLine(queued.addonId(), declaration.idSuffix(), material,
                        declaration.stepSize(), declaration.invSize()));
                for (var size : ItemPipeSpec.ItemPipeSize.values()) {
                    String id = "item_pipe_" + size.name().toLowerCase(Locale.ROOT) + "_" + declaration.idSuffix();
                    owners.put(id, queued.addonId());
                }
            }
        }
        OWNERS.clear();
        OWNERS.putAll(owners);
        FLUID = List.copyOf(fluid);
        ITEM = List.copyOf(item);
    }

    private static void claim(String addonId, String suffix, Map<String, String> seen, java.util.Set<String> builtin, String kind) {
        if (builtin.contains(suffix))
            throw new IllegalStateException("Addon '" + addonId + "' " + kind + " pipe '" + suffix + "' uses a built-in pipe");
        String previous = seen.putIfAbsent(suffix, addonId);
        if (previous == null) return;
        if (previous.equals(addonId))
            throw new IllegalStateException("Addon '" + addonId + "' " + kind + " pipe '" + suffix + "' is declared twice");
        throw new IllegalStateException("Addon '" + addonId + "' " + kind + " pipe '" + suffix
                + "' uses the same name as addon '" + previous + "'");
    }

    private static GTMaterial material(String addonId, String suffix, String name, String kind) {
        var material = GTMaterialRegistry.get(name);
        if (material == null || !material.isValid())
            throw new IllegalStateException("Addon '" + addonId + "' " + kind + " pipe '" + suffix
                    + "' uses unknown material '" + name + "'");
        return material;
    }
}
