package com.gregtech.gregtech.api.material;

import java.util.Map;

/** Canonical common-tag material names and the alternate spellings other mods still publish. */
public final class MaterialTagAliases {
    private MaterialTagAliases() {}

    /**
     * Pairs copied onto Forge and common tags: aluminium/aluminum, quartz/nether_quartz,
     * sulfur/sulphur, tungsten_steel/tungstensteel.
     */
    public static final Map<String, String> ALTERNATE_SPELLINGS = Map.of(
            "aluminium", "aluminum",
            "quartz", "nether_quartz",
            "sulfur", "sulphur",
            "tungsten_steel", "tungstensteel");
}
