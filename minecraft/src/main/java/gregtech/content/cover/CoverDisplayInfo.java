package gregtech.content.cover;

import gregtech.content.logistics.LogisticsCoverInteraction;
import gregtech.content.logistics.LogisticsCoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

/** Read-only cover observations shared by item tooltips and Jade. Values stay in the client's language. */
public final class CoverDisplayInfo {
    public record Text(String key) {}
    public record Line(String key, Object... arguments) {}
    private CoverDisplayInfo() {}

    public static List<Line> describe(ItemStack stack, Level level) {
        String behavior = CoverItems.behavior(stack);
        if (behavior == null) return List.of();
        var lines = new ArrayList<Line>();
        var kind = ComponentCoverRuntime.kind(stack);
        if (kind != null) {
            // Original CoverPump/Conveyor/RobotArm.addToolTips; derive rates from the runtime rules.
            if (kind == ComponentCoverRules.Kind.PUMP)
                add(lines, "pump_rate", CoverItems.pumpThroughput(stack));
            else add(lines, kind == ComponentCoverRules.Kind.ROBOT_ARM ? "arm_rate" : "item_rate",
                    CoverItems.itemInterval(stack));
            add(lines, "mode", text(ComponentCoverRuntime.visual(stack) == 0 ? "export" : "import"));
            if (kind == ComponentCoverRules.Kind.ROBOT_ARM) {
                int slot = ComponentCoverRuntime.slot(stack);
                add(lines, slot < 0 ? "source_slot" : "target_slot", slot < 0 ? -1 - slot : slot);
            }
        }
        var logistics = LogisticsCoverType.of(stack);
        boolean itemFilter = CoverUtilityBehaviors.FILTER_ITEM.equals(behavior)
                || CoverUtilityBehaviors.RETRIEVER_ITEM.equals(behavior);
        boolean fluidFilter = CoverUtilityBehaviors.FILTER_FLUID.equals(behavior);
        if (logistics != null) {
            add(lines, "mode", text("logistics_" + logistics.role().name().toLowerCase(java.util.Locale.ROOT)));
            itemFilter |= logistics.filtered() && logistics.channel() == LogisticsCoverType.Channel.ITEM;
            fluidFilter |= logistics.filtered() && logistics.channel() == LogisticsCoverType.Channel.FLUID;
            if (logistics.usesPriority()) add(lines, "priority", logistics.effectivePriority(LogisticsCoverInteraction.value(stack)));
            if (logistics.targetStackSize()) add(lines, "target_count", LogisticsCoverInteraction.targetStackSize(stack));
        }
        if (itemFilter || fluidFilter) {
            Object target = text("filter_empty");
            // NeoForge stack decoding needs the current registry lookup; never mutate the template.
//? if >=1.21.1 {
            /*if (level != null) {
                if (itemFilter) {
                    var item = CoverUtilityBehaviors.itemFilter(stack, level.registryAccess());
                    if (!item.isEmpty()) target = item;
                } else {
                    var fluid = CoverUtilityBehaviors.fluidFilter(stack, level.registryAccess());
                    if (!fluid.isEmpty()) target = fluid;
                }
            }
*///?} else {
            if (itemFilter) {
                var item = CoverUtilityBehaviors.itemFilter(stack);
                if (!item.isEmpty()) target = item;
            } else {
                var fluid = CoverUtilityBehaviors.fluidFilter(stack);
                if (!fluid.isEmpty()) target = fluid;
            }
//?}
            add(lines, "filter_target", target);
            if (logistics == null) {
                lines.add(new Line("tooltip.gt6ce.cover.mode", new Text(MachineCoverSpec.inverted(stack)
                        ? "gt6ce.filter.blacklist" : "gt6ce.filter.whitelist")));
                add(lines, "filter_nbt");
                if (itemFilter && CoverStackData.readOrEmpty(stack).getBoolean("gt.filter.wildcard"))
                    add(lines, "filter_wildcard");
            }
        }
        var panel = PanelCover.of(stack);
        if (panel != null) {
            if (panel.selector() || panel == PanelCover.EMITTER) add(lines, "value", PanelCoverRuntime.value(stack));
            if (panel.invertible()) add(lines, "mode", text(MachineCoverSpec.inverted(stack) ? "inverted" : "normal"));
            if (panel.strongConfig()) add(lines, "signal_type", text(MachineCoverSpec.strong(stack) ? "strong" : "weak"));
        }
        var controller = MachineCoverSpec.of(stack);
        if (controller != null) {
            if (controller.invertible()) add(lines, "mode", text(MachineCoverSpec.inverted(stack) ? "inverted" : "normal"));
            if (controller.detector()) add(lines, "signal_type", text(MachineCoverSpec.strong(stack) ? "strong" : "weak"));
            if (controller.interval > 0) add(lines, "reboot_interval", controller.interval);
        }
        int selector = CoverItems.selectorTag(stack);
        if (selector >= 0) add(lines, "value", selector);
        return List.copyOf(lines);
    }

    private static Text text(String suffix) { return new Text("tooltip.gt6ce.cover." + suffix); }
    private static void add(List<Line> lines, String suffix, Object... arguments) {
        lines.add(new Line("tooltip.gt6ce.cover." + suffix, arguments));
    }
}
