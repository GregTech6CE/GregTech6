package com.gregtech.gregtech.compat;

import com.gregtech.gregtech.api.inventory.MassStorageMaterialForms;
import com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.recipe.ExternalOreProcessing;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import java.util.ArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Loaded only in development. Production jars do not contain this source set. */
@GameTestHolder("gregtech_compat")
@PrefixGameTestTemplate(false)
public final class TaggedMaterialDataGameTests {
    private TaggedMaterialDataGameTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 8000)
    public static void commonFormsFollowWhetherImmersiveEngineeringIsLoaded(GameTestHelper helper) {
        boolean loaded = ModList.get().isLoaded("immersiveengineering");
        if (!loaded) {
            helper.assertTrue(ExternalOreProcessing.taggedCompositions() == 0,
                    "common form compositions without IE: " + ExternalOreProcessing.taggedCompositions());
            helper.succeed();
            return;
        }
        assertImmersiveEngineering(helper);
        int tagged = ExternalOreProcessing.taggedCompositions();
        int ambiguous = ExternalOreProcessing.ambiguousTaggedItems();
        var server = helper.getLevel().getServer();
        server.reloadResources(new ArrayList<>(server.getPackRepository().getSelectedIds())).join();
        helper.assertTrue(ExternalOreProcessing.taggedCompositions() == tagged,
                "reload changed common form compositions: " + ExternalOreProcessing.taggedCompositions() + " vs " + tagged);
        helper.assertTrue(ExternalOreProcessing.ambiguousTaggedItems() == ambiguous,
                "reload changed ambiguous common-form skips: " + ExternalOreProcessing.ambiguousTaggedItems() + " vs " + ambiguous);
        assertImmersiveEngineering(helper);
        helper.succeed();
    }

    private static void assertImmersiveEngineering(GameTestHelper helper) {
        var lead = ie("immersiveengineering:ingot_lead");
        var rod = ie("immersiveengineering:stick_iron");
        var plate = ie("immersiveengineering:plate_lead");
        var aluminum = ie("immersiveengineering:stick_aluminum");
        var storage = ie("immersiveengineering:storage_lead");
        assertForm(helper, lead, MaterialPrefix.ingot, Materials.Lead, GTValues.U, "IE lead ingot");
        assertForm(helper, rod, MaterialPrefix.stick, Materials.Iron, GTValues.U2, "IE iron rod");
        assertForm(helper, plate, MaterialPrefix.plate, Materials.Lead, MaterialPrefix.plate.getMaterialWeight(), "IE lead plate");
        assertForm(helper, aluminum, MaterialPrefix.stick, Materials.Aluminium, GTValues.U2, "IE aluminum rod");
        var storageData = ItemMaterialRegistry.base(storage.getItem());
        helper.assertTrue(storageData.isEmpty()
                        || !storageData.get().source().equals(ExternalOreProcessing.TAGGED_COMPOSITION_SOURCE),
                "lead storage block must not gain a common-form composition");
        var melted = CrucibleItemInput.parse(lead);
        helper.assertTrue(melted.size() == 1 && melted.get(0).material.resolve() == Materials.Lead && melted.get(0).amount == GTValues.U,
                "IE lead ingot melts as one unit of lead");
        var gtLead = GTItems.getStack(MaterialPrefix.ingot, Materials.Lead, 1);
        helper.assertTrue(MassStorageMaterialForms.compatibleUnits(gtLead, lead) > 0,
                "IE lead ingot is accepted by a GT lead ingot mass storage");
        helper.assertTrue(MaterialEquivalence.matches(lead, gtLead),
                "an IE lead ingot requirement accepts a GT lead ingot");
        helper.assertTrue(!lead.is(ItemTags.create(ResourceLocation.parse("gregtech:ingot/lead"))),
                "IE lead ingot must stay out of gregtech:ingot/lead");
    }

    private static void assertForm(GameTestHelper helper, ItemStack stack, MaterialPrefix prefix,
                                   com.gregtech.gregtech.api.material.GTMaterial material, long amount, String label) {
        var data = ItemMaterialRegistry.base(stack.getItem()).orElse(null);
        helper.assertTrue(data != null
                        && data.prefix() == prefix
                        && data.material().resolve() == material
                        && data.amount() == amount
                        && data.source().equals(ExternalOreProcessing.TAGGED_COMPOSITION_SOURCE),
                label + " composition " + data);
    }

    private static ItemStack ie(String id) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item == Items.AIR) throw new IllegalStateException("Missing " + id);
        return new ItemStack(item);
    }
}
