package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Crafting and smelting rows for the {@code components} and {@code smeltery_survival} recipe
 * directories, written as family templates over one voltage-tier table instead of 215 literal rows.
 *
 * <p>Ingredients are {@code item:<id>} or {@code tag:<id>} specs, so the platform pack
 * ({@code ComponentAndSmelteryRecipePack}) can turn a row into recipe JSON without this class
 * touching Minecraft types. {@code allowMirror} stays {@code null} for recipe types that have no
 * such flag; {@code gt6ce:tool_shaped} rows carry {@code allow_mirror: false}.</p>
 */
public final class ComponentAndSmelteryRecipeRules {
    private ComponentAndSmelteryRecipeRules() {}

    /** One published recipe row; the platform pack dispatches on the three concrete kinds. */
    public sealed interface Row permits Shaped, Shapeless, Smelting {
        String path();
    }

    /** A shaped row. {@code allowMirror} is null when the recipe type has no mirror flag. */
    public record Shaped(
            String path,
            String type,
            Boolean allowMirror,
            List<String> pattern,
            Map<String, String> key,
            String result,
            int count)
            implements Row {}

    /** A shapeless row; {@code ingredients} are ingredient specs in recipe order. */
    public record Shapeless(String path, String type, List<String> ingredients, String result, int count)
            implements Row {}

    /** A furnace row; {@code ingredient} is an ingredient spec. */
    public record Smelting(String path, String ingredient, String result, float experience, int cookingTime)
            implements Row {}

    /**
     * One voltage tier of the compact component families. Item ids keep GT6's material spelling
     * ({@code stainlesssteel}, {@code osmiumelemental}); the recipe tags use the underscored form.
     * The two gem components are full ingredient specs, because the sensor gems are tags while the
     * field-emitter gems are items.
     */
    public record Tier(
            String name,
            int circuitTier,
            String cableItem,
            String cableMaterial,
            String materialItem,
            String materialTag,
            String magneticTag,
            String motorWire,
            String osmiumWire,
            String fieldGem,
            String sensorGem) {
        String circuitTag() {
            return "gt6ce:circuits_tier_" + circuitTier + "_plus";
        }
    }

