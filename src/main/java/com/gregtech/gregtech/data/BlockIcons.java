package com.gregtech.gregtech.data;

import com.gregtech.gregtech.GregTech;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Block / machine icon paths from GT6 BI.java. Auto-transpiled skeleton. */
public class BlockIcons {
    protected BlockIcons() {}

    public record IconRef(ResourceLocation texture) {}

    private static final Map<String, IconRef> REGISTRY = new LinkedHashMap<>();

    private static IconRef icon(String field, String path) {
        IconRef ref = new IconRef(GregTech.id(path));
        REGISTRY.put(field, ref);
        return ref;
    }

    public static final IconRef
            CHAR_0 = icon("CHAR_0", "textures/block/char_0"),
            CHAR_1 = icon("CHAR_1", "textures/block/char_1"),
            CHAR_2 = icon("CHAR_2", "textures/block/char_2"),
            CHAR_3 = icon("CHAR_3", "textures/block/char_3"),
            CHAR_4 = icon("CHAR_4", "textures/block/char_4"),
            CHAR_5 = icon("CHAR_5", "textures/block/char_5"),
            CHAR_6 = icon("CHAR_6", "textures/block/char_6"),
            CHAR_7 = icon("CHAR_7", "textures/block/char_7"),
            CHAR_8 = icon("CHAR_8", "textures/block/char_8"),
            CHAR_9 = icon("CHAR_9", "textures/block/char_9"),
            CHAR_A = icon("CHAR_A", "textures/block/char_a"),
            CHAR_B = icon("CHAR_B", "textures/block/char_b"),
            CHAR_C = icon("CHAR_C", "textures/block/char_c"),
            CHAR_D = icon("CHAR_D", "textures/block/char_d"),
            CHAR_E = icon("CHAR_E", "textures/block/char_e"),
            CHAR_F = icon("CHAR_F", "textures/block/char_f"),
            CHAR_HEX = icon("CHAR_HEX", "textures/block/char_hex"),
            CHAR_HEX_0 = icon("CHAR_HEX_0", "textures/block/char_hex_0"),
            CHAR_HEX_1 = icon("CHAR_HEX_1", "textures/block/char_hex_1"),
            CHAR_HEX_2 = icon("CHAR_HEX_2", "textures/block/char_hex_2"),
            CHAR_HEX_3 = icon("CHAR_HEX_3", "textures/block/char_hex_3"),
            CHAR_HEX_4 = icon("CHAR_HEX_4", "textures/block/char_hex_4"),
            CHAR_HEX_5 = icon("CHAR_HEX_5", "textures/block/char_hex_5"),
            CHAR_HEX_6 = icon("CHAR_HEX_6", "textures/block/char_hex_6"),
            CHAR_HEX_7 = icon("CHAR_HEX_7", "textures/block/char_hex_7"),
            CHAR_HEX_8 = icon("CHAR_HEX_8", "textures/block/char_hex_8"),
            CHAR_HEX_9 = icon("CHAR_HEX_9", "textures/block/char_hex_9"),
            CHAR_HEX_A = icon("CHAR_HEX_A", "textures/block/char_hex_a"),
            CHAR_HEX_B = icon("CHAR_HEX_B", "textures/block/char_hex_b"),
            CHAR_HEX_C = icon("CHAR_HEX_C", "textures/block/char_hex_c"),
            CHAR_HEX_D = icon("CHAR_HEX_D", "textures/block/char_hex_d"),
            CHAR_HEX_E = icon("CHAR_HEX_E", "textures/block/char_hex_e"),
            CHAR_HEX_F = icon("CHAR_HEX_F", "textures/block/char_hex_f"),
            CHAR_DIVIDE = icon("CHAR_DIVIDE", "textures/block/char_divide"),
            CHAR_MULTIPLY = icon("CHAR_MULTIPLY", "textures/block/char_multiply"),
            CHAR_PLUS = icon("CHAR_PLUS", "textures/block/char_plus"),
            CHAR_MINUS = icon("CHAR_MINUS", "textures/block/char_minus"),
            CHAR_MOD = icon("CHAR_MOD", "textures/block/char_mod"),
            CHAR_GREATER = icon("CHAR_GREATER", "textures/block/char_greater"),
            CHAR_SMALLER = icon("CHAR_SMALLER", "textures/block/char_smaller"),
            CHAR_EQUAL = icon("CHAR_EQUAL", "textures/block/char_equal"),
            CHAR_UP = icon("CHAR_UP", "textures/block/char_up"),
            CHAR_UP_RIGHT = icon("CHAR_UP_RIGHT", "textures/block/char_up_right"),
            CHAR_RIGHT = icon("CHAR_RIGHT", "textures/block/char_right"),
            CHAR_DOWN_RIGHT = icon("CHAR_DOWN_RIGHT", "textures/block/char_down_right"),
            CHAR_DOWN = icon("CHAR_DOWN", "textures/block/char_down"),
            CHAR_DOWN_LEFT = icon("CHAR_DOWN_LEFT", "textures/block/char_down_left"),
            CHAR_LEFT = icon("CHAR_LEFT", "textures/block/char_left"),
            CHAR_UP_LEFT = icon("CHAR_UP_LEFT", "textures/block/char_up_left"),
            CHAR_SLASH = icon("CHAR_SLASH", "textures/block/char_slash"),
            CHAR_PERCENT = icon("CHAR_PERCENT", "textures/block/char_percent"),
            CHAR_METER = icon("CHAR_METER", "textures/block/char_meter"),
            CHAR_METER_3 = icon("CHAR_METER_3", "textures/block/char_meter_3"),
            CHAR_DECAMETER_3 = icon("CHAR_DECAMETER_3", "textures/block/char_decameter_3"),
            CHAR_KELVIN = icon("CHAR_KELVIN", "textures/block/char_kelvin"),
            CHAR_LITER = icon("CHAR_LITER", "textures/block/char_liter"),
            CHAR_LUMIN = icon("CHAR_LUMIN", "textures/block/char_lumin"),
            CHAR_CLOCK = icon("CHAR_CLOCK", "textures/block/char_clock"),
            CHAR_GIBBL = icon("CHAR_GIBBL", "textures/block/char_gibbl"),
            CHAR_GRAMM = icon("CHAR_GRAMM", "textures/block/char_gramm"),
            CHAR_KILOGRAMM = icon("CHAR_KILOGRAMM", "textures/block/char_kilogramm"),
            CHAR_TON = icon("CHAR_TON", "textures/block/char_ton"),
            CHAR_KILOTON = icon("CHAR_KILOTON", "textures/block/char_kiloton"),
            CHAR_EU = icon("CHAR_EU", "textures/block/char_eu"),
            CHAR_LU = icon("CHAR_LU", "textures/block/char_lu"),
            CHAR_RU = icon("CHAR_RU", "textures/block/char_ru"),
            CHAR_NEUTRON = icon("CHAR_NEUTRON", "textures/block/char_neutron"),
            CHAR_SCALE = icon("CHAR_SCALE", "textures/block/char_scale"),
            CHAR_GREG = icon("CHAR_GREG", "textures/block/char_greg"),
            CHAR_NEI = icon("CHAR_NEI", "textures/block/char_nei"),
            BAROMETER = icon("BAROMETER", "textures/block/barometer");

    public static Map<String, IconRef> all() {
        return Map.copyOf(REGISTRY);
    }

    public static void bootstrap() {
        if (REGISTRY.isEmpty()) {
            throw new IllegalStateException("BI failed to initialize");
        }
    }
}
