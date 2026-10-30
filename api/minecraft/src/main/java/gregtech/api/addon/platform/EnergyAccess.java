/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import gregtech.api.energy.EnergyTransfer;
import gregtech.api.energy.EnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * Energy buffers, emission and bound-part supply for one behavior machine.
 * Obtained through {@link MachineBehaviorContext#energy()} and valid only during the same
 * behavior callback as its context. Every operation executes immediately on the server.
 * <p>
 * Negative unit amounts, negative packet counts, zero-sized nonempty packets and overflowing
 * requests throw before the machine changes. Implementations must check these conditions before
 * changing machine state and must never report more transferred units than the request.
 * Emission methods return exact {@link EnergyTransfer} accounting built from the packets the host
 * actually accepted, so {@link EnergyTransfer#transferredUnits()} is authoritative and may include
 * a partial packet for a part buffer.
 */
public interface EnergyAccess {
    /**
     * Energy stored in the main input, in the machine's energy unit.
     * A machine that declares no main input returns 0. {@link #generateMain} and the emission
     * methods still move energy. The auxiliary buffer is {@link #auxiliaryStored(EnergyType)}.
     */
    long mainStored();

    /** Removes all requested units from the main input, or returns false without removing any. */
    boolean consumeMain(long units);

    /**
     * Adds as much of {@code units} as the main buffer can hold.
     * The buffer is the declared capacity, or the rated input maximum when that capacity is 0.
     * A machine with no main input holds one full output packet ({@code maxSize} times {@code maxAmount}).
     * Returns how much was stored.
     */
    long generateMain(long units);

    /** Main buffer cap. Zero means the machine still drains its rated input each tick. */
    long mainCapacity();

    /** Changes {@link #mainCapacity()} for later ticks and for the cap at the end of this tick. */
    void setMainCapacity(long units);

    /** Auxiliary buffer for {@code type}, in that energy's unit. An unknown type, or a machine with no auxiliary input, is 0. */
    long auxiliaryStored(EnergyType type);

    /** Removes all requested units from the auxiliary buffer of {@code type}, or returns false without removing any. */
    boolean consumeAuxiliary(EnergyType type, long units);

    /**
     * Pushes {@code packetCount} packets of {@code packetSize} out of the declared output faces.
     * Spends {@code abs(packetSize)} energy units for each packet a neighbor accepts.
     * The result contains whole packets only. A request above the declared maximum throws.
     */
    EnergyTransfer emitFromFaces(long packetSize, long packetCount);

    /**
     * Sends {@code packetCount} packets of {@code packetSize} to the block entity at {@code target},
     * entering it through {@code side}. It uses the declared output type and packet limits and
     * spends {@code abs(packetSize)} stored units for each packet the target accepts.
     * The declared output faces are not used. A null {@code side} means no side.
     * A target in an unloaded chunk, without a block entity, or with no energy receiver accepts
     * nothing. A request above the declared maximum throws.
     */
    EnergyTransfer emitTo(BlockPos target, @Nullable Direction side, long packetSize, long packetCount);

    /**
     * Moves up to {@code packetCount} packets of {@code packetSize} from this machine's main
     * buffer into the named bound part and arms it. The part emits on its own tick.
     * This debits the main buffer for every accepted unit, including a partial packet.
     * It does not require the machine to declare output faces, but a machine whose stored energy
     * has a declared type only feeds a matching part. A request the part's output declaration
     * rejects throws. The returned units describe the buffer transfer, not energy already
     * delivered to the part's neighbors.
     */
    EnergyTransfer transferToPart(String part, long packetSize, long packetCount);

    /**
     * Supplies externally accounted energy to a bound two-way part and arms its output.
     * This NEVER debits this machine's buffers: the caller keeps its own accounting and must
     * debit exactly {@link EnergyTransfer#transferredUnits()} from its source, including any
     * partial packet. A missing part, wrong role or wrong energy type accepts zero.
     * A request the part's output declaration rejects throws.
     */
    EnergyTransfer supplyPart(String part, EnergyType type, long packetSize, long packetCount);
}