    private static final List<Tier> TIERS = List.of(
            new Tier(
                    "ulv",
                    0,
                    "gt6ce:cable_01_lead",
                    "lead",
                    "tinalloy",
                    "tin_alloy",
                    "forge:bolts/iron_magnetic",
                    "gt6ce:wire_fine_copper",
                    "gt6ce:wire_fine_osmiumelemental",
                    "item:minecraft:ender_pearl",
                    "tag:forge:gems/quartz"),
            new Tier(
                    "lv",
                    1,
                    "gt6ce:cable_01_tin",
                    "tin",
                    "steelgalvanized",
                    "steel_galvanized",
                    "forge:rods/iron_magnetic",
                    "gt6ce:wire_01_copper",
                    "gt6ce:wire_01_osmium",
                    "item:minecraft:ender_pearl",
                    "tag:forge:gems/quartz"),
            new Tier(
                    "mv",
                    2,
                    "gt6ce:cable_01_copper",
                    "copper",
                    "aluminium",
                    "aluminium",
                    "forge:rods/steel_magnetic",
                    "gt6ce:wire_02_copper",
                    "gt6ce:wire_02_osmium",
                    "item:minecraft:ender_eye",
                    "tag:forge:gems/quartz"),
            new Tier(
                    "hv",
                    3,
                    "gt6ce:cable_01_gold",
                    "gold",
                    "stainlesssteel",
                    "stainless_steel",
                    "forge:rods/steel_magnetic",
                    "gt6ce:wire_03_copper",
                    "gt6ce:wire_04_osmium",
                    "item:minecraft:ender_eye",
                    "tag:forge:gems/emerald"),
            new Tier(
                    "ev",
                    4,
                    "gt6ce:cable_01_aluminium",
                    "aluminium",
                    "chromium",
                    "chromium",
                    "forge:rods/neodymium_magnetic",
                    "gt6ce:wire_04_annealed_copper",
                    "gt6ce:wire_06_osmium",
                    "item:minecraft:nether_star",
                    "item:minecraft:ender_pearl"),
            new Tier(
                    "iv",
                    5,
                    "gt6ce:cable_01_platinum",
                    "platinum",
                    "titanium",
                    "titanium",
                    "forge:rods/neodymium_magnetic",
                    "gt6ce:wire_05_annealed_copper",
                    "gt6ce:wire_08_osmium",
                    "item:minecraft:nether_star",
                    "item:minecraft:ender_eye"),
            new Tier(
                    "luv",
                    6,
                    "gt6ce:wire_01_graphene",
                    "graphene",
                    "iridium",
                    "iridium",
                    "forge:long_rods/neodymium_magnetic",
                    "gt6ce:wire_06_annealed_copper",
                    "gt6ce:wire_10_osmium",
                    "item:minecraft:nether_star",
                    "item:minecraft:nether_star"),
            new Tier(
                    "zpm",
                    7,
                    "gt6ce:wire_01_graphene",
                    "graphene",
                    "osmiumelemental",
                    "osmium_elemental",
                    "forge:long_rods/neodymium_magnetic",
                    "gt6ce:wire_07_annealed_copper",
                    "gt6ce:wire_12_osmium",
                    "item:minecraft:nether_star",
                    "item:minecraft:nether_star"),
            new Tier(
                    "uv",
                    8,
                    "gt6ce:wire_01_graphene",
                    "graphene",
                    "trinitanium",
                    "trinitanium",
                    "forge:long_rods/neodymium_magnetic",
                    "gt6ce:wire_08_annealed_copper",
                    "gt6ce:wire_14_osmium",
                    "item:minecraft:nether_star",
                    "item:minecraft:nether_star"),
            new Tier(
                    "puv1",
                    9,
                    "gt6ce:wire_01_graphene",
                    "graphene",
                    "trinaquadalloy",
                    "trinaquadalloy",
                    "forge:long_rods/neodymium_magnetic",
                    "gt6ce:wire_09_annealed_copper",
                    "gt6ce:wire_16_osmium",
                    "item:minecraft:nether_star",
                    "item:minecraft:nether_star"));

    /** A metal with its recipe tag spelling: {@code path} is the id suffix, {@code material} the tag suffix. */
    public record Metal(String path, String material) {}

    private static final Metal TA4HFC5 = new Metal("ta4hfc5", "tantalum_hafnium_carbide");

    /** Metals with the plate forms the fluid-bed and solid burning boxes are built from. */
    private static final List<Metal> PLATE_METALS = List.of(
            metal("arsenic_bronze"),
            metal("arsenic_copper"),
            metal("bismuth"),
            metal("bronze"),
            metal("chromium"),
            metal("invar"),
            metal("lead"),
            metal("netherite"),
            metal("steel"),
            TA4HFC5,
            metal("titanium"),
            metal("tungsten"),
            metal("tungsten_steel"));

    /** Metals the liquid burning boxes ship for. */
    private static final List<Metal> LIQUID_METALS = List.of(
            metal("arsenic_bronze"),
            metal("arsenic_copper"),
            metal("bronze"),
            metal("chromium"),
            metal("invar"),
            metal("netherite"),
            metal("steel"),
            TA4HFC5,
            metal("titanium"),
            metal("tungsten"),
            metal("tungsten_steel"));

    /** Boiler metals; ultimet replaces Ta4HfC5 here. */
    private static final List<Metal> BOILER_METALS = List.of(
            metal("arsenic_bronze"),
            metal("arsenic_copper"),
            metal("bismuth"),
            metal("bronze"),
            metal("chromium"),
            metal("invar"),
            metal("lead"),
            metal("netherite"),
            metal("steel"),
            metal("titanium"),
            metal("tungsten"),
            metal("tungsten_steel"),
            metal("ultimet"));

