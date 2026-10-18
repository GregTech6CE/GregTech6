package com.gregtech.gregtech.api.definition;

import java.util.ArrayList;
import java.util.List;

/** Standalone contract checks; runs with JDK 17 without loading Minecraft or Forge. */
public final class DefinitionCatalogTest {
    public static void main(String[] args) {
        var source = new ArrayList<>(List.of("oven_steel", "electric_motor_lv"));
        var catalog = DefinitionCatalog.validated(source, id -> id);
        source.clear();
        if (!catalog.equals(List.of("oven_steel", "electric_motor_lv"))) {
            throw new AssertionError("Catalog must preserve order and isolate source mutation");
        }
        rejects(UnsupportedOperationException.class, () -> catalog.add("extra"));
        rejects(IllegalArgumentException.class,
                () -> DefinitionCatalog.validated(List.of("same", "same"), id -> id));
        for (String invalid : List.of("", "Uppercase", "contains space", "gregtech:oven")) {
            rejects(IllegalArgumentException.class,
                    () -> DefinitionCatalog.validated(List.of(invalid), id -> id));
        }
        rejects(NullPointerException.class,
                () -> DefinitionCatalog.validated(List.of("valid"), id -> null));
        System.out.println("DefinitionCatalog contract checks passed");
    }

    private static void rejects(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("Wrong failure type", failure);
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }
}
