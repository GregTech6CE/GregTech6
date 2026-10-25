package gregtech.gametest;

/** Fixed source manufacturing IDs; output registry names are not recipe paths. */
final class CraftingFixtureIds {
    private CraftingFixtureIds() {}
    private static final java.util.Map<String, String> AXIAL = java.util.Map.ofEntries(
            java.util.Map.entry("adamantium_dynamo_main_housing", "axial/large_dynamo_adamantium"),
            java.util.Map.entry("graphene_gas_turbine_main_housing", "axial/large_gas_turbine_graphene"),
            java.util.Map.entry("graphene_steam_turbine_main_housing", "axial/large_steam_turbine_graphene"),
            java.util.Map.entry("magnalium_gas_turbine_main_housing", "axial/large_gas_turbine_main"),
            java.util.Map.entry("magnalium_steam_turbine_main_housing", "axial/large_turbine_main"),
            java.util.Map.entry("stainless_steel_dynamo_main_housing", "axial/large_dynamo_main"),
            java.util.Map.entry("titanium_dynamo_main_housing", "axial/large_dynamo_titanium"),
            java.util.Map.entry("trinitanium_gas_turbine_main_housing", "axial/large_gas_turbine_trinitanium"),
            java.util.Map.entry("trinitanium_steam_turbine_main_housing", "axial/large_steam_turbine_trinitanium"),
            java.util.Map.entry("tungstensteel_dynamo_main_housing", "axial/large_dynamo_tungstensteel"),
            java.util.Map.entry("vibramantium_gas_turbine_main_housing", "axial/large_gas_turbine_vibramantium"),
            java.util.Map.entry("vibramantium_steam_turbine_main_housing", "axial/large_steam_turbine_vibramantium"));

    static String axialPath(String output) {
        String path = AXIAL.get(output);
        if (path == null) throw new AssertionError("Unknown source axial output: " + output);
        return path;
    }
}
