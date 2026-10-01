package com.gregtech.gregtech.api.prefix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlockPrefixRegistry {
    private static final List<BlockMaterialPrefix> ALL = new ArrayList<>();

    private BlockPrefixRegistry() {}

    static void register(BlockMaterialPrefix prefix) {
        ALL.add(prefix);
    }

    public static void ensurePrefixesLoaded() {
        com.gregtech.gregtech.data.MaterialPrefixes.bootstrap();
    }

    public static List<BlockMaterialPrefix> all() {
        ensurePrefixesLoaded();
        return Collections.unmodifiableList(ALL);
    }

    /** GT6-name lookup ({@code "blockIngot"}, {@code "blockPlateGem"}, …); null when missing. */
    public static BlockMaterialPrefix byName(String name) {
        for (BlockMaterialPrefix prefix : all()) if (prefix.getName().equals(name)) return prefix;
        return null;
    }
}
