package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.api.addon.material.*;
import gregtech.api.material.MaterialProperty;
import gregtech.engine.addon.AddonMaterials;
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

    public static final class ExistingFormAddon implements AddonMaterialContributor {
        private static final String WOOD = "BathProbeWood";
        static final MaterialProcessKey<SpecialProcessValue> PROCESS = MaterialProcessKey.multiple(
                new DataKey("gt6ce_form_test", "alias_probe"), SpecialProcessValue.class);
        @Override public String addonId() { return "gt6ce_form_test"; }
        @Override public List<MaterialDeclaration> materials() {
            return List.of(MaterialDeclaration.builder(WOOD).build());
        }
        @Override public void link() {
            GTMaterialRegistry.get(WOOD).put(MaterialProperty.WOOD);
        }
        @Override public void materialInfo(MaterialInfoTool.Registrar registrar) {
            registrar.registerForm("Cinnabar", "dust");
            registrar.registerSpecialProcess(new SpecialProcessMethod(PROCESS, "gt.recipe.bath", "probe.alias"));
            var input = new FormKey("crushedPurified");
            var output = new SpecialProcessValue.ItemOutput(new MaterialKey("Fe"), List.of(
                    new SpecialProcessValue.FormCount(new FormKey("casingSmall"), 1),
                    new SpecialProcessValue.FormCount(new FormKey("dust"), 2)), 10000);
            registrar.registerProcess(new MaterialKey("Gold"), PROCESS,
                    new MaterialProcessRule<>(java.util.Set.of(input), new SpecialProcessValue.Target(
                            java.util.Map.of(input, 1), List.of(output), List.of(), 1, 20, 0)));
            registrar.registerProcess(new MaterialKey("Lava"), PROCESS,
                    new MaterialProcessRule<>(java.util.Set.of(new FormKey("liquid")), new SpecialProcessValue.Reagent()));
        }
    }

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void addonWoodCanBeTreatedAndPolished(GameTestHelper helper) {
        var input = GTItems.getStack(MaterialPrefix.stick, GTMaterialRegistry.get(ExistingFormAddon.WOOD), 1);
        helper.assertTrue(!input.isEmpty(), "addon wood stick is registered");
        for (String name : List.of("WoodTreated", "WoodPolished")) {
            var output = GTItems.getStack(MaterialPrefix.stick, GTMaterialRegistry.get(name), 1);
            helper.assertTrue(!output.isEmpty() && MachineRecipeMaps.Bath.recipeRows.stream().anyMatch(
                    row -> row.itemInputs.length == 1 && row.matchesItemInput(0, input)
                            && row.itemOutputs.length == 1 && ItemStack.matches(row.itemOutputs[0], output)),
                    "addon wood stick has a Bath recipe for " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void repeatedCinnabarDustRequestPreservesShredderOutputs(GameTestHelper helper) {
        var cinnabar = GTMaterialRegistry.get("Cinnabar");
        helper.assertTrue(AddonMaterials.accepts("Cinnabar", "dust"), "test addon request was accepted");
        var input = GTItems.getStack(MaterialPrefix.crushed, cinnabar, 1);
        var dust = GTItems.getStack(MaterialPrefix.dust, cinnabar, 1);
        var tiny = GTItems.getStack(MaterialPrefix.dustTiny, cinnabar, 1);
        var fraction = GTItems.getStack(MaterialPrefix.dustDiv72, cinnabar, 1);
        helper.assertTrue(!input.isEmpty() && !dust.isEmpty() && !tiny.isEmpty() && !fraction.isEmpty(),
                "the source forms are registered");
        helper.assertTrue(SourceCinnabarFollowonAdapter.supports(cinnabar), "duplicate request keeps the source profile");
        var rows = MachineRecipeMaps.Shredder.recipeRows.stream()
                .filter(row -> row.itemInputs.length == 1 && ItemStack.matches(row.itemInputs[0], input))
                .toList();
        helper.assertTrue(rows.stream().anyMatch(row -> row.itemOutputs.length >= 3
                && row.itemOutputs[0].getItem() == dust.getItem()
                && row.itemOutputs[1].getItem() == tiny.getItem()
                && row.itemOutputs[2].getItem() == fraction.getItem()),
                "crushed Cinnabar retains dust, tiny dust and 1/72 dust outputs");
        helper.succeed();
    }

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
                    MachineRecipeMaps.Shredder.recipeRows.stream()
                            .anyMatch(row -> "test:fallback".equals(row.source)),
                    "the existing row survives an isolated candidate conflict");

            helper.assertTrue(
                    RecoveryRecipePublisher.publish(MachineRecipeMaps.Shredder, specific) != null,
                    "specific owner replaces a generic row");
            helper.assertTrue(
                    MachineRecipeMaps.Shredder.recipeRows.stream()
                            .anyMatch(row -> "test:owner".equals(row.source)),
                    "specific row is published");
            helper.assertFalse(
                    MachineRecipeMaps.Shredder.recipeRows.stream()
                            .anyMatch(row -> "test:fallback".equals(row.source)),
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
