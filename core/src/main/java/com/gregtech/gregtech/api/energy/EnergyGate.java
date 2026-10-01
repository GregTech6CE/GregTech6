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

import java.util.function.LongSupplier;

import com.gregtech.gregtech.data.GregTechTags;

/**
 * The pure part of the upstream TileEntityBase01Root energy gate (TileEntityBase01Root.java:716-717),
 * extracted as static functions so the mdk Root default block (D3) can delegate to it and the
 * math stays offline-testable in the root module.
 *
 * Semantics kept verbatim against upstream Root:716-717:
 * - aSize 0 or the wrong directionality (not accepting / not emitting) -> 0, nothing happens;
 * - GregTechTags.Energy.ALL_SIZE_IRRELEVANT types (TD.java:218, e.g. RF/Steam/Heat - NOT EU) skip the
 *   minimum packet size check entirely;
 * - injection: a packet below getEnergySizeInputMin returns aAmount (the offered energy counts
 *   as USED but doInject is never called - the energy is swallowed, Root:717);
 * - extraction: a packet below getEnergySizeOutputMin returns 0 (refused, Root:716).
 *
 * Not synchronized on purpose: this is the pure math. The mdk default block keeps upstream's
 * "synchronized" on the doEnergyInjection/doEnergyExtraction entry points that call into these.
 */
public final class EnergyGate {
	private EnergyGate() {}

	/** Upstream TileEntityBase01Root.java:717 (doEnergyInjection gate). */
	public static long gateInjection(GregTechTags.Tag aEnergyType, boolean aAccepting, long aSize, long aInputMin, long aAmount, LongSupplier aDoInject) {
		if (aSize == 0 || !aAccepting) return 0;
		if (GregTechTags.Energy.ALL_SIZE_IRRELEVANT.contains(aEnergyType) || Math.abs(aSize) >= aInputMin) return aDoInject.getAsLong();
		return aAmount; // packet too small: upstream swallows the offered amount without storing it
	}

	/** Upstream TileEntityBase01Root.java:716 (doEnergyExtraction gate). */
	public static long gateExtraction(GregTechTags.Tag aEnergyType, boolean aEmitting, long aSize, long aOutputMin, long aAmount, LongSupplier aDoExtract) {
		if (aSize == 0 || !aEmitting) return 0;
		if (GregTechTags.Energy.ALL_SIZE_IRRELEVANT.contains(aEnergyType) || Math.abs(aSize) >= aOutputMin) return aDoExtract.getAsLong();
		return 0; // packet too small: upstream refuses the extraction
	}
}
