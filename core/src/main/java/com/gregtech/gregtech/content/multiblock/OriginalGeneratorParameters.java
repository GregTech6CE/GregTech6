package com.gregtech.gregtech.content.multiblock;
import java.util.*;

/** Original 17211-17234 rating tables and pure conversion/layout policy. */
public final class OriginalGeneratorParameters {

    private OriginalGeneratorParameters() {
    }

    public record GasGrade(String id, String rotor, String casingMaterial, int input, int output, int wallId, float hardness) {

        public int inputMaximum() {
            return input * 2;
        }

        public int outputMaximum() {
            return output * 2;
        }
    }

    public static final List<GasGrade> GRADES = List.of(
            new GasGrade("magnalium_gas_turbine_main_housing",    "Magnalium",    "StainlessSteel", 6144,   4096,   18022, 6),
            new GasGrade("trinitanium_gas_turbine_main_housing",  "Trinitanium",  "Titanium",       12288,  8192,   18026, 9),
            new GasGrade("graphene_gas_turbine_main_housing",     "Graphene",     "TungstenSteel",  24576,  16384,  18023, 12.5f),
            new GasGrade("vibramantium_gas_turbine_main_housing", "Vibramantium", "Adamantium",     196608, 131072, 18025, 100));

    public record AxialGrade(String id, int materialIndex, boolean steam, int input, int output) {

        public int inputMaximum() {
            return input * 2;
        }

        public int outputMaximum() {
            return output * 2;
        }
    }

    public static final List<AxialGrade> STEAM = grades(true), DYNAMO = grades(false);

    private static List<AxialGrade> grades(boolean steam) {
        var gradesByMaterial = new ArrayList<AxialGrade>();
        String[] steamIds = {
            "magnalium_steam_turbine_main_housing",
            "trinitanium_steam_turbine_main_housing",
            "graphene_steam_turbine_main_housing",
            "vibramantium_steam_turbine_main_housing"
        };
        String[] dynamoIds = {
            "stainless_steel_dynamo_main_housing",
            "titanium_dynamo_main_housing",
            "tungstensteel_dynamo_main_housing",
            "adamantium_dynamo_main_housing"
        };
        for (int gradeIndex = 0; gradeIndex < 4; gradeIndex++) {
            var gasGrade = GRADES.get(gradeIndex);
            gradesByMaterial.add(new AxialGrade((steam ? steamIds : dynamoIds)[gradeIndex], gradeIndex, steam,
                    steam ? gasGrade.input() * 2 : gasGrade.output(),
                    steam ? gasGrade.output() : gasGrade.output() * 3 / 4));
        }
        return List.copyOf(gradesByMaterial);
    }

    public static boolean usesCopperCoil(boolean steam, int back) {
        return !steam && (back == 1 || back == 2);
    }

    public static long convert(long energy, int output, int input) {
        return energy * output / input;
    }

    public static final int STEAM_CONDENSATION_RATIO = 170, HEAT_EXCHANGER_RATE = 16384;
}
