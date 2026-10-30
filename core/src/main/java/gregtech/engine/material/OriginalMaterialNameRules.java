/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */
package gregtech.engine.material;

import gregtech.api.material.*;
import gregtech.engine.prefix.PrefixRegistry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Original GT6 material naming rules; material-specific rows live in {@code data/gt6ce/material_name_rules.tsv}. */
public final class OriginalMaterialNameRules {
    private OriginalMaterialNameRules() {}

    private static final boolean APRIL = aprilFools(LocalDate.now());
    private static final String RULES_RESOURCE = "/data/gt6ce/material_name_rules.tsv";
    private static final Map<String, List<Rule>> RULES = loadRules();

    public static boolean aprilFools(LocalDate day) {
        return day.getMonthValue() == 4 && day.getDayOfMonth() <= 2;
    }

    public static String name(String prefix, GTMaterial material, Function<GTMaterial, String> displayName) {
        return name(prefix, material, displayName, APRIL);
    }
    /** Null means a port-only prefix with no source naming rule. */
    public static String name(
            String prefixName, GTMaterial input, Function<GTMaterial, String> displayName, boolean aprilFools) {
        Prefix prefix = PREFIXES.get(PrefixRegistry.sourceName(prefixName));
        if (prefix == null) return null;
        GTMaterial material = input.resolve();
        String local = displayName.apply(material);

        if (prefix.name().equals("crateGtRaw")
                || prefix.name().equals("crateGt64Raw")
                || prefix.name().equals("blockRaw"))
            return prefix.before() + name("ore", material, displayName, aprilFools);
        if (prefix.name().equals("crateGtGem")
                || prefix.name().equals("crateGt64Gem")
                || prefix.name().equals("blockGem"))
            return prefix.before() + name("gem", material, displayName, aprilFools);
        if (prefix.name().equals("crateGtDust")
                || prefix.name().equals("crateGt64Dust")
                || prefix.name().equals("blockDust"))
            return prefix.before() + name("dust", material, displayName, aprilFools);
        if (prefix.name().equals("crateGtIngot")
                || prefix.name().equals("crateGt64Ingot")
                || prefix.name().equals("blockIngot"))
            return prefix.before() + name("ingot", material, displayName, aprilFools);
        if (prefix.name().equals("crateGtPlate")
                || prefix.name().equals("crateGt64Plate")
                || prefix.name().equals("blockPlate"))
            return prefix.before() + name("plate", material, displayName, aprilFools);
        if (prefix.name().equals("crateGtPlateGem")
                || prefix.name().equals("crateGt64PlateGem")
                || prefix.name().equals("blockPlateGem"))
            return prefix.before() + name("plateGem", material, displayName, aprilFools);

        if (material.getId() >= 0 && material.getId() < 10 && prefix.has("ORE"))
            return aprilFools ? "Schrödingers Ore" : "Unidentified Ore";

        if (aprilFools) {
            if (material == MaterialSentinels.EMPTY) {
                if (prefix.name().equals("bulletGtSmall")) return prefix.before() + "Bolt Shaft";
                if (prefix.name().equals("bulletGtMedium")) return prefix.before() + "Bolt Shaft";
                if (prefix.name().equals("bulletGtLarge")) return prefix.before() + "Bolt Shaft";
            } else {
                if (prefix.name().equals("bulletGtSmall")) return prefix.before() + local + " Bolt";
                if (prefix.name().equals("bulletGtMedium")) return prefix.before() + local + " Bolt";
                if (prefix.name().equals("bulletGtLarge")) return prefix.before() + local + " Bolt";
            }
        }

        if (material == MaterialSentinels.EMPTY) {
            if (prefix.name().equals("chemtube")) return "Empty Glass Tube";
            if (prefix.name().equals("arrowGtWood")) return "Headless Wood Arrow";
            if (prefix.name().equals("arrowGtPlastic")) return "Headless Plastic Arrow";
            if (prefix.name().equals("bulletGtSmall")) return "Small Bullet Casing";
            if (prefix.name().equals("bulletGtMedium")) return "Medium Bullet Casing";
            if (prefix.name().equals("bulletGtLarge")) return "Large Bullet Casing";
            if (prefix.has("ORE")) return "Unidentified Ore";
            if (prefix.has("ORE_PROCESSING_BASED")) return "Processed Unidentified Ore";
        }
        List<Rule> rules = RULES.get(material.getName());
        if (rules != null)
            for (Rule rule : rules) if (rule.matches(prefix)) return rule.render(prefix, local, displayName);

        if (material.has(MaterialProperty.WOOD)) {
            if (prefix.name().equals("rockGt")) return local;
            if (prefix.name().equals("scrapGt")) return local + " Splinters";
            if (prefix.name().startsWith("bolt")) return "Short " + local + " Stick";
            if (prefix.name().startsWith("stick")) return prefix.before() + local + " Stick";
            if (prefix.has("DUST_BASED")) return prefix.before() + local + " Pulp";
            if (prefix.name().equals("nugget")) return prefix.before() + local + " Chip";
            if (prefix.name().startsWith("plate")) return prefix.before() + local + " Plank";
        }
        if (material.has(MaterialProperty.STONE)) {
            if (prefix.name().equals("rockGt"))
                return local.endsWith("rock") ? (local + " §§§").replaceFirst("rock §§§", " Rock") : local + " Rock";
            if (prefix.name().equals("scrapGt")) return local + " Pebbles";
        }
        if (material.getId() > 0
                && material.getId() <= 830
                && material.getId() % 10 == 0
                && material.getMeltingPoint() > 273
                && material.getTargetCrushingMaterial().resolve() == material
                && material.has(MaterialProperty.SMITHABLE)) {
            if ((prefix.has("ORE") || prefix.has("ORE_PROCESSING_BASED")))
                return prefix.before() + "Native " + local + prefix.after();
        }

        return prefix.before() + local + prefix.after();
    }

