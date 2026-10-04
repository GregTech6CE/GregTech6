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
            if (info != null) out.addAll(materialRows(info));
        }
        return out;
    }

    /** Keep the body material fixed in each viewer row, just as GT6 registers per material. */
    private static List<ToolAssemblyInfo> materialRows(ToolAssemblyInfo info) {
        var out = new ArrayList<ToolAssemblyInfo>();
        var type = info.type();
        if (info.headAssembly()) {
            for (var headStack : info.inputs().get(0)) {
                var head = MaterialItem.getMaterial(headStack);
                var handle = com.gregtech.gregtech.content.tool.OriginalToolMaterials.defaultHandle(head);
                if (handle == null) continue;
                var handles = sticks();
                handles.removeIf(stack -> !com.gregtech.gregtech.content.tool.OriginalToolMaterials.acceptsHandle(head, MaterialItem.getMaterial(stack)));
                listsHandle(handles, handle);
                out.add(new ToolAssemblyInfo(type,List.of(List.of(headStack),handles),GTToolItem.create(type,head,handle),true,null));
            }
            return out;
        }
        var pattern = info.pattern();
        if (pattern == null) return List.of(info);
        var letters = new ArrayList<Character>();
        for (int y=0;y<pattern.height();y++) for (int x=0;x<pattern.width();x++)
            if (pattern.at(x,y)!=' ') letters.add(pattern.at(x,y));
        int first=-1;
        for (int i=0;i<letters.size();i++) if (isMaterialCell(pattern,letters.get(i)) && !(pattern.normalHandle() && letters.get(i)=='H')) { first=i;break; }
        if (first<0) return List.of(info);
        var seen = new java.util.HashSet<GTMaterial>();
        for (var candidate : info.inputs().get(first)) {
            var material = MaterialItem.getMaterial(candidate);
            if (material == null || !seen.add(material)) continue;
            if (type != GTToolType.FLINT_AND_TINDER && !com.gregtech.gregtech.content.tool.OriginalToolMaterials.acceptsHead(type.definition(),material)) continue;
            var handle = type == GTToolType.FLINT_AND_TINDER ? GTMaterialRegistry.get("Flint")
                : pattern.normalHandle() ? com.gregtech.gregtech.content.tool.OriginalToolMaterials.defaultHandle(material) : material;
            if (handle == null) continue;
            var slots = new ArrayList<List<ItemStack>>();boolean valid=true;
            for (int i=0;i<letters.size();i++) {
                char letter=letters.get(i);var options=info.inputs().get(i);
                if (pattern.normalHandle() && letter=='H') {
                    var handles=new ArrayList<>(options);
                    handles.removeIf(stack -> !com.gregtech.gregtech.content.tool.OriginalToolMaterials.acceptsHandle(material,MaterialItem.getMaterial(stack)));
                    if (handles.isEmpty()) { valid=false;break; }
                    listsHandle(handles,handle);slots.add(handles);
                } else if (isMaterialCell(pattern,letter)) {
                    var stack=findMaterial(options,material);
                    if (stack==null) { valid=false;break; }
                    slots.add(List.of(stack));
                } else slots.add(options);
            }
            if (valid) out.add(new ToolAssemblyInfo(type,List.copyOf(slots),GTToolItem.create(type,material,type==GTToolType.POCKET_MULTITOOL?GTMaterialRegistry.get("Blue"):handle),false,pattern));
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
        List<ItemStack> heads = stacksOf(type == GTToolType.MAGNIFYING_GLASS ? MaterialPrefix.lens : type.headPrefix(), type);
        heads.removeIf(stack -> !com.gregtech.gregtech.content.tool.OriginalToolMaterials.acceptsAssemblyHead(type.definition(), MaterialItem.getMaterial(stack))
                || com.gregtech.gregtech.content.tool.OriginalToolMaterials.defaultHandle(MaterialItem.getMaterial(stack)) == null);
        if (heads.isEmpty()) return null;
        GTMaterial head = MaterialItem.getMaterial(heads.get(0));
        var handles = sticks();
        handles.removeIf(stack -> !com.gregtech.gregtech.content.tool.OriginalToolMaterials.acceptsHandle(head, MaterialItem.getMaterial(stack)));
        GTMaterial handle = com.gregtech.gregtech.content.tool.OriginalToolMaterials.defaultHandle(head);
        var sample = GTItems.getStack(MaterialPrefix.stick, handle, 1);
        handles.removeIf(stack -> stack.getItem() == sample.getItem()); handles.add(0, sample);
        ItemStack output = GTToolItem.create(type, head, handle);
        if (output.isEmpty()) return null;
        return new ToolAssemblyInfo(type, List.of(heads, handles), output, true, null);
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
        GTMaterial handle = sample;
        if (pattern.normalHandle()) {
            handle = com.gregtech.gregtech.content.tool.OriginalToolMaterials.defaultHandle(sample);
            if (handle == null) return null;
            for (int i = 0; i < letters.size(); i++) if (letters.get(i) == 'H') {
                GTMaterial target = handle;
                listsHandle(inputs.get(i), target);
            }
        }
        ItemStack output = GTToolItem.create(type, sample, type==GTToolType.POCKET_MULTITOOL?GTMaterialRegistry.get("Blue"):handle);
        if (output.isEmpty()) return null;
        return new ToolAssemblyInfo(type, List.copyOf(inputs), output, false, pattern);
    }

    private static void listsHandle(List<ItemStack> handles, GTMaterial target) {
        var sample = GTItems.getStack(MaterialPrefix.stick, target, 1);
        handles.removeIf(stack -> stack.getItem() == sample.getItem());
        handles.add(0, sample);
    }

    /** A slot that a material form fills: everything but the fixed vanilla items and the tools. */
    private static boolean isMaterialCell(GTToolRecipes.Pattern pattern, char letter) {
        return letter != 'F' && ((letter != 'V' && letter != 'W') || pattern.forms().get(letter)==MaterialPrefix.toolHeadSword) && pattern.tools().get(letter) == null
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
            if (isMaterialCell(pattern, letters.get(i)) && !(pattern.normalHandle() && letters.get(i) == 'H')) {
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
                if (!isMaterialCell(pattern, letters.get(i)) || pattern.normalHandle() && letters.get(i) == 'H') continue;
                everywhere = findMaterial(lists.get(i), material) != null;
            }
            if (!everywhere) continue;
            for (int i = 0; i < letters.size(); i++) {
                if (!isMaterialCell(pattern, letters.get(i)) || pattern.normalHandle() && letters.get(i) == 'H') continue;
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
        var special = GTToolPatternRecipe.specialIngredient(type, pattern, letter);
        if (special != null) return java.util.Arrays.asList(special.getItems());
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
        MaterialPrefix prefix = letter == 'A' && type.headPrefix()!=null ? type.headPrefix() : pattern.forms().get(letter);
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
        return headAndHandle(type);
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
            if (ItemStack.isSameItemSameTags(other, stack)) return true;
        }
        return false;
    }
}
