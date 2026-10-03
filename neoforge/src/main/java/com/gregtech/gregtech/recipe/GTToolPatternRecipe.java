package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.tool.ManualToolRecipeCatalog;
import com.gregtech.gregtech.content.tool.OriginalToolMaterials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Shared machinery of GT6's shaped tool rows ({@link GTToolRecipes}).
 *
 * <p>GT6 registers one such row <em>per material</em> ({@code OreProcessing_Tool.onOreRegistration}),
 * with {@code OP.plate.dat(mMaterial)} and friends as ingredients. The port has ~1,100 materials and
 * would need tens of thousands of rows that way, so it registers one row per pattern and matches it
 * with predicates: the pattern decides the shape, the material check decides which materials it
 * accepts. The displayed ingredients are a single representative material, which is what the
 * crafting-table recipe list shows; the tool-assembly JEI category lists every material that works
 * ({@code ToolAssemblyCatalog}).</p>
 */
public abstract class GTToolPatternRecipe extends ToolShapedRecipe {

    protected final GTToolType type;
    protected final GTToolRecipes.Pattern pattern;

    protected GTToolPatternRecipe(ResourceLocation id, GTToolType type, GTToolRecipes.Pattern pattern) {
        this(id, type, pattern, false);
    }

    protected GTToolPatternRecipe(ResourceLocation id, GTToolType type, GTToolRecipes.Pattern pattern, boolean head) {
        super(new ShapedRecipe("gt.tools",CraftingBookCategory.EQUIPMENT,new net.minecraft.world.item.crafting.ShapedRecipePattern(pattern.width(),pattern.height(),ingredientsOf(type,pattern),java.util.Optional.empty()),displayResult(type,pattern,head)),
                pattern.mirror());
        this.type = type;
        this.pattern = pattern;
    }

    public GTToolType toolType() { return type; }

    public GTToolRecipes.Pattern pattern() { return pattern; }

    /** Index of this row in the pattern list the tool was built from, for the serializer. */
    public int patternIndex() {
        int index = GTToolRecipes.shaped(type).indexOf(pattern);
        return Math.max(index, 0);
    }

    /** The material forms a placed pattern was made of. */
    protected record Match(GTMaterial material, GTMaterial handle) {}

    /**
     * Matches the pattern (mirrored when GT6's row allows it) and resolves the materials, or null.
     *
     * <p>Deliberately not {@code super.matches}: the displayed ingredients are one representative
     * material, while the row has to accept every material GT6 registers it for.</p>
     */
    @Override
    public boolean matches(CraftingInput grid, Level level) {
        return match(grid) != null;
    }

    /**
     * Matches the pattern (mirrored when GT6's row allows it) and resolves the materials, or null.
     */
    @Nullable
    protected Match match(CraftingInput grid) {
        if (!toolsUsable(grid)) return null;
        for (int left = 0; left <= grid.width() - pattern.width(); left++) {
            for (int top = 0; top <= grid.height() - pattern.height(); top++) {
                Match plain = match(grid, left, top, false);
                if (plain != null) return plain;
                if (pattern.mirror()) {
                    Match mirrored = match(grid, left, top, true);
                    if (mirrored != null) return mirrored;
                }
            }
        }
        return null;
    }

