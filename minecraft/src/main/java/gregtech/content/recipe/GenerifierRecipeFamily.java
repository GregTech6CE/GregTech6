package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.Recipe;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/**
 * The generifier's single rule family: one unit of a form in a material the source generifies
 * becomes one unit of the same form in that material's canonical target. See
 * {@link GenerifyingRules} for the target table and the form applicability.
 *
 * <p>The rows are not materialized. Machines resolve one from the input stack, and viewers, the
 * dump and the addon query surface stream the same rule with {@link #rows()} so the enumeration and
 * the actual match cannot drift apart. Explicit conversions (wood dictionary blocks, stone variants,
 * food, vanilla replacements, fluids) stay separate static recipes and win the ordinary lookup first,
 * exactly like the source's {@code RM.generify} rows next to its handler.</p>
 */
public final class GenerifierRecipeFamily {
    private GenerifierRecipeFamily() {}

    /**
     * One row per distinct material item, which is the port's identity for a (form, material) pair.
     * An empty result is cached too: slot admission asks about every item a player or hopper offers.
     */
    private static final Map<Item, java.util.Optional<Recipe>> RESOLVED = new ConcurrentHashMap<>();

    /** The storage and casing blocks the source generifies on this map. */
    private static final List<BlockMaterialPrefix> BLOCK_FORMS = List.of(
            BlockMaterialPrefix.blockSolid, BlockMaterialPrefix.blockDust, BlockMaterialPrefix.blockPlate,
            BlockMaterialPrefix.blockPlateGem, BlockMaterialPrefix.blockGem, BlockMaterialPrefix.blockIngot,
            BlockMaterialPrefix.casingMachine, BlockMaterialPrefix.casingMachineDouble,
            BlockMaterialPrefix.casingMachineQuadruple, BlockMaterialPrefix.casingMachineDense);

    public static int register() {
        MachineRecipeMaps.Generifier.dynamicRecipes(GenerifierRecipeFamily::find);
        MachineRecipeMaps.Generifier.dynamicEnumeration(GenerifierRecipeFamily::rows);
        int rows = 0;
        for (Recipe ignored : rows()) rows++;
        return rows;
    }