    /** Metals with a plate-formed crucible. */
    private static final List<Metal> CRUCIBLE_METALS = List.of(
            metal("ad"),
            metal("bronze"),
            metal("chromium"),
            metal("fiery_steel"),
            metal("invar"),
            metal("iridium"),
            metal("knightmetal"),
            metal("molybdenum"),
            metal("netherite"),
            metal("niobium"),
            metal("niobium_titanium"),
            metal("osmium"),
            metal("stainless_steel"),
            metal("steel"),
            TA4HFC5,
            metal("tantalum"),
            metal("thaumium"),
            metal("titanium"),
            metal("tungsten"),
            metal("vanadium"));

    /** A crucible shell hammered from one block or plate form; {@code symbol} is its grid key. */
    private record Shell(String path, String symbol, String ingredient) {}

    private static final List<Shell> CRUCIBLE_SHELLS = List.of(
            new Shell("basalt", "B", item("gt6ce:stone_basalt_stone")),
            new Shell("carbon", "C", tag("forge:plates/graphene")),
            new Shell("granite_black", "B", item("gt6ce:stone_granite_black_stone")),
            new Shell("granite_red", "B", item("gt6ce:stone_granite_red_stone")),
            new Shell("nether_brick", "B", item("minecraft:nether_bricks")),
            new Shell("quartz", "B", tag("gt6ce:gem/quartz")),
            new Shell("stone", "B", item("minecraft:stone")));

    /** Every row of both directories: 84 {@code components} plus 131 {@code smeltery_survival}. */
    public static List<Row> all() {
        List<Row> rows = new ArrayList<>(215);
        rows.addAll(components());
        rows.addAll(smelterySurvival());
        return List.copyOf(rows);
    }

    /** The eight compact component families over all ten tiers, plus the two single rows. */
    public static List<Shaped> components() {
        List<Shaped> rows = new ArrayList<>(84);
        for (Tier tier : TIERS) {
            rows.add(motor(tier, tier.name(), tier.magneticTag()));
            rows.add(conveyor(tier));
            rows.add(pump(tier));
            rows.add(piston(tier));
            rows.add(robotArm(tier));
            rows.add(fieldEmitter(tier));
            rows.add(sensor(tier));
            rows.add(signalEmitter(tier));
        }
        // The steel-magnet variants GT6 offers next to the iron-magnet ULV and LV motors.
        for (Tier tier : TIERS.subList(0, 2)) {
            rows.add(motor(tier, tier.name() + "_steel", steelMagnet(tier)));
        }
        rows.add(circuitWiringCopper());
        rows.add(laserEmitterEmptyGas());
        return List.copyOf(rows);
    }

    /** The three burning-box families, the boilers, the crucibles and the four single rows. */
    public static List<Row> smelterySurvival() {
        List<Row> rows = new ArrayList<>(131);
        for (Metal metal : PLATE_METALS) {
            rows.add(fluidBedBox(metal, false));
            rows.add(fluidBedBox(metal, true));
            rows.add(solidBox(metal, false));
            rows.add(solidBox(metal, true));
        }
        for (Metal metal : LIQUID_METALS) {
            rows.add(liquidBox(metal, false));
            rows.add(liquidBox(metal, true));
        }
        rows.add(brickBox());
        for (Metal metal : BOILER_METALS) {
            rows.add(steamBoiler(metal, false));
            rows.add(steamBoiler(metal, true));
        }
        for (Metal metal : CRUCIBLE_METALS) {
            rows.add(plateCrucible(metal));
        }
        for (Shell shell : CRUCIBLE_SHELLS) {
            rows.add(shellCrucible(shell));
        }
        rows.add(clayCrucible());
        rows.add(clayCrucibleReclaim());
        rows.add(ceramicCrucibleFiring());
        return List.copyOf(rows);
    }

