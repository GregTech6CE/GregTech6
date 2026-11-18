package gregtech.content.data;

import static gregtech.content.data.BasicMachineCraftingRecipes.ComponentType.MOTORS;
import static gregtech.content.data.BasicMachineCraftingRecipes.ItemRef.*;
import static gregtech.content.data.MaterialPrefix.*;
import static gregtech.content.material.Materials.*;
import static gregtech.engine.machine.PipeSpec.PipeSize.*;
import static gregtech.engine.tool.ToolDefinition.*;

import gregtech.content.data.BasicMachineCraftingRecipes.*;
import gregtech.engine.prefix.BlockMaterialPrefix;
import java.util.List;
import java.util.Map;

/**
 * Crafting recipes for the multiblock machines and their structural parts, copied from the
 * original GregTech 6 {@code Loader_MultiTileEntities} Multiblock Machines and Logistics registrations.
 * Original 170xx/17996 controllers retain their GT6 ingredients. Axial controllers use their
 * canonical 172xx identities.
 */
public final class MultiblockCraftingRecipes {
    public record Entry(String blockId, String originalName, Recipe recipe) {
        private Entry(String blockId, String originalName, List<String> rows, Map<Character, Ingredient> keys) {
            this(blockId, originalName, new Recipe(rows, keys));
        }
    }

