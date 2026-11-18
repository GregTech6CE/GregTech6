package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Shaped crafting rows for the four energy-equipment recipe directories: {@code chemical_batteries},
 * {@code engines}, {@code energy_nodes} and {@code long_distance}.
 *
 * <p>Their 126 recipes repeat the same handful of shapes across tiers and materials, so they are
 * expressed here as tier/material tables and expanded once into {@link #ROWS}; the platform side
 * ({@code EnergyEquipmentRecipePack}) only turns a {@link Row} into JSON. Ingredient ids are GT6CE
 * item and tag ids, which keeps this class free of loader types.</p>
 *
 * <p>Two invariants hold for every row and are relied on by the emitters: a row crafts the item its
 * id is named after, and its key symbols are exactly the symbols its pattern spells. Vanilla rejects
 * a shaped recipe whose key declares an unused symbol, so the tables must not carry spare keys.</p>
 */
public final class EnergyEquipmentRecipeRules {
    /** Recipe directory below {@code data/gt6ce} that a category's rows are published to. */
    public enum Category {
        CHEMICAL_BATTERIES("chemical_batteries"),
        ENGINES("engines"),
        ENERGY_NODES("energy_nodes"),
        LONG_DISTANCE("long_distance");

        private final String directory;

        Category(String directory) {
            this.directory = directory;
        }

        public String directory() {
            return directory;
        }
    }

    /** How a key symbol resolves to an ingredient. */
    public enum IngredientKind {
        ITEM,
        TAG,
        /** A 1000 mB fluid container; {@link Ingredient#id()} names the fluid. */
        LUBRICANT_CONTAINER
    }

    /** One key symbol's ingredient, expressed as a GT6CE item id, tag id or fluid container. */
    public record Ingredient(IngredientKind kind, String id) {
        public static Ingredient item(String id) {
            return new Ingredient(IngredientKind.ITEM, id);
        }

        public static Ingredient tag(String id) {
            return new Ingredient(IngredientKind.TAG, id);
        }

        /** The diesel engines' lubricant slot: any container holding 1000 mB of the given fluid. */
        public static Ingredient lubricantContainer(String fluid) {
            return new Ingredient(IngredientKind.LUBRICANT_CONTAINER, fluid);
        }
    }

    /** One shaped recipe: where it is published, its serializer and its 3x3 pattern with keys. */
    public record Row(
            Category category, String name, String type, List<String> pattern, Map<Character, Ingredient> key) {
        /** The crafted item; every row in these four categories crafts the item its id names. */
        public String resultId() {
            return "gt6ce:" + name;
        }

        /** Path below the recipe root, e.g. {@code chemical_batteries/battery_alkaline_lv}. */
        public String jsonPath() {
            return category.directory() + "/" + name;
        }

        /**
         * {@code null} for vanilla shaped rows, whose serializer has no mirror flag; GT6's
         * {@code CR.DEF_REV_NCC} recipes never mirror, so every tool-shaped row is {@code false}.
         */
        public Boolean allowMirror() {
            return TOOL_SHAPED.equals(type) ? Boolean.FALSE : null;
        }
    }

    private static final String TOOL_SHAPED = "gt6ce:tool_shaped";
    private static final String VANILLA_SHAPED = "minecraft:crafting_shaped";
    private static final String HAMMER = "gt6ce:tool_hammer";
    private static final String WRENCH = "gt6ce:tool_wrench";
    private static final String WIRE_CUTTER = "gt6ce:tool_wire_cutter";
    private static final String BATTERY_PLATE = "gt6ce:plate_batteryalloy";
    /** The lowest circuit tier a battery recipe is allowed to ask for. */
    private static final int MIN_CIRCUIT_TIER = 2;

    private EnergyEquipmentRecipeRules() {}

    /** Sorts the symbols so the emitted JSON is stable and reads like the spec tables. */
    private static Map<Character, Ingredient> keys(Map<Character, Ingredient> ingredients) {
        return new TreeMap<>(ingredients);
    }

    private static Row toolRow(Category category, String name, List<String> pattern, Map<Character, Ingredient> key) {
        return new Row(category, name, TOOL_SHAPED, pattern, key);
    }

    // ---------------------------------------------------------------- chemical_batteries

    /** A battery chemistry: the filled cell it is built from and the circuit tier its MV recipe uses. */
    private record BatteryChemistry(String id, String cell, int mvCircuitTier) {}

    private static final List<BatteryChemistry> BATTERY_CHEMISTRIES = List.of(
            new BatteryChemistry("alkaline", "gt6ce:alkaline_button_cell_filled", 2),
            new BatteryChemistry("lead_acid", "gt6ce:lead_acid_cell_filled", 2),
            new BatteryChemistry("lithium_cobalt", "gt6ce:lithium_cobalt_cell_filled", 4),
            new BatteryChemistry("lithium_manganese", "gt6ce:lithium_manganese_cell_filled", 4),
            new BatteryChemistry("nickel_cadmium", "gt6ce:nickel_cadmium_cell_filled", 2));

    /**
     * A battery tier: its cable, the offset from {@link BatteryChemistry#mvCircuitTier()} and the
     * pattern, whose {@code C} cell stays empty while the chemistry needs no circuit yet.
     */
    private record BatteryTier(String id, String cable, int circuitOffset, boolean wireCutter, List<String> pattern) {}

    private static final List<BatteryTier> BATTERY_TIERS = List.of(
            new BatteryTier("ulv", "gt6ce:cable_01_lead", -2, true, List.of("Wx", "BC", "P ")),
            new BatteryTier("lv", "gt6ce:cable_01_tin", -1, true, List.of("CWx", "PBP", " B ")),
            new BatteryTier("mv", "gt6ce:cable_01_copper", 0, true, List.of("WxW", "BCB", "PBP")),
            new BatteryTier("hv", "gt6ce:cable_01_gold", 1, true, List.of("WxW", "BCB", "BPB")),
            new BatteryTier("ev", "gt6ce:cable_01_aluminium", 2, false, List.of("WPW", "BCB", "BBB")));

    private static void batteries(List<Row> rows) {
        for (BatteryChemistry chemistry : BATTERY_CHEMISTRIES) {
            for (BatteryTier tier : BATTERY_TIERS) {
                int circuitTier = chemistry.mvCircuitTier() + tier.circuitOffset();
                boolean circuit = circuitTier >= MIN_CIRCUIT_TIER;
                Map<Character, Ingredient> key = new HashMap<>();
                key.put('B', Ingredient.item(chemistry.cell()));
                key.put('P', Ingredient.item(BATTERY_PLATE));
                key.put('W', Ingredient.item(tier.cable()));
                if (tier.wireCutter()) key.put('x', Ingredient.item(WIRE_CUTTER));
                if (circuit) key.put('C', Ingredient.tag(ElectricalCraftingRules.circuitTag(circuitTier)));
                rows.add(toolRow(
                        Category.CHEMICAL_BATTERIES,
                        "battery_" + chemistry.id() + "_" + tier.id(),
                        circuit ? tier.pattern() : blankCircuit(tier.pattern()),
                        keys(key)));
            }
        }
    }

    /** The tier pattern with its circuit cell left empty, for chemistries below the circuit tier. */
    private static List<String> blankCircuit(List<String> pattern) {
        List<String> blanked = new ArrayList<>(pattern.size());
        for (String row : pattern) blanked.add(row.replace('C', ' '));
        return blanked;
    }

    // ---------------------------------------------------------------- engines

    /** Materials that only exist as a family tag ({@code gt6ce:engines/<form>/any_<family>}). */
    private static final Set<String> ENGINE_FAMILIES = Set.of("steel", "tungsten");

    /** One material form of an engine, resolved through the family tag where the material has none. */
    private static Ingredient formTag(String form, String material) {
        return Ingredient.tag(
                ENGINE_FAMILIES.contains(material)
                        ? "gt6ce:engines/" + form + "/any_" + material
                        : "gt6ce:" + form + "/" + material);
    }

    private static final List<String> DIESEL_MATERIALS = List.of(
            "arsenic_bronze", "arsenic_copper", "bronze", "invar", "iridium", "steel", "titanium", "tungsten_steel");

    /** An electric engine: the plate material plus the wire and magnet its rotor is wound from. */
    private record ElectricEngine(String id, String plateMaterial, Ingredient wire, Ingredient magnet) {}

    private static final List<ElectricEngine> ELECTRIC_ENGINES = List.of(
            new ElectricEngine(
                    "aluminium",
                    "aluminium",
                    Ingredient.tag("gt6ce:wire_02/any_copper"),
                    Ingredient.tag("gt6ce:stick_long/steel_magnetic")),
            new ElectricEngine(
                    "chromium",
                    "chromium",
                    Ingredient.item("gt6ce:wire_08_annealed_copper"),
                    Ingredient.tag("gt6ce:stick_long/neodymium_magnetic")),
            new ElectricEngine(
                    "galvanized_steel",
                    "steel_galvanized",
                    Ingredient.tag("gt6ce:wire_01/any_copper"),
                    Ingredient.tag("gt6ce:stick_long/iron_magnetic")),
            new ElectricEngine(
                    "stainless_steel",
                    "stainless_steel",
                    Ingredient.item("gt6ce:wire_04_annealed_copper"),
                    Ingredient.tag("gt6ce:stick_long/steel_magnetic")),
            new ElectricEngine(
                    "titanium",
                    "titanium",
                    Ingredient.item("gt6ce:wire_16_annealed_copper"),
                    Ingredient.tag("gt6ce:stick_long/neodymium_magnetic")));

    /** A flux engine: its gear material and the electric engine it is built around. */
    private record FluxEngine(String id, String gearMaterial, String core) {}

    private static final List<FluxEngine> FLUX_ENGINES = List.of(
            new FluxEngine("electrum", "electrum", "engine_electric_stainless_steel"),
            new FluxEngine("enderium", "enderium", "engine_electric_titanium"),
            new FluxEngine("enderium_base", "enderium_base", "engine_electric_chromium"),
            new FluxEngine("invar", "invar", "engine_electric_aluminium"),
            new FluxEngine("lead", "lead", "engine_electric_galvanized_steel"));

    /** A steam engine family: same pattern and materials, different spring and plate forms. */
    private record SteamFamily(String prefix, String spring, String plate) {}

    private static final List<SteamFamily> STEAM_FAMILIES = List.of(
            new SteamFamily("engine_steam_", "spring_small", "plate_double"),
            new SteamFamily("engine_steam_strong_", "spring", "plate_dense"));

    private static final List<String> STEAM_MATERIALS = List.of(
            "arsenic_bronze",
            "arsenic_copper",
            "brass",
            "bronze",
            "chromium",
            "fiery_steel",
            "invar",
            "ironwood",
            "lead",
            "steel",
            "tin_alloy",
            "titanium",
            "tungsten",
            "tungsten_steel");

    private static void engines(List<Row> rows) {
        for (String material : DIESEL_MATERIALS) {
            rows.add(toolRow(
                    Category.ENGINES,
                    "engine_diesel_" + material,
                    List.of("PLP", "SMS", "GPC"),
                    keys(Map.of(
                            'C', formTag("gear_gt_small", material),
                            'G', formTag("gear_gt", material),
                            'L', Ingredient.lubricantContainer("Lubricant"),
                            'M', formTag("casing_machine_double", material),
                            'P', formTag("plate_curved", material),
                            'S', formTag("stick", material)))));
        }
        for (ElectricEngine engine : ELECTRIC_ENGINES) {
            rows.add(toolRow(
                    Category.ENGINES,
                    "engine_electric_" + engine.id(),
                    List.of("PhP", "CIC", "PwP"),
                    keys(Map.of(
                            'C', engine.wire(),
                            'I', engine.magnet(),
                            'P', Ingredient.tag("gt6ce:plate_triple/" + engine.plateMaterial()),
                            'h', Ingredient.item(HAMMER),
                            'w', Ingredient.item(WRENCH)))));
        }
        for (FluxEngine engine : FLUX_ENGINES) {
            rows.add(toolRow(
                    Category.ENGINES,
                    "engine_flux_" + engine.id(),
                    List.of("G", "M", "G"),
                    keys(Map.of(
                            'G', Ingredient.tag("gt6ce:gear_gt/" + engine.gearMaterial()),
                            'M', Ingredient.item("gt6ce:" + engine.core())))));
        }
        for (SteamFamily family : STEAM_FAMILIES) {
            for (String material : STEAM_MATERIALS) {
                rows.add(toolRow(
                        Category.ENGINES,
                        family.prefix() + material,
                        List.of("PhP", "SIS", "PwP"),
                        keys(Map.of(
                                'I', formTag(family.spring(), material),
                                'P', formTag(family.plate(), material),
                                'S', formTag("stick", material),
                                'h', Ingredient.item(HAMMER),
                                'w', Ingredient.item(WRENCH)))));
            }
        }
    }

    // ---------------------------------------------------------------- energy_nodes

    /**
     * One voltage tier: its machine casing, the conductor its battery box and energy storage use,
     * the conductor its transformers use and its circuit tier.
     */
    private record EnergyTier(String id, String casing, String conductor, String transformerConductor, int circuit) {}

    private static final List<EnergyTier> ENERGY_TIERS = List.of(
            new EnergyTier("ulv", "tinalloy", "lead", "any_copper", 0),
            new EnergyTier("lv", "steelgalvanized", "tin", "any_copper", 1),
            new EnergyTier("mv", "aluminium", "any_copper", "any_copper", 2),
            new EnergyTier("hv", "stainlesssteel", "gold", "annealed_copper", 3),
            new EnergyTier("ev", "chromium", "aluminium", "annealed_copper", 4),
            new EnergyTier("iv", "titanium", "platinum", "annealed_copper", 5),
            new EnergyTier("luv", "iridium", "graphene", "annealed_copper", 6),
            new EnergyTier("zpm", "osmiumelemental", "graphene", "annealed_copper", 7),
            new EnergyTier("uv", "trinitanium", "graphene", "annealed_copper", 8),
            new EnergyTier("xv", "trinaquadalloy", "graphene", "annealed_copper", 9));

    private static final List<String> BATTERY_BOX_PATTERN = List.of("WCW", "WCW", "XMX");
    private static final List<String> TRANSFORMER_PATTERN = List.of("WIW", "XMx", "WIW");

    /**
     * One conductor form of a tier: {@code wire_01}, {@code wire_04}, {@code cable_01} or
     * {@code cable_04}. Copper is a family with no single item, and graphene has no cable item, so
     * its cable forms are the wire items.
     */
    private static Ingredient conductor(String prefix, String gauge, String conductor) {
        return switch (conductor) {
            case "any_copper" -> Ingredient.tag("gt6ce:" + prefix + "_" + gauge + "/any_copper");
            case "graphene" -> Ingredient.item("gt6ce:wire_" + gauge + "_graphene");
            default -> Ingredient.item("gt6ce:" + prefix + "_" + gauge + "_" + conductor);
        };
    }

    private static void energyNodes(List<Row> rows) {
        for (int index = 0; index < ENERGY_TIERS.size(); index++) {
            EnergyTier tier = ENERGY_TIERS.get(index);
            rows.add(new Row(
                    Category.ENERGY_NODES,
                    "battery_box_" + tier.id(),
                    VANILLA_SHAPED,
                    BATTERY_BOX_PATTERN,
                    keys(Map.of(
                            'C', conductor("cable", "01", tier.conductor()),
                            'M', Ingredient.item("gt6ce:casing_machine_" + tier.casing()),
                            'W', conductor("wire", "01", tier.conductor()),
                            'X', Ingredient.tag(ElectricalCraftingRules.circuitTag(tier.circuit()))))));
            if (index + 1 == ENERGY_TIERS.size()) continue;
            EnergyTier next = ENERGY_TIERS.get(index + 1);
            rows.add(new Row(
                    Category.ENERGY_NODES,
                    "energy_storage_" + tier.id(),
                    VANILLA_SHAPED,
                    BATTERY_BOX_PATTERN,
                    keys(Map.of(
                            'C', conductor("cable", "04", tier.conductor()),
                            'M', Ingredient.item("gt6ce:transformer_" + tier.id() + "_" + next.id()),
                            'W', conductor("wire", "04", tier.conductor()),
                            'X', Ingredient.tag(ElectricalCraftingRules.circuitTag(tier.circuit()))))));
            rows.add(toolRow(
                    Category.ENERGY_NODES,
                    "transformer_" + tier.id() + "_" + next.id(),
                    TRANSFORMER_PATTERN,
                    keys(Map.of(
                            'I', Ingredient.tag("gt6ce:plate_double/any_iron_or_steel"),
                            'M', Ingredient.item("gt6ce:casing_machine_" + tier.casing()),
                            'W', conductor("wire", "01", tier.transformerConductor()),
                            'X', conductor("wire", "04", tier.transformerConductor()),
                            'x', Ingredient.item(WIRE_CUTTER)))));
        }
    }

    // ---------------------------------------------------------------- long_distance

    private static final String LONG_DISTANCE_PLASTIC = "gt6ce:long_distance/plate/any_plastic";

    /** A long-distance endpoint: the pipe it accepts and the machine casing it is built on. */
    private record LongDistanceEndpoint(String id, String pipe, String casing) {}

    private static final List<LongDistanceEndpoint> LONG_DISTANCE_ENDPOINTS = List.of(
            new LongDistanceEndpoint(
                    "long_dist_endpoint_fluid", "gt6ce:pipe_medium_tungsten", "gt6ce:casing_machine_tungsten"),
            new LongDistanceEndpoint(
                    "long_dist_endpoint_item", "gt6ce:item_pipe_medium_platinum", "gt6ce:casing_machine_platinum"));

    /** A long-distance pipe segment and the pipe it is made of. */
    private record LongDistancePipe(String id, Ingredient pipe) {}

    private static final List<LongDistancePipe> LONG_DISTANCE_PIPES = List.of(
            new LongDistancePipe("long_dist_pipe_adamantium", Ingredient.item("gt6ce:pipe_medium_adamantium")),
            new LongDistancePipe("long_dist_pipe_fluid", Ingredient.item("gt6ce:pipe_medium_stainless_steel")),
            new LongDistancePipe("long_dist_pipe_item", Ingredient.item("gt6ce:item_pipe_medium_electrum")),
            new LongDistancePipe(
                    "long_dist_pipe_tungsten", Ingredient.tag("gt6ce:long_distance/pipe_medium/any_tungsten")));

    /** A long-distance transformer and the HV transformer it is built from. */
    private record LongDistanceTransformer(String id, String transformer) {}

    private static final List<LongDistanceTransformer> LONG_DISTANCE_TRANSFORMERS = List.of(
            new LongDistanceTransformer("long_dist_transformer_ulv", "gt6ce:transformer_ev_iv"),
            new LongDistanceTransformer("long_dist_transformer_lv", "gt6ce:transformer_iv_luv"),
            new LongDistanceTransformer("long_dist_transformer_mv", "gt6ce:transformer_luv_zpm"),
            new LongDistanceTransformer("long_dist_transformer_uv", "gt6ce:transformer_uv_xv"),
            new LongDistanceTransformer("long_dist_transformer_zpm", "gt6ce:transformer_zpm_uv"));

    /** A long-distance wire and the 16x wire it is made of. */
    private record LongDistanceWire(String id, Ingredient wire) {}

    private static final List<LongDistanceWire> LONG_DISTANCE_WIRES = List.of(
            new LongDistanceWire("long_dist_wire_aluminium", Ingredient.item("gt6ce:wire_16_aluminium")),
            new LongDistanceWire("long_dist_wire_blue_alloy", Ingredient.item("gt6ce:wire_16_blue_alloy")),
            new LongDistanceWire(
                    "long_dist_wire_electrotine_alloy", Ingredient.item("gt6ce:wire_16_electrotine_alloy")),
            new LongDistanceWire("long_dist_wire_electrum", Ingredient.item("gt6ce:wire_16_electrum")),
            new LongDistanceWire("long_dist_wire_ev", Ingredient.item("gt6ce:wire_16_tin")),
            new LongDistanceWire("long_dist_wire_gold", Ingredient.item("gt6ce:wire_16_gold")),
            new LongDistanceWire("long_dist_wire_graphene", Ingredient.item("gt6ce:wire_16_graphene")),
            new LongDistanceWire("long_dist_wire_iv", Ingredient.tag("gt6ce:long_distance/wire_16/any_copper")),
            new LongDistanceWire("long_dist_wire_lead", Ingredient.item("gt6ce:wire_16_lead")),
            new LongDistanceWire("long_dist_wire_luv", Ingredient.item("gt6ce:wire_16_silver")),
            new LongDistanceWire("long_dist_wire_naquadah", Ingredient.item("gt6ce:wire_16_naquadah")),
            new LongDistanceWire("long_dist_wire_platinum", Ingredient.item("gt6ce:wire_16_platinum")),
            new LongDistanceWire("long_dist_wire_tungsten", Ingredient.tag("gt6ce:long_distance/wire_16/any_tungsten")),
            new LongDistanceWire("long_dist_wire_tungsten_steel", Ingredient.item("gt6ce:wire_16_tungsten_steel")),
            new LongDistanceWire("long_dist_wire_uv", Ingredient.item("gt6ce:wire_16_osmium")),
            new LongDistanceWire("long_dist_wire_zpm", Ingredient.tag("gt6ce:long_distance/wire_16/any_steel")));

    private static void longDistance(List<Row> rows) {
        for (LongDistanceEndpoint endpoint : LONG_DISTANCE_ENDPOINTS) {
            rows.add(toolRow(
                    Category.LONG_DISTANCE,
                    endpoint.id(),
                    List.of("ZPZ", "PMP", "ZPZ"),
                    keys(Map.of(
                            'M', Ingredient.item(endpoint.casing()),
                            'P', Ingredient.item(endpoint.pipe()),
                            'Z', Ingredient.tag(LONG_DISTANCE_PLASTIC)))));
        }
        for (LongDistancePipe pipe : LONG_DISTANCE_PIPES) {
            rows.add(toolRow(
                    Category.LONG_DISTANCE,
                    pipe.id(),
                    List.of("SPS", "PwP", "SPS"),
                    keys(Map.of(
                            'P', pipe.pipe(),
                            'S', Ingredient.tag(LONG_DISTANCE_PLASTIC),
                            'w', Ingredient.item(WRENCH)))));
        }
        for (LongDistanceTransformer transformer : LONG_DISTANCE_TRANSFORMERS) {
            rows.add(toolRow(
                    Category.LONG_DISTANCE,
                    transformer.id(),
                    List.of("WMW", "MxM", "WMW"),
                    keys(Map.of(
                            'M', Ingredient.item(transformer.transformer()),
                            'W', Ingredient.item("gt6ce:cable_04_annealed_copper"),
                            'x', Ingredient.item(WIRE_CUTTER)))));
        }
        for (LongDistanceWire wire : LONG_DISTANCE_WIRES) {
            rows.add(toolRow(
                    Category.LONG_DISTANCE,
                    wire.id(),
                    List.of("RSR", "PWP", "RSR"),
                    keys(Map.of(
                            'P', Ingredient.tag("gt6ce:long_distance/plate_curved/any_copper"),
                            'R', Ingredient.tag("gt6ce:plate/rubber"),
                            'S', Ingredient.tag("gt6ce:plate_curved/aluminium"),
                            'W', wire.wire()))));
        }
    }

    // Declared last: expanding the tables above reads them during class initialisation.
    /** Every row of the four categories, in directory order. */
    public static final List<Row> ROWS = build();

    private static List<Row> build() {
        List<Row> rows = new ArrayList<>(126);
        batteries(rows);
        engines(rows);
        energyNodes(rows);
        longDistance(rows);
        return List.copyOf(rows);
    }
}
