package gregtech.engine.material;

import gregtech.api.material.*;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.MaterialItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
    /** One tag path plus the alias spellings {@link #hasForgeTag} also accepts, cached per Form. */
    private record TagForms(String path, String alias, String alternate) {
        static final TagForms NONE = new TagForms(null, null, null);

        static TagForms of(String path) {
            return new TagForms(path,
                    path.replace("/aluminium", "/aluminum").replace("/quartz", "/nether_quartz")
                            .replace("/sulfur", "/sulphur"),
                    MaterialTagAliases.alternatePath(path));
        }
    }

    private static final Map<Form, TagForms> FORM_TAGS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, TagForms> PATH_TAGS = new java.util.concurrent.ConcurrentHashMap<>();
    /** Tag identities only; membership stays a live read of the candidate stack. */
    private static final Map<String, List<TagKey<Item>>> TAG_KEYS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, TagKey<Item>> FORGE_TAGS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, TagKey<Item>> COMMON_TAGS = new java.util.concurrent.ConcurrentHashMap<>();

    public static String tagPath(Form f) {
        return forms(f).path();
    }
    /** Check the canonical Forge form tag and the established alternate material spellings. */
    public static boolean hasForgeTag(ItemStack candidate, String path) {
        var forms = pathForms(path);
        if(tagged(candidate,forms.path()))return true;
        return (!forms.alias().equals(forms.path())&&tagged(candidate,forms.alias()))
                ||(forms.alternate()!=null&&tagged(candidate,forms.alternate()));
    }
    /**
     * Tags {@link #hasForgeTag(ItemStack, String)} may test for this form. The recipe lookup index
     * uses these as requirement keys; change this together with {@code hasForgeTag}.
     */
    public static void addTagKeys(Form form, Set<Object> out) {
        String path = tagPath(form);
        if (path == null) return;
        out.addAll(tagKeys(path));
    }

    private static TagForms forms(Form form) {
        return FORM_TAGS.computeIfAbsent(form, f -> {
            String group = COMMON.get(f.prefix().getName());
            return group == null ? TagForms.NONE : pathForms(group + "/" + materialName(f.material()));
        });
    }

    private static TagForms pathForms(String path) {
        return PATH_TAGS.computeIfAbsent(path, TagForms::of);
    }

    /** Forge and common tags of the canonical path, then of its alias and alternate spellings. */
    private static List<TagKey<Item>> tagKeys(String path) {
        return TAG_KEYS.computeIfAbsent(path, p -> {
            var forms = pathForms(p);
            var keys = new ArrayList<TagKey<Item>>(8);
            addTagKeys(keys, forms.path());
            if (!forms.alias().equals(forms.path())) addTagKeys(keys, forms.alias());
            if (forms.alternate() != null) addTagKeys(keys, forms.alternate());
            return List.copyOf(keys);
        });
    }

    private static void addTagKeys(List<TagKey<Item>> out, String path) {
        out.add(forgeTag(path));
        out.add(commonTag(path));
    }

    private static TagKey<Item> forgeTag(String path) {
        return FORGE_TAGS.computeIfAbsent(path,
                p -> ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", p)));
    }

    private static TagKey<Item> commonTag(String path) {
        return COMMON_TAGS.computeIfAbsent(path,
                p -> ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", p)));
    }

    private static boolean tagged(ItemStack candidate, String path) {
//? if >=1.21.1 {
        /*return candidate.is(forgeTag(path)) || candidate.is(commonTag(path));
*///?} else {
        return candidate.is(forgeTag(path));
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
