package gregtech.engine.material;

import java.util.Map;

/** Canonical common-tag material names and the alternate spellings other mods still publish. */
public final class MaterialTagAliases {
    private MaterialTagAliases() {}

    /**
     * Pairs copied onto Forge and common tags: aluminium/aluminum, quartz/nether_quartz,
     * sulfur/sulphur, tungsten_steel/tungstensteel. The mod materials follow the spelling their
     * own mod publishes, so both mods' items land in one tag: Botania's {@code elementium} and
     * Twilight Forest's {@code fiery}.
     */
    public static final Map<String, String> ALTERNATE_SPELLINGS = Map.of(
            "osmium_elemental", "osmium",
            "adamantium", "ad",
            "bedrock_hslaalloy", "bedrock_hsla_alloy",
            "aluminium", "aluminum",
            "quartz", "nether_quartz",
            "sulfur", "sulphur",
            "tungsten_steel", "tungstensteel",
            "elven_elementium", "elementium",
            "fiery_steel", "fiery");

    /** Alternate material spelling for the last segment of a tag path ({@code ingots/elven_elementium}). */
    public static String alternatePath(String path) {
        int slash = path.lastIndexOf('/');
        if (slash < 0) return null;
        String alternate = ALTERNATE_SPELLINGS.get(path.substring(slash + 1));
        return alternate == null ? null : path.substring(0, slash + 1) + alternate;
    }

    /** Alternate spelling of one material name ({@code elven_elementium} → {@code elementium}). */
    public static String alternate(String materialName) {
        return ALTERNATE_SPELLINGS.get(materialName);
    }
}
