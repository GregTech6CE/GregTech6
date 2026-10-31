package gregtech.engine.material;

import gregtech.api.material.*;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.MaterialItem;
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
        if(stack.getItem() instanceof gregtech.engine.material.MaterialFormItem m) return new Form(m.getPrefix(),m.getMaterial().resolve());
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
    /** Check the canonical Forge form tag and the established alternate material spellings. */
    public static boolean hasForgeTag(ItemStack candidate, String path) {
        if(tagged(candidate,path))return true;
        String alias=path.replace("/aluminium","/aluminum").replace("/quartz","/nether_quartz").replace("/sulfur","/sulphur");
        String alternate=MaterialTagAliases.alternatePath(path);
        return (!alias.equals(path)&&tagged(candidate,alias))||(alternate!=null&&tagged(candidate,alternate));
    }
    /**
     * Tags {@link #hasForgeTag(ItemStack, String)} may test for this form. The recipe lookup index
     * uses these as requirement keys; change this together with {@code hasForgeTag}.
     */
    public static void addTagKeys(Form form, Set<Object> out) {
        String path = tagPath(form);
        if (path == null) return;
        addTagKey(out, path);
        String alias = path.replace("/aluminium", "/aluminum").replace("/quartz", "/nether_quartz")
                .replace("/sulfur", "/sulphur");
        if (!alias.equals(path)) addTagKey(out, alias);
        String alternate = MaterialTagAliases.alternatePath(path);
        if (alternate != null) addTagKey(out, alternate);
    }

    private static void addTagKey(Set<Object> out, String path) {
        out.add(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", path)));
        out.add(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path)));
    }

    private static boolean tagged(ItemStack candidate, String path) {
//? if >=1.21.1 {
        /*return candidate.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge",path))) || candidate.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c",path)));
*///?} else {
        return candidate.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge",path)));
//?}
    }
    /** Modern vanilla raw metals have their own recipes, not GT6 oreRaw substitutions. */
    private static boolean vanillaRaw(ItemStack stack) {
        var item = stack.getItem();
        return item == net.minecraft.world.item.Items.RAW_COPPER
                || item == net.minecraft.world.item.Items.RAW_IRON
                || item == net.minecraft.world.item.Items.RAW_GOLD
                || item == net.minecraft.world.item.Items.RAW_COPPER_BLOCK
                || item == net.minecraft.world.item.Items.RAW_IRON_BLOCK
                || item == net.minecraft.world.item.Items.RAW_GOLD_BLOCK;
    }
    /** Every accepting branch here must have a matching key in {@code RecipeInputs.requireKeys}. */
    public static boolean matches(ItemStack required,ItemStack available) {
        // Common raw_materials tags do not erase the distinct ore/raw and block quantities.
        if (required.getItem() != available.getItem() && (vanillaRaw(required) || vanillaRaw(available))) return false;
//? if >=1.21.1 {
        /*if(!required.getComponentsPatch().isEmpty()||!available.getComponentsPatch().isEmpty()||required.isDamaged()||available.isDamaged()) return false;
*///?} else {
        if(required.hasTag()||available.hasTag()||required.isDamaged()||available.isDamaged()) return false;
//?}
        var canonicalRequired = MaterialUnification.canonical(required);
        var canonicalAvailable = MaterialUnification.canonical(available);
        if (canonicalRequired.getItem() == canonicalAvailable.getItem()
                && (canonicalRequired.getItem() != required.getItem() || canonicalAvailable.getItem() != available.getItem())) return true;
        var expected=form(required); if(expected==null) return false;
        var actual=form(available);
        if(actual!=null) return expected.prefix()==actual.prefix()&&expected.material()==actual.material();
        String path=tagPath(expected);
        return path!=null&&hasForgeTag(available,path);
    }
}
