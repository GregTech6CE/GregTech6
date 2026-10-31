package gregtech.engine.material;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.MaterialBlockLike;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Explicit equal-quantity vanilla targets. Existing registered GT stacks remain usable. */
public final class MaterialUnification {
    private MaterialUnification() {}

    private static Item requestedTarget(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof MaterialBlockLike block) {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gt6ce")) return null;
            String name = block.material().resolve().getName();
            return switch (block.prefix().getName() + "/" + name) {
                case "blockIngot/Iron", "blockSolid/Iron" -> Items.IRON_BLOCK;
                case "blockIngot/Gold", "blockSolid/Gold" -> Items.GOLD_BLOCK;
                case "blockIngot/Copper", "blockSolid/Copper" -> Items.COPPER_BLOCK;
                case "blockIngot/Netherite", "blockSolid/Netherite" -> Items.NETHERITE_BLOCK;
                case "blockGem/Diamond" -> Items.DIAMOND_BLOCK;
                case "blockGem/Emerald" -> Items.EMERALD_BLOCK;
                case "blockGem/Coal" -> Items.COAL_BLOCK;
                case "blockGem/Lapis" -> Items.LAPIS_BLOCK;
                case "blockDust/Redstone", "blockGem/Redstone" -> Items.REDSTONE_BLOCK;
                default -> null;
            };
        }
        var form = MaterialEquivalence.form(stack);
        if (form == null) return null;
        return switch (form.prefix().getName() + "/" + form.material().getName()) {
            case "ingot/Iron" -> Items.IRON_INGOT;
            case "ingot/Gold" -> Items.GOLD_INGOT;
            case "ingot/Copper" -> Items.COPPER_INGOT;
            case "ingot/Netherite" -> Items.NETHERITE_INGOT;
            case "ingot/AncientDebris" -> Items.NETHERITE_SCRAP;
            case "ingot/Brick" -> Items.BRICK;
            case "ingot/NetherBrick" -> Items.NETHER_BRICK;
            case "nugget/Iron" -> Items.IRON_NUGGET;
            case "nugget/Gold" -> Items.GOLD_NUGGET;
            case "gem/Diamond" -> Items.DIAMOND;
            case "gem/Emerald" -> Items.EMERALD;
            case "gem/EnderPearl" -> Items.ENDER_PEARL;
            case "gem/EnderEye" -> Items.ENDER_EYE;
            case "gem/NetherQuartz" -> Items.QUARTZ;
            case "gem/NetherStar" -> Items.NETHER_STAR;
            case "stick/Wood" -> Items.STICK;
            case "stick/Blaze" -> Items.BLAZE_ROD;
            case "dustTiny/Blaze" -> Items.BLAZE_POWDER;
            case "dust/Bone" -> Items.BONE_MEAL;
            case "plate/Paper" -> Items.PAPER;
            case "plate/Glass", "plateGem/Glass" -> Items.GLASS_PANE;
            case "gem/Amethyst" -> Items.AMETHYST_SHARD;
            case "gem/Coal" -> Items.COAL;
            case "gem/Charcoal" -> Items.CHARCOAL;
            case "gem/Lapis" -> Items.LAPIS_LAZULI;
            case "dust/Redstone" -> Items.REDSTONE;
            case "dust/Sugar" -> Items.SUGAR;
            case "dust/Gunpowder" -> Items.GUNPOWDER;
            case "dust/Glowstone" -> Items.GLOWSTONE_DUST;
            // GT6 stores these food materials' ingot as the matching multi-item food bar.
            case "ingot/Cheese" -> gtItem("cheese_bar");
            case "ingot/Chocolate" -> gtItem("chocolate_bar");
            case "ingot/Tofu" -> gtItem("tofu_bar");
            case "ingot/SoylentGreen" -> gtItem("emerald_green_bar");
            case "ingot/MeatRaw" -> gtItem("raw_meat_bar");
            case "ingot/MeatCooked" -> gtItem("cooked_meat_bar");
            case "ingot/FishRaw" -> gtItem("raw_fish_bar");
            case "ingot/FishCooked" -> gtItem("cooked_fish_bar");
            default -> null;
        };
    }

    /** A registered port item by id, or null when this runtime set does not have it. */
    private static Item gtItem(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
        return item == Items.AIR ? null : item;
    }

    /**
     * Material forms GT6 stores as a vanilla item, keyed by form and material. These are the
     * material's only stack of that form in the original; metals keep their GT storage blocks
     * and are deliberately absent here.
     */
    private static final Map<String, Item> VANILLA_FORM_ITEMS = vanillaFormItems();

    private static Map<String, Item> vanillaFormItems() {
        Map<String, Item> items = new HashMap<>();
        items.put(formKey("ingot", "NetherBrick"), Items.NETHER_BRICK);
        items.put(formKey("plate", "Glass"), Items.GLASS_PANE);
        items.put(formKey("plateGem", "Glass"), Items.GLASS_PANE);
        items.put(formKey("blockDust", "Stone"), Items.GRAVEL);
        items.put(formKey("blockSolid", "Stone"), Items.SMOOTH_STONE);
        items.put(formKey("blockSolid", "Endstone"), Items.END_STONE);
        items.put(formKey("blockSolid", "Netherrack"), Items.NETHERRACK);
        items.put(formKey("blockSolid", "Glass"), Items.GLASS);
        items.put(formKey("blockSolid", "Obsidian"), Items.OBSIDIAN);
        return Map.copyOf(items);
    }

    private static String formKey(String form, String material) {
        return (form + "/" + material).replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    /** The vanilla stack GT6 uses for this material form, or EMPTY when it keeps the GT item. */
    public static ItemStack vanillaFormStack(String form, GTMaterial material) {
        if (material == null || !material.isValid()) return ItemStack.EMPTY;
        Item item = VANILLA_FORM_ITEMS.get(formKey(form, material.getName()));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The explicitly replaced GT entries, shared by creative tabs, JEI and EMI. */
    public static boolean hiddenDuplicate(ItemStack stack) {
        if (stack == null || stack.isEmpty()
                || !BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("gt6ce")) return false;
        Item target = requestedTarget(stack);
        return target != null && target != stack.getItem();
    }

    /** Its item mappings are requirement keys in {@code RecipeInputs.requireKeys}; change both together. */
    public static ItemStack canonical(ItemStack stack) {
//? if >=1.21.1 {
        /*if (stack == null || stack.isEmpty() || !stack.getComponentsPatch().isEmpty() || stack.isDamaged()) return stack;
*///?} else {
        if (stack == null || stack.isEmpty() || stack.hasTag() || stack.isDamaged()) return stack;
//?}
        Item target = requestedTarget(stack);
        if (target != null) return target == stack.getItem() ? stack : new ItemStack(target, stack.getCount());
        if (target == null) {
            var form = MaterialEquivalence.form(stack);
            if (form != null) target = switch (form.prefix().getName() + "/" + form.material().getName()) {
                case "gem/Emerald" -> Items.EMERALD;
                case "gem/NetherQuartz" -> Items.QUARTZ;
                case "stick/Wood" -> Items.STICK;
                default -> null;
            };
        }
        if (target == null || target == stack.getItem()) return stack;
        // Block forms have a separately verified 9U mapping; item forms must agree with vanilla data.
        if (stack.getItem() instanceof BlockItem) return new ItemStack(target, stack.getCount());
        var expected = MaterialEquivalence.form(stack);
        var result = new ItemStack(target, stack.getCount());
        var actual = MaterialEquivalence.form(result);
        return expected != null && actual != null && actual.prefix() == expected.prefix()
                && actual.material() == expected.material() ? result : stack;
    }
}
