package gregtech.content.recipe;

import gregtech.api.addon.AddonMaterials;
import gregtech.api.material.*;
import gregtech.data.MaterialPrefix;
import gregtech.registry.GTItems;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceOreFollowonRows.Evidence.*;

/** Bounded original Handlers145-150 source ORES/MORTAR/self-pulver facts.
 * The roster is independently reviewed; Centrifuge byproducts and ambient phases do not own these rows.
 * No claims for foreign/unknown forms, non-self targets, other input families or other machines.
 */
public final class ReviewedOreShredderFacts implements SourceOreFollowonRows.Facts {
    public static final ReviewedOreShredderFacts INSTANCE = new ReviewedOreShredderFacts();
    private ReviewedOreShredderFacts() {}
    private record Profile(int id, String name, int quality, boolean mortar) {}
    private static final List<Profile> PROFILES = List.of(
            new Profile(290, "Copper", 0, true),
            new Profile(9117, "Galena", 0, true),
            new Profile(8346, "NetherQuartz", 3, true),
            new Profile(9111, "Chalcopyrite", 0, true),
            new Profile(9145, "Pentlandite", 0, true),
            new Profile(9130, "Sphalerite", 0, true),
            new Profile(9132, "Tetrahedrite", 0, true),
            new Profile(9125, "Pyrite", 0, true),
            new Profile(280, "Nickel", 2, true),
            new Profile(9116, "Cooperite", 0, true),
            new Profile(9137, "YellowLimonite", 0, true));
    private static final List<MaterialPrefix> INPUTS = List.of(MaterialPrefix.crushed,
            MaterialPrefix.crushedPurified, MaterialPrefix.crushedCentrifuged,
            MaterialPrefix.crushedTiny, MaterialPrefix.crushedPurifiedTiny, MaterialPrefix.crushedCentrifugedTiny);
    private static final List<MaterialPrefix> OUTPUTS = List.of(MaterialPrefix.dust,
            MaterialPrefix.dustSmall, MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72);

    private static Profile profile(GTMaterial material) {
        if (material == null || !material.isValid()) return null;
        var m = material.resolve();
        if (!GTMaterialRegistry.isCoreMaterial(m) || AddonMaterials.owns(m)) return null;
        for (var row : PROFILES) if (m.getId() == row.id() && m.getName().equals(row.name())) return row;
        return null;
    }
    private static boolean usedPrefix(String prefix) {
        return INPUTS.stream().anyMatch(p -> p.getName().equals(prefix))
                || OUTPUTS.stream().anyMatch(p -> p.getName().equals(prefix));
    }
    private static boolean externalForms(Profile row) {
        if (AddonMaterials.acceptedForms().stream().anyMatch(f -> f.materialName().equals(row.name())
                && usedPrefix(f.prefixName()))) return true;
        for (var item : ItemMaterialRegistry.entries().keySet()) {
            String namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
            if (namespace.equals("gt6ce") || namespace.equals("minecraft")) continue;
            var form = MaterialEquivalence.form(new ItemStack(item));
            if (form != null && form.material().resolve().getName().equals(row.name())
                    && usedPrefix(form.prefix().getName())) return true;
        }
        return false;
    }
    private static ItemStack nativeForm(MaterialPrefix prefix, GTMaterial material, int count) {
        var stack = GTItems.getStack(prefix, material.resolve(), count);
        if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("gt6ce")
                || !(stack.getItem() instanceof MaterialFormItem form) || form.getPrefix() != prefix
                || form.getMaterial().resolve() != material.resolve()) return ItemStack.EMPTY;
        return stack;
    }
    public static boolean supports(GTMaterial material) { return INSTANCE.eligible(material) == YES; }
    @Override public SourceOreFollowonRows.Evidence eligible(GTMaterial material) {
        var row = profile(material);
        if (row == null) return UNKNOWN;
        var m = material.resolve();
        if (m.has(MaterialProperty.ANTIMATTER) || m.getToolQuality() != row.quality()
                || m.getTargetPulverMaterial().resolve() != m || m.getTargetPulverAmount() != U
                || externalForms(row)) return UNKNOWN;
        // MORTAR/ORES are positive source facts, not inferred from port item existence.
        for (var prefix : INPUTS) if (nativeForm(prefix, m, 1).isEmpty()) return UNKNOWN;
        for (var prefix : OUTPUTS) if (nativeForm(prefix, m, 1).isEmpty()) return UNKNOWN;
        return YES;
    }
    @Override public SourceOreFollowonRows.Evidence mortar(GTMaterial material) {
        var row = profile(material);
        return row == null || !supports(material) ? UNKNOWN : row.mortar() ? YES : NO;
    }
    @Override public ItemStack direct(MaterialPrefix prefix, GTMaterial material, int count) {
        if (count < 1 || profile(material) == null || (!INPUTS.contains(prefix) && !OUTPUTS.contains(prefix)))
            return ItemStack.EMPTY;
        return nativeForm(prefix, material, count);
    }
    @Override public SourceOreFollowonRows.Evidence pulverizingCinnabar(GTMaterial material) { return UNKNOWN; }
    @Override public SourceOreFollowonRows.Environment environment(GTMaterial material, long amount) {
        return new SourceOreFollowonRows.Environment(UNKNOWN, null);
    }
    @Override public ItemStack blockDust(GTMaterial material, int count) { return ItemStack.EMPTY; }
}
