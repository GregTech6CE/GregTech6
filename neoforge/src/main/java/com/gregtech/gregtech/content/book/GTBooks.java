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
 public static ItemStack bookStack(String name){String title=GTBookContent.vanillaTitleOf(name);if(title==null)return ItemStack.EMPTY;var stack=com.gregtech.gregtech.registry.GTColoredBooks.stack(ColoredBookRules.manualCover(name,pagesOf(name).size()));
  List<net.minecraft.server.network.Filterable<Component>> pages=pagesOf(name).stream().map(page->net.minecraft.server.network.Filterable.<Component>passThrough(Component.literal(page))).toList();
  stack.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,new net.minecraft.world.item.component.WrittenBookContent(net.minecraft.server.network.Filterable.passThrough(title),authorOf(name),0,pages,true));
  return stack;
 }
}