    /** The predicate form of the rule: one item in, the same form in the target material out. */
    static Recipe find(List<ItemStack> items, List<FluidStack> fluids) {
        if (fluids != null) for (FluidStack fluid : fluids) if (fluid != null && !fluid.isEmpty()) return null;
        ItemStack input = null;
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;
            if (input != null) return null;
            input = stack;
        }
        return input == null ? null : rowFor(input.getItem());
    }

    private static Recipe rowFor(Item item) {
        var cached = RESOLVED.get(item);
        if (cached != null) return cached.orElse(null);
        var resolved = java.util.Optional.ofNullable(build(new ItemStack(item)));
        var previous = RESOLVED.putIfAbsent(item, resolved);
        return (previous == null ? resolved : previous).orElse(null);
    }

    private static Recipe build(ItemStack input) {
        var form = MaterialEquivalence.form(input);
        if (form != null && GenerifyingRules.simplifiable(form.prefix().getName()))
            return row(form.prefix(), form.material());
        if (input.getItem() instanceof BlockItem item && item.getBlock() instanceof MaterialBlockLike block
                && GenerifyingRules.simplifiable(block.prefix().getName()))
            return row(block.prefix(), block.material().resolve());
        return null;
    }

    private static Recipe row(MaterialPrefix prefix, GTMaterial material) {
        GTMaterial target = target(material);
        if (target == null) return null;
        ItemStack in = GTItems.getCreativeStack(prefix, material);
        ItemStack out = vanillaOutput(prefix.getName(), target.getName());
        if (out == null) out = GTItems.getCreativeStack(prefix, target);
        if (in.isEmpty() || out.isEmpty() || in.getItem() == out.getItem()) return null;
        return new Recipe(new ItemStack[] {in}, new ItemStack[] {out}, null, null, null, null,
                GenerifyingRules.duration(prefix.getMaterialWeight()), 0, 0)
                .withItemInputGuard(0, candidate -> candidate.getItem() == in.getItem());
    }

    private static Recipe row(BlockMaterialPrefix prefix, GTMaterial material) {
        GTMaterial target = target(material);
        if (target == null) return null;
        ItemStack in = GTBlocks.getStack(prefix, material);
        ItemStack out = vanillaOutput(prefix.getName(), target.getName());
        if (out == null) out = GTBlocks.getStack(prefix, target);
        if (in.isEmpty() || out.isEmpty() || in.getItem() == out.getItem()) return null;
        return new Recipe(new ItemStack[] {in}, new ItemStack[] {out}, null, null, null, null,
                GenerifyingRules.duration(prefix.getMaterialWeight()), 0, 0)
                .withItemInputGuard(0, candidate -> candidate.getItem() == in.getItem());
    }

    /**
     * The forms whose target the source stores as the vanilla item rather than a GT one, keyed by
     * form and target material. Every other form keeps the GT stack: a solid iron block, an iron
     * nugget or an amethyst gem has no vanilla counterpart in the source's storage rules.
     */
    private static final Map<String, Item> VANILLA_OUTPUTS = vanillaOutputs();

    private static Map<String, Item> vanillaOutputs() {
        Map<String, Item> outputs = new java.util.HashMap<>();
        outputs.put("dust/Glowstone", Items.GLOWSTONE_DUST);
        outputs.put("dust/Redstone", Items.REDSTONE);
        outputs.put("gem/Diamond", Items.DIAMOND);
        outputs.put("gem/Emerald", Items.EMERALD);
        outputs.put("gem/Coal", Items.COAL);
        outputs.put("gem/Lapis", Items.LAPIS_LAZULI);
        outputs.put("gem/NetherQuartz", Items.QUARTZ);
        outputs.put("ingot/Iron", Items.IRON_INGOT);
        outputs.put("ingot/Gold", Items.GOLD_INGOT);
        outputs.put("ingot/Coal", Items.COAL);
        outputs.put("ingot/Charcoal", Items.CHARCOAL);
        outputs.put("nugget/Gold", Items.GOLD_NUGGET);
        outputs.put("stick/Wood", Items.STICK);
        outputs.put("blockDust/Stone", Items.GRAVEL);
        outputs.put("blockDust/Redstone", Items.REDSTONE_BLOCK);
        outputs.put("blockGem/Diamond", Items.DIAMOND_BLOCK);
        outputs.put("blockGem/Emerald", Items.EMERALD_BLOCK);
        outputs.put("blockGem/Coal", Items.COAL_BLOCK);
        outputs.put("blockGem/Lapis", Items.LAPIS_BLOCK);
        outputs.put("blockIngot/Iron", Items.IRON_BLOCK);
        outputs.put("blockIngot/Gold", Items.GOLD_BLOCK);
        return Map.copyOf(outputs);
    }

    private static ItemStack vanillaOutput(String formName, String materialName) {
        Item item = VANILLA_OUTPUTS.get(formName + "/" + materialName);
        return item == null ? null : new ItemStack(item);
    }

    private static GTMaterial target(GTMaterial material) {
        int targetId = GenerifyingRules.target(material.getId());
        if (targetId == material.getId()) return null;
        GTMaterial target = GTMaterialRegistry.get(targetId);
        return target.isValid() ? target : null;
    }

    /** Every row of the rule, generated on demand in a stable order. */
    static Iterable<Recipe> rows() {
        return RuleRows::new;
    }

    private static final class RuleRows implements Iterator<Recipe> {
        private final List<GTMaterial[]> pairs = pairs();
        private final List<MaterialPrefix> itemForms = itemForms();
        private final List<BlockMaterialPrefix> blockForms = blockForms();
        private int index;
        private int totalItems = pairs.size() * itemForms.size();
        private Recipe next = advance();

        private static List<GTMaterial[]> pairs() {
            List<GTMaterial[]> pairs = new ArrayList<>();
            for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
                GTMaterial target = target(material);
                if (target != null) pairs.add(new GTMaterial[] {material, target});
            }
            return pairs;
        }

        private static List<MaterialPrefix> itemForms() {
            List<MaterialPrefix> forms = new ArrayList<>();
            for (MaterialPrefix prefix : PrefixRegistry.all())
                if (GenerifyingRules.simplifiable(prefix.getName())) forms.add(prefix);
            return forms;
        }

        private static List<BlockMaterialPrefix> blockForms() {
            List<BlockMaterialPrefix> forms = new ArrayList<>();
            for (BlockMaterialPrefix prefix : BLOCK_FORMS)
                if (GenerifyingRules.simplifiable(prefix.getName())) forms.add(prefix);
            return forms;
        }

        private Recipe advance() {
            while (index < totalItems + pairs.size() * blockForms.size()) {
                int position = index++;
                if (position < totalItems) {
                    Recipe row = row(itemForms.get(position % itemForms.size()),
                            pairs.get(position / itemForms.size())[0]);
                    if (row != null && !shadowed(row)) return row;
                } else {
                    int blockPosition = position - totalItems;
                    Recipe row = row(blockForms.get(blockPosition % blockForms.size()),
                            pairs.get(blockPosition / blockForms.size())[0]);
                    if (row != null && !shadowed(row)) return row;
                }
            }
            return null;
        }

        /**
         * The source map keeps one row per input: an explicit {@code RM.generify} call on the same
         * item replaces the handler's row, which is why the original dump never lists both.
         */
        private static boolean shadowed(Recipe row) {
            var indexed = MachineRecipeMaps.Generifier.mRecipeItemMap.get(row.mInputs[0].getItem());
            return indexed != null && !indexed.isEmpty();
        }

        @Override public boolean hasNext() { return next != null; }

        @Override public Recipe next() {
            Recipe row = next;
            if (row == null) throw new java.util.NoSuchElementException();
            next = advance();
            return row;
        }
    }
}
