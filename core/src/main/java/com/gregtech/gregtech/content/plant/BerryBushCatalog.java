package com.gregtech.gregtech.content.plant;





import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GT6's berry bush colour table ({@code gregapi.data.CS.BushesGT}) — one entry per berry that grows
 * on a GT6 bush, with GT6's own four colours {@code {bush, bloom, immature, berry}}:
 * {@code MultiItemFood} line-for-line ({@code BushesGT.put(IL.Food_Blueberry.get(1), 0x22ff22, ...)}).
 *
 * <p>The colours tint GT6's greyscale bush textures per stage — stage 0 shows only the bush,
 * stage 1 the immature berries ({@code bloom}), stage 2 the berries in {@code immature} and
 * stage 3 the ripe berries in {@code berry}. A bush without a berry renders in GT6's placeholder
 * magenta ({@code 0xff00ff}), and an unknown berry falls back to GT6's {@code DEFAULT} entry.
 */
public class BerryBushCatalog {
    protected BerryBushCatalog() {}

    /** GT6 {@code BushesGT} colours of one berry type. */
    public record BerryType(String id, int bush, int bloom, int immature, int berry) {}

    /** GT6's registration order — the worldgen noise picks its index in exactly this list. */
    private static final Map<String, BerryType> TYPES = new LinkedHashMap<>();

    /** GT6 {@code BushesGT.DEFAULT} (the string/cotton entry). */
    public static final BerryType DEFAULT = new BerryType("minecraft:string", 0x22cc22, 0x33cc33, 0x44cc44, 0xeeeeee);
    /** GT6 renders a bush that has no berry yet in this colour. */
    public static final int NO_BERRY_COLOUR = 0xff00ff;

    static {
        entry("blueberry", 0x22ff22, 0xffcccc, 0x6666dd, 0x0000ff);
        entry("candleberry", 0x44ff44, 0xccffcc, 0xaaffaa, 0xccffcc);
        entry("cranberry", 0x00dd00, 0xffcccc, 0x66ff66, 0xff0000);
        entry("black_currants", 0x33ff33, 0xaaaaaa, 0x66ff66, 0x111111);
        entry("white_currants", 0x33ff33, 0xaaaaaa, 0x66ff66, 0xeeeedd);
        entry("red_currants", 0x33ff33, 0xaaaaaa, 0x66ff66, 0xee0000);
        entry("blackberry", 0x11ff11, 0xffcccc, 0x663333, 0x331111);
        entry("raspberry", 0x11ff11, 0xffcccc, 0x664444, 0xffaaaa);
    }

    private static void entry(String id, int bush, int bloom, int immature, int berry) {
        TYPES.put(id, new BerryType(id, bush, bloom, immature, berry));
    }

    /** The berry types in GT6's order (the worldgen noise indexes into this list). */
    public static java.util.List<BerryType> types() {
        return java.util.List.copyOf(TYPES.values());
    }

    public static int size() { return TYPES.size(); }

    /** Stable block identity; cotton is the original string-producing default bush. */
    public static String blockPath(String id) {
        if (id == null || id.isEmpty()) return "bush";
        if (id.startsWith("gregtech:")) id = id.substring("gregtech:".length());
        if (id.startsWith("plant_gt_berry_")) return "bush_" + id;
        BerryType type = byId(id);
        return type == null ? null : type == DEFAULT ? "bush_cotton" : "bush_" + type.id();
    }

    public static BerryType byIndex(int index) {
        var list = types();
        return list.get(Math.floorMod(index, list.size()));
    }

    public static BerryType byId(String id) {
        if ("minecraft:string".equals(id) || "string".equals(id) || "default".equals(id)) return DEFAULT;
        return TYPES.get(id);
    }

    /** CS.BushesGT also registers string as its default cotton bush. */
    public static java.util.List<BerryType> worldgenTypes() {
        var entries = new java.util.ArrayList<BerryType>();
        entries.add(DEFAULT);
        entries.addAll(types());
        return java.util.List.copyOf(entries);
    }

    public static int worldgenSize() { return TYPES.size() + 1; }

    public static BerryType worldgenByIndex(int index) {
        return worldgenTypes().get(Math.floorMod(index, worldgenSize()));
    }

    /** The tint colour of a stage's render layer, exactly as GT6's {@code getRenderPasses2} picks it. */
    public static int stageColour(BerryType type, int stage) {
        BerryType resolved = type == null ? DEFAULT : type;
        return switch (stage) {
            case 1 -> resolved.bloom();
            case 2 -> resolved.immature();
            case 3 -> resolved.berry();
            default -> resolved.bush();
        };
    }

    /** Source inventory pass: food/cotton show immature berries, material berries show solid RGB. */
    public static int inventoryColour(BerryType type) {
        if (type == null) return NO_BERRY_COLOUR;
        return type.id().startsWith("gregtech:plant_gt_berry_") ? type.berry() : type.immature();
    }
}
