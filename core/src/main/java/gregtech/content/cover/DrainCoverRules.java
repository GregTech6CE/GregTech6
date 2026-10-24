/**
 * Copyright (c) 2021 GregTech-6 Team
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

/* Adapted from Gregorius Techneticies' CoverDrain, LGPL-3.0-or-later. */
package gregtech.content.cover;

/** Original world-water conversion and rain yield, independent of native fluid identities. */
public final class DrainCoverRules {
    private DrainCoverRules() {}

    /** CoverDrain:136-143: river becomes ordinary water, swamp becomes dirty water. */
    public static String bulkWaterField(String worldFluid) {
        if (worldFluid == null) return null;
        return switch (worldFluid) {
            case "riverwater" -> "Water";
            case "seawater" -> "Ocean";
            case "swampwater" -> "Dirty_Water";
            default -> null;
        };
    }

    public static long rainAmount(float downfall, float temperature, boolean thundering) {
        if (downfall <= 0 || temperature < 0.2F) return 0;
        return Math.max(1L, (long) (downfall * 10000)) * (thundering ? 2 : 1);
    }
}
