package gregtech.content.recipe;

import gregtech.content.data.*;
import gregtech.content.recipe.GrapheneNanofabricationRecipeRows.Entry;
import gregtech.content.recipe.GrapheneNanofabricationRecipeRows.PipeEntry;
import gregtech.content.registry.*;
import gregtech.engine.machine.PipeSpec;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.*;
import gregtech.platform.Mc;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Exact carbon quantities, selector modes and times from GT6 Loader_Recipes_Other Nanofab rows. */
public final class GrapheneNanofabricationRecipes {

    private static final Entry[] DEFINITIONS = GrapheneNanofabricationRecipeRows.definitions();
    private static final PipeEntry[] PIPE_DEFINITIONS = GrapheneNanofabricationRecipeRows.pipeDefinitions();
    private static final List<Recipe> RECIPES = new ArrayList<>();

    private GrapheneNanofabricationRecipes() {}

    public static List<Recipe> recipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static int register() {
        if (!RECIPES.isEmpty()) throw new IllegalStateException("Nanofab recipes registered twice");
        var graphene = GTMaterialRegistry.get("Graphene");
        var carbon = GTMaterialRegistry.get("Carbon");
        var wire = GTWires.allWires().stream()
                .map(r -> r.get())
                .filter(b -> b.spec().id().equals("graphene") && b.spec().size() == 1)
                .findFirst()
                .orElseThrow();
        for (var def : DEFINITIONS) {
            var input = GTItems.getStack(def.input(), carbon, def.inputCount());
            var output = def.output() == null
                    ? new ItemStack(wire, def.outputCount())
                    : GTItems.getStack(def.output(), graphene, def.outputCount());
            add(def.ticks(), def.selector(), input, output, def);
        }
        for (var def : PIPE_DEFINITIONS) {
            var input = GTItems.getStack(def.input(), carbon, def.inputCount());
            var output = carbonPipe(def.outputSize(), def.outputCount());
            add(def.ticks(), def.selector(), input, output, def);
        }
        return RECIPES.size();
    }

    private static void add(int ticks, int selector, ItemStack input, ItemStack output, Object source) {
        if (input.isEmpty() || output.isEmpty())
            throw new IllegalStateException("Unresolved graphene recipe: " + source);
        var recipe = new Recipe(
                new ItemStack[] {new ItemStack(GTTechnological.selectorTag(selector)), input},
                new ItemStack[] {output},
                null,
                null,
                null,
                null,
                ticks,
                16,
                0);
        if (MachineRecipeMaps.Nanofab.addRecipe(recipe) == null)
            throw new IllegalStateException("Nanofab recipe collision: " + source);
        RECIPES.add(recipe);
    }

    /** Fluid pipes are block items, not material form items; resolve the registered carbon pipe by its id. */
    private static ItemStack carbonPipe(PipeSpec.PipeSize size, int count) {
        String id = "pipe_" + size.name().toLowerCase(Locale.ROOT) + "_carbon";
        Item item = BuiltInRegistries.ITEM.get(Mc.gregtechId(id));
        if (item == Items.AIR) throw new IllegalStateException("Missing carbon fluid pipe item: " + id);
        return new ItemStack(item, count);
    }
}
