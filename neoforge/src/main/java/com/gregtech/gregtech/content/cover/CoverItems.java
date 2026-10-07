package com.gregtech.gregtech.content.cover;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Which items act as covers, and how a cover's tier scales its work.
 *
 * <p>GT6 does not register a separate item for most of its covers — the cover <em>is</em> the
 * machine part, or even a vanilla block:</p>
 *
 * <ul>
 *   <li>{@code MultiItemTechnological:50-53} gives the compact electric pump, conveyor and robot arm
 *       a cover instance ({@code new CoverPump(250 << 2i)}, {@code new CoverConveyor(512 >> i)},
 *       {@code new CoverRobotArm(512 >> i)}), so those three items are simultaneously machine
 *       components and covers.</li>
 *   <li>{@code GT_API:799-802} registers the vanilla redstone torch (both lit and unlit states) as
 *       {@code CoverRedstoneTorch} and the vanilla repeater as {@code CoverRedstoneRepeater}.</li>
 *   <li>{@code ItemIntegratedCircuit:87} registers the programmed circuit (meta 0…15) as
 *       {@code CoverSelectorTag(i)}, i.e. the circuit's variant <em>is</em> the selected tag.</li>
 * </ul>
 *
 * <p>The port keeps GT6's model: {@link #behavior(ItemStack)} maps a held stack onto the cover
 * behaviour it stands for, so no duplicate "cover" item exists for these families. Covers the port
 * registers on their own (blank cover, filters, detectors, …) keep their registry id as their
 * behaviour id.</p>
 */
public final class CoverItems {

    /** Cover behaviours whose item is shared with another role (GT6's registration sites above). */
    public static final String PUMP = "cover_pump";
    public static final String CONVEYOR = "cover_conveyor";
    public static final String ROBOT_ARM = "cover_robot_arm";
    public static final String REDSTONE_TORCH = "cover_redstone_torch";
    public static final String REDSTONE_REPEATER = "cover_redstone_repeater";
    public static final String TAG_SELECTOR = "cover_tag_selector";

    /** GT6 {@code CS.VN} voltage order; {@code IL.*} arrays are indexed by these. */
    private static final String[] TIERS = {
            "ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "puv1", "xv"};

    private CoverItems() {}

    /**
     * The cover behaviour a stack provides, or {@code null} when the stack is not a cover.
     *
     * <p>Ids are the port's registry paths; the shared-role families are translated to the behaviour
     * constants above, everything else must be one of the port's own cover items.</p>
     */
    public static String behavior(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) return null;
        String id = key.getPath();
        if (stack.getItem() instanceof com.gregtech.gregtech.item.PanelItemView panel)
            return panel.panelSpec().asphalt() ? CoverUtilityBehaviors.ASPHALT_PANEL : "decorative_panel";
        if (stack.getItem() instanceof com.gregtech.gregtech.item.CanvasItem) return "canvas_cover";
        if(stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem material&&!MaterialCoverRules.textures(material.getPrefix().getName()).isEmpty())return "material_plate_cover";
        if (key.getNamespace().equals("minecraft")) {
            // GT_API:799-802 - the vanilla redstone torch (either state) and repeater are covers
            return switch (id) {
                case "redstone_torch", "redstone_wall_torch" -> REDSTONE_TORCH;
                case "repeater" -> REDSTONE_REPEATER;
                default -> null;
            };
        }
        if (!key.getNamespace().equals("gregtech")) return null;
        if (id.startsWith("compact_electric_pump_")) return PUMP;               // IL.PUMPS[i]
        if (id.startsWith("compact_electric_conveyor_")) return CONVEYOR;       // IL.CONVEYERS[i]
        if (id.startsWith("compact_robot_arm_")) return ROBOT_ARM;              // IL.ROBOT_ARMS[i]
        if (id.startsWith("integrated_circuit_")) return TAG_SELECTOR;          // ItemIntegratedCircuit:87
        if (id.equals(TAG_SELECTOR)) return TAG_SELECTOR;                       // pre-§17 saves
        return portCoverId(id) ? id : null;
    }

    /** The port's own cover items: the ones GT6 registers with a dedicated cover instance. */
    private static boolean portCoverId(String id) {
        if (id.startsWith("logistics_display_cpu_") || id.startsWith("filtered_logistics_")
                || id.startsWith("generic_logistics_") || id.equals("logistics_dump_bus_item")) return true;
        return id.endsWith("_cover") || id.equals("drain") || id.equals("air_vent")
                || id.equals("item_filter") || id.equals("fluid_filter")
                // §108: GT6 registers these two on items whose names do not end in "_cover" - the
                // pressure valve is a fluid-pipe cover (CoverPressureValve:44) and the asphalt cover
                // is bound to the Asphalt Panel block (Loader_MultiTileEntities:2054). Both could be
                // attached but never dispatched before, because this list did not name them.
                || id.equals("pressure_value") || id.equals("panel_asphalt") || id.startsWith("panel_concrete") || id.startsWith("panel_wood")
                || id.contains("machine_switch") || id.contains("selector") && id.startsWith("redstone")
                || id.startsWith("activity_detector") || id.equals("energy_sensor")
                || id.equals("progress_sensor") || id.equals("redstone_emitter")
                || id.startsWith("auto_reboot_switch") || id.equals("cover_controller");
    }

    /** True when a machine face accepts this stack as a cover. */
    public static boolean isCover(ItemStack stack) {
        return behavior(stack) != null;
    }

    /**
     * GT6 tier index of a compact component: 0 = ULV … 9 = PUV1, matching the {@code IL.*} arrays.
     * Returns -1 for covers that are not tiered components.
     */
    public static int tierIndex(ItemStack stack) {
        String id = path(stack);
        if (id == null) return -1;
        if (!id.startsWith("compact_electric_pump_") && !id.startsWith("compact_electric_conveyor_")
                && !id.startsWith("compact_robot_arm_")) {
            return -1;
        }
        String suffix = id.substring(id.lastIndexOf('_') + 1);
        for (int i = 0; i < TIERS.length; i++) if (TIERS[i].equals(suffix)) return i;
        return -1;
    }

    /**
     * GT6 {@code CoverPump(250 << (2 * i))}: millibuckets moved per 20-tick operation, i.e. the
     * original's "Transfers N L/sec" tooltip.
     */
    public static int pumpThroughput(ItemStack stack) {
        int tier = tierIndex(stack);
        return ComponentCoverRules.pumpThroughput(tier);
    }

    /**
     * GT6 {@code CoverConveyor(512 >> i)} / {@code CoverRobotArm(512 >> i)}: ticks between two item
     * moves.
     */
    public static int itemInterval(ItemStack stack) {
        int tier = tierIndex(stack);
        return ComponentCoverRules.itemInterval(tier);
    }

    /**
     * The recipe tag an {@code integrated_circuit_N} cover selects (GT6 {@code ST.tag(N)}), or -1
     * when the stack is not a selector circuit.
     */
    public static int selectorTag(ItemStack stack) {
        String id = path(stack);
        if (id == null || !id.startsWith("integrated_circuit_")) return -1;
        try {
            return Integer.parseInt(id.substring("integrated_circuit_".length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String path(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && key.getNamespace().equals("gregtech") ? key.getPath() : null;
    }
}
