/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from ITileEntityMultiBlockController.Util, MultiBlockBase and UT.Code.makeString. */
package com.gregtech.gregtech.api.multiblock;

public final class MultiblockToolRules {
    private MultiblockToolRules() {}
    public static final long BUILDER_COST = 10, MAGNIFIER_COST = 1;
    public static final String FORMED = "Structure is formed already!";
    public static final String JUST_FORMED = "Structure did form just now!";
    public static final String INCOMPLETE = "Structure did not form!";
    public static final String NO_CONTROLLER = "There is no Multiblock Controller for this Block.";

    /** The source checks each axis, not Manhattan distance or the wand material quality. */
    public static boolean inBuilderReach(int dx, int dy, int dz) {
        return dx >= -1 && dx <= 1 && dy >= -1 && dy <= 1 && dz >= -1 && dz <= 1;
    }

    public static String structureMessage(boolean wasFormed, boolean nowFormed) {
        return !nowFormed ? INCOMPLETE : wasFormed ? FORMED : JUST_FORMED;
    }

    /** Original decimal grouping starts at 10,000, using underscores rather than locale separators. */
    public static String amount(long amount) {
        String value = Long.toString(amount);
        if (amount > -10000 && amount < 10000) return value;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            result.append(value.charAt(i));
            int remaining = value.length() - i - 1;
            if (value.charAt(i) != '-' && remaining > 0 && remaining % 3 == 0) result.append('_');
        }
        return result.toString();
    }
}
