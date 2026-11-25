package gregtech.content.blockentity.energy;

import gregtech.api.platform.machine.MachineControl;
import gregtech.content.cover.ComponentCoverAccess;
import gregtech.content.data.GregTechTags;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTFluids;
import gregtech.engine.energy.SteamTurbineConversion;
import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.inventory.BlockContents;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.stack.NbtCompat;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import java.util.EnumMap;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** Steam enters at the rear, adds energy over two ticks and returns condensate through the lateral faces. */
final class SteamTurbineNode implements MachineControl {
    private static final String PENDING = "gt.turbine_pending";
    private static final String REMAINDER = "gt.steam_remainder";
    private final EnergyNodeBlockEntity host;
    private final FluidTankGT steamTank;
    private boolean detached;
    private long pendingEnergy;
    private long steamRemainder;
    /** Returned to players who installed a rotor in an older version. */
    private ItemStack legacyRotor = ItemStack.EMPTY;

//? if <1.21.1 {
    private final EnumMap<Direction, LazyOptional<IFluidHandler>> sidedCapabilities = new EnumMap<>(Direction.class);
    @Nullable
    private LazyOptional<IFluidHandler> unsidedCapability;
//?}

    SteamTurbineNode(EnergyNodeBlockEntity host, long inputRate) {
        this.host = host;
        steamTank = new FluidTankGT(inputRate * 4).setOnChanged(host::setChanged);
    }

    long steamAmount() {
        return steamTank.getAmount();
    }

    long steamCapacity() {
        return steamTank.capacity();
    }

    static boolean isRotorItem(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof MaterialFormItem materialItem
                && materialItem.getPrefix() == MaterialPrefix.rotor;
    }

    boolean hasRotor() {
        return !legacyRotor.isEmpty();
    }

    @Nullable
    EnergyDeviceTool toolAction(ItemStack tool) {
        if (GTToolHelper.matchesTool(tool, GTToolType.PLUNGER)) return this::purgeWithTool;
        return hasRotor() && GTToolHelper.matchesTool(tool, GTToolType.CROWBAR) ? this::returnRotor : null;
    }

    private void purgeWithTool(Player player, ItemStack tool) {
        long removed = purge();
        if (removed > 0) GTToolHelper.damageForToolClickReturn(tool, removed, player);
    }

    private void returnRotor(Player player, ItemStack tool) {
        EnergyDeviceTool.returnItem(player, removeRotor());
    }

    ItemStack removeRotor() {
        ItemStack removedRotor = legacyRotor;
        legacyRotor = ItemStack.EMPTY;
        host.setChanged();
        return removedRotor;
    }

    long purge() {
        long removedSteam = steamTank.getAmount();
        steamTank.remove(removedSteam);
        host.setChanged();
        return removedSteam;
    }

    void tick() {
        if (host.spec().inputRate() <= 0) return;
        // Older saves can contain other fluids. Discard them before converting steam into RU.
        if (!steamTank.getFluid().isEmpty() && !isSteam(steamTank.getFluid())) {
            steamTank.remove(steamTank.getAmount());
            host.setChanged();
        }

        var conversion = SteamTurbineConversion.step(
                host.bufferedInput, pendingEnergy, steamTank.getAmount(), steamRemainder, host.spec().inputRate());
        host.bufferedInput = conversion.energy();
        pendingEnergy = conversion.pending();
        steamRemainder = conversion.remainder();
        if (conversion.consumed() > 0) {
            steamTank.remove(conversion.consumed());
        }
        emitCondensate(conversion.condensate());
        host.setChanged();
    }

    private void emitCondensate(long amount) {
        var distilledWater = GTFluids.still("DistW");
        if (host.getLevel() == null || amount <= 0 || distilledWater == null) return;
        var condensate = new FluidStack(distilledWater.get(), (int) Math.min(Integer.MAX_VALUE, amount));
        for (Direction side : Direction.values()) {
            if (side.getAxis() == host.facing().getAxis()) continue;
            var adjacentPos = host.getBlockPos().relative(side);
            var level = host.getLevel();
            if (!level.hasChunkAt(adjacentPos)) continue;
            var neighbor = level.getBlockEntity(adjacentPos);
            if (neighbor == null) continue;
//? if >=1.21.1 {
            /*var receiver = level.getCapability(Capabilities.FluidHandler.BLOCK, adjacentPos, side.getOpposite());
*///?} else {
            var receiver = neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite())
                    .resolve()
                    .orElse(null);
//?}
            if (receiver != null) {
                int offered = condensate.getAmount();
                int accepted = receiver.fill(condensate.copy(), IFluidHandler.FluidAction.EXECUTE);
                condensate.shrink(Math.max(0, Math.min(offered, accepted)));
            }
            if (condensate.isEmpty()) break;
        }
        // The turbine discards condensate that no adjacent tank recovers.
    }

    long inject(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize, long packetCount,
            boolean execute) {
        if (packetCount <= 0 || packetSize == 0 || packetSize == Long.MIN_VALUE
                || !host.isEnergyAcceptingFrom(energyType, side, false)) {
            return 0;
        }
        long magnitude = Math.abs(packetSize);
        if (magnitude > host.spec().inputRate() * 2) {
            if (execute) host.handleInputOverload(magnitude);
            return packetCount;
        }
        long freeCapacity = Math.max(0, host.capacity() - host.bufferedInput);
        long packetsToFill = freeCapacity / magnitude + (freeCapacity % magnitude == 0 ? 0 : 1);
        long acceptedPackets = Math.min(packetCount, packetsToFill);
        if (execute && acceptedPackets > 0) {
            long acceptedEnergy = magnitude * acceptedPackets;
            if (host.getLevel() != null) host.conversionMeter().input(host.getLevel().getGameTime(), acceptedEnergy);
            host.bufferedInput += acceptedEnergy;
            host.setChanged();
        }
        return acceptedPackets;
    }

    boolean exposesInlet(@Nullable Direction side) {
        return !detached && !host.isRemoved() && side != host.facing();
    }

    private IFluidHandler coveredInlet(Direction side) {
        return ComponentCoverAccess.fluids(host, side, new SteamInlet(side));
    }

