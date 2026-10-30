package gregtech.content.recipe;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceOreCrushingRules.Hexorium;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;
import static gregtech.content.recipe.SourceRecyclingFacts.Identity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Finite source facts for native ore Crusher/Hammer planning, not global material metadata.
 * Pinned GT6 3703e40308c8c030763fd6297dea8b210d2a77b1: Crushing 50-140, MT 470/1540/3709,
 * OP 653-714. Existing recycling facts supply only the 1,002 exact input identities, never their
 * thermal flags/targets/default fingerprints. The five special Hexorium identities are separately
 * proven by MT 203/1596-1611. Caller must independently prove native material, item and host/mode.
 *
 * <p>PULVERIZING_CINNABAR has exactly three explicit positive declarations in this scope and no
 * factory implication. MT and the bounded main/API/ANY/item/ore/rock/wood initialization closure
 * contain no further assignment or ALL_ORES tag-list application. Group re-registration and
 * setAllToTheOutputOf do not copy processing tags. Unknown/addon overrides are not a source NO.
 */
public final class SourceOreCrushingFacts {
    private SourceOreCrushingFacts() {}

    private static boolean reviewedInput(int id, String name) {
        var fact = SourceRecyclingFacts.find(id, name);
        return fact != null && fact.portDefaults() != null; // roster membership ONLY
    }

    public static Decision pulverizingCinnabar(int id, String canonicalName) {
        if (specialHexorium(id, canonicalName) != null) return Decision.NO;
        if (!reviewedInput(id, canonicalName)) return Decision.UNKNOWN;
        return id == 800 || id == 8333 || id == 9114 ? Decision.YES : Decision.NO;
    }
    /** Null is unreviewed identity, not the NONE classification of a proven ordinary material. */
    public static Hexorium hexorium(int id, String canonicalName) {
        Hexorium special = specialHexorium(id, canonicalName);
        return special != null ? special : reviewedInput(id, canonicalName) ? Hexorium.NONE : null;
    }

    private static Hexorium specialHexorium(int id, String name) {
        return switch (id) {
            case 9224 -> "HexoriumBlack".equals(name) ? Hexorium.BLACK_WHITE : null;
            case 9239 -> "HexoriumWhite".equals(name) ? Hexorium.BLACK_WHITE : null;
            case 9225 -> "HexoriumRed".equals(name) ? Hexorium.RED_GREEN_BLUE : null;
            case 9226 -> "HexoriumGreen".equals(name) ? Hexorium.RED_GREEN_BLUE : null;
            case 9228 -> "HexoriumBlue".equals(name) ? Hexorium.RED_GREEN_BLUE : null;
            default -> null;
        };
    }

    /** Source prefix association only. A normal/broken native representation needs separate proof;
     * this table is not an item matcher or a promise that a native holder currently exists. */
    public record HostProfile(
            String sourcePrefix, Identity gangueMaterial, long gangueAmount, boolean hexoriumAdjusted) {
        public HostProfile {
            Objects.requireNonNull(sourcePrefix);
            if (gangueAmount < 0) throw new IllegalArgumentException("Negative gangue amount");
            // Sandstone has an empty source byproduct list; only a positive amount needs an identity.
            if (gangueAmount > 0) Objects.requireNonNull(gangueMaterial);
        }

        public boolean hasGangue() {
            return gangueAmount > 0;
        }
    }

    private static final Map<String, HostProfile> HOSTS = createHosts();
    // Same OP prefix for normal/broken: Loader_Ores 56/58/59, Loader_Rocks 57-138.
    private static final Set<String> BROKEN_SOURCE_PAIRS = Set.of(
            "oreVanillastone",
            "oreNetherrack",
            "oreEndstone",
            "oreBlackgranite",
            "oreRedgranite",
            "oreBasalt",
            "oreMarble",
            "oreLimestone",
            "oreVanillagranite",
            "oreDiorite",
            "oreAndesite",
            "oreKomatiite",
            "oreGreenschist",
            "oreBlueschist",
            "oreKimberlite",
            "oreQuartzite",
            "oreLightprismarine",
            "oreDarkprismarine",
            "oreSlate",
            "oreShale");

    public static HostProfile hostProfile(String nativeHostId) {
        return nativeHostId == null ? null : HOSTS.get(nativeHostId);
    }

    public static Decision hostDecision(String nativeHostId) {
        if (hostProfile(nativeHostId) != null) return Decision.YES;
        // GT6's crusher handler excludes every DUST_ORE prefix, so their hosts never get a
        // crushing row of their own (the sifter and OilSand events handle them instead).
        if ("bedrock".equals(nativeHostId)
                || "sand".equals(nativeHostId)
                || "red_sand".equals(nativeHostId)
                || "gravel".equals(nativeHostId)
                || "mud".equals(nativeHostId)) return Decision.NO;
        return Decision.UNKNOWN;
    }
    /** False is lack of paired source evidence, not a new material/host prohibition.
     * In particular, Deepslate and Gneiss have ordinary OP profiles but no reviewed broken pair. */
    public static boolean hasBrokenSourcePair(String nativeHostId) {
        HostProfile profile = hostProfile(nativeHostId);
        return profile != null && BROKEN_SOURCE_PAIRS.contains(profile.sourcePrefix());
    }

