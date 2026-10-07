/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntityTank3x3x3/5x5x5, MultiTileEntityTank and UT.Code.makeString. */
package com.gregtech.gregtech.content.multiblock;

import java.util.List;

public final class OriginalTankTooltipData {
    private OriginalTankTooltipData() {}
    public static List<String> structureKeys(int size) {
        return switch(size) {
            case 3 -> List.of("gt.tooltip.multiblock.tank3x3x3.1", "gt.tooltip.multiblock.tank3x3x3.2",
                    "gt.tooltip.multiblock.tank3x3x3.3");
            case 5 -> List.of("gt.tooltip.multiblock.tank5x5x5.1", "gt.tooltip.multiblock.tank5x5x5.2",
                    "gt.tooltip.multiblock.tank5x5x5.3");
            default -> throw new IllegalArgumentException("Original tank size: " + size);
        };
    }
    /** Original decimal groups use underscores only when the number has at least five digits. */
    public static String formatNumber(long value) {
        if(value > -10000 && value < 10000) return Long.toString(value);
        String digits=Long.toString(value);
        int first=value < 0 ? 1 : 0;
        var result=new StringBuilder();
        for(int i=0; i<digits.length(); i++) {
            if(i > first && (digits.length()-i)%3==0) result.append('_');
            result.append(digits.charAt(i));
        }
        return result.toString();
    }
}
