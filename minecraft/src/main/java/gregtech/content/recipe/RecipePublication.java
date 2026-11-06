package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.RecipeHolder;
*///?}

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
        RecipeManager manager = server.getRecipeManager();
        Collection<?> current = manager.getRecipes();
        var applied = APPLIED.get(manager);
        if (applied != null && applied.size() == current.size() && applied.containsAll(current)) return;
        RegistryAccess access = server.registryAccess();
//? if >=1.21.1 {
        /*var working = new RecipeManager(access);
*///?} else {
        var working = new RecipeManager();
//?}
        working.replaceRecipes(manager.getRecipes());
        for (var contribution : CONTRIBUTIONS) contribution.apply(working, access);
        // The furnace bridge also adds rows; deduplicate only after every producer ran.
        OvenRecipeBridge.apply(working, access);
        dedup(working, access);
        manager.replaceRecipes(working.getRecipes());
//? if >=1.21.1 {
        /*ShapelessRecipeTooltipIndex.rebuild(manager, access);
*///?} else {
        ShapelessRecipeTooltipIndex.rebuild(manager);
//?}
        var snapshot = Collections.newSetFromMap(new IdentityHashMap<>());
        snapshot.addAll(manager.getRecipes());
        APPLIED.put(manager, snapshot);
        // Indexes, previews and viewers read the committed generation, never a half-applied table.
        for (var map : gregtech.engine.recipe.RecipeMap.RECIPE_MAP_LIST) map.invalidateLookup();
        server.execute(gregtech.engine.recipe.RecipeMap::prebuildLookupIndexes);
        com.mojang.logging.LogUtils.getLogger()
                .info("[gregtech] Published GT runtime recipes: {} rows", manager.getRecipes().size());
    }

    /** Drops source rows that a generated row already covers; input rows keep the first identity. */
    private static void dedup(RecipeManager manager, RegistryAccess access) {
//? if >=1.21.1 {
        /*var deduped = new ArrayList<RecipeHolder<?>>(manager.getRecipes());
*///?} else {
        var deduped = new ArrayList<Recipe<?>>(manager.getRecipes());
//?}
        int duplicates =
                FormConversionCraftingRecipeInjector.dedupGenerated(deduped, access, RecipePublication::idOf, RecipePublication::valueOf);
        if (duplicates > 0) {
            manager.replaceRecipes(deduped);
            com.mojang.logging.LogUtils.getLogger()
                    .info("[gregtech] Removed {} duplicate source crafting rows", duplicates);
        }
    }

//? if >=1.21.1 {
    /*private static ResourceLocation idOf(Object entry) { return ((RecipeHolder<?>) entry).id(); }
    private static Recipe<?> valueOf(Object entry) { return ((RecipeHolder<?>) entry).value(); }
*///?} else {
    private static ResourceLocation idOf(Object entry) { return ((Recipe<?>) entry).getId(); }
    private static Recipe<?> valueOf(Object entry) { return (Recipe<?>) entry; }
//?}

    /** Drops the generation snapshot after the server stops; the next world starts fresh. */
    public static void clear() {
        APPLIED.clear();
    }
}
