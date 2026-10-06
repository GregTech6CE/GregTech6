package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.content.book.ColoredBookRules;
import net.minecraft.world.item.*;
import java.util.*;
/** Registry identities stay independent of book content and native NBT/component storage. */
public final class GTColoredBooks {
    private GTColoredBooks() {}
    private static final Map<String,java.util.function.Supplier<? extends Item>> BOOKS=new LinkedHashMap<>();
    public static void registerAll(){
        if(!BOOKS.isEmpty())return;
        for(var spec:ColoredBookRules.VARIANTS)BOOKS.put(spec.path(),GTItems.ITEMS.register(spec.path(),()->new com.gregtech.gregtech.item.ColoredBookItem(spec)));
    }
    public static ItemStack stack(int originalId){return new ItemStack(BOOKS.get(ColoredBookRules.byId(originalId).path()).get());}
    public static List<Item> items(){return BOOKS.values().stream().map(java.util.function.Supplier::get).map(i->(Item)i).toList();}
    private static boolean recipesRegistered;
    /** Original directed RM.generify conversion: one book to a blank vanilla written book. */
    public static synchronized void registerRecipes(){
        if(recipesRegistered)return;
        recipesRegistered=true;
        for(var variant:ColoredBookRules.VARIANTS) {
            var book=stack(variant.originalId());
            com.gregtech.gregtech.data.MachineRecipeMaps.generify(book,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK));
            var usb=new ItemStack(GTTechnological.get("usb1_stick"));var scanned=usb.copy();
            scanned.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Containing scanned Book"));
            com.gregtech.gregtech.data.MachineRecipeMaps.ScannerVisuals.addFakeRecipe(false,new ItemStack[]{book,usb},new ItemStack[]{scanned,book.copy()},null,null,null,512,16,0);
        }
    }
}
