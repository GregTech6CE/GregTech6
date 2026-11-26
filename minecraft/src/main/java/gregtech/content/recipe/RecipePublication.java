package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;

/**
 * The one ordered publication of generated crafting and furnace rows.
 *
 * <p>Platform nodes supply only their injector list; the sequence (contributions, deduplication,
 * furnace bridge, commit) and the unchanged-generation check live here. The rows are prepared on a
 * scratch manager, so a failing generation never clears the live table half-way.</p>
 */
public final class RecipePublication {
    @FunctionalInterface
    public interface Contribution {
        void apply(RecipeManager manager, RegistryAccess access);
    }

    private static final List<Contribution> CONTRIBUTIONS = new ArrayList<>();
    private static final Map<RecipeManager, java.util.Set<Object>> APPLIED = new WeakHashMap<>();

    private RecipePublication() {}

    /** Registers a platform injector list; called once while the node initializes. */
    public static void register(Contribution contribution) {
        CONTRIBUTIONS.add(contribution);
    }

    /** Publishes the current generation unless the manager already holds exactly these rows. */
    public static void publish(MinecraftServer server) {
        publish(server, false);
    }

    static void publish(MinecraftServer server, boolean tagsUpdated) {
        RecipeManager manager = server.getRecipeManager();
        Collection<?> current = manager.getRecipes();
        var applied = APPLIED.get(manager);
        boolean alreadyPublished = applied != null && applied.size() == current.size() && applied.containsAll(current);
        if (alreadyPublished && !tagsUpdated) return;
        RegistryAccess access = server.registryAccess();
        var working = CraftingRecipeCompat.newScratchRecipeManager(access);
        working.replaceRecipes(manager.getRecipes());
        if (!alreadyPublished) {
            for (var contribution : CONTRIBUTIONS) contribution.apply(working, access);
            dedup(working, access);
        }
        MachineRecipeOverlay.apply(working, access);
        MachineRecipeOverlay.publishFurnace(working, access);
        manager.replaceRecipes(working.getRecipes());
        commitOverlay();
        ShapelessRecipeTooltipIndex.rebuild(manager, access);
        var snapshot = Collections.newSetFromMap(new IdentityHashMap<>());
        snapshot.addAll(manager.getRecipes());
        APPLIED.put(manager, snapshot);
        // Indexes, previews and viewers read the committed generation, never a half-applied table.
        for (var map : gregtech.engine.recipe.RecipeMap.RECIPE_MAP_LIST) map.invalidateLookup();
        MachineRecipeLifecycle.prebuild(server);
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "[gregtech] Published GT runtime recipes: {} rows",
                        manager.getRecipes().size());
    }

    private static void commitOverlay() {
        var result = MachineRecipeOverlay.lastResult();
        if (!result.skipped() && !result.failed())
            gregtech.engine.material.MaterialPublication.commit(MachineRecipeOverlay.lastPublication());
    }

    /** Drops source rows that a generated row already covers; input rows keep the first identity. */
    private static void dedup(RecipeManager manager, RegistryAccess access) {
        int duplicates = CraftingRecipeCompat.dedupRecipes(manager, access);
        if (duplicates > 0) {
            com.mojang.logging.LogUtils.getLogger()
                    .info("[gregtech] Removed {} duplicate source crafting rows", duplicates);
        }
    }

    /** Drops the generation snapshot after the server stops; the next world starts fresh. */
    static void clear() {
        APPLIED.clear();
    }
}
