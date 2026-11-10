package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
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
public final class RecoveryRecipeConflictsTests {
    private RecoveryRecipeConflictsTests() {}

    @GameTest(template = "test_empty", batch = "material_baseline", timeoutTicks = 200)
    public static void genericRecoveryConflictsAreExplicitAndSpecificWins(GameTestHelper helper) {
        var input = new ItemStack(Items.DRAGON_EGG);
        Recipe generic = row(input, new ItemStack(Items.ARROW), "recovery:test-generic");
        Recipe specific = row(input, new ItemStack(Items.SPECTRAL_ARROW), "recovery:test-specific");
        try {
            helper.assertTrue(
                    RecoveryRecipeConflicts.publish(MachineRecipeMaps.Shredder, generic) != null,
                    "first generic row publishes");
            helper.assertTrue(
                    RecoveryRecipeConflicts.publish(MachineRecipeMaps.Shredder, copy(generic)) == null,
                    "equivalent generic row deduplicates");

            boolean conflict = false;
            try {
                RecoveryRecipeConflicts.publish(
                        MachineRecipeMaps.Shredder,
                        row(input, new ItemStack(Items.DRAGON_BREATH), "recovery:other-generic"));
            } catch (IllegalStateException expected) {
                conflict = expected.getMessage().contains("Conflicting recovery rows");
            }
            helper.assertTrue(conflict, "different generic outputs fail with a conflict");

            helper.assertTrue(
                    RecoveryRecipeConflicts.publish(MachineRecipeMaps.Shredder, specific) != null,
                    "specific owner replaces a generic row");
            helper.assertTrue(
                    MachineRecipeMaps.Shredder.mRecipeList.stream()
                            .anyMatch(row -> "recovery:test-specific".equals(row.mSource)),
                    "specific row is published");
            helper.assertFalse(
                    MachineRecipeMaps.Shredder.mRecipeList.stream()
                            .anyMatch(row -> "recovery:test-generic".equals(row.mSource)),
                    "generic row is withdrawn");
        } finally {
            MachineRecipeMaps.Shredder.removeAll(List.of(generic, specific));
        }
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
                .withSource(source);
    }

    private static Recipe copy(Recipe source) {
        return row(new ItemStack(Items.DRAGON_EGG), new ItemStack(Items.ARROW), source.mSource);
    }
}
