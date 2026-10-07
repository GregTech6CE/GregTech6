/* Canvas rules adapted from GregTech-6 Team, LGPL-3.0-or-later.
 * MultiItemRandomTools:443-447; RecipeMapScannerVisuals:101-114,184-194;
 * RecipeMapPrinter:86-97; CoverTextureCanvas. */
package com.gregtech.gregtech.content.cover;

import com.gregtech.gregtech.content.tool.PaintingRules;
import java.util.*;

/** Stable image identities and original dye order, independent of loader and registry indices. */
public final class CanvasRules {
    private CanvasRules() {}
    public static final String BLOCK = "gt.canvas.block", PROPERTIES = "gt.canvas.properties";
    public static final long BLOCK_SCAN_TICKS = 512, CANVAS_SCAN_TICKS = 64, PRINT_TICKS = 64, POWER = 16;
    public static final int PRINT_DYE_AMOUNT = 16;
    public record Variant(int originalId, String path, String dye, String name, int rgb) {}
    public static final List<Variant> VARIANTS = PaintingRules.DYES.stream().map(dye -> new Variant(
            7030 + dye.index(), "canvas_" + dye.id(), dye.id(), display(dye.id()) + " Canvas", dye.rgb())).toList();
    private static String display(String id) {
        var words = new ArrayList<String>();
        for (String word : id.split("_")) words.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
        return String.join(" ", words);
    }
    public record Image(String block, Map<String,String> properties) {
        public Image { properties = Collections.unmodifiableMap(new TreeMap<>(properties)); }
    }
    /** Missing registry names remain invalid; runtime numeric IDs are never persisted as identities. */
    public static Image image(String block, Map<String,String> properties) {
        if (block == null || !block.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || block.equals("minecraft:air")) return null;
        return new Image(block, properties == null ? Map.of() : properties);
    }
    public static List<String> printingDyes() { return List.of("Black", "Cyan", "Magenta", "Yellow"); }
}
