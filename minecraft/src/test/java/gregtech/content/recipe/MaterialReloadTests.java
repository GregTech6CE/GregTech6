package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialPublication;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MaterialReloadTests {
    private MaterialReloadTests() {}

    @GameTest(template = "test_empty", batch = "material_reload", timeoutTicks = 6000)
    public static void twoRealReloadsPreserveOwnedRecipes(GameTestHelper helper) {
        assertAddonProcessTags(helper);
        var server = helper.getLevel().getServer();
        var packs = List.copyOf(server.getPackRepository().getSelectedIds());
        int applications = MachineRecipeOverlay.applications();
        int owned = SharedSpecialProcessRows.owned().size();
        int bath = MachineRecipeMaps.Bath.recipeRows.size();
        var previous = MaterialPublication.current();
        var done = new AtomicBoolean();
        var failure = new AtomicReference<Throwable>();
        server.reloadResources(packs).thenCompose(ignored -> server.reloadResources(packs))
                .whenComplete((ignored, error) -> { failure.set(error); done.set(true); });
        await(helper, applications, owned, bath, previous, done, failure);
    }

    private static void await(GameTestHelper helper, int applications, int owned, int bath,
            MaterialPublication.Stamp previous, AtomicBoolean done, AtomicReference<Throwable> failure) {
        if (!done.get()) {
            helper.runAfterDelay(1, () -> await(helper, applications, owned, bath, previous, done, failure));
            return;
        }
        if (failure.get() != null) { helper.fail("resource reload failed: " + failure.get()); return; }
        assertAddonProcessTags(helper);
        helper.assertTrue(MachineRecipeOverlay.applications() >= applications + 2, "both real overlays applied");
        helper.assertTrue(SharedSpecialProcessRows.owned().size() == owned, "special-process rows do not accumulate");
        helper.assertTrue(MachineRecipeMaps.Bath.recipeRows.size() == bath, "Bath recipe count remains stable");
        var current = MaterialPublication.current();
        helper.assertTrue(current.revision() > previous.revision(), "successful reload advances publication");
        helper.assertTrue(current.recipeRevision() == MachineRecipeOverlay.generation(), "stamp names the committed overlay");
        helper.assertTrue(current.compositionRevision() == ItemMaterialRegistry.compositionRevision(),
                "stamp names the composition used by the overlay");
        var server = helper.getLevel().getServer();
        try {
            var failed = MachineRecipeOverlay.apply(null);
            helper.assertTrue(failed.failed(), "invalid overlay fails through the real recovery path");
            helper.assertTrue(MaterialPublication.current() == current, "failed overlay preserves the prior stamp");
            helper.assertTrue(MachineRecipeMaps.Bath.recipeRows.size() == MachineRecipeMaps.Bath.baselineSize(),
                    "failure restores the Bath baseline instead of a partial overlay");
        } finally {
            var restored = MachineRecipeOverlay.apply(server.getRecipeManager());
            if (restored.failed()) throw new IllegalStateException("could not restore overlay after failure probe");
            OvenRecipeBridge.apply(server.getRecipeManager(), server.registryAccess());
            MaterialPublication.commit(MachineRecipeOverlay.lastPublication());
        }
        helper.assertTrue(SharedSpecialProcessRows.owned().size() == owned, "successful overlay restores owned rows");
        helper.succeed();
    }

    private static void assertAddonProcessTags(GameTestHelper helper) {
        var target = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("gt6ce_form_test", "alias_probe/target"));
        var reagentId = ResourceLocation.fromNamespaceAndPath("gt6ce_form_test", "alias_probe/reagent");
        var reagent = TagKey.create(Registries.FLUID, reagentId);
        var gold = GTMaterialRegistry.get("Gold");
        helper.assertTrue(GTItems.getStack(MaterialPrefix.crushedPurified, gold).is(target),
                "the addon namespace publishes the declared purified ore target");
        helper.assertFalse(GTItems.getStack(MaterialPrefix.crushedPurifiedTiny, gold).is(target),
                "the addon target tag excludes forms absent from the rule");
        helper.assertTrue(Fluids.LAVA.defaultFluidState().is(reagent),
                "the liquid reagent tag resolves the registered vanilla lava");
        helper.assertFalse(Fluids.FLOWING_LAVA.defaultFluidState().is(reagent),
                "the reagent tag uses the published lava registry identity");
        helper.assertTrue(!BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, reagentId))
                        .iterator().hasNext(),
                "a fluid-only reagent does not produce an item tag");
    }
}
