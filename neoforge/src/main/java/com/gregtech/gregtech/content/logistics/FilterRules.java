package com.gregtech.gregtech.content.logistics;

import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.world.item.*;

/** GT6 filter templates: an untagged template is an NBT wildcard, never a tag-family wildcard. */
public final class FilterRules {
    private FilterRules() {}
    public static boolean itemMatches(ItemStack template,ItemStack candidate){
        return FilterPolicy.matches(!template.isEmpty()&&!candidate.isEmpty(),template.is(candidate.getItem()),template.getComponentsPatch().isEmpty(),ItemStack.isSameItemSameComponents(template,candidate));
    }
    public static String prefix(ItemStack stack){
        if(stack.isEmpty())return "";
        if(stack.getItem() instanceof MaterialItem item)return item.getPrefix().getName();
        if(stack.getItem() instanceof com.gregtech.gregtech.api.inventory.ContainerShapeLike item&&item.shapeId().equals("cell"))return "capcellcon";
        if(stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof com.gregtech.gregtech.api.inventory.PipeFormLike pipe)
            return pipePrefix(pipe.pipeSizeName());
        if(stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof com.gregtech.gregtech.block.MaterialBlockLike block)return block.prefix().getName();
        // Known standard material forms bridge Forge tag conventions to original prefixes.
        // Do not treat arbitrary forge tags (e.g. ores/metals) as interchangeable prefixes.
        for(String[] entry:new String[][]{{"ingots","ingot"},{"nuggets","nugget"},{"dusts","dust"},{"plates","plate"},{"rods","stick"},{"gems","gem"}})
            if(stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge",entry[0])))||stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c",entry[0]))))return entry[1];
        return "";
    }
    private static String pipePrefix(String size){
        var result=new StringBuilder("pipe");
        for(String word:size.toLowerCase(java.util.Locale.ROOT).split("_"))result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        return result.toString();
    }
}
