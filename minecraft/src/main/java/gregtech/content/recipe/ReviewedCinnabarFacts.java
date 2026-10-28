package gregtech.content.recipe;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceOreFollowonRows.Evidence.*;

import gregtech.api.material.*;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.*;
import gregtech.engine.prefix.BlockMaterialPrefix;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Bounded baseline fact adapter. Shared row rules live separately; unknown identities defer.
 * Source MT3709/w3803; Ores generic loop; Handlers145-150; reviewed oracle48fe504f.
 * No recipe is registered here; no list removal and no item/fluid ID changes.
 */
public final class ReviewedCinnabarFacts implements SourceOreFollowonRows.Facts {
    public static final ReviewedCinnabarFacts INSTANCE = new ReviewedCinnabarFacts();

    private ReviewedCinnabarFacts() {}

    private record Identity(int id, String name) {}

    private static final Identity CINNABAR = new Identity(9114, "Cinnabar");
    private static final Identity[] BYPRODUCTS = {
        new Identity(8333, "Redstone"),
        new Identity(160, "Sulfur"),
        new Identity(8341, "Glowstone"),
        new Identity(340, "Selenium")
    };

    private static boolean identity(GTMaterial m, Identity i) {
        return m != null
                && m.isValid()
                && !AddonMaterials.owns(m)
                && GTMaterialRegistry.isCoreMaterial(m.resolve())
                && m.resolve().getId() == i.id
                && m.resolve().getName().equals(i.name);
    }

    private static boolean hasExternalFormProfile() {
        // This bounded adapter is not an external unifier. Defer instead of manufacturing builtin replacements.
        Set<String> names = Set.of("Cinnabar", "Redstone", "Sulfur", "Glowstone", "Selenium", "SluiceSand");
        if (AddonMaterials.acceptedForms().stream().anyMatch(f -> names.contains(f.materialName()))) return true;
        for (var item : gregtech.engine.material.ItemMaterialRegistry.entries().keySet()) {
            String ns = BuiltInRegistries.ITEM.getKey(item).getNamespace();
            if (ns.equals("gt6ce") || ns.equals("minecraft")) continue; // source baseline native/vanilla forms
            var form = gregtech.engine.material.MaterialEquivalence.form(new ItemStack(item));
            if (form != null && names.contains(form.material().getName())) return true;
        }
        return false;
    }

    public static boolean supports(GTMaterial m) {
        return INSTANCE.eligible(m) == YES;
    }

    @Override
    public SourceOreFollowonRows.Evidence eligible(GTMaterial m) {
        if (!identity(m, CINNABAR) || hasExternalFormProfile()) return UNKNOWN;
        m = m.resolve();
        if (m.has(MaterialProperty.ANTIMATTER)
                || m.getToolQuality() != 0
                || m.getOreMultiplier() != 1
                || m.getOreProcessingMultiplier() != 1
                || !identity(m.getTargetCrushingMaterial(), CINNABAR)
                || m.getTargetCrushingAmount() != U
                || !identity(m.getTargetPulverMaterial(), CINNABAR)
                || m.getTargetPulverAmount() != U) return UNKNOWN;
        var by = m.getByProducts();
        if (by.size() != BYPRODUCTS.length) return UNKNOWN;
        for (int i = 0; i < by.size(); i++) if (!identity(by.get(i), BYPRODUCTS[i])) return UNKNOWN;
        return YES; // proof is this source profile, not missing flags or current generated-form presence
    }

    @Override
    public SourceOreFollowonRows.Evidence mortar(GTMaterial m) {
        return supports(m) ? YES : UNKNOWN;
    }

    @Override
    public SourceOreFollowonRows.Evidence pulverizingCinnabar(GTMaterial m) {
        return supports(m) ? YES : UNKNOWN;
    }

    @Override
    public SourceOreFollowonRows.Environment environment(GTMaterial m, long units) {
        // Baseline source DEF_ENV_TEMP293 < melting500/388/500. Never infer from missing molten fluid.
        int melt = identity(m, BYPRODUCTS[0])
                ? 500
                : identity(m, BYPRODUCTS[1]) ? 388 : identity(m, BYPRODUCTS[2]) ? 500 : -1;
        if (melt < 0 || m.resolve().getMeltingPoint() != melt)
            return new SourceOreFollowonRows.Environment(UNKNOWN, null);
        return new SourceOreFollowonRows.Environment(YES, null);
    }

    @Override
    public ItemStack direct(MaterialPrefix p, GTMaterial m, int n) {
        if (m == null || n < 1) return ItemStack.EMPTY;
        // Source-backed forms used by this bounded rule slice; allow no arbitrary legacy-only graded form.
        boolean base = identity(m, CINNABAR);
        boolean by = false;
        for (var id : BYPRODUCTS) by |= identity(m, id);
        boolean ore = p == MaterialPrefix.crushed
                || p == MaterialPrefix.crushedTiny
                || p == MaterialPrefix.crushedPurified
                || p == MaterialPrefix.crushedPurifiedTiny
                || p == MaterialPrefix.crushedCentrifuged
                || p == MaterialPrefix.crushedCentrifugedTiny;
        boolean dust = p == MaterialPrefix.dust
                || p == MaterialPrefix.dustTiny
                || p == MaterialPrefix.dustSmall
                || p == MaterialPrefix.dustDiv72;
        boolean grade = p == MaterialPrefix.gem || p == MaterialPrefix.gemFlawless || p == MaterialPrefix.gemExquisite;
        boolean sand = identity(m, new Identity(8102, "SluiceSand"));
        if (!((base && (ore || dust || grade || p == MaterialPrefix.oreRaw))
                || (by && (ore || dust))
                || (sand && dust))) return ItemStack.EMPTY;
        return GTItems.getStack(p, m.resolve(), n);
    }

    @Override
    public ItemStack blockDust(GTMaterial m, int n) {
        if (!identity(m, CINNABAR)) return ItemStack.EMPTY;
        var block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.parse("gt6ce:" + BlockMaterialPrefix.blockDust.getBlockId(m.resolve())));
        return block == Blocks.AIR ? ItemStack.EMPTY : new ItemStack(block, n);
    }

    public static List<GTMaterial> sourceWaxes() {
        int[] ids = {8235, 8236, 8237, 8238, 8239, 8240, 8280, 8281};
        String[] names = {
            "Wax", "WaxBee", "WaxRefractory", "WaxParaffin", "WaxPlant", "WaxMagic", "WaxAmnesic", "WaxSoulful"
        };
        var result = new ArrayList<GTMaterial>();
        for (int i = 0; i < ids.length; i++) {
            var m = GTMaterialRegistry.get(names[i]);
            if (identity(m, new Identity(ids[i], names[i]))) result.add(m);
        }
        return List.copyOf(result);
    }
}
