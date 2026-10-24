/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from MultiTileEntityItemInternal.addInformation, ItemBlockBase and LH.getToolTipHarvest. */
package gregtech.client;

import gregtech.internal.block.OriginalBlockTooltipRules;
import gregtech.block.BookShelfBlock;
import gregtech.content.cover.CoverItems;
import gregtech.data.BlockHarvestPolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/** Native block/item boundary for the original common rows; never creates hover-time tile entities. */
public final class CommonBlockTooltips {
    private CommonBlockTooltips() {}
    private static final String HARVEST = "gt.lang.tool.to.harvest";
    private static final String FLAMMABLE = "gt.lang.flammable";
    private static final String UNCOVER = "gt.lang.use.crowbar.to.uncover";
    private static final String ENCHANT = "gt.lang.enchantment.bonus";
    private static final Component HAND_HARVEST = Component.literal("Hand-Harvestable").withStyle(ChatFormatting.DARK_GRAY);
    private static final Set<String> OLD_HARVEST = Set.of(HARVEST, "tooltip.gt6ce.ore.hand_harvest_preferred",
            "tooltip.gt6ce.machine.harvest_wrench", "tooltip.gt6ce.machine.harvest_wrench_short",
            "tooltip.gt6ce.machine.harvest_axe", "tooltip.gt6ce.machine.harvest_shears",
            "tooltip.gt6ce.machine.harvest.tool_label", "tooltip.gt6ce.machine.hand_harvest",
            "tooltip.gt6ce.crucible.harvest_pickaxe", "tooltip.gt6ce.wire.harvest_tool",
            "gt.lang.harvest.shovel");
    private static final Set<String> FLAME_KEYS = Set.of(FLAMMABLE, "tooltip.gt6ce.flammable");

