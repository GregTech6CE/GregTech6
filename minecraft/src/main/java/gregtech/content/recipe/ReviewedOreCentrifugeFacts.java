package gregtech.content.recipe;

import static gregtech.content.recipe.SourceOreFollowonRows.Evidence.*;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.material.MaterialFormItem;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Bounded original ORES/quality/solid-phase facts for reviewed purified ores.
 * Only the Centrifuge consumer uses this adapter; no global material flags or unrelated routes change.
 * Source Ores464-470; OP135-140; literal MT profiles with independently reviewed first-three byproducts.
 * The roster is data-only: unknown identities, changed facts and relevant external forms still defer.
 */
public final class ReviewedOreCentrifugeFacts implements SourceOreFollowonRows.Facts {
    public static final ReviewedOreCentrifugeFacts INSTANCE = new ReviewedOreCentrifugeFacts();

    private ReviewedOreCentrifugeFacts() {}

    private record Identity(int id, String name, int melting) {}

    private record Profile(Identity source, int quality, List<Identity> byproducts) {}

    private static final List<Profile> PROFILES = List.of(
            new Profile(
                    new Identity(290, "Copper", 1357),
                    0,
                    List.of(
                            new Identity(9115, "Cobaltite", 1014),
                            new Identity(790, "Gold", 1337),
                            new Identity(280, "Nickel", 1728))),
            new Profile(
                    new Identity(9117, "Galena", 784),
                    0,
                    List.of(
                            new Identity(9130, "Sphalerite", 540),
                            new Identity(470, "Silver", 1234),
                            new Identity(820, "Lead", 600))),
            new Profile(
                    new Identity(8346, "NetherQuartz", 1986),
                    3,
                    List.of(
                            new Identity(9160, "Barite", 1853),
                            new Identity(8747, "Efrine", 2246),
                            new Identity(8457, "VoidQuartz", 1986))),
            new Profile(
                    new Identity(9111, "Chalcopyrite", 986),
                    0,
                    List.of(
                            new Identity(9125, "Pyrite", 862),
                            new Identity(9115, "Cobaltite", 1014),
                            new Identity(480, "Cadmium", 594))),
            new Profile(
                    new Identity(9145, "Pentlandite", 1097),
                    0,
                    List.of(
                            new Identity(9104, "Hematite", 1207),
                            new Identity(160, "Sulfur", 388),
                            new Identity(9115, "Cobaltite", 1014))),
            new Profile(
                    new Identity(9130, "Sphalerite", 540),
                    0,
                    List.of(
                            new Identity(480, "Cadmium", 594),
                            new Identity(310, "Gallium", 302),
                            new Identity(300, "Zinc", 692))),
            new Profile(
                    new Identity(9132, "Tetrahedrite", 993),
                    0,
                    List.of(
                            new Identity(290, "Copper", 1357),
                            new Identity(510, "Antimony", 903),
                            new Identity(300, "Zinc", 692))),
            new Profile(
                    new Identity(9125, "Pyrite", 862),
                    0,
                    List.of(
                            new Identity(160, "Sulfur", 388),
                            new Identity(8208, "Phosphorus", 829),
                            new Identity(9104, "Hematite", 1207))),
            new Profile(
                    new Identity(280, "Nickel", 1728),
                    2,
                    List.of(
                            new Identity(9115, "Cobaltite", 1014),
                            new Identity(9116, "Cooperite", 1677),
                            new Identity(9104, "Hematite", 1207))),
            new Profile(
                    new Identity(9116, "Cooperite", 1677),
                    0,
                    List.of(
                            new Identity(460, "Palladium", 1828),
                            new Identity(280, "Nickel", 1728),
                            new Identity(770, "Iridium", 2719))),
            new Profile(
                    new Identity(9137, "YellowLimonite", 1523),
                    0,
                    List.of(
                            new Identity(280, "Nickel", 1728),
                            new Identity(9106, "BrownLimonite", 1523),
                            new Identity(9115, "Cobaltite", 1014))));

    private static boolean identity(GTMaterial material, Identity expected) {
        if (material == null || !material.isValid()) return false;
        var m = material.resolve();
        return GTMaterialRegistry.isCoreMaterial(m)
                && !AddonMaterials.owns(m)
                && m.getId() == expected.id()
                && m.getName().equals(expected.name());
    }

