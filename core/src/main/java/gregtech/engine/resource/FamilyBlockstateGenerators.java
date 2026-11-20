package gregtech.engine.resource;

import static gregtech.engine.resource.VirtualBlockstateCatalog.facingRotation;
import static gregtech.engine.resource.VirtualBlockstateCatalog.flatDownVariants;
import static gregtech.engine.resource.VirtualBlockstateCatalog.ref;
import static gregtech.engine.resource.VirtualBlockstateCatalog.renderVariantsJson;
import static gregtech.engine.resource.VirtualBlockstateCatalog.variant;

import gregtech.content.plant.WoodSlabCatalog;
import gregtech.content.transport.TrackCatalog;
import gregtech.engine.resource.VirtualBlockstateCatalog.ModelRef;
import gregtech.engine.resource.VirtualBlockstateCatalog.Variant;
import gregtech.engine.sensor.SensorCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.jspecify.annotations.Nullable;

/**
 * Blockstate families whose stems and model references follow a catalogue table.
 *
 * <p>Storage safes, scaffolds, engines, fluid attachments, sensors, energy machines, tracks and wood
 * slabs repeat one variant table per family, so their stems are generated from the catalogue rows that
 * also drive registration instead of costing one definition line each. Every document is rendered
 * through the variant primitives of {@link VirtualBlockstateCatalog}, so a generated blockstate is the
 * document its definition row would have produced.
 */
public final class FamilyBlockstateGenerators {
    private static final String MACHINE = "gt6ce:block/machine/";
    private static final String TOOL = "gt6ce:block/tool/";
    private static final String TRACKS = "gt6ce:block/tracks/";
    private static final String WOOD_SLABS = "gt6ce:block/wood_slabs/";

    private static final List<String> ALL_FACINGS = List.of("down", "up", "north", "south", "west", "east");
    private static final List<String> HORIZONTAL_FACINGS = List.of("north", "east", "south", "west");

    /** Storage materials in registration order; plain steel owns a key-locked safe only. */
    private static final List<String> STORAGE_MATERIALS = List.of(
            "adamantium",
            "aluminium",
            "antimony",
            "arsenic_bronze",
            "arsenic_copper",
            "bismuth",
            "bismuth_bronze",
            "brass",
            "bronze",
            "chromium",
            "cobalt",
            "constantan",
            "duranium",
            "efrine",
            "electrum",
            "elven_elementium",
            "germanium",
            "gold",
            "invar",
            "iridium",
            "knightmetal",
            "lead",
            "lumium",
            "manasteel",
            "manganese",
            "molybdenum",
            "netherite",
            "nickel",
            "osmium",
            "palladium",
            "platinum",
            "silver",
            "stainless_steel",
            "steel",
            "steel_galvanized",
            "terrasteel",
            "thaumium",
            "tin_alloy",
            "titanium",
            "tritanium",
            "tungsten",
            "tungsten_carbide",
            "tungstensteel",
            "ultimet");

    /** Storage materials that also own a mechanical safe and a scaffold. */
    private static final List<String> MECHANICAL_MATERIALS = withoutPlainSteel(STORAGE_MATERIALS);

    private static final List<String> ELECTRIC_ENGINE_MATERIALS =
            List.of("aluminium", "chromium", "galvanized_steel", "stainless_steel", "titanium");

    private static final List<String> FLUX_ENGINE_MATERIALS =
            List.of("electrum", "enderium", "enderium_base", "invar", "lead");

