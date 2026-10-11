package com.gregtech.gregtech.block.wood;

/**
 * GT6 tree species with tint colors and fireproof flag.
 *
 * <p>The first group is GT6's own nine trees ({@code BlockTreeSaplingAB} metas 0-7 plus
 * {@code BlockTreeSaplingCD} meta 0, {@code gregapi/data/MT.java} wood materials); the last three
 * are wood-dictionary species (Forestry's Pine/Ebony and "White Mahoe") that GT6 itself never
 * grows. Colours are GT6's {@code woodnormal} RGB values; {@link #color()} is currently unused
 * metadata (kept because the GT6 data is authoritative).
 */
public enum WoodSpecies {
    // ── GT6's own trees ──
    RUBBER("rubber", 0xB49600, false, "Rubber"),
    MAPLE("maple", 0xE0A860, false, "Maple"),
    RAINBOWOOD("rainbowood", 0xC840F5, true, "Rainbowood"),
    WILLOW("willow", 0x259600, false, "Willow"),
    BLUE_MAHOE("blue_mahoe", 0x0F67FE, false, "Blue Mahoe"),
    HAZEL("hazel", 0xE4AFAF, false, "Hazel"),
    CINNAMON("cinnamon", 0x41C0C0, false, "Cinnamon"),
    COCONUT("coconut", 0xFFAA00, false, "Coconut"),
    BLUE_SPRUCE("bluespruce", 0xD5D5D9, false, "Blue Spruce"),
    // ── wood-dictionary species (other mods' woods in GT6; no GT6 tree shape) ──
    PINE("pine", 0xBB974D, false, "Pine"),
    EBONY("ebony", 0x3A342E, false, "Ebony"),
    WHITE_MAHOE("white_mahoe", 0x7993A6, false, "White Mahoe");

    private final String id;
    private final int color;
    private final boolean fireproof;
    private final String enName;

    WoodSpecies(String id, int color, boolean fireproof, String en) {
        this.id = id;
        this.color = color;
        this.fireproof = fireproof;
        this.enName = en;
    }

    public String id() { return id; }
    public int color() { return color; }
    public boolean fireproof() { return fireproof; }
    public String enName() { return enName; }
    /** Compatibility only; localized names come from language resources. */
    @Deprecated
    public String zhName() { return enName; }
}