    @Nullable
    private Match match(CraftingInput grid, int left, int top, boolean mirrored) {
        GTMaterial material = null;
        GTMaterial handle = null;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                ItemStack stack = grid.getItem(x + y * grid.width());
                int px = x - left;
                int py = y - top;
                char letter = ' ';
                if (px >= 0 && py >= 0 && px < pattern.width() && py < pattern.height()) {
                    letter = pattern.at(mirrored ? pattern.width() - 1 - px : px, py);
                }
                if (letter == ' ') {
                    if (!stack.isEmpty()) return null;
                    continue;
                }
                if (stack.isEmpty()) return null;
                var tool = pattern.tools().get(letter);
                if (tool != null) {
                    if (!CraftingTools.matches(stack, tool)) return null;
                    continue;
                }
                var special = specialIngredient(type, pattern, letter);
                if (special != null) {
                    if (!special.test(stack)) return null;
                    continue;
                }
                GTMaterial form = classify(stack, letter, material);
                if (form == null) return null;
                if (letter == 'H') {
                    if (handle != null && handle != form) return null;
                    handle = form;
                } else if (material == null) {
                    material = form;
                }
            }
        }
        if (material == null || !acceptsMaterial(material)) return null;
        if (handle != null && !(pattern.gate().skipHeadGate()
                ? OriginalToolMaterials.earlyHandle(handle)
                : pattern.normalHandle() ? OriginalToolMaterials.acceptsHandle(material, handle) : handle == material)) return null;
        return new Match(material, handle == null ? material : handle);
    }

    /** Whether the resolved material may be used for this row; GT6 gates this per registration. */
    protected boolean acceptsMaterial(GTMaterial material) {
        if (material == null || !material.isValid()) return false;
        var gate = pattern.gate();
        if (gate.onlyMaterial() != null && !gate.onlyMaterial().equals(material.getName())) return false;
        // GT6's ANY.Stone (Loader_Tools:272) is the set of materials carrying the STONE property.
        if (gate.onlyStone() && !material.has(com.gregtech.gregtech.api.material.MaterialProperty.STONE)) {
            return false;
        }
        // The early rows take any rock (a stone never has a tool-head form), and a fixed tool material
        // (flint, bone) is settled by the item itself.
        if (gate.skipHeadGate() || gate.toolMaterial() != null) return true;
        return type.canUseHead(material) && OriginalToolMaterials.acceptsHead(type.definition(), material);
    }

    /** The material a fixed item stands for (GT6 {@code MT.Flint} / {@code MT.Bone}), or null. */
    @Nullable
    private GTMaterial fixedMaterial() {
        String name = pattern.gate().toolMaterial();
        return name == null ? null : GTMaterialRegistry.get(name);
    }

    /**
     * Checks one slot and returns the material it contributes, or null when it does not fit.
     *
     * @param material the material already established by the head/first form, or null
     */
    @Nullable
    protected GTMaterial classify(ItemStack stack, char letter, @Nullable GTMaterial material) {
        if (letter == 'F') {
            return stack.is(net.minecraft.world.item.Items.FLINT) ? material : null;
        }
        // GT6's early rows put a fixed vanilla item in the grid (flint, bone) and take the tool's
        // material from it.
        var fixed = pattern.gate().items().get(letter);
        if (fixed != null) {
            if (!stack.is(fixed)) return null;
            GTMaterial forced = fixedMaterial();
            return forced != null ? forced : material;
        }
        if (letter == 'A') {
            if (type.headPrefix() == null) return null;
            var form = MaterialEquivalence.form(stack);
            if (form == null || form.prefix() != type.headPrefix()) return null;
            GTMaterial head = form.material().resolve();
            return head != null && type.canUseHead(head) ? head : null;
        }
        if (letter == 'H') {
            if (pattern.gate().skipHeadGate()) {
                if (stack.is(net.minecraft.world.item.Items.BONE)) return Materials.Bone;
                if (stack.is(net.minecraft.world.item.Items.BAMBOO)) return com.gregtech.gregtech.content.material.generated.WoodMaterials.Bamboo;
            }
            var form = MaterialEquivalence.form(stack);
            if (form == null || form.prefix() != MaterialPrefix.stick) return null;
            GTMaterial stick = form.material().resolve();
            if (stick == null || !GTToolHelper.isValidStick(stick)) return null;
            // GT6 mUseNormalHandle: the handle is the head material's handle material, usually wood;
            // otherwise the tool is made of one material throughout.
            return !pattern.normalHandle() && !pattern.gate().skipHeadGate() && material != null && stick != material ? null : stick;
        }
        MaterialPrefix prefix = pattern.forms().get(letter);
        if (prefix == null) return null;
        var form = MaterialEquivalence.form(stack);
        if (form == null || form.prefix() != prefix) return null;
        GTMaterial found = form.material().resolve();
        if (found == null) return null;
        return material == null || material == found ? found : null;
    }

    /** The output of this row: the tool, or the head, built from the matched material. */
    protected abstract ItemStack assembleFrom(Match match);

    @Override
    public ItemStack assemble(CraftingInput grid, net.minecraft.core.HolderLookup.Provider access) {
        Match match = match(grid);
        return match == null ? ItemStack.EMPTY : assembleFrom(match);
    }

    // ── display material ──────────────────────────────────────────────────

    /** The material the crafting-table recipe list shows: steel when it has the needed forms. */
    protected static GTMaterial displayMaterial(GTToolType type, GTToolRecipes.Pattern pattern) {
        String fixed = pattern.gate().toolMaterial();
        if (fixed != null) {
            GTMaterial material = GTMaterialRegistry.get(fixed);
            if (material != null && material.isValid()) return material;
        }
        GTMaterial steel = Materials.Steel;
        if (steel != null && fits(type, pattern, steel)) return steel;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.isValid() && fits(type, pattern, material)) return material;
        }
        return steel;
    }

    public static boolean hasMaterials(GTToolType type, GTToolRecipes.Pattern pattern) {
        return GTMaterialRegistry.allMaterials().stream().anyMatch(material -> material.isValid() && fits(type, pattern, material));
    }

    private static boolean fits(GTToolType type, GTToolRecipes.Pattern pattern, GTMaterial material) {
        var gate = pattern.gate();
        if (gate.toolMaterial() != null) return true;
        if (gate.onlyMaterial() != null && !gate.onlyMaterial().equals(material.getName())) return false;
        // GT6's ANY.Stone (Loader_Tools:272) is the set of materials carrying the STONE property.
        if (gate.onlyStone() && !material.has(com.gregtech.gregtech.api.material.MaterialProperty.STONE)) {
            return false;
        }
        if (!gate.skipHeadGate() && (!type.canUseHead(material) || !OriginalToolMaterials.acceptsHead(type.definition(), material))) return false;
        for (var entry : pattern.forms().entrySet()) {
            if (entry.getKey() == 'H' || specialIngredient(type, pattern, entry.getKey()) != null) continue;
            if (entry.getKey() == 'A') {
                if (type.headPrefix() == null || !type.headPrefix().isValidFor(material)) return false;
                continue;
            }
            if (!entry.getValue().isValidFor(material)) return false;
        }
        return true;
    }

    /** Representative ingredients for the recipe viewer: one material, the shape GT6 wrote. */
    protected static NonNullList<Ingredient> ingredientsOf(GTToolType type, GTToolRecipes.Pattern pattern) {
        GTMaterial material = displayMaterial(type, pattern);
        GTMaterial handle = handleMaterial(material, pattern);
        NonNullList<Ingredient> ingredients =
                NonNullList.withSize(pattern.width() * pattern.height(), Ingredient.EMPTY);
        for (int y = 0; y < pattern.height(); y++) {
            for (int x = 0; x < pattern.width(); x++) {
                char letter = pattern.at(x, y);
                if (letter == ' ') continue;
                Ingredient ingredient = ingredientFor(type, pattern, letter, material, handle);
                if (!ingredient.isEmpty()) ingredients.set(x + y * pattern.width(), ingredient);
            }
        }
        return ingredients;
    }

    private static Ingredient ingredientFor(GTToolType type, GTToolRecipes.Pattern pattern, char letter,
                                           GTMaterial material, GTMaterial handle) {
        var special = specialIngredient(type, pattern, letter);
        if (special != null) return special;
        var tool = pattern.tools().get(letter);
        if (tool != null) {
            ItemStack stack = GTToolHelper.displayTool(tool);
            return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack);
        }
        var fixed = pattern.gate().items().get(letter);
        if (fixed != null) return Ingredient.of(fixed);
        if (letter == 'F') {
            return Ingredient.of(net.minecraft.world.item.Items.FLINT);
        }
        if (letter == 'H') {
            ItemStack stick = GTItems.getStack(MaterialPrefix.stick, handle, 1);
            return stick.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stick);
        }
        MaterialPrefix prefix = letter == 'A' ? type.headPrefix() : pattern.forms().get(letter);
        if (prefix == null) return Ingredient.EMPTY;
        ItemStack stack = GTItems.getStack(prefix, material, 1);
        return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack);
    }

    private static GTMaterial handleMaterial(@Nullable GTMaterial material, GTToolRecipes.Pattern pattern) {
        if (material == null) return com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood;
        if (pattern.gate().skipHeadGate()) return com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood;
        return pattern.normalHandle() ? OriginalToolMaterials.defaultHandle(material) : material;
    }

    public static Ingredient specialIngredient(GTToolType type, GTToolRecipes.Pattern pattern, char letter) {
        String fixed = ManualToolRecipeCatalog.specialItem(type.definition(), letter);
        if (fixed != null) {
            var id = net.minecraft.resources.ResourceLocation.parse(fixed.startsWith("#") ? fixed.substring(1) : fixed);
            return fixed.startsWith("#") ? Ingredient.of(net.minecraft.tags.ItemTags.create(id))
                    : Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id));
        }
        String family = ManualToolRecipeCatalog.specialMaterial(type.definition(), letter);
        if (family == null || !pattern.forms().containsKey(letter)) return null;
        var prefix = pattern.forms().get(letter);
        var stacks = new java.util.ArrayList<ItemStack>();
        for (var candidate : GTMaterialRegistry.allMaterials()) {
            if (!OriginalToolMaterials.inFamily(candidate, family)) continue;
            var stack = GTItems.getStack(prefix, candidate, 1);
            if (!stack.isEmpty()) stacks.add(stack);
        }
        return Ingredient.of(stacks.stream());
    }

    private static ItemStack displayResult(GTToolType type, GTToolRecipes.Pattern pattern, boolean head) {
        GTMaterial material = displayMaterial(type, pattern);
        return head ? GTItems.getStack(GTToolRecipes.headPrefix(type), material, 1)
                : GTToolItem.create(type, material, type == GTToolType.FLINT_AND_TINDER
                        ? com.gregtech.gregtech.content.material.Materials.Flint : handleMaterial(material, pattern));
    }
}
