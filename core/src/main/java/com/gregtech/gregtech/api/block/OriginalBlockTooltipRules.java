/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from gregapi.data.LH.getToolTipBlastResistance. */
package com.gregtech.gregtech.api.block;

/** Original tooltip thresholds; these labels do not change native explosion mechanics. */
public final class OriginalBlockTooltipRules {
    private OriginalBlockTooltipRules() {}
    public enum BlastRating {
        TERRIBLE("gt.lang.blast.resist.terrible"),
        GHAST("gt.lang.blast.resist.ghast.proof"),
        CREEPER("gt.lang.blast.resist.creeper.proof"),
        TNT("gt.lang.blast.resist.tnt.proof"),
        DYNAMITE("gt.lang.blast.resist.dynamite.proof"),
        IC2_NUKE_UNPROTECTED("gt.lang.blast.resist.nuke.not");
        private final String key;
        BlastRating(String key) { this.key = key; }
        public String key() { return key; }
    }
    public static BlastRating blastRating(double resistance, boolean hasIC2ExplosionCompat, boolean whitelisted) {
        if (resistance < 4) return BlastRating.TERRIBLE;
        if (resistance < 12) return BlastRating.GHAST;
        if (resistance < 16) return BlastRating.CREEPER;
        if (resistance <= 40) return BlastRating.TNT;
        if (resistance < 3330 || !hasIC2ExplosionCompat || whitelisted) return BlastRating.DYNAMITE;
        return BlastRating.IC2_NUKE_UNPROTECTED;
    }
    /** The port currently has no original IC2 explosion compatibility provider. */
    public static BlastRating blastRating(double resistance) { return blastRating(resistance, false, false); }
    /** Original LH truncates to one decimal; it does not round. */
    public static String blastNumber(double resistance) {
        return ((int) resistance) + "." + (((int) (resistance * 10)) % 10);
    }
}
