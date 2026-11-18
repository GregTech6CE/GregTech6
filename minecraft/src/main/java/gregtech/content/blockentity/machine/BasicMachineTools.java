package gregtech.content.blockentity.machine;

import gregtech.content.cover.PanelCoverHost;
import gregtech.content.world.GarbageData;
import gregtech.engine.energy.FaceConfig;
import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.machine.MachineInventory;
import gregtech.engine.machine.MachineToolSettings;
import gregtech.engine.machine.MachineToolSettings.Transfer;
import gregtech.engine.multiblock.MultiblockToolTarget;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** Tool interactions and automatic transfer settings of a basic machine. */
final class BasicMachineTools {
    private final BasicMachineBlockEntity machine;
    private final Runnable markMachineDataChanged;
    private final MachineToolSettings settings = new MachineToolSettings();

    BasicMachineTools(BasicMachineBlockEntity machine, Runnable markMachineDataChanged) {
        this.machine = machine;
        this.markMachineDataChanged = markMachineDataChanged;
    }

    void save(CompoundTag tag) { settings.save(tag); }
    void load(CompoundTag tag) { settings.load(tag); }
    boolean enabled(Transfer transfer) { return settings.enabled(transfer); }
    boolean emptyInputsOnly() { return settings.emptyInputsOnly(); }
    boolean emptyOutputsOnly() { return settings.emptyOutputsOnly(); }

    boolean allowsRecipeStart(MachineInventory inventory) {
        return !settings.emptyOutputsOnly() || inventory == null
                || (!inventory.hasItemOutput() && !inventory.hasFluidOutput());
    }

    boolean consumes(ItemStack tool) {
        return PanelCoverHost.blockConsumesTool(machine, tool)
                || GTToolHelper.matchesTool(tool, GTToolType.CROWBAR)
                || GTToolHelper.isScrewdriver(tool)
                || GTToolHelper.isMonkeyWrench(tool)
                || GTToolHelper.isSoftHammer(tool)
                || GTToolHelper.isMagnifyingGlass(tool)
                || GTToolHelper.matchesTool(tool, GTToolType.PLUNGER);
    }

    void autoIO(Level level, BlockPos pos, FaceConfig faceConfig) {
        MachineInventory inventory = machine.inventoryStorage();
        if (faceConfig == null || inventory == null) return;
        if (machine.automaticTransferEnabled(Transfer.ITEM_INPUT)) {
            int side = faceConfig.itemAutoInput();
            if (FaceConfig.autoValid(side)) machine.autoInputItems(level, pos, side);
            else machine.positionedItemInput();
        }
        if (machine.automaticTransferEnabled(Transfer.ITEM_OUTPUT)) {
            int side = faceConfig.itemAutoOutput();
            if (FaceConfig.autoValid(side)) machine.autoOutputItems(level, pos, side);
            else machine.positionedItemOutput();
        }
        if (machine.automaticTransferEnabled(Transfer.FLUID_INPUT)
                && inventory.tanksInput().length > 0) {
            int side = faceConfig.fluidAutoInput();
            if (FaceConfig.autoValid(side)) machine.autoInputFluids(level, pos, side);
            else machine.positionedFluidInput();
        }
        if (machine.automaticTransferEnabled(Transfer.FLUID_OUTPUT)
                && inventory.tanksOutput().length > 0 && FaceConfig.autoValid(faceConfig.fluidAutoOutput())) {
            machine.autoOutputFluids(level, pos, faceConfig.fluidAutoOutput());
        }
    }

    InteractionResult use(Player player, InteractionHand hand, BlockHitResult hit, FaceConfig faceConfig,
            boolean configured) {
        Level level = machine.getLevel();
        BlockPos pos = machine.getBlockPos();
        var held = player.getItemInHand(hand);
        GTToolType tool = null;
        for (var candidate : new GTToolType[] {
                GTToolType.SCREWDRIVER, GTToolType.MONKEY_WRENCH,
                GTToolType.SOFT_HAMMER, GTToolType.MAGNIFYING_GLASS, GTToolType.PLUNGER}) {
            if (GTToolHelper.matchesTool(held, candidate)) { tool = candidate; break; }
        }
        if (tool == null || level == null || !configured) return InteractionResult.PASS;
        if (player.isSpectator() || !player.mayBuild() || !level.mayInteract(player, pos))
            return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var messages = new ArrayList<Component>();
        long cost = 0;
        switch (tool) {
            case SCREWDRIVER -> {
                settings.cycleMode();
                appendProcessingMode(messages);
                cost = 10000;
            }
            case MONKEY_WRENCH -> {
                for (var transfer : Transfer.values()) {
                    int side = transfer.side(faceConfig);
                    if (FaceConfig.autoValid(side) && machine.relativeToAbsolute(side) == hit.getDirection()) {
                        settings.toggle(transfer);
                        messages.add(Component.translatable(transfer.message(settings.enabled(transfer))));
                        cost += 10000;
                    }
                }
                if (cost == 0) messages.add(Component.translatable("message.gt6ce.machine.no_auto_transfer"));
            }
            case SOFT_HAMMER -> {
                machine.resetProcessingWithTool();
                messages.add(Component.translatable("message.gt6ce.machine.processing_reset"));
                cost = 10000;
            }
            case MAGNIFYING_GLASS -> {
                // Preserve multiblock inspection as well as the basic machine settings report.
                if (machine instanceof MultiblockToolTarget target)
                    target.useMultiblockTool(new UseOnContext(player, hand, hit), messages);
                else machine.appendMachineInspection(messages);
                cost = 1;
            }
            case PLUNGER -> {
                MachineInventory inventory = machine.inventoryStorage();
                if (inventory == null) return InteractionResult.PASS;
                long trashed = purgeTank(level, inventory.tanksOutput());
                if (trashed == 0) trashed = purgeTank(level, inventory.tanksInput());
                if (trashed == 0) return InteractionResult.PASS;
                cost = trashed;
            }
            default -> { return InteractionResult.PASS; }
        }
        for (var message : messages) player.displayClientMessage(message, false);
        if (cost > 0) {
            GTToolHelper.damageForToolClickReturn(held, cost, player,
                    hand == InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            if (tool != GTToolType.MAGNIFYING_GLASS) {
                machine.inventoryChanged();
                markMachineDataChanged.run();
                level.playSound(null, pos, SoundEvents.LEVER_CLICK,
                        SoundSource.BLOCKS, 0.5F, 1.0F);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** Original GT6 plunger: trash up to 1000 mB from the first non-empty output, then input tank. */
    private static long purgeTank(Level level, FluidTankGT[] tanks) {
        for (var tank : tanks) {
            var drained = tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                GarbageData.get(level).trash(drained);
                return drained.getAmount();
            }
        }
        return 0;
    }

    void appendInspection(List<Component> messages, FaceConfig faceConfig) {
        appendProcessingMode(messages);
        for (var transfer : Transfer.values())
            if (FaceConfig.autoValid(transfer.side(faceConfig)))
                messages.add(Component.translatable(transfer.message(settings.enabled(transfer))));
    }

    private void appendProcessingMode(List<Component> messages) {
        messages.add(Component.translatable(settings.emptyOutputsOnly()
                ? "message.gt6ce.machine.output_empty" : "message.gt6ce.machine.output_space"));
        messages.add(Component.translatable(settings.emptyInputsOnly()
                ? "message.gt6ce.machine.input_empty" : "message.gt6ce.machine.input_all"));
    }
}
