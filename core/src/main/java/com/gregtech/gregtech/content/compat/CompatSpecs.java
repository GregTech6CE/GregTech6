package com.gregtech.gregtech.content.compat;

import java.util.List;

/**
 * Loader-neutral compat rows. Platforms resolve registry names, tags and GT forms;
 * this package does not reference Minecraft or a third-party mod API.
 */
public final class CompatSpecs {
    private CompatSpecs() {}

    public enum Op { SAW, ONE, TWO, THREE, GENERIFY, BATH }

    public sealed interface Stack permits Stack.Item, Stack.Tag, Stack.Form {
        int count();
        record Item(String id, int count) implements Stack {}
        /** Forge {@code forge:} tag and NeoForge {@code c:} tag for the same ingredient. */
        record Tag(String forgeTag, String neoTag, int count) implements Stack {}
        record Form(String prefix, String material, int count) implements Stack {}
    }

    public record MachineRow(String id, String map, Op op, List<Stack> inputs, List<Stack> outputs,
                              String fluid, int fluidAmount, long eut, long duration, boolean food,
                              long lubricant, boolean optimize, String source) {}

    public record CraftingRow(String id, Stack output, List<Stack> inputs, String source) {}

    /** One-character pattern symbols. The platform writes a shaped crafting recipe. */
    public record ShapedKey(String symbol, Stack stack) {}

    public record ShapedRow(String id, Stack output, List<String> pattern, List<ShapedKey> keys, String source) {}

    public record Removal(String recipeId, String source) {}

    public record Module(String modernId, boolean forge, boolean neo, String originalClass,
                         List<MachineRow> machines, List<CraftingRow> crafting, List<ShapedRow> shaped,
                         List<Removal> removals, List<String> deferred) {}

    public static List<Module> modules() {
        return List.of(ImmersiveEngineeringCompat.module(), MekanismCompat.module(),
                AppliedEnergisticsCompat.module());
    }

    /** Structural checks only. A loaded game still has to resolve the registry names. */
    public static int check() {
        int assertions = 0;
        var byId = new java.util.LinkedHashMap<String, Module>();
        for (var module : modules()) {
            if (byId.put(module.modernId(), module) != null)
                throw new IllegalStateException("Duplicate compat module " + module.modernId());
            assertions++;
        }
        if (byId.size() != 3 || !byId.containsKey("immersiveengineering") || !byId.containsKey("mekanism")
                || !byId.containsKey("ae2"))
            throw new IllegalStateException("compat modules " + byId.keySet());
        assertions++;
        assertions += immersiveEngineering(byId.get("immersiveengineering"));
        assertions += mekanism(byId.get("mekanism"));
        assertions += appliedEnergistics(byId.get("ae2"));
        return assertions;
    }

    private static int immersiveEngineering(Module module) {
        int assertions = 0;
        if (!module.forge() || !module.neo()) throw new IllegalStateException("IE is a dual-loader target");
        assertions++;
        if (!module.originalClass().equals("Compat_Recipes_ImmersiveEngineering"))
            throw new IllegalStateException(module.originalClass());
        assertions++;
        var ids = new java.util.HashSet<String>();
        for (var row : module.machines()) {
            if (!ids.add(row.id()) || row.source().isBlank() || row.inputs().isEmpty() || row.outputs().isEmpty())
                throw new IllegalStateException("Bad machine row " + row.id());
            if (!switch (row.map()) {
                case "Cutter", "Compressor", "Shredder", "Loom", "Generifier", "Bath" -> true;
                default -> false;
            }) throw new IllegalStateException("Unknown map " + row.map());
            assertions++;
        }
        if (module.crafting().size() != 2 || !module.shaped().isEmpty() || module.removals().size() != 11)
            throw new IllegalStateException("IE crafting/removal counts");
        assertions++;
        for (var row : module.crafting()) {
            if (!row.id().startsWith("compat/immersiveengineering/") || row.inputs().size() != 1)
                throw new IllegalStateException(row.id());
            assertions++;
        }
        for (var removal : module.removals()) {
            if (!removal.recipeId().startsWith("immersiveengineering:crafting/plate_")
                    || !removal.recipeId().endsWith("_hammering"))
                throw new IllegalStateException(removal.recipeId());
            assertions++;
        }
        if (module.deferred().size() < 4) throw new IllegalStateException("deferred IE notes");
        assertions++;
        long baths = module.machines().stream().filter(row -> row.op() == Op.BATH).count();
        if (baths != 11L * 7 * 2) throw new IllegalStateException("stair/slab oil baths " + baths);
        assertions++;
        return assertions;
    }

    private static int mekanism(Module module) {
        int assertions = 0;
        if (!module.forge() || !module.neo() || !module.originalClass().equals("Compat_Recipes_Mekanism"))
            throw new IllegalStateException(module.originalClass());
        assertions++;
        if (!module.machines().isEmpty() || !module.crafting().isEmpty() || !module.shaped().isEmpty()
                || module.removals().size() != 1)
            throw new IllegalStateException("Mekanism publishes one crafting removal");
        assertions++;
        if (!module.removals().get(0).recipeId().equals("mekanism:storage_blocks/salt"))
            throw new IllegalStateException(module.removals().get(0).recipeId());
        assertions++;
        if (module.deferred().size() < 8) throw new IllegalStateException("deferred Mekanism dye targets");
        assertions++;
        return assertions;
    }

    private static int appliedEnergistics(Module module) {
        int assertions = 0;
        if (!module.forge() || !module.neo() || !module.originalClass().equals("Compat_Recipes_AppliedEnergistics"))
            throw new IllegalStateException(module.originalClass());
        assertions++;
        if (!module.crafting().isEmpty() || module.shaped().size() != 2 || module.removals().size() != 2)
            throw new IllegalStateException("AE crafting counts");
        assertions++;
        if (module.machines().stream().noneMatch(row -> row.op() == Op.THREE)
                || module.machines().stream().noneMatch(row -> row.op() == Op.SAW))
            throw new IllegalStateException("AE press and saw rows");
        assertions++;
        for (var row : module.machines()) {
            if (!switch (row.map()) {
                case "Press", "Compressor", "Cutter", "Hammer", "Crusher" -> true;
                default -> false;
            }) throw new IllegalStateException("Unknown AE map " + row.map());
            if (row.op() == Op.THREE && row.inputs().size() != 3)
                throw new IllegalStateException(row.id());
            assertions++;
        }
        if (module.deferred().size() < 4) throw new IllegalStateException("deferred AE notes");
        assertions++;
        return assertions;
    }
}
