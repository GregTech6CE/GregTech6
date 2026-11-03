package gregtech.content.block.stone;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Saved IDs only; these compatibility rocks no longer register blocks or ore backgrounds. */
public final class RetiredStoneIds {
    private static final Set<String> HOSTS = Set.of(
            "stone_chalk",
            "stone_dolomite",
            "stone_gabbro",
            "stone_gneiss",
            "stone_sylvite",
            "stone_rhyolite",
            "stone_talc",
            "stone_oilshale");

    private RetiredStoneIds() {}

    private static final Set<String> MATERIALS = Set.of("Chalk", "Dolomite", "Gabbro", "Gneiss", "Rhyolite");

    public static boolean isMaterial(String name) {
        return MATERIALS.contains(name);
    }

    public static String material(String name) {
        return isMaterial(name) ? "Stone" : name;
    }

    /** Same form, ordinary stone material. The caller resolves forms unavailable for stone. */
    public static String materialPath(String path) {
        for (String name : MATERIALS) {
            String suffix = "_" + name.toLowerCase(java.util.Locale.ROOT);
            if (path.endsWith(suffix)) return path.substring(0, path.length() - suffix.length()) + "_stone";
        }
        return path;
    }

    public static java.util.Map<String, String> materialPaths() {
        java.util.Set<String> prefixes = new java.util.HashSet<>();
        gregtech.engine.prefix.PrefixRegistry.ensurePrefixesLoaded();
        gregtech.engine.prefix.BlockPrefixRegistry.ensurePrefixesLoaded();
        for (var prefix : gregtech.engine.prefix.PrefixRegistry.all())
            if (!prefix.isAddon()) prefixes.add(prefix.getRegistryName());
        for (var prefix : gregtech.engine.prefix.BlockPrefixRegistry.all())
            if (!prefix.isAddon()) prefixes.add(prefix.getRegistryName());
        java.util.Map<String, String> paths = new java.util.HashMap<>();
        for (String prefix : prefixes)
            for (String material : MATERIALS)
                paths.put(prefix + "_" + material.toLowerCase(java.util.Locale.ROOT), prefix + "_stone");
        return java.util.Map.copyOf(paths);
    }

    public static boolean isHost(String id) {
        return HOSTS.contains(id);
    }

    public static List<String> blockPaths() {
        List<String> paths = new ArrayList<>();
        for (String host : HOSTS) {
            for (StoneVariant variant : StoneVariant.values()) {
                String path = host + "_" + variant.registrySuffix();
                paths.add(path);
                paths.add(path + "_slab");
            }
        }
        return List.copyOf(paths);
    }
}
