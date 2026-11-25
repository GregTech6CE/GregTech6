package gregtech.engine.stack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootTable;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
*///?}

/** Cross-version server loot table lookup. */
public final class LootCompat {
    private LootCompat() {}

    /** Resolves a loot table by ID, returning {@link LootTable#EMPTY} when absent. */
    public static LootTable getLootTable(MinecraftServer server, ResourceLocation id) {
//? if >=1.21.1 {
/*        return server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
*///?} else {
        return server.getLootData().getLootTable(id);
//?}
    }
}
