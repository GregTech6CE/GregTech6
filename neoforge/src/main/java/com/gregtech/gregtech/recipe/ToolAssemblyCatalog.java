package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The manual (crafting-grid) recipe of every GT6 tool, as data: which material forms the tool needs
 * and what comes out.
 *
 * <p>The recipes themselves are {@link GTToolAssemblyRecipe} — a data recipe without a fixed grid —
 * so this catalogue is the single description of their contract. The JEI tool-assembly category
 * renders it, and {@code ToolAssemblyTests} checks that every entry really matches an input set the
 * recipe accepts.</p>
 *
 * <p>GT6 source: {@code Loader_Tools:332-350} registers one {@code AdvancedCraftingTool} per tool
 * type out of {@code OP} prefix forms plus an "any material" match; the head/handle split is GT6's
 * ({@code ToolCompat}'s head + handle tools), the one-piece tools use the material forms listed in
 * {@link GTToolAssemblyRecipe#directRequirements()}.</p>
 */
public final class ToolAssemblyCatalog {
    private ToolAssemblyCatalog() {}

    /**
     * One tool's manual recipe.
     *
     * @param type          the tool
     * @param inputs        one entry per required <em>item</em>; each entry lists every item that
     *                      satisfies that slot (the same material form across all valid materials).
     *                      For a shaped row the order is the pattern's reading order, spaces skipped;
     *                      for a head + handle row it is head, then handle
     * @param output        a sample assembled tool (material of the first input)
     * @param headAssembly  true when the tool is head + handle, false when it is built from forms
     * @param pattern       GT6's shaped pattern, or null for the head + handle rows
     */
    public record ToolAssemblyInfo(GTToolType type, List<List<ItemStack>> inputs, ItemStack output,
                                   boolean headAssembly,
                                   @javax.annotation.Nullable GTToolRecipes.Pattern pattern) {}

    /** Every tool the port can assemble, in GT6 id order. */
    public static List<ToolAssemblyInfo> build() {
        List<ToolAssemblyInfo> out = new ArrayList<>();
        for (GTToolType type : GTToolType.values()) {
            ToolAssemblyInfo info = build(type);
            if (info != null) out.add(info);
        }
        return out;
    }

    private static ToolAssemblyInfo build(GTToolType type) {
        if (type == GTToolType.FLINT_AND_TINDER) return flintAndTinder(type);
        // A head-based tool shows its head + handle row: that is GT6's main route, the shaped early
        // rows (rock or flint plus a stick) are the recipes JEI lists next to it.
        if (type.requiresHeadAssembly()) return headAndHandle(type);
        if (type == GTToolType.MAGNIFYING_GLASS) return magnifyingGlass(type);
        // GT6's shaped rows (one-piece tools, §29): list every material that satisfies each slot.
        var patterns = GTToolRecipes.shaped(type);
        if (!patterns.isEmpty()) return shaped(type, patterns.get(0));
        return null;
    }

    /** GT6's head + handle assembly (the shapeless {@code AdvancedCraftingTool} row). */
    private static ToolAssemblyInfo headAndHandle(GTToolType type) {
        List<ItemStack> heads = stacksOf(type.headPrefix(), type);
        List<ItemStack> handles = sticks();
        if (heads.isEmpty() || handles.isEmpty()) return null;
        List<List<ItemStack>> inputs = new ArrayList<>(List.of(heads, handles));
        // The handle may be a different material than the head (GT6 keeps the head's stats and allows
        // any valid stick), so the two lists need no alignment.
        GTMaterial head = MaterialItem.getMaterial(heads.get(0));
        GTMaterial handle = MaterialItem.getMaterial(handles.get(0));
        ItemStack output = GTToolItem.create(type, head, handle);
        if (output.isEmpty()) return null;
        return new ToolAssemblyInfo(type, List.copyOf(inputs), output, true, null);
    }

    /** One slot per pattern cell, each listing every item that fits there. */
    private static ToolAssemblyInfo shaped(GTToolType type, GTToolRecipes.Pattern pattern) {
        List<Character> letters = new ArrayList<>();
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (int y = 0; y < pattern.height(); y++) {
            for (int x = 0; x < pattern.width(); x++) {
                char letter = pattern.at(x, y);
                if (letter == ' ') continue;
                List<ItemStack> options = optionsFor(type, pattern, letter);
                if (options.isEmpty()) return null;
                letters.add(letter);
                inputs.add(new ArrayList<>(options));
            }
        }
        // The viewer (and the tests) take the first option of every slot, so one material has to come
        // first in all of them — otherwise the shown combination would mix materials and fail.
        GTMaterial sample = alignToCommonMaterial(pattern, letters, inputs);
        if (sample == null) return null;
        ItemStack output = GTToolItem.create(type, sample, sample);
        if (output.isEmpty()) return null;
        return new ToolAssemblyInfo(type, List.copyOf(inputs), output, false, pattern);
    }

    /** A slot that a material form fills: everything but the fixed vanilla items and the tools. */
    private static boolean isMaterialCell(GTToolRecipes.Pattern pattern, char letter) {
        return letter != 'F' && pattern.tools().get(letter) == null
                && !pattern.gate().items().containsKey(letter);
    }

    /**
     * Puts one material first in every material slot, so the combination shown first is one the
     * recipe accepts (the row requires a single material across all its material slots).
     *
     * @return the aligned material, or null when no material has every required form
     */
    private static GTMaterial alignToCommonMaterial(GTToolRecipes.Pattern pattern, List<Character> letters,
                                                    List<List<ItemStack>> lists) {
        int first = -1;
        for (int i = 0; i < letters.size(); i++) {
            if (isMaterialCell(pattern, letters.get(i))) {
                first = i;
                break;
            }
        }
        if (first < 0) return null;
        for (ItemStack candidate : List.copyOf(lists.get(first))) {
            GTMaterial material = MaterialItem.getMaterial(candidate);
            if (material == null) continue;
            boolean everywhere = true;
            for (int i = 0; i < letters.size() && everywhere; i++) {
                if (!isMaterialCell(pattern, letters.get(i))) continue;
                everywhere = findMaterial(lists.get(i), material) != null;
            }
            if (!everywhere) continue;
            for (int i = 0; i < letters.size(); i++) {
                if (!isMaterialCell(pattern, letters.get(i))) continue;
                List<ItemStack> rebuilt = new ArrayList<>(lists.get(i).size());
                rebuilt.add(findMaterial(lists.get(i), material));
                for (ItemStack stack : lists.get(i)) {
                    if (MaterialItem.getMaterial(stack) != material) rebuilt.add(stack);
                }
                lists.set(i, rebuilt);
            }
            return material;
        }
        return null;
    }

    /** Every item that satisfies one pattern letter. */
    private static List<ItemStack> optionsFor(GTToolType type, GTToolRecipes.Pattern pattern, char letter) {
        GTToolType tool = pattern.tools().get(letter);
        if (tool != null) {
            // A usable tool, not the bare item: a stack without GT.ToolStats cannot be crafted with.
            ItemStack stack = GTToolHelper.displayTool(tool);
            return stack.isEmpty() ? List.of() : List.of(stack);
        }
        if (letter == 'F') return List.of(new ItemStack(Items.FLINT));
        var fixed = pattern.gate().items().get(letter);
        if (fixed != null) return List.of(new ItemStack(fixed));
        if (letter == 'H') return sticks();
        MaterialPrefix prefix = letter == 'A' ? type.headPrefix() : pattern.forms().get(letter);
        if (prefix == null) return List.of();
        // The early rows take any rock, which no stone has as a tool head, so their gate is skipped.
        var gate = pattern.gate();
        if (gate.skipHeadGate() || gate.toolMaterial() != null) {
            List<ItemStack> out = new ArrayList<>();
            for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
                if (!material.isValid()) continue;
                if (gate.onlyMaterial() != null && !gate.onlyMaterial().equals(material.getName())) continue;
                if (gate.onlyStone()
                        && !material.has(com.gregtech.gregtech.api.material.MaterialProperty.STONE)) continue;
                ItemStack stack = GTItems.getStack(prefix, material, 1);
                if (!stack.isEmpty()) out.add(stack);
            }
            return out;
        }
        return stacksOf(prefix, type);
    }

    private static ItemStack findMaterial(List<ItemStack> list, GTMaterial material) {
        for (ItemStack stack : list) {
            if (MaterialItem.getMaterial(stack) == material) return stack;
        }
        return null;
    }

    /** Flint and tinder: one flint plus any striking material (GT6's {@code FLINT_AND_TINDER} row). */
    private static ToolAssemblyInfo flintAndTinder(GTToolType type) {
        List<ItemStack> striking = new ArrayList<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid()) continue;
            for (MaterialPrefix prefix : List.of(MaterialPrefix.gem, MaterialPrefix.gemChipped,
                    MaterialPrefix.rockGt, MaterialPrefix.nugget)) {
                ItemStack stack = GTItems.getStack(prefix, material, 1);
                if (!stack.isEmpty() && !sameItem(striking, stack)) striking.add(stack);
            }
        }
        if (striking.isEmpty()) return null;
        GTMaterial sample = MaterialItem.getMaterial(striking.get(0));
        // Pattern order ("T "/" F"): the striking form first, the flint in the bottom right.
        var pattern = GTToolRecipes.shaped(GTToolType.FLINT_AND_TINDER).get(0);
        return new ToolAssemblyInfo(type, List.of(List.copyOf(striking), List.of(new ItemStack(Items.FLINT))),
                GTToolItem.create(type, sample, GTMaterialRegistry.get("Flint")), false, pattern);
    }

    /** Magnifying glass: an optic lens plus a handle (GT6 lens + stick), shapeless like GT6's row. */
    private static ToolAssemblyInfo magnifyingGlass(GTToolType type) {
        List<ItemStack> lenses = stacksOf(MaterialPrefix.lens, type);
        List<ItemStack> handles = sticks();
        if (lenses.isEmpty() || handles.isEmpty()) return null;
        GTMaterial lens = MaterialItem.getMaterial(lenses.get(0));
        GTMaterial handle = MaterialItem.getMaterial(handles.get(0));
        return new ToolAssemblyInfo(type, List.of(lenses, handles),
                GTToolItem.create(type, lens, handle), true, null);
    }

    /** Every registered item of {@code prefix} whose material may be used as this tool's head. */
    public static List<ItemStack> stacksOf(MaterialPrefix prefix, GTToolType type) {
        if (prefix == null) return List.of();
        List<ItemStack> out = new ArrayList<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid() || !type.canUseHead(material)) continue;
            ItemStack stack = GTItems.getStack(prefix, material, 1);
            if (!stack.isEmpty()) out.add(stack);
        }
        return out;
    }

    /** The valid handles: GT6 only accepts sticks of materials with the WOOD or SMITHABLE flag. */
    public static List<ItemStack> sticks() {
        List<ItemStack> out = new ArrayList<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid() || !GTToolHelper.isValidStick(material)) continue;
            ItemStack stack = GTItems.getStack(MaterialPrefix.stick, material, 1);
            if (!stack.isEmpty()) out.add(stack);
        }
        return out;
    }

    private static boolean sameItem(List<ItemStack> stacks, ItemStack stack) {
        for (ItemStack other : stacks) {
            if (ItemStack.isSameItemSameComponents(other, stack)) return true;
        }
        return false;
    }
}
