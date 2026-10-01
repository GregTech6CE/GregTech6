package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.content.book.GTMaterialDictionary;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's <b>material data</b> chain: the Molecular Scanner writes a material onto a USB stick, and the
 * Printer and the Matter Replicator read that stick back.
 *
 * <p>GT6 implements all three as {@code RecipeMap} subclasses that override {@code findRecipe()} and
 * build a row from the <em>NBT</em> of their inputs — something a static table cannot express:</p>
 *
 * <ul>
 *   <li>{@code RecipeMapScannerMolecular:46-67} — a scannable material form plus an {@code USB_Stick_3}
 *       becomes that same stick carrying {@code gt.usb.data = {gt.replicator.data: <material id>}} and
 *       {@code gt.usb.tier = 3}; the work is {@code (protons + neutrons) * 512} ticks at 512 QU/t.</li>
 *   <li>{@code RecipeMapPrinter:139-149} — the stick plus paper prints the material's dictionary:
 *       3 paper, or 6 for a book past 50 pages, at 512 / 1024 EU/t for 16 ticks, with black dye
 *       ({@code FL.mul(DYE_FLUIDS_CHEMICAL[Black], 1, 2 or 1, T)} = 72 / 144 mB in GT6's 144 mB unit).</li>
 *   <li>{@code RecipeMapReplicator:81-84} + {@code getReplicatorRecipe:88-114} — the stick plus
 *       {@code neutralmatter} (the material's neutrons) and {@code chargedmatter} (its protons)
 *       rebuilds the material: gem → gem plate → ingot → plate → nugget (9) → chunk (4) → dust → tiny
 *       dust (9) → small dust (4) → stick (2), and its fluid when it has no such form.</li>
 * </ul>
 *
 * <p>The port installs the three providers through {@link RecipeMap#dynamicRecipes}, so the
 * table-driven lookup is untouched and these rows are computed only when nothing else matched. An idle
 * machine re-runs the lookup every tick. Scanner, Printer and Replicator rows close over the current
 * medium, so each row is built from that stack's exact item and NBT.</p>
 *
 * <p>Port differences, all deliberate:</p>
 * <ul>
 *   <li>GT6 prints {@code Paper_Printed_Pages} and the player then binds them into a book with leather
 *       and dye ({@code MultiItemBooks:99-121}). The port represents GT6 books as vanilla written
 *       books (see {@link GTMaterialDictionary}), so the printer hands out the bound dictionary
 *       directly and the port has no printed-pages item family.</li>
 *   <li>GT6 gates replication on {@code TD.Processing.UUM}, a flag the port does not model. The port
 *       replicates every material that is not {@code ANTIMATTER}, has at least one nucleon and has a
 *       form or fluid to hand out.</li>
 *   <li>GT6's scanner asks for {@code TD.Prefix.SCANNABLE} and its printer/replicator also read a USB
     *       <em>cable</em> pointing at a USB port block. The port does not model per-prefix SCANNABLE;
     *       it scans every item with a registered material composition. Printer and replicator also
     *       read cables from adjacent USB/HDD switches via their machine context.</li>
 * </ul>
 */
public final class GTMaterialDataRecipes {

    /** GT6 {@code CS.NBT_USB_DATA}. */
    public static final String NBT_USB_DATA = "gt.usb.data";
    /** GT6 {@code CS.NBT_REPLICATOR_DATA} — a short holding the material id. */
    public static final String NBT_REPLICATOR_DATA = "gt.replicator.data";
    /** GT6 {@code CS.NBT_USB_TIER}. */
    public static final String NBT_USB_TIER = "gt.usb.tier";

    /** GT6 {@code OD_USB_STICKS[3]} — the tier the molecular scanner reads and writes. */
    public static final String USB_STICK = "usb3_stick";
    private static final int USB_TIER = 3;

    /** GT6's scanner power ({@code RecipeMapScannerMolecular:57}); the T3 machine accepts 256..1024. */
    private static final long SCANNER_EU = 512;
    /** GT6's printer power for a normal / a many-pages book ({@code RecipeMapPrinter:135}). */
    private static final long PRINTER_EU = 512;
    private static final long PRINTER_EU_MANY = 1024;
    /** GT6's printer duration. */
    private static final long PRINTER_TICKS = 16;
    /** GT6's black dye per print, in the port's 144 mB dye unit. */
    private static final int DYE_MB = 72;
    private static final int DYE_MB_MANY = 144;
    /** GT6's threshold for the "large" printed book. */
    private static final int MANY_PAGES = 50;
    /** GT6's replicator power per nucleon ({@code RecipeMapReplicator:91}). */
    private static final long REPLICATOR_EU_PER_NUCLEON = 256;

    private static boolean registered;

    private GTMaterialDataRecipes() {}

    /** Installs the three providers; idempotent. Returns 1 once they are in place. */
    public static synchronized int register() {
        if (registered) return 0;
        registered = true;
        MachineRecipeMaps.ScannerMolecular.dynamicRecipes(GTMaterialDataRecipes::scanner);
        MachineRecipeMaps.Printer.dynamicRecipes(GTMaterialDataRecipes::printer);
        MachineRecipeMaps.Replicator.dynamicRecipes(GTMaterialDataRecipes::replicator);
        return 1;
    }

    /** Whether the providers are installed. */
    public static boolean isRegistered() { return registered; }

    /**
     * How many rows each provider can produce, for the start-up report and the tests:
     * {@code [scannable materials, printable dictionaries, replicable materials]}.
     * Iterates every material, so it is computed on demand rather than at start-up.
     */
    public static long[] coverage() {
        long scannable = 0, printable = 0, replicable = 0;
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<GTMaterial, Boolean>());
        for (GTMaterial raw : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = raw.resolve();
            if (!material.isValid() || material.getId() <= 0 || !seen.add(material)) continue;
            scannable++;
            if (!GTMaterialDictionary.bookStack(material).isEmpty()) printable++;
            if (replicationRecipe(material) != null) replicable++;
        }
        return new long[]{scannable, printable, replicable};
    }

    // ── the USB stick ────────────────────────────────────────────────────

    /** GT6's {@code OD_USB_STICKS[3]} also accepts a higher-tier stick. */
    public static boolean isScannerStick(ItemStack stack) {
        return com.gregtech.gregtech.content.data.UsbDataMedia.stickTier(stack) >= USB_TIER;
    }

    /** The scanner accepts tier-3/4 sticks, never a HDD; the HDD switch transfers files to drives. */
    public static boolean isScannerMedium(ItemStack stack) {
        return isScannerStick(stack);
    }

    /** Writes material data onto whichever medium this is: a drive gets a slot, a stick gets the file. */
    public static ItemStack withData(ItemStack medium, GTMaterial material) {
        if (!isDrive(medium)) return withMaterialData(medium, material);
        ItemStack out = medium.copyWithCount(1);
        writeMaterialData(out, material);
        return out;
    }

    /** The data GT6 stores under {@code gt.usb.data}, or null when the stick carries none. */
    @Nullable
    public static CompoundTag usbData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_USB_DATA)) return null;
        CompoundTag data = tag.getCompound(NBT_USB_DATA);
        return data.isEmpty() ? null : data;
    }

    /**
     * The material a scanned stick or drive points at, or null.
     *
     * <p>A stick carries one file ({@code gt.usb.data}); a drive carries sixteen ({@code gt.usb.drive}).
     * Adjacent machines use the HDD switch's selected slot through {@code UsbDataPort}. This convenience
     * reader takes the first readable drive slot; use {@link #scannedMaterialIn} for an exact slot.</p>
     */
    @Nullable
    public static GTMaterial scannedMaterial(ItemStack stack) {
        GTMaterial fromStick = materialOf(usbData(stack));
        if (fromStick != null) return fromStick;
        if (isDrive(stack)) {
            int slot = firstUsedSlot(stack);
            if (slot >= 0) return scannedMaterialIn(stack, slot);
        }
        return null;
    }

    /** A copy of the stick carrying GT6's material data, written the way the scanner writes it. */
    public static ItemStack withMaterialData(ItemStack usb, GTMaterial material) {
        ItemStack out = usb.copyWithCount(1);
        CompoundTag tag = out.getOrCreateTag();
        CompoundTag data = new CompoundTag();
        data.putShort(NBT_REPLICATOR_DATA, (short) material.getId());
        tag.put(NBT_USB_DATA, data);
        tag.putByte(NBT_USB_TIER, (byte) USB_TIER);
        return out;
    }

    // ── GT6's USB drives: sixteen files in one item ──────────────────────

    /**
     * GT6 {@code CS.NBT_USB_DRIVE} ({@code CS.java:1278}) — the compound holding a drive's slots.
     * <p>
     * GT6 keys a drive's slot {@code i} as {@code gt.usb.data<i>} plus {@code gt.usb.tier<i>}
     * ({@code MultiTileEntityHDDSwitch:61-83}), i.e. the stick's two keys with the slot index
     * concatenated — the layout below keeps that exactly.
     * </p>
     */
    public static final String NBT_USB_DRIVE = "gt.usb.drive";

    /** GT6's drives hold sixteen files ({@code MultiTileEntityHDDSwitch} mode index 0-15). */
    public static final int DRIVE_SLOTS = 16;

    /** The tier a file carries when the port writes it — the same 3 the scanner's stick gets. */
    public static final int DRIVE_FILE_TIER = USB_TIER;

    private static final java.util.regex.Pattern DRIVE_ID =
            java.util.regex.Pattern.compile("usb([1-4])_hdd");

    /** GT6 {@code NBT_USB_DATA+i}: the key of one slot's data compound. */
    public static String slotDataKey(int slot) { return NBT_USB_DATA + slot; }

    /** GT6 {@code NBT_USB_TIER+i}: the key of one slot's tier byte. */
    public static String slotTierKey(int slot) { return NBT_USB_TIER + slot; }

    /**
     * The drive's tier, read from its item id ({@code usb<N>_hdd}), or 0 when the stack is not a drive.
     * <p>
     * GT6 carries the tier in the tile entity ({@code MultiTileEntityHDDSwitch.mUSBTier}); an item has
     * no tile entity, and the port registers exactly one item per drive tier, so the id <em>is</em> the
     * tier. Documented rather than hidden: a renamed item would silently stop being a drive.
     * </p>
     */
    public static int driveTier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return 0;
        var matcher = DRIVE_ID.matcher(id.getPath());
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    /** Whether the stack is one of GT6's USB drives ({@code usb1_hdd} … {@code usb4_hdd}). */
    public static boolean isDrive(ItemStack stack) { return driveTier(stack) > 0; }

    /** A drive's slot compound, or null when that slot holds no file. */
    @Nullable
    public static CompoundTag driveSlot(ItemStack stack, int slot) {
        if (slot < 0 || slot >= DRIVE_SLOTS) return null;
        CompoundTag tag = stack == null ? null : stack.getTag();
        if (tag == null || !tag.contains(NBT_USB_DRIVE)) return null;
        CompoundTag drive = tag.getCompound(NBT_USB_DRIVE);
        if (!drive.contains(slotDataKey(slot))) return null;
        CompoundTag data = drive.getCompound(slotDataKey(slot));
        return data.isEmpty() ? null : data;
    }

    /**
     * The material a drive's slot points at, or null.
     *
     * <p>GT6 {@code MultiTileEntityHDDSwitch:61-62} answers a slot only when
     * {@code gt.usb.tier<i> <= driveTier}: a drive reads its own tier and below, and a file written by a
     * higher-tier drive stays invisible to a lower one. The port keeps that gate — a tier-1 drive cannot
     * read a tier-3 file, which is the whole point of the four drive tiers.</p>
     */
    @Nullable
    public static GTMaterial scannedMaterialIn(ItemStack drive, int slot) {
        int tier = driveTier(drive);
        if (tier <= 0) return null;
        CompoundTag data = driveSlot(drive, slot);
        if (data == null) return null;
        CompoundTag tag = drive.getTag();
        int fileTier = tag != null && tag.getCompound(NBT_USB_DRIVE).contains(slotTierKey(slot))
                ? tag.getCompound(NBT_USB_DRIVE).getByte(slotTierKey(slot)) : 0;
        if (fileTier > tier) return null;                          // GT6 :61
        return materialOf(data);
    }

    /** The first slot of a drive that carries material data readable by this drive, or −1. */
    public static int firstUsedSlot(ItemStack drive) {
        for (int slot = 0; slot < DRIVE_SLOTS; slot++) {
            if (scannedMaterialIn(drive, slot) != null) return slot;
        }
        return -1;
    }

    /** How many of a drive's sixteen slots hold a file (readable or not). */
    public static int usedSlots(ItemStack drive) {
        int used = 0;
        for (int slot = 0; slot < DRIVE_SLOTS; slot++) {
            if (driveSlot(drive, slot) != null) used++;
        }
        return used;
    }

    /**
     * Writes material data into the first free slot of a drive, the way GT6's HDD switch writes the
     * slot its mode index points at ({@code MultiTileEntityHDDSwitch:72-83}).
     *
     * @return the slot the file landed in, or −1 when every slot is taken
     */
    public static int writeMaterialData(ItemStack drive, GTMaterial material) {
        if (!isDrive(drive) || material == null || !material.isValid()) return -1;
        CompoundTag tag = drive.getOrCreateTag();
        CompoundTag driveTag = tag.getCompound(NBT_USB_DRIVE);
        for (int slot = 0; slot < DRIVE_SLOTS; slot++) {
            if (driveTag.contains(slotDataKey(slot)) && !driveTag.getCompound(slotDataKey(slot)).isEmpty()) {
                continue;
            }
            CompoundTag data = new CompoundTag();
            data.putShort(NBT_REPLICATOR_DATA, (short) material.getId());
            driveTag.put(slotDataKey(slot), data);
            driveTag.putByte(slotTierKey(slot), (byte) DRIVE_FILE_TIER);
            tag.put(NBT_USB_DRIVE, driveTag);
            return slot;
        }
        return -1;
    }

    /**
     * Whether the replicator can rebuild this material — GT6 {@code RecipeMapReplicator:88-114} refuses
     * antimatter, anything without nucleons and anything the port has no fluid for, and the tooltip
     * ({@code UT.java:2250,2266}) distinguishes the two cases.
     */
    public static boolean isReplicable(GTMaterial material) {
        return material != null && replicationRecipe(material) != null;
    }

    /**
     * The replicator's energy for one material: {@code (protons + neutrons) * REPLICATOR_EU_PER_NUCLEON}
     * — the same number {@link #replicationRecipe} charges, so a tooltip that prints it prints what the
     * machine actually spends.
     */
    public static long replicatorEnergy(GTMaterial material) {
        if (material == null || !material.isValid()) return 0;
        return (long) (material.getProtons() + material.getNeutrons()) * REPLICATOR_EU_PER_NUCLEON;
    }

    /** The material a data compound points at, or null when it carries none. */
    @Nullable
    private static GTMaterial materialOf(CompoundTag data) {
        if (data == null || !data.contains(NBT_REPLICATOR_DATA)) return null;
        GTMaterial material = GTMaterialRegistry.get(data.getShort(NBT_REPLICATOR_DATA));
        return material.isValid() ? material : null;
    }

    // ── the molecular scanner ────────────────────────────────────────────

    /**
     * GT6 {@code RecipeMapScannerMolecular:46-67}: a material form plus a USB 3 stick is replaced by
     * that stick carrying the material's id.
     */
    @Nullable
    public static Recipe scanner(List<ItemStack> items, List<FluidStack> fluids) {
        ItemStack usb = null;
        ItemStack scanned = null;
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;
            if (usb == null && isScannerMedium(stack)) { usb = stack; continue; }
            if (scanned == null) scanned = stack;
        }
        if (usb == null || scanned == null) return null;
        GTMaterial material = scannable(scanned);
        if (material == null) return null;
        long nucleons = material.getProtons() + material.getNeutrons();
        return new Recipe(
                new ItemStack[]{scanned.copyWithCount(1), usb.copyWithCount(1)},
                new ItemStack[]{withMaterialData(usb, material)},
                null, null, null, null,
                Math.max(1, nucleons * SCANNER_EU), SCANNER_EU, 0);
    }

    /** The material a scanned item is made of, or null when it carries none. */
    @Nullable
    private static GTMaterial scannable(ItemStack stack) {
        // GT6 asks for TD.Prefix.SCANNABLE; the port's material items carry (prefix, material) on the
        // Item instance, and the vanilla/block side carries a composition in ItemMaterialRegistry.
        GTMaterial material = com.gregtech.gregtech.item.MaterialItem.getMaterial(stack);
        if (material != null && material.isValid() && material.getId() > 0) return material.resolve();
        var data = ItemMaterialRegistry.get(stack);
        if (data.isEmpty()) return null;
        material = data.get().material().resolve();
        return material.isValid() && material.getId() > 0 ? material : null;
    }

    // ── the printer ──────────────────────────────────────────────────────

    /**
     * GT6 {@code RecipeMapPrinter:139-149}: a scanned USB stick plus paper prints the material's
     * dictionary — 3 paper, or 6 when the dictionary runs past 50 pages. The port prints the bound
     * written book directly (see the class comment).
     */
    @Nullable
    public static Recipe printer(List<ItemStack> items, List<FluidStack> fluids) {
        ItemStack usb = null;
        ItemStack paper = null;
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;
            if (usb == null && com.gregtech.gregtech.content.data.UsbDataMedia.stickTier(stack) >= 1) {
                usb = stack;
                continue;
            }
            if (paper == null && stack.is(Items.PAPER)) paper = stack;
        }
        if (usb == null || paper == null) return null;
        GTMaterial material = scannedMaterial(usb);
        return printerForMedium(paper, usb, material);
    }

    /** Printer row for a USB cable connected to a selected file on a neighboring port. */
    @Nullable
    public static Recipe printerFromPort(List<ItemStack> items, ItemStack cable, CompoundTag data) {
        ItemStack paper = null;
        for (ItemStack stack : items) if (stack != null && !stack.isEmpty() && stack.is(Items.PAPER)) {
            paper = stack;
            break;
        }
        return paper == null ? null : printerForMedium(paper, cable, materialOf(data));
    }

    @Nullable
    private static Recipe printerForMedium(ItemStack paper, ItemStack usb, GTMaterial material) {
        if (material == null) return null;
        ItemStack book = GTMaterialDictionary.bookStack(material);
        if (book.isEmpty()) return null;
        boolean many = GTMaterialDictionary.pages(material).size() > MANY_PAGES;
        int sheets = many ? 6 : 3;
        if (paper.getCount() < sheets) return null;
        FluidStack dye = GTFluids.stack("Dye_Chemical_Black", many ? DYE_MB_MANY : DYE_MB);
        if (dye == null || dye.isEmpty()) return null;
        // RecipeInputs matches a tagged catalyst by exact NBT; a material-only cache would pin
        // the first stick's tag and reject a second stick carrying the same material.
        return new Recipe(
                new ItemStack[]{paper.copyWithCount(sheets), usb.copyWithCount(1)},
                new ItemStack[]{book},
                null, null, new FluidStack[]{dye}, null,
                PRINTER_TICKS, many ? PRINTER_EU_MANY : PRINTER_EU, 0).withCatalystInputs(1);
    }

    // ── the matter replicator ────────────────────────────────────────────

    /**
     * GT6 {@code RecipeMapReplicator:81-84}: a scanned USB stick plus the material's neutrons as
     * {@code neutralmatter} and its protons as {@code chargedmatter} rebuild the material.
     */
    @Nullable
    public static Recipe replicator(List<ItemStack> items, List<FluidStack> fluids) {
        ItemStack usb = null;
        for (ItemStack stack : items) {
            if (stack != null && !stack.isEmpty() && isScannerStick(stack)) { usb = stack; break; }
        }
        if (usb == null) return null;
        GTMaterial material = scannedMaterial(usb);
        if (material == null) return null;
        return replicationRecipe(material, usb);
    }

    /** Replicator row for a USB cable connected to a selected file on a neighboring port. */
    @Nullable
    public static Recipe replicatorFromPort(ItemStack cable, CompoundTag data) {
        GTMaterial material = materialOf(data);
        return material == null ? null : replicationRecipe(material, cable);
    }

    /** GT6's {@code getReplicatorRecipe}: the material's form plus the nucleon matter that builds it. */
    @Nullable
    private static Recipe replicationRecipe(GTMaterial material) {
        return replicationRecipe(material, null);
    }

    @Nullable
    private static Recipe replicationRecipe(GTMaterial material, @Nullable ItemStack usb) {
        if (!material.isValid() || material.getId() <= 0) return null;
        if (material.has(MaterialProperty.ANTIMATTER)) return null;
        long nucleons = material.getProtons() + material.getNeutrons();
        if (nucleons <= 0) return null;
        FluidStack[] matter = matter(material);
        if (matter == null) return null;
        long power = nucleons * REPLICATOR_EU_PER_NUCLEON;
        ItemStack output = primaryForm(material);
        FluidStack fluid = FluidStack.EMPTY;
        if (output.isEmpty()) {
            // GT6 falls back to the material's own fluid when it has no item form at all.
            fluid = com.gregtech.gregtech.loaders.c.GTGeneratedChem
                    .materialFluid(material.getName(), 1000);
            if (fluid == null || fluid.isEmpty()) return null;
        }
        Recipe recipe = new Recipe(
                usb == null ? null : new ItemStack[]{usb.copyWithCount(1)},
                output.isEmpty() ? null : new ItemStack[]{output},
                null, null, matter,
                fluid.isEmpty() ? null : new FluidStack[]{fluid},
                1, power, 0);
        return usb == null ? recipe : recipe.withCatalystInputs(0);
    }

    /** GT6's {@code MatterNeutral(neutrons)} + {@code MatterCharged(protons)}, one mB per nucleon. */
    @Nullable
    private static FluidStack[] matter(GTMaterial material) {
        List<FluidStack> out = new ArrayList<>(2);
        FluidStack neutral = GTFluids.stack("MatterNeutral", (int) material.getNeutrons());
        if (neutral != null && !neutral.isEmpty()) out.add(neutral);
        FluidStack charged = GTFluids.stack("MatterCharged", (int) material.getProtons());
        if (charged != null && !charged.isEmpty()) out.add(charged);
        return out.isEmpty() ? null : out.toArray(FluidStack[]::new);
    }

    /**
     * GT6's form order for a material without a priority prefix, with GT6's counts:
     * gem, gem plate, ingot, plate, nugget (9), chunk (4), dust, tiny dust (9), small dust (4),
     * stick (2).
     */
    private static ItemStack primaryForm(GTMaterial material) {
        List<MaterialPrefix> forms = List.of(MaterialPrefix.gem, MaterialPrefix.plateGem,
                MaterialPrefix.ingot, MaterialPrefix.plate, MaterialPrefix.nugget,
                MaterialPrefix.chunkGt, MaterialPrefix.dust, MaterialPrefix.dustTiny,
                MaterialPrefix.dustSmall, MaterialPrefix.stick);
        for (MaterialPrefix form : forms) {
            if (!form.isValidFor(material)) continue;
            ItemStack stack = GTItems.getStack(form, material, 1);
            if (stack.isEmpty()) continue;
            stack.setCount(Math.min(64, (int) unitCount(form)));
            return stack;
        }
        return ItemStack.EMPTY;
    }

    /** GT6 hands out a whole material unit: 9 nuggets, 4 chunks, 9 tiny dusts, 4 small dusts, 2 sticks. */
    private static long unitCount(MaterialPrefix form) {
        if (form == MaterialPrefix.nugget || form == MaterialPrefix.dustTiny) return 9;
        if (form == MaterialPrefix.chunkGt || form == MaterialPrefix.dustSmall) return 4;
        if (form == MaterialPrefix.stick) return 2;
        return 1;
    }

}
