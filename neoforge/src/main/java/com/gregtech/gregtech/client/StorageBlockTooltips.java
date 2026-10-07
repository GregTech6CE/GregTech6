/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from original MassStorage, Safe, DrawerQuad, BottleCrate and AdvancedCraftingTable. */
package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.storage.OriginalStorageTooltipData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Reads item state only; no hover-time tile creation, inventory access or loot generation. */
public final class StorageBlockTooltips {
    private StorageBlockTooltips() {}
    public static void massStorage(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> lines) {
        var data = CommonBlockTooltips.blockData(stack);
        massStorage(stack, data == null || context.registries() == null ? ItemStack.EMPTY : ItemStack.parseOptional(context.registries(), data.getCompound("gt.template")), lines);
    }
    public static void massStorage(ItemStack stack, ItemStack template, List<Component> lines) {
        var data = CommonBlockTooltips.blockData(stack);
        if (!template.isEmpty()) lines.add(template.getHoverName().copy().withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(Long.toString(Math.max(0, data == null ? 0 : data.getLong("gt.stored"))))
                        .withStyle(ChatFormatting.WHITE)));
        lines.add(Component.translatable(OriginalStorageTooltipData.MASS_SIZE).withStyle(ChatFormatting.AQUA)
                .append(Long.toString(OriginalStorageTooltipData.MASS_CAPACITY)));
        add(lines, OriginalStorageTooltipData.MASS_TABLE, ChatFormatting.AQUA);
        tools(lines, OriginalStorageTooltipData.massTools(data == null ? 0 : data.getInt("gt.mode")));
        facing(lines);
    }
    public static void safe(ItemStack stack, boolean keyed, List<Component> lines) {
        add(lines, keyed ? "gt.lang.key.controlled" : "gt.lang.owner.controlled", ChatFormatting.GOLD);
        loot(stack, lines);
        facing(lines);
    }
    public static void drawer(List<Component> lines) { tools(lines, OriginalStorageTooltipData.drawerTools()); facing(lines); }
    public static void bottleCrate(List<Component> lines) {
        add(lines, "gt.lang.nogui.rightclick.interact", ChatFormatting.GOLD); facing(lines);
    }
    public static void craftingTable(List<Component> lines) { tools(lines, OriginalStorageTooltipData.craftingTableTools()); facing(lines); }
    public static void bookShelf(List<Component> lines) {
        add(lines, "gt.lang.nogui.rightclick.interact", ChatFormatting.GOLD);
        tools(lines, OriginalStorageTooltipData.bookShelfTools()); facing(lines);
    }
    private static void loot(ItemStack stack, List<Component> lines) {
        var data = CommonBlockTooltips.blockData(stack);
        if (data == null) return;
        String tableId = data.getString("gt.dungeonloot");
        if (tableId.isEmpty()) return;
        var table = ResourceLocation.tryParse(tableId);
        if (table == null) return;
        String key = OriginalStorageTooltipData.lootKey(table.toString());
        lines.add(Component.literal("Contains Loot of ").withStyle(ChatFormatting.AQUA)
                .append((key == null ? Component.literal(table.toString()) : Component.translatable(key))
                        .withStyle(ChatFormatting.WHITE)));
    }
    public static void tools(List<Component> lines, List<String> keys) {
        for (var key : keys) add(lines, key, ChatFormatting.DARK_GRAY);
    }
    public static void facing(List<Component> lines) {
        lines.add(Component.translatable("gt.lang.use.x.to.toggle.facing.pre").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable("gt.lang.tool.name.wrench"))
                .append(Component.translatable("gt.lang.use.x.to.toggle.facing.post")));
    }
    private static void add(List<Component> lines, String key, ChatFormatting color) {
        lines.add(Component.translatable(key).withStyle(color));
    }
}
