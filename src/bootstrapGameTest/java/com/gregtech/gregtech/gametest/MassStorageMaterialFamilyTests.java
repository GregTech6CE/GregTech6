package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.inventory.MassStorageMaterialForms;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTStorage;
import com.gregtech.gregtech.registry.GTWires;
import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** GT6 Mass Storage converts only its declared form family, conserving fractional material. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class MassStorageMaterialFamilyTests {
    private MassStorageMaterialFamilyTests() {}

    private static ItemStack copperWire(int size, boolean insulated) {
        var blocks = insulated ? GTWires.allCables() : GTWires.allWires();
        for (var registered : blocks) {
            var block = registered.get();
            if (block.spec().size() == size
                    && block.spec().material().resolve() == Materials.Copper.resolve()) {
                return new ItemStack(block.asItem());
            }
        }
        return ItemStack.EMPTY;
    }

    private static MassStorageBlockEntity storage(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 1, 1);
        helper.setBlock(local, GTStorage.MASS_STORAGE.get());
        return (MassStorageBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(local));
    }

    @GameTest(template = "test_empty")
    public static void ironIngotsAndNuggetsConserveUnitsAcrossSimulateAndExtract(GameTestHelper helper) {
        var storage = storage(helper);
        var handler = storage.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                .resolve().orElseThrow();
        var iron = new ItemStack(Items.IRON_INGOT);
        var nugget = new ItemStack(Items.IRON_NUGGET);
        var plate = GTItems.getStack(MaterialPrefix.plate, Materials.Iron);
        helper.assertTrue(!plate.isEmpty()
                        && MassStorageMaterialForms.compatibleUnits(iron, nugget) == GTValues.U9
                        && MassStorageMaterialForms.compatibleUnits(iron, plate) == 0,
                "vanilla iron forms bind to GT6 ingot family, but iron plates do not");

        helper.assertTrue(handler.insertItem(0, iron.copyWithCount(2), true).isEmpty()
                        && storage.stored() == 0 && storage.template().isEmpty(),
                "simulated first insert must not choose a template");
        helper.assertTrue(handler.insertItem(0, iron.copyWithCount(2), false).isEmpty()
                        && storage.stored() == 2 && storage.template().is(Items.IRON_INGOT),
                "actual insertion chooses the vanilla iron-ingot template");

        helper.assertTrue(handler.insertItem(0, nugget.copyWithCount(8), true).isEmpty()
                        && storage.stored() == 2 && storage.partialUnits() == 0,
                "simulating eight nuggets accepts the batch without adding fractional material");
        helper.assertTrue(handler.insertItem(0, nugget.copyWithCount(8), false).isEmpty()
                        && storage.stored() == 2 && storage.partialUnits() == GTValues.U9 * 8,
                "eight nuggets remain as eight ninths of one ingot");
        helper.assertTrue(handler.insertItem(0, nugget, true).isEmpty()
                        && storage.stored() == 2 && storage.partialUnits() == GTValues.U9 * 8,
                "simulation of the ninth nugget leaves the stored material unchanged");
        helper.assertTrue(handler.insertItem(0, nugget, false).isEmpty()
                        && storage.stored() == 3 && storage.partialUnits() == 0,
                "nine nuggets become exactly one additional iron ingot");
        helper.assertTrue(!handler.isItemValid(0, plate)
                        && ItemStack.isSameItemSameTags(handler.insertItem(0, plate, false), plate)
                        && storage.stored() == 3,
                "a same-material plate cannot enter the ingot family");

        helper.assertTrue(handler.extractItem(0, 2, true).getCount() == 2 && storage.stored() == 3,
                "simulated extraction leaves all ingots in place");
        ItemStack taken = handler.extractItem(0, 2, false);
        helper.assertTrue(taken.is(Items.IRON_INGOT) && taken.getCount() == 2
                        && storage.stored() == 1 && storage.partialUnits() == 0,
                "real extraction returns exactly the converted material once");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void namedEquivalentFormIsReservedButNotConsumed(GameTestHelper helper) {
        var storage = storage(helper);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack namedNugget = new ItemStack(Items.IRON_NUGGET);
        namedNugget.getOrCreateTag().putString("audit.custom_name", "keep this item");
        helper.assertTrue(storage.insert(iron) == 1
                        && MassStorageMaterialForms.compatibleUnits(iron, namedNugget) == 0
                        && MassStorageMaterialForms.reserves(iron, namedNugget)
                        && storage.insert(namedNugget) == 0
                        && storage.stored() == 1 && storage.partialUnits() == 0,
                "Dump reserves an NBT-bearing iron nugget, but conversion cannot erase its NBT");
        ItemStack namedIron = iron.copy();
        namedIron.getOrCreateTag().putString("audit.custom_name", "stored template");
        helper.assertTrue(MassStorageMaterialForms.reserves(namedIron, new ItemStack(Items.IRON_NUGGET))
                        && MassStorageMaterialForms.compatibleUnits(namedIron, new ItemStack(Items.IRON_NUGGET)) == 0,
                "a tagged template also reserves equivalent forms without permitting conversion");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void partialMaterialSurvivesReloadAndResetWhenEmpty(GameTestHelper helper) {
        var storage = storage(helper);
        storage.toggleFilterReset();
        helper.assertTrue(storage.resetsFilter() && storage.insert(new ItemStack(Items.IRON_INGOT)) == 1
                        && storage.insert(new ItemStack(Items.IRON_NUGGET, 8)) == 8,
                "reset-enabled iron storage accepts fractional units");
        ItemStack extracted = storage.extractAmount(1);
        helper.assertTrue(extracted.is(Items.IRON_INGOT) && storage.stored() == 0
                        && storage.partialUnits() == GTValues.U9 * 8
                        && storage.template().is(Items.IRON_INGOT),
                "a zero-count template stays while fractional material remains");

        storage.load(storage.saveWithoutMetadata());
        helper.assertTrue(storage.stored() == 0 && storage.partialUnits() == GTValues.U9 * 8
                        && storage.template().is(Items.IRON_INGOT),
                "save and reload preserve the fractional iron without choosing another material");
        helper.assertTrue(storage.insert(new ItemStack(Items.IRON_NUGGET)) == 1
                        && storage.stored() == 1 && storage.partialUnits() == 0,
                "the ninth nugget completes the original ingot after reload");
        helper.assertTrue(storage.extractAmount(1).is(Items.IRON_INGOT)
                        && storage.stored() == 0 && storage.template().isEmpty(),
                "reset mode clears the filter only after all units are removed");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void bareWireSizesShareOneFamilyButInsulatedCableDoesNot(GameTestHelper helper) {
        ItemStack wireOne = copperWire(1, false);
        ItemStack wireTwo = copperWire(2, false);
        ItemStack cableOne = copperWire(1, true);
        helper.assertTrue(!wireOne.isEmpty() && !wireTwo.isEmpty() && !cableOne.isEmpty()
                        && MassStorageMaterialForms.templateUnits(wireOne) == GTValues.U2
                        && MassStorageMaterialForms.compatibleUnits(wireOne, wireTwo) == GTValues.U
                        && MassStorageMaterialForms.compatibleUnits(wireOne, cableOne) == 0,
                "GT6 wireGt01/02 are one material family; insulated cables are excluded");

        var storage = storage(helper);
        helper.assertTrue(storage.insert(wireOne) == 1 && storage.insert(wireTwo) == 1
                        && storage.stored() == 3 && storage.partialUnits() == 0
                        && storage.insert(cableOne) == 0,
                "a two-size bare wire converts to two one-size wires without admitting cable");
        ItemStack extracted = storage.extractAmount(3);
        helper.assertTrue(ItemStack.isSameItemSameTags(extracted, wireOne)
                        && extracted.getCount() == 3 && storage.stored() == 0,
                "extracting the converted wires preserves all three half-unit pieces");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void crushedOreStagesRemainSeparateFamilies(GameTestHelper helper) {
        var bauxite = GTMaterialRegistry.get("Bauxite");
        helper.assertTrue(bauxite != null && bauxite.isValid(), "bauxite material exists");
        ItemStack crushed = GTItems.getStack(MaterialPrefix.crushed, bauxite);
        ItemStack crushedTiny = GTItems.getStack(MaterialPrefix.crushedTiny, bauxite);
        ItemStack purified = GTItems.getStack(MaterialPrefix.crushedPurified, bauxite);
        ItemStack purifiedTiny = GTItems.getStack(MaterialPrefix.crushedPurifiedTiny, bauxite);
        ItemStack centrifuged = GTItems.getStack(MaterialPrefix.crushedCentrifuged, bauxite);
        ItemStack centrifugedTiny = GTItems.getStack(MaterialPrefix.crushedCentrifugedTiny, bauxite);
        helper.assertTrue(!crushed.isEmpty() && !crushedTiny.isEmpty()
                        && !purified.isEmpty() && !purifiedTiny.isEmpty()
                        && !centrifuged.isEmpty() && !centrifugedTiny.isEmpty(),
                "all six GT6 crushed-bauxite prefixes are registered");
        helper.assertTrue(MassStorageMaterialForms.compatibleUnits(crushed, crushedTiny)
                            == MaterialPrefix.crushedTiny.getMaterialWeight()
                        && MassStorageMaterialForms.compatibleUnits(crushed, purifiedTiny) == 0
                        && MassStorageMaterialForms.compatibleUnits(purified, purifiedTiny)
                            == MaterialPrefix.crushedPurifiedTiny.getMaterialWeight()
                        && MassStorageMaterialForms.compatibleUnits(purified, centrifugedTiny) == 0
                        && MassStorageMaterialForms.compatibleUnits(centrifuged, centrifugedTiny)
                            == MaterialPrefix.crushedCentrifugedTiny.getMaterialWeight(),
                "normal, purified, and centrifuged crushed ore convert only within their own stage");
        helper.succeed();
    }
}
