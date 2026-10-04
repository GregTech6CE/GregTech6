/**
 * Copyright (c) 2026 GregTech-6 Team
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

package com.gregtech.gregtech.api.machine.crucible;
/** MT.setAloy/uumAloy/alloySimple recipe declarations, not all ALLOY chemistry. */
public final class OriginalCrucibleCompositions {
    private OriginalCrucibleCompositions() {}
    private static final java.util.Set<Integer> IDS=java.util.Set.of(8003,8600,8601,8602,8603,8604,8605,8610,8611,8612,8613,8614,8615,8620,8621,8631,8632,8633,8635,8636,8638,8640,8643,8653,8654,8657,8658,8659,8660,8661,8662,8663,8664,8665,8666,8667,8668,8669,8671,8672,8682,8684,8686,8689,8690,8691,8692,8700,8702,8703,8704,8705,8706,8708,8709,8710,8711,8725,8726,8727,8728,8729,8730,8731,8732,8733,8734,8737,8745,8750,8751,8752,8777,8778,8779,8781,8782,8783,8790,8793,8794,8796,8797,8798,8802,8806,8807,8808,8809,8810,8811);
    public static boolean hasRecipe(int materialId){return IDS.contains(materialId);}
}
