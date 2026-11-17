package gregtech.content.recipe;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceOreFollowonRows.Evidence.*;

import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialProperties;
import gregtech.api.material.*;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.MaterialFactKeys;
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
public final class SourceCinnabarFollowonAdapter implements SourceOreFollowonRows.Facts {
    public static final SourceCinnabarFollowonAdapter INSTANCE = new SourceCinnabarFollowonAdapter();

    private SourceCinnabarFollowonAdapter() {}

    private static boolean identity(GTMaterial m, MaterialFactKeys.Identity i) {
        return m != null
                && m.isValid()
                && !AddonMaterials.owns(m)
                && GTMaterialRegistry.isCoreMaterial(m.resolve())
                && m.resolve().getId() == i.id()
                && m.resolve().getName().equals(i.name());
    }

    private static boolean hasExternalFormProfile(MaterialFactKeys.CinnabarFollowonRule rule) {
        // This bounded adapter is not an external unifier. Defer instead of manufacturing builtin replacements.
        Set<String> names = new HashSet<>();
        names.add(rule.cinnabar().name());
        rule.byproducts().forEach(value -> names.add(value.name()));
        names.add(rule.sluiceSand().name());
        for (var item : gregtech.engine.material.ItemMaterialRegistry.entries().keySet()) {
            String ns = BuiltInRegistries.ITEM.getKey(item).getNamespace();
            if (ns.equals("gt6ce") || ns.equals("minecraft")) continue; // source baseline native/vanilla forms
            var form = gregtech.engine.material.MaterialEquivalence.form(new ItemStack(item));
            if (form != null && names.contains(form.material().getName())) return true;
        }
        return false;
    }

    private static MaterialFactKeys.CinnabarFollowonRule cinnabarRule() {
        var material = GTMaterialRegistry.get(9114);
        if (!material.isValid()
                || !material.getName().equals("Cinnabar")
                || !GTMaterialRegistry.isCoreMaterial(material)
                || AddonMaterials.owns(material)) return null;
        return MaterialFactKeys.value(material, MaterialFactKeys.SOURCE_CINNABAR_FOLLOWON)
                .orElse(null);
    }

    public static boolean supports(GTMaterial m) {
        return INSTANCE.eligible(m) == YES;
    }

    @Override
    public SourceOreFollowonRows.Evidence eligible(GTMaterial m) {
        var rule = cinnabarRule();
        if (rule == null || !identity(m, rule.cinnabar()) || hasExternalFormProfile(rule)) return UNKNOWN;
        m = m.resolve();
        if (m.has(MaterialProperty.ANTIMATTER)
                || m.getToolQuality() != 0
                || PublishedMaterialFacts.ore(m).oreMultiplier() != 1
                || PublishedMaterialFacts.ore(m).processingMultiplier() != 1
                || !identity(PublishedMaterialFacts.targetMaterial(m, MaterialProcessKeys.CRUSHING), rule.cinnabar())
                || PublishedMaterialFacts.target(m, MaterialProcessKeys.CRUSHING)
                                .amount()
                        != U
                || !identity(PublishedMaterialFacts.targetMaterial(m, MaterialProcessKeys.PULVERIZING), rule.cinnabar())
                || PublishedMaterialFacts.target(m, MaterialProcessKeys.PULVERIZING)
                                .amount()
                        != U) return UNKNOWN;
        var by = PublishedMaterialFacts.ore(m).byproducts().stream()
                .map(key -> GTMaterialRegistry.get(key.name()))
                .filter(GTMaterial::isValid)
                .map(GTMaterial::resolve)
                .toList();
        if (by.size() != rule.byproducts().size()) return UNKNOWN;
        for (int i = 0; i < by.size(); i++)
            if (!identity(by.get(i), rule.byproducts().get(i))) return UNKNOWN;
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
        var rule = cinnabarRule();
        if (rule == null || rule.byproducts().size() < 3) return new SourceOreFollowonRows.Environment(UNKNOWN, null);
        // Baseline source DEF_ENV_TEMP293 < melting500/388/500. Never infer from missing molten fluid.
        int melt = identity(m, rule.byproducts().get(0))
                ? 500
                : identity(m, rule.byproducts().get(1))
                        ? 388
                        : identity(m, rule.byproducts().get(2)) ? 500 : -1;
        if (melt < 0 || PublishedMaterialFacts.scalar(m, MaterialProperties.MELTING_POINT) != melt)
            return new SourceOreFollowonRows.Environment(UNKNOWN, null);
        return new SourceOreFollowonRows.Environment(YES, null);
    }

    @Override
    public ItemStack direct(MaterialPrefix p, GTMaterial m, int n) {
        if (m == null || n < 1) return ItemStack.EMPTY;
        var rule = cinnabarRule();
        if (rule == null) return ItemStack.EMPTY;
        // Source-backed forms used by this bounded rule slice; allow no arbitrary legacy-only graded form.
        boolean base = identity(m, rule.cinnabar());
        boolean by = false;
        for (var id : rule.byproducts()) by |= identity(m, id);
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
        boolean sand = identity(m, rule.sluiceSand());
        if (!((base && (ore || dust || grade || p == MaterialPrefix.oreRaw))
                || (by && (ore || dust))
                || (sand && dust))) return ItemStack.EMPTY;
        return GTItems.getStack(p, m.resolve(), n);
    }

    @Override
    public ItemStack blockDust(GTMaterial m, int n) {
        var rule = cinnabarRule();
        if (rule == null || !identity(m, rule.cinnabar())) return ItemStack.EMPTY;
        var block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.parse("gt6ce:" + BlockMaterialPrefix.blockDust.getBlockId(m.resolve())));
        return block == Blocks.AIR ? ItemStack.EMPTY : new ItemStack(block, n);
    }
}