    private interface PrefixMatch {
        boolean test(Prefix prefix);
    }

    private record Rule(PrefixMatch match, String template) {
        boolean matches(Prefix prefix) {
            return match.test(prefix);
        }

        String render(Prefix prefix, String local, Function<GTMaterial, String> displayName) {
            String value = template.replace("{before}", prefix.before())
                    .replace("{after}", prefix.after())
                    .replace("{local}", local);
            int start = value.indexOf("{display:");
            while (start >= 0) {
                int end = value.indexOf('}', start);
                if (end < 0) throw new IllegalStateException("Unclosed display placeholder in " + template);
                value = value.substring(0, start)
                        + displayName.apply(GTMaterialRegistry.get(value.substring(start + 9, end)))
                        + value.substring(end + 1);
                start = value.indexOf("{display:", start);
            }
            return value;
        }
    }

    private static Map<String, List<Rule>> loadRules() {
        try (var stream = OriginalMaterialNameRules.class.getResourceAsStream(RULES_RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing classpath resource " + RULES_RESOURCE);
            return parseRules(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + RULES_RESOURCE, e);
        }
    }

    private static Map<String, List<Rule>> parseRules(BufferedReader reader) throws IOException {
        Map<String, List<Rule>> rules = new LinkedHashMap<>();
        String line;
        int number = 0;
        while ((line = reader.readLine()) != null) {
            number++;
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            String[] columns = line.split("\t", -1);
            if (columns.length != 3)
                throw new IllegalStateException("Expected 3 columns on line " + number + " of " + RULES_RESOURCE);
            String names = columns[0].strip();
            String condition = columns[1].strip();
            String template = columns[2].strip();
            if (names.isEmpty() || condition.isEmpty() || template.isEmpty())
                throw new IllegalStateException("Empty column on line " + number + " of " + RULES_RESOURCE);
            Rule rule = new Rule(prefixMatch(condition, number), template);
            for (String name : names.split(",")) {
                String key = GTMaterial.sanitize(name.strip());
                if (key.isEmpty())
                    throw new IllegalStateException("Empty material name on line " + number + " of " + RULES_RESOURCE);
                rules.computeIfAbsent(key, ignored -> new ArrayList<>()).add(rule);
            }
        }
        rules.replaceAll((ignored, value) -> List.copyOf(value));
        return Map.copyOf(rules);
    }

    private static PrefixMatch prefixMatch(String condition, int line) {
        if (condition.equals("*")) return prefix -> true;
        if (condition.startsWith("=") && condition.length() > 1) {
            String exact = condition.substring(1);
            return prefix -> prefix.name().equals(exact);
        }
        if (condition.startsWith("#") && condition.length() > 1) {
            String[] tags = condition.substring(1).split("\\|");
            return prefix -> {
                for (String tag : tags) if (prefix.has(tag.strip())) return true;
                return false;
            };
        }
        if (condition.endsWith("*") && condition.length() > 1) {
            String start = condition.substring(0, condition.length() - 1);
            return prefix -> prefix.name().startsWith(start);
        }
        throw new IllegalStateException(
                "Bad prefix condition '" + condition + "' on line " + line + " of " + RULES_RESOURCE);
    }

    private record Prefix(String name, String before, String after, Set<String> tags) {
        boolean has(String tag) {
            return tags.contains(tag);
        }
    }

    private static final Map<String, Prefix> PREFIXES = Map.<String, Prefix>ofEntries(
            Map.entry("armor", new Prefix("armor", "", "", Set.of())),
            Map.entry("armorBoots", new Prefix("armorBoots", "", "", Set.of())),
            Map.entry("armorChestplate", new Prefix("armorChestplate", "", "", Set.of())),
            Map.entry("armorHelmet", new Prefix("armorHelmet", "", "", Set.of())),
            Map.entry("armorLeggings", new Prefix("armorLeggings", "", "", Set.of())),
            Map.entry("arrow", new Prefix("arrow", "", "", Set.of())),
            Map.entry("arrowGtPlastic", new Prefix("arrowGtPlastic", "Light ", " Arrow", Set.of())),
            Map.entry("arrowGtWood", new Prefix("arrowGtWood", "", " Arrow", Set.of())),
            Map.entry("beam", new Prefix("beam", "", "", Set.of())),
            Map.entry("billet", new Prefix("billet", "", " Billet", Set.of())),
            Map.entry("block", new Prefix("block", "", "", Set.of())),
            Map.entry("blockBamboo", new Prefix("blockBamboo", "", "", Set.of())),
            Map.entry("blockDust", new Prefix("blockDust", "Block of ", " Dust", Set.of("DUST_BASED"))),
            Map.entry("blockGem", new Prefix("blockGem", "Block of ", "", Set.of())),
            Map.entry("blockGlass", new Prefix("blockGlass", "", "", Set.of())),
            Map.entry("blockIngot", new Prefix("blockIngot", "Block of ", " Ingots", Set.of())),
            Map.entry("blockPlate", new Prefix("blockPlate", "Block of ", " Plates", Set.of())),
            Map.entry("blockPlateGem", new Prefix("blockPlateGem", "Block of ", " Gem Plates", Set.of())),
            Map.entry("blockRaw", new Prefix("blockRaw", "Block of ", " Ore", Set.of("ORE"))),
            Map.entry("blockSolid", new Prefix("blockSolid", "Block of solid ", "", Set.of())),
            Map.entry("blockWool", new Prefix("blockWool", "", "", Set.of())),
            Map.entry("block_", new Prefix("block_", "", "", Set.of())),
            Map.entry("bolt", new Prefix("bolt", "", " Bolt", Set.of())),
            Map.entry("book", new Prefix("book", "", "", Set.of())),
            Map.entry("bottle", new Prefix("bottle", "", " Bottle", Set.of("IS_CONTAINER"))),
            Map.entry("bouleGt", new Prefix("bouleGt", "", " Boule", Set.of())),
            Map.entry("bucket", new Prefix("bucket", "", " Bucket", Set.of("IS_CONTAINER"))),
            Map.entry("bulletGtLarge", new Prefix("bulletGtLarge", "Large ", " Bullet", Set.of())),
            Map.entry("bulletGtMedium", new Prefix("bulletGtMedium", "Medium ", " Bullet", Set.of())),
            Map.entry("bulletGtSmall", new Prefix("bulletGtSmall", "Small ", " Bullet", Set.of())),
            Map.entry("cableGt01", new Prefix("cableGt01", "1x ", " Cable", Set.of())),
            Map.entry("cableGt02", new Prefix("cableGt02", "2x ", " Cable", Set.of())),
            Map.entry("cableGt04", new Prefix("cableGt04", "4x ", " Cable", Set.of())),
            Map.entry("cableGt08", new Prefix("cableGt08", "8x ", " Cable", Set.of())),
            Map.entry("cableGt12", new Prefix("cableGt12", "12x ", " Cable", Set.of())),
            Map.entry("capcellcon", new Prefix("capcellcon", "", " Capsule Cell Container", Set.of())),
            Map.entry("capsule", new Prefix("capsule", "", " Capsule", Set.of("IS_CONTAINER"))),
            Map.entry("casingMachine", new Prefix("casingMachine", "", " Machine Casing", Set.of())),
            Map.entry("casingMachineDense", new Prefix("casingMachineDense", "Dense ", " Machine Casing", Set.of())),
            Map.entry("casingMachineDouble", new Prefix("casingMachineDouble", "Robust ", " Machine Casing", Set.of())),
            Map.entry(
                    "casingMachineQuadruple",
                    new Prefix("casingMachineQuadruple", "Reinforced ", " Machine Casing", Set.of())),
            Map.entry("casingSmall", new Prefix("casingSmall", "", " Item Casing", Set.of())),
            Map.entry("cell", new Prefix("cell", "", " Cell", Set.of("IS_CONTAINER"))),
            Map.entry("chain", new Prefix("chain", "", " Chain", Set.of())),
            Map.entry("chemtube", new Prefix("chemtube", "Glass Tube containing ", "", Set.of("IS_CONTAINER"))),
            Map.entry("chunk", new Prefix("chunk", "", "", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("chunkGt", new Prefix("chunkGt", "", " Chunk", Set.of())),
            Map.entry("cleanGravel", new Prefix("cleanGravel", "", "", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("clump", new Prefix("clump", "", "", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("cluster", new Prefix("cluster", "Native ", " Cluster", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("cobblestone", new Prefix("cobblestone", "", "", Set.of())),
            Map.entry("compressed", new Prefix("compressed", "Compressed ", "", Set.of())),
            Map.entry("compressedCobblestone", new Prefix("compressedCobblestone", "", "", Set.of())),
            Map.entry("compressedDirt", new Prefix("compressedDirt", "", "", Set.of())),
            Map.entry("compressedGravel", new Prefix("compressedGravel", "", "", Set.of())),
            Map.entry("compressedSand", new Prefix("compressedSand", "", "", Set.of())),
            Map.entry("compressedStone", new Prefix("compressedStone", "", "", Set.of())),
            Map.entry("craft", new Prefix("craft", "", "", Set.of())),
            Map.entry("crafting", new Prefix("crafting", "", "", Set.of())),
            Map.entry("craftingTool", new Prefix("craftingTool", "", "", Set.of())),
            Map.entry("crateGt64Dust", new Prefix("crateGt64Dust", "Crate of ", " Dusts", Set.of("IS_CONTAINER"))),
            Map.entry("crateGt64Gem", new Prefix("crateGt64Gem", "Crate of ", " Gems", Set.of("IS_CONTAINER"))),
            Map.entry("crateGt64Ingot", new Prefix("crateGt64Ingot", "Crate of ", " Ingots", Set.of("IS_CONTAINER"))),
            Map.entry("crateGt64Plate", new Prefix("crateGt64Plate", "Crate of ", " Plates", Set.of("IS_CONTAINER"))),
            Map.entry(
                    "crateGt64PlateGem",
                    new Prefix("crateGt64PlateGem", "Crate of ", " Gem Plates", Set.of("IS_CONTAINER"))),
            Map.entry("crateGt64Raw", new Prefix("crateGt64Raw", "Crate of ", " Ore", Set.of("IS_CONTAINER", "ORE"))),
            Map.entry("crateGtDust", new Prefix("crateGtDust", "Partial Crate of ", " Dusts", Set.of("IS_CONTAINER"))),
            Map.entry("crateGtGem", new Prefix("crateGtGem", "Partial Crate of ", " Gems", Set.of("IS_CONTAINER"))),
            Map.entry(
                    "crateGtIngot", new Prefix("crateGtIngot", "Partial Crate of ", " Ingots", Set.of("IS_CONTAINER"))),
            Map.entry(
                    "crateGtPlate", new Prefix("crateGtPlate", "Partial Crate of ", " Plates", Set.of("IS_CONTAINER"))),
            Map.entry(
                    "crateGtPlateGem",
                    new Prefix("crateGtPlateGem", "Partial Crate of ", " Gem Plates", Set.of("IS_CONTAINER"))),
            Map.entry(
                    "crateGtRaw", new Prefix("crateGtRaw", "Partial Crate of ", " Ore", Set.of("IS_CONTAINER", "ORE"))),
            Map.entry("crushed", new Prefix("crushed", "Crushed ", " Ore", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry(
                    "crushedCentrifuged",
                    new Prefix("crushedCentrifuged", "Refined ", " Ore", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry(
                    "crushedCentrifugedTiny",
                    new Prefix("crushedCentrifugedTiny", "Tiny Refined ", " Ore", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry(
                    "crushedPurified",
                    new Prefix("crushedPurified", "Purified ", " Ore", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry(
                    "crushedPurifiedTiny",
                    new Prefix("crushedPurifiedTiny", "Tiny Purified ", " Ore", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry(
                    "crushedTiny", new Prefix("crushedTiny", "Tiny Crushed ", " Ore", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("crystal", new Prefix("crystal", "", " Crystal", Set.of())),
            Map.entry("crystalPure", new Prefix("crystalPure", "Pure ", " Crystal", Set.of())),
            Map.entry("crystalline", new Prefix("crystalline", "", "", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("dirtyGravel", new Prefix("dirtyGravel", "", "", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("dust", new Prefix("dust", "", " Dust", Set.of("DUST_BASED"))),
            Map.entry("dustDiv72", new Prefix("dustDiv72", "1/72nd of a Pile of ", " Dust", Set.of("DUST_BASED"))),
            Map.entry(
                    "dustImpure", new Prefix("dustImpure", "Impure Pile of ", " Dust", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("dustPure", new Prefix("dustPure", "Purified Pile of ", " Dust", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry(
                    "dustRefined",
                    new Prefix("dustRefined", "Refined Pile of ", " Dust", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("dustSmall", new Prefix("dustSmall", "Small Pile of ", " Dust", Set.of("DUST_BASED"))),
            Map.entry("dustTiny", new Prefix("dustTiny", "Tiny Pile of ", " Dust", Set.of("DUST_BASED"))),
            Map.entry("dye", new Prefix("dye", "", "", Set.of())),
            Map.entry("dyeCeramic", new Prefix("dyeCeramic", "", "", Set.of())),
            Map.entry("dyeMixable", new Prefix("dyeMixable", "", "", Set.of())),
            Map.entry("fence", new Prefix("fence", "", "", Set.of())),
            Map.entry("foil", new Prefix("foil", "", " Foil", Set.of())),
            Map.entry("frameGt", new Prefix("frameGt", "", "", Set.of())),
            Map.entry("gearGt", new Prefix("gearGt", "", " Gear", Set.of())),
            Map.entry("gearGtSmall", new Prefix("gearGtSmall", "Small ", " Gear", Set.of())),
            Map.entry("gem", new Prefix("gem", "", "", Set.of())),
            Map.entry("gemChipped", new Prefix("gemChipped", "Chipped ", "", Set.of())),
            Map.entry("gemExquisite", new Prefix("gemExquisite", "Exquisite ", "", Set.of())),
            Map.entry("gemFlawed", new Prefix("gemFlawed", "Flawed ", "", Set.of())),
            Map.entry("gemFlawless", new Prefix("gemFlawless", "Flawless ", "", Set.of())),
            Map.entry("gemLegendary", new Prefix("gemLegendary", "Legendary ", "", Set.of())),
            Map.entry("gemOre", new Prefix("gemOre", "", " Ore", Set.of())),
            Map.entry("gemPolished", new Prefix("gemPolished", "Polished ", "", Set.of())),
            Map.entry("gemRaw", new Prefix("gemRaw", "Raw ", "", Set.of())),
            Map.entry("gemUncut", new Prefix("gemUncut", "Uncut ", "", Set.of())),
            Map.entry("glass", new Prefix("glass", "", "", Set.of())),
            Map.entry("ingot", new Prefix("ingot", "", " Ingot", Set.of())),
            Map.entry("ingotDouble", new Prefix("ingotDouble", "Double ", " Ingot", Set.of())),
            Map.entry("ingotHot", new Prefix("ingotHot", "Hot ", " Ingot", Set.of())),
            Map.entry("ingotQuadruple", new Prefix("ingotQuadruple", "Quadruple ", " Ingot", Set.of())),
            Map.entry("ingotQuintuple", new Prefix("ingotQuintuple", "Quintuple ", " Ingot", Set.of())),
            Map.entry("ingotTriple", new Prefix("ingotTriple", "Triple ", " Ingot", Set.of())),
            Map.entry("item", new Prefix("item", "", "", Set.of())),
            Map.entry("item_", new Prefix("item_", "", "", Set.of())),
            Map.entry("lens", new Prefix("lens", "", " Lens", Set.of())),
            Map.entry("log", new Prefix("log", "", "", Set.of())),
            Map.entry("minecartWheels", new Prefix("minecartWheels", "", " Cart Wheels", Set.of())),
            Map.entry("nugget", new Prefix("nugget", "", " Nugget", Set.of())),
            Map.entry("ore", new Prefix("ore", "", " Ore", Set.of("ORE"))),
            Map.entry("oreAndesite", new Prefix("oreAndesite", "Andesite ", " Ore", Set.of("ORE"))),
            Map.entry("oreBasalt", new Prefix("oreBasalt", "Basalt ", " Ore", Set.of("ORE"))),
            Map.entry("oreBedrock", new Prefix("oreBedrock", "Bedrock ", " Ore", Set.of("ORE"))),
            Map.entry("oreBetweenstone", new Prefix("oreBetweenstone", "Betweenstone ", " Ore", Set.of("ORE"))),
            Map.entry("oreBlackgranite", new Prefix("oreBlackgranite", "Granite ", " Ore", Set.of("ORE"))),
            Map.entry("oreBlackstone", new Prefix("oreBlackstone", "Blackstone ", " Ore", Set.of("ORE"))),
            Map.entry("oreBlueschist", new Prefix("oreBlueschist", "Schist ", " Ore", Set.of("ORE"))),
            Map.entry("oreCallisto", new Prefix("oreCallisto", "Callisto ", " Ore", Set.of("ORE"))),
            Map.entry("oreCeres", new Prefix("oreCeres", "Ceres ", " Ore", Set.of("ORE"))),
            Map.entry("oreDarkprismarine", new Prefix("oreDarkprismarine", "Prismarine ", " Ore", Set.of("ORE"))),
            Map.entry("oreDeadrock", new Prefix("oreDeadrock", "Deadrock ", " Ore", Set.of("ORE"))),
            Map.entry("oreDeepslate", new Prefix("oreDeepslate", "Deepslate ", " Ore", Set.of("ORE"))),
            Map.entry("oreDeimos", new Prefix("oreDeimos", "Deimos ", " Ore", Set.of("ORE"))),
            Map.entry("oreDense", new Prefix("oreDense", "Dense ", " Ore", Set.of("ORE"))),
            Map.entry("oreDiorite", new Prefix("oreDiorite", "Diorite ", " Ore", Set.of("ORE"))),
            Map.entry("oreEnd", new Prefix("oreEnd", "End ", " Ore", Set.of("ORE"))),
            Map.entry("oreEndstone", new Prefix("oreEndstone", "End ", " Ore", Set.of("ORE"))),
            Map.entry("oreEris", new Prefix("oreEris", "Eris ", " Ore", Set.of("ORE"))),
            Map.entry("oreEuropa", new Prefix("oreEuropa", "Europa ", " Ore", Set.of("ORE"))),
            Map.entry("oreGanymede", new Prefix("oreGanymede", "Ganymede ", " Ore", Set.of("ORE"))),
            Map.entry("oreGneiss", new Prefix("oreGneiss", "Gneiss ", " Ore", Set.of("ORE"))),
            Map.entry("oreGravel", new Prefix("oreGravel", "Gravel ", " Ore", Set.of("ORE"))),
            Map.entry("oreGrayschist", new Prefix("oreGrayschist", "Schist ", " Ore", Set.of("ORE"))),
            Map.entry("oreGreenschist", new Prefix("oreGreenschist", "Schist ", " Ore", Set.of("ORE"))),
            Map.entry("oreHolystone", new Prefix("oreHolystone", "Holystone ", " Ore", Set.of("ORE"))),
            Map.entry("oreIapetus", new Prefix("oreIapetus", "Iapetus ", " Ore", Set.of("ORE"))),
            Map.entry("oreIo", new Prefix("oreIo", "Io ", " Ore", Set.of("ORE"))),
            Map.entry("oreJupiter", new Prefix("oreJupiter", "Jupiter ", " Ore", Set.of("ORE"))),
            Map.entry("oreKepler22b", new Prefix("oreKepler22b", "Kepler22b ", " Ore", Set.of("ORE"))),
            Map.entry("oreKimberlite", new Prefix("oreKimberlite", "Kimberlite ", " Ore", Set.of("ORE"))),
            Map.entry("oreKomatiite", new Prefix("oreKomatiite", "Komatiite ", " Ore", Set.of("ORE"))),
            Map.entry("oreLightprismarine", new Prefix("oreLightprismarine", "Prismarine ", " Ore", Set.of("ORE"))),
            Map.entry("oreLimestone", new Prefix("oreLimestone", "Limestone ", " Ore", Set.of("ORE"))),
            Map.entry("oreLivingrock", new Prefix("oreLivingrock", "Livingrock ", " Ore", Set.of("ORE"))),
            Map.entry("oreMarble", new Prefix("oreMarble", "Marble ", " Ore", Set.of("ORE"))),
            Map.entry("oreMars", new Prefix("oreMars", "Mars ", " Ore", Set.of("ORE"))),
            Map.entry("oreMercury", new Prefix("oreMercury", "Mercury ", " Ore", Set.of("ORE"))),
            Map.entry("oreMoon", new Prefix("oreMoon", "Moon ", " Ore", Set.of("ORE"))),
            Map.entry("oreMud", new Prefix("oreMud", "Mud ", " Ore", Set.of("ORE"))),
            Map.entry("oreNeptune", new Prefix("oreNeptune", "Neptune ", " Ore", Set.of("ORE"))),
            Map.entry("oreNether", new Prefix("oreNether", "Nether ", " Ore", Set.of("ORE"))),
            Map.entry("oreNetherrack", new Prefix("oreNetherrack", "Nether ", " Ore", Set.of("ORE"))),
            Map.entry("oreNormal", new Prefix("oreNormal", "Normal ", " Ore", Set.of("ORE"))),
            Map.entry("oreOberon", new Prefix("oreOberon", "Oberon ", " Ore", Set.of("ORE"))),
            Map.entry("orePhobos", new Prefix("orePhobos", "Phobos ", " Ore", Set.of("ORE"))),
            Map.entry("orePinkschist", new Prefix("orePinkschist", "Schist ", " Ore", Set.of("ORE"))),
            Map.entry("orePitstone", new Prefix("orePitstone", "Pitstone ", " Ore", Set.of("ORE"))),
            Map.entry("orePluto", new Prefix("orePluto", "Pluto ", " Ore", Set.of("ORE"))),
            Map.entry("orePoor", new Prefix("orePoor", "Poor ", " Ore", Set.of("ORE"))),
            Map.entry("oreQuartzite", new Prefix("oreQuartzite", "Quartzite ", " Ore", Set.of("ORE"))),
            Map.entry("oreRaw", new Prefix("oreRaw", "Raw ", " Ore", Set.of("ORE"))),
            Map.entry("oreRedSand", new Prefix("oreRedSand", "Sand ", " Ore", Set.of("ORE"))),
            Map.entry("oreRedgranite", new Prefix("oreRedgranite", "Granite ", " Ore", Set.of("ORE"))),
            Map.entry("oreRhea", new Prefix("oreRhea", "Rhea ", " Ore", Set.of("ORE"))),
            Map.entry("oreRich", new Prefix("oreRich", "Rich ", " Ore", Set.of("ORE"))),
            Map.entry("oreSand", new Prefix("oreSand", "Sand ", " Ore", Set.of("ORE"))),
            Map.entry("oreSandstone", new Prefix("oreSandstone", "Sandstone ", " Ore", Set.of("ORE"))),
            Map.entry("oreSaturn", new Prefix("oreSaturn", "Saturn ", " Ore", Set.of("ORE"))),
            Map.entry("oreShale", new Prefix("oreShale", "Shale ", " Ore", Set.of("ORE"))),
            Map.entry("oreSiltstone", new Prefix("oreSiltstone", "Siltstone ", " Ore", Set.of("ORE"))),
            Map.entry("oreSlate", new Prefix("oreSlate", "Slate ", " Ore", Set.of("ORE"))),
            Map.entry("oreSmall", new Prefix("oreSmall", "Small ", " Ore", Set.of("ORE"))),
            Map.entry("oreSpace", new Prefix("oreSpace", "Space ", " Ore", Set.of("ORE"))),
            Map.entry("oreStrangesand", new Prefix("oreStrangesand", "Strange Sand ", " Ore", Set.of("ORE"))),
            Map.entry("oreTitan", new Prefix("oreTitan", "Titan ", " Ore", Set.of("ORE"))),
            Map.entry("oreTitania", new Prefix("oreTitania", "Titania ", " Ore", Set.of("ORE"))),
            Map.entry("oreTriton", new Prefix("oreTriton", "Triton ", " Ore", Set.of("ORE"))),
            Map.entry("oreUmberstone", new Prefix("oreUmberstone", "Umberstone ", " Ore", Set.of("ORE"))),
            Map.entry("oreUranus", new Prefix("oreUranus", "Uranus ", " Ore", Set.of("ORE"))),
            Map.entry("oreVanillagranite", new Prefix("oreVanillagranite", "Granite ", " Ore", Set.of("ORE"))),
            Map.entry("oreVanillastone", new Prefix("oreVanillastone", "Stone ", " Ore", Set.of("ORE"))),
            Map.entry("oreVenus", new Prefix("oreVenus", "Venus ", " Ore", Set.of("ORE"))),
            Map.entry("oreberry", new Prefix("oreberry", "", " Berry", Set.of())),
            Map.entry("orebush", new Prefix("orebush", "", " Bush", Set.of())),
            Map.entry("paneGlass", new Prefix("paneGlass", "", "", Set.of())),
            Map.entry("paper", new Prefix("paper", "", "", Set.of())),
            Map.entry("pebbles", new Prefix("pebbles", "", "", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("pipe", new Prefix("pipe", "", " Pipe", Set.of())),
            Map.entry("pipeHuge", new Prefix("pipeHuge", "Huge ", " Pipe", Set.of())),
            Map.entry("pipeLarge", new Prefix("pipeLarge", "Large ", " Pipe", Set.of())),
            Map.entry("pipeMedium", new Prefix("pipeMedium", "Medium ", " Pipe", Set.of())),
            Map.entry("pipeNonuple", new Prefix("pipeNonuple", "Nonuple ", " Pipe", Set.of())),
            Map.entry("pipeQuadruple", new Prefix("pipeQuadruple", "Quadruple ", " Pipe", Set.of())),
            Map.entry("pipeRestrictiveHuge", new Prefix("pipeRestrictiveHuge", "Huge Restrictive ", " Pipe", Set.of())),
            Map.entry(
                    "pipeRestrictiveLarge",
                    new Prefix("pipeRestrictiveLarge", "Large Restrictive ", " Pipe", Set.of())),
            Map.entry(
                    "pipeRestrictiveMedium",
                    new Prefix("pipeRestrictiveMedium", "Medium Restrictive ", " Pipe", Set.of())),
            Map.entry(
                    "pipeRestrictiveSmall",
                    new Prefix("pipeRestrictiveSmall", "Small Restrictive ", " Pipe", Set.of())),
            Map.entry("pipeRestrictiveTiny", new Prefix("pipeRestrictiveTiny", "Tiny Restrictive ", " Pipe", Set.of())),
            Map.entry("pipeSmall", new Prefix("pipeSmall", "Small ", " Pipe", Set.of())),
            Map.entry("pipeTiny", new Prefix("pipeTiny", "Tiny ", " Pipe", Set.of())),
            Map.entry("plank", new Prefix("plank", "", "", Set.of())),
            Map.entry("plantGtBerry", new Prefix("plantGtBerry", "", " Berry", Set.of())),
            Map.entry("plantGtBlossom", new Prefix("plantGtBlossom", "", " Blossom", Set.of())),
            Map.entry("plantGtFiber", new Prefix("plantGtFiber", "Raw ", " Fiber", Set.of())),
            Map.entry("plantGtTwig", new Prefix("plantGtTwig", "", " Twig", Set.of())),
            Map.entry("plantGtWart", new Prefix("plantGtWart", "", " Wart", Set.of())),
            Map.entry("plate", new Prefix("plate", "", " Plate", Set.of())),
            Map.entry("plateCurved", new Prefix("plateCurved", "Curved ", " Plate", Set.of())),
            Map.entry("plateDense", new Prefix("plateDense", "Dense ", " Plate", Set.of())),
            Map.entry("plateDouble", new Prefix("plateDouble", "Double ", " Plate", Set.of())),
            Map.entry("plateGem", new Prefix("plateGem", "Crystalline ", " Plate", Set.of())),
            Map.entry("plateGemTiny", new Prefix("plateGemTiny", "Tiny Crystalline ", " Plate", Set.of())),
            Map.entry("plateQuadruple", new Prefix("plateQuadruple", "Quadruple ", " Plate", Set.of())),
            Map.entry("plateQuintuple", new Prefix("plateQuintuple", "Quintuple ", " Plate", Set.of())),
            Map.entry("plateSteamcraft", new Prefix("plateSteamcraft", "Thin ", " Plate", Set.of())),
            Map.entry("plateTiny", new Prefix("plateTiny", "Tiny ", " Plate", Set.of())),
            Map.entry("plateTriple", new Prefix("plateTriple", "Triple ", " Plate", Set.of())),
            Map.entry("railGt", new Prefix("railGt", "", " Rail", Set.of())),
            Map.entry(
                    "rawOreChunk", new Prefix("rawOreChunk", "Raw Chunk of ", " Ore", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("record", new Prefix("record", "", "", Set.of())),
            Map.entry("reduced", new Prefix("reduced", "", "", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("ring", new Prefix("ring", "", " Ring", Set.of())),
            Map.entry("rock", new Prefix("rock", "", "", Set.of())),
            Map.entry("rockGt", new Prefix("rockGt", "", " bearing Rock", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("rotor", new Prefix("rotor", "", " Rotor", Set.of())),
            Map.entry("round", new Prefix("round", "", " Round", Set.of())),
            Map.entry("rubble", new Prefix("rubble", "", "", Set.of("ORE_PROCESSING_BASED"))),
            Map.entry("scrap", new Prefix("scrap", "", "", Set.of())),
            Map.entry("scrapGt", new Prefix("scrapGt", "", " Scrap", Set.of())),
            Map.entry("scraps", new Prefix("scraps", "", "", Set.of())),
            Map.entry("screw", new Prefix("screw", "", " Screw", Set.of())),
            Map.entry("sheetGt", new Prefix("sheetGt", "", " Sheet", Set.of())),
            Map.entry("slab", new Prefix("slab", "", "", Set.of())),
            Map.entry("spring", new Prefix("spring", "", " Spring", Set.of())),
            Map.entry("springSmall", new Prefix("springSmall", "Small ", " Spring", Set.of())),
            Map.entry("stainedClay", new Prefix("stainedClay", "", "", Set.of())),
            Map.entry("stair", new Prefix("stair", "", "", Set.of())),
            Map.entry("stick", new Prefix("stick", "", " Rod", Set.of())),
            Map.entry("stickLong", new Prefix("stickLong", "Long ", " Rod", Set.of())),
            Map.entry("stone", new Prefix("stone", "", "", Set.of())),
            Map.entry("stoneBricks", new Prefix("stoneBricks", "", "", Set.of())),
            Map.entry("stoneChiseled", new Prefix("stoneChiseled", "", "", Set.of())),
            Map.entry("stoneCobble", new Prefix("stoneCobble", "", "", Set.of())),
            Map.entry("stoneCracked", new Prefix("stoneCracked", "", "", Set.of())),
            Map.entry("stoneMossy", new Prefix("stoneMossy", "", "", Set.of())),
            Map.entry("stoneMossyBricks", new Prefix("stoneMossyBricks", "", "", Set.of())),
            Map.entry("stonePolished", new Prefix("stonePolished", "", "", Set.of())),
            Map.entry("stoneSmooth", new Prefix("stoneSmooth", "", "", Set.of())),
            Map.entry("tool", new Prefix("tool", "", "", Set.of())),
            Map.entry("toolAxe", new Prefix("toolAxe", "", "", Set.of())),
            Map.entry("toolHeadArrow", new Prefix("toolHeadArrow", "", " Arrow Head", Set.of())),
            Map.entry("toolHeadAxe", new Prefix("toolHeadAxe", "", " Axe Head", Set.of())),
            Map.entry("toolHeadAxeDouble", new Prefix("toolHeadAxeDouble", "", " Double Axe Head", Set.of())),
            Map.entry("toolHeadBuilderwand", new Prefix("toolHeadBuilderwand", "", " Builder's Wand Cap", Set.of())),
            Map.entry("toolHeadBuzzSaw", new Prefix("toolHeadBuzzSaw", "", " Buzzsaw Blade", Set.of())),
            Map.entry("toolHeadChainsaw", new Prefix("toolHeadChainsaw", "", " Chainsaw Tip", Set.of())),
            Map.entry("toolHeadChisel", new Prefix("toolHeadChisel", "", " Chisel Head", Set.of())),
            Map.entry(
                    "toolHeadConstructionPickaxe",
                    new Prefix("toolHeadConstructionPickaxe", "", " Construction Pickaxe Head", Set.of())),
            Map.entry("toolHeadDrill", new Prefix("toolHeadDrill", "", " Drill Tip", Set.of())),
            Map.entry("toolHeadFile", new Prefix("toolHeadFile", "", " File Head", Set.of())),
            Map.entry("toolHeadHammer", new Prefix("toolHeadHammer", "", " Hammer Head", Set.of())),
            Map.entry("toolHeadHoe", new Prefix("toolHeadHoe", "", " Hoe Head", Set.of())),
            Map.entry("toolHeadPickaxe", new Prefix("toolHeadPickaxe", "", " Pickaxe Head", Set.of())),
            Map.entry("toolHeadPickaxeGem", new Prefix("toolHeadPickaxeGem", "", " tipped Pickaxe Head", Set.of())),
            Map.entry("toolHeadPlow", new Prefix("toolHeadPlow", "", " Plow Head", Set.of())),
            Map.entry("toolHeadRawArrow", new Prefix("toolHeadRawArrow", "Raw ", " Arrow Head", Set.of())),
            Map.entry("toolHeadRawAxe", new Prefix("toolHeadRawAxe", "Raw ", " Axe Head", Set.of())),
            Map.entry("toolHeadRawAxeDouble", new Prefix("toolHeadRawAxeDouble", "Raw ", " Double Axe Head", Set.of())),
            Map.entry("toolHeadRawChisel", new Prefix("toolHeadRawChisel", "Raw ", " Chisel Head", Set.of())),
            Map.entry("toolHeadRawHoe", new Prefix("toolHeadRawHoe", "Raw ", " Hoe Head", Set.of())),
            Map.entry("toolHeadRawPickaxe", new Prefix("toolHeadRawPickaxe", "Raw ", " Pickaxe Head", Set.of())),
            Map.entry("toolHeadRawPlow", new Prefix("toolHeadRawPlow", "Raw ", " Plow Head", Set.of())),
            Map.entry("toolHeadRawSaw", new Prefix("toolHeadRawSaw", "Raw ", " Saw Blade", Set.of())),
            Map.entry("toolHeadRawSense", new Prefix("toolHeadRawSense", "Raw ", " Sense Blade", Set.of())),
            Map.entry("toolHeadRawShovel", new Prefix("toolHeadRawShovel", "Raw ", " Shovel Head", Set.of())),
            Map.entry("toolHeadRawSpade", new Prefix("toolHeadRawSpade", "Raw ", " Spade Head", Set.of())),
            Map.entry("toolHeadRawSword", new Prefix("toolHeadRawSword", "Raw ", " Sword Blade", Set.of())),
            Map.entry(
                    "toolHeadRawUniversalSpade",
                    new Prefix("toolHeadRawUniversalSpade", "Raw ", " Universal Spade Head", Set.of())),
            Map.entry("toolHeadSaw", new Prefix("toolHeadSaw", "", " Saw Blade", Set.of())),
            Map.entry("toolHeadScrewdriver", new Prefix("toolHeadScrewdriver", "", " Screwdriver Tip", Set.of())),
            Map.entry("toolHeadSense", new Prefix("toolHeadSense", "", " Sense Blade", Set.of())),
            Map.entry("toolHeadShovel", new Prefix("toolHeadShovel", "", " Shovel Head", Set.of())),
            Map.entry("toolHeadSpade", new Prefix("toolHeadSpade", "", " Spade Head", Set.of())),
            Map.entry("toolHeadSword", new Prefix("toolHeadSword", "", " Sword Blade", Set.of())),
            Map.entry(
                    "toolHeadUniversalSpade",
                    new Prefix("toolHeadUniversalSpade", "", " Universal Spade Head", Set.of())),
            Map.entry("toolHeadWrench", new Prefix("toolHeadWrench", "", " Wrench Tip", Set.of())),
            Map.entry("toolHoe", new Prefix("toolHoe", "", "", Set.of())),
            Map.entry("toolPickaxe", new Prefix("toolPickaxe", "", "", Set.of())),
            Map.entry("toolShears", new Prefix("toolShears", "", "", Set.of())),
            Map.entry("toolShovel", new Prefix("toolShovel", "", "", Set.of())),
            Map.entry("toolSword", new Prefix("toolSword", "", "", Set.of())),
            Map.entry("tree", new Prefix("tree", "", "", Set.of())),
            Map.entry("treeLeaves", new Prefix("treeLeaves", "", "", Set.of())),
            Map.entry("treeSapling", new Prefix("treeSapling", "", "", Set.of())),
            Map.entry("wireFine", new Prefix("wireFine", "Fine ", " Wire", Set.of())),
            Map.entry("wireGt01", new Prefix("wireGt01", "1x ", " Wire", Set.of())),
            Map.entry("wireGt02", new Prefix("wireGt02", "2x ", " Wire", Set.of())),
            Map.entry("wireGt03", new Prefix("wireGt03", "3x ", " Wire", Set.of())),
            Map.entry("wireGt04", new Prefix("wireGt04", "4x ", " Wire", Set.of())),
            Map.entry("wireGt05", new Prefix("wireGt05", "5x ", " Wire", Set.of())),
            Map.entry("wireGt06", new Prefix("wireGt06", "6x ", " Wire", Set.of())),
            Map.entry("wireGt07", new Prefix("wireGt07", "7x ", " Wire", Set.of())),
            Map.entry("wireGt08", new Prefix("wireGt08", "8x ", " Wire", Set.of())),
            Map.entry("wireGt09", new Prefix("wireGt09", "9x ", " Wire", Set.of())),
            Map.entry("wireGt10", new Prefix("wireGt10", "10x ", " Wire", Set.of())),
            Map.entry("wireGt11", new Prefix("wireGt11", "11x ", " Wire", Set.of())),
            Map.entry("wireGt12", new Prefix("wireGt12", "12x ", " Wire", Set.of())),
            Map.entry("wireGt13", new Prefix("wireGt13", "13x ", " Wire", Set.of())),
            Map.entry("wireGt14", new Prefix("wireGt14", "14x ", " Wire", Set.of())),
            Map.entry("wireGt15", new Prefix("wireGt15", "15x ", " Wire", Set.of())),
            Map.entry("wireGt16", new Prefix("wireGt16", "16x ", " Wire", Set.of())));
}
