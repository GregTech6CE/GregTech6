package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.SourceThermalInputRules;
import gregtech.content.recipe.SourceThermalRecyclingRules;
import gregtech.engine.machine.crucible.CrucibleInputRules;
import gregtech.engine.machine.crucible.CrucibleMaterialStack;
import gregtech.engine.machine.crucible.MoldShapes;
import gregtech.engine.material.MaterialChemistry;
import gregtech.engine.prefix.PrefixRegistry;
import java.util.List;
import java.util.Objects;

/** OP quantities and their pure consumers; native holder/recipe/persistence checks are separate. */
public final class SourcePrefixAmountContracts {
    private SourcePrefixAmountContracts() {}

    private record Expected(MaterialPrefix prefix, String name, String registry, long units) {}

    // Independent literal amounts from GT6 OP183,356,360,358,359,250 at3703e403.
    private static final List<Expected> EXPECTED = List.of(
            new Expected(MaterialPrefix.bouleGt, "bouleGt", "boule_gt", 2594592000L),
            new Expected(MaterialPrefix.plantGtBerry, "plantGtBerry", "plant_gt_berry", 72072000L),
            new Expected(MaterialPrefix.plantGtBlossom, "plantGtBlossom", "plant_gt_blossom", 72072000L),
            new Expected(MaterialPrefix.plantGtFiber, "plantGtFiber", "plant_gt_fiber", 72072000L),
            new Expected(MaterialPrefix.plantGtWart, "plantGtWart", "plant_gt_wart", 162162000L),
            new Expected(MaterialPrefix.toolHeadConstructionPickaxe, "toolHeadConstructionPickaxe",
                    "tool_head_construction_pickaxe", 1873872000L));

    public static void main(String[] args) {
        run();
        System.out.println("Source prefix amount contracts passed.");
    }

    public static void run() {
        var material = new GTMaterial(999991, "PrefixAmountContract", "Prefix Amount Contract", 0xFFFFFF);
        for (var expected : EXPECTED) {
            var prefix = expected.prefix();
            eq(expected.units(), prefix.getMaterialWeight(), expected.name() + " original quantity");
            eq(expected.name(), prefix.getName(), "source identity unchanged");
            eq(expected.registry(), prefix.getRegistryName(), "registry spelling unchanged");
            eq(prefix, PrefixRegistry.byName(expected.name()), "existing prefix object retained");
            var input = CrucibleInputRules.materialItem(material, prefix);
            eq(material, input.material, "ordinary input material unchanged");
            eq(expected.units(), input.amount, "future crucible input uses corrected amount");
            var weights = MaterialChemistry.prefixMaterialWeights(material, prefix);
            eq(1, weights.size(), "single primary tooltip component");
            eq(material, weights.get(0).material(), "tooltip material unchanged");
            eq(expected.units(), weights.get(0).amount(), "tooltip amount agrees with ingestion");
            eq(false, MoldShapes.recipes().values().stream().anyMatch(r -> r.itemPrefix() == prefix),
                    "no active casting shape requires a filled-mold migration for " + expected.name());
            eq(SourceThermalRecyclingRules.Truth.UNKNOWN, SourceThermalInputRules.prefixDecision(prefix),
                    "metadata correction does not expand thermal permission");
            eq(null, SourceThermalInputRules.form(prefix), "no implicit thermal descriptor");
        }
        // These remain separate pending fixes: changing a shared switch must not silently fix half
        // of Twig's composite data or discard excess material from an old Builderwand filled mold.
        eq(1873872000L, MaterialPrefix.toolHeadBuilderwand.getMaterialWeight(), "Builderwand retained");
        eq(648648000L, MaterialPrefix.plantGtTwig.getMaterialWeight(), "Twig retained");
        eq(1873872000L, MaterialPrefix.toolHeadPickaxe.getMaterialWeight(), "ordinary pickaxe retained");
        eq(1873872000L, MaterialPrefix.toolHeadSense.getMaterialWeight(), "sense retained");
        eq(648648000L, MaterialPrefix.dust.getMaterialWeight(), "ordinary dust retained");
        eq(72072000L, MaterialPrefix.nugget.getMaterialWeight(), "mold fallback amount retained");

        // A filled crucible is already expressed in absolute units, not an item-prefix quantity.
        var existingContent = CrucibleMaterialStack.of(material, 123456789L);
        eq(123456789L, existingContent.copy().amount, "existing absolute content is not rescaled");

        // PartRecipeLoader's three-long-rod lathe row now has exactly oneU left; its fixed four
        // gem/plate outputs already represented a full boule and must not be multiplied again.
        eq(648648000L, MaterialPrefix.bouleGt.getMaterialWeight()
                - 3L * MaterialPrefix.stickLong.getMaterialWeight(), "boule lathe remainder");
        eq(MaterialPrefix.bouleGt.getMaterialWeight(), 4L * MaterialPrefix.gem.getMaterialWeight(),
                "four-gem boule conservation");
        eq(MaterialPrefix.bouleGt.getMaterialWeight(), 4L * MaterialPrefix.plateGem.getMaterialWeight(),
                "four-plate boule conservation");
    }

    private static void eq(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual))
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }
}
