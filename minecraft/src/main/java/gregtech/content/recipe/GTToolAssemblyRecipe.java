package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.GTToolItem;
import gregtech.content.material.Materials;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.tool.GTToolType;
import gregtech.engine.tool.ToolAssemblyRules;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

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
public final class GTToolAssemblyRecipe extends GTShapelessCraftingRecipe {

    private final GTToolType type;

    public GTToolAssemblyRecipe(ResourceLocation id, GTToolType type) {
        super(
                id,
                "gt.tools",
                CraftingBookCategory.EQUIPMENT,
                displayResult(type),
                NonNullList.of(Ingredient.EMPTY, displayHead(type), displayHandle(type)));
        this.type = type;
    }

    public GTToolType toolType() {
        return type;
    }

    @Override
    protected boolean onMatches(CraftingGridView grid, Level level) {
        return readInputs(grid) != null;
    }

    @Override
    protected ItemStack onAssemble(CraftingGridView grid, HolderLookup.Provider access) {
        ToolAssemblyRules.Inputs inputs = readInputs(grid);
        return inputs == null ? ItemStack.EMPTY : GTToolItem.create(type, inputs.head(), inputs.handle());
    }

    @Nullable
    private ToolAssemblyRules.Inputs readInputs(CraftingGridView grid) {
        var forms = new java.util.ArrayList<ToolAssemblyRules.Form>(2);
        for (int slot = 0; slot < grid.size(); slot++) {
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
        if (head == null) return ItemStack.EMPTY;
        GTMaterial handle = gregtech.content.tool.OriginalToolMaterials.defaultHandle(head);
        return GTToolItem.create(type, head, handle);
    }

    private static Ingredient displayHead(GTToolType type) {
        MaterialPrefix prefix = type == GTToolType.MAGNIFYING_GLASS ? MaterialPrefix.lens : type.headPrefix();
        if (prefix == null) return Ingredient.EMPTY;
        ItemStack stack = GTItems.getStack(prefix, displayHeadMaterial(type), 1);
        return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack);
    }

    private static Ingredient displayHandle(GTToolType type) {
        ItemStack stack = GTItems.getStack(
                MaterialPrefix.stick,
                gregtech.content.tool.OriginalToolMaterials.defaultHandle(displayHeadMaterial(type)),
                1);
        return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack);
    }

    private static GTMaterial displayHeadMaterial(GTToolType type) {
        MaterialPrefix prefix = type == GTToolType.MAGNIFYING_GLASS ? MaterialPrefix.lens : type.headPrefix();
        if (prefix == null) return null;
        GTMaterial preferred = gregtech.content.tool.OriginalToolMaterials.displayHead(type.definition());
        if (isValidDisplayHead(type, prefix, preferred)) return preferred;
        if (Materials.Steel != null && isValidDisplayHead(type, prefix, Materials.Steel)) return Materials.Steel;
        for (var candidate : gregtech.engine.material.GTMaterialRegistry.allMaterials()) {
            if (isValidDisplayHead(type, prefix, candidate)) return candidate;
        }
        return null;
    }

    private static boolean isValidDisplayHead(GTToolType type, MaterialPrefix prefix, GTMaterial material) {
        return material != null
                && prefix.isValidFor(material)
                && (type == GTToolType.MAGNIFYING_GLASS || type.canUseHead(material))
                && gregtech.content.tool.OriginalToolMaterials.acceptsAssemblyHead(type.definition(), material)
                && !GTItems.getStack(prefix, material, 1).isEmpty();
    }
}
