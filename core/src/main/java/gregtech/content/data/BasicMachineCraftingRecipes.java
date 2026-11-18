/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Shaped patterns from the original Loader_MultiTileEntities. */
package gregtech.content.data;

import static gregtech.content.data.BasicMachineCraftingRecipes.Casing.*;
import static gregtech.content.data.BasicMachineCraftingRecipes.ComponentType.*;
import static gregtech.content.data.BasicMachineCraftingRecipes.ItemRef.*;
import static gregtech.content.data.BasicMachineCraftingRecipes.TagRef.*;
import static gregtech.content.data.MaterialPrefix.*;
import static gregtech.engine.machine.PipeSpec.PipeSize.*;

import gregtech.api.material.GTMaterial;
import gregtech.content.material.Materials;
import gregtech.content.material.generated.OreMaterials;
import gregtech.engine.machine.CrucibleSpec;
import gregtech.engine.machine.OriginalSmelteryDefinitions;
import gregtech.engine.machine.PipeSpec.PipeSize;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.tool.ToolDefinition;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Shaped crafting from GT6 Loader_MultiTileEntities, shared across each machine family's tiers. */
public final class BasicMachineCraftingRecipes {
    private BasicMachineCraftingRecipes() {}

    public record Recipe(List<String> rows, Map<Character, Ingredient> keys) {
        public Recipe {
            rows = List.copyOf(rows);
            keys = Map.copyOf(keys);
            if (rows.isEmpty() || rows.size() > 3)
                throw new IllegalArgumentException("Invalid basic machine crafting height: " + rows);
            int width = rows.get(0).length();
            var symbols = new HashSet<Character>();
            for (String row : rows) {
                if (width == 0 || width > 3 || row.length() != width)
                    throw new IllegalArgumentException("Invalid basic machine crafting width: " + rows);
                for (int i = 0; i < width; i++) {
                    char symbol = row.charAt(i);
                    if (symbol != ' ') symbols.add(symbol);
                }
            }
            if (symbols.isEmpty() || !symbols.equals(keys.keySet()))
                throw new IllegalArgumentException("Invalid basic machine crafting keys for " + rows);
        }
    }

    public sealed interface Ingredient
            permits Form,
                    Casing,
                    Pipe,
                    Wire,
                    TierWire,
                    Component,
                    ItemRef,
                    TagRef,
                    Circuit,
                    Tool,
                    Crucible,
                    Alternatives,
                    FluidContainer {}

    /** Null material means the source casing material. An ANY group recovers its canonical material. */
    public record Form(MaterialPrefix prefix, GTMaterial material, GTMaterial group) implements Ingredient {}

    public enum Casing implements Ingredient {
        STANDARD,
        ROBUST,
        REINFORCED;

        public BlockMaterialPrefix prefix() {
            return switch (this) {
                case STANDARD -> MaterialPrefixes.casingMachine.getBlockDelegate();
                case ROBUST -> MaterialPrefixes.casingMachineDouble.getBlockDelegate();
                case REINFORCED -> MaterialPrefixes.casingMachineQuadruple.getBlockDelegate();
            };
        }
    }

    /** Null material means the source casing material. */
    public record Pipe(PipeSize size, GTMaterial material) implements Ingredient {}

    public record Wire(int size, GTMaterial material) implements Ingredient {}

    public record TierWire(int tier, boolean cable) implements Ingredient {}

    public record Component(ComponentType type, int tier) implements Ingredient {}

    public record Circuit(int tier) implements Ingredient {}

    public record Tool(ToolDefinition type) implements Ingredient {}

    public record Crucible(CrucibleSpec spec) implements Ingredient {}

    public record Alternatives(List<Ingredient> options) implements Ingredient {
        public Alternatives {
            options = List.copyOf(options);
            if (options.isEmpty()) throw new IllegalArgumentException("Missing basic machine crafting alternatives");
        }
    }

    public enum FluidContainer implements Ingredient {
        HONEY;

        public String fluidField() {
            return "Honey";
        }
    }

