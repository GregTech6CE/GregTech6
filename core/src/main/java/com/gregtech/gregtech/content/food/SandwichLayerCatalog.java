package com.gregtech.gregtech.content.food;


import java.util.HashMap;
import java.util.Map;

/**
 * The GT6 sandwich's ingredient display table, separated from block registration and rendering.
 * The numeric IDs, model footprints and heights come from GT6's {@code CS.Sandwiches} and
 * {@code Loader_Others}; the names are the port's registry names for those same food items.
 */
public final class SandwichLayerCatalog {
    public record Layer(int id,String texture,int footprint,int thickness,int tint){}

    private static final Layer[] LAYERS = new Layer[256];
    private static final Map<String, Integer> ITEMS = new HashMap<>();

    private SandwichLayerCatalog() {}

    private static void layer(int id, String texture, int footprint, int thickness, String... items) {
        LAYERS[id] = new Layer(id, texture, footprint, thickness, 0xFFFFFF);
        for (String item : items) register("gregtech:"+item,id);
    }

    /** Other mods can register an equivalent food without coupling themselves to a GT6 class. */
    public static void register(String item, int layerId) {
        if (item == null || layerId < 0 || layerId > 255 || LAYERS[layerId] == null)
            throw new IllegalArgumentException("Unknown sandwich layer: " + layerId);
        ITEMS.put(item, layerId);
    }

    public static Layer of(int id) {
        return id >= 0 && id < LAYERS.length ? LAYERS[id] : null;
    }

    public static Layer forId(String item){Integer id=ITEMS.get(item);return id==null?null:LAYERS[id];}

    static {
        // GT6 CS.DYES uses black=0 through white=15. Condiments and spices are
        // one-pixel white-mask overlays tinted with those exact original RGB values.
        int[] dyes = {0x202020, 0xFF0000, 0x00FF00, 0x604000, 0x0000FF, 0x800080,
                0x00FFFF, 0xC0C0C0, 0x808080, 0xFFC0C0, 0x80FF80, 0xFFFF00,
                0x8080FF, 0xFF00FF, 0xFF8000, 0xFFFFFF};
        for (int i = 0; i < 16; i++) {
            LAYERS[i] = new Layer(i, "condiment", 252, 1, dyes[i]);
            LAYERS[200 + i] = new Layer(200 + i, "spice", 252, 1, dyes[i]);
        }
        layer(16, "ananas", 2, 1, "ananas_slice");
        layer(17, "banana", 14, 1, "banana_slice");
        layer(18, "carrot", 14, 1, "carrot_slice");
        layer(19, "cheese", 1, 1, "cheese_slice");
        layer(20, "chili", 2, 2, "chili_pepper");
        layer(21, "chocolate", 3, 2, "chocolate_bar");
        layer(22, "comb", 1, 1, "honey_comb", "water_comb", "jungle_comb", "shroomy_comb", "royal_comb");
        layer(23, "cucumber", 14, 1, "cucumber_slice");
        layer(24, "fish_raw", 1, 2, "raw_fish_bar");
        layer(25, "fish", 1, 2, "cooked_fish_bar");
        layer(26, "ham_raw", 2, 1, "raw_ham_slice");
        layer(27, "ham", 2, 1, "cooked_ham_slice");
        layer(28, "lemon", 14, 1, "lemon_slice");
        layer(29, "meat_raw", 1, 2, "raw_meat_bar", "scrap_meat");
        layer(30, "meat", 1, 2, "cooked_meat_bar");
        layer(31, "onion", 14, 1, "onion_slice");
        layer(32, "ripeye_raw", 1, 3, "raw_rib_eye_steak");
        layer(33, "ripeye", 1, 3, "grilled_rib_eye_steak");
        layer(34, "soylent", 1, 2, "emerald_green_bar");
        layer(35, "tofu", 1, 2, "tofu_bar");
        layer(36, "tomato", 14, 1, "tomato_slice");
        layer(37, "bacon_raw", 1, 1, "raw_bacon");
        layer(38, "bacon", 1, 1, "grilled_bacon");
        layer(39, "chum", 1, 2, "chum");
        layer(40, "rainbow", 252, 1);
        layer(41, "pickle", 14, 1, "pickle_slice");
        layer(42, "pill", 14, 1, "empty_wax_pill", "radaway", "peppermint", "blue_pill", "red_pill", "antidote", "cure_all");
        layer(43, "egg_fried", 2, 1, "fried_egg");
        layer(44, "egg_sliced", 14, 1, "sliced_egg");
        layer(253, "toasted", 253, 2, "toasted_toast");
        layer(254, "toast", 254, 2, "toast");

        // Vanilla equivalents explicitly present in GT6 MultiItemFood and ore-dictionary listeners.
        ITEMS.put("minecraft:beef", 29);
        ITEMS.put("minecraft:cooked_beef", 30);
        ITEMS.put("minecraft:chicken", 29);
        ITEMS.put("minecraft:cooked_chicken", 30);
        ITEMS.put("minecraft:porkchop", 29);
        ITEMS.put("minecraft:cooked_porkchop", 30);
        ITEMS.put("minecraft:rotten_flesh", 29);
        ITEMS.put("minecraft:cod", 24);
        ITEMS.put("minecraft:cooked_cod", 25);
        ITEMS.put("minecraft:salmon", 24);
        ITEMS.put("minecraft:cooked_salmon", 25);
        register("gregtech:mayo", 15);
        register("gregtech:dressing", 15);
        register("gregtech:tomato_ketchup", 1);
        register("gregtech:butter", 11);
        register("gregtech:salted_butter", 11);
        register("gregtech:chili_sauce", 1);
        register("gregtech:hot_sauce", 1);
        register("gregtech:barbecue_sauce", 3);
        register("gregtech:olive_oil", 2);
        register("gregtech:egg_yolk", 211);
        register("gregtech:egg_white", 215);
        register("gregtech:scrambled_egg", 211);
        // Source rows override historical guessed aliases, including sauce and pill colors.
        for (var row : SandwichSourceIngredients.ROWS) register(row.item(), row.layer());
    }
}
