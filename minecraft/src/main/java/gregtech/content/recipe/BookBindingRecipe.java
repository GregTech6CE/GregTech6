package gregtech.content.recipe;

import gregtech.engine.stack.StackData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Original KEEPNBT dye/printed-pages bindings; cloning follows vanilla generation and retains the source. */
public final class BookBindingRecipe extends GTShapelessCraftingRecipe {
    private final ShapelessRecipe base;

    public BookBindingRecipe(ShapelessRecipe base) {
        super(base);
        this.base = base;
    }

    private boolean copies() { return getGroup().equals("gt6ce.books.copy"); }

    private static boolean payload(ItemStack stack) {
        if (stack.getItem() instanceof gregtech.content.item.ColoredBookItem || stack.is(Items.WRITTEN_BOOK)) return true;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)
                && (id.getPath().equals("printed_pages") || id.getPath().equals("many_printed_pages"));
    }

    private ItemStack source(CraftingGridView input) {
        for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (payload(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean onMatches(CraftingGridView input, Level level) {
        return super.onMatches(input, level) && (!copies() || StackData.writtenBookGeneration(source(input)) < 2);
    }

    @Override
    protected ItemStack onAssemble(CraftingGridView input, HolderLookup.Provider lookup) {
        var output = super.onAssemble(input, lookup);
        var payload = source(input);
        if (payload.isEmpty()) return output;
        StackData.copyAll(payload, output);
        if (copies()) {
            if (StackData.writtenBookGeneration(payload) >= 2) return ItemStack.EMPTY;
            StackData.incrementWrittenBookGeneration(output, 1);
        }
        return output;
    }

    @Override
    protected NonNullList<ItemStack> onGetRemainingItems(CraftingGridView input) {
        var remaining = super.onGetRemainingItems(input);
        if (copies()) {
            for (int i = 0; i < input.size(); i++) {
                if (payload(input.getItem(i))) {
                    remaining.set(i, input.getItem(i).copyWithCount(1));
                    break;
                }
            }
        }
        return remaining;
    }

    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }

    public static final RecipeSerializer<BookBindingRecipe> SERIALIZER =
            CraftingRecipeCompat.wrapShapelessSerializer(BookBindingRecipe::new, r -> r.base);
}
