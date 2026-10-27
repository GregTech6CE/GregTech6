package gregtech.content.recipe;

import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.registry.GTBlocks;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import gregtech.engine.recipe.Recipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** BlockStones:494-537 and Loader_Recipes_OreDict:74-78, native stone and generator engravings. */
public final class LaserStoneRecipes {
    public record Entry(String family, StoneType stone, SourceLensRoles.Color color, Recipe recipe) {}
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static boolean registered;
    private LaserStoneRecipes() {}
    public static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

    public static StoneVariant output(SourceLensRoles.Color color) {
        return switch (color) {
            case WHITE -> StoneVariant.BRICKS_CHISELED;
            case RED -> StoneVariant.TILES;
            case GREEN -> StoneVariant.SMALL_TILES;
            case BLUE -> StoneVariant.WINDMILL_TILES_A;
            case CYAN -> StoneVariant.BRICKS;
            case MAGENTA -> StoneVariant.SMALL_BRICKS;
            case YELLOW -> StoneVariant.WINDMILL_TILES_B;
            case PINK -> StoneVariant.SQUARE_BRICKS;
        };
    }

    public static int register() {
        if (registered) return 0;
        registered = true;
        for (var stone : StoneType.values()) for (var color : SourceLensRoles.Color.values())
            for (var lens : SourceLensRoles.lenses(color)) {
                var inputBlock = GTBlocks.getStone(stone, StoneVariant.SMOOTH);
                var outputBlock = GTBlocks.getStone(stone, output(color));
                if (inputBlock == null || outputBlock == null)
                    throw new IllegalStateException("Missing native laser stone " + stone + "/" + color);
                var input = new ItemStack(inputBlock);
                var output = new ItemStack(outputBlock);
                add("stone", stone, color, input, lens, output, color == SourceLensRoles.Color.WHITE ? 64 : 16, false);
            }
        for (var lens : SourceLensRoles.lenses(SourceLensRoles.Color.WHITE)) {
            add("vanilla", null, SourceLensRoles.Color.WHITE, new ItemStack(Items.STONE), lens,
                    new ItemStack(Items.CHISELED_STONE_BRICKS), 64, false);
            module("module_stone_generator", new ItemStack(Items.CHISELED_STONE_BRICKS), lens);
            // Modern vanilla owns the optional Netherlicious/NetherPlus outputs of the original.
            module("module_basalt_generator", new ItemStack(Items.POLISHED_BASALT), lens);
            module("module_blackstone_generator", new ItemStack(Items.POLISHED_BLACKSTONE_BRICKS), lens);
        }
        return ENTRIES.size();
    }

    private static void module(String id, ItemStack output, ItemStack lens) {
        // IL module names alias GTMultiItems identities, not GTTechnological's own holders.
        var input = GeneratedRecipeResolver.resolveSpec("tech:" + id + ":1");
        if (input == null || input.isEmpty())
            throw new IllegalStateException("Missing source laser generator module " + id);
        add("module", null, SourceLensRoles.Color.WHITE, input, lens, output, 128, true);
    }

    private static void add(String family, StoneType stone, SourceLensRoles.Color color, ItemStack input,
                            ItemStack lens, ItemStack output, long ticks, boolean module) {
        if (input.isEmpty() || lens.isEmpty() || output.isEmpty())
            throw new IllegalStateException("Missing native laser engraving holder: " + family + "/" + stone + "/" + color);
        var recipe = new Recipe(new ItemStack[]{input, lens.copy()}, new ItemStack[]{output},
                null, null, null, null, ticks, 16, 0).withCatalystInputs(module ? new int[]{0, 1} : new int[]{1});
        if (MachineRecipeMaps.LaserEngraver.addRecipe(recipe) == null)
            throw new IllegalStateException("Conflicting native laser engraving: " + family + "/" + stone + "/" + color);
        ENTRIES.add(new Entry(family, stone, color, recipe));
    }
}
