/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import gregtech.api.energy.EnergyTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Energy operations for a behavior machine, with explicit packet and unit accounting.
 * Obtained through {@link MachineBehaviorContext#energyAccess()} and valid only during the
 * same behavior callback as its context. Operations execute immediately on the server;
 * this view does not add simulation, new channels, or change the machine's declarations.
 * Negative packet sizes retain the declared KU/RU direction policy. Negative unit amounts
 * and overflowing requests are rejected before changing the machine.
 */
public final class MachineEnergy {
    private final MachineBehaviorContext context;

    private MachineEnergy(MachineBehaviorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /** Adapts an existing context without adding implementation requirements to that context. */
    public static MachineEnergy of(MachineBehaviorContext context) {
        return new MachineEnergy(context);
    }

    /** Main input's stored units. Returns zero when no main input is declared, even for a generator. */
    public long mainInputStored() { return context.mainInputStored(); }

    /** Removes all requested units from the main input, or returns false without removing any. */
    public boolean consumeMainInput(long units) {
        requireUnits(units);
        return context.consume(units);
    }

    /** Stores generated units in the main buffer, including on output-only machines. Returns units stored. */
    public long generate(long units) {
        requireUnits(units);
        return context.generate(units);
    }

    /** Configured main capacity in units. Zero keeps the host's rated-input drain policy. */
    public long capacity() { return context.energyCapacity(); }

    /** Changes the configured capacity; the host applies its existing tick-end policy. */
    public void setCapacity(long units) {
        requireUnits(units);
        context.setCapacity(units);
    }

    /** Stored auxiliary-input units; unknown or undeclared types return zero. */
    public long auxiliaryStored(String type) { return context.auxiliaryStored(type); }

    /** Removes all requested auxiliary-input units, or returns false without removing any. */
    public boolean consumeAuxiliary(String type, long units) {
        requireUnits(units);
        return context.consume(type, units);
    }

    /**
     * Emits through the declared output faces and debits the main buffer for accepted packets.
     * The result contains whole packets only. Output declaration limits still apply.
     */
    public EnergyTransfer emitStored(long packetSize, long packetCount) {
        if (EnergyTransfer.requestUnits(packetSize, packetCount) == 0)
            return new EnergyTransfer(packetSize, packetCount, 0);
        return EnergyTransfer.fromPackets(packetSize, packetCount,
                context.emitPacketsFromFaces(packetSize, packetCount));
    }

    /**
     * Emits to one receiver and debits the main buffer. {@code receivingSide} is the receiver's
     * world-facing side, or null for an unsided request. Declared output faces are not used;
     * declared type and packet limits still apply. Missing or unloaded targets accept nothing.
     */
    public EnergyTransfer emitStoredTo(BlockPos target, @Nullable Direction receivingSide,
                                       long packetSize, long packetCount) {
        if (EnergyTransfer.requestUnits(packetSize, packetCount) == 0)
            return new EnergyTransfer(packetSize, packetCount, 0);
        return EnergyTransfer.fromPackets(packetSize, packetCount,
                context.emitTo(target, receivingSide, packetSize, packetCount));
    }

    /**
     * Transfers from the main buffer to a bound part and arms its output. Accepted units are
     * debited from the host, possibly including a partial packet. The part emits on its own tick;
     * this result describes buffer transfer, not energy already delivered to the part's neighbors.
     */
    public EnergyTransfer transferStoredToPart(String part, long packetSize, long packetCount) {
        if (EnergyTransfer.requestUnits(packetSize, packetCount) == 0)
            return new EnergyTransfer(packetSize, packetCount, 0);
        return new EnergyTransfer(packetSize, packetCount,
                context.moveUnitsToPart(part, packetSize, packetCount));
    }

    /**
     * Supplies externally accounted energy to a bound two-way part and arms its output.
     * This NEVER debits the host buffer. The caller must account for exactly the returned
     * {@link EnergyTransfer#transferredUnits()} in its own energy source, including any partial
     * packet. A missing part, wrong role or wrong energy type accepts zero. Use
     * {@link #transferStoredToPart} when the host buffer owns the energy.
     */
    public EnergyTransfer supplyPart(String part, String type, long packetSize, long packetCount) {
        if (EnergyTransfer.requestUnits(packetSize, packetCount) == 0)
            return new EnergyTransfer(packetSize, packetCount, 0);
        return new EnergyTransfer(packetSize, packetCount,
                context.pushToPart(part, type, packetSize, packetCount));
    }

    private static void requireUnits(long units) {
        if (units < 0) throw new IllegalArgumentException("Energy units must not be negative");
    }
}
