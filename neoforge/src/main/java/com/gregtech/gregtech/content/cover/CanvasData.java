/* Adapted from GregTech-6 Team's CoverTextureCanvas / visual recipe maps, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.cover;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.*;

/** Native block-state boundary; stored names/properties survive changes to runtime registry order. */
public final class CanvasData {
    private CanvasData() {}
    public static boolean isCanvas(ItemStack stack) { return stack.getItem() instanceof com.gregtech.gregtech.item.CanvasItem; }
    public static boolean hasImage(ItemStack stack) { return CoverStackData.readOrEmpty(stack).contains(CanvasRules.BLOCK); }
    public static CanvasRules.Image read(CompoundTag tag) {
        if (tag == null || !tag.contains(CanvasRules.BLOCK, 8)) return null;
        var properties = new TreeMap<String,String>();
        var data = tag.getCompound(CanvasRules.PROPERTIES);
        for (String key : data.getAllKeys()) if (data.contains(key, 8)) properties.put(key, data.getString(key));
        return CanvasRules.image(tag.getString(CanvasRules.BLOCK), properties);
    }
    public static CanvasRules.Image read(ItemStack stack) { return read(CoverStackData.readOrEmpty(stack)); }
    public static CompoundTag write(CanvasRules.Image image) {
        var tag = new CompoundTag(); tag.putString(CanvasRules.BLOCK, image.block());
        var properties = new CompoundTag(); image.properties().forEach(properties::putString);
        tag.put(CanvasRules.PROPERTIES, properties); return tag;
    }
    public static void write(ItemStack stack, CanvasRules.Image image) {
        var root = CoverStackData.readOrEmpty(stack); root.merge(write(image)); CoverStackData.write(stack, root);
    }
    public static CanvasRules.Image fromState(BlockState state) {
        var values = new TreeMap<String,String>();
        for (var entry : state.getValues().entrySet()) values.put(entry.getKey().getName(), valueName(entry.getKey(), entry.getValue()));
        return CanvasRules.image(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), values);
    }
    @SuppressWarnings({"unchecked","rawtypes"}) private static String valueName(Property property, Comparable value) { return property.getName(value); }
    public static BlockState state(CanvasRules.Image image) {
        if (image == null) return null;
        var id = ResourceLocation.tryParse(image.block());
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return null;
        var block = BuiltInRegistries.BLOCK.get(id); if (block == Blocks.AIR) return null;
        var state = block.defaultBlockState();
        for (var entry : image.properties().entrySet()) {
            var property = block.getStateDefinition().getProperty(entry.getKey());
            if (property != null) state = set(state, property, entry.getValue());
        }
        return state;
    }
    private static <T extends Comparable<T>> BlockState set(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(v -> state.setValue(property,v)).orElse(state);
    }
    public static CanvasRules.Image scanned(ItemStack stack) {
        if (isCanvas(stack)) return read(stack);
        var state = mapped(stack);
        if (state == null && stack.getItem() instanceof BlockItem item) {
            state = item.getBlock().defaultBlockState();
            var component = stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            var properties = new CompoundTag();
            if (component != null) component.properties().forEach(properties::putString);
            for (String key : properties.getAllKeys()) {
                var property = state.getBlock().getStateDefinition().getProperty(key);
                if (property != null) state = set(state, property, properties.getString(key));
            }
            if (item.getBlock() instanceof com.gregtech.gregtech.block.misc.ColoredConstructionBlock)
                state = state.setValue(com.gregtech.gregtech.block.misc.ColoredConstructionBlock.COLOR, com.gregtech.gregtech.block.misc.ColoredConstructionBlock.itemColor(stack));
            if (item.getBlock() instanceof com.gregtech.gregtech.block.misc.ColoredGlassBlock)
                state = state.setValue(com.gregtech.gregtech.block.misc.ColoredGlassBlock.COLOR, com.gregtech.gregtech.block.misc.ColoredGlassBlock.itemColor(stack));
        }
        return state == null ? null : fromState(state);
    }
    /** The original visual scanner's vanilla item-to-block mappings. Modern variants use states. */
    private static BlockState mapped(ItemStack stack) {
        if (stack.is(Items.FLINT_AND_STEEL)) return Blocks.FIRE.defaultBlockState();
        if (stack.is(Items.SUGAR_CANE)) return Blocks.SUGAR_CANE.defaultBlockState();
        if (stack.is(Items.SNOWBALL)) return Blocks.SNOW_BLOCK.defaultBlockState();
        if (stack.is(Items.WHEAT_SEEDS) || stack.is(Items.WHEAT)) return Blocks.WHEAT.defaultBlockState();
        if (stack.is(Items.CARROT)) return Blocks.CARROTS.defaultBlockState();
        if (stack.is(Items.POTATO) || stack.is(Items.POISONOUS_POTATO)) return Blocks.POTATOES.defaultBlockState();
        if (stack.is(Items.MELON_SEEDS) || stack.is(Items.MELON_SLICE)) return Blocks.MELON_STEM.defaultBlockState();
        if (stack.is(Items.PUMPKIN_SEEDS)) return Blocks.PUMPKIN_STEM.defaultBlockState();
        if (stack.is(Items.COCOA_BEANS)) return Blocks.COCOA.defaultBlockState();
        if (stack.is(Items.STRING)) return Blocks.COBWEB.defaultBlockState();
        if (stack.is(Items.NETHER_WART)) return Blocks.NETHER_WART.defaultBlockState();
        if (stack.is(Items.COMPARATOR)) return Blocks.COMPARATOR.defaultBlockState();
        if (stack.is(Items.REPEATER)) return Blocks.REPEATER.defaultBlockState();
        if (stack.is(Items.ENDER_PEARL)) return Blocks.NETHER_PORTAL.defaultBlockState();
        if (stack.is(Items.ENDER_EYE)) return Blocks.END_PORTAL_FRAME.defaultBlockState();
        if (stack.is(Items.WATER_BUCKET)) return Blocks.WATER.defaultBlockState();
        if (stack.is(Items.LAVA_BUCKET)) return Blocks.LAVA.defaultBlockState();
        if (stack.is(Items.CAULDRON)) return Blocks.CAULDRON.defaultBlockState();
        if (stack.is(Items.BREWING_STAND)) return Blocks.BREWING_STAND.defaultBlockState();
        if (stack.is(Items.FLOWER_POT)) return Blocks.FLOWER_POT.defaultBlockState();
        return null;
    }
    public static void tooltip(CompoundTag data, List<Component> lines) {
        var state = state(read(data));
        if (state != null) lines.add(Component.translatable("gt.tooltip.canvas.image").append(state.getBlock().getName()).withStyle(net.minecraft.ChatFormatting.AQUA));
    }
}
