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

package com.gregtech.gregtech.api.energy;

import com.gregtech.gregtech.data.GregTechConstants;

/**
 * Static seam for the MC-free FE packet math between GT emitters/consumers and foreign
 * (non-GregTech) FE storages. Both faces adapt the platform {@code IEnergyStorage} (the
 * same six-method twin on both legs) with a two-argument lambda.
 *
 * <h2>The generalized outbound face (task p28-cut-eu-fe-bridge)</h2>
 *
 * {@link #pushPacketTrain} is the GENERALIZED outbound face: it pushes a train of whole FE
 * packets into a foreign storage and is RATIO-AGNOSTIC — the packet-train primitive carries
 * no unit conversion of its own, the ratio lives in the CALLER's registration constants
 * (for the Flux Dynamo the RU->FE 2.75 is expressed by its NBT_OUTPUT/NBT_INPUT ladder
 * computing the packet sizes; the bridge math never sees a ratio). It is the former EU->FE
 * outbound face (task p26-eu-bridge-outbound, cut by ruling decisions.p28-cut-eu-fe-bridge)
 * with the x4 EU conversion factored out: the simulate-the-whole-train / align-the-real-send
 * / divup bill / bind31 clamp family is kept verbatim from that face, so the packet unit,
 * the rounding alignment and the overflow guard stay pinned by the same unit tests that
 * covered the old face.
 *
 * <ul>
 * <li><b>Overflow</b>: aPacketCount*aPacketSizeFE is computed in the long domain and clamped
 *     into int range by {@link #bind31} (upstream UT.Code.bind31, UT.java:1564, called at
 *     EnergyCompat.java:212 for the old EU face) — top-tier ladders reach products where
 *     naive int math overflows (the research card risk ②). GTCEu saturatedCast
 *     (GTMath.java:125) is the same guard family.</li>
 * <li><b>Rounding alignment</b>: the simulated acceptance is rounded DOWN to a whole packet
 *     before the real send (GTCEu FeCompat.insertEu's {@code feSent - feSent % ratio} form,
 *     FeCompat.java:63-64, at the packet unit) — without it a partially-accepted packet
 *     would be billed as a full one via divup and the FE side would silently lose the
 *     remainder. The final bill stays {@code divup(sent, aPacketSizeFE)}
 *     (upstream EnergyCompat.java:212-213 form), which is exact when the send was
 *     packet-aligned.</li>
 * <li><b>No gate, no config</b>: the former outbound gate/config structure belonged to the
 *     EU dispatch and went with it — a pushPacketTrain caller has
 *     already resolved and adapted the receiver's FE storage before calling.</li>
 * <li><b>DECLARED DEVIATION (deviation ledger, inherited)</b>: no overcharge concept on the
 *     foreign side (upstream checkOverCharge destroyed foreign receivers above VMAX[3];
 *     cut following the GTCEu native-outbound precedent — modern FE has no voltage concept
 *     to violate). GT-side machines keep their own overcharge path (Root :494-509).</li>
 * </ul>
 *
 * <h2>The FE->EU inbound math (task p28-a-fe-inbound-math, design = research.p28-r-eu-inbound)</h2>
 *
 * The dual face: an FE storage is PULLED into whole GT packet trains via {@link #extractFe},
 * adapted from the platform {@code IEnergyStorage.extractEnergy} by {@link IFESource}. The
 * direction split follows the research ruling: GT energy is purely passive push
 * (doEnergyInjection — no GT machine ever pulls), FE is pull-first push-second, so the inbound
 * face is the pull math the converter machine (p28-b) drives per tick; the machine, its
 * buffers and its overload explosion live behind this seam, not in it. Every step mirrors the
 * {@link #pushPacketTrain} family — same whole-packet accounting unit, same {@link #bind31}
 * request clamp, same FeCompat floor alignment — plus the FE->EU rebuild ratio
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
	 * legs' {@code IEnergyStorage} (forge-1.20.1 IEnergyStorage.java:31-64 and neoforge 21.1.249
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
	 * this math only knows packets. The guard/shape family is the former EU->FE outbound face
	 * verbatim (EnergyCompat.java:141/147/212-213 + FeCompat.java:64), minus the x4.
	 *
	 * @param aStorage the receiver's FE storage (platform-adapted)
	 * @param aPacketSizeFE the FE size of ONE packet; may be negative (bridged by its
	 *        magnitude, the upstream :147 directional form)
	 * @param aPacketCount the packet count
	 * @return the amount of packets actually accepted (the divup bill, exact for an
	 *         aligned send)
	 */
	public static long pushPacketTrain(IFEReceiver aStorage, long aPacketSizeFE, long aPacketCount) {
		if (aStorage == null || aPacketCount <= 0 || aPacketSizeFE == 0 || aPacketSizeFE == Long.MIN_VALUE) return 0; // the :141 guard family
		long tPacketFe = Math.abs(aPacketSizeFE); // upstream EnergyCompat.java:147
		int tFeWanted = requestSize(aPacketCount, tPacketFe); // the long-domain product, clamped
		int tFeSimulated = aStorage.receiveEnergy(tFeWanted, true);
		if (tFeSimulated <= 0) return 0; // nothing accepts: the whole train is unused
		long tAligned = tFeSimulated - tFeSimulated % tPacketFe; // GTCEu FeCompat.java:64, packet-unit form
		int tFeSent = aStorage.receiveEnergy(bind31(tAligned), false);
		return tFeSent <= 0 ? 0 : tFeSent / tPacketFe + (tFeSent % tPacketFe == 0 ? 0 : 1); // the divup bill, exact for an aligned send
	}

	/**
	 * The pull face: extracts whole GT packets out of an FE storage — the
	 * research.p28-r-eu-inbound pull-side math (FE is pull-first push-second, GT purely
	 * passive push) the converter machine (p28-b) drives per tick. Every step mirrors the
	 * {@link #pushPacketTrain} family — same whole-packet accounting unit, same
	 * {@link #bind31} request clamp, same FeCompat floor alignment — with the two
	 * direction-mandated differences: the FE side is pulled through {@link IFESource} (an FE
	 * source never pushes), and the result is the EXACT whole-packet count (floor division),
	 * never divup — a pull over-bills nobody, and a misbehaving source returning a
	 * non-aligned remainder simply keeps that remainder in itself. No overcharge concept
	 * exists on this face.
	 *
	 * @param aStorage the foreign FE source (platform-adapted)
	 * @param aSize the EU packet size (voltage) to rebuild; may be negative (the :147 magnitude form)
	 * @param aAmount the maximum packet count to pull
	 * @return the amount of EU packets actually obtained (times aSize = the EU; times
	 *         aSize*RF_PER_EU = the FE taken)
	 */
	public static long extractFe(IFESource aStorage, long aSize, long aAmount) {
		if (aStorage == null || aAmount <= 0 || aSize == 0 || aSize == Long.MIN_VALUE) return 0; // the :141 guard family, pull-side
		long tSize = Math.abs(aSize); // upstream EnergyCompat.java:147
		if (tSize > Integer.MAX_VALUE / GregTechConstants.RF_PER_EU) return 0;
		long tPacketFe = tSize * GregTechConstants.RF_PER_EU; // one EU packet in FE (the inbound reconstruction unit)
		int tWanted = requestSize(aAmount, tPacketFe); // the long-domain request, clamped
		int tGot = aStorage.extractEnergy(tWanted, true);
		if (tGot <= 0) return 0; // nothing to pull: the whole train stays unused
		long tAligned = tGot - tGot % tPacketFe; // GTCEu FeCompat.java:64 form, floored to whole packets
		int tExtracted = aStorage.extractEnergy(bind31(tAligned), false);
		return tExtracted / tPacketFe; // exact whole packets; any misbehaving remainder stays in the source
	}
}
