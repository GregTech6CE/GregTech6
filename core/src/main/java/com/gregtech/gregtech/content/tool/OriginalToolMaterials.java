package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.api.tool.ToolDefinition;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.Map;
import java.util.Set;

/** GT6 MT/ANY handle families and Loader_Tools material conditions. */
public final class OriginalToolMaterials {
    private OriginalToolMaterials() {}
    private static final Map<String, String> HANDLES = Map.ofEntries(
        Map.entry("GaiaSpirit", "ElvenElementium"), Map.entry("Endium", "Endstone"),
        Map.entry("SpectreIron", "Obsidian"), Map.entry("DarkMatter", "Diamond"),
        Map.entry("RedMatter", "DarkMatter"), Map.entry("Desh", "Desh"),
        Map.entry("Ethaxium", "Ethaxium"), Map.entry("Vibranium", "VibraniumSteel"),
        Map.entry("VibraniumSteel", "VibraniumSteel"), Map.entry("VibraniumSilver", "VibraniumSteel"),
        Map.entry("Vibramantium", "Vibramantium"));
    private static final Set<String> MAGIC_WOOD_HEADS = Set.of("Mauftrium", "Elvorium", "MuspelheimPower", "NiflheimPower",
        "ElvenElementium", "ElvenDragonstone", "Manasteel", "Terrasteel", "ManaDiamond", "Thaumium");
    private static final Set<String> BLAZE_HEADS = Set.of("FierySteel", "Fireleaf", "MeteoflameSteel", "MeteoflameBlackSteel",
        "MeteoflameBlueSteel", "MeteoflameRedSteel", "FlamascusSteel", "Firestone");
    private static final Set<String> IRON_HEADS = Set.of("EnderAmethyst", "Meteorite", "Kreknorite", "Sugilite");
    private static final Set<String> MAGIC_IRON_HEADS = Set.of("VoidMetal", "InfusedAir", "InfusedBalance", "InfusedDull",
        "InfusedEarth", "InfusedEntropy", "InfusedFire", "InfusedOrder", "InfusedWater", "InfusedVis", "DarkThaumium");
    private static final Set<String> MAGIC_WOODS = Set.of("Greatwood", "Silverwood", "Livingwood", "Dreamwood", "Shimmerwood",
        "Magic", "Tainted", "Witchwood", "Rainbowood");
    private static final Set<String> MAGIC_IRONS = Set.of("Manasteel", "Thaumium", "DarkThaumium", "SpectreIron", "FierySteel", "MeteoflameSteel");
    private static String name(GTMaterial material) { return material.getName().replace(" ", ""); }
    public static boolean inFamily(GTMaterial material, String family) {
        if (material == null) return false;
        String name = name(material);
        if (name.equals(family)) return true;
        if (material.getReRegistrations().stream().anyMatch(m -> name(m).equals(family))) return true;
        return switch (family) {
            case "WoodPlastic" -> material.has(MaterialProperty.WOOD) || name.equals("PetrifiedWood") || inFamily(material, "Plastic");
            case "Plastic" -> Set.of("Plastic", "HardPlastic", "Polycarbonate", "PVC", "Teflon", "Bakelite").contains(name);
            case "WoodMagical" -> MAGIC_WOODS.contains(name) || (material.has(MaterialProperty.WOOD) && material.has(MaterialProperty.MAGICAL));
            case "MagicIron" -> MAGIC_IRONS.contains(name);
            case "Rubber" -> material.getTextureSet() == MaterialTextureSet.RUBBER;
            case "Iron" -> Set.of("Iron", "WroughtIron", "IronMagnetic").contains(name);
            case "Steel" -> Set.of("Steel", "SteelMagnetic", "HSLA", "SpringSteel").contains(name);
            default -> false;
        };
    }
    private static String handleFamily(GTMaterial head) {
        String name = name(head);
        if (HANDLES.containsKey(name)) return HANDLES.get(name);
        if (MAGIC_IRON_HEADS.contains(name)) return "MagicIron";
        if (MAGIC_WOOD_HEADS.contains(name) || name.startsWith("Infused") && head.getTextureSet().name().contains("SHARD")) return "WoodMagical";
        if (BLAZE_HEADS.contains(name) || name.equals("Blaze")) return "Blaze";
        if (IRON_HEADS.contains(name)) return "Iron";
        return "WoodPlastic";
    }
    public static boolean acceptsHandle(GTMaterial head, GTMaterial handle) {
        return head != null && handle != null && MaterialPrefix.stick.isValidFor(handle) && inFamily(handle, handleFamily(head));
    }
    public static GTMaterial defaultHandle(GTMaterial head) {
        GTMaterial wood = GTMaterialRegistry.get("Wood");
        if (acceptsHandle(head, wood)) return wood;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) if (acceptsHandle(head, material)) return material;
        return null;
    }
    public static boolean earlyHandle(GTMaterial handle) {
        return handle != null && (inFamily(handle, "WoodPlastic") || Set.of("Bone", "Bamboo").contains(name(handle)));
    }
    public static boolean soft(GTMaterial material) {
        return material.has(MaterialProperty.WOOD) || inFamily(material, "Rubber") || inFamily(material, "Plastic");
    }
    /** AdvancedCraftingTool permits already formed heads; Loader_Tools gates forming them separately. */
    public static boolean acceptsAssemblyHead(ToolDefinition type, GTMaterial material) {
        if (material == null || !material.isValid()) return false;
        return switch (type) {
            case SOFT_HAMMER -> soft(material);
            case HARD_HAMMER -> !soft(material);
            case MAGNIFYING_GLASS -> material.getToolTypes() >= 1;
            default -> type.canUseHead(material);
        };
    }
    public static boolean acceptsHead(ToolDefinition type, GTMaterial material) {
        if (material == null || material.has(MaterialProperty.ANTIMATTER)) return false;
        if (material.getToolTypes() < type.minToolTypes()) return false;
        // Loader_Tools excludes the generic Wood material; named woods still make soft hammers.
        if (type != ToolDefinition.BUILDER_WAND && name(material).equals("Wood")) return false;
        if (switch (type) {
            case SWORD, PICKAXE, CONSTRUCTION_PICK, SHOVEL, SPADE, HOE, AXE, DOUBLE_AXE, SENSE, PLOW,
                    FILE, CHISEL, SCREWDRIVER, SAW, HARD_HAMMER, SOFT_HAMMER -> true;
            default -> false;
        } && Set.of("WoodTreated", "WoodPolished", "GildedIron", "SteelGalvanized", "NetherizedDiamond").contains(name(material))) return false;
        // All MT rubber/plastic families carry EXTRUDER; shaped soft heads require EXTRUDER.NOT.
        if (type == ToolDefinition.SOFT_HAMMER) return material.has(MaterialProperty.WOOD);
        if (type == ToolDefinition.HARD_HAMMER) return !soft(material);
        if (type == ToolDefinition.MAGNIFYING_GLASS) return material.getToolTypes() >= 1;
        if (type == ToolDefinition.WRENCH || type == ToolDefinition.MONKEY_WRENCH) return material.getToolQuality() >= 1;
        if (type == ToolDefinition.HAND_DRILL) return !soft(material) && material.getToolQuality() >= 2;
        if (type == ToolDefinition.FILE) return material.getToolQuality() <= 2;
        if (type == ToolDefinition.BUTCHERY_KNIFE || type == ToolDefinition.WIRE_CUTTER
                || type == ToolDefinition.BRANCH_CUTTER || type == ToolDefinition.SCISSORS) return !inFamily(material, "Rubber") && !inFamily(material, "Plastic");
        return true;
    }
}
