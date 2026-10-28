/**
 * Ported from GregTech 6 (1.7.10), task p7-d1-energy-core (ADR 2026-08-31-p7-energy-network).
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */
package gregtech.engine.energy;

import gregtech.content.data.GregTechConstants;

/**
 * Static seam for the MC-free FE packet math between GT emitters/consumers and foreign
 * (non-GregTech) FE storages. Both faces adapt the platform {@code IEnergyStorage} (the
 * same six-method twin on both legs) with a two-argument lambda.
 *
 * <h2>The generalized outbound face</h2>
 *
 * {@link #pushPacketTrain} is the general outbound face: it pushes a train of whole FE packets
 * into a foreign storage and is ratio-agnostic — the primitive carries no unit conversion, the
 * ratio lives in the caller's registration constants (for the Flux Dynamo the RU->FE 2.75 is
 * expressed by its NBT_OUTPUT/NBT_INPUT ladder computing the packet sizes; the bridge math
 * never sees a ratio). The simulate-the-whole-train / align-the-real-send / divup bill /
 * bind31 clamp family is the one the EU&#8594;FE face used, so the packet unit, the rounding
 * alignment and the overflow guard are covered by the same unit tests.
 *
 * <ul>
 * <li><b>Overflow</b>: aPacketCount*aPacketSizeFE is computed in the long domain and clamped
 *     into int range by {@link #bind31} (upstream {@code UT.Code.bind31}, UT.java:1564) —
 *     top-tier ladders reach products where naive int math overflows. GTCEu's
 *     {@code GTMath.saturatedCast} is the same guard family.</li>
 * <li><b>Rounding alignment</b>: the simulated acceptance is rounded DOWN to a whole packet
 *     before the real send (GTCEu {@code FeCompat.insertEu}'s {@code feSent - feSent % ratio}
 *     form, at the packet unit) — without it a partially-accepted packet would be billed as a
 *     full one via divup and the FE side would silently lose the remainder. The final bill
 *     stays {@code divup(sent, aPacketSizeFE)} (the platform {@code EnergyCompat} form), which
 *     is exact when the send was packet-aligned.</li>
 * <li><b>No gate, no config</b>: this seam has no gate or config structure of its own; a
 *     pushPacketTrain caller has already resolved and adapted the receiver's FE storage before
 *     calling.</li>
 * <li><b>No overcharge on the foreign side</b>: FE has no voltage concept to violate, so
 *     foreign receivers are never destroyed (GT6's {@code checkOverCharge} dropped them above
 *     VMAX[3]). GT machines keep their own overcharge path.</li>
 * </ul>
 *
 * <h2>The FE&#8594;EU inbound math</h2>
 *
 * The inbound face: an FE storage is PULLED into whole GT packet trains via {@link #extractFe},
 * adapted from the platform {@code IEnergyStorage.extractEnergy} by {@link IFESource}. The
 * direction split: GT energy is a passive push
 * (doEnergyInjection — no GT machine ever pulls), FE is pull-first push-second, so the inbound
 * face is the pull math the converter machine drives per tick; the machine, its
 * buffers and its overload explosion live behind this seam, not in it. Every step mirrors the
 * {@link #pushPacketTrain} family — same whole-packet accounting unit, same {@link #bind31}
 * request clamp, same FeCompat floor alignment — plus the FE&#8594;EU rebuild ratio
 * ({@link GregTechConstants#RF_PER_EU}, which belongs to the INBOUND packet reconstruction only). The
 * one direction-mandated asymmetry is the bill: a pull over-bills nobody, so the result is
 * the exact whole-packet count (floor division), never divup, and there is no overcharge
 * concept on this face.
 */
public final class EnergyBridge {
    private EnergyBridge() {}

    private static int requestSize(long count, long packet) {
        return count > Integer.MAX_VALUE / packet ? Integer.MAX_VALUE : (int) (count * packet);
    }

    /**
     * The MC-free FE face of the outbound math: the only platform method the push needs. Both
     * legs' {@code IEnergyStorage} (forge-1.20.1 IEnergyStorage.java and neoforge 21.1.249
     * IEnergyStorage, javap-verified six-method twins) adapt with a two-argument lambda.
     */
    public interface IFEReceiver {
        /** Same contract as the platform method: energy accepted (or would be, simulated). */
        int receiveEnergy(int aAmount, boolean aSimulate);
    }

