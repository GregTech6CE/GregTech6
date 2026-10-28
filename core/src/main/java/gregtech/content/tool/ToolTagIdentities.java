package gregtech.content.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GT6 registers each hand tool under several ore-dictionary tool names ({@code Loader_Tools:116-183}).
 * The port keeps one {@code forge:tools/<tool id>} tag per tool, so the additional GT6 identities are
 * listed here and emitted alongside the primary one. Spellings follow the port's snake_case tool ids.
 */
public final class ToolTagIdentities {
    private ToolTagIdentities() {}

    private static final Map<String, List<String>> ADDITIONAL = Map.ofEntries(
            Map.entry("sword", List.of("blade")),
            Map.entry("knife", List.of("blade")),
            Map.entry("butchery_knife", List.of("blade")),
            Map.entry("sense", List.of("blade")),
            Map.entry("universal_spade", List.of("blade", "shovel", "crowbar", "saw")),
            Map.entry("spade", List.of("shovel")),
            Map.entry("double_axe", List.of("axe")),
            Map.entry("monkey_wrench", List.of("wrench")),
            Map.entry("scissors", List.of("shears")),
            Map.entry("construction_pick", List.of("pickaxe")),
            Map.entry("gem_tipped_pickaxe", List.of("pickaxe")),
            Map.entry("pocket_knife", List.of("blade", "knife")),
            Map.entry("pocket_saw", List.of("saw")),
            Map.entry("pocket_file", List.of("file")),
            Map.entry("pocket_screwdriver", List.of("screwdriver")),
            Map.entry("pocket_wire_cutter", List.of("wire_cutter")),
            Map.entry("pocket_scissors", List.of("scissors", "shears")),
            Map.entry("pocket_chisel", List.of("chisel")));

    /** The primary tag identity followed by the GT6 aliases. */
    public static List<String> all(String toolId) {
        var additional = ADDITIONAL.get(toolId);
        if (additional == null) return List.of(toolId);
        var identities = new ArrayList<String>(additional.size() + 1);
        identities.add(toolId);
        identities.addAll(additional);
        return List.copyOf(identities);
    }
}