    private static Shaped motor(Tier tier, String variant, String magneticTag) {
        return shaped(
                "components/motor_" + variant,
                List.of("CWR", "WIW", "PWC"),
                keys(
                        "C", item(tier.cableItem()),
                        "W", item(tier.motorWire()),
                        "R", tag("forge:rods/" + tier.materialTag()),
                        "P", item(curvedPlate(tier)),
                        "I", tag(magneticTag)),
                motorItem(tier));
    }

    private static Shaped conveyor(Tier tier) {
        return shaped(
                "components/conveyor_" + tier.name(),
                List.of("RRR", "MCM", "RRR"),
                keys(
                        "C", item(tier.cableItem()),
                        "M", item(motorItem(tier)),
                        "R", tag("forge:plates/rubber")),
                "gt6ce:compact_electric_conveyor_" + tier.name());
    }

    private static Shaped pump(Tier tier) {
        return toolShaped(
                "components/pump_" + tier.name(),
                List.of("TXO", "dPw", "OMT"),
                keys(
                        "M", item(motorItem(tier)),
                        "O", tag("forge:rings/rubber"),
                        "P", item(curvedPlate(tier)),
                        "T", tag("forge:screws/" + tier.materialTag()),
                        "X", item("gt6ce:rotor_" + tier.materialItem()),
                        "d", item("gt6ce:tool_screwdriver"),
                        "w", item("gt6ce:tool_wrench")),
                "gt6ce:compact_electric_pump_" + tier.name());
    }

    private static Shaped piston(Tier tier) {
        return toolShaped(
                "components/piston_" + tier.name(),
                List.of("TPP", "dSS", "TMG"),
                keys(
                        "G", item("gt6ce:gear_gt_small_" + tier.materialItem()),
                        "M", item(motorItem(tier)),
                        "P", tag("forge:plates/" + tier.materialTag()),
                        "S", tag("forge:rods/" + tier.materialTag()),
                        "T", tag("forge:screws/" + tier.materialTag()),
                        "d", item("gt6ce:tool_screwdriver")),
                pistonItem(tier));
    }

    private static Shaped robotArm(Tier tier) {
        return shaped(
                "components/robot_arm_" + tier.name(),
                List.of("CCC", "MSM", "PES"),
                keys(
                        "C", item(tier.cableItem()),
                        "E", tag(tier.circuitTag()),
                        "M", item(motorItem(tier)),
                        "P", item(pistonItem(tier)),
                        "S", tag("forge:rods/" + tier.materialTag())),
                "gt6ce:compact_robot_arm_" + tier.name());
    }

    private static Shaped fieldEmitter(Tier tier) {
        return shaped(
                "components/field_emitter_" + tier.name(),
                List.of("WPW", "CGC", "WPW"),
                keys(
                        "C", tag(tier.circuitTag()),
                        "G", tier.fieldGem(),
                        "P", item("gt6ce:plate_double_" + tier.materialItem()),
                        "W", item(tier.osmiumWire())),
                "gt6ce:compact_force_field_emitter_" + tier.name());
    }

    private static Shaped sensor(Tier tier) {
        return shaped(
                "components/sensor_" + tier.name(),
                List.of("P Q", "PS ", "CPP"),
                keys(
                        "C", tag(tier.circuitTag()),
                        "P", item(curvedPlate(tier)),
                        "Q", tier.sensorGem(),
                        "S", item("gt6ce:wire_01_" + tier.cableMaterial())),
                "gt6ce:compact_sensor_" + tier.name());
    }

    private static Shaped signalEmitter(Tier tier) {
        return shaped(
                "components/signal_emitter_" + tier.name(),
                List.of("SPC", "WQP", "CWS"),
                keys(
                        "C", tag(tier.circuitTag()),
                        "P", item(curvedPlate(tier)),
                        "Q", tier.sensorGem(),
                        "S", item("gt6ce:wire_04_" + tier.cableMaterial()),
                        "W", item(tier.cableItem())),
                "gt6ce:compact_signal_emitter_" + tier.name());
    }

