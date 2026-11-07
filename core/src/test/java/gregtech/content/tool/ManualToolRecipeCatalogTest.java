package gregtech.content.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.engine.tool.ToolDefinition;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Pins the original head + handle assembly table to GT6 {@code Loader_Tools}. */
final class ManualToolRecipeCatalogTest {

    /**
     * Every {@code AdvancedCraftingTool} registration of {@code Loader_Tools.java:332-350}. The
     * universal spade is absent: its row is the shaped {@code "AT", "Sd"} tool recipe at
     * {@code Loader_Tools.java:298}.
     */
    private static final Set<ToolDefinition> ORIGINAL_ASSEMBLIES = EnumSet.of(
            ToolDefinition.MAGNIFYING_GLASS,
            ToolDefinition.HARD_HAMMER,
            ToolDefinition.SOFT_HAMMER,
            ToolDefinition.SWORD,
            ToolDefinition.BUILDER_WAND,
            ToolDefinition.CONSTRUCTION_PICK,
            ToolDefinition.GEM_PICK,
            ToolDefinition.PICKAXE,
            ToolDefinition.SHOVEL,
            ToolDefinition.SPADE,
            ToolDefinition.AXE,
            ToolDefinition.DOUBLE_AXE,
            ToolDefinition.HOE,
            ToolDefinition.SENSE,
            ToolDefinition.PLOW,
            ToolDefinition.FILE,
            ToolDefinition.CHISEL,
            ToolDefinition.SCREWDRIVER,
            ToolDefinition.SAW);

    @Test
    void assemblyEligibilityMatchesTheOriginalAdvancedCraftingToolRows() {
        var actual = new TreeSet<String>();
        for (ToolDefinition type : ToolDefinition.values()) {
            if (ManualToolRecipeCatalog.isHeadAssembly(type)) actual.add(type.name());
        }
        var expected = new TreeSet<String>();
        ORIGINAL_ASSEMBLIES.forEach(type -> expected.add(type.name()));
        assertEquals(expected, actual);
    }

    @Test
    void universalSpadeKeepsOnlyItsShapedToolRow() {
        assertFalse(ManualToolRecipeCatalog.isHeadAssembly(ToolDefinition.UNIVERSAL_SPADE));
        assertTrue(
                ManualToolRecipeCatalog.shaped(ToolDefinition.UNIVERSAL_SPADE).stream()
                        .anyMatch(pattern -> String.join("/", pattern.rows()).equals("AT/Sd")),
                "the universal spade is built from its head, screw, stick and a screwdriver");
    }

    @Test
    void headAndHandleToolsKeepTheirShapedHeads() {
        for (ToolDefinition type : ORIGINAL_ASSEMBLIES) {
            if (type == ToolDefinition.MAGNIFYING_GLASS) continue;
            assertTrue(type.requiresHeadAssembly(), type + " keeps its head prefix");
            assertTrue(ManualToolRecipeCatalog.headPrefix(type) != null, type + " resolves a head prefix");
        }
    }
}
