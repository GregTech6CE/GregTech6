package gregtech.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.*;

/** One uses index per EMI manager. Material candidates share recipe lists instead of expanding edges. */
public final class MaterialUsesIndex {
    private final Map<EmiStack, List<List<EmiRecipe>>> byAlias;
    private long expandedEdges;
    private long sharedEdges;

    public MaterialUsesIndex(List<EmiRecipe> recipes, Hash.Strategy<? super EmiStack> comparison) {
        byAlias = new Object2ObjectOpenCustomHashMap<>(comparison);
        var groups = new IdentityHashMap<List<EmiStack>, LinkedHashSet<EmiRecipe>>();
        var icons = new IdentityHashMap<List<EmiStack>, EmiStack>();
        for (var recipe : recipes)
            for (var inputs : List.of(recipe.getInputs(), recipe.getCatalysts()))
                for (var input : inputs) {
                    var icon = indexKey(input);
                    if (icon == null) continue;
                    var aliases = input.getEmiStacks();
                    groups.computeIfAbsent(aliases, ignored -> new LinkedHashSet<>())
                            .add(recipe);
                    icons.put(aliases, icon);
                }
        groups.forEach((aliases, rows) -> {
            var shared = List.copyOf(rows);
            // Preserve EMI's configured comparison, including component-sensitive item subtypes.
            Set<EmiStack> seen = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(comparison);
            for (var alias : aliases) {
                if (comparison.equals(alias, icons.get(aliases)) || !seen.add(alias)) continue;
                byAlias.computeIfAbsent(alias, ignored -> new ArrayList<>()).add(shared);
                expandedEdges += shared.size();
                sharedEdges++;
            }
            sharedEdges += shared.size();
        });
        byAlias.replaceAll((key, groupsForAlias) -> List.copyOf(groupsForAlias));
    }

    /** Only GT-owned shared alternatives participate; other plugins keep their native indexing. */
    private static EmiStack indexKey(EmiIngredient input) {
        if (input instanceof MaterialUnitEmiIngredient unit) return unit.indexKey();
        if (input instanceof MachineEmiIngredient && input.getEmiStacks().size() > 1)
            return input.getEmiStacks().get(0);
        return null;
    }

    /** Used exclusively while EMI constructs its input index, never by widgets or recipe trees. */
    public static List<EmiIngredient> compactInputs(List<EmiIngredient> inputs) {
        List<EmiIngredient> compact = null;
        for (int i = 0; i < inputs.size(); i++) {
            var icon = indexKey(inputs.get(i));
            if (icon == null) continue;
            if (compact == null) compact = new ArrayList<>(inputs);
            compact.set(i, icon);
        }
        return compact == null ? inputs : compact;
    }

    public List<EmiRecipe> uses(EmiStack stack, List<EmiRecipe> direct) {
        var groups = byAlias.get(stack);
        if (groups == null) return direct;
        if (direct.isEmpty() && groups.size() == 1) return groups.get(0);
        var union = new LinkedHashSet<>(direct);
        for (var group : groups) union.addAll(group);
        return List.copyOf(union);
    }

    public long expandedEdges() {
        return expandedEdges;
    }

    public long sharedEdges() {
        return sharedEdges;
    }
}