    private static Shaped circuitWiringCopper() {
        return toolShaped(
                "components/circuit_wiring_copper",
                List.of("WWW", "WxW", "WWW"),
                keys("W", item("gt6ce:wire_fine_copper"), "x", item("gt6ce:tool_wire_cutter")),
                "gt6ce:circuit_wiring_copper");
    }

    private static Shaped laserEmitterEmptyGas() {
        return toolShaped(
                "components/laser_emitter_emptygas",
                List.of("CWM", "WGx", "MTd"),
                keys(
                        "C", tag("gt6ce:circuits_tier_2_plus"),
                        "W", item("gt6ce:cable_01_copper"),
                        "M", tag("forge:plates/silver"),
                        "G", item("minecraft:glass"),
                        "x", item("gt6ce:tool_wire_cutter"),
                        "T", tag("forge:screws/stainless_steel"),
                        "d", item("gt6ce:tool_screwdriver")),
                "gt6ce:laser_emitter_emptygas");
    }

    private static Shaped fluidBedBox(Metal metal, boolean dense) {
        String name = "burning_box_fluidbed_" + densePrefix(dense) + metal.path();
        return toolShaped(
                "smeltery_survival/" + name,
                List.of("PCP", "UwU", "BXB"),
                keys(
                        "P", tag(boxPlate(metal, dense)),
                        "C", tag(copperPlate(dense)),
                        "U", tag("gt6ce:plate_curved/" + metal.material()),
                        "w", item("gt6ce:tool_wrench"),
                        "B", item("minecraft:bricks"),
                        "X", tag("gt6ce:rotor/" + metal.material())),
                "gt6ce:" + name);
    }

    private static Shaped liquidBox(Metal metal, boolean dense) {
        String name = "burning_box_liquid_" + densePrefix(dense) + metal.path();
        return toolShaped(
                "smeltery_survival/" + name,
                List.of("PCP", "IwI", "BBB"),
                keys(
                        "P", tag(boxPlate(metal, dense)),
                        "C", tag(copperPlate(dense)),
                        "I", tag((dense ? "gt6ce:pipe_large/" : "gt6ce:plate_curved/") + metal.material()),
                        "w", item("gt6ce:tool_wrench"),
                        "B", item("minecraft:bricks")),
                "gt6ce:" + name);
    }

    private static Shaped solidBox(Metal metal, boolean dense) {
        String name = "burning_box_solid_" + densePrefix(dense) + metal.path();
        return toolShaped(
                "smeltery_survival/" + name,
                List.of("PCP", "PwP", "BBB"),
                keys(
                        "P", tag(boxPlate(metal, dense)),
                        "C", tag(copperPlate(dense)),
                        "w", item("gt6ce:tool_wrench"),
                        "B", item("minecraft:bricks")),
                "gt6ce:" + name);
    }

    private static Shaped brickBox() {
        return toolShaped(
                "smeltery_survival/burning_box_solid_brick",
                List.of("BBB", "BBB", "BFB"),
                keys("B", tag("gt6ce:ingot/brick"), "F", tag("gt6ce:crafting_firestarters")),
                "gt6ce:burning_box_solid_brick");
    }

    private static Shaped steamBoiler(Metal metal, boolean strong) {
        String name = (strong ? "strong_steam_boiler_" : "steam_boiler_") + metal.path();
        return toolShaped(
                "smeltery_survival/" + name,
                List.of(" P ", "PwP", "PhP"),
                keys(
                        "P", tag((strong ? "gt6ce:plate_dense/" : "gt6ce:plate_double/") + metal.material()),
                        "w", item("gt6ce:tool_wrench"),
                        "h", item("gt6ce:tool_hammer")),
                "gt6ce:" + name);
    }

