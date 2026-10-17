package com.gregtech.gregtech.integration.server;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeRecipeMachineBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import com.gregtech.gregtech.data.GregTechTags;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Opt-in dedicated-server checks. A marker file in the server directory selects the phase. */
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.DEDICATED_SERVER,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AddonSamplePersistenceProbe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BlockPos PRESS = new BlockPos(40, 200, 40);
    private static MinecraftServer server;
    private static String phase = "";

    private AddonSamplePersistenceProbe() {}

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        Path game = event.getServer().getServerDirectory().toPath().toAbsolutePath().normalize();
        Path world = event.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        LOGGER.info("ADDON_SAMPLE_PERSISTENCE_PATHS world {} game {}", world, game);
        Path marker = game.resolve("addon-sample-persistence.txt");
        if (!Files.isRegularFile(marker) && world.getParent() != null)
            marker = world.getParent().resolve("addon-sample-persistence.txt");
        if (!Files.isRegularFile(marker)) return;
        String text;
        try {
            text = Files.readString(marker).replace("\uFEFF", "").trim();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
        phase = text.lines().findFirst().orElse("").trim();
        LOGGER.info("ADDON_SAMPLE_PERSISTENCE_MARKER {} {}", phase, marker);
        if (phase.equals("oldsave") || phase.equals("prec")) {
            server = event.getServer();
            try {
                if (phase.equals("oldsave")) inspectOldSave(server.overworld(), text);
                else verifyPrec(server.overworld(), marker.getParent().resolve("prec-receipt.txt"));
                LOGGER.info(phase.equals("oldsave") ? "OLD_SAVE_VERIFIED" : "PRE_C_SAVE_VERIFIED");
            } finally {
                server.halt(false);
            }
            return;
        }
        if (!phase.equals("prepare") && !phase.equals("verify")) return;
        server = event.getServer();
        ServerLevel level = server.overworld();
        level.getChunk(PRESS);
        level.getChunk(PRESS.offset(0, 0, 12));
        if (phase.equals("prepare")) prepare(level);
        else verify(level);
        LOGGER.info(phase.equals("prepare") ? "ADDON_SAMPLE_PERSISTENCE_PREPARED" : "ADDON_SAMPLE_PERSISTENCE_VERIFIED");
        server.halt(false);
    }

    /** Compares a world written by the pre-change code with the receipt that code saved. */
    private static void verifyPrec(ServerLevel level, Path receipt) {
        java.util.List<String> lines;
        try {
            lines = java.nio.file.Files.readAllLines(receipt);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] parts = line.split(" ");
            BlockPos pos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            level.getChunk(pos);
            String block = String.valueOf(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()));
            var be = level.getBlockEntity(pos);
            String beId = be == null ? "missing" : String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()));
            if (!(be instanceof BasicMachineBlockEntity machine))
                throw new IllegalStateException("Pre-C machine missing at " + pos + ": " + block + " / " + beId);
            boolean large = parts[0].equals("large");
            if (large && !((LargeRecipeMachineBlockEntity) machine).isStructureOk())
                throw new IllegalStateException("Pre-C multiblock structure did not load at " + pos);
            ItemStack stack = large
                    ? ((LargeRecipeMachineBlockEntity) machine).portItems(Role.ITEM_FLUID_IO).getStackInSlot(0)
                    : machine.inventory().getStackInSlot(0);
            String item = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            long energy = machine.getEnergyTick();
            long progress = machine.machineControl(null).progress();
            long progressMax = machine.machineControl(null).progressMax();
            LOGGER.info("PRE_C_SAVE_STATE {} block {} be {} energy {} progress {}/{} item {}x{}",
                    parts[0], block, beId, energy, progress, progressMax, item, stack.getCount());
            if (!block.equals(parts[4]) || !beId.equals(parts[5]) || energy != Long.parseLong(parts[6])
                    || progress != Long.parseLong(parts[7]) || progressMax != Long.parseLong(parts[8])
                    || !item.equals(parts[9]) || stack.getCount() != Integer.parseInt(parts[10]))
                throw new IllegalStateException("Pre-C " + parts[0] + " state changed at " + pos + ": " + line);
        }
    }

    /** Reads one built-in machine from a save written before the data-driven registration change. */
    private static void inspectOldSave(ServerLevel level, String text) {
        String[] lines = text.split("\\R");
        if (lines.length < 2) throw new IllegalStateException("Old save marker needs a position line");
        String[] parts = lines[1].trim().split(" ");
        BlockPos pos = new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        level.getChunk(pos);
        var state = level.getBlockState(pos);
        var be = level.getBlockEntity(pos);
        String block = String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        String beId = be == null ? "missing" : String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()));
        if (!(be instanceof BasicMachineBlockEntity machine))
            throw new IllegalStateException("Old replicator is " + block + " / " + beId);
        ItemStack cable = machine.inventory().getStackInSlot(0);
        ItemStack ingot = machine.inventory().getStackInSlot(3);
        String cableId = String.valueOf(BuiltInRegistries.ITEM.getKey(cable.getItem()));
        String ingotId = String.valueOf(BuiltInRegistries.ITEM.getKey(ingot.getItem()));
        int dir = cable.getTag() == null ? -1 : cable.getTag().getInt("gt.usb.dir");
        long progress = machine.machineControl(null).progress();
        long progressMax = machine.machineControl(null).progressMax();
        LOGGER.info("OLD_SAVE_STATE block {} be {} energy {} progress {}/{} slot0 {}x{} dir {} slot3 {}x{}",
                block, beId, machine.getEnergyTick(), progress, progressMax, cableId, cable.getCount(), dir, ingotId, ingot.getCount());
        if (!block.equals("gregtech:replicator_osmium") || !beId.equals("gregtech:be_replicator_osmium")
                || machine.getEnergyTick() != 0 || progress != 0 || progressMax != 0
                || !cableId.equals("gregtech:usb3_cable") || cable.getCount() != 1 || dir != 2
                || !ingotId.equals("gregtech:ingot_iron") || ingot.getCount() != 1)
            throw new IllegalStateException("Old replicator state changed");
    }

    private static void prepare(ServerLevel level) {
        if (!level.getBlockState(PRESS).isAir()) throw new IllegalStateException("Persistence prepare found a block at " + PRESS);
        var machine = place(level, "addon_sample:sample_press", PRESS);
        machine.inventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        machine.doEnergyInjection(GregTechTags.Energy.EU, null, 32, 64, true);
        BasicMachineBlockEntity.serverTick(level, PRESS, machine.getBlockState(), machine);
        machine.doEnergyInjection(GregTechTags.Energy.EU, null, 32, 64, true);
        machine.inventory().setStackInSlot(0, new ItemStack(Items.STICK));
        if (machine.machineControl(null).progress() <= 0 || machine.getEnergyTick() <= 0
                || !machine.inventory().getStackInSlot(0).is(Items.STICK))
            throw new IllegalStateException("Sample press did not keep partial work before the save");
        var largePos = PRESS.offset(0, 0, 12);
        var large = (LargeRecipeMachineBlockEntity) place(level, "addon_sample:large_sample_press", largePos);
        var cells = LargeMachineLayouts.cells("largesamplepress");
        for (var cell : cells) level.setBlockAndUpdate(cell.at(largePos, Direction.NORTH), cell.block().defaultBlockState());
        if (!large.isStructureOk()) throw new IllegalStateException("Large sample press did not form before the save");
        if (!large.portItems(Role.ITEM_FLUID_IO).insertItem(0, new ItemStack(Items.IRON_BLOCK), false).isEmpty())
            throw new IllegalStateException("Large sample press port refused the block");
        large.injectPortEnergy(Role.ENERGY_INPUT, GregTechTags.Energy.EU, 512, 1024, true);
        BasicMachineBlockEntity.serverTick(level, largePos, large.getBlockState(), large);
        large.injectPortEnergy(Role.ENERGY_INPUT, GregTechTags.Energy.EU, 512, 1024, true);
        if (!large.portItems(Role.ITEM_FLUID_IO).insertItem(0, new ItemStack(Items.STICK), false).isEmpty())
            throw new IllegalStateException("Large sample press port refused the saved item");
        LOGGER.info("ADDON_SAMPLE_PERSISTENCE_STATE press {}/{} energy {} large {}/{} energy {}",
                machine.machineControl(null).progress(), machine.machineControl(null).progressMax(), machine.getEnergyTick(),
                large.machineControl(null).progress(), large.machineControl(null).progressMax(), large.getEnergyTick());
        if (machine.machineControl(null).progress() <= 0 || machine.getEnergyTick() <= 0
                || !machine.inventory().getStackInSlot(0).is(Items.STICK)
                || large.machineControl(null).progress() <= 0 || large.getEnergyTick() <= 0
                || !large.portItems(Role.ITEM_FLUID_IO).getStackInSlot(0).is(Items.STICK))
            throw new IllegalStateException("Sample machines did not keep partial work before the save");
    }

    private static void verify(ServerLevel level) {
        var press = (BasicMachineBlockEntity) level.getBlockEntity(PRESS);
        if (press == null || !"samplepress".equals(press.spec().machineName()))
            throw new IllegalStateException("Saved sample press did not load");
        if (press.machineControl(null).progress() <= 0 || press.getEnergyTick() <= 0
                || !press.inventory().getStackInSlot(0).is(Items.STICK))
            throw new IllegalStateException("Saved sample press lost progress, energy, or the input");
        var largePos = PRESS.offset(0, 0, 12);
        var entity = level.getBlockEntity(largePos);
        if (!(entity instanceof LargeRecipeMachineBlockEntity large))
            throw new IllegalStateException("Saved large sample press block entity did not load: " + entity);
        if (!large.isStructureOk()) throw new IllegalStateException("Saved large sample press structure did not load");
        if (large.machineControl(null).progress() <= 0)
            throw new IllegalStateException("Saved large sample press lost progress " + large.machineControl(null).progress());
        if (large.getEnergyTick() <= 0)
            throw new IllegalStateException("Saved large sample press lost energy " + large.getEnergyTick());
        var stored = large.portItems(Role.ITEM_FLUID_IO).getStackInSlot(0);
        if (!stored.is(Items.STICK))
            throw new IllegalStateException("Saved large sample press lost the stick, slot 0 is " + stored);
    }

    private static BasicMachineBlockEntity place(ServerLevel level, String id, BlockPos pos) {
        var block = (BasicMachineBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        level.setBlockAndUpdate(pos, block.defaultBlockState().setValue(BasicMachineBlock.FACING, Direction.NORTH));
        return (BasicMachineBlockEntity) level.getBlockEntity(pos);
    }
}
