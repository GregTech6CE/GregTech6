/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Viewer identity for the original gt.usb.data / gt.usb.tier file layout. */
package com.gregtech.gregtech.api.recipe;

import com.gregtech.gregtech.content.data.UsbDataMedia;
import com.gregtech.gregtech.content.data.UsbDataRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.stream.Collectors;

/** Match USB files in recipe browsers without making a renamed file a different ingredient. */
public final class UsbRecipeDisplayBinding {
    private UsbRecipeDisplayBinding() {}
    public static List<Item> sticks() {
        var items=new ArrayList<Item>(4);
        for(int tier=1;tier<=4;tier++) {
            var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech","usb"+tier+"_stick"));
            if(item!=Items.AIR)items.add(item);
        }
        return List.copyOf(items);
    }
    public static String subtype(ItemStack stack) {
        if(stack==null||stack.isEmpty())return "";
        var tag=UsbDataMedia.tag(stack);
        if(tag==null||!tag.contains(UsbDataRules.DATA,Tag.TAG_COMPOUND))return "";
        var file=tag.getCompound(UsbDataRules.DATA);
        if(file.isEmpty())return "";
        return UsbDataMedia.stickFileTier(stack)+":"+canonical(file);
    }
    /** Compound insertion order is irrelevant; list order and tag value/type remain significant. */
    private static String canonical(Tag tag) {
        if(tag instanceof CompoundTag compound)return compound.getAllKeys().stream().sorted()
                .map(key->StringTag.quoteAndEscape(key)+":"+canonical(compound.get(key)))
                .collect(Collectors.joining(",","{","}"));
        if(tag instanceof ListTag list) {
            var values=new ArrayList<String>(list.size());
            for(var value:list)values.add(canonical(value));
            return String.join(",",values).transform(value->"["+value+"]");
        }
        return tag.toString();
    }
}
