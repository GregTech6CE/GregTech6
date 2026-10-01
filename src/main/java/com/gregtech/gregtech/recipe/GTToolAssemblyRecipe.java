package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * GT6's head + handle assembly ({@code AdvancedCraftingTool extends ShapelessOreRecipe},
 * {@code gregapi/recipes/AdvancedCraftingTool.java}) for the tools that have a separate head: pickaxe,
 * axe, sword, shovel, hoe, saw, file, chisel, screwdriver, hammer and friends.
 *
 * <p>GT6 registers it once with a steel head and a wood stick as the <em>displayed</em> ingredients and
 * an ore-dictionary ingredient that accepts every material at match time; the port does the same with
 * a representative head plus a material check in {@link #matches}, so the row is a real shapeless
 * crafting recipe and therefore shows up in the crafting-table recipe list.</p>
 *
 * <p>Two tools are not head + handle in GT6 and are handled here anyway because they are "material
 * plus stick" rows without a head form: the magnifying glass (GT6
 * {@code AdvancedCraftingTool(MAGNIFYING_GLASS, lens, typemin(1), MT.Glass)}) and the flint and
 * tinder ({@link GTFlintAndTinderRecipe}, shaped).</p>
 */
public final class GTToolAssemblyRecipe extends ShapelessRecipe {

    private final GTToolType type;

    public GTToolAssemblyRecipe(ResourceLocation id, GTToolType type) {
        super(id, "gt.tools", CraftingBookCategory.EQUIPMENT, displayResult(type),
                NonNullList.of(Ingredient.EMPTY, displayHead(type), displayHandle(type)));
        this.type = type;
    }

    public GTToolType toolType() { return type; }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        return readInputs(grid) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        Inputs inputs = readInputs(grid);
        return inputs == null ? ItemStack.EMPTY : GTToolItem.create(type, inputs.head(), inputs.handle());
    }

    /** A magnifying glass is a lens plus a handle; every other row here is head + handle. */
    private boolean isLensTool() {
        return type == GTToolType.MAGNIFYING_GLASS;
    }

    @Nullable
    private Inputs readInputs(CraftingContainer grid) {
        MaterialPrefix wanted = isLensTool() ? MaterialPrefix.lens : type.headPrefix();
        if (wanted == null) return null;
        GTMaterial head = null;
        GTMaterial handle = null;
        int items = 0;
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            ItemStack stack = grid.getItem(slot);
            if (stack.isEmpty()) continue;
            if (++items > 2) return null;
            var form = MaterialEquivalence.form(stack);
            if (form == null) return null;
            if (form.prefix() == wanted) {
                if (head != null) return null;
                GTMaterial candidate = form.material().resolve();
                if (candidate == null || (!isLensTool() && !type.canUseHead(candidate))) return null;
                head = candidate;
            } else if (form.prefix() == MaterialPrefix.stick) {
                if (handle != null) return null;
                GTMaterial candidate = form.material().resolve();
                if (candidate == null || !GTToolHelper.isValidStick(candidate)) return null;
                handle = candidate;
            } else {
                return null;
            }
        }
        return head != null && handle != null ? new Inputs(head, handle) : null;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GTToolRecipeSerializers.ASSEMBLY;
    }

    private record Inputs(GTMaterial head, GTMaterial handle) {}

    // ── display ───────────────────────────────────────────────────────────

    private static ItemStack displayResult(GTToolType type) {
        GTMaterial head = displayHeadMaterial(type);
        GTMaterial handle = firstHandle();
        return GTToolItem.create(type, head, handle);
    }

    private static Ingredient displayHead(GTToolType type) {
        MaterialPrefix prefix = type == GTToolType.MAGNIFYING_GLASS ? MaterialPrefix.lens : type.headPrefix();
        if (prefix == null) return Ingredient.EMPTY;
        ItemStack stack = GTItems.getStack(prefix, displayHeadMaterial(type), 1);
        return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack);
    }

    private static Ingredient displayHandle(GTToolType type) {
        ItemStack stack = GTItems.getStack(MaterialPrefix.stick, firstHandle(), 1);
        return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack);
    }

    private static GTMaterial displayHeadMaterial(GTToolType type) {
        MaterialPrefix prefix = type == GTToolType.MAGNIFYING_GLASS ? MaterialPrefix.lens : type.headPrefix();
        if (prefix != null && Materials.Steel != null && prefix.isValidFor(Materials.Steel)
                && (type == GTToolType.MAGNIFYING_GLASS || type.canUseHead(Materials.Steel))) {
            return Materials.Steel;
        }
        return GTToolHelper.firstToolMaterial(prefix, type);
    }

    private static GTMaterial firstHandle() {
        return GTToolHelper.firstHandleMaterial();
    }
}