    private static final List<String> STEAM_ENGINE_MATERIALS = List.of(
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

    private static final List<String> DIESEL_ENGINE_MATERIALS = List.of(
            "arsenic_bronze", "arsenic_copper", "bronze", "invar", "iridium", "steel", "titanium", "tungsten_steel");

    private static final List<String> ROTATION_ENGINE_MATERIALS = List.of(
            "adamantium",
            "arsenic_bronze",
            "arsenic_copper",
            "brass",
            "bronze",
            "iridium",
            "iritanium",
            "steel",
            "titanium",
            "trinaquadalloy",
            "trinitanium",
            "tungsten_steel",
            "wood");

    /** Materials that own a nozzle or a cap nozzle. */
    private static final List<String> NOZZLE_MATERIALS =
            List.of("adamantium", "plastic", "stainless_steel", "steel", "tantalum_hafnium_carbide", "tungsten");

    /** Materials that own a tap; ceramic taps exist, ceramic nozzles do not. */
    private static final List<String> TAP_MATERIALS =
            List.of("adamantium", "ceramic", "plastic", "stainless_steel", "tantalum_hafnium_carbide", "tungsten");

    private static final List<String> LASER_TIERS = List.of("lv", "mv", "hv", "ev", "iv");
    private static final List<String> QUANTUM_ENERGIZER_TIERS = List.of("ev", "iv", "luv", "zpm", "uv");

    private static final List<String> TRACK_KINDS = List.of("track_booster", "track_detector");

    private static final List<String> SLAB_TYPES = List.of("bottom", "double", "top");

    /** Slab model of a horizontal facing; the downward facing is handled separately. */
    private static final Map<String, Integer> SLAB_FACING_MODEL =
            Map.of("up", 2, "east", 3, "north", 4, "south", 5, "west", 6);

    /** One engine family: the stem prefix, the model base name and the materials that own an engine. */
    private record EngineFamily(String prefix, String model, List<String> materials) {}

    private static final List<EngineFamily> SINGLE_MODEL_ENGINES = List.of(
            new EngineFamily("engine_electric", "engine_electric", ELECTRIC_ENGINE_MATERIALS),
            new EngineFamily("engine_flux", "engine_flux", FLUX_ENGINE_MATERIALS),
            new EngineFamily("engine_steam", "engine_steam", STEAM_ENGINE_MATERIALS),
            new EngineFamily("engine_steam_strong", "engine_steam", STEAM_ENGINE_MATERIALS));

    private static final List<EngineFamily> TWO_MODEL_ENGINES = List.of(
            new EngineFamily("engine_diesel", "engine_diesel", DIESEL_ENGINE_MATERIALS),
            new EngineFamily("engine_rotation", "engine_rotation", ROTATION_ENGINE_MATERIALS));

    /** One fluid attachment family: the bare block's model and the model its material variants use. */
    private record FluidAttachment(String prefix, String bareModel, String materialModel, List<String> materials) {}

    private static final List<FluidAttachment> FLUID_ATTACHMENTS = List.of(
            new FluidAttachment("cap_nozzle", "cap_nozzle", "cap_nozzle", NOZZLE_MATERIALS),
            new FluidAttachment("nozzle", "cap_nozzle", "nozzle", NOZZLE_MATERIALS),
            new FluidAttachment("tap", "tap", "material_tap", TAP_MATERIALS));

    /** One energy machine family: the stem prefix and the tiers it exists in. */
    private record EnergyMachine(String name, List<String> tiers) {}

    private static final List<EnergyMachine> ENERGY_MACHINES = List.of(
            new EnergyMachine("co2_laser", LASER_TIERS),
            new EnergyMachine("flux_laser", LASER_TIERS),
            new EnergyMachine("laser_absorber", LASER_TIERS),
            new EnergyMachine("quantum_energizer", QUANTUM_ENERGIZER_TIERS));

    /**
     * One straight rail shape: the model offset within the powered or unpowered model set and the yaw
     * that turns the north-facing model onto it.
     */
    private record RailShape(String name, int model, int yaw) {}

    private static final List<RailShape> RAIL_SHAPES = List.of(
            new RailShape("ascending_east", 0, 90),
            new RailShape("ascending_north", 0, 0),
            new RailShape("ascending_south", 1, 0),
            new RailShape("ascending_west", 1, 90),
            new RailShape("east_west", 2, 90),
            new RailShape("north_south", 2, 0));

    private static volatile @Nullable Map<String, String> cache;

    private FamilyBlockstateGenerators() {}

    /** Every family blockstate stem, in family order. */
    public static Set<String> stems() {
        return rendered().keySet();
    }

    /** Reports whether {@code stem} belongs to a generated family. */
    public static boolean contains(String stem) {
        return stem != null && rendered().containsKey(stem);
    }

    /** Renders {@code stem}, or returns {@code null} when it is not a family blockstate. */
    public static @Nullable String renderJson(String stem) {
        return stem == null ? null : rendered().get(stem);
    }

    /** Emits every family blockstate to {@code sink}, sharing one byte array per distinct document. */
    public static void emitAll(BiConsumer<String, byte[]> sink) {
        Map<String, byte[]> encoded = new HashMap<>();
        for (Map.Entry<String, String> entry : rendered().entrySet()) {
            sink.accept(entry.getKey(), VirtualBlockstateCatalog.encodeUtf8(encoded, entry.getValue()));
        }
    }

    /** Renders every family blockstate once, keyed by stem in family order. */
    private static Map<String, String> rendered() {
        Map<String, String> current = cache;
        if (current != null) return current;
        synchronized (FamilyBlockstateGenerators.class) {
            current = cache;
            if (current == null) {
                Map<String, String> result = new LinkedHashMap<>(320);
                addMechanicalSafes(result);
                addKeySafes(result);
                addScaffolds(result);
                addSingleModelEngines(result);
                addTwoModelEngines(result);
                addFluidAttachments(result);
                addSensors(result);
                addEnergyMachines(result);
                addTracks(result);
                addWoodSlabs(result);
                current = Collections.unmodifiableMap(result);
                cache = current;
            }
            return current;
        }
    }

    /** Renders one family member and rejects a stem that two families claim. */
    private static void put(Map<String, String> out, String stem, List<String> models, List<Variant> variants) {
        if (out.putIfAbsent(stem, renderVariantsJson(variants, models)) != null) {
            throw new IllegalStateException("Duplicate family blockstate: " + stem);
        }
    }

    /** Storage materials without plain steel, which owns a key-locked safe only. */
    private static List<String> withoutPlainSteel(List<String> materials) {
        List<String> result = new ArrayList<>(materials.size());
        for (String material : materials) {
            if (!material.equals("steel")) result.add(material);
        }
        return List.copyOf(result);
    }

    // -------------------------------------------------------------- families

    /** Mechanical safes: one model, turned by facing and by the open flag. */
    private static void addMechanicalSafes(Map<String, String> out) {
        List<Variant> variants = openVariants(false);
        List<String> models = List.of(MACHINE + "storage/safe_mechanical");
        put(out, "safe", models, variants);
        for (String material : MECHANICAL_MATERIALS) {
            put(out, "safe_" + material, models, variants);
        }
    }

    /** Key-locked safes: the closed and the open door are separate models. */
    private static void addKeySafes(Map<String, String> out) {
        List<Variant> variants = openVariants(true);
        List<String> models = List.of(MACHINE + "storage/safe_keylocked", MACHINE + "storage/safe_keylocked_open");
        for (String material : STORAGE_MATERIALS) {
            put(out, "key_safe_" + material, models, variants);
        }
    }

    /** Scaffolds: one model per design. */
    private static void addScaffolds(Map<String, String> out) {
        List<Variant> variants = scaffoldDesigns();
        put(out, "scaffold", scaffoldModels("scaffold"), variants);
        for (String material : MECHANICAL_MATERIALS) {
            put(out, "scaffold_" + material, scaffoldModels("scaffold_" + material), variants);
        }
    }

    private static List<String> scaffoldModels(String base) {
        return List.of(TOOL + base, TOOL + base + "_1", TOOL + base + "_2", TOOL + base + "_3");
    }

    /** Engines whose lit state only changes the texture of the single model. */
    private static void addSingleModelEngines(Map<String, String> out) {
        List<Variant> variants = litVariants(false);
        for (EngineFamily family : SINGLE_MODEL_ENGINES) {
            List<String> models = List.of(MACHINE + "engine/" + family.model());
            for (String material : family.materials()) {
                put(out, family.prefix() + "_" + material, models, variants);
            }
        }
    }

    /** Engines that swap to a second model while lit. */
    private static void addTwoModelEngines(Map<String, String> out) {
        List<Variant> variants = litVariants(true);
        for (EngineFamily family : TWO_MODEL_ENGINES) {
            List<String> models =
                    List.of(MACHINE + "engine/" + family.model(), MACHINE + "engine/" + family.model() + "_active");
            for (String material : family.materials()) {
                put(out, family.prefix() + "_" + material, models, variants);
            }
        }
    }

    /** Fluid attachments: the bare block and its material variants use different models. */
    private static void addFluidAttachments(Map<String, String> out) {
        List<Variant> variants = flatDownVariants(0);
        for (FluidAttachment attachment : FLUID_ATTACHMENTS) {
            put(out, attachment.prefix(), List.of(TOOL + attachment.bareModel()), variants);
            for (String material : attachment.materials()) {
                put(out, attachment.prefix() + "_" + material, List.of(TOOL + attachment.materialModel()), variants);
            }
        }
    }

    /** Sensors: one model per sensor, named after its description. */
    private static void addSensors(Map<String, String> out) {
        List<Variant> variants = sensorRotations();
        for (SensorCatalog.Entry sensor : SensorCatalog.ALL) {
            put(out, sensor.id(), List.of(MACHINE + "sensor/" + sensorModel(sensor)), variants);
        }
    }

    /** Two sensor models keep the name of the sensor id instead of its description. */
    private static String sensorModel(SensorCatalog.Entry sensor) {
        return switch (sensor.id()) {
            case "sensor_weightometric" -> "weightometric";
            case "sensor_geiger" -> "geiger";
            default -> sensor.description();
        };
    }

    /** Energy machines: a base model and an active model per tier. */
    private static void addEnergyMachines(Map<String, String> out) {
        List<Variant> variants = activeVariants();
        for (EnergyMachine machine : ENERGY_MACHINES) {
            List<String> models =
                    List.of(MACHINE + "energy/" + machine.name(), MACHINE + "energy/" + machine.name() + "_active");
            for (String tier : machine.tiers()) {
                put(out, machine.name() + "_" + tier, models, variants);
            }
        }
    }

    /** Tracks: boosters and detectors share one six-model layout per material. */
    private static void addTracks(Map<String, String> out) {
        List<Variant> variants = railBoosterVariants();
        for (TrackCatalog.Material material : TrackCatalog.MATERIALS) {
            for (String kind : TRACK_KINDS) {
                String base = kind + "_" + material.slug();
                put(out, base, trackModels(base), variants);
            }
        }
    }

    private static List<String> trackModels(String base) {
        return List.of(
                TRACKS + base + "_raised_ne",
                TRACKS + base + "_raised_sw",
                TRACKS + base + "_flat",
                TRACKS + base + "_raised_ne_on",
                TRACKS + base + "_raised_sw_on",
                TRACKS + base + "_flat_on");
    }

    /** Wood slabs: one model per facing plus the planks model a double slab shows. */
    private static void addWoodSlabs(Map<String, String> out) {
        List<Variant> variants = woodSlabVariants();
        for (String wood : WoodSlabCatalog.WOODS) {
            put(out, "slab_" + wood, slabModels(wood), variants);
        }
    }

    private static List<String> slabModels(String wood) {
        String base = WOOD_SLABS + wood;
        return List.of(
                base + "_down",
                planksModel(wood),
                base + "_up",
                base + "_east",
                base + "_north",
                base + "_south",
                base + "_west");
    }

    /** The blue mahoe planks ship a block model; every other wood only has its iconset model. */
    private static String planksModel(String wood) {
        return wood.equals("blue_mahoe") ? "gt6ce:block/wood/planks_blue_mahoe" : "gt6ce:block/iconsets/planks_" + wood;
    }

    // -------------------------------------------------------------- variant tables

    /** Facing and open flag; {@code distinctModels} gives the open state its own model. */
    private static List<Variant> openVariants(boolean distinctModels) {
        List<Variant> variants = new ArrayList<>(12);
        for (String facing : ALL_FACINGS) {
            for (boolean open : new boolean[] {false, true}) {
                variants.add(variant(
                        "facing=" + facing + ",open=" + open, facingRotation(distinctModels && open ? 1 : 0, facing)));
            }
        }
        return variants;
    }

    /** Scaffold designs 1 and 2 hang their hatch on the opposite side, so they turn a half turn further. */
    private static List<Variant> scaffoldDesigns() {
        List<Variant> variants = new ArrayList<>(16);
        for (int design = 0; design < 4; design++) {
            for (String facing : HORIZONTAL_FACINGS) {
                int yaw = HORIZONTAL_FACINGS.indexOf(facing) * 90;
                if (design == 1 || design == 2) yaw = (yaw + 180) % 360;
                variants.add(variant("design=" + design + ",facing=" + facing, ref(design, 0, yaw)));
            }
        }
        return variants;
    }

    /** Facing and lit flag; {@code distinctModels} gives the lit state its own model. */
    private static List<Variant> litVariants(boolean distinctModels) {
        List<Variant> variants = new ArrayList<>(12);
        for (String facing : ALL_FACINGS) {
            for (boolean lit : new boolean[] {false, true}) {
                variants.add(variant(
                        "facing=" + facing + ",lit=" + lit, facingRotation(distinctModels && lit ? 1 : 0, facing)));
            }
        }
        return variants;
    }

    private static List<Variant> activeVariants() {
        List<Variant> variants = new ArrayList<>(12);
        for (String facing : ALL_FACINGS) {
            for (boolean active : new boolean[] {false, true}) {
                variants.add(variant("facing=" + facing + ",active=" + active, facingRotation(active ? 1 : 0, facing)));
            }
        }
        return variants;
    }

    /** Sensor blocks sit a half turn further round than the plain facing rotation. */
    private static ModelRef sensorRotation(int model, String facing) {
        return switch (facing) {
            case "up" -> ref(model, 270, 180);
            case "down" -> ref(model, 90, 180);
            default -> facingRotation(model, facing);
        };
    }

    private static List<Variant> sensorRotations() {
        List<Variant> variants = new ArrayList<>(ALL_FACINGS.size());
        for (String facing : ALL_FACINGS) {
            variants.add(variant("facing=" + facing, sensorRotation(0, facing)));
        }
        return variants;
    }

    /** Boosters only exist on the straight rail shapes, and the powered set starts at model three. */
    private static List<Variant> railBoosterVariants() {
        List<Variant> variants = new ArrayList<>(12);
        for (boolean powered : new boolean[] {false, true}) {
            for (RailShape shape : RAIL_SHAPES) {
                variants.add(variant(
                        "powered=" + powered + ",shape=" + shape.name(),
                        ref((powered ? 3 : 0) + shape.model(), 0, shape.yaw())));
            }
        }
        return variants;
    }

    private static List<Variant> woodSlabVariants() {
        List<Variant> variants = new ArrayList<>(18);
        for (String facing : ALL_FACINGS) {
            for (String type : SLAB_TYPES) {
                variants.add(variant("facing=" + facing + ",type=" + type, ref(slabModel(facing, type))));
            }
        }
        return variants;
    }

    /**
     * Slab model for one facing and slab type: a double slab shows the planks model, and the top half
     * of a downward facing block shows the upward model.
     */
    private static int slabModel(String facing, String type) {
        if (type.equals("double")) return 1;
        if (facing.equals("down")) return type.equals("bottom") ? 0 : 2;
        return SLAB_FACING_MODEL.get(facing);
    }
}
