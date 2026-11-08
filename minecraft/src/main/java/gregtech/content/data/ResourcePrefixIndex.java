package gregtech.content.data;

import java.util.NavigableMap;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Indexes pack resources by their {@code namespace:path} key so a path-prefix query does not scan
 * the whole pack. The vanilla resource manager asks each pack for every directory it walks, and
 * the generated recipes and tags contain tens of thousands of rows.
 */
final class ResourcePrefixIndex {
    private final NavigableMap<String, ResourceLocation> byKey = new TreeMap<>();

    void add(ResourceLocation id) {
        byKey.put(id.toString(), id);
    }

    /** Resources in {@code namespace} whose path starts with {@code path + "/"}. */
    Iterable<ResourceLocation> under(String namespace, String path) {
        String prefix = namespace + ":" + path + "/";
        // Every valid resource path is ASCII, so U+FFFF sorts after every key under the prefix.
        return byKey.subMap(prefix, prefix + '\uFFFF').values();
    }
}