    public static final List<Entry> ENTRIES = List.of(
            new Entry(
                    "distillation_tower_main",
                    "Distillation Tower",
                    List.of("PPP", "PMP", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:distillation_tower_part"),
                            'P', new Pipe(NONUPLE, StainlessSteel))),
            new Entry(
                    "cryo_distillation_main",
                    "Cryo Distillation Tower",
                    List.of("PPP", "PMP", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:distillation_tower_part"),
                            'P', new Pipe(NONUPLE, Copper))),
            new Entry(
                    "fusion_reactor_main",
                    "Fusion Reactor",
                    List.of("FFF", "FMF", "FFF"),
                    Map.of(
                            'F', new RegistryItem("gt6ce:compact_force_field_emitter_iv"),
                            'M', new RegistryItem("gt6ce:tungstensteel_wall"))),
            new Entry(
                    "largecentrifuge_tungsten_steel",
                    "Large Centrifuge",
                    List.of("CMC", "RCR"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new RegistryItem("gt6ce:centrifuge_part"),
                            'R', CRYSTAL_PROCESSOR_RUBY)),
            new Entry(
                    "largeelectrolyzer_stainless_steel",
                    "Large Electrolyzer",
                    List.of("CMC", "RCR"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new RegistryItem("gt6ce:electrolyzer_part"),
                            'R', CRYSTAL_PROCESSOR_RUBY)),
            new Entry(
                    "largecoagulator_stainless_steel",
                    "Large Coagulator Array",
                    List.of("CRC", "PMP", "PPP"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new RegistryItem("gt6ce:tank_wall"),
                            'P', new Form(plateDense, StainlessSteel, null),
                            'R', CRYSTAL_PROCESSOR_RUBY)),
            new Entry(
                    "largeautoclave_stainless_steel",
                    "Large Autoclave",
                    List.of("CRC", "PMP", "PPP"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new RegistryItem("gt6ce:tank_wall_dense"),
                            'P', new Form(plateDense, StainlessSteel, null),
                            'R', CRYSTAL_PROCESSOR_RUBY)),
            new Entry(
                    "largebath_stainless_steel",
                    "Large Bathing Vat",
                    List.of("CRC", "PMP", "APA"),
                    Map.of(
                            'A', new RegistryItem("gt6ce:compact_robot_arm_mv"),
                            'C', new Circuit(6),
                            'M', new RegistryItem("gt6ce:tank_wall"),
                            'P', new Form(plateDense, StainlessSteel, null),
                            'R', CRYSTAL_PROCESSOR_RUBY)),
            new Entry(
                    "largemixer_stainless_steel",
                    "Large Batch Mixer",
                    List.of("PSP", "PSP", "RMC"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new RegistryItem("gt6ce:tank_wall"),
                            'P', new Form(plateDense, StainlessSteel, null),
                            'R', CRYSTAL_PROCESSOR_RUBY,
                            'S', new Form(stickLong, StainlessSteel, null))),
            new Entry(
                    "largefermenter_stainless_steel",
                    "Large Fermenter",
                    List.of("PPP", "CRC", "PMP"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new RegistryItem("gt6ce:tank_wall"),
                            'P', new Form(plateDense, StainlessSteel, null),
                            'R', CRYSTAL_PROCESSOR_RUBY)),
            new Entry(
                    "largeoven_invar",
                    "Large Electric Oven",
                    List.of("PPP", "PwP", "RMC"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new RegistryItem("gt6ce:invar_wall"),
                            'P', new Form(plateDense, Invar, null),
                            'R', CRYSTAL_PROCESSOR_RUBY,
                            'w', new Tool(WRENCH))),
            new Entry(
                    "largesluice_titanium",
                    "Large Sluice",
                    List.of("GGG", "SwS", "RMC"),
                    Map.of(
                            'C', new Circuit(6),
                            'G', new Form(gearGt, Titanium, null),
                            'M', new RegistryItem("gt6ce:titanium_wall"),
                            'R', CRYSTAL_PROCESSOR_RUBY,
                            'S', new Form(stick, Titanium, null),
                            'w', new Tool(WRENCH))),
            new Entry(
                    "largecrusher_tungsten_steel",
                    "Large Crusher",
                    List.of("GSG", "SGS", "RMC"),
                    Map.of(
                            'C', new Circuit(6),
                            'G', new Form(gearGt, Tungstensteel, null),
                            'M', new RegistryItem("gt6ce:tungstensteel_wall"),
                            'R', CRYSTAL_PROCESSOR_RUBY,
                            'S', new Form(gearGtSmall, Tungstensteel, null))),
            new Entry(
                    "largeshredder_tungsten_steel",
                    "Large Shredder",
                    List.of("SGS", "GSG", "RMC"),
                    Map.of(
                            'C', new Circuit(6),
                            'G', new Form(gearGt, Tungstensteel, null),
                            'M', new RegistryItem("gt6ce:tungstensteel_wall"),
                            'R', CRYSTAL_PROCESSOR_RUBY,
                            'S', new Form(gearGtSmall, Tungstensteel, null))),
            new Entry(
                    "largesqueezer_steel",
                    "Large Squeezer",
                    List.of("GSG", "GSG", "RMC"),
                    Map.of(
                            'C', new Circuit(6),
                            'G', new Form(gearGt, Steel, null),
                            'M', new RegistryItem("gt6ce:steel_wall"),
                            'R', CRYSTAL_PROCESSOR_RUBY,
                            'S', new Form(gearGtSmall, Steel, null))),
            new Entry(
                    "largemassfab_lead",
                    "Large Matter Fabricator",
                    List.of("FFF", "FMF", "FFF"),
                    Map.of(
                            'F', new RegistryItem("gt6ce:compact_force_field_emitter_iv"),
                            'M', new RegistryItem("gt6ce:dense_lead_wall"))),
            new Entry(
                    "heat_exchanger_main",
                    "Large Heat Exchanger",
                    List.of("DDD", "PMP", "DDD"),
                    Map.of(
                            'D', new Form(plateDense, AnnealedCopper, null),
                            'M', new RegistryItem("gt6ce:dense_tungsten_wall"),
                            'P', new Pipe(HUGE, Copper))),
            new Entry(
                    "coke_oven_main",
                    "Coke Oven",
                    List.of("hRT", "PMR", "dRT"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:fire_bricks"),
                            'P', new RegistryTag("gt6ce:technology/any_iron_plate"),
                            'R', new RegistryTag("gt6ce:technology/any_iron_rod"),
                            'T', new RegistryTag("gt6ce:technology/any_iron_screw"),
                            'd', new Tool(SCREWDRIVER),
                            'h', new Tool(HARD_HAMMER))),
            new Entry(
                    "implosion_compressor_main",
                    "Implosion Compressor",
                    List.of("CPC", "PAP", "RMR"),
                    Map.of(
                            'A', new RegistryItem("gt6ce:compact_robot_arm_mv"),
                            'C', new Circuit(6),
                            'M', new RegistryItem("gt6ce:implosion_compressor_wall"),
                            'P', new Form(plateDense, Tungstensteel, null),
                            'R', CRYSTAL_PROCESSOR_RUBY)),
            new Entry(
                    "wood_tank_main_valve",
                    "Wood Tank Main Valve",
                    List.of(" R ", "rMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:wood_wall"),
                            'R', new Form(ring, Lead, null),
                            'r', new Tool(SOFT_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "small_stainless_steel_tank_main_valve",
                    "Small Stainless Steel Tank Main Valve",
                    List.of(" R ", "hMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:tank_wall"),
                            'R', new Form(ring, StainlessSteel, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "small_tungstensteel_tank_main_valve",
                    "Small Tungstensteel Tank Main Valve",
                    List.of(" R ", "hMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:tungstensteel_wall"),
                            'R', new Form(ring, Tungstensteel, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "small_tungsten_tank_main_valve",
                    "Small Tungsten Tank Main Valve",
                    List.of(" R ", "hMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:tungsten_wall"),
                            'R', new Form(ring, Tungsten, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "small_adamantium_tank_main_valve",
                    "Small Adamantium Tank Main Valve",
                    List.of(" R ", "hMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:adamantium_wall"),
                            'R', new Form(ring, Adamantium, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "small_titanium_tank_main_valve",
                    "Small Titanium Tank Main Valve",
                    List.of(" R ", "hMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:titanium_wall"),
                            'R', new Form(ring, Titanium, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "small_invar_tank_main_valve",
                    "Small Invar Tank Main Valve",
                    List.of(" R ", "hMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:invar_wall"),
                            'R', new Form(ring, Invar, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "small_dense_stainless_steel_tank_main_valve",
                    "Small Dense Stainless Steel Tank Main Valve",
                    List.of(" R ", "hMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:tank_wall_dense"),
                            'R', new Form(ring, StainlessSteel, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "small_dense_tungstensteel_tank_main_valve",
                    "Small Dense Tungstensteel Tank Main Valve",
                    List.of(" R ", "hMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:implosion_compressor_wall"),
                            'R', new Form(ring, Tungstensteel, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "small_dense_tungsten_tank_main_valve",
                    "Small Dense Tungsten Tank Main Valve",
                    List.of(" R ", "hMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:dense_tungsten_wall"),
                            'R', new Form(ring, Tungsten, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "small_dense_adamantium_tank_main_valve",
                    "Small Dense Adamantium Tank Main Valve",
                    List.of(" R ", "hMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:dense_adamantium_wall"),
                            'R', new Form(ring, Adamantium, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "small_dense_titanium_tank_main_valve",
                    "Small Dense Titanium Tank Main Valve",
                    List.of(" R ", "hMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:dense_titanium_wall"),
                            'R', new Form(ring, Titanium, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "small_dense_invar_tank_main_valve",
                    "Small Dense Invar Tank Main Valve",
                    List.of(" R ", "hMs", " R "),
                    Map.of(
                            'M', new RegistryItem("gt6ce:dense_invar_wall"),
                            'R', new Form(ring, Invar, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "large_stainless_steel_tank_main_valve",
                    "Large Stainless Steel Tank Main Valve",
                    List.of("PPP", "hMs", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:small_stainless_steel_tank_main_valve"),
                            'P', new Form(plate, StainlessSteel, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "large_tungstensteel_tank_main_valve",
                    "Large Tungstensteel Tank Main Valve",
                    List.of("PPP", "hMs", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:small_tungstensteel_tank_main_valve"),
                            'P', new Form(plate, Tungstensteel, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "large_tungsten_tank_main_valve",
                    "Large Tungsten Tank Main Valve",
                    List.of("PPP", "hMs", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:small_tungsten_tank_main_valve"),
                            'P', new Form(plate, Tungsten, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "large_adamantium_tank_main_valve",
                    "Large Adamantium Tank Main Valve",
                    List.of("PPP", "hMs", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:small_adamantium_tank_main_valve"),
                            'P', new Form(plate, Adamantium, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "large_titanium_tank_main_valve",
                    "Large Titanium Tank Main Valve",
                    List.of("PPP", "hMs", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:small_titanium_tank_main_valve"),
                            'P', new Form(plate, Titanium, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "large_invar_tank_main_valve",
                    "Large Invar Tank Main Valve",
                    List.of("PPP", "hMs", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:small_invar_tank_main_valve"),
                            'P', new Form(plate, Invar, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "large_dense_stainless_steel_tank_main_valve",
                    "Large Dense Stainless Steel Tank Main Valve",
                    List.of("PPP", "hMs", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:small_dense_stainless_steel_tank_main_valve"),
                            'P', new Form(plateDense, StainlessSteel, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "large_dense_tungstensteel_tank_main_valve",
                    "Large Dense Tungstensteel Tank Main Valve",
                    List.of("PPP", "hMs", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:small_dense_tungstensteel_tank_main_valve"),
                            'P', new Form(plateDense, Tungstensteel, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "large_dense_tungsten_tank_main_valve",
                    "Large Dense Tungsten Tank Main Valve",
                    List.of("PPP", "hMs", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:small_dense_tungsten_tank_main_valve"),
                            'P', new Form(plateDense, Tungsten, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "large_dense_adamantium_tank_main_valve",
                    "Large Dense Adamantium Tank Main Valve",
                    List.of("PPP", "hMs", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:small_dense_adamantium_tank_main_valve"),
                            'P', new Form(plateDense, Adamantium, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "large_dense_titanium_tank_main_valve",
                    "Large Dense Titanium Tank Main Valve",
                    List.of("PPP", "hMs", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:small_dense_titanium_tank_main_valve"),
                            'P', new Form(plateDense, Titanium, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "large_dense_invar_tank_main_valve",
                    "Large Dense Invar Tank Main Valve",
                    List.of("PPP", "hMs", "PPP"),
                    Map.of(
                            'M', new RegistryItem("gt6ce:small_dense_invar_tank_main_valve"),
                            'P', new Form(plateDense, Invar, null),
                            'h', new Tool(HARD_HAMMER),
                            's', new Tool(SAW))),
            new Entry(
                    "von_da_graagg_generator",
                    "Von da Graagg Generator",
                    List.of("CSC", "PMP", "CEC"),
                    Map.of(
                            'C', new Circuit(6),
                            'E', new RegistryItem("minecraft:ender_eye"),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachine, SteelGalvanized),
                            'P', CRYSTAL_PROCESSOR_RUBY,
                            'S', new RegistryItem("minecraft:nether_star"))),
            new Entry(
                    "lightning_rod_electric_output",
                    "Lightning Rod Electric Output",
                    List.of("CWC", "PMP", "CWC"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachine, Tungsten),
                            'P', CRYSTAL_PROCESSOR_SAPPHIRE,
                            'W', new Wire(16, NiobiumTitanium))),
            new Entry(
                    "bedrock_mining_drill_controller",
                    "Bedrock Mining Drill Controller",
                    List.of("PYP", "CMC", "GIG"),
                    Map.of(
                            'C', new Circuit(6),
                            'G', new Form(gearGt, Tungstensteel, null),
                            'I', new Form(toolHeadDrill, Tungstensteel, null),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachineDense, Titanium),
                            'P', CRYSTAL_PROCESSOR_RUBY,
                            'Y', new RegistryItem("gt6ce:compact_electric_conveyor_iv"))),
            new Entry(
                    "tungsten_wall",
                    "Tungsten Wall",
                    List.of("wPP", "hPP"),
                    Map.of(
                            'P', new Form(plate, Tungsten, null),
                            'h', new Tool(HARD_HAMMER),
                            'w', new Tool(WRENCH))),
            new Entry(
                    "adamantium_wall",
                    "Adamantium Wall",
                    List.of("wPP", "hPP"),
                    Map.of(
                            'P', new Form(plate, Adamantium, null),
                            'h', new Tool(HARD_HAMMER),
                            'w', new Tool(WRENCH))),
            new Entry(
                    "centrifuge_part",
                    "Centrifuge Part",
                    List.of("TwT", "GMG", "TdT"),
                    Map.of(
                            'G', new Form(gearGt, Tungstensteel, null),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachine, Tungstensteel),
                            'T', new Form(screw, Tungstensteel, null),
                            'd', new Tool(SCREWDRIVER),
                            'w', new Tool(WRENCH))),
            new Entry(
                    "electrolyzer_part",
                    "Electrolyzer Part",
                    List.of("WwW", "WMW", "CCC"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachine, StainlessSteel),
                            'W', new Wire(1, Platinum),
                            'w', new Tool(WRENCH))),
            new Entry(
                    "heat_transmitter",
                    "Heat Transmitter",
                    List.of("MPM", "hRw", "MPM"),
                    Map.of(
                            'M', new Form(plate, Invar, null),
                            'P', new Form(plateTriple, Copper, null),
                            'R', new Form(stickLong, Copper, null),
                            'h', new Tool(HARD_HAMMER),
                            'w', new Tool(WRENCH))),
            new Entry(
                    "distillation_tower_part",
                    "Distillation Tower Part",
                    List.of("TMT", "hPd", "TMT"),
                    Map.of(
                            'M', new Form(plate, StainlessSteel, null),
                            'P', new Pipe(SMALL, StainlessSteel),
                            'T', new Form(screw, StainlessSteel, null),
                            'd', new Tool(SCREWDRIVER),
                            'h', new Tool(HARD_HAMMER))),
            new Entry(
                    "sluice_part",
                    "Sluice Part",
                    List.of("GGG", "PMP", "PwP"),
                    Map.of(
                            'G', new Form(gearGt, Titanium, null),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachine, Titanium),
                            'P', new Pipe(MEDIUM, Titanium),
                            'w', new Tool(WRENCH))),
            new Entry(
                    "crusher_wheels",
                    "Crusher Wheels",
                    List.of("DDD", "GDG", "GMG"),
                    Map.of(
                            'D', new Form(gem, Diamond, null),
                            'G', new Form(gearGt, Tungstensteel, null),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachineDouble, Tungstensteel))),
            new Entry(
                    "shredder_blades",
                    "Shredder Blades",
                    List.of("DGD", "GwG", "GMG"),
                    Map.of(
                            'D', new Form(plateGem, Diamond, null),
                            'G', new Form(gearGt, Tungstensteel, null),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachineDouble, Tungstensteel),
                            'w', new Tool(WRENCH))),
            new Entry(
                    "large_niobium_titanium_coil",
                    "Large Niobium-Titanium Coil",
                    List.of("WWW", "WxW", "WWW"),
                    Map.of(
                            'W', new Wire(4, NiobiumTitanium),
                            'x', new Tool(WIRE_CUTTER))),
            new Entry(
                    "large_nichrome_coil",
                    "Large Nichrome Coil",
                    List.of("WWW", "WxW", "WWW"),
                    Map.of(
                            'W', new Wire(4, Nichrome),
                            'x', new Tool(WIRE_CUTTER))),
            new Entry(
                    "large_carborundum_coil",
                    "Large Carborundum Coil",
                    List.of("WWW", "WxW", "WWW"),
                    Map.of(
                            'W', new Wire(4, Carborundum),
                            'x', new Tool(WIRE_CUTTER))),
            new Entry(
                    "large_osmium_coil",
                    "Large Osmium Coil",
                    List.of("WWW", "WxW", "WWW"),
                    Map.of(
                            'W', new Wire(4, OsmiumElemental),
                            'x', new Tool(WIRE_CUTTER))),
            new Entry(
                    "large_iridium_coil",
                    "Large Iridium Coil",
                    List.of("WWW", "WxW", "WWW"),
                    Map.of(
                            'W', new Wire(4, Iridium),
                            'x', new Tool(WIRE_CUTTER))),
            new Entry(
                    "versatile_processor_unit",
                    "Versatile Quadcore Processor Unit",
                    List.of("DCS", "CMC", "RCE"),
                    Map.of(
                            'C', new Circuit(6),
                            'D', CRYSTAL_PROCESSOR_DIAMOND,
                            'E', CRYSTAL_PROCESSOR_EMERALD,
                            'M', new BlockForm(BlockMaterialPrefix.casingMachine, SteelGalvanized),
                            'R', CRYSTAL_PROCESSOR_RUBY,
                            'S', CRYSTAL_PROCESSOR_SAPPHIRE)),
            new Entry(
                    "logic_processor_unit",
                    "Logic Quadcore Processor Unit",
                    List.of("PCP", "CMC", "PCP"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachine, SteelGalvanized),
                            'P', CRYSTAL_PROCESSOR_DIAMOND)),
            new Entry(
                    "control_processor_unit",
                    "Control Quadcore Processor Unit",
                    List.of("PCP", "CMC", "PCP"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachine, SteelGalvanized),
                            'P', CRYSTAL_PROCESSOR_RUBY)),
            new Entry(
                    "conversion_processor_unit",
                    "Conversion Quadcore Processor Unit",
                    List.of("PCP", "CMC", "PCP"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachine, SteelGalvanized),
                            'P', CRYSTAL_PROCESSOR_SAPPHIRE)),
            new Entry(
                    "fusion_ventilation_unit",
                    "Ventilation Unit",
                    List.of("FwF", "CMC", "EdE"),
                    Map.of(
                            'C', new Circuit(3),
                            'E', new Component(MOTORS, 1),
                            'F', new RegistryItem("gt6ce:air_vent"),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachine, SteelGalvanized),
                            'd', new Tool(SCREWDRIVER),
                            'w', new Tool(WRENCH))),
            new Entry(
                    "logistics_core",
                    "Logistics Core",
                    List.of("CCC", "PMP", "CCC"),
                    Map.of(
                            'C', new Circuit(6),
                            'M', new BlockForm(BlockMaterialPrefix.casingMachine, SteelGalvanized),
                            'P', CRYSTAL_PROCESSOR_EMERALD)),
            new Entry(
                    "logistics_wire",
                    "Logistics Wire",
                    List.of("PEP", "dFx", "POP"),
                    Map.of(
                            'E', new Form(gem, Emerald, null),
                            'F', new RegistryItem("gt6ce:laser_fiber_wire"),
                            'O', new Form(wireFine, OsmiumElemental, null),
                            'P', new Form(foil, Plastic, null),
                            'd', new Tool(SCREWDRIVER),
                            'x', new Tool(WIRE_CUTTER))));

    /** Recipe entries for one port block id (a block can have at most one). */
    public static Entry find(String blockId) {
        for (Entry entry : ENTRIES) {
            if (entry.blockId().equals(blockId)) return entry;
        }
        return null;
    }

    /** Original GT6 registration names covered by this table. */
    public static java.util.Set<String> coveredMachines() {
        java.util.Set<String> names = new java.util.TreeSet<>();
        for (Entry entry : ENTRIES) names.add(entry.originalName());
        return java.util.Collections.unmodifiableSet(names);
    }

    private MultiblockCraftingRecipes() {}
}
