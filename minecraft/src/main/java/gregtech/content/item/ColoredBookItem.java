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

package gregtech.content.item;

import gregtech.api.material.*;
import gregtech.content.book.ColoredBookRules;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;
/** Native written book, with original cover identity, paper quantity and source title/author tooltips. */
public final class ColoredBookItem extends WrittenBookItem {
    private final ColoredBookRules.Variant variant;
    public ColoredBookItem(ColoredBookRules.Variant variant){super(new Item.Properties().stacksTo(64));this.variant=variant;
        var parts=new java.util.ArrayList<gregtech.api.material.MaterialComponent>();
        parts.add(gregtech.api.material.MaterialComponent.of(gregtech.content.material.Materials.Paper,gregtech.api.material.GTValues.U*(variant.large()?6:3)));
        if(variant.emblem().equals("bronze"))parts.add(gregtech.api.material.MaterialComponent.of(gregtech.content.material.Materials.Bronze,gregtech.api.material.GTValues.U/9));
        if(variant.emblem().equals("radiation"))parts.add(gregtech.api.material.MaterialComponent.of(gregtech.content.material.Materials.Technetium,gregtech.api.material.GTValues.U/9));
        gregtech.engine.material.ItemMaterialRegistry.register(this,new gregtech.engine.material.ItemComposition(null,parts,"GT6 MultiItemBooks",true));
    }
    public ColoredBookRules.Variant variant(){return variant;}
//? if >=1.21.1 {
//?} else {
    /** GT6 UT.Books.display leaves an untitled, empty cover closed. */
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!stack.hasTag()||stack.getTag().getString("title").isBlank())return net.minecraft.world.InteractionResultHolder.pass(stack);
        return super.use(level,player,hand);
    }
//?}
    @Override public Component getName(ItemStack stack){return Component.translatable(getDescriptionId());}
//? if >=1.21.1 {
    /*@Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        var content=stack.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);String title=content==null?"":content.title().raw();String author=content==null?"":content.author();
        var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if(data!=null&&data.copyTag().contains("gt.book.title"))title=data.copyTag().getString("gt.book.title");
*///?} else {
    /** Ordinary GT6 covers have no vanilla written-book glint. */
    @Override public boolean isFoil(ItemStack stack){return stack.isEnchanted();}
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,List<Component> tooltip,TooltipFlag flag){
        var tag=stack.getTag();String title=tag==null?"":tag.getString(tag.contains("gt.book.title")?"gt.book.title":"title");String author=tag==null?"":tag.getString("author");
//?}
        if(title.isBlank())tooltip.add(Component.translatable("gt6ce.book.empty").withStyle(ChatFormatting.AQUA));
        else {tooltip.add(Component.literal(title).withStyle(ChatFormatting.AQUA));if(!author.isBlank())tooltip.add(Component.translatable("book.byAuthor",author).withStyle(ChatFormatting.AQUA));}
        if(variant.emblem().equals("bronze"))tooltip.add(Component.translatable("gt6ce.book.bronze"));
        if(variant.emblem().equals("radiation"))tooltip.add(Component.translatable("gt6ce.book.radiation"));
    }
    @Override public int getBurnTime(ItemStack stack,net.minecraft.world.item.crafting.RecipeType<?> type){return variant.large()||variant.emblem().equals("radiation")?400:200;}
}
