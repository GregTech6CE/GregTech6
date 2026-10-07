package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.book.GTMaterialDictionary;
import com.gregtech.gregtech.content.recipe.GTMaterialDataRecipes;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * GT6's material data chain: the Molecular Scanner writes a material onto a USB 3 stick and the
 * Printer and Matter Replicator read it back ({@code RecipeMapScannerMolecular},
 * {@code RecipeMapPrinter:139-149}, {@code RecipeMapReplicator:81-84}; ported as
 * {@link GTMaterialDataRecipes}).
 *
 * <p>The port used to have a tier-1 Molecular Scanner whose accepted energy range was 16..64 QU/t
 * while the scanner recipe asks for 512 QU/t, so the machine could never run a single scan. These
 * tests pin the machine's original T3 registration and run the scanner end to end.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class MaterialDataChainTests {

    /** Lightest material the port can hand out as an item; GT6's scan work grows with the nucleons. */
    private static GTMaterial lightest() {
        GTMaterial best = null;
        long bestNucleons = Long.MAX_VALUE;
        for (GTMaterial raw : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = raw.resolve();
            if (!material.isValid() || material.getId() <= 0) continue;
            long nucleons = material.getProtons() + material.getNeutrons();
            if (nucleons <= 0 || nucleons >= bestNucleons) continue;
            if (sample(material).isEmpty()) continue;
            best = material;
            bestNucleons = nucleons;
        }
        if (best == null) throw new IllegalStateException("no scannable material with an item form");
        return best;
    }

    /** The first form GT6's scanner accepts for a material. */
    private static ItemStack sample(GTMaterial material) {
        for (MaterialPrefix form : List.of(MaterialPrefix.dust, MaterialPrefix.ingot, MaterialPrefix.gem)) {
            if (!form.isValidFor(material)) continue;
            ItemStack stack = GTItems.getStack(form, material, 1);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack stick() {
        var item = com.gregtech.gregtech.registry.GTTechnological.get(GTMaterialDataRecipes.USB_STICK);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The three maps compute their rows from the inputs instead of a static table. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void theThreeMapsReadInputData(GameTestHelper h) {
        h.assertTrue(GTMaterialDataRecipes.isRegistered(), "the material data providers are installed");
        h.assertTrue(MachineRecipeMaps.ScannerMolecular.hasDynamicRecipes(), "the scanner reads input NBT");
        h.assertTrue(MachineRecipeMaps.Printer.hasDynamicRecipes(), "the printer reads input NBT");
        h.assertTrue(MachineRecipeMaps.Replicator.hasDynamicRecipes(), "the replicator reads input NBT");
        long[] coverage = GTMaterialDataRecipes.coverage();
        h.assertTrue(coverage[0] > 800 && coverage[1] > 800,
                "scannable materials / printable dictionaries: " + coverage[0] + " / " + coverage[1]);
        h.assertTrue(coverage[2] > 100, "replicable materials: " + coverage[2]);
        h.succeed();
    }

    /** A scanned stick plus paper and black dye prints that material's dictionary. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void printerPrintsTheScannedDictionary(GameTestHelper h) {
        GTMaterial material = lightest();
        ItemStack scanned = GTMaterialDataRecipes.withMaterialData(stick(), material);
        h.assertTrue(GTMaterialDataRecipes.scannedMaterial(scanned) == material,
                "the stick carries " + material.getName());
        FluidStack dye = GTFluids.stack("Dye_Chemical_Black", 72);
        h.assertTrue(dye != null && !dye.isEmpty(), "the port registers GT6's chemical black dye");

        Recipe printed = MachineRecipeMaps.Printer.findRecipe(
                List.of(new ItemStack(Items.PAPER, 3), scanned), List.of(dye), false, 2, 1);
        h.assertTrue(printed != null, "a scanned stick plus three paper and dye prints something");
        ItemStack page = printed.getOutput(0);
        h.assertTrue(page.getItem() instanceof com.gregtech.gregtech.item.ColoredBookItem, "the printer prints a source dictionary cover: " + page);
        h.assertTrue(GTMaterialDictionary.materialOf(page) == material,
                "the printed book is the scanned material's dictionary");
        h.assertTrue(page.getTag().getList("pages", 8).size() == GTMaterialDictionary.pages(material).size(),
                "every dictionary page is printed");
        h.assertTrue(printed.mEUt == 512 && printed.mDuration == 16,
                "GT6's printer power and duration: " + printed.mEUt + " EU/t for " + printed.mDuration);
        h.assertTrue(printed.mFluidInputs.length == 1 && printed.mFluidInputs[0].getAmount() == 72,
                "GT6's half dye unit (72 mB) of black dye is required");
        h.assertTrue(printed.isCatalystInput(1), "the data stick is a retained printer input");
        ItemStack otherStick = scanned.copy();
        otherStick.getOrCreateTag().putString("gt.test.marker", "different stick");
        Recipe otherPrint = MachineRecipeMaps.Printer.findRecipe(
                List.of(new ItemStack(Items.PAPER, 3), otherStick), List.of(dye), false, 2, 1);
        h.assertTrue(otherPrint != null, "another stick with the same file but different NBT also prints");
        var remaining = RecipeInputs.consume(otherPrint,
                List.of(new ItemStack(Items.PAPER, 3), otherStick), List.of(dye), 1);
        h.assertTrue(remaining != null && remaining.items().get(0).isEmpty()
                        && ItemStack.isSameItemSameTags(remaining.items().get(1), otherStick)
                        && remaining.items().get(1).getCount() == 1,
                "printing consumes paper but keeps the exact data stick");

        h.assertTrue(MachineRecipeMaps.Printer.findRecipe(
                        List.of(new ItemStack(Items.PAPER, 3), scanned), List.of(), false, 2, 1) == null,
                "without dye the printer does not print");
        h.assertTrue(MachineRecipeMaps.Printer.findRecipe(
                        List.of(new ItemStack(Items.PAPER, 2), scanned), List.of(dye), false, 2, 1) == null,
                "two paper are not enough (GT6 prints with three)");
        h.assertTrue(MachineRecipeMaps.Printer.findRecipe(
                        List.of(new ItemStack(Items.PAPER, 3), stick()), List.of(dye), false, 2, 1) == null,
                "an empty stick prints nothing");
        h.succeed();
    }

    /** A powered Printer finishes its job with the USB stick intact and refuses a second job without it. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void printerKeepsTheStickAfterPrinting(GameTestHelper h) {
        var block = MachineRegistry.basicMachines().stream()
                .filter(entry -> entry.isPresent() && "printer_stainless_steel".equals(entry.get().basicSpec().id()))
                .findFirst().orElseThrow(() -> new IllegalStateException("no tier-3 printer registered"));
        var spec = block.get().basicSpec();
        h.assertTrue(spec.energyIn() == 512 && spec.recipeMap() == MachineRecipeMaps.Printer,
                "the tier-3 Printer can run a 512 EU/t dictionary job");

        BlockPos pos = new BlockPos(4, 3, 4);
        h.setBlock(pos, Blocks.AIR);
        h.setBlock(pos, block.get());
        var machine = (BasicMachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
        h.assertTrue(machine != null, "the Printer has a block entity");
        GTMaterial material = lightest();
        ItemStack scanned = GTMaterialDataRecipes.withMaterialData(stick(), material);
        scanned.getOrCreateTag().putString("gt.test.marker", "the second stick");
        machine.inventory().setStackInSlot(0, new ItemStack(Items.PAPER, 3));
        machine.inventory().setStackInSlot(1, scanned.copy());
        FluidStack dye = GTFluids.stack("Dye_Chemical_Black", 72);
        h.assertTrue(dye != null && !dye.isEmpty(), "black dye is registered");
        machine.getTanksInput()[0].setFluid(dye.copy());

        for (int i = 0; i < 40; i++) {
            machine.doEnergyInjection(GregTechTags.Energy.EU, null, spec.energyInMax(), 1, true);
            BasicMachineBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(),
                    machine.getBlockState(), machine);
        }
        h.assertTrue(machine.inventory().getStackInSlot(0).isEmpty(), "the Printer used the paper");
        h.assertTrue(machine.getTanksInput()[0].isEmpty(), "the Printer used the black dye");
        ItemStack retained = machine.inventory().getStackInSlot(1);
        h.assertTrue(retained.getCount() == 1 && ItemStack.isSameItemSameTags(retained, scanned),
                "the same data stick, including its extra NBT, remains after a real print");
        int outputSlot = spec.recipeMap().mInputItemsCount;
        var nearby = new net.minecraft.world.phys.AABB(machine.getBlockPos()).inflate(4);
        boolean printed = GTMaterialDictionary.materialOf(machine.inventory().getStackInSlot(outputSlot)) == material;
        for (var dropped : h.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class, nearby)) {
            if (GTMaterialDictionary.materialOf(dropped.getItem()) == material) printed = true;
        }
        h.assertTrue(printed, "the powered Printer produced the material dictionary");

        machine.inventory().setStackInSlot(1, ItemStack.EMPTY);
        machine.inventory().setStackInSlot(outputSlot, ItemStack.EMPTY);
        for (var dropped : h.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class, nearby)) {
            if (GTMaterialDictionary.materialOf(dropped.getItem()) == material) dropped.discard();
        }
        machine.inventory().setStackInSlot(0, new ItemStack(Items.PAPER, 3));
        machine.getTanksInput()[0].setFluid(dye.copy());
        for (int i = 0; i < 40; i++) {
            machine.doEnergyInjection(GregTechTags.Energy.EU, null, spec.energyInMax(), 1, true);
            BasicMachineBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(),
                    machine.getBlockState(), machine);
        }
        h.assertTrue(machine.inventory().getStackInSlot(0).getCount() == 3
                        && machine.getTanksInput()[0].getAmount() == 72,
                "without a USB stick the Printer cannot use paper or dye");
        h.assertTrue(machine.inventory().getStackInSlot(outputSlot).isEmpty()
                        && h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                                nearby).stream().noneMatch(drop ->
                                GTMaterialDictionary.materialOf(drop.getItem()) == material),
                "without a USB stick no second dictionary is produced");
        h.succeed();
    }

    /** The replicator rebuilds the material from its own neutrons and protons. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void replicatorRebuildsTheScannedMaterial(GameTestHelper h) {
        GTMaterial material = lightest();
        ItemStack scanned = GTMaterialDataRecipes.withMaterialData(stick(), material);
        FluidStack neutral = GTFluids.stack("MatterNeutral", (int) material.getNeutrons());
        FluidStack charged = GTFluids.stack("MatterCharged", (int) material.getProtons());
        h.assertTrue(neutral != null && charged != null, "the port registers both matter fluids");

        Recipe replicated = MachineRecipeMaps.Replicator.findRecipe(
                List.of(scanned), List.of(neutral, charged), false, 3, 3);
        h.assertTrue(replicated != null, "a scanned stick plus its matter replicates the material");
        h.assertTrue(!replicated.getOutput(0).isEmpty(),
                "the replicator hands out " + material.getName() + ": " + replicated.getOutput(0));
        h.assertTrue(replicated.mEUt == 1 && replicated.mDuration == (material.getProtons() + material.getNeutrons()) * 256L,
                "GT6's 256 ticks per nucleon, at one QU/t");
        h.assertTrue(replicated.isCatalystInput(0), "the replicator retains its data stick");
        var remaining = RecipeInputs.consume(replicated, List.of(scanned), List.of(neutral, charged), 1);
        h.assertTrue(remaining != null && remaining.items().get(0).getCount() == 1
                        && ItemStack.isSameItemSameTags(remaining.items().get(0), scanned),
                "the replicator's execution plan keeps the exact stick");
        h.assertTrue(RecipeInputs.consume(replicated, List.of(), List.of(neutral, charged), 1) == null,
                "the replicator cannot execute without its data stick");
        ItemStack otherStick = scanned.copy();
        otherStick.getOrCreateTag().putString("gt.test.marker", "different stick");
        Recipe otherReplication = MachineRecipeMaps.Replicator.findRecipe(
                List.of(otherStick), List.of(neutral, charged), false, 3, 3);
        h.assertTrue(otherReplication != null && otherReplication.mInputs.length == 1
                        && ItemStack.isSameItemSameTags(otherReplication.mInputs[0], otherStick),
                "the replicator accepts another stick with the same file but different NBT");
        boolean neutralSeen = false, chargedSeen = false;
        for (FluidStack fluid : replicated.mFluidInputs) {
            if (fluid.isFluidEqual(neutral)) neutralSeen = fluid.getAmount() == material.getNeutrons();
            if (fluid.isFluidEqual(charged)) chargedSeen = fluid.getAmount() == material.getProtons();
        }
        h.assertTrue(neutralSeen || material.getNeutrons() == 0, "one mB of neutral matter per neutron");
        h.assertTrue(chargedSeen || material.getProtons() == 0, "one mB of charged matter per proton");
        h.assertTrue(MachineRecipeMaps.Replicator.findRecipe(
                        List.of(stick()), List.of(neutral, charged), false, 3, 3) == null,
                "an empty stick replicates nothing");
        h.succeed();
    }

    /** The real T3 Molecular Scanner actually scans: original energy envelope, real output. */
    @GameTest(template = "test_empty", timeoutTicks = 2000)
    public static void theMolecularScannerScansEndToEnd(GameTestHelper h) {
        var block = MachineRegistry.basicMachines().stream()
                .filter(entry -> entry.isPresent() && "scannermolecular_osmium".equals(entry.get().basicSpec().id()))
                .findFirst().orElseThrow(() -> new IllegalStateException("no molecular scanner registered"));
        var spec = block.get().basicSpec();
        h.assertTrue(spec.tier() == 3, "GT6 registers the scanner as T3, not T1: tier " + spec.tier());
        h.assertTrue(spec.energyIn() == 512 && spec.energyInMin() == 256 && spec.energyInMax() == 1024,
                "GT6's scanner energy: " + spec.energyIn() + " QU/t in "
                        + spec.energyInMin() + ".." + spec.energyInMax());

        GTMaterial material = lightest();
        ItemStack scanned = sample(material);
        long nucleons = material.getProtons() + material.getNeutrons();
        h.assertTrue(nucleons <= 24, "the lightest scannable material is light: " + material.getName()
                + " with " + nucleons + " nucleons");

        BlockPos pos = new BlockPos(4, 3, 4);
        h.setBlock(pos, Blocks.AIR);
        h.setBlock(pos, block.get());
        var machine = (BasicMachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
        h.assertTrue(machine != null, "the molecular scanner has a block entity");
        machine.inventory().setStackInSlot(0, scanned.copy());
        machine.inventory().setStackInSlot(1, stick());

        long energyMax = spec.energyInMax();
        // Each tick adds min(energyInMax, mEnergy) to the work and drains energyInMax, so GT6's
        // (protons + neutrons) * 512 ticks of work finish in nucleons * 512 * 512 / energyInMax ticks.
        int ticks = (int) Math.min(20000, nucleons * 512L * 512L / energyMax + 60);
        for (int i = 0; i < ticks; i++) {
            machine.doEnergyInjection(GregTechTags.Energy.QU, null, energyMax, 1, true);
            BasicMachineBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(),
                    machine.getBlockState(), machine);
        }

        ItemStack output = machine.inventory().getStackInSlot(spec.recipeMap().mInputItemsCount);
        // GT6's scanner auto-outputs to its right face, and the port drops into the air when that
        // neighbour has no inventory, so the finished stick is either in the output slot or on the
        // ground next to the machine.
        if (output.isEmpty()) {
            for (var dropped : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(6))) {
                if (GTMaterialDataRecipes.scannedMaterial(dropped.getItem()) != null) {
                    output = dropped.getItem();
                    break;
                }
            }
        }
        h.assertTrue(!output.isEmpty(), "the scanner produced " + material.getName()
                + "'s data stick (" + ticks + " ticks): " + output);
        h.assertTrue(GTMaterialDataRecipes.isScannerStick(output), "the scanner output is a USB 3 stick");
        h.assertTrue(GTMaterialDataRecipes.scannedMaterial(output) == material,
                "the stick carries the scanned material: " + GTMaterialDataRecipes.scannedMaterial(output));
        h.succeed();
    }

    // ── §114: GT6's USB drives, the sixteen-file medium ─────────────────────────────

    /** One of GT6's drives: {@code usb1_hdd} … {@code usb4_hdd}. */
    private static ItemStack drive(int tier) {
        var item = com.gregtech.gregtech.registry.GTTechnological.get("usb" + tier + "_hdd");
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Three materials that differ from each other, for the slot round-trip. */
    private static List<GTMaterial> threeMaterials() {
        List<GTMaterial> result = new java.util.ArrayList<>();
        for (GTMaterial raw : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = raw.resolve();
            if (!material.isValid() || material.getId() <= 0) continue;
            if (result.contains(material)) continue;
            result.add(material);
            if (result.size() == 3) break;
        }
        return result;
    }

    /**
     * GT6 {@code MultiTileEntityHDDSwitch:61-83}: a drive holds sixteen files, keyed
     * {@code gt.usb.data<i>} / {@code gt.usb.tier<i>}, and a reader gets each of them back.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void aDriveStoresSixteenFilesAndReadsThemBack(GameTestHelper h) {
        ItemStack drive = drive(3);
        h.assertTrue(!drive.isEmpty(), "gregtech:usb3_hdd is registered");
        h.assertTrue(GTMaterialDataRecipes.isDrive(drive) && GTMaterialDataRecipes.driveTier(drive) == 3,
                "the item id carries the drive's tier");
        List<GTMaterial> materials = threeMaterials();
        h.assertTrue(materials.size() == 3, "the registry offers three materials to store");

        for (int i = 0; i < materials.size(); i++) {
            int slot = GTMaterialDataRecipes.writeMaterialData(drive, materials.get(i));
            h.assertTrue(slot == i, "file " + i + " landed in slot " + slot + ", expected " + i);
        }
        for (int i = 0; i < materials.size(); i++) {
            h.assertTrue(GTMaterialDataRecipes.scannedMaterialIn(drive, i) == materials.get(i),
                    "slot " + i + " reads back as " + GTMaterialDataRecipes.scannedMaterialIn(drive, i));
        }
        h.assertTrue(GTMaterialDataRecipes.usedSlots(drive) == 3,
                "three of sixteen slots are used, got " + GTMaterialDataRecipes.usedSlots(drive));
        h.assertTrue(GTMaterialDataRecipes.firstUsedSlot(drive) == 0, "the first file is in slot 0");
        h.assertTrue(GTMaterialDataRecipes.scannedMaterial(drive) == materials.get(0),
                "a reader without a slot index takes the first file, got "
                        + GTMaterialDataRecipes.scannedMaterial(drive));
        h.succeed();
    }

    /** The seventeenth file does not fit, and refusing it must not disturb the first sixteen. */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void aFullDriveRefusesTheSeventeenthFile(GameTestHelper h) {
        ItemStack drive = drive(4);
        GTMaterial material = threeMaterials().get(0);
        for (int i = 0; i < GTMaterialDataRecipes.DRIVE_SLOTS; i++) {
            h.assertTrue(GTMaterialDataRecipes.writeMaterialData(drive, material) == i,
                    "slot " + i + " was free");
        }
        h.assertTrue(GTMaterialDataRecipes.usedSlots(drive) == GTMaterialDataRecipes.DRIVE_SLOTS,
                "all sixteen slots are used, got " + GTMaterialDataRecipes.usedSlots(drive));
        h.assertTrue(GTMaterialDataRecipes.writeMaterialData(drive, material) == -1,
                "the seventeenth write reports a full drive");
        h.assertTrue(GTMaterialDataRecipes.scannedMaterialIn(drive, 0) == material
                        && GTMaterialDataRecipes.scannedMaterialIn(drive, GTMaterialDataRecipes.DRIVE_SLOTS - 1)
                        == material,
                "and the first and last files are untouched");
        h.succeed();
    }

    /**
     * GT6 {@code MultiTileEntityHDDSwitch:61}: a slot answers only when its tier is not above the
     * drive's — a tier-1 drive cannot read a file a tier-3 scanner wrote.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void aDriveOnlyReadsFilesOfItsOwnTierOrBelow(GameTestHelper h) {
        GTMaterial material = threeMaterials().get(0);
        ItemStack high = drive(3);
        h.assertTrue(GTMaterialDataRecipes.writeMaterialData(high, material) == 0,
                "a tier-3 file is written into the tier-3 drive");
        net.minecraft.nbt.CompoundTag file = high.getTag()
                .getCompound(GTMaterialDataRecipes.NBT_USB_DRIVE).copy();

        for (int tier : new int[]{1, 3, 4}) {
            ItemStack drive = drive(tier);
            net.minecraft.nbt.CompoundTag tag = drive.getOrCreateTag();
            tag.put(GTMaterialDataRecipes.NBT_USB_DRIVE, file.copy());
            boolean readable = GTMaterialDataRecipes.scannedMaterialIn(drive, 0) == material;
            h.assertTrue(readable == (tier >= 3),
                    "a tier-" + tier + " drive reading a tier-3 file: expected " + (tier >= 3)
                            + ", got " + readable);
        }
        h.succeed();
    }

    /** The stick's one-file layout is unchanged, and a stick is not a drive. */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void theStickLayoutIsUnchangedByTheDrives(GameTestHelper h) {
        GTMaterial material = threeMaterials().get(0);
        ItemStack stick = GTMaterialDataRecipes.withMaterialData(stick(), material);
        h.assertTrue(!GTMaterialDataRecipes.isDrive(stick), "a stick is not a drive");
        h.assertTrue(GTMaterialDataRecipes.driveTier(stick) == 0, "and has no drive tier");
        h.assertTrue(GTMaterialDataRecipes.usbData(stick) != null, "it carries gt.usb.data");
        h.assertTrue(stick.getTag() != null
                        && !stick.getTag().contains(GTMaterialDataRecipes.NBT_USB_DRIVE),
                "and no gt.usb.drive compound");
        h.assertTrue(GTMaterialDataRecipes.scannedMaterial(stick) == material,
                "the stick still reads back its material");
        h.succeed();
    }

    /** GT6 scanners write sticks; a HDD receives the resulting file through its switch. */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void theScannerRequiresAStickAndHddSwitchCanReceiveTheFile(GameTestHelper h) {
        GTMaterial material = lightest();
        ItemStack drive = drive(3);
        h.assertFalse(GTMaterialDataRecipes.isScannerMedium(drive),
                "GT6 scanner refuses a HDD in its data slot");
        Recipe recipe = GTMaterialDataRecipes.scanner(
                List.of(sample(material).copy(), drive.copy()), List.of());
        h.assertTrue(recipe == null, "scanner has no drive recipe");

        // The stick path still produces the stick layout for the same material.
        Recipe stickRecipe = GTMaterialDataRecipes.scanner(
                List.of(sample(material).copy(), stick()), List.of());
        h.assertTrue(stickRecipe != null && GTMaterialDataRecipes.isScannerStick(stickRecipe.mOutputs[0])
                        && GTMaterialDataRecipes.usbData(stickRecipe.mOutputs[0]) != null,
                "the scanner writes the stick");
        ItemStack output = stickRecipe.mOutputs[0];
        h.setBlock(new BlockPos(2, 1, 2), com.gregtech.gregtech.registry.GTStorage.HDD_SWITCH.get());
        var port = (com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity) h.getLevel().getBlockEntity(
                h.absolutePos(new BlockPos(2, 1, 2)));
        port.items().setStackInSlot(0, drive);
        port.setMode(0);
        h.assertTrue(port.writeFromHeldStick(output, net.minecraft.core.Direction.UP),
                "HDD switch receives the scanned stick file");
        h.assertTrue(GTMaterialDataRecipes.scannedMaterialIn(port.items().getStackInSlot(0), 0) == material,
                "HDD slot zero now stores the scanned material");
        ItemStack higherTier = new ItemStack(com.gregtech.gregtech.registry.GTTechnological.get("usb4_stick"));
        higherTier.getOrCreateTag().putString("test.marker", "preserve-me");
        Recipe higherTierRecipe = GTMaterialDataRecipes.scanner(
                List.of(sample(material).copy(), higherTier), List.of());
        h.assertTrue(higherTierRecipe != null
                        && higherTierRecipe.mOutputs[0].is(higherTier.getItem())
                        && "preserve-me".equals(higherTierRecipe.mOutputs[0].getTag().getString("test.marker")),
                "higher-tier stick and its own NBT survive a dynamic scan");
        h.succeed();
    }

    /**
     * GT6 {@code Behavior_DataStorage16:37-58}: an untouched drive is "Perfectly Formatted", an emptied
     * one "Uncleanly Formatted", and a used one lists all sixteen slots.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void theDriveTooltipListsItsSixteenSlots(GameTestHelper h) {
        ItemStack fresh = drive(3);
        List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        com.gregtech.gregtech.item.behavior.ItemBehaviors.tooltip(fresh, lines);
        h.assertTrue(lines.size() == 1 && text(lines.get(0)).contains("Perfectly Formatted"),
                "an unwritten drive says it is perfectly formatted, got " + lines);

        ItemStack wiped = drive(3);
        wiped.getOrCreateTag().put(GTMaterialDataRecipes.NBT_USB_DRIVE, new net.minecraft.nbt.CompoundTag());
        lines.clear();
        com.gregtech.gregtech.item.behavior.ItemBehaviors.tooltip(wiped, lines);
        h.assertTrue(lines.size() == 1 && text(lines.get(0)).contains("Uncleanly Formatted"),
                "an emptied drive says it is uncleanly formatted, got " + lines);

        GTMaterial material = threeMaterials().get(0);
        ItemStack used = drive(3);
        GTMaterialDataRecipes.writeMaterialData(used, material);
        lines.clear();
        com.gregtech.gregtech.item.behavior.ItemBehaviors.tooltip(used, lines);
        h.assertTrue(lines.size() == GTMaterialDataRecipes.DRIVE_SLOTS,
                "a used drive lists all " + GTMaterialDataRecipes.DRIVE_SLOTS + " slots, got " + lines.size());
        long empty = lines.stream().filter(line -> text(line).contains("is Empty")).count();
        h.assertTrue(empty == GTMaterialDataRecipes.DRIVE_SLOTS - 1,
                "fifteen slots are reported empty, got " + empty);
        h.assertTrue(lines.stream().anyMatch(line -> text(line).contains(material.getLocalName())),
                "and the used slot names the material, got " + lines);

        // The stick's tooltip is the other one: the verbose form plus its tier line.
        ItemStack stick = GTMaterialDataRecipes.withMaterialData(stick(), material);
        lines.clear();
        com.gregtech.gregtech.item.behavior.ItemBehaviors.tooltip(stick, lines);
        h.assertTrue(lines.size() >= 2 && text(lines.get(lines.size() - 1)).contains("USB 3.0"),
                "the stick prints its tier line last, got " + lines);
        h.succeed();
    }

    private static String text(net.minecraft.network.chat.Component component) {
        return component.getString();
    }
}