    private static Profile profile(GTMaterial material) {
        for (var row : PROFILES) if (identity(material, row.source())) return row;
        return null;
    }

    private static List<Identity> identities(Profile row) {
        return List.of(
                row.source(),
                row.byproducts().get(0),
                row.byproducts().get(1),
                row.byproducts().get(2));
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
            if (form != null
                    && usedForm(
                            row,
                            form.material().resolve().getName(),
                            form.prefix().getName())) return true;
        }
        return false;
    }

    private static ItemStack nativeOre(MaterialPrefix prefix, GTMaterial material, int count) {
        var stack = GTItems.getStack(prefix, material.resolve(), count);
        if (stack.isEmpty()
                || !BuiltInRegistries.ITEM
                        .getKey(stack.getItem())
                        .getNamespace()
                        .equals("gt6ce")
                || !(stack.getItem() instanceof MaterialFormItem form)
                || form.getPrefix() != prefix
                || form.getMaterial().resolve() != material.resolve()) return ItemStack.EMPTY;
        return stack;
    }

    public static boolean supports(GTMaterial material) {
        return INSTANCE.eligible(material) == YES;
    }

    @Override
    public SourceOreFollowonRows.Evidence eligible(GTMaterial material) {
        var row = profile(material);
        if (row == null || externalForms(row)) return UNKNOWN;
        var source = material.resolve();
        if (source.has(MaterialProperty.ANTIMATTER) || source.getToolQuality() != row.quality()) return UNKNOWN;
        var products = source.getByProducts();
        if (products.size() < 3) return UNKNOWN;
        for (int i = 0; i < 3; i++) {
            var expected = row.byproducts().get(i);
            if (!identity(products.get(i), expected)
                    || products.get(i).resolve().getMeltingPoint() != expected.melting()) return UNKNOWN;
        }
        // Original ORES is independently proven for every identity, not inferred from these port items.
        for (var expected : identities(row)) {
            var m = GTMaterialRegistry.get(expected.name()).resolve();
            if (!identity(m, expected)
                    || nativeOre(MaterialPrefix.crushedCentrifugedTiny, m, 1).isEmpty()) return UNKNOWN;
        }
        if (nativeOre(MaterialPrefix.crushedPurified, source, 1).isEmpty()
                || nativeOre(MaterialPrefix.crushedPurifiedTiny, source, 1).isEmpty()) return UNKNOWN;
        return YES;
    }

    @Override
    public SourceOreFollowonRows.Environment environment(GTMaterial material, long amount) {
        for (var row : PROFILES)
            for (var expected : row.byproducts()) {
                if (identity(material, expected) && material.resolve().getMeltingPoint() == expected.melting()) {
                    // All reviewed melting points are >293 K: absence is a source solid phase, not a missing holder.
                    return new SourceOreFollowonRows.Environment(YES, null);
                }
            }
        return new SourceOreFollowonRows.Environment(UNKNOWN, null);
    }

    @Override
    public ItemStack direct(MaterialPrefix prefix, GTMaterial material, int count) {
        if (material == null || count < 1) return ItemStack.EMPTY;
        if (prefix == MaterialPrefix.crushedPurified || prefix == MaterialPrefix.crushedPurifiedTiny) {
            return profile(material) == null ? ItemStack.EMPTY : nativeOre(prefix, material, count);
        }
        if (prefix == MaterialPrefix.crushedCentrifugedTiny) {
            for (var row : PROFILES)
                for (var expected : identities(row))
                    if (identity(material, expected)) return nativeOre(prefix, material, count);
        }
        return ItemStack.EMPTY;
    }
    // Deliberately no source claims for other follow-on consumers.
    @Override
    public SourceOreFollowonRows.Evidence mortar(GTMaterial material) {
        return UNKNOWN;
    }

    @Override
    public SourceOreFollowonRows.Evidence pulverizingCinnabar(GTMaterial material) {
        return UNKNOWN;
    }

    @Override
    public ItemStack blockDust(GTMaterial material, int count) {
        return ItemStack.EMPTY;
    }
}
