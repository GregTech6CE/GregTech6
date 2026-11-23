package gregtech.content.worldgen.dungeon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/** Resolves the existing registered platform blocks. Does not register a second content family. */
public final class DungeonBindings {
    private DungeonBindings() {}

    public static Block block(String id) {
        var key = ResourceLocation.fromNamespaceAndPath("gt6ce", id);
        if (!BuiltInRegistries.BLOCK.containsKey(key))
            throw new IllegalStateException("Dungeon dependency missing: " + key);
        return BuiltInRegistries.BLOCK.get(key);
    }

    public static java.util.function.Supplier<Block> supplier(String id) {
        return () -> block(id);
    }
}
