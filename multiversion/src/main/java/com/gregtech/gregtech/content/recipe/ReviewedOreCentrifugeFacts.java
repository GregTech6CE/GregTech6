package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.material.MaterialFormItem;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import static com.gregtech.gregtech.content.recipe.SourceOreFollowonRows.Evidence.*;

/** Bounded original ORES/quality/solid-phase facts for purified Copper, Galena and Nether Quartz.
 * Only the Centrifuge consumer uses this adapter; no global material flags or unrelated routes change.
 * Source MT418/586/3040,3717/3034,179/1554/3205; Ores464-470; OP135-140.
 */
public final class ReviewedOreCentrifugeFacts implements SourceOreFollowonRows.Facts {
    public static final ReviewedOreCentrifugeFacts INSTANCE = new ReviewedOreCentrifugeFacts();
    private ReviewedOreCentrifugeFacts() {}
    private record Identity(int id, String name, int melting) {}
    private record Profile(Identity source, int quality, List<Identity> byproducts) {}
    private static final List<Profile> PROFILES = List.of(
            new Profile(new Identity(290, "Copper", 1357), 0, List.of(
                    new Identity(9115, "Cobaltite", 1014), new Identity(790, "Gold", 1337), new Identity(280, "Nickel", 1728))),
            new Profile(new Identity(9117, "Galena", 784), 0, List.of(
                    new Identity(9130, "Sphalerite", 540), new Identity(470, "Silver", 1234), new Identity(820, "Lead", 600))),
            new Profile(new Identity(8346, "NetherQuartz", 1986), 3, List.of(
                    new Identity(9160, "Barite", 1853), new Identity(8747, "Efrine", 2246), new Identity(8457, "VoidQuartz", 1986))));

    private static boolean identity(GTMaterial material, Identity expected) {
        if (material == null || !material.isValid()) return false;
        var m = material.resolve();
        return GTMaterialRegistry.isCoreMaterial(m) && !AddonMaterials.owns(m)
                && m.getId() == expected.id() && m.getName().equals(expected.name());
    }
    private static Profile profile(GTMaterial material) {
        for (var row : PROFILES) if (identity(material, row.source())) return row;
        return null;
    }
    private static List<Identity> identities(Profile row) {
        return List.of(row.source(), row.byproducts().get(0), row.byproducts().get(1), row.byproducts().get(2));
    }
    private static boolean usedForm(Profile row, String material, String prefix) {
        if (prefix.equals(MaterialPrefix.crushedCentrifugedTiny.getName()))
            return identities(row).stream().anyMatch(identity -> identity.name().equals(material));
        return row.source().name().equals(material)
                && (prefix.equals(MaterialPrefix.crushedPurified.getName())
                || prefix.equals(MaterialPrefix.crushedPurifiedTiny.getName()));
    }
    private static boolean externalForms(Profile row) {
        // Ownership is form-specific: an unrelated addon plate/custom prefix does not
        // own this Centrifuge input or output. The exact used ore-form roles still defer.
        if (AddonMaterials.acceptedForms().stream()
                .anyMatch(form -> usedForm(row, form.materialName(), form.prefixName()))) return true;
        for (var item : ItemMaterialRegistry.entries().keySet()) {
            String namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
            if (namespace.equals("gt6ce") || namespace.equals("minecraft")) continue;
            var form = MaterialEquivalence.form(new ItemStack(item));
            if (form != null && usedForm(row, form.material().resolve().getName(), form.prefix().getName())) return true;
        }
        return false;
    }
    private static ItemStack nativeOre(MaterialPrefix prefix, GTMaterial material, int count) {
        var stack = GTItems.getStack(prefix, material.resolve(), count);
        if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("gt6ce")
                || !(stack.getItem() instanceof MaterialFormItem form) || form.getPrefix() != prefix
                || form.getMaterial().resolve() != material.resolve()) return ItemStack.EMPTY;
        return stack;
    }
    public static boolean supports(GTMaterial material) { return INSTANCE.eligible(material) == YES; }
    @Override public SourceOreFollowonRows.Evidence eligible(GTMaterial material) {
        var row = profile(material);
        if (row == null || externalForms(row)) return UNKNOWN;
        var source = material.resolve();
        if (source.has(MaterialProperty.ANTIMATTER) || source.getToolQuality() != row.quality()) return UNKNOWN;
        var products = source.getByProducts();
        if (products.size() < 3) return UNKNOWN;
        for (int i = 0; i < 3; i++) {
            var expected = row.byproducts().get(i);
            if (!identity(products.get(i), expected) || products.get(i).resolve().getMeltingPoint() != expected.melting()) return UNKNOWN;
        }
        // Original ORES is independently proven for every identity, not inferred from these port items.
        for (var expected : identities(row)) {
            var m = GTMaterialRegistry.get(expected.name()).resolve();
            if (!identity(m, expected) || nativeOre(MaterialPrefix.crushedCentrifugedTiny, m, 1).isEmpty()) return UNKNOWN;
        }
        if (nativeOre(MaterialPrefix.crushedPurified, source, 1).isEmpty()
                || nativeOre(MaterialPrefix.crushedPurifiedTiny, source, 1).isEmpty()) return UNKNOWN;
        return YES;
    }
    @Override public SourceOreFollowonRows.Environment environment(GTMaterial material, long amount) {
        for (var row : PROFILES) for (var expected : row.byproducts()) {
            if (identity(material, expected) && material.resolve().getMeltingPoint() == expected.melting()) {
                // All reviewed melting points are >293 K: absence is a source solid phase, not a missing holder.
                return new SourceOreFollowonRows.Environment(YES, null);
            }
        }
        return new SourceOreFollowonRows.Environment(UNKNOWN, null);
    }
    @Override public ItemStack direct(MaterialPrefix prefix, GTMaterial material, int count) {
        if (material == null || count < 1) return ItemStack.EMPTY;
        if (prefix == MaterialPrefix.crushedPurified || prefix == MaterialPrefix.crushedPurifiedTiny) {
            return profile(material) == null ? ItemStack.EMPTY : nativeOre(prefix, material, count);
        }
        if (prefix == MaterialPrefix.crushedCentrifugedTiny) {
            for (var row : PROFILES) for (var expected : identities(row))
                if (identity(material, expected)) return nativeOre(prefix, material, count);
        }
        return ItemStack.EMPTY;
    }
    // Deliberately no source claims for other follow-on consumers.
    @Override public SourceOreFollowonRows.Evidence mortar(GTMaterial material) { return UNKNOWN; }
    @Override public SourceOreFollowonRows.Evidence pulverizingCinnabar(GTMaterial material) { return UNKNOWN; }
    @Override public ItemStack blockDust(GTMaterial material, int count) { return ItemStack.EMPTY; }
}
