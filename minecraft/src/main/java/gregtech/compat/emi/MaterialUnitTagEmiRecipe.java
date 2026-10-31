package gregtech.compat.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiIngredientRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Both unit queries lead to the real machine-input tag and its paginated members. */
final class MaterialUnitTagEmiRecipe extends EmiIngredientRecipe {
    private static final Map<TagKey<Item>, MaterialUnitTagEmiRecipe> PAGES = new HashMap<>();
    private final EmiRecipeCategory category;
    private final TagKey<Item> tag;
    private final EmiIngredient ingredient;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> members;
    private final List<EmiStack> outputs;

    private MaterialUnitTagEmiRecipe(EmiRecipeCategory category, TagKey<Item> tag, EmiStack unit) {
        this.category = category;
        this.tag = tag;
        ingredient = EmiIngredient.of(tag);
        members = List.copyOf(ingredient.getEmiStacks());
        inputs = List.of(unit);
        outputs = List.of(unit);
    }

    static void register(EmiRegistry registry) {
        PAGES.clear();
        var categories = new HashMap<String, EmiRecipeCategory>();
        for (var entry : gregtech.compat.common.MaterialUnitTagDisplay.entries()) {
            var tag = entry.tag();
            var unit = EmiStack.of(entry.unit());
            String family = entry.family();
            var category = categories.computeIfAbsent(family, key -> {
                var value =
                        new EmiRecipeCategory(
                                ResourceLocation.fromNamespaceAndPath("gt6ce", "material_inputs/" + key), unit) {
                            @Override
                            public Component getName() {
                                return Component.translatable("gt6ce.recipe.material_inputs." + key);
                            }
                        };
                registry.addCategory(value);
                return value;
            });
            var page = new MaterialUnitTagEmiRecipe(category, tag, unit);
            if (!page.members.isEmpty()) {
                registry.addRecipe(page);
                PAGES.put(tag, page);
            }
        }
    }

    static boolean open(TagKey<Item> tag) {
        var page = PAGES.get(tag);
        if (page == null) return false;
        dev.emi.emi.api.EmiApi.displayRecipe(page);
        return true;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(
                "gt6ce", "/material_inputs/" + tag.location().getPath());
    }

    @Override
    protected EmiIngredient getIngredient() {
        return ingredient;
    }

    @Override
    protected List<EmiStack> getStacks() {
        return members;
    }

    @Override
    protected EmiRecipe getRecipeContext(EmiStack stack, int index) {
        return null;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }
}
