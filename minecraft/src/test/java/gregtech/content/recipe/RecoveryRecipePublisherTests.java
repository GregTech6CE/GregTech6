package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class RecoveryRecipePublisherTests {
    private RecoveryRecipePublisherTests() {}

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void genericRecoveryConflictsAreExplicitAndSpecificWins(GameTestHelper helper) {
        var input = new ItemStack(Items.DRAGON_EGG);
        Recipe generic = generic(row(input, new ItemStack(Items.ARROW), "test:fallback"));
        Recipe specific = explicit(row(input, new ItemStack(Items.SPECTRAL_ARROW), "test:owner"));
        try {
            helper.assertTrue(
                    RecoveryRecipePublisher.publish(MachineRecipeMaps.Shredder, generic) != null,
                    "first generic row publishes");
            helper.assertTrue(
                    RecoveryRecipePublisher.publish(MachineRecipeMaps.Shredder, copy(generic)) == null,
                    "equivalent generic row deduplicates");

            boolean conflict = false;
            try {
                RecoveryRecipePublisher.publish(
                        MachineRecipeMaps.Shredder,
                        generic(row(input, new ItemStack(Items.DRAGON_BREATH), "test:other-fallback")));
            } catch (IllegalStateException expected) {
                conflict = expected.getMessage().contains("Conflicting recovery rows");
            }
            helper.assertTrue(conflict, "different generic outputs fail with a conflict");
            helper.assertTrue(
                    RecoveryRecipePublisher.publishIsolated(
                                    MachineRecipeMaps.Shredder,
                                    generic(row(input, new ItemStack(Items.DRAGON_BREATH), "test:isolated-conflict")))
                            == null,
                    "an external row conflict is isolated instead of aborting the generation");
            helper.assertTrue(
                    MachineRecipeMaps.Shredder.mRecipeList.stream()
                            .anyMatch(row -> "test:fallback".equals(row.mSource)),
                    "the existing row survives an isolated candidate conflict");

            helper.assertTrue(
                    RecoveryRecipePublisher.publish(MachineRecipeMaps.Shredder, specific) != null,
                    "specific owner replaces a generic row");
            helper.assertTrue(
                    MachineRecipeMaps.Shredder.mRecipeList.stream()
                            .anyMatch(row -> "test:owner".equals(row.mSource)),
                    "specific row is published");
            helper.assertFalse(
                    MachineRecipeMaps.Shredder.mRecipeList.stream()
                            .anyMatch(row -> "test:fallback".equals(row.mSource)),
                    "generic row is withdrawn");
        } finally {
            MachineRecipeMaps.Shredder.removeAll(List.of(generic, specific));
        }
        helper.assertTrue(
                RecoveryRecipePublisher.inspect(
                                MachineRecipeMaps.Shredder,
                                generic(row(input, new ItemStack(Items.ARROW), "test:fallback")))
                        .action()
                        == RecoveryRecipePublisher.Action.PUBLISH,
                "source text does not participate in recovery ownership");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void cinnabarUsesOrdinaryThermalOwnership(GameTestHelper helper) {
        var cinnabar = GTMaterialRegistry.get("Cinnabar");
        helper.assertFalse(
                ThermalRecoveryOwnership.reviewedSpecialOwner(MaterialPrefix.dust, cinnabar),
                "Cinnabar dust is not a reviewed special thermal owner");
        helper.assertFalse(
                ThermalRecoveryOwnership.reviewedSpecialOwner(MaterialPrefix.crushedPurified, cinnabar),
                "processed Cinnabar forms are not a reviewed special thermal owner");
        helper.assertTrue(
                ThermalRecoveryOwnership.reviewedSpecialOwner(
                        MaterialPrefix.dustSmall, GTMaterialRegistry.get("Titanium")),
                "the bounded Ti dust-fraction owner remains explicit");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void matchingInputsPrefersTheSameRecoveryFamily(GameTestHelper helper) {
        var input = new ItemStack(Items.DRAGON_EGG);
        Recipe first = RecoveryRecipePublisher.generic(
                row(input, new ItemStack(Items.ARROW), "test:first-family"), "first-family");
        Recipe second = RecoveryRecipePublisher.generic(
                row(input, new ItemStack(Items.ARROW), "test:second-family"), "second-family");
        helper.assertTrue(
                RecoveryRecipePublisher.matchingInputs(second, List.of(first, second)) == second,
                "family-aware owner lookup does not select a different family first");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void mixedTagsIgnoreNonCompositionForms(GameTestHelper helper) {
        var ore = new OriginalFormConversions.Form("ore", "Tin");
        var raw = new OriginalFormConversions.Form("oreRaw", "Tin");
        helper.assertTrue(
                ore.equals(CraftingMaterialForms.compositionTagForm(List.of(raw, ore))),
                "an allowed ore tag survives an unrelated recognized tag");
        helper.succeed();
    }

    private static Recipe row(ItemStack input, ItemStack output, String source) {
        return new Recipe(
                        new ItemStack[] {input.copy()},
                        new ItemStack[] {output.copy()},
                        null,
                        null,
                        null,
                        null,
                        16,
                        16,
                        0)
                .withMaterialRecovery()
                .withItemInputGuard(0, stack -> true)
                .withSource(source);
    }

    private static Recipe copy(Recipe source) {
        return generic(row(new ItemStack(Items.DRAGON_EGG), new ItemStack(Items.ARROW), "test:copy"));
    }

    private static Recipe generic(Recipe recipe) {
        return RecoveryRecipePublisher.generic(recipe, "test-generic");
    }

    private static Recipe explicit(Recipe recipe) {
        return RecoveryRecipePublisher.explicit(recipe, "test-explicit");
    }
}
