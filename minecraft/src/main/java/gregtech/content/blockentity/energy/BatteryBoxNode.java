package gregtech.content.blockentity.energy;

import gregtech.GregTechConfig;
import gregtech.api.platform.machine.MachineControl;
import gregtech.content.block.energy.BatteryBoxBlock;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.BatteryBoxEnergy;
import gregtech.content.energy.OriginalEnergyDeviceTooltipData;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyTransfer;
import gregtech.engine.energy.MachineOverload;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Connects the battery box's slot and buffer state to the energy network and the world. */
final class BatteryBoxNode extends EnergyDeviceBehavior implements MachineControl {
    private final EnergyNodeSpec spec;
    final BatteryBoxInventory inventory;
    final BatteryBoxEnergy energy;
    private final MenuProvider menu;
    private final EnergyDeviceTool batteryRemoval = this::returnBattery;
    private boolean updatePending;

    BatteryBoxNode(EnergyNodeBlockEntity host, EnergyNodeSpec spec) {
        super(host);
        this.spec = spec;
        inventory = new BatteryBoxInventory(host, spec);
        energy = new BatteryBoxEnergy(inventory.batteries, spec.inputRate(), spec.inType(), this::energyChanged);
        menu = inventory.menu();
    }

    private void energyChanged() {
        if (isCurrent()) host.setChanged();
    }

    @Override
    void onHostChanged() {
        updatePending = true;
    }

    @Override
    boolean allowsCovers() {
        return false;
    }

    @Override
    boolean supportsControlPanels() {
        return true;
    }

    @Override
    boolean coverPossible() {
        return energy.buffer() > spec.outputRate();
    }

    @Override
    @Nullable
    EnergyDeviceTool toolAction(ItemStack tool) {
        var action = super.toolAction(tool);
        if (action != null) return action;
        return GTToolHelper.matchesTool(tool, GTToolType.CROWBAR) ? batteryRemoval : null;
    }

    @Override
    InteractionResult use(Player player, ItemStack held) {
        boolean client = player.level().isClientSide;
        var action = toolAction(held);
        if ((action == null || action == batteryRemoval) && BatteryBoxInventory.capacityOf(held, spec.inType()) > 0) {
            if (!client && inventory.install(held) && !player.getAbilities().instabuild) held.shrink(1);
        } else if (!client) {
            if (action != null) action.use(player, held);
            else if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(menu);
        }
        return InteractionResult.sidedSuccess(client);
    }

    private void returnBattery(Player player, ItemStack tool) {
        EnergyDeviceTool.returnItem(player, inventory.remove());
    }

    @Override
    @Nullable
    MenuProvider menu() {
        return isCurrent() ? menu : null;
    }