    private static Shaped plateCrucible(Metal metal) {
        String name = "smelting_crucible_" + metal.path();
        return toolShaped(
                "smeltery_survival/" + name,
                List.of("PhP", "PwP", "PPP"),
                keys(
                        "P", tag("forge:plates/" + metal.material()),
                        "h", item("gt6ce:tool_hammer"),
                        "w", item("gt6ce:tool_wrench")),
                "gt6ce:" + name);
    }

    private static Shaped shellCrucible(Shell shell) {
        String symbol = shell.symbol();
        String name = "smelting_crucible_" + shell.path();
        return toolShaped(
                "smeltery_survival/" + name,
                List.of(symbol + "h" + symbol, symbol + "y" + symbol, symbol + symbol + symbol),
                keys(symbol, shell.ingredient(), "h", item("gt6ce:tool_hammer"), "y", item("gt6ce:tool_chisel")),
                "gt6ce:" + name);
    }

    private static Shaped clayCrucible() {
        return toolShaped(
                "smeltery_survival/clay_crucible",
                List.of("CkC", "CRC", "CCC"),
                keys(
                        "C", tag("gt6ce:clay_balls"),
                        "k", item("gt6ce:tool_knife"),
                        "R", item("gt6ce:tool_rolling_pin")),
                "gt6ce:clay_crucible");
    }

    private static Shapeless clayCrucibleReclaim() {
        return new Shapeless(
                "smeltery_survival/clay_crucible_reclaim",
                "minecraft:crafting_shapeless",
                List.of(item("gt6ce:clay_crucible")),
                "minecraft:clay_ball",
                7);
    }

    private static Smelting ceramicCrucibleFiring() {
        return new Smelting(
                "smeltery_survival/ceramic_crucible_firing",
                item("gt6ce:clay_crucible"),
                "gt6ce:smelting_crucible_ceramic",
                0.0F,
                200);
    }

    /** The steel-magnet variant of a motor: the same row with the iron magnet swapped for steel. */
    private static String steelMagnet(Tier tier) {
        return tier.magneticTag().replace("iron_magnetic", "steel_magnetic");
    }

    private static String motorItem(Tier tier) {
        return "gt6ce:compact_electric_motor_" + tier.name();
    }

    private static String pistonItem(Tier tier) {
        return "gt6ce:compact_electric_piston_" + tier.name();
    }

    private static String curvedPlate(Tier tier) {
        return "gt6ce:plate_curved_" + tier.materialItem();
    }

    private static String densePrefix(boolean dense) {
        return dense ? "dense_" : "";
    }

    private static String boxPlate(Metal metal, boolean dense) {
        return (dense ? "gt6ce:plate_quintuple/" : "forge:plates/") + metal.material();
    }

    private static String copperPlate(boolean dense) {
        return dense ? "gt6ce:plate_dense/copper" : "gt6ce:plate_double/copper";
    }

    private static Metal metal(String name) {
        return new Metal(name, name);
    }

    private static Shaped shaped(String path, List<String> pattern, Map<String, String> key, String result) {
        return new Shaped(path, "minecraft:crafting_shaped", null, pattern, key, result, 1);
    }

    private static Shaped toolShaped(String path, List<String> pattern, Map<String, String> key, String result) {
        return new Shaped(path, "gt6ce:tool_shaped", Boolean.FALSE, pattern, key, result, 1);
    }

    /** Builds a grid key from alternating symbol and ingredient-spec arguments. */
    private static Map<String, String> keys(String... symbolsAndSpecs) {
        Map<String, String> key = new LinkedHashMap<>();
        for (int i = 0; i < symbolsAndSpecs.length; i += 2) {
            key.put(symbolsAndSpecs[i], symbolsAndSpecs[i + 1]);
        }
        return key;
    }

    private static String item(String id) {
        return "item:" + id;
    }

    private static String tag(String id) {
        return "tag:" + id;
    }
}
