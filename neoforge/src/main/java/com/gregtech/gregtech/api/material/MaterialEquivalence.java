package com.gregtech.gregtech.api.material;

import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Only equal material forms may substitute. Generic tags (all ingots/ores) never imply equality. */
public final class MaterialEquivalence {
    public record Form(MaterialPrefix prefix, GTMaterial material) {}
    private static final Map<String,String> COMMON = Map.ofEntries(
        Map.entry("ingot","ingots"),Map.entry("nugget","nuggets"),Map.entry("dust","dusts"),
        Map.entry("dustSmall","small_dusts"),Map.entry("dustTiny","tiny_dusts"),Map.entry("gem","gems"),
        Map.entry("plate","plates"),Map.entry("stick","rods"),Map.entry("stickLong","long_rods"),
        Map.entry("gearGt","gears"),Map.entry("ring","rings"),Map.entry("bolt","bolts"),Map.entry("screw","screws"),
        Map.entry("foil","foils"),Map.entry("wireGt01","wires"),Map.entry("oreRaw","raw_materials"));
    private MaterialEquivalence() {}
    public static Form form(ItemStack stack) {
        if(stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem m) return new Form(m.getPrefix(),m.getMaterial().resolve());
        var data=ItemMaterialRegistry.get(stack).orElse(null);
        if(data==null||data.prefix()==null||data.components().size()!=1||data.amount()!=data.prefix().getMaterialWeight()) return null;
        return new Form(data.prefix(),data.material().resolve());
    }
    public static String materialName(GTMaterial m) {
        if(m.getName().equals("NetherQuartz")) return "quartz";
        if(m.getName().equals("Tungstensteel")) return "tungsten_steel";
        return m.getName().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase(Locale.ROOT);
    }
    public static String tagPath(Form f) {
        String group=COMMON.get(f.prefix().getName());
        return group==null?null:group+"/"+materialName(f.material());
    }
    /** Check the canonical Forge form tag and the established alternate material spelling. */
    public static boolean hasForgeTag(ItemStack candidate, String path) {
        if(candidate.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge",path))) || candidate.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c",path))))return true;
        String alias=path.replace("/aluminium","/aluminum").replace("/quartz","/nether_quartz").replace("/sulfur","/sulphur");
        return !alias.equals(path)&&(candidate.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge",alias))) || candidate.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c",alias))));
    }
    public static boolean matches(ItemStack required,ItemStack available) {
        if(!required.getComponentsPatch().isEmpty()||!available.getComponentsPatch().isEmpty()||required.isDamaged()||available.isDamaged()) return false;
        var expected=form(required); if(expected==null) return false;
        var actual=form(available);
        if(actual!=null) return expected.prefix()==actual.prefix()&&expected.material()==actual.material();
        String path=tagPath(expected);
        return path!=null&&hasForgeTag(available,path);
    }
}