    public enum ItemRef implements Ingredient {
        BRICKS("minecraft:bricks"),
        HONEY_BOTTLE("minecraft:honey_bottle"),
        CRYSTAL_PROCESSOR_DIAMOND("gt6ce:crystal_processor_diamond"),
        CRYSTAL_PROCESSOR_EMERALD("gt6ce:crystal_processor_emerald"),
        CRYSTAL_PROCESSOR_RUBY("gt6ce:crystal_processor_ruby"),
        CRYSTAL_PROCESSOR_SAPPHIRE("gt6ce:crystal_processor_sapphire"),
        LASER_EMITTER_ARGON("gt6ce:laser_emitter_argon"),
        LASER_EMITTER_KRYPTON("gt6ce:laser_emitter_krypton"),
        LASER_EMITTER_XENON("gt6ce:laser_emitter_xenon");

        private final String id;

        ItemRef(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public enum TagRef implements Ingredient {
        SANDSTONE("forge:sandstone"),
        GLASS_COLORLESS("gt6ce:glass_colorless"),
        HARDENED_CLAY("gt6ce:crafting_hardened_clay"),
        LENS("gt6ce:lens"),
        SAPLINGS("gt6ce:saplings"),
        IRON_MACHINE_CASING("gt6ce:casing_machine/any_iron_or_steel"),
        IRON_PIPE_MEDIUM("gt6ce:pipe_medium/any_iron_or_steel"),
        IRON_WIRE_01("gt6ce:wire_01/any_iron_or_steel"),
        IRON_WIRE_02("gt6ce:wire_02/any_iron_or_steel"),
        IRON_WIRE_04("gt6ce:wire_04/any_iron_or_steel"),
        IRON_WIRE_08("gt6ce:wire_08/any_iron_or_steel"),
        IRON_WIRE_16("gt6ce:wire_16/any_iron_or_steel");

        private final String id;

        TagRef(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public enum ComponentType {
        MOTORS("compact_electric_motor_"),
        PUMPS("compact_electric_pump_"),
        PISTONS("compact_electric_piston_"),
        EMITTERS("compact_signal_emitter_"),
        CONVEYERS("compact_electric_conveyor_"),
        SENSORS("compact_sensor_"),
        ROBOT_ARMS("compact_robot_arm_"),
        FIELD_GENERATORS("compact_force_field_emitter_");

        private final String itemPrefix;

        ComponentType(String itemPrefix) {
            this.itemPrefix = itemPrefix;
        }

        public String itemPrefix() {
            return itemPrefix;
        }
    }

    private static final Tool HAMMER = new Tool(ToolDefinition.HARD_HAMMER);
    private static final Tool WRENCH = new Tool(ToolDefinition.WRENCH);
    private static final Tool SCREWDRIVER = new Tool(ToolDefinition.SCREWDRIVER);
    private static final Tool WIRE_CUTTER = new Tool(ToolDefinition.WIRE_CUTTER);

    public static Recipe oven(int tier) {
        return new Recipe(
                List.of("wMh", "BCB"),
                Map.of(
                        'B', BRICKS,
                        'C', anyCopper(plateDouble),
                        'M', STANDARD,
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe roaster(int tier) {
        return new Recipe(
                List.of("wPh", "PMP", "BCB"),
                Map.of(
                        'B', BRICKS,
                        'C', anyCopper(plateDouble),
                        'M', STANDARD,
                        'P', new Pipe(MEDIUM, null),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe distillery(int tier) {
        return new Recipe(
                List.of("GPG", "WMW", "hCw"),
                Map.of(
                        'C', anyCopper(plateDouble),
                        'G', GLASS_COLORLESS,
                        'M', STANDARD,
                        'P', new Pipe(pipeRank(tier), Materials.StainlessSteel),
                        'W',
                                switch (tier) {
                                    case 1 -> new Wire(2, Materials.Constantan);
                                    case 2 -> new Wire(4, Materials.Kanthal);
                                    case 3 -> new Wire(8, Materials.Nichrome);
                                    default -> new Wire(16, Materials.Carborundum);
                                },
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe extruder(int tier) {
        return new Recipe(
                List.of("GPw", "PMS", "GPD"),
                Map.of(
                        'D',
                        anyDiamond(dust),
                        'G',
                        form(gearGt),
                        'M',
                        ROBUST,
                        'P',
                        tier == 1 ? anySteel(plate) : form(plateRank(tier), Materials.TungstenCarbide),
                        'S',
                        tier == 1 ? anySteel(toolHeadBuzzSaw) : form(toolHeadBuzzSaw, Materials.TungstenCarbide),
                        'w',
                        WRENCH));
    }

    public static Recipe smelter(int tier) {
        return new Recipe(
                List.of("wUh", "PMP", "BCB"),
                Map.of(
                        'B', BRICKS,
                        'C', anyCopper(plateDouble),
                        'M', STANDARD,
                        'P', new Pipe(MEDIUM, null),
                        'U',
                                switch (tier) {
                                    case 1 -> crucible(1024);
                                    case 2, 3 -> crucible(1019);
                                    default -> crucible(1043);
                                },
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe crystallisationCrucible(int tier) {
        return new Recipe(
                List.of("wUh", "PMP", "BCB"),
                Map.of(
                        'B', BRICKS,
                        'C', anyCopper(plateDouble),
                        'M', ROBUST,
                        'P', new Pipe(MEDIUM, null),
                        'U', tier == 1 ? crucible(1018) : crucible(1039),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe dryer(int tier) {
        return new Recipe(
                List.of("wPh", "BMB", "BCB"),
                Map.of(
                        'B', BRICKS,
                        'C', anyCopper(plateDouble),
                        'M', STANDARD,
                        'P', new Pipe(MEDIUM, null),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe laminator(int tier) {
        return new Recipe(
                List.of("SwS", "GMG", "SCS"),
                Map.of(
                        'C', anyCopper(plateDouble),
                        'G', form(gearGtSmall),
                        'M', STANDARD,
                        'S', form(stick),
                        'w', WRENCH));
    }

    public static Recipe catalyticCracker(int tier) {
        return new Recipe(
                List.of("IPI", "ZMZ", "ICI"),
                Map.of(
                        'C', anyCopper(plateRank(tier + 1)),
                        'I', form(plateRank(tier + 1), Materials.Invar),
                        'M', ROBUST,
                        'P', new Pipe(QUADRUPLE, null),
                        'Z', form(dust, OreMaterials.Zeolite)));
    }

    public static Recipe steamCracker(int tier) {
        return new Recipe(
                List.of("IwI", "PMP", "ICI"),
                Map.of(
                        'C',
                        anyCopper(plateRank(tier + 1)),
                        'I',
                        form(plateRank(tier + 1), Materials.Invar),
                        'M',
                        ROBUST,
                        'P',
                        new Pipe(MEDIUM, null),
                        'w',
                        WRENCH));
    }

    public static Recipe shredder(int tier) {
        return new Recipe(
                List.of("GDG", "hMw"),
                Map.of(
                        'D', anyDiamond(plateGem),
                        'G', form(gearGt),
                        'M', ROBUST,
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe lathe(int tier) {
        return new Recipe(
                List.of("TDS", "dMG"),
                Map.of(
                        'D', anyDiamond(plateGemTiny),
                        'G', form(gearGt),
                        'M', ROBUST,
                        'S', form(gearGtSmall),
                        'T', form(screw),
                        'd', SCREWDRIVER));
    }

    public static Recipe buzzsaw(int tier) {
        return new Recipe(
                List.of("DGS", "wMS"),
                Map.of(
                        'D',
                        anyDiamond(dust),
                        'G',
                        tier == 1 ? anySteel(toolHeadBuzzSaw) : form(toolHeadBuzzSaw, Materials.CobaltBrass),
                        'M',
                        ROBUST,
                        'S',
                        form(gearGtSmall),
                        'w',
                        WRENCH));
    }

    public static Recipe centrifuge(int tier) {
        return new Recipe(
                List.of("Gw", "SM", "Gh"),
                Map.of(
                        'G', form(gearGt),
                        'M', ROBUST,
                        'S', form(stickLong),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe rollingMill(int tier) {
        return new Recipe(
                List.of("Gh", "M ", "Gw"),
                Map.of(
                        'G', form(gearGt),
                        'M', ROBUST,
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe rollBender(int tier) {
        return new Recipe(
                List.of("wS ", "GMG", " Sh"),
                Map.of(
                        'G', form(gearGt),
                        'M', ROBUST,
                        'S', form(gearGtSmall),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe rollFormer(int tier) {
        return new Recipe(
                List.of("wG ", "GMG", " Gh"),
                Map.of(
                        'G', form(gearGt),
                        'M', ROBUST,
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe clusterMill(int tier) {
        return new Recipe(
                List.of("SSS", "wGh", "SMS"),
                Map.of(
                        'G', form(gearGt),
                        'M', REINFORCED,
                        'S', form(gearGtSmall),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe wireMill(int tier) {
        return new Recipe(
                List.of("SGS", "wMh"),
                Map.of(
                        'G', form(gearGt),
                        'M', STANDARD,
                        'S', form(gearGtSmall),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe mixer(int tier) {
        return new Recipe(
                List.of("PMP", "PRP", "hSw"),
                Map.of(
                        'M',
                        STANDARD,
                        'P',
                        form(plateRank(tier), Materials.StainlessSteel),
                        'R',
                        form(rotor, Materials.StainlessSteel),
                        'S',
                        form(stick),
                        'h',
                        HAMMER,
                        'w',
                        WRENCH));
    }

    public static Recipe loom(int tier) {
        return new Recipe(
                List.of("ShS", "GMG", "SwS"),
                Map.of(
                        'G', form(gearGt),
                        'M', STANDARD,
                        'S', form(stickLong),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe sluice(int tier) {
        return new Recipe(
                List.of("PPP", "RGR", "GMG"),
                Map.of(
                        'G', form(gearGt),
                        'M', ROBUST,
                        'P', form(plateDouble),
                        'R', form(rotor)));
    }

    public static Recipe sander(int tier) {
        return new Recipe(
                List.of("SGS", "XXX", "wMh"),
                Map.of(
                        'G', form(gearGt),
                        'M', ROBUST,
                        'S', form(gearGtSmall),
                        'X', SANDSTONE,
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe burnMixer(int tier) {
        return new Recipe(
                List.of("PMP", "PRP", "hSw"),
                Map.of(
                        'M',
                        STANDARD,
                        'P',
                        form(plateRank(tier), Materials.Invar),
                        'R',
                        form(rotor, Materials.Invar),
                        'S',
                        form(stick),
                        'h',
                        HAMMER,
                        'w',
                        WRENCH));
    }

    public static Recipe debarker(int tier) {
        return new Recipe(
                List.of("RPG", "wMG"),
                Map.of(
                        'G',
                        form(gearGtSmall),
                        'M',
                        STANDARD,
                        'P',
                        new Pipe(pipeRank(tier + 1), Materials.StainlessSteel),
                        'R',
                        form(rotor, Materials.StainlessSteel),
                        'w',
                        WRENCH));
    }

    public static Recipe crusher(int tier) {
        return new Recipe(
                List.of("DMD", "hSw"),
                Map.of(
                        'D', anyDiamond(gem),
                        'M', ROBUST,
                        'S', form(spring),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe sifter(int tier) {
        return new Recipe(
                List.of("WxW", "RMR", "SwS"),
                Map.of(
                        'M',
                        ROBUST,
                        'R',
                        form(stick),
                        'S',
                        form(spring),
                        'W',
                        form(wireFine),
                        'w',
                        WRENCH,
                        'x',
                        WIRE_CUTTER));
    }

    public static Recipe squeezer(int tier) {
        return new Recipe(
                List.of("RS", "PM", "Pw"),
                Map.of(
                        'M', ROBUST,
                        'P', form(plateTriple),
                        'R', form(stick),
                        'S', form(spring),
                        'w', WRENCH));
    }

    public static Recipe compressor(int tier) {
        return new Recipe(
                List.of("PPR", "wMS"),
                Map.of(
                        'M', ROBUST,
                        'P', form(plateQuintuple),
                        'R', form(stick),
                        'S', form(spring),
                        'w', WRENCH));
    }

    public static Recipe press(int tier) {
        return new Recipe(
                List.of("RS", "PM", "Pw"),
                Map.of(
                        'M', STANDARD,
                        'P', form(plateDouble),
                        'R', form(stick),
                        'S', form(spring),
                        'w', WRENCH));
    }

    public static Recipe electrolyzer(int tier) {
        return new Recipe(
                List.of("SMS", "WwW"),
                Map.of(
                        'M',
                        STANDARD,
                        'S',
                        new Wire(1, Materials.Platinum),
                        'W',
                        new TierWire(tier, true),
                        'w',
                        WRENCH));
    }

    public static Recipe canner(int tier) {
        return new Recipe(
                List.of("wPh", "XMX", "CPW"),
                Map.of(
                        'C', new Circuit(tier),
                        'M', STANDARD,
                        'P', new Pipe(pipeRank(tier), Materials.StainlessSteel),
                        'W', new TierWire(tier, true),
                        'X', new Component(PUMPS, tier),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe injector(int tier) {
        return new Recipe(
                List.of("XPw", "CMW"),
                Map.of(
                        'C', new Circuit(tier),
                        'M', STANDARD,
                        'P', new Pipe(pipeRank(tier), Materials.StainlessSteel),
                        'W', new TierWire(tier, true),
                        'X', new Component(PISTONS, tier),
                        'w', WRENCH));
    }

    public static Recipe printer(int tier) {
        return new Recipe(
                List.of("CPC", "wXh", "WMW"),
                Map.of(
                        'C', new Circuit(tier),
                        'M', STANDARD,
                        'P', new Pipe(TINY, Materials.StainlessSteel),
                        'W', new TierWire(tier, true),
                        'X', new Component(CONVEYERS, tier),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe scannerVisuals(int tier) {
        return new Recipe(
                List.of("CPC", "wXh", "WMW"),
                Map.of(
                        'C',
                        new Circuit(tier),
                        'M',
                        STANDARD,
                        'P',
                        form(plate, Materials.Lumium),
                        'W',
                        new TierWire(tier, true),
                        'X',
                        new Component(CONVEYERS, tier),
                        'h',
                        HAMMER,
                        'w',
                        WRENCH));
    }

    public static Recipe autocrafter(int tier) {
        return new Recipe(
                List.of("WRW", "RwR", "CMC"),
                Map.of(
                        'C', new Circuit(tier),
                        'M', ROBUST,
                        'R', new Component(ROBOT_ARMS, tier),
                        'W', new TierWire(tier, true),
                        'w', WRENCH));
    }

    public static Recipe electricMixer(int tier) {
        return new Recipe(
                List.of("PMP", "PRP", "hSw"),
                Map.of(
                        'M',
                        STANDARD,
                        'P',
                        form(plateRank(Math.min(tier, 4)), Materials.StainlessSteel),
                        'R',
                        form(rotor, Materials.StainlessSteel),
                        'S',
                        new Component(MOTORS, tier),
                        'h',
                        HAMMER,
                        'w',
                        WRENCH));
    }

    public static Recipe electricLoom(int tier) {
        return new Recipe(
                List.of("ShS", "GMG", "SwS"),
                Map.of(
                        'G', new Component(MOTORS, tier),
                        'M', STANDARD,
                        'S', form(stickLong),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe electricSifter(int tier) {
        return new Recipe(
                List.of("WxW", "RMR", "SwS"),
                Map.of(
                        'M',
                        ROBUST,
                        'R',
                        form(stick),
                        'S',
                        new Component(PISTONS, tier),
                        'W',
                        form(wireFine),
                        'w',
                        WRENCH,
                        'x',
                        WIRE_CUTTER));
    }

    public static Recipe slicer(int tier) {
        return new Recipe(
                List.of("PRw", "YMC"),
                Map.of(
                        'C',
                        new Circuit(tier),
                        'M',
                        STANDARD,
                        'P',
                        new Component(PISTONS, tier),
                        'R',
                        form(stick),
                        'Y',
                        new Component(CONVEYERS, tier),
                        'w',
                        WRENCH));
    }

    public static Recipe nanofab(int tier) {
        return new Recipe(
                List.of("KAX", "ZMY", "CSC"),
                Map.of(
                        'A', LASER_EMITTER_ARGON,
                        'C', new Circuit(6),
                        'K', LASER_EMITTER_KRYPTON,
                        'M', STANDARD,
                        'S', CRYSTAL_PROCESSOR_SAPPHIRE,
                        'X', LASER_EMITTER_XENON,
                        'Y', new Component(EMITTERS, tier),
                        'Z', new Component(SENSORS, tier)));
    }

    public static Recipe plantalyzer(int tier) {
        return new Recipe(
                List.of("WXW", "ZMP", "CYC"),
                Map.of(
                        'C', new Circuit(tier),
                        'M', STANDARD,
                        'P', CRYSTAL_PROCESSOR_DIAMOND,
                        'W', new TierWire(tier, true),
                        'X', new Component(EMITTERS, tier),
                        'Y', new Component(SENSORS, tier),
                        'Z', SAPLINGS));
    }

    public static Recipe bumblelyzer(int tier) {
        return new Recipe(
                List.of("WXW", "ZMP", "CYC"),
                Map.of(
                        'C', new Circuit(tier),
                        'M', STANDARD,
                        'P', CRYSTAL_PROCESSOR_DIAMOND,
                        'W', new TierWire(tier, true),
                        'X', new Component(EMITTERS, tier),
                        'Y', new Component(SENSORS, tier),
                        'Z', new Alternatives(List.of(HONEY_BOTTLE, FluidContainer.HONEY))));
    }

    public static Recipe boxinator(int tier) {
        return new Recipe(
                List.of("wP", "CY", "CM"),
                Map.of(
                        'C', new Circuit(tier),
                        'M', STANDARD,
                        'P', new Component(PISTONS, tier),
                        'Y', new Component(CONVEYERS, tier),
                        'w', WRENCH));
    }

    public static Recipe unboxinator(int tier) {
        return new Recipe(
                List.of("Pw", "YC", "MC"),
                Map.of(
                        'C', new Circuit(tier),
                        'M', STANDARD,
                        'P', new Component(PISTONS, tier),
                        'Y', new Component(CONVEYERS, tier),
                        'w', WRENCH));
    }

    public static Recipe polarizer(int tier) {
        return new Recipe(
                List.of("TwT", "PMP", "TdT"),
                Map.of(
                        'M', STANDARD,
                        'P', form(plate),
                        'T', form(screw),
                        'd', SCREWDRIVER,
                        'w', WRENCH));
    }

    public static Recipe magneticSeparator(int tier) {
        return new Recipe(
                List.of("TwT", "TdT", "PMP"),
                Map.of(
                        'M', STANDARD,
                        'P', form(plate),
                        'T', form(screw),
                        'd', SCREWDRIVER,
                        'w', WRENCH));
    }

    public static Recipe laserEngraver(int tier) {
        return new Recipe(
                List.of("TdT", "GPG", "CMC"),
                Map.of(
                        'C',
                        new Circuit(tier),
                        'G',
                        form(gearGtSmall),
                        'M',
                        STANDARD,
                        'P',
                        HARDENED_CLAY,
                        'T',
                        form(screw),
                        'd',
                        SCREWDRIVER));
    }

    public static Recipe laserWelder(int tier) {
        return new Recipe(
                List.of("TLT", "GPG", "CMC"),
                Map.of(
                        'C',
                        new Circuit(tier),
                        'G',
                        form(gearGtSmall),
                        'L',
                        LENS,
                        'M',
                        STANDARD,
                        'P',
                        HARDENED_CLAY,
                        'T',
                        form(screw)));
    }

    public static Recipe freezer(int tier) {
        return new Recipe(
                List.of("hPw", "PMP", "PSP"),
                Map.of(
                        'M', STANDARD,
                        'P', form(plateRank(Math.min(tier, 4)), Materials.StainlessSteel),
                        'S', form(plateRank(tier), Materials.Silicon),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe cryoMixer(int tier) {
        return new Recipe(
                List.of("PMP", "PRP", "hSw"),
                Map.of(
                        'M',
                        STANDARD,
                        'P',
                        form(plateRank(Math.min(tier, 4)), Materials.StainlessSteel),
                        'R',
                        form(rotor, Materials.StainlessSteel),
                        'S',
                        form(plateRank(tier), Materials.Silicon),
                        'h',
                        HAMMER,
                        'w',
                        WRENCH));
    }

    public static Recipe massFab(int tier) {
        return new Recipe(
                List.of("RFS", "FMF", "RFS"),
                Map.of(
                        'F', new Component(FIELD_GENERATORS, tier),
                        'M', STANDARD,
                        'R', CRYSTAL_PROCESSOR_RUBY,
                        'S', CRYSTAL_PROCESSOR_SAPPHIRE));
    }

    public static Recipe scannerMolecular(int tier) {
        if (tier != 3) return null;
        return new Recipe(
                List.of("DXE", "FMF", "RYS"),
                Map.of(
                        'D', CRYSTAL_PROCESSOR_DIAMOND,
                        'E', CRYSTAL_PROCESSOR_EMERALD,
                        'F', new Component(FIELD_GENERATORS, tier),
                        'M', STANDARD,
                        'R', CRYSTAL_PROCESSOR_RUBY,
                        'S', CRYSTAL_PROCESSOR_SAPPHIRE,
                        'X', new Component(EMITTERS, tier),
                        'Y', new Component(SENSORS, tier)));
    }

    public static Recipe replicator(int tier) {
        return new Recipe(
                List.of("EXE", "FMF", "SXS"),
                Map.of(
                        'E', CRYSTAL_PROCESSOR_EMERALD,
                        'F', new Component(FIELD_GENERATORS, tier),
                        'M', STANDARD,
                        'S', CRYSTAL_PROCESSOR_SAPPHIRE,
                        'X', new Component(EMITTERS, tier)));
    }

    public static Recipe autoclave(int tier) {
        if (tier != 1) return null;
        return new Recipe(
                List.of("CwC", "PMP", "GPG"),
                Map.of(
                        'C',
                        form(itemCasing),
                        'G',
                        form(gearGtSmall),
                        'M',
                        REINFORCED,
                        'P',
                        new Pipe(SMALL, null),
                        'w',
                        WRENCH));
    }

    public static Recipe bath(int tier) {
        if (tier != 1) return null;
        return new Recipe(
                List.of("CwC", "PMP", "PPP"),
                Map.of('C', form(itemCasing), 'M', STANDARD, 'P', form(plate), 'w', WRENCH));
    }

    public static Recipe generifier(int tier) {
        if (tier != 1) return null;
        return new Recipe(
                List.of("ChC", "CMC", "CwC"),
                Map.of(
                        'C', form(itemCasing),
                        'M', STANDARD,
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe coagulator(int tier) {
        if (tier != 1) return null;
        return new Recipe(
                List.of("T T", "hMw", "TdT"),
                Map.of(
                        'M', STANDARD,
                        'T', form(screw),
                        'd', SCREWDRIVER,
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe fermenter(int tier) {
        if (tier != 1) return null;
        return new Recipe(
                List.of("wMh", "PPP", "BCB"),
                Map.of(
                        'B', BRICKS,
                        'C', anyCopper(plateDouble),
                        'M', STANDARD,
                        'P', new Pipe(LARGE, null),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe melter(int tier) {
        if (tier != 1) return null;
        return new Recipe(
                List.of("wUh", "PMP", "BCB"),
                Map.of(
                        'B', BRICKS,
                        'C', anyCopper(plateDouble),
                        'M', IRON_MACHINE_CASING,
                        'P', IRON_PIPE_MEDIUM,
                        'U', crucible(1005),
                        'h', HAMMER,
                        'w', WRENCH));
    }

    public static Recipe lightning(int tier) {
        return new Recipe(
                List.of("XxX", "WwW", "XMX"),
                Map.of(
                        'M', STANDARD,
                        'W', new TierWire(tier, false),
                        'X',
                                switch (tier) {
                                    case 1 -> IRON_WIRE_01;
                                    case 2 -> IRON_WIRE_02;
                                    case 3 -> IRON_WIRE_04;
                                    case 4 -> IRON_WIRE_08;
                                    default -> IRON_WIRE_16;
                                },
                        'w', WRENCH,
                        'x', WIRE_CUTTER));
    }

    private static Form form(MaterialPrefix prefix) {
        return form(prefix, null);
    }

    private static Form form(MaterialPrefix prefix, GTMaterial material) {
        return new Form(prefix, material, null);
    }

    private static Form form(MaterialPrefix prefix, GTMaterial material, GTMaterial group) {
        return new Form(prefix, material, group);
    }

    private static Form anyCopper(MaterialPrefix prefix) {
        return form(prefix, Materials.Copper, MaterialGroups.Cu);
    }

    private static Form anyDiamond(MaterialPrefix prefix) {
        return form(prefix, Materials.Diamond, MaterialGroups.Diamond);
    }

    private static Form anySteel(MaterialPrefix prefix) {
        return form(prefix, Materials.Steel, MaterialGroups.Steel);
    }

    private static MaterialPrefix plateRank(int rank) {
        return switch (rank) {
            case 1 -> plate;
            case 2 -> plateDouble;
            case 3 -> plateTriple;
            case 4 -> plateQuadruple;
            case 5 -> plateQuintuple;
            default -> throw new IllegalArgumentException("Unknown plate rank: " + rank);
        };
    }

    private static PipeSize pipeRank(int rank) {
        return switch (rank) {
            case 1 -> TINY;
            case 2 -> SMALL;
            case 3 -> MEDIUM;
            case 4 -> LARGE;
            case 5 -> HUGE;
            default -> throw new IllegalArgumentException("Unknown pipe rank: " + rank);
        };
    }

    private static Crucible crucible(int sourceId) {
        return new Crucible(Objects.requireNonNull(OriginalSmelteryDefinitions.get(sourceId)));
    }
}
