package com.gregtech.gregtech.api.definition;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Validates a complete definition batch before its first Forge registration. */
public final class DefinitionCatalog {
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");
    private DefinitionCatalog() {}

    public static <T> List<T> validated(List<T> definitions, Function<T, String> idOf) {
        var snapshot = List.copyOf(definitions);
        var ids = new HashSet<String>();
        for (T definition : snapshot) {
            String id = Objects.requireNonNull(idOf.apply(definition), "Definition ID");
            if (!PATH.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid definition ID: " + id);
            }
            if (!ids.add(id)) {
                throw new IllegalArgumentException("Duplicate definition ID: " + id);
            }
        }
        return snapshot;
    }
}
