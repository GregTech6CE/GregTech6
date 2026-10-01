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
    RUBBER("rubber", 0xB49600, false, "Rubber", "橡胶"),
    MAPLE("maple", 0xE0A860, false, "Maple", "枫"),
    RAINBOWOOD("rainbowood", 0xC840F5, true, "Rainbowood", "彩虹木"),
    WILLOW("willow", 0x259600, false, "Willow", "柳"),
    BLUE_MAHOE("blue_mahoe", 0x0F67FE, false, "Blue Mahoe", "蓝桃花心"),
    HAZEL("hazel", 0xE4AFAF, false, "Hazel", "榛"),
    CINNAMON("cinnamon", 0x41C0C0, false, "Cinnamon", "桂皮"),
    COCONUT("coconut", 0xFFAA00, false, "Coconut", "椰"),
    BLUE_SPRUCE("bluespruce", 0xD5D5D9, false, "Blue Spruce", "蓝云杉"),
    // ── wood-dictionary species (other mods' woods in GT6; no GT6 tree shape) ──
    PINE("pine", 0xBB974D, false, "Pine", "松"),
    EBONY("ebony", 0x3A342E, false, "Ebony", "黑檀"),
    WHITE_MAHOE("white_mahoe", 0x7993A6, false, "White Mahoe", "白桃花心");

    private final String id;
    private final int color;
    private final boolean fireproof;
    private final String enName;
    private final String zhName;

    WoodSpecies(String id, int color, boolean fireproof, String en, String zh) {
        this.id = id;
        this.color = color;
        this.fireproof = fireproof;
        this.enName = en;
        this.zhName = zh;
    }

    public String id() { return id; }
    public int color() { return color; }
    public boolean fireproof() { return fireproof; }
    public String enName() { return enName; }
    public String zhName() { return zhName; }
}
