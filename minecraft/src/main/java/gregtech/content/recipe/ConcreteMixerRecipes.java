package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialGroups;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/** Loader_Recipes_Other:314-319, GT6 3703e403: the five dry-concrete mixing forms.
 * Real dust blocks carry 9 U each; do not flatten them into capped stacks of dust.
 */
public final class ConcreteMixerRecipes {
    private static final List<Recipe> REGISTERED = new ArrayList<>();
    private static final ClayMoldCatalog.Form SOURCE_STONE = new ClayMoldCatalog.Form("dust", "AnyStone");

    private ConcreteMixerRecipes() {}

    public static List<Recipe> recipes() {
        return List.copyOf(REGISTERED);
    }

    public static int register() {
        if (!REGISTERED.isEmpty()) throw new IllegalStateException("Concrete mixing recipes registered twice");
        GTMaterial concrete = GTMaterialRegistry.get("Concrete").resolve();
        Set<GTMaterial> calcites = members(MaterialGroups.Calcite);
        Set<GTMaterial> ashes = members(MaterialGroups.Ash);
        Set<GTMaterial> stones = new LinkedHashSet<>();
        // The shared source factory catalog includes ANY.Stone members omitted by the old
        // imported group. It uses original material identities, not every arbitrary rock tag.
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.isValid() && SOURCE_STONE.accepts(MaterialPrefix.dust, material))
                stones.add(material.resolve());
        }
        stones.remove(concrete);
        stones.removeAll(calcites);
        var stoneDust = group("stone_dusts/aggregate", stones, false);
        var stoneBlocks = group("stone_dust_blocks/aggregate", stones, true);
        var calciteDust = group("calcite_dusts", calcites, false);
        var calciteBlocks = group("calcite_dust_blocks", calcites, true);
        var ashDust = group("ash_dusts", ashes, false);
        var ashBlocks = group("ash_dust_blocks", ashes, true);
        add(1296, stoneBlocks, 9, calciteBlocks, 1, ashBlocks, 1, block(concrete, 11));
        add(1296, stoneBlocks, 9, calciteBlocks, 1, ashDust, 9, block(concrete, 11));
        add(1296, stoneBlocks, 9, calciteDust, 9, ashBlocks, 1, block(concrete, 11));
        add(144, stoneBlocks, 1, calciteDust, 1, ashDust, 1, dust(concrete, 11));
        add(16, stoneDust, 9, calciteDust, 1, ashDust, 1, dust(concrete, 11));
        return REGISTERED.size();
    }

    private static Set<GTMaterial> members(GTMaterial group) {
        Set<GTMaterial> result = new LinkedHashSet<>();
        for (GTMaterial material : group.getReRegistrations()) if (material.isValid()) result.add(material.resolve());
        return result;
    }

    private static ItemStack dust(GTMaterial material, int count) {
        return GTItems.getStack(MaterialPrefix.dust, material, count);
    }

    private static ItemStack block(GTMaterial material, int count) {
        ItemStack stack = GTBlocks.getStack(BlockMaterialPrefix.blockDust, material);
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(count);
    }

    private static RecipeInputGroups.Items group(String name, Set<GTMaterial> materials, boolean blocks) {
        var stacks = materials.stream()
                .map(m -> blocks ? block(m, 1) : dust(m, 1))
                .filter(stack -> !stack.isEmpty())
                .toList();
        return stacks.isEmpty() ? null : RecipeInputGroups.items(name, stacks);
    }

    private static void add(
            long ticks,
            RecipeInputGroups.Items stone,
            int stones,
            RecipeInputGroups.Items calcite,
            int calcites,
            RecipeInputGroups.Items ash,
            int ashes,
            ItemStack result) {
        if (stone == null || calcite == null || ash == null || result.isEmpty()) return;
        Recipe recipe = new Recipe(
                new ItemStack[] {stone.stack(stones), calcite.stack(calcites), ash.stack(ashes)},
                new ItemStack[] {result},
                null,
                null,
                null,
                null,
                ticks,
                16,
                0);
        stone.apply(recipe, 0);
        calcite.apply(recipe, 1);
        ash.apply(recipe, 2);
        if (MachineRecipeMaps.Mixer.addRecipe(recipe) == recipe) REGISTERED.add(recipe);
    }

    /** Only the lossy old concrete row is superseded, not unrelated blockDust imports. */
    public static boolean isLegacyImport(
            String map,
            long power,
            long ticks,
            String[] inputs,
            String[] fluidsIn,
            String[] fluidsOut,
            String[] outputs) {
        return map.equals("Mixer")
                && power == 16
                && ticks == 1296
                && fluidsIn.length == 0
                && fluidsOut.length == 0
                && Arrays.equals(
                        inputs, new String[] {"i:blockDust:Stone:9", "i:blockDust:Calcite:1", "i:blockDust:Ash:1"})
                && Arrays.equals(outputs, new String[] {"i:blockDust:Concrete:11"});
    }
}