    EnergyNodeBlockEntity.BatteryStatus status() {
        var items = new ArrayList<ItemStack>(inventory.getSlots());
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            items.add(inventory.getStackInSlot(slot).copy());
        }
        return new EnergyNodeBlockEntity.BatteryStatus(
                energy.buffer(),
                energy.bufferCapacity(),
                energy.stored(),
                energy.capacity(),
                energy.offered(),
                List.copyOf(items));
    }

    @Override
    void saveState(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        energy.save(tag);
        inventory.save(tag, lookup);
    }

    @Override
    void loadState(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        host.bufferedInput = 0;
        energy.load(tag);
        inventory.load(tag, lookup);
        // A block-item snapshot can be applied after the block has already been placed.
        onHostChanged();
    }

    @Override
    void dropContents() {
        inventory.dropContents();
    }

    @Override
    void detach() {
        inventory.detach();
    }

    @Override
    void tick(Level level, BlockPos pos, BlockState state) {
        long time = level.getGameTime();
        host.panels().beforeTick();
        host.tickCoverComponents(level);
        energy.tick(time, level, pos);
        long offered = energy.offered();
        if (offered > 0) {
            long sent = EnergyTransfer.emitEnergyToSide(
                    spec.outType(), host.facing(), host.effectiveOutputRate(), offered, host);
            energy.emitted(sent);
        }
        host.panels().afterTick();
        if (time % 20 == 0) {
            int charge = energy.displayState();
            boolean sendUpdate = updatePending;
            updatePending = false;
            if (state.hasProperty(BatteryBoxBlock.CHARGE_STATE)
                    && state.getValue(BatteryBoxBlock.CHARGE_STATE) != charge) {
                level.setBlock(pos, state.setValue(BatteryBoxBlock.CHARGE_STATE, charge), Block.UPDATE_ALL);
            } else if (sendUpdate) {
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    boolean emitsTo(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) {
        return (theoretical || energy.enabled()) && (side == null || side == host.facing());
    }

    @Override
    long minInput(GregTechTags.Tag type, @Nullable Direction side) {
        return type == spec.inType() ? OriginalEnergyDeviceTooltipData.batteryInputMinimum(inputRate()) : 0;
    }

    @Override
    long minOutput(GregTechTags.Tag type, @Nullable Direction side) {
        return type == spec.outType() ? outputRate() : 0;
    }

    @Override
    long maxOutput(GregTechTags.Tag type, @Nullable Direction side) {
        return type == spec.outType() ? outputRate() : 0;
    }

    @Override
    long demanded(GregTechTags.Tag type, @Nullable Direction side, long size) {
        if (size == Long.MIN_VALUE || !host.isEnergyAcceptingFrom(type, side, false)) return 0;
        return energy.inject(Math.abs(size), Long.MAX_VALUE, false);
    }

    @Override
    long inject(GregTechTags.Tag type, @Nullable Direction side, long size, long amount, boolean execute) {
        if (size == Long.MIN_VALUE
                || size == 0
                || amount <= 0
                || !host.isEnergyAcceptingFrom(type, side, false)
                || !energy.canReceive()) return 0;
        long packet = Math.abs(size);
        if (packet > host.getEnergySizeInputMax(type, side)) {
            if (execute) overload(packet);
            return amount;
        }
        long used = energy.inject(packet, amount, execute);
        if (execute && used > 0) {
            Level level = host.getLevel();
            if (level != null) host.conversionMeter().input(level.getGameTime(), packet, used);
        }
        return used;
    }

    private void overload(long packet) {
        Level level = host.getLevel();
        if (level == null || level.isClientSide) return;
        if (GregTechConfig.breakByOverload()) host.dropContents();
        MachineOverload.apply(host, packet, spec.inType());
    }

    @Override
    long stored() {
        return energy.buffer();
    }

    @Override
    long capacity() {
        return inventory.installedCapacity();
    }

    @Override
    Collection<GregTechTags.Tag> capacitorTypes(@Nullable Direction side) {
        return List.of(spec.inType());
    }

    @Override
    long energyStored(GregTechTags.Tag type) {
        return type == spec.inType() ? energy.stored() : 0;
    }

    @Override
    long energyCapacity(GregTechTags.Tag type) {
        return type == spec.inType() ? energy.capacity() : 0;
    }

    @Override
    MachineControl control() {
        return this;
    }

    @Override
    public boolean available() {
        return isCurrent();
    }

    @Override
    public boolean supportsMode() {
        return true;
    }

    @Override
    public int mode() {
        return energy.mode();
    }

    @Override
    public int setMode(int mode) {
        if (!available()) return 0;
        energy.mode(mode);
        return energy.mode();
    }

    @Override
    public boolean enabled() {
        return energy.enabled();
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        if (!available()) return false;
        energy.enabled(enabled);
        return enabled;
    }

    @Override
    public boolean running() {
        return energy.running();
    }

    @Override
    public boolean active() {
        return energy.emitted();
    }

    @Override
    public long progress() {
        return energy.buffer();
    }

    @Override
    public long progressMax() {
        return energy.bufferCapacity();
    }
}
