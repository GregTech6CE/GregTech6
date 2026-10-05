/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package com.gregtech.gregtech.item;
import com.gregtech.gregtech.content.book.ColoredBookRules;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;
/** Native written book, with original cover identity, paper quantity and source title/author tooltips. */
public final class ColoredBookItem extends WrittenBookItem {
    private final ColoredBookRules.Variant variant;
    public ColoredBookItem(ColoredBookRules.Variant variant){super(new Item.Properties().stacksTo(64));this.variant=variant;
        var parts=new java.util.ArrayList<com.gregtech.gregtech.api.material.MaterialComponent>();
        parts.add(com.gregtech.gregtech.api.material.MaterialComponent.of(com.gregtech.gregtech.content.material.Materials.Paper,com.gregtech.gregtech.api.material.GTValues.U*(variant.large()?6:3)));
        if(variant.emblem().equals("bronze"))parts.add(com.gregtech.gregtech.api.material.MaterialComponent.of(com.gregtech.gregtech.content.material.Materials.Bronze,com.gregtech.gregtech.api.material.GTValues.U/9));
        if(variant.emblem().equals("radiation"))parts.add(com.gregtech.gregtech.api.material.MaterialComponent.of(com.gregtech.gregtech.content.material.Materials.Technetium,com.gregtech.gregtech.api.material.GTValues.U/9));
        com.gregtech.gregtech.api.material.ItemMaterialRegistry.register(this,new com.gregtech.gregtech.api.material.ItemComposition(null,parts,"GT6 MultiItemBooks",true));
    }
    public ColoredBookRules.Variant variant(){return variant;}
    /** GT6 UT.Books.display leaves an untitled, empty cover closed. */
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!stack.hasTag()||stack.getTag().getString("title").isBlank())return net.minecraft.world.InteractionResultHolder.pass(stack);
        return super.use(level,player,hand);
    }
    @Override public Component getName(ItemStack stack){return Component.translatable(getDescriptionId());}
    /** Ordinary GT6 covers have no vanilla written-book glint. */
    @Override public boolean isFoil(ItemStack stack){return stack.isEnchanted();}
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,List<Component> tooltip,TooltipFlag flag){
        var tag=stack.getTag();String title=tag==null?"":tag.getString(tag.contains("gt.book.title")?"gt.book.title":"title");String author=tag==null?"":tag.getString("author");
        if(title.isBlank())tooltip.add(Component.translatable("gregtech.book.empty").withStyle(ChatFormatting.AQUA));
        else {tooltip.add(Component.literal(title).withStyle(ChatFormatting.AQUA));if(!author.isBlank())tooltip.add(Component.translatable("book.byAuthor",author).withStyle(ChatFormatting.AQUA));}
        if(variant.emblem().equals("bronze"))tooltip.add(Component.translatable("gregtech.book.bronze"));
        if(variant.emblem().equals("radiation"))tooltip.add(Component.translatable("gregtech.book.radiation"));
    }
    @Override public int getBurnTime(ItemStack stack,net.minecraft.world.item.crafting.RecipeType<?> type){return variant.large()||variant.emblem().equals("radiation")?400:200;}
}
