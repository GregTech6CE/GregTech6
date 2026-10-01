package com.gregtech.gregtech.content.book;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GT6's <b>Material Dictionary</b> ({@code MultiItemBooks:59-60}, metas 32002/32003; loot variant
 * {@code 32766} "Dusty Material Dictionary").
 *
 * <p>GT6 builds one of these per material and stores it on the material
 * ({@code OreDictMaterial.mDictionaryBook}, created by {@code UT.java:762} as
 * {@code ST.book("Material_Dictionary_<name>")}); the pages come from
 * {@code UT.Books.addMaterialDictionary}, which walks the material's data: chemical components,
 * alloy recipes it takes part in, byproducts, tool stats, the enchantments its tools accept, its
 * properties, its machine and ore flags, and the materials that smelt/solidify/burn/pulverize into
 * it. The loot table {@code gt.matdicts} hands out one row per material
 * ({@code Loader_Loot:359-364}) and the Dusty Material Dictionary rolls that table
 * ({@code MultiItemBooks:68} + {@code Behavior_Drop_Loot}).</p>
 *
 * <p>The port generates the same kind of page from its own material data. This is the first slice —
 * identity, composition, the forms the material actually has, tool stats, properties and ore/byproduct
 * links; GT6's alloy/enchantment/target matrices are the documented remainder (see §31 of the porting
 * notes). Pages are plain strings inside a vanilla written book, exactly like {@link GTBooks}, so the
 * player opens them with the vanilla book screen.</p>
 */
public final class GTMaterialDictionary {

    /** GT6's title mapping prefix: {@code Material_Dictionary_<mNameInternal>}. */
    public static final String MAPPING_PREFIX = "Material_Dictionary_";

    /** Author GT6 writes into every book ({@code Loader_Books}). */
    private static final String AUTHOR = "Gregorius Techneticies";

    /** GT6 drops pages of 256 characters or more; the port keeps the same rule. */
    private static final int MAX_PAGE_LENGTH = 256;

    private GTMaterialDictionary() {}

    /** The material a dictionary stack belongs to, or null. */
    public static GTMaterial materialOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        String mapping = tag.contains("gt.material") ? tag.getString("gt.material")
                : tag.contains("book") ? tag.getString("book") : "";
        if (mapping.startsWith(MAPPING_PREFIX)) mapping = mapping.substring(MAPPING_PREFIX.length());
        return mapping.isEmpty() ? null : GTMaterialRegistry.get(mapping);
    }

    /** GT6's mapping name for a material, e.g. {@code Material_Dictionary_Iron}. */
    public static String mapping(GTMaterial material) {
        return MAPPING_PREFIX + material.getName();
    }

    /** The dictionary of a material as a written book, or an empty stack when it has no pages. */
    public static ItemStack bookStack(GTMaterial material) {
        if (material == null || !material.isValid()) return ItemStack.EMPTY;
        List<String> pages = pages(material);
        if (pages.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("title", MaterialPresentation.name(material) + " - Material Dictionary");
        tag.putString("author", AUTHOR);
        tag.putString("book", mapping(material));
        tag.putString("gt.material", mapping(material));
        ListTag list = new ListTag();
        for (String page : pages) list.add(StringTag.valueOf(page));
        tag.put("pages", list);
        stack.setHoverName(Component.literal(MaterialPresentation.name(material) + " - Material Dictionary"));
        return stack;
    }

    /** The dictionary for a GT6 mapping name ({@code Material_Dictionary_Xxx}), or an empty stack. */
    public static ItemStack bookStack(String mapping) {
        if (mapping == null || !mapping.startsWith(MAPPING_PREFIX)) return ItemStack.EMPTY;
        return bookStack(GTMaterialRegistry.get(mapping.substring(MAPPING_PREFIX.length())));
    }

    /** Every material that has a dictionary, in registry order. */
    public static List<GTMaterial> materials() {
        List<GTMaterial> out = new ArrayList<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.isValid() && !pages(material).isEmpty()) out.add(material);
        }
        return out;
    }

    // ── pages ─────────────────────────────────────────────────────────────

    public static List<String> pages(GTMaterial material){return GTMaterialDictionaryPages.pages(material,GTMaterialDictionary::forms,GTMaterialDictionary::ore);}
    private static String forms(GTMaterial material) {
        List<String> names = new ArrayList<>();
        for (MaterialPrefix form : com.gregtech.gregtech.api.prefix.PrefixRegistry.all()) {
            if (!form.isValidFor(material)) continue;
            if (GTItems.getStack(form, material, 1).isEmpty()) continue;
            names.add(form.getName());
        }
        if (names.isEmpty()) return null;
        StringBuilder out = new StringBuilder("Forms available (" + names.size() + "):\n");
        for (int i = 0; i < names.size(); i++) {
            out.append(names.get(i));
            out.append((i + 1) % 3 == 0 ? '\n' : ", ");
        }
        return out.toString();
    }
    private static String ore(GTMaterial material) {
        // The port's ore blocks are blocks (BlockMaterialPrefix.ore), the raw ore is an item.
        ItemStack ore = com.gregtech.gregtech.registry.GTBlocks.getStack(
                com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.ore, material);
        ItemStack raw = GTItems.getStack(MaterialPrefix.oreRaw, material, 1);
        if (ore.isEmpty() && raw.isEmpty()) return null;
        StringBuilder out = new StringBuilder();
        if (!ore.isEmpty()) out.append("This material generates ore.\n");
        if (!raw.isEmpty()) out.append("Raw ore: ").append(raw.getHoverName().getString()).append('\n');
        if (!material.getByProducts().isEmpty()) {
            out.append("Byproducts: ");
            int i = 0;
            for (GTMaterial byproduct : material.getByProducts()) {
                out.append(i++ > 0 ? ", " : "").append(byproduct.getName());
            }
        }
        return out.toString();
    }
}