//? if >=1.21.1 {
    /*IFluidHandler capability(@Nullable Direction side) {
        return side == null ? new SteamInlet(null) : coveredInlet(side);
    }
*///?} else {
    LazyOptional<IFluidHandler> capability(@Nullable Direction side) {
        if (detached) return LazyOptional.empty();
        if (unsidedCapability == null || !unsidedCapability.isPresent()) {
            unsidedCapability = LazyOptional.of(() -> new SteamInlet(null));
        }
        if (side == null) return unsidedCapability;
        return sidedCapabilities.computeIfAbsent(side, face -> LazyOptional.of(() -> coveredInlet(face)));
    }

    void invalidateCaps() {
        sidedCapabilities.values().forEach(LazyOptional::invalidate);
        sidedCapabilities.clear();
        if (unsidedCapability != null) {
            unsidedCapability.invalidate();
            unsidedCapability = null;
        }
    }
//?}

    void detach() {
        detached = true;
//? if <1.21.1 {
        invalidateCaps();
//?}
    }

    private static boolean isSteam(FluidStack fluid) {
        var steam = GTFluids.still("Steam");
        return !fluid.isEmpty() && steam != null && fluid.getFluid().isSame(steam.get());
    }

    private final class SteamInlet implements IFluidHandler {
        @Nullable
        private final Direction side;

        SteamInlet(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return exposesInlet(side) ? 1 : 0;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return getTanks() == 0 ? FluidStack.EMPTY : steamTank.getFluid().copy();
        }

        @Override
        public int getTankCapacity(int tank) {
            return getTanks() == 0 ? 0 : (int) steamTank.capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack fluid) {
            return exposesInlet(side) && isSteam(fluid);
        }

        @Override
        public int fill(FluidStack fluid, FluidAction action) {
            if (!exposesInlet(side)) return 0;
            int filled = 0;
            if (!host.converterStopped
                    && (side == null || side == host.facing().getOpposite())
                    && isSteam(fluid)) {
                filled = steamTank.fill(fluid, action);
            }
            if (action.execute() && host.getLevel() != null) {
                host.conversionMeter().input(host.getLevel().getGameTime(), filled);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack fluid, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    @Override
    public boolean available() {
        return !detached && !host.isRemoved();
    }

    @Override
    public boolean enabled() {
        return !host.converterStopped;
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        if (!available()) return false;
        host.converterStopped = !enabled;
        host.setChanged();
        return enabled;
    }

    @Override
    public boolean running() {
        return !host.converterStopped && (host.bufferedInput > 0 || pendingEnergy > 0);
    }

    @Override
    public boolean active() {
        return running();
    }

    @Override
    public long progress() {
        return host.bufferedInput;
    }

    @Override
    public long progressMax() {
        return host.capacity();
    }

    static void saveEmptyConversion(CompoundTag tag) {
        tag.putLong(PENDING, 0);
        tag.putLong(REMAINDER, 0);
    }

    void save(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        tag.putLong(PENDING, pendingEnergy);
        tag.putLong(REMAINDER, steamRemainder);
        if (!legacyRotor.isEmpty()) tag.put("gt.rotor", NbtCompat.saveItem(legacyRotor, lookup));
        CompoundTag tankTag = new CompoundTag();
        NbtCompat.writeTank(steamTank, tankTag, lookup);
        tag.put("gt.steam", tankTag);
    }

    void load(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        pendingEnergy = Math.max(0, tag.getLong(PENDING));
        steamRemainder = Math.floorMod(tag.getLong(REMAINDER), SteamTurbineConversion.STEAM_PER_WATER);
        legacyRotor = tag.contains("gt.rotor")
                ? NbtCompat.readItem(tag.getCompound("gt.rotor"), lookup)
                : ItemStack.EMPTY;
        NbtCompat.readTank(steamTank, tag.getCompound("gt.steam"), lookup);
        steamTank.setCapacity(host.spec().inputRate() * 4);
    }

    void dropContents() {
        BlockContents.drop(host, legacyRotor);
        legacyRotor = ItemStack.EMPTY;
    }
}
