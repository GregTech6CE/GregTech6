/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from gregapi.data.LH.getToolTipBlastResistance/getToolTipHarvest. */
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
    /** MultiTileEntityItemInternal suppresses the blast row below4. */
    public static boolean showMultitileBlast(double resistance) { return resistance >= 4; }
    /** Original LH truncates to one decimal; it does not round. */
    public static String blastNumber(double resistance) {
        return ((int) resistance) + "." + (((int) (resistance * 10)) % 10);
    }
    /** Original LH prints tier details only above1 (its case1 arm is unreachable). */
    public static boolean showHarvestLevel(int level) { return level > 1; }
    public static String harvestToolKey(String tool) {
        return switch (tool.toLowerCase(java.util.Locale.ROOT)) {
            case "pickaxe" -> "gt.lang.tool.name.pickaxe";
            case "axe" -> "gt.lang.tool.name.axe";
            case "shovel" -> "gt.lang.tool.name.shovel";
            case "sword" -> "gt.lang.tool.name.sword";
            case "wrench" -> "gt.lang.tool.name.wrench";
            case "crowbar" -> "gt.lang.tool.name.crowbar";
            case "cutter" -> "gt.lang.tool.name.cutter";
            case "shears" -> "gt.lang.tool.name.shears";
            default -> null;
        };
    }
    public static String harvestTierMaterial(int level) {
        return switch (level) {
            case 2 -> "iron";
            case 3 -> "diamond";
            case 4 -> "netherite";
            case 5 -> "adamantium";
            default -> level > 14 ? "infinity" : null;
        };
    }
}
