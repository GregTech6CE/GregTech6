package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTConstructionVariants;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTToolItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.tool.GTToolType;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * The remaining rows of GT6's shared block constructors that the port never carried.
 *
 * <p>{@code BlockMetaType} gives every full {@code BlockColored} block and its slab three rows
 * per meta: two slabs rebuild the block ({@code "X"/"X"}), a saw cuts the block into two slabs
 * ({@code "sX"}), and {@code RM.sawing(16, 16, F, 5, …)} is the Cutter equivalent. The port models
 * GT6's sixteen metas as one fixed-colour identity per {@link DyeColor} (the retained legacy palette
 * ids serve save compatibility only and its items decode to those colours), so the three
 * glass/CFoam slab families need two grid rows per colour, which they had none of. Their Cutter row
 * already exists with the dye chain in {@link DyeProcessingRecipes}.</p>
 *
 * <p>{@code Loader_Blocks} adds the reinforced-concrete hand row
 * ({@code "Se"/"X ", 'X' = concrete, 'S' = iron stick, mirrored}). GT6's {@code 'e'} is
 * {@code OreDictToolNames.drill} ({@code CR}), which GT6's Hand Drill registers, so the row
 * uses {@link GTToolType#HAND_DRILL}.</p>
 *
 * <p>Concrete, reinforced concrete, asphalt and fresh CFoam have no slab block in the port yet: GT6's
 * {@code BlockColored} gives each of them sixteen slabs (64 colour blocks) that would need
 * registration, blockstates, six models, item models and language entries before their rows could
 * exist. That is a content port, not a recipe port, so this class only covers the families whose
 * slabs already exist.</p>
 */
public final class ConstructionCraftingRecipeInjector {

    /** Families with both a full block and a six-direction slab, in registration order. */
    private static final String[] SLAB_FAMILIES = {"glass_clear", "glass_glow", "cfoam"};

    private static final List<String> REGISTERED = new ArrayList<>();
    private static final List<String> SKIPPED = new ArrayList<>();

    private ConstructionCraftingRecipeInjector() {}

    /** Recipe ids the last server start added, for tests and reports. */
    public static List<String> registeredIds() {
        return List.copyOf(REGISTERED);
    }

    /** Rows GT6 has but a palette of the port cannot express, with the reason. */
    public static List<String> skipped() {
        return List.copyOf(SKIPPED);
    }

    public static void add(GeneratedCraftingSink sink) {
        REGISTERED.clear();
        SKIPPED.clear();
        int added = 0;
        for (String family : SLAB_FAMILIES) added += slabFamily(sink, family);
        added += reinforcedConcrete(sink);
        if (added > 0) sink.commit();
        Mc.logger().info("Registered {} GT6 construction crafting rows ({} skipped)", added, SKIPPED.size());
        for (String reason : SKIPPED) Mc.logger().debug("[gregtech] construction row skipped: {}", reason);
    }

    /** The sixteen colour pairs of one family: GT6's {@code BlockColored} metas. */
    private static int slabFamily(GeneratedCraftingSink sink, String family) {
        int added = 0;
        for (DyeColor color : DyeColor.values()) {
            added += slabPair(
                    sink,
                    family,
                    color.getName(),
                    GTConstructionVariants.coloredBlock(family, color),
                    GTConstructionVariants.coloredBlock(family + "_slab", color));
        }
        return added;
    }

    private static int slabPair(GeneratedCraftingSink sink, String family, String key, Block full, Block slab) {
        String path = "construction/" + family + "/" + key;
        if (full == null || slab == null) {
            SKIPPED.add(path + ": the family has no full block and slab pair");
            return 0;
        }
        ItemStack fullStack = new ItemStack(full);
        ItemStack slabStack = new ItemStack(slab);
        int added = 0;
        added += shaped(
                sink,
                path + "/slabs_to_block",
                new String[] {"X", "X"},
                Map.of('X', Ingredient.of(slabStack)),
                fullStack,
                false);
        added += shaped(
                sink,
                path + "/block_to_slab_by_saw",
                new String[] {"sX"},
                Map.of('X', Ingredient.of(fullStack), 's', Ingredient.of(tool(GTToolType.SAW))),
                copy(slabStack, 2),
                false);
        return added;
    }

    /** GT6 {@code Loader_Blocks}: one mirrored iron-stick row per concrete colour. */
    private static int reinforcedConcrete(GeneratedCraftingSink sink) {
        ItemStack stick = GTItems.getStack(MaterialPrefix.stick, GTMaterialRegistry.get("Iron"), 1);
        ItemStack drill = tool(GTToolType.HAND_DRILL);
        if (stick.isEmpty() || drill.isEmpty()) {
            SKIPPED.add("concrete_reinforced: the iron stick or hand drill is not registered");
            return 0;
        }
        int added = 0;
        for (DyeColor color : DyeColor.values()) {
            Block concrete = GTConstructionVariants.coloredBlock("concrete", color);
            Block reinforced = GTConstructionVariants.coloredBlock("concrete_reinforced", color);
            if (concrete == null || reinforced == null) {
                SKIPPED.add("concrete_reinforced/" + color.getName() + ": the palette has no concrete pair");
                continue;
            }
            added += shaped(
                    sink,
                    "construction/concrete_reinforced/" + color.getName() + "/from_stick_and_concrete",
                    new String[] {"Se", "X "},
                    Map.of(
                            'X',
                            Ingredient.of(new ItemStack(concrete)),
                            'S',
                            Ingredient.of(stick),
                            'e',
                            Ingredient.of(drill)),
                    new ItemStack(reinforced),
                    true);
        }
        return added;
    }

    // ── recipe helpers ────────────────────────────────────────────────────

    private static int shaped(
            GeneratedCraftingSink sink,
            String path,
            String[] pattern,
            Map<Character, Ingredient> key,
            ItemStack output,
            boolean allowMirror) {
        if (output.isEmpty()) {
            SKIPPED.add(path + ": the palette has no form for this output");
            return 0;
        }
        int width = pattern[0].length();
        int height = pattern.length;
        for (int y = 0; y < height; y++) {
            if (pattern[y].length() != width) return 0;
            for (int x = 0; x < width; x++) {
                char symbol = pattern[y].charAt(x);
                if (symbol == ' ') continue;
                Ingredient ingredient = key.get(symbol);
                if (ingredient == null || ingredient.isEmpty()) {
                    SKIPPED.add(path + ": the palette has no form for key '" + symbol + "'");
                    return 0;
                }
            }
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, path);
        sink.shaped(id, "gt.construction", CraftingBookCategory.BUILDING, output.copy(), pattern, key, allowMirror);
        REGISTERED.add(id.toString());
        return 1;
    }

    private static ItemStack copy(ItemStack stack, int count) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    /** GT6's {@code craftingTool<Type>} key: the tool item, matched whatever its material and wear. */
    private static ItemStack tool(GTToolType type) {
        var item = GTToolItems.get(type);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