    public static void append(ItemStack stack, List<Component> lines) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return;
        Block block = item.getBlock();
        if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("gt6ce")) return;
        boolean flammable = lines.stream().anyMatch(line -> containsAnyKey(line, FLAME_KEYS));
        var state = block.defaultBlockState();
        var ore = gregtech.data.OreHostPhysics.itemParams(stack);
        for (var side : Direction.values())
            flammable |= state.getFlammability(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, side) > 0;
        // Native tooltip events include the item name at index0. Keep player names/lore intact.
        Component name = lines.isEmpty() ? null : lines.get(0);
        Component blast = lines.stream().filter(line -> line != name
                && containsAnyKey(line, Set.of("gt.lang.blastresistance"))).findFirst().orElse(null);
        lines.removeIf(line -> line != name && (containsAnyKey(line, OLD_HARVEST) || containsAnyKey(line, FLAME_KEYS)
                || containsAnyKey(line, Set.of("gt.lang.blastresistance"))
                || line.getContents().equals(HAND_HARVEST.getContents()) && line.getStyle().equals(HAND_HARVEST.getStyle())));
        var rows = new ArrayList<Component>();
        if (flammable) rows.add(Component.translatable(FLAMMABLE).withStyle(ChatFormatting.RED));
        if (block instanceof BookShelfBlock && !containsKey(lines, ENCHANT))
            rows.add(Component.translatable(ENCHANT).withStyle(ChatFormatting.DARK_GRAY));
        if (hasAttachedCover(stack) && !containsKey(lines, UNCOVER))
            rows.add(Component.translatable(UNCOVER).withStyle(ChatFormatting.DARK_GRAY));
        if (ore.isPresent()) TooltipHelper.appendBlastResistance(ore.get().resistance(), rows);
        else if (blast != null) rows.add(blast);
        else if (!containsKey(lines, "gt.lang.blastresistance"))
            TooltipHelper.appendBlastResistance(block.getExplosionResistance(), rows);
        // Display actual source drop policy for reviewed ore states, not the old LH adventure-exempt flag.
        // Preferred shovel remains visible, but hand-droppable sediment must not advertise a mandatory tier.
        rows.add(ore.isPresent() ? (!ore.get().requiresToolForDrops()
                ? Component.translatable("tooltip.gt6ce.ore.hand_harvest_preferred",
                    Component.translatable(OriginalBlockTooltipRules.harvestToolKey(ore.get().tool())).withStyle(ChatFormatting.WHITE))
                    .withStyle(ChatFormatting.DARK_GRAY)
                : harvestLine(BlockHarvestPolicy.Tool.valueOf(ore.get().tool()), false, ore.get().harvest())) : harvestLine(block));
        int insertion = lines.size();
        for (int i = 0; i < lines.size(); i++) if (containsAnyKey(lines.get(i),
                Set.of("tooltip.gt6ce.contained_materials", "tooltip.gt6ce.f3h_hint"))) {
            insertion = i; break;
        }
        lines.addAll(insertion, rows);
    }

    public static Component harvestLine(Block block) {
        return harvestLine(BlockHarvestPolicy.tool(block), BlockHarvestPolicy.handHarvestable(block), BlockHarvestPolicy.level(block));
    }
    private static Component harvestLine(BlockHarvestPolicy.Tool tool, boolean hand, int level) {
        String toolKey = OriginalBlockTooltipRules.harvestToolKey(tool.name());
        if (hand) {
            // LH hardcodes these two English phrases; the source patch has no translation keys.
            MutableComponent row = HAND_HARVEST.copy();
            if (toolKey != null) row.append(", but ").append(Component.translatable(toolKey).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" is faster").withStyle(ChatFormatting.DARK_GRAY));
            return row;
        }
        MutableComponent row = Component.translatable(HARVEST).withStyle(ChatFormatting.DARK_GRAY)
                .append(": ").append(toolKey == null ? Component.literal("Unknown").withStyle(ChatFormatting.WHITE)
                        : Component.translatable(toolKey).withStyle(ChatFormatting.WHITE));
        if (toolKey != null && OriginalBlockTooltipRules.showHarvestLevel(level)) {
            var tier = Component.literal(" (" + level).withStyle(ChatFormatting.WHITE);
            String material = OriginalBlockTooltipRules.harvestTierMaterial(level);
            if (material != null) tier.append(", ").append(Component.translatable("material.gt6ce." + material));
            row.append(tier.append(")"));
        }
        return row;
    }

    public static boolean hasAttachedCover(ItemStack stack) {
        CompoundTag data = blockData(stack);
        if (data == null) return false;
        for (int side = 0; side < 6; side++) {
            if (savedCover(data, "gt_cover_" + side) || savedCover(data, "gt.logistics.cover." + side)) return true;
        }
        return false;
    }

    private static boolean savedCover(CompoundTag data, String key) {
        if (!data.contains(key, Tag.TAG_COMPOUND)) return false;
        CompoundTag face = data.getCompound(key);
//? if >=1.21.1 {
        /*// Native ItemStack.CODEC defaults an absent count to1 and restricts explicit counts to1..99.
        int count = face.contains("count") ? face.getInt("count") : 1;
        if (count < 1 || count > 99) return false;
*///?} else {
        if (!face.contains("Count", Tag.TAG_ANY_NUMERIC) || face.getByte("Count") <= 0) return false;
//?}
        ResourceLocation id = ResourceLocation.tryParse(face.getString("id"));
        return id != null && BuiltInRegistries.ITEM.containsKey(id)
                && CoverItems.isCover(new ItemStack(BuiltInRegistries.ITEM.get(id)));
    }

    static CompoundTag blockData(ItemStack stack) {
//? if >=1.21.1 {
        /*var data = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTag();
*///?} else {
        return stack.getTagElement("BlockEntityTag");
//?}
    }

    public static boolean containsKey(List<Component> lines, String key) {
        return lines.stream().anyMatch(line -> containsAnyKey(line, Set.of(key)));
    }
    private static boolean containsAnyKey(Component line, Set<String> keys) {
        if (line.getContents() instanceof TranslatableContents contents && keys.contains(contents.getKey())) return true;
        return line.getSiblings().stream().anyMatch(sibling -> containsAnyKey(sibling, keys));
    }
}
