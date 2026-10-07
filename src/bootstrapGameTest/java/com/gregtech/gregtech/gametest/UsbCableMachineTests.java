package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.book.GTMaterialDictionary;
import com.gregtech.gregtech.content.data.UsbDataCable;
import com.gregtech.gregtech.content.data.UsbDataMedia;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.recipe.GTMaterialDataRecipes;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTStorage;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** A USB cable is an item in the machine input and reads a selected adjacent data-switch file. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class UsbCableMachineTests {
    private UsbCableMachineTests() {}

    private static CompoundTag materialFile() {
        CompoundTag data = new CompoundTag();
        data.putShort(GTMaterialDataRecipes.NBT_REPLICATOR_DATA, (short) Materials.Iron.getId());
        return data;
    }

    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void printerReadsSelectedAdjacentHddOverCable(GameTestHelper h) {
        var block = MachineRegistry.basicMachines().stream()
                .filter(entry -> entry.isPresent() && "printer_stainless_steel".equals(entry.get().basicSpec().id()))
                .findFirst().orElseThrow(() -> new IllegalStateException("no printer registered")).get();
        BlockPos printerPos = new BlockPos(2, 1, 2);
        BlockPos switchPos = printerPos.north();
        h.setBlock(printerPos, Blocks.AIR);
        h.setBlock(printerPos, block);
        h.setBlock(switchPos, GTStorage.HDD_SWITCH.get());
        BasicMachineBlockEntity printer = (BasicMachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(printerPos));
        UsbSwitchBlockEntity port = (UsbSwitchBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(switchPos));
        ItemStack drive = new ItemStack(GTTechnological.get("usb3_hdd"));
        h.assertTrue(UsbDataMedia.writeDrive(drive, 7, 1, materialFile()), "HDD slot 7 stores a tier-1 printer file");
        port.items().setStackInSlot(0, drive);
        port.setMode(7);
        ItemStack cable = new ItemStack(GTTechnological.get("usb3_cable"));
        cable.getOrCreateTag().putByte(UsbDataCable.NBT_DIRECTION, (byte) Direction.EAST.ordinal());
        h.assertTrue(UsbDataCable.readAdjacent(printer, cable, 1) == null,
                "a cable directed east cannot silently read the north-side port");
        cable.getOrCreateTag().putByte(UsbDataCable.NBT_DIRECTION, (byte) Direction.NORTH.ordinal());
        h.assertTrue(UsbDataCable.readAdjacent(printer, cable, 1) != null,
                "the same cable reads the selected port when its direction points north");
        int sheets = GTMaterialDictionary.pages(Materials.Iron).size() > 50 ? 6 : 3;
        int dyeAmount = sheets == 6 ? 144 : 72;
        printer.inventory().setStackInSlot(0, new ItemStack(Items.PAPER, sheets));
        printer.inventory().setStackInSlot(1, cable.copy());
        var dye = GTFluids.stack("Dye_Chemical_Black", dyeAmount);
        h.assertTrue(dye != null && !dye.isEmpty(), "printer dye is available");
        printer.getTanksInput()[0].setFluid(dye);
        for (int i = 0; i < 40; i++) {
            printer.doEnergyInjection(GregTechTags.Energy.EU, null, block.basicSpec().energyInMax(), 1, true);
            BasicMachineBlockEntity.serverTick(h.getLevel(), printer.getBlockPos(), printer.getBlockState(), printer);
        }
        h.assertTrue(printer.inventory().getStackInSlot(0).isEmpty(), "the powered printer consumed paper");
        h.assertTrue(ItemStack.isSameItemSameTags(cable, printer.inventory().getStackInSlot(1)),
                "the USB cable remains in the input after printing");
        boolean found = GTMaterialDictionary.materialOf(printer.inventory().getStackInSlot(2)) == Materials.Iron;
        for (var drop : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(printer.getBlockPos()).inflate(4)))
            if (GTMaterialDictionary.materialOf(drop.getItem()) == Materials.Iron) found = true;
        h.assertTrue(found, "printer output comes from the adjacent selected HDD file");
        h.assertTrue(port.mode() == 7 && port.readUsbData(Direction.SOUTH, 1) != null,
                "printing retains the selected HDD file");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void replicatorCableRecipeRetainsCableAndHonorsTier(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 1), GTStorage.HDD_SWITCH.get());
        var port = (UsbSwitchBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 1, 1)));
        ItemStack drive = new ItemStack(GTTechnological.get("usb3_hdd"));
        UsbDataMedia.writeDrive(drive, 4, 3, materialFile());
        port.items().setStackInSlot(0, drive);
        port.setMode(4);
        ItemStack cable = new ItemStack(GTTechnological.get("usb3_cable"));
        var recipe = GTMaterialDataRecipes.replicatorFromPort(cable, port.readUsbData(Direction.SOUTH, 3));
        h.assertTrue(recipe != null && recipe.isCatalystInput(0), "replicator accepts a tier-3 port file as retained cable");
        h.assertTrue(RecipeInputs.consume(recipe, List.of(cable), List.of(recipe.mFluidInputs), 1) != null,
                "matching cable and matter can execute the recipe");
        h.assertTrue(port.readUsbData(Direction.SOUTH, 2) == null, "tier-2 cable cannot read tier-3 data");
        port.setMode(5);
        h.assertTrue(port.readUsbData(Direction.SOUTH, 3) == null, "unwritten selected slot serves no data");
        h.succeed();
    }
}
