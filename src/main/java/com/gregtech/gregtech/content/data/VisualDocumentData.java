/* GregTech-6 Team (2024) / Gregorius Techneticies (2019), LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.data;

import com.gregtech.gregtech.content.book.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.*;

/** Portable original book/map USB fields, with NBT/component conversion at the native boundary. */
public final class VisualDocumentData {
    private VisualDocumentData() {}
    public static boolean isPages(ItemStack stack) {
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("gregtech")&&(id.getPath().equals("printed_pages")||id.getPath().equals("many_printed_pages"));
    }
    public static boolean isBook(ItemStack stack) {
        return stack.getItem() instanceof WrittenBookItem || isPages(stack)
            || stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","written_books_small")))
            || stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","written_books_large")));
    }
    public static String title(CompoundTag data) { return data==null?"":data.getString("title"); }
    public static int pages(CompoundTag data) { return data==null?0:data.getList("pages",Tag.TAG_STRING).size(); }
    /** Old short IDs are readable; modern IDs are stored as int without truncation. */
    public static int mapId(CompoundTag data) {
        return data!=null&&data.contains(VisualDocumentRules.MAP_ID,Tag.TAG_ANY_NUMERIC)
            ? data.getInt(VisualDocumentRules.MAP_ID) : -1;
    }
    public static CompoundTag mapData(int id) {
        if(id<0)return null;
        var data=new CompoundTag();data.putInt(VisualDocumentRules.MAP_ID,id);return data;
    }
    public static ItemStack printedPages(ItemStack book,boolean many) {
        var output=new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",many?"many_printed_pages":"printed_pages")));
        if(book.hasTag())output.setTag(book.getTag().copy());
        return output;
    }
    /** GT6 may store a catalog mapping instead of inline pages. Resolve it using existing content. */
    public static CompoundTag resolve(Level level,CompoundTag data) {
        if(data==null)return null;
        var resolved=data.copy();
        if(pages(resolved)==0&&!resolved.getString("book").isBlank()) {
            String mapping=resolved.getString("book");
            var book=mapping.startsWith(GTMaterialDictionary.MAPPING_PREFIX)?GTMaterialDictionary.bookStack(mapping):GTBooks.bookStack(mapping);
            if(!book.isEmpty()) {
                var content=bookData(level,book);
                if(content!=null) {resolved.put("pages",content.getList("pages",Tag.TAG_STRING).copy());resolved.putBoolean("resolved",content.getBoolean("resolved"));}
            }
        }
        return resolved;
    }
    /** Original USB details: drives show title only; sticks also show the author. */
    public static boolean tooltip(CompoundTag data,List<Component> lines,boolean details) {
        if(!title(data).isBlank()) {
            lines.add(Component.literal("Book: "+title(data)).withStyle(ChatFormatting.AQUA));
            if(details&&!data.getString("author").isBlank())lines.add(Component.literal("by "+data.getString("author")).withStyle(ChatFormatting.AQUA));
            return true;
        }
        int id=mapId(data);
        if(id>=0){lines.add(Component.literal("Map ID: "+id).withStyle(ChatFormatting.AQUA));return true;}
        return false;
    }
    public static void pagesTooltip(ItemStack stack,List<Component> lines) {
        CompoundTag data=stack.getTag();
        String title=title(data);
        if(title.isBlank())lines.add(Component.literal("These Pages are Empty").withStyle(ChatFormatting.AQUA));
        else {lines.add(Component.literal(title).withStyle(ChatFormatting.AQUA));lines.add(Component.literal("by "+data.getString("author")).withStyle(ChatFormatting.AQUA));}
    }
    public static CompoundTag bookData(Level level,ItemStack stack) {
        if(!isBook(stack)||!stack.hasTag())return null;
        var data=stack.getTag().copy();return title(data).isBlank()?null:data;
    }
    public static boolean applyBook(Level level,ItemStack stack,CompoundTag data) {
        if(data==null||title(data).isBlank())return false;
        stack.setTag(data.copy());return true;
    }
    public static int mapId(ItemStack stack) {
        if(!stack.is(Items.FILLED_MAP))return -1;
        Integer id=MapItem.getMapId(stack);return id==null?-1:id;
    }
    public static ItemStack filledMap(int id) {
        if(id<0)return ItemStack.EMPTY;
        var map=new ItemStack(Items.FILLED_MAP);map.getOrCreateTag().putInt("map",id);return map;
    }
}
