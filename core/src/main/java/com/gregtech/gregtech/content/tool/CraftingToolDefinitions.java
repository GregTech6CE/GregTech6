package com.gregtech.gregtech.content.tool;

import java.util.List;

/** GT6 MultiItemRandomTools:470-512. Tips are infinite only in the GT crafting table/autocrafter. */
public final class CraftingToolDefinitions {
    private CraftingToolDefinitions() {}
    public record Tool(String tip, String token, List<String> kinds, int count, String prefix, String material) {}
    public static final List<Tool> ALL = List.of(
        tool("wrench", "wrench", 2, "plateTiny", "Steel", "wrench"),
        tool("screwdriver", "screwdriver", 32, "plateTiny", "Steel", "screwdriver"),
        tool("saw", "saw", 32, "plateTiny", "Steel", "saw"),
        tool("hammer", "hammer", 2, "plateTiny", "Steel", "hammer"),
        tool("cutter", "cutter", 4, "plateTiny", "Steel", "wire_cutter", "scissors"),
        tool("chisel", "chisel", 9, "plateTiny", "Steel", "chisel"),
        tool("rubber_hammer", "rubber_hammer", 4, "nugget", "Rubber", "soft_hammer"),
        tool("blade", "blade", 32, "plateTiny", "Steel", "sword", "blade"),
        tool("drill", "drill", 32, "plateTiny", "Steel", "hand_drill", "drill"),
        tool("file", "file", 9, "plateTiny", "Steel", "file")
    );
    private static Tool tool(String tip, String token, int count, String prefix, String material, String... kinds) {
        return new Tool("robot_arm_" + tip + "_tip", "single_use_" + token, List.of(kinds), count, prefix, material);
    }
    public static boolean matches(String id, String kind) {
        return ALL.stream().anyMatch(t -> (t.tip().equals(id) || t.token().equals(id)) && t.kinds().contains(kind));
    }
    public static boolean infinite(String id) { return ALL.stream().anyMatch(t -> t.tip().equals(id)); }
}
