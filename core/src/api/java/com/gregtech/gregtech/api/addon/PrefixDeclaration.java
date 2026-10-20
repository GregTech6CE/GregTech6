package com.gregtech.gregtech.api.addon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SPDX-License-Identifier: LGPL-3.0-or-later.
 * One addon item prefix. {@code unitAmount} counts {@code U}, so {@code 8} is 8U.
 * {@code texture} is a path in the addon's own namespace, with no namespace written in the string.
 */
public record PrefixDeclaration(
        String name,
        String displayPrefix,
        String displaySuffix,
        long unitAmount,
        String texture,
        List<String> materials,
        Set<PrefixCondition> conditions
) {
    public PrefixDeclaration {
        if (name == null || !name.matches("[a-z][A-Za-z0-9]{0,63}"))
            throw new IllegalArgumentException(
                    "Prefix name must start with a lower-case letter and use letters and digits: " + name);
        displayPrefix = displayPrefix == null ? "" : displayPrefix;
        displaySuffix = displaySuffix == null ? "" : displaySuffix;
        if (displayPrefix.isBlank() && displaySuffix.isBlank())
            throw new IllegalArgumentException("Prefix '" + name + "' needs a display prefix or a display suffix");
        if (displayPrefix.length() > 64 || displaySuffix.length() > 64
                || displayPrefix.indexOf('\n') >= 0 || displaySuffix.indexOf('\n') >= 0)
            throw new IllegalArgumentException("Prefix '" + name + "' display text is too long");
        if (unitAmount <= 0)
            throw new IllegalArgumentException("Prefix '" + name + "' unit amount must be a positive number of U");
        if (texture == null || !texture.matches("[a-z0-9]+(?:[._-][a-z0-9]+)*(?:/[a-z0-9]+(?:[._-][a-z0-9]+)*)*"))
            throw new IllegalArgumentException(
                    "Prefix '" + name + "' texture must be a path in the addon namespace: " + texture);
        if (materials == null) throw new IllegalArgumentException("Prefix '" + name + "' needs a material list");
        var names = new ArrayList<String>();
        var seen = new HashSet<String>();
        for (String material : materials) {
            if (material == null || material.isBlank())
                throw new IllegalArgumentException("Prefix '" + name + "' has an empty material name");
            if (!seen.add(material))
                throw new IllegalArgumentException("Prefix '" + name + "' lists '" + material + "' twice");
            names.add(material);
        }
        materials = List.copyOf(names);
        if (conditions == null || conditions.isEmpty()) conditions = Set.of();
        else conditions = Set.copyOf(conditions);
        boolean positive = false;
        for (PrefixCondition condition : conditions) if (condition.positive()) positive = true;
        if (materials.isEmpty() && !positive)
            throw new IllegalArgumentException(
                    "Prefix '" + name + "' needs materials or a tool or smithable condition");
    }
}
