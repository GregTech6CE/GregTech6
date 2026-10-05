package com.gregtech.gregtech.content.book;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import java.util.List;
/** Native book serialization around the one shared source text catalog. */
public final class GTBooks {
 private GTBooks(){}
 public static List<String> names(){return GTBookContent.names();}
 public static String titleOf(String name){return GTBookContent.titleOf(name);}
 public static String authorOf(String name){return GTBookContent.authorOf(name);}
 public static List<String> pagesOf(String name){return GTBookContent.pagesOf(name);}
 public static ItemStack bookStack(String name){String title=GTBookContent.vanillaTitleOf(name);if(title==null)return ItemStack.EMPTY;var stack=com.gregtech.gregtech.registry.GTColoredBooks.stack(ColoredBookRules.manualCover(name,pagesOf(name).size()));var tag=stack.getOrCreateTag();tag.putString("title",title);tag.putString("author",authorOf(name));var pages=new net.minecraft.nbt.ListTag();for(String page:pagesOf(name))pages.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal(page))));tag.put("pages",pages);tag.putBoolean("resolved",true);return stack;}
}
