package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.food.GTFoodItems;
import gregtech.content.food.MeatCutCatalog;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.platform.Mc;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * GT6 6.17 cooking-oil furnace rows ({@code RecipeMapFurnace.java:57-61}): smelting a raw meat or fish
 * cut with 50 mB of cooking oil yields two cooked items instead of one. The original synthesises the
 * row inside {@code findRecipe} from the smelting result; this port keeps its table-driven lookup and
 * publishes the oil variant as a higher-priority static row beside the plain one, so the doubled row
 * wins only when the input tank actually holds oil and the plain row still answers without it.
 *
 * <p>Scope follows the original ore-dictionary membership ({@code listAllmeatraw} /
 * {@code listAllmeatcooked}, excluding {@code listAllmeatsubstitute}): the whole GT meat cuts, the ingot
 * and dust forms of MeatRaw (including the raw meat bar), and the vanilla meats and fish. The ham slice,
 * the smaller MeatRaw forms and the FishRaw forms carry no meat ore-dict name in the original
 * ({@code MultiItemFood.java:534}, {@code LoaderOreDictReRegistrations.java:815-817}) and do not double.
 * The rows stay in the GT oven table only; the vanilla furnace has no fluid tank and never mirrors them.</p>
 */
public final class CookingOilRecipes {
    /** Oil consumed per doubled row; {@code RecipeMapFurnace.java:58-60} requires and takes 50 mB. */
    private static final int OIL_MB = 50;
    /** Above the plain smelting rows (priority 0) so the oil variant wins when both match. */
    private static final long OIL_PRIORITY = 1;

    private static final List<String> SKIPPED = new ArrayList<>();
    private static final java.util.Set<Item> COVERED =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private static int registered;

    private CookingOilRecipes() {}

    /** Registers the cooking-oil furnace variants and returns how many rows were added. */
    public static int register() {
        SKIPPED.clear();
        COVERED.clear();
        registered = 0;
        var oils = cookingOils();
        if (oils == null) {
            SKIPPED.add("cooking_oils fluid group: the port registers no COOKING_OIL fluid");
            return 0;
        }
        // Whole GT meat cuts; the ham slice has no meat ore-dict name and is not a doubling cut.
        for (var cut : MeatCutCatalog.CUTS) {
            if (cut.raw().equals("raw_ham_slice")) continue;
            variant(oils, cut.raw(), cut.cooked());
        }
        // The ingot-form meat bar plus the ingot and dust material forms of MeatRaw.
        variant(oils, "raw_meat_bar", "cooked_meat_bar");
        materialForm(oils, MaterialPrefix.ingot);
        materialForm(oils, MaterialPrefix.dust);
        // Vanilla meats and fish; mutton and rabbit already come from the cut table above.
        variant(oils, "minecraft:beef", "minecraft:cooked_beef");
        variant(oils, "minecraft:porkchop", "minecraft:cooked_porkchop");
        variant(oils, "minecraft:chicken", "minecraft:cooked_chicken");
        variant(oils, "minecraft:cod", "minecraft:cooked_cod");
        variant(oils, "minecraft:salmon", "minecraft:cooked_salmon");
        Mc.logger().info("Registered {} GT6 cooking-oil furnace rows ({} rows recorded as not expressible: {})",
                registered, SKIPPED.size(), SKIPPED);
        return registered;
    }

    /** The COOKING_OIL fluids as one tag input group, sorted so the representative is deterministic. */
    private static RecipeInputGroups.Fluids cookingOils() {
        var fields = new TreeSet<String>();
        for (var entry : RegisteredFluids.all().entrySet()) {
            if (!entry.getValue().hasFlag(RegisteredFluids.FluidFlags.COOKING_OIL)) continue;
            var stack = GTFluids.stack(entry.getKey(), 1);
            if (stack != null && !stack.isEmpty()) fields.add(entry.getKey());
        }
        if (fields.isEmpty()) return null;
        return RecipeInputGroups.fluids("cooking_oils", fields.toArray(new String[0]));
    }

    private static void materialForm(RecipeInputGroups.Fluids oils, MaterialPrefix prefix) {
        var input = GTItems.getStack(prefix, GTMaterialRegistry.get("MeatRaw"), 1);
        var output = GTItems.getStack(prefix, GTMaterialRegistry.get("MeatCooked"), 1);
        if (input.isEmpty() || output.isEmpty()) {
            SKIPPED.add(prefix.getName() + "/MeatRaw -> MeatCooked: the port registers no such material form");
            return;
        }
        addVariant(oils, input, output);
    }

    private static void variant(RecipeInputGroups.Fluids oils, String inputId, String outputId) {
        var input = stack(inputId);
        var output = stack(outputId);
        if (input.isEmpty() || output.isEmpty()) {
            SKIPPED.add(inputId + " -> " + outputId + ": an item of this row is not in the port");
            return;
        }
        addVariant(oils, input, output);
    }

    private static void addVariant(RecipeInputGroups.Fluids oils, ItemStack input, ItemStack output) {
        if (!COVERED.add(input.getItem())) return;   // the same item form already answers
        var recipe = new Recipe(new ItemStack[] {input.copyWithCount(1)}, new ItemStack[] {output.copyWithCount(2)},
                null, null, new FluidStack[] {oils.stack(OIL_MB)}, null, 16, 16, 0).withPriority(OIL_PRIORITY);
        if (MachineRecipeMaps.Furnace.addRecipe(oils.apply(recipe, 0)) != null) registered++;
    }

    /** A GT food item or a namespaced registry item, mirroring the food recipe table's resolution. */
    private static ItemStack stack(String id) {
        Item item;
        if (id.indexOf(':') >= 0) {
            var key = ResourceLocation.parse(id);
            item = gregtech.platform.Mc.item(key);
            if (item == net.minecraft.world.item.Items.AIR) item = null;
        } else {
            item = GTFoodItems.itemOrNull(id);
        }
        return item == null ? ItemStack.EMPTY : new ItemStack(item, 1);
    }
}