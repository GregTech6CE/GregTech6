package com.gregtech.gregtech.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tag definitions from GT6 {@code gregapi.data.TD}.
 * {@link Energy} holds all GregTech energy types ({@link Tag}  - GT6 {@code TagData}).
 */
public class GregTechTags {
    protected GregTechTags() {}

    public static final class Tag {
        private final String id;
        private final String display;
        private final String shortName;

        Tag(String id, String display) {
            this(id, display, null);
        }

        Tag(String id, String display, String shortName) {
            this.id = id;
            this.display = display;
            this.shortName = shortName;
        }

        /** Stable name, e.g. {@code ENERGY.ELECTRICITY} (GT6 {@code TagData.mName}). */
        public String getId() { return id; }

        /** Long display name, e.g. {@code Electric Energy}. */
        public String getDisplay() { return display; }

        /** Short unit label, e.g. {@code EU}, {@code HU}. */
        public String getShortName() { return shortName != null ? shortName : id; }

        public List<Tag> asList() { return List.of(this); }

        @Override public String toString() { return id; }
    }

    private static final Map<String, Tag> REGISTRY = new LinkedHashMap<>();

    private static Tag tag(String field, String id, String display) {
        Tag t = new Tag(id, display);
        REGISTRY.put(field, t);
        return t;
    }

    private static Tag energyTag(String field, String id, String shortName, String longName) {
        Tag t = new Tag(id, longName, shortName);
        REGISTRY.put(field, t);
        return t;
    }

    public static final class Creative {
        private Creative() {}

        public static final Tag HIDDEN = tag("HIDDEN", "Creative.HIDDEN", "NEI.HIDDEN");
    }

    public static final class Projectiles {
        private Projectiles() {}

        public static final Tag ARROW = tag("ARROW", "Projectiles.ARROW", "PROJECTILES.ARROW");
        public static final Tag BULLET_SMALL = tag("BULLET_SMALL", "Projectiles.BULLET_SMALL", "PROJECTILES.BULLET_SMALL");
        public static final Tag BULLET_MEDIUM = tag("BULLET_MEDIUM", "Projectiles.BULLET_MEDIUM", "PROJECTILES.BULLET_MEDIUM");
        public static final Tag BULLET_LARGE = tag("BULLET_LARGE", "Projectiles.BULLET_LARGE", "PROJECTILES.BULLET_LARGE");
    }

    public static final class Connectors {
        private Connectors() {}

        public static final Tag PNEUMATIC_ITEM = tag("PNEUMATIC_ITEM", "Connectors.PNEUMATIC_ITEM", "CONNECTORS.PNEUMATIC_ITEM");
        public static final Tag PIPE_FLUID = tag("PIPE_FLUID", "Connectors.PIPE_FLUID", "CONNECTORS.PIPE_FLUID");
        public static final Tag WIRE_REDSTONE = tag("WIRE_REDSTONE", "Connectors.WIRE_REDSTONE", "CONNECTORS.WIRE_REDSTONE");
        public static final Tag WIRE_ELECTRIC = tag("WIRE_ELECTRIC", "Connectors.WIRE_ELECTRIC", "CONNECTORS.WIRE_ELECTRIC");
        public static final Tag WIRE_LASER = tag("WIRE_LASER", "Connectors.WIRE_LASER", "CONNECTORS.WIRE_LASER");
        public static final Tag WIRE_LOGISTICS = tag("WIRE_LOGISTICS", "Connectors.WIRE_LOGISTICS", "CONNECTORS.WIRE_LOGISTICS");
        public static final Tag AXLE_ROTATION = tag("AXLE_ROTATION", "Connectors.AXLE_ROTATION", "CONNECTORS.AXLE_ROTATION");
    }

    /**
     * GregTech energy types. Packet semantics match GT6:
     * <ul>
     *   <li>EU: Size = voltage, Amount = amperage</li>
     *   <li>RU: Size = speed (+/- direction), Amount = power</li>
     *   <li>KU: Size = push/pull, Amount = power</li>
     *   <li>HU/CU/NU: Size = 1, Amount = temperature</li>
     *   <li>LU: Size = beam strength, Amount = frequency</li>
     *   <li>MU: Size = field polarity/strength, Amount = 1</li>
     *   <li>TU: Size = 1, Amount = ticks</li>
     *   <li>Steam: Size = 1, Amount = L/t (2 Steam = 1 EU)</li>
     *   <li>RF: Size = 1, Amount = RF (4 RF = 1 EU)</li>
     *   <li>MJ: Size = push size, Amount = MJ (1 MJ = 2.5 EU = 10 RF)</li>
     * </ul>
     * GU (Greg Unit) is not an energy tag; it is the NEI/recipe cost unit ({@code Recipe.mEUt}).
     */
    public static final class Energy {
        private Energy() {}

        public static final Tag ELECTRICITY = energyTag("ELECTRICITY", "ENERGY.ELECTRICITY", "EU", "Electric Energy");
        public static final Tag KINETIC_ROTATION = energyTag("KINETIC_ROTATION", "ENERGY.KINETIC_ROTATION", "RU", "Rotation Energy");
        public static final Tag KINETIC_PUSH = energyTag("KINETIC_PUSH", "ENERGY.KINETIC_PUSH", "KU", "Kinetic Energy");
        public static final Tag HEAT = energyTag("HEAT", "ENERGY.HEAT", "HU", "Heat Energy");
        public static final Tag CRYO = energyTag("CRYO", "ENERGY.CRYO", "CU", "Cryo Energy");
        public static final Tag LIGHT = energyTag("LIGHT", "ENERGY.LIGHT", "LU", "Light Energy");
        public static final Tag MAGNETIC = energyTag("MAGNETIC", "ENERGY.MAGNETIC", "MU", "Magnetic Energy");
        public static final Tag NEUTRON = energyTag("NEUTRON", "ENERGY.NEUTRON", "NU", "Neutron Energy");
        public static final Tag QUANTUM = energyTag("QUANTUM", "ENERGY.QUANTUM", "QU", "Quantum Energy");
        public static final Tag TIME = energyTag("TIME", "ENERGY.TIME", "TU", "Time");
        public static final Tag REDSTONE_FLUX = energyTag("REDSTONE_FLUX", "ENERGY.REDSTONE_FLUX", "RF", "Redstone Flux");
        public static final Tag MINECRAFT_JOULES = energyTag("MINECRAFT_JOULES", "ENERGY.MINECRAFT_JOULES", "MJ", "Minecraft Joules");
        public static final Tag STEAM = energyTag("STEAM", "ENERGY.STEAM", "Steam", "Steam");
        public static final Tag AIR = energyTag("AIR", "ENERGY.AIR", "AU", "Air Pressure");
        public static final Tag VIS_ORDO = energyTag("VIS_ORDO", "ENERGY.VIS_ORDO", "Ordo", "Ordo Vis");
        public static final Tag VIS_AER = energyTag("VIS_AER", "ENERGY.VIS_AER", "Aer", "Aer Vis");
        public static final Tag VIS_AQUA = energyTag("VIS_AQUA", "ENERGY.VIS_AQUA", "Aqua", "Aqua Vis");
        public static final Tag VIS_TERRA = energyTag("VIS_TERRA", "ENERGY.VIS_TERRA", "Terra", "Terra Vis");
        public static final Tag VIS_IGNIS = energyTag("VIS_IGNIS", "ENERGY.VIS_IGNIS", "Ignis", "Ignis Vis");
        public static final Tag VIS_PERDITIO = energyTag("VIS_PERDITIO", "ENERGY.VIS_PERDITIO", "Perditio", "Perditio Vis");

        /** GT6-style short aliases (same Tag instance). */
        public static final Tag EU = ELECTRICITY;
        public static final Tag RU = KINETIC_ROTATION;
        public static final Tag KU = KINETIC_PUSH;
        public static final Tag HU = HEAT;
        public static final Tag CU = CRYO;
        public static final Tag LU = LIGHT;
        public static final Tag MU = MAGNETIC;
        public static final Tag NU = NEUTRON;
        public static final Tag QU = QUANTUM;
        public static final Tag TU = TIME;
        public static final Tag TICK = TIME;
        public static final Tag RF = REDSTONE_FLUX;
        public static final Tag MJ = MINECRAFT_JOULES;
        public static final Tag AU = AIR;
        /** Community/wiki name for {@link #STEAM} (2 L/t Steam  - 1 EU). */
        public static final Tag SU = STEAM;

        public static final List<Tag> VIS = List.of(
                VIS_ORDO, VIS_AER, VIS_AQUA, VIS_TERRA, VIS_IGNIS, VIS_PERDITIO);

        public static final List<Tag> ALL = List.of(
                VIS_ORDO, VIS_AER, VIS_AQUA, VIS_TERRA, VIS_IGNIS, VIS_PERDITIO,
                STEAM, MJ, RF, AU, QU, MU, LU, HU, CU, KU, RU, EU, NU, TU);

        public static final List<Tag> ALL_RF = List.of(MJ, RF, KU);

        public static final List<Tag> ALL_EU = List.of(AU, QU, MU, LU, KU, RU, EU, HU, NU, CU);

        /** Energy types actively used by GT6 machines (excludes RF/MJ/Steam compat tags). */
        public static final List<Tag> ALL_GT = List.of(AU, QU, MU, LU, KU, RU, EU, HU, NU, CU, TU);

        public static final List<Tag> ALL_KINETIC = List.of(KU, RU);

        public static final List<Tag> ALL_ELECTRIC = List.of(EU);

        public static final List<Tag> ALL_NEGATIVE_ALLOWED = List.of(AU, QU, MU, KU, RU, EU);

        public static final List<Tag> ALL_WEAK_TO_WATER = List.of(EU, HU, CU);

        public static final List<Tag> ALL_WEAK_TO_THUNDER = List.of(EU);

        public static final List<Tag> ALL_WEAK_TO_FIRE = List.of(EU, CU);

        public static final List<Tag> ALL_HOT_COLD = List.of(HU, CU, VIS_IGNIS);

        public static final List<Tag> ALL_COMSUMPTION_LIMITED = List.of(
                EU, RF, VIS_ORDO, VIS_AER, VIS_AQUA, VIS_TERRA, VIS_IGNIS, VIS_PERDITIO);

        public static final List<Tag> ALL_EXPLODING = List.of(
                AU, QU, MU, LU, KU, RU, EU, HU, NU, MJ, STEAM);

        public static final List<Tag> ALL_ALTERNATING = List.of(KU);

        public static final List<Tag> ALL_SIZE_IRRELEVANT = List.of(
                VIS_ORDO, VIS_AER, VIS_AQUA, VIS_TERRA, VIS_IGNIS, VIS_PERDITIO,
                STEAM, MJ, RF, HU, CU, NU, QU, TU);

        private static final Set<Tag> SIZE_IRRELEVANT = Set.copyOf(ALL_SIZE_IRRELEVANT);

        public static boolean isSizeIrrelevant(Tag energyType) {
            return SIZE_IRRELEVANT.contains(energyType);
        }

        public static Tag byId(String id) {
            if (id == null) {
                return null;
            }
            String key = id.toUpperCase();
            for (Tag tag : ALL) {
                if (tag.getId().equals(key)) {
                    return tag;
                }
            }
            return null;
        }
    }

    public static final class Prefix {
        private Prefix() {}

        public static final Tag PREFIX_UNUSED = tag("PREFIX_UNUSED", "Prefix.PREFIX_UNUSED", "PREFIX.PREFIX_UNUSED");
        public static final Tag IS_CONTAINER = tag("IS_CONTAINER", "Prefix.IS_CONTAINER", "PREFIX.IS_CONTAINER");
        public static final Tag IS_CRATE = tag("IS_CRATE", "Prefix.IS_CRATE", "PREFIX.IS_CRATE");
        public static final Tag MATERIAL_BASED = tag("MATERIAL_BASED", "Prefix.MATERIAL_BASED", "PREFIX.MATERIAL_BASED");
        public static final Tag BLOCK_BASED = tag("BLOCK_BASED", "Prefix.BLOCK_BASED", "PREFIX.BLOCK_BASED");
        public static final Tag STORAGE_BASED = tag("STORAGE_BASED", "Prefix.STORAGE_BASED", "PREFIX.STORAGE_BASED");
        public static final Tag PLANT_DROP = tag("PLANT_DROP", "Prefix.PLANT_DROP", "PREFIX.PLANT_DROP");
        public static final Tag UNIFICATABLE = tag("UNIFICATABLE", "Prefix.UNIFICATABLE", "PREFIX.UNIFICATABLE");
        public static final Tag TOOLTIP_MATERIAL = tag("TOOLTIP_MATERIAL", "Prefix.TOOLTIP_MATERIAL", "PREFIX.TOOLTIP_MATERIAL");
        public static final Tag TOOLTIP_ENCHANTS = tag("TOOLTIP_ENCHANTS", "Prefix.TOOLTIP_ENCHANTS", "PREFIX.TOOLTIP_ENCHANTS");
        public static final Tag NO_PREFIX_FILTERING = tag("NO_PREFIX_FILTERING", "Prefix.NO_PREFIX_FILTERING", "PREFIX.NO_PREFIX_FILTERING");
        public static final Tag GEM_BASED = tag("GEM_BASED", "Prefix.GEM_BASED", "PREFIX.GEM_BASED");
        public static final Tag DUST_BASED = tag("DUST_BASED", "Prefix.DUST_BASED", "PREFIX.DUST_BASED");
        public static final Tag INGOT_BASED = tag("INGOT_BASED", "Prefix.INGOT_BASED", "PREFIX.INGOT_BASED");
        public static final Tag WIRE_BASED = tag("WIRE_BASED", "Prefix.WIRE_BASED", "PREFIX.WIRE_BASED");
        public static final Tag UNIFICATABLE_RECIPES = tag("UNIFICATABLE_RECIPES", "Prefix.UNIFICATABLE_RECIPES", "PREFIX.UNIFICATABLE_RECIPES");
        public static final Tag EXTRUDER_FODDER = tag("EXTRUDER_FODDER", "Prefix.EXTRUDER_FODDER", "PREFIX.EXTRUDER_FODDER");
        public static final Tag ORE = tag("ORE", "Prefix.ORE", "PREFIX.ORE");
        public static final Tag STANDARD_ORE = tag("STANDARD_ORE", "Prefix.STANDARD_ORE", "PREFIX.STANDARD_ORE");
        public static final Tag DUST_ORE = tag("DUST_ORE", "Prefix.DUST_ORE", "PREFIX.DUST_ORE");
        public static final Tag DENSE_ORE = tag("DENSE_ORE", "Prefix.DENSE_ORE", "PREFIX.DENSE_ORE");
        public static final Tag ORE_PROCESSING_DIRTY = tag("ORE_PROCESSING_DIRTY", "Prefix.ORE_PROCESSING_DIRTY", "PREFIX.ORE_PROCESSING_DIRTY");
        public static final Tag ORE_PROCESSING_CLEAN = tag("ORE_PROCESSING_CLEAN", "Prefix.ORE_PROCESSING_CLEAN", "PREFIX.ORE_PROCESSING_CLEAN");
        public static final Tag ORE_PROCESSING_REFINED = tag("ORE_PROCESSING_REFINED", "Prefix.ORE_PROCESSING_REFINED", "PREFIX.ORE_PROCESSING_REFINED");
        public static final Tag ORE_PROCESSING_BASED = tag("ORE_PROCESSING_BASED", "Prefix.ORE_PROCESSING_BASED", "PREFIX.ORE_PROCESSING_BASED");
        public static final Tag TOOL_HEAD = tag("TOOL_HEAD", "Prefix.TOOL_HEAD", "PREFIX.TOOL_HEAD");
        public static final Tag NEEDS_HANDLE = tag("NEEDS_HANDLE", "Prefix.NEEDS_HANDLE", "PREFIX.NEEDS_HANDLE");
        public static final Tag NEEDS_SHARPENING = tag("NEEDS_SHARPENING", "Prefix.NEEDS_SHARPENING", "PREFIX.NEEDS_SHARPENING");
        public static final Tag TOOL_ALIKE = tag("TOOL_ALIKE", "Prefix.TOOL_ALIKE", "PREFIX.TOOL_ALIKE");
        public static final Tag WEAPON_ALIKE = tag("WEAPON_ALIKE", "Prefix.WEAPON_ALIKE", "PREFIX.WEAPON_ALIKE");
        public static final Tag ARMOR_ALIKE = tag("ARMOR_ALIKE", "Prefix.ARMOR_ALIKE", "PREFIX.ARMOR_ALIKE");
        public static final Tag AMMO_ALIKE = tag("AMMO_ALIKE", "Prefix.AMMO_ALIKE", "PREFIX.AMMO_ALIKE");
        public static final Tag RECYCLABLE = tag("RECYCLABLE", "Prefix.RECYCLABLE", "PREFIX.RECYCLABLE");
        public static final Tag SCANNABLE = tag("SCANNABLE", "Prefix.SCANNABLE", "PREFIX.SCANNABLE");
        public static final Tag BURNABLE = tag("BURNABLE", "Prefix.BURNABLE", "PREFIX.BURNABLE");
        public static final Tag SIMPLIFIABLE = tag("SIMPLIFIABLE", "Prefix.SIMPLIFIABLE", "PREFIX.SIMPLIFIABLE");
        public static final Tag SELF_REFERENCING = tag("SELF_REFERENCING", "Prefix.SELF_REFERENCING", "PREFIX.SELF_REFERENCING");
    }

    public static final class Atomic {
        private Atomic() {}

        public static final Tag ELEMENT = tag("ELEMENT", "Atomic.ELEMENT", "ATOMIC.ELEMENT");
        public static final Tag PARTICLE = tag("PARTICLE", "Atomic.PARTICLE", "ATOMIC.PARTICLE");
        public static final Tag MOLECULE = tag("MOLECULE", "Atomic.MOLECULE", "ATOMIC.MOLECULE");
        public static final Tag ANTIMATTER = tag("ANTIMATTER", "Atomic.ANTIMATTER", "ATOMIC.ANTIMATTER");
        public static final Tag METAL = tag("METAL", "Atomic.METAL", "ATOMIC.METAL");
        public static final Tag ALKALI_METAL = tag("ALKALI_METAL", "Atomic.ALKALI_METAL", "ATOMIC.ALKALI_METAL");
        public static final Tag ALKALINE_EARTH_METAL = tag("ALKALINE_EARTH_METAL", "Atomic.ALKALINE_EARTH_METAL", "ATOMIC.ALKALINE_EARTH_METAL");
        public static final Tag LANTHANIDE = tag("LANTHANIDE", "Atomic.LANTHANIDE", "ATOMIC.LANTHANIDE");
        public static final Tag ACTINIDE = tag("ACTINIDE", "Atomic.ACTINIDE", "ATOMIC.ACTINIDE");
        public static final Tag TRANSITION_METAL = tag("TRANSITION_METAL", "Atomic.TRANSITION_METAL", "ATOMIC.TRANSITION_METAL");
        public static final Tag POST_TRANSITION_METAL = tag("POST_TRANSITION_METAL", "Atomic.POST_TRANSITION_METAL", "ATOMIC.POST_TRANSITION_METAL");
        public static final Tag METALLOID = tag("METALLOID", "Atomic.METALLOID", "ATOMIC.METALLOID");
        public static final Tag NONMETAL = tag("NONMETAL", "Atomic.NONMETAL", "ATOMIC.NONMETAL");
        public static final Tag POLYATOMIC_NONMETAL = tag("POLYATOMIC_NONMETAL", "Atomic.POLYATOMIC_NONMETAL", "ATOMIC.POLYATOMIC_NONMETAL");
        public static final Tag DIATOMIC_NONMETAL = tag("DIATOMIC_NONMETAL", "Atomic.DIATOMIC_NONMETAL", "ATOMIC.DIATOMIC_NONMETAL");
        public static final Tag NOBLE_GAS = tag("NOBLE_GAS", "Atomic.NOBLE_GAS", "ATOMIC.NOBLE_GAS");
        public static final Tag HALOGEN = tag("HALOGEN", "Atomic.HALOGEN", "ATOMIC.HALOGEN");
        public static final Tag CHALCOGEN = tag("CHALCOGEN", "Atomic.CHALCOGEN", "ATOMIC.CHALCOGEN");
        public static final Tag PNICTOGEN = tag("PNICTOGEN", "Atomic.PNICTOGEN", "ATOMIC.PNICTOGEN");
        public static final Tag CRYSTALLOGEN = tag("CRYSTALLOGEN", "Atomic.CRYSTALLOGEN", "ATOMIC.CRYSTALLOGEN");
        public static final Tag ICOSAGEN = tag("ICOSAGEN", "Atomic.ICOSAGEN", "ATOMIC.ICOSAGEN");
        public static final Tag ZINC_GROUP = tag("ZINC_GROUP", "Atomic.ZINC_GROUP", "ATOMIC.ZINC_GROUP");
        public static final Tag COPPER_GROUP = tag("COPPER_GROUP", "Atomic.COPPER_GROUP", "ATOMIC.COPPER_GROUP");
        public static final Tag NICKEL_GROUP = tag("NICKEL_GROUP", "Atomic.NICKEL_GROUP", "ATOMIC.NICKEL_GROUP");
        public static final Tag COBALT_GROUP = tag("COBALT_GROUP", "Atomic.COBALT_GROUP", "ATOMIC.COBALT_GROUP");
        public static final Tag IRON_GROUP = tag("IRON_GROUP", "Atomic.IRON_GROUP", "ATOMIC.IRON_GROUP");
        public static final Tag MANGANESE_GROUP = tag("MANGANESE_GROUP", "Atomic.MANGANESE_GROUP", "ATOMIC.MANGANESE_GROUP");
        public static final Tag CHROMIUM_GROUP = tag("CHROMIUM_GROUP", "Atomic.CHROMIUM_GROUP", "ATOMIC.CHROMIUM_GROUP");
        public static final Tag VANADIUM_GROUP = tag("VANADIUM_GROUP", "Atomic.VANADIUM_GROUP", "ATOMIC.VANADIUM_GROUP");
        public static final Tag TITANIUM_GROUP = tag("TITANIUM_GROUP", "Atomic.TITANIUM_GROUP", "ATOMIC.TITANIUM_GROUP");
        public static final Tag SCANDIUM_GROUP = tag("SCANDIUM_GROUP", "Atomic.SCANDIUM_GROUP", "ATOMIC.SCANDIUM_GROUP");
        public static final Tag NOBLE_METAL = tag("NOBLE_METAL", "Atomic.NOBLE_METAL", "ATOMIC.NOBLE_METAL");
        public static final Tag REFRACTORY_METAL = tag("REFRACTORY_METAL", "Atomic.REFRACTORY_METAL", "ATOMIC.REFRACTORY_METAL");
        public static final Tag PRECIOUS_METAL = tag("PRECIOUS_METAL", "Atomic.PRECIOUS_METAL", "ATOMIC.PRECIOUS_METAL");
        public static final Tag PLATINUM_GROUP = tag("PLATINUM_GROUP", "Atomic.PLATINUM_GROUP", "ATOMIC.PLATINUM_GROUP");
    }

    public static final class Properties {
        private Properties() {}

        public static final Tag ACID = tag("ACID", "Properties.ACID", "PROPERTIES.ACID");
        public static final Tag WOOD = tag("WOOD", "Properties.WOOD", "PROPERTIES.WOOD");
        public static final Tag FOOD = tag("FOOD", "Properties.FOOD", "PROPERTIES.FOOD");
        public static final Tag MEAT = tag("MEAT", "Properties.MEAT", "PROPERTIES.MEAT");
        public static final Tag ROTTEN = tag("ROTTEN", "Properties.ROTTEN", "PROPERTIES.ROTTEN");
        public static final Tag COAL = tag("COAL", "Properties.COAL", "PROPERTIES.COAL");
        public static final Tag STONE = tag("STONE", "Properties.STONE", "PROPERTIES.STONE");
        public static final Tag PEARL = tag("PEARL", "Properties.PEARL", "PROPERTIES.PEARL");
        public static final Tag QUARTZ = tag("QUARTZ", "Properties.QUARTZ", "PROPERTIES.QUARTZ");
        public static final Tag CRYSTAL = tag("CRYSTAL", "Properties.CRYSTAL", "PROPERTIES.CRYSTAL");
        public static final Tag VALUABLE = tag("VALUABLE", "Properties.VALUABLE", "PROPERTIES.VALUABLE");
        public static final Tag MAGICAL = tag("MAGICAL", "Properties.MAGICAL", "PROPERTIES.MAGICAL");
        public static final Tag WARPING = tag("WARPING", "Properties.WARPING", "PROPERTIES.WARPING");
        public static final Tag BETWEENLANDS = tag("BETWEENLANDS", "Properties.BETWEENLANDS", "PROPERTIES.BETWEENLANDS");
        public static final Tag MAZEBREAKER = tag("MAZEBREAKER", "Properties.MAZEBREAKER", "PROPERTIES.MAZEBREAKER");
        public static final Tag BURNING = tag("BURNING", "Properties.BURNING", "PROPERTIES.BURNING");
        public static final Tag FLAMMABLE = tag("FLAMMABLE", "Properties.FLAMMABLE", "PROPERTIES.FLAMMABLE");
        public static final Tag UNBURNABLE = tag("UNBURNABLE", "Properties.UNBURNABLE", "PROPERTIES.UNBURNABLE");
        public static final Tag EXPLOSIVE = tag("EXPLOSIVE", "Properties.EXPLOSIVE", "PROPERTIES.EXPLOSIVE");
        public static final Tag BOUNCY = tag("BOUNCY", "Properties.BOUNCY", "PROPERTIES.BOUNCY");
        public static final Tag GLOWING = tag("GLOWING", "Properties.GLOWING", "PROPERTIES.GLOWING");
        public static final Tag LIGHTING = tag("LIGHTING", "Properties.LIGHTING", "PROPERTIES.LIGHTING");
        public static final Tag SOFT = tag("SOFT", "Properties.SOFT", "PROPERTIES.SOFT");
        public static final Tag BRITTLE = tag("BRITTLE", "Properties.BRITTLE", "PROPERTIES.BRITTLE");
        public static final Tag STRETCHY = tag("STRETCHY", "Properties.STRETCHY", "PROPERTIES.STRETCHY");
        public static final Tag INVISIBLE = tag("INVISIBLE", "Properties.INVISIBLE", "PROPERTIES.INVISIBLE");
        public static final Tag TRANSPARENT = tag("TRANSPARENT", "Properties.TRANSPARENT", "PROPERTIES.TRANSPARENT");
        public static final Tag MAGNETIC_PASSIVE = tag("MAGNETIC_PASSIVE", "Properties.MAGNETIC_PASSIVE", "PROPERTIES.MAGNETIC_PASSIVE");
        public static final Tag MAGNETIC_ACTIVE = tag("MAGNETIC_ACTIVE", "Properties.MAGNETIC_ACTIVE", "PROPERTIES.MAGNETIC_ACTIVE");
        public static final Tag AUTO_COLLECTING = tag("AUTO_COLLECTING", "Properties.AUTO_COLLECTING", "PROPERTIES.AUTO_COLLECTING");
        public static final Tag ENDER_DRAGON_PROOF = tag("ENDER_DRAGON_PROOF", "Properties.ENDER_DRAGON_PROOF", "PROPERTIES.ENDER_DRAGON_PROOF");
        public static final Tag WITHER_PROOF = tag("WITHER_PROOF", "Properties.WITHER_PROOF", "PROPERTIES.WITHER_PROOF");
        public static final Tag NO_ADVANCED_TOOLS = tag("NO_ADVANCED_TOOLS", "Properties.NO_ADVANCED_TOOLS", "PROPERTIES.NO_ADVANCED_TOOLS");
        public static final Tag HAS_TOOL_STATS = tag("HAS_TOOL_STATS", "Properties.HAS_TOOL_STATS", "PROPERTIES.HAS_TOOL_STATS");
        public static final Tag HAS_COLOR = tag("HAS_COLOR", "Properties.HAS_COLOR", "PROPERTIES.HAS_COLOR");
        public static final Tag COMMON_ORE = tag("COMMON_ORE", "Properties.COMMON_ORE", "PROPERTIES.COMMON_ORE");
        public static final Tag EXPLODES_IN_NONVANILLA_CRAFTING_GRID = tag("EXPLODES_IN_NONVANILLA_CRAFTING_GRID", "Properties.EXPLODES_IN_NONVANILLA_CRAFTING_GRID", "PROPERTIES.EXPLODES_IN_NONVANILLA_CRAFTING_GRID");
        public static final Tag RANDOM_SMALL_GEM_ORE = tag("RANDOM_SMALL_GEM_ORE", "Properties.RANDOM_SMALL_GEM_ORE", "PROPERTIES.RANDOM_SMALL_GEM_ORE");
        public static final Tag AUTO_BLACKLIST = tag("AUTO_BLACKLIST", "Properties.AUTO_BLACKLIST", "PROPERTIES.AUTO_BLACKLIST");
        public static final Tag AUTO_MATERIAL = tag("AUTO_MATERIAL", "Properties.AUTO_MATERIAL", "PROPERTIES.AUTO_MATERIAL");
        public static final Tag INVALID_MATERIAL = tag("INVALID_MATERIAL", "Properties.INVALID_MATERIAL", "PROPERTIES.INVALID_MATERIAL");
        public static final Tag UNUSED_MATERIAL = tag("UNUSED_MATERIAL", "Properties.UNUSED_MATERIAL", "PROPERTIES.UNUSED_MATERIAL");
        public static final Tag IGNORE_IN_COLOR_LOG = tag("IGNORE_IN_COLOR_LOG", "Properties.IGNORE_IN_COLOR_LOG", "PROPERTIES.IGNORE_IN_COLOR_LOG");
        public static final Tag DONT_SHOW_THIS_COMPONENT = tag("DONT_SHOW_THIS_COMPONENT", "Properties.DONT_SHOW_THIS_COMPONENT", "PROPERTIES.DONT_SHOW_THIS_COMPONENT");
    }

