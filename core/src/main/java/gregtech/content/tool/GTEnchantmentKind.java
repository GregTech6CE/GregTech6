package gregtech.content.tool;

/** The four enchantments registered by GT6, independent of Minecraft's registry format. */
public enum GTEnchantmentKind {
    DISJUNCTION("disjunction"),
    DISSOLVING("dissolving"),
    WEREBANE("werebane"),
    RADIOACTIVITY("radioactivity");

    private final String path;

    GTEnchantmentKind(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public boolean obtainable() {
        return this != RADIOACTIVITY;
    }

    public int amplifier(int level) {
        return (int) Math.max(1, Math.min(5, (this == WEREBANE ? 10L : 5L) * level / 7));
    }

    public int duration(int level) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, 200L * level));
    }
}