    private static Map<String, HostProfile> createHosts() {
        var result = new LinkedHashMap<String, HostProfile>();
        host(result, "oreVanillastone", 8500, "Stone", false, "stone"); // OP659, MT1631
        host(result, "oreVanillagranite", 8518, "Granite", false, "granite", "stone_granite"); // OP658, MT3831
        host(result, "oreDiorite", 8511, "Diorite", false, "diorite", "stone_diorite"); // OP654, MT3833
        host(result, "oreAndesite", 9188, "Andesite", false, "andesite", "stone_andesite"); // OP653, MT3832
        host(result, "oreDeepslate", 9248, "Deepslate", true, "deepslate"); // OP660, MT3842
        host(result, "oreNetherrack", 8502, "Netherrack", true, "netherrack"); // OP707, MT1633
        host(result, "oreEndstone", 8504, "Endstone", false, "end_stone"); // OP709, MT1635
        host(result, "oreBlackgranite", 8508, "GraniteBlack", false, "stone_granite_black"); // OP657, MT3830
        host(result, "oreRedgranite", 8507, "GraniteRed", false, "stone_granite_red"); // OP656, MT3829
        host(result, "oreKomatiite", 9177, "Komatiite", true, "stone_komatiite"); // OP691, MT3816
        host(result, "oreBasalt", 8505, "Basalt", true, "stone_basalt"); // OP692, MT3819
        host(result, "oreMarble", 8506, "Marble", false, "stone_marble"); // OP693, MT3820
        host(result, "oreLimestone", 9189, "Limestone", false, "stone_limestone"); // OP694, MT3821
        host(result, "oreShale", 9190, "Shale", false, "stone_shale"); // OP696, MT3814
        host(result, "oreSlate", 9222, "Slate", false, "stone_slate"); // OP697, MT3841
        host(result, "oreGreenschist", 9171, "Greenschist", false, "stone_greenschist"); // OP698, MT3822
        host(result, "oreBlueschist", 9184, "Blueschist", false, "stone_blueschist"); // OP699, MT3823
        host(result, "oreGneiss", 9170, "Gneiss", false, "stone_gneiss"); // OP702, MT3826
        host(result, "oreLightprismarine", 9219, "Prismarine", false, "stone_prismarine_light"); // OP703, MT1639
        host(result, "oreDarkprismarine", 9220, "PrismarineDark", false, "stone_prismarine_dark"); // OP704, MT1640
        host(result, "oreKimberlite", 9218, "Kimberlite", false, "stone_kimberlite"); // OP705, MT3827
        host(result, "oreQuartzite", 9180, "Quartzite", false, "stone_quartzite"); // OP706, MT3828
        // OP.oreSandstone declares no byproducts, so this host contributes no gangue slot.
        if (result.put("sandstone", new HostProfile("oreSandstone", null, 0, false)) != null)
            throw new IllegalStateException("Duplicate source host sandstone");
        // Port-only hosts: these StoneTypes are GT6 worldgen *stone layers*, not ore-host blocks
        // (GT6's WD.setOre does not place ores in them), so the reference dump has no rows for them.
        // The port's block-state model does place ores in every StoneType, so each gets the same
        // 4U self-gangue profile as the reviewed hosts; the prefix names are port-local, not GT6.
        portHost(result, "chalk", 9112, "Chalk");
        portHost(result, "dolomite", 9163, "Dolomite");
        portHost(result, "gabbro", 9176, "Gabbro");
        portHost(result, "gypsum", 9161, "Gypsum");
        portHost(result, "oilshale", 9853, "OilShale");
        portHost(result, "rhyolite", 9179, "Rhyolite");
        portHost(result, "salt", 8204, "Salt");
        portHost(result, "sylvite", 8203, "Sylvite");
        portHost(result, "talc", 9169, "Talc");
        return Map.copyOf(result);
    }

    private static void host(
            Map<String, HostProfile> result,
            String prefix,
            int materialId,
            String canonicalName,
            boolean adjusted,
            String... nativeHostIds) {
        var profile = new HostProfile(prefix, new Identity(materialId, canonicalName), 4 * U, adjusted);
        for (String host : nativeHostIds) {
            if (result.put(host, profile) != null) throw new IllegalStateException("Duplicate source host " + host);
        }
    }

    /** One port-only stone-layer host, keyed by its {@code StoneType.registryId()}. */
    private static void portHost(Map<String, HostProfile> result, String stone, int materialId, String name) {
        String host = "stone_" + stone;
        if (result.put(
                        host,
                        new HostProfile(
                                "portOre" + Character.toUpperCase(stone.charAt(0)) + stone.substring(1),
                                new Identity(materialId, name),
                                4 * U,
                                false))
                != null) throw new IllegalStateException("Duplicate source host " + host);
    }
}
