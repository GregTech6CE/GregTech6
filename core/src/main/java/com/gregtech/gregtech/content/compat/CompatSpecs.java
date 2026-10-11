package com.gregtech.gregtech.content.compat;

import java.util.List;

/**
 * Loader-neutral compat rows. Platforms resolve registry names, tags and GT forms;
 * this package does not reference Minecraft or a third-party mod API.
 */
public final class CompatSpecs {
    private CompatSpecs() {}

    public enum Op { SAW, ONE, TWO, GENERIFY, BATH }

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

    public record Removal(String recipeId, String source) {}

    public record Module(String modernId, boolean forge, boolean neo, String originalClass,
                         List<MachineRow> machines, List<CraftingRow> crafting, List<Removal> removals,
                         List<String> deferred) {}

    public static List<Module> modules() {
        return List.of(ImmersiveEngineeringCompat.module());
    }

    /** Structural checks only. A loaded game still has to resolve the registry names. */
    public static int check() {
        int assertions = 0;
        if (modules().size() != 1) throw new IllegalStateException("IE pilot publishes one module");
        assertions++;
        var module = modules().get(0);
        if (!module.modernId().equals("immersiveengineering") || !module.forge() || !module.neo())
            throw new IllegalStateException("IE is a dual-loader target");
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
        if (module.crafting().size() != 2 || module.removals().size() != 11)
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
}
