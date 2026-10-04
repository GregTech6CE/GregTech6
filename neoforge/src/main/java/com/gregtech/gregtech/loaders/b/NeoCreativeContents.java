package com.gregtech.gregtech.loaders.b;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
/** Compatibility entry point for the original shared tab assignments. */
public final class NeoCreativeContents {
 private NeoCreativeContents() {}
 public static Collection<ItemStack> contents(String family){return OriginCreativeContents.contents(family);}
 public static ItemStack icon(String family){return OriginCreativeContents.icon(family);}
}