    /**
     * The MC-free FE face of the INBOUND math: the platform dual of {@link IFEReceiver}.
     * Both legs' {@code IEnergyStorage} adapt with the same two-argument lambda shape
     * ({@code storage::extractEnergy}).
     */
    public interface IFESource {
        /** Same contract as the platform method: energy extracted (or would be, simulated). */
        int extractEnergy(int aAmount, boolean aSimulate);
    }

    /** Upstream UT.Code.bind31 (UT.java:1564 verbatim): clamp [0, Integer.MAX_VALUE] and narrow. */
    public static int bind31(long aBoundValue) {
        return (int) Math.max(0, Math.min(2147483647, aBoundValue));
    }

    /**
     * The generalized outbound face: pushes a train of whole FE packets into a foreign FE
     * storage. Ratio-agnostic — the caller computes the packet size from its own registration
     * constants (the Flux Dynamo's NBT_OUTPUT/NBT_INPUT ladder, a future face's own ladder);
     * this math only knows packets. The guard and shape follow the platform
     * {@code EnergyCompat} and GTCEu's {@code FeCompat}, minus the x4.
     *
     * @param aStorage the receiver's FE storage (platform-adapted)
     * @param aPacketSizeFE the FE size of ONE packet; may be negative (bridged by its
     *        magnitude, the upstream directional form)
     * @param aPacketCount the packet count
     * @return the amount of packets actually accepted (the divup bill, exact for an
     *         aligned send)
     */
    public static long pushPacketTrain(IFEReceiver aStorage, long aPacketSizeFE, long aPacketCount) {
        if (aStorage == null || aPacketCount <= 0 || aPacketSizeFE == 0 || aPacketSizeFE == Long.MIN_VALUE)
            return 0; // the guard family
        long tPacketFe = Math.abs(aPacketSizeFE); // the platform EnergyCompat's magnitude form
        int tFeWanted = requestSize(aPacketCount, tPacketFe); // the long-domain product, clamped
        int tFeSimulated = aStorage.receiveEnergy(tFeWanted, true);
        if (tFeSimulated <= 0) return 0; // nothing accepts: the whole train is unused
        long tAligned = tFeSimulated - tFeSimulated % tPacketFe; // GTCEu FeCompat's packet-unit form
        int tFeSent = aStorage.receiveEnergy(bind31(tAligned), false);
        return tFeSent <= 0
                ? 0
                : tFeSent / tPacketFe + (tFeSent % tPacketFe == 0 ? 0 : 1); // the divup bill, exact for an aligned send
    }

    /**
     * The pull face: extracts whole GT packets out of an FE storage — the pull-side math (FE
     * is pull-first push-second, GT purely passive push) the converter machine drives per tick.
     * Every step mirrors the
     * {@link #pushPacketTrain} family — same whole-packet accounting unit, same
     * {@link #bind31} request clamp, same FeCompat floor alignment — with the two
     * direction-mandated differences: the FE side is pulled through {@link IFESource} (an FE
     * source never pushes), and the result is the EXACT whole-packet count (floor division),
     * never divup — a pull over-bills nobody, and a misbehaving source returning a
     * non-aligned remainder simply keeps that remainder in itself. No overcharge concept
     * exists on this face.
     *
     * @param aStorage the foreign FE source (platform-adapted)
     * @param aSize the EU packet size (voltage) to rebuild; may be negative (the magnitude form)
     * @param aAmount the maximum packet count to pull
     * @return the amount of EU packets actually obtained (times aSize = the EU; times
     *         aSize*RF_PER_EU = the FE taken)
     */
    public static long extractFe(IFESource aStorage, long aSize, long aAmount) {
        if (aStorage == null || aAmount <= 0 || aSize == 0 || aSize == Long.MIN_VALUE)
            return 0; // the guard family, pull-side
        long tSize = Math.abs(aSize); // the platform EnergyCompat's magnitude form
        if (tSize > Integer.MAX_VALUE / GregTechConstants.RF_PER_EU) return 0;
        long tPacketFe = tSize * GregTechConstants.RF_PER_EU; // one EU packet in FE (the inbound reconstruction unit)
        int tWanted = requestSize(aAmount, tPacketFe); // the long-domain request, clamped
        int tGot = aStorage.extractEnergy(tWanted, true);
        if (tGot <= 0) return 0; // nothing to pull: the whole train stays unused
        long tAligned = tGot - tGot % tPacketFe; // GTCEu FeCompat's form, floored to whole packets
        int tExtracted = aStorage.extractEnergy(bind31(tAligned), false);
        return tExtracted / tPacketFe; // exact whole packets; any misbehaving remainder stays in the source
    }
}
