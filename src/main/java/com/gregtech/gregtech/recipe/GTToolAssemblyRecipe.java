package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.api.tool.ToolAssemblyRules;
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
        ToolAssemblyRules.Inputs inputs = readInputs(grid);
        return inputs == null ? ItemStack.EMPTY : GTToolItem.create(type, inputs.head(), inputs.handle());
    }

    @Nullable
    private ToolAssemblyRules.Inputs readInputs(CraftingContainer grid) {
        var forms = new java.util.ArrayList<ToolAssemblyRules.Form>(2);
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            ItemStack stack = grid.getItem(slot);
            if (stack.isEmpty()) continue;
            if (forms.size() == 2) return null;
            var form = MaterialEquivalence.form(stack);
            if (form == null) return null;
            forms.add(new ToolAssemblyRules.Form(form.prefix(), form.material()));
        }
        return ToolAssemblyRules.match(type.definition(), forms);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GTToolRecipeSerializers.ASSEMBLY;
    }

    // ── display ───────────────────────────────────────────────────────────

    private static ItemStack displayResult(GTToolType type) {
        GTMaterial head = displayHeadMaterial(type);
        GTMaterial handle = com.gregtech.gregtech.content.tool.OriginalToolMaterials.defaultHandle(head);
        return GTToolItem.create(type, head, handle);
    }

    private static Ingredient displayHead(GTToolType type) {
        MaterialPrefix prefix = type == GTToolType.MAGNIFYING_GLASS ? MaterialPrefix.lens : type.headPrefix();
        if (prefix == null) return Ingredient.EMPTY;
        ItemStack stack = GTItems.getStack(prefix, displayHeadMaterial(type), 1);
        return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack);
    }

    private static Ingredient displayHandle(GTToolType type) {
        ItemStack stack = GTItems.getStack(MaterialPrefix.stick, com.gregtech.gregtech.content.tool.OriginalToolMaterials.defaultHandle(displayHeadMaterial(type)), 1);
        return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack);
    }

    private static GTMaterial displayHeadMaterial(GTToolType type) {
        MaterialPrefix prefix = type == GTToolType.MAGNIFYING_GLASS ? MaterialPrefix.lens : type.headPrefix();
        if (prefix != null && Materials.Steel != null && prefix.isValidFor(Materials.Steel)
                && (type == GTToolType.MAGNIFYING_GLASS || type.canUseHead(Materials.Steel))
                && com.gregtech.gregtech.content.tool.OriginalToolMaterials.acceptsAssemblyHead(type.definition(), Materials.Steel)) {
            return Materials.Steel;
        }
        for (var candidate : com.gregtech.gregtech.api.material.GTMaterialRegistry.allMaterials()) {
            if (prefix != null && prefix.isValidFor(candidate) && type.canUseHead(candidate)
                    && com.gregtech.gregtech.content.tool.OriginalToolMaterials.acceptsAssemblyHead(type.definition(), candidate)) return candidate;
        }
        return null;
    }

}
