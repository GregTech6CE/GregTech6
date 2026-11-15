package gregtech.content.block.stone;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.material.MaterialFactKeys.Decision;
import static gregtech.content.material.MaterialFactKeys.Identity;

/** Source ore-host prefixes, gangue composition and broken-ore pairing. */
public final class OreHostFacts {
    private OreHostFacts() {}

    public record HostProfile(
            String sourcePrefix, Identity gangueMaterial, long gangueAmount, boolean hexoriumAdjusted) {
        public HostProfile {
            Objects.requireNonNull(sourcePrefix);
            if (gangueAmount < 0) throw new IllegalArgumentException("Negative gangue amount");
            if (gangueAmount > 0) Objects.requireNonNull(gangueMaterial);
        }

        public boolean hasGangue() {
            return gangueAmount > 0;
        }
    }

    private static final Map<String, HostProfile> HOSTS = createHosts();
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
        if ("bedrock".equals(nativeHostId)
                || "sand".equals(nativeHostId)
                || "red_sand".equals(nativeHostId)
                || "gravel".equals(nativeHostId)
                || "mud".equals(nativeHostId)) return Decision.NO;
        return Decision.UNKNOWN;
    }

    public static boolean hasBrokenSourcePair(String nativeHostId) {
        HostProfile profile = hostProfile(nativeHostId);
        return profile != null && BROKEN_SOURCE_PAIRS.contains(profile.sourcePrefix());
    }

    private static Map<String, HostProfile> createHosts() {
        var result = new LinkedHashMap<String, HostProfile>();
        host(result, "oreVanillastone", 8500, "Stone", false, "stone");
        host(result, "oreVanillagranite", 8518, "Granite", false, "granite", "stone_granite");
        host(result, "oreDiorite", 8511, "Diorite", false, "diorite", "stone_diorite");
        host(result, "oreAndesite", 9188, "Andesite", false, "andesite", "stone_andesite");
        host(result, "oreDeepslate", 9248, "Deepslate", true, "deepslate");
        host(result, "oreNetherrack", 8502, "Netherrack", true, "netherrack");
        host(result, "oreEndstone", 8504, "Endstone", false, "end_stone");
        host(result, "oreBlackgranite", 8508, "GraniteBlack", false, "stone_granite_black");
        host(result, "oreRedgranite", 8507, "GraniteRed", false, "stone_granite_red");
        host(result, "oreKomatiite", 9177, "Komatiite", true, "stone_komatiite");
        host(result, "oreBasalt", 8505, "Basalt", true, "stone_basalt");
        host(result, "oreMarble", 8506, "Marble", false, "stone_marble");
        host(result, "oreLimestone", 9189, "Limestone", false, "stone_limestone");
        host(result, "oreShale", 9190, "Shale", false, "stone_shale");
        host(result, "oreSlate", 9222, "Slate", false, "stone_slate");
        host(result, "oreGreenschist", 9171, "Greenschist", false, "stone_greenschist");
        host(result, "oreBlueschist", 9184, "Blueschist", false, "stone_blueschist");
        host(result, "oreLightprismarine", 9219, "Prismarine", false, "stone_prismarine_light");
        host(result, "oreDarkprismarine", 9220, "PrismarineDark", false, "stone_prismarine_dark");
        host(result, "oreKimberlite", 9218, "Kimberlite", false, "stone_kimberlite");
        host(result, "oreQuartzite", 9180, "Quartzite", false, "stone_quartzite");
        if (result.put("sandstone", new HostProfile("oreSandstone", null, 0, false)) != null)
            throw new IllegalStateException("Duplicate source host sandstone");
        portHost(result, "gypsum", 9161, "Gypsum");
        portHost(result, "salt", 8204, "Salt");
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
        for (String host : nativeHostIds)
            if (result.put(host, profile) != null) throw new IllegalStateException("Duplicate source host " + host);
    }

    private static void portHost(Map<String, HostProfile> result, String stone, int materialId, String name) {
        String host = "stone_" + stone;
        String prefix = "portOre" + Character.toUpperCase(stone.charAt(0)) + stone.substring(1);
        if (result.put(host, new HostProfile(prefix, new Identity(materialId, name), 4 * U, false)) != null)
            throw new IllegalStateException("Duplicate source host " + host);
    }
}
