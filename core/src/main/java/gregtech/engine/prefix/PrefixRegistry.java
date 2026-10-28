package gregtech.engine.prefix;

import gregtech.content.data.MaterialPrefix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PrefixRegistry {
    private static final List<MaterialPrefix> ALL = new ArrayList<>();

    /**
     * GT6 prefix names the port registers under a different name for the same form.
     *
     * <p>Only spellings of the identical form belong here — the port renamed a few prefixes when it
     * split GT6's single item class into several items, and recipes carried over from the original
     * name them by their GT6 name ({@code OP.casingSmall} is the port's {@code itemCasing}, registered
     * with the original's {@code casingsmall} texture).</p>
     */
    private static final java.util.Map<String, String> ALIASES = java.util.Map.of("casingSmall", "itemCasing");

    private PrefixRegistry() {}

    /** Source spelling for an identical renamed form, without loading material definitions. */
    public static String sourceName(String name) {
        for (var alias : ALIASES.entrySet()) if (alias.getValue().equals(name)) return alias.getKey();
        return name;
    }

    /** Original whole-name language key; registry and recipe identities stay unchanged. */
    public static String sourceTranslationKey(String prefixName, String materialName) {
        return "oredict." + sourceName(prefixName) + materialName;
    }

    public static void register(MaterialPrefix prefix) {
        ALL.add(prefix);
    }

    /** Built-in or earlier addon prefix that already owns this name, ignoring case and registry spelling. */
    public static MaterialPrefix conflict(String name) {
        if (name == null || name.isEmpty()) return null;
        MaterialPrefix direct = byName(name);
        if (direct != null) return direct;
        String snake = MaterialPrefix.camelToSnake(name);
        for (MaterialPrefix prefix : all()) {
            if (prefix.getName().equalsIgnoreCase(name)
                    || prefix.getRegistryName().equals(snake)) return prefix;
        }
        return null;
    }

    /** Addon prefixes in registration order: addon id, then declaration order. */
    public static List<MaterialPrefix> addonPrefixes() {
        ensurePrefixesLoaded();
        var addon = new ArrayList<MaterialPrefix>();
        for (MaterialPrefix prefix : ALL) if (prefix.isAddon()) addon.add(prefix);
        return List.copyOf(addon);
    }

    /**
     * Must run MaterialPrefix static initializers before reading {@link #ALL}.
     * Otherwise {@code PrefixRegistry.all()} returns empty and no items register.
     */
    public static void ensurePrefixesLoaded() {
        MaterialPrefix.bootstrap();
    }

    public static List<MaterialPrefix> all() {
        ensurePrefixesLoaded();
        return Collections.unmodifiableList(ALL);
    }

    /** GT6-name lookup ({@code "blockDust"}, {@code "gemFlawed"}, …); null when the port lacks the form. */
    public static MaterialPrefix byName(String name) {
        MaterialPrefix direct = byRegistryName(name);
        if (direct != null) return direct;
        String alias = ALIASES.get(name);
        return alias == null ? null : byRegistryName(alias);
    }

    private static MaterialPrefix byRegistryName(String name) {
        for (MaterialPrefix prefix : all()) if (prefix.getName().equals(name)) return prefix;
        return null;
    }
}