    public static final class Compounds {
        private Compounds() {}

        public static final Tag ALLOY = tag("ALLOY", "Compounds.ALLOY", "COMPOUNDS.ALLOY");
        public static final Tag COATED = tag("COATED", "Compounds.COATED", "COMPOUNDS.COATED");
        public static final Tag LAYERED = tag("LAYERED", "Compounds.LAYERED", "COMPOUNDS.LAYERED");
        public static final Tag APPROXIMATE = tag("APPROXIMATE", "Compounds.APPROXIMATE", "COMPOUNDS.APPROXIMATE");
        public static final Tag DECOMPOSABLE = tag("DECOMPOSABLE", "Compounds.DECOMPOSABLE", "COMPOUNDS.DECOMPOSABLE");
    }

    public static final class Processing {
        private Processing() {}

        public static final Tag UNRECYCLABLE = tag("UNRECYCLABLE", "Processing.UNRECYCLABLE", "PROCESSING.UNRECYCLABLE");
        public static final Tag CENTRIFUGE = tag("CENTRIFUGE", "Processing.CENTRIFUGE", "PROCESSING.CENTRIFUGABLE");
        public static final Tag ELECTROLYSER = tag("ELECTROLYSER", "Processing.ELECTROLYSER", "PROCESSING.ELECTROLYSABLE");
        public static final Tag CRUCIBLE_ALLOY = tag("CRUCIBLE_ALLOY", "Processing.CRUCIBLE_ALLOY", "PROCESSING.CRUCIBLE_ALLOY");
        public static final Tag EXTRUDER = tag("EXTRUDER", "Processing.EXTRUDER", "PROCESSING.EXTRUDABLE");
        public static final Tag EXTRUDER_SIMPLE = tag("EXTRUDER_SIMPLE", "Processing.EXTRUDER_SIMPLE", "PROCESSING.EXTRUDABLE_SIMPLE");
        public static final Tag UUM = tag("UUM", "Processing.UUM", "PROCESSING.UUM_SYNTHESISABLE");
        public static final Tag FUSION = tag("FUSION", "Processing.FUSION", "PROCESSING.FUSION_SYNTHESISABLE");
        public static final Tag MORTAR = tag("MORTAR", "Processing.MORTAR", "PROCESSING.MORTAR_GRINDABLE");
        public static final Tag COOL2CRYSTAL = tag("COOL2CRYSTAL", "Processing.COOL2CRYSTAL", "PROCESSING.COOL2CRYSTAL");
        public static final Tag SMITHABLE = tag("SMITHABLE", "Processing.SMITHABLE", "PROCESSING.SMITHABLE");
        public static final Tag FURNACE = tag("FURNACE", "Processing.FURNACE", "PROCESSING.FURNACE");
        public static final Tag NEVER_FURNACE = tag("NEVER_FURNACE", "Processing.NEVER_FURNACE", "PROCESSING.NEVER_FURNACE");
        public static final Tag MELTING = tag("MELTING", "Processing.MELTING", "PROCESSING.MELTING");
        public static final Tag BLACKLISTED_SMELTER = tag("BLACKLISTED_SMELTER", "Processing.BLACKLISTED_SMELTER", "PROCESSING.BLACKLISTED_SMELTER");
        public static final Tag CRYSTALLISABLE = tag("CRYSTALLISABLE", "Processing.CRYSTALLISABLE", "PROCESSING.CRYSTALLISABLE");
        public static final Tag REACTS_WITH_GLASS = tag("REACTS_WITH_GLASS", "Processing.REACTS_WITH_GLASS", "PROCESSING.REACTS_WITH_GLASS");
        public static final Tag SOLDERING_MATERIAL = tag("SOLDERING_MATERIAL", "Processing.SOLDERING_MATERIAL", "PROCESSING.SOLDERING_MATERIAL");
        public static final Tag SOLDERING_MATERIAL_BAD = tag("SOLDERING_MATERIAL_BAD", "Processing.SOLDERING_MATERIAL_BAD", "PROCESSING.SOLDERING_MATERIAL_BAD");
        public static final Tag SOLDERING_MATERIAL_GOOD = tag("SOLDERING_MATERIAL_GOOD", "Processing.SOLDERING_MATERIAL_GOOD", "PROCESSING.SOLDERING_MATERIAL_GOOD");
        public static final Tag WASHING_FIRESTONE = tag("WASHING_FIRESTONE", "Processing.WASHING_FIRESTONE", "PROCESSING.WASHING_FIRESTONE");
        public static final Tag WASHING_PERSULFATE = tag("WASHING_PERSULFATE", "Processing.WASHING_PERSULFATE", "PROCESSING.WASHING_PERSULFATE");
        public static final Tag WASHING_MERCURY = tag("WASHING_MERCURY", "Processing.WASHING_MERCURY", "PROCESSING.WASHING_MERCURY");
        public static final Tag PULVERIZING_CINNABAR = tag("PULVERIZING_CINNABAR", "Processing.PULVERIZING_CINNABAR", "PROCESSING.PULVERIZING_CINNABAR");
    }

    public static Map<String, Tag> allTags() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    public static void bootstrap() {
        // Nested holders populate REGISTRY lazily  - touch one field from each group.
        if (Creative.HIDDEN == null
                || Projectiles.ARROW == null
                || Connectors.PIPE_FLUID == null
                || Energy.EU == null
                || Energy.ALL.isEmpty()
                || Energy.ALL_GT.contains(Energy.RF)
                || Prefix.ORE == null
                || Atomic.ELEMENT == null
                || Properties.WOOD == null
                || Compounds.ALLOY == null
                || Processing.MELTING == null
                || REGISTRY.isEmpty()) {
            throw new IllegalStateException("TD tags failed to initialize");
        }
    }
}
