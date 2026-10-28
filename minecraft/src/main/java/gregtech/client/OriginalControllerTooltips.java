/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from original specialized controllers and MultiTileEntityBasicMachine.addToolTips. */
package gregtech.client;

import gregtech.content.multiblock.OriginalControllerTooltipData;
import gregtech.content.multiblock.OriginalControllerTooltipData.Family;
import gregtech.engine.machine.BasicMachineSpec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class OriginalControllerTooltips {
    private OriginalControllerTooltips() {}

    public static void structure(Family family, List<Component> lines) {
        add(lines, "gt.lang.structure", ChatFormatting.AQUA).append(":");
        var keys = OriginalControllerTooltipData.structureKeys(family);
        for (int i = 0; i < keys.size(); i++)
            add(
                    lines,
                    keys.get(i),
                    family == Family.LOGISTICS_CORE && i >= 4 ? ChatFormatting.YELLOW : ChatFormatting.WHITE);
    }

    /** These source classes inherit basic-machine text through their registered item. */
    public static void basic(BasicMachineSpec spec, List<Component> lines) {
        boolean coke = OriginalControllerTooltipData.basicFamily(spec.machineName()) == Family.COKE_OVEN;
        boolean cryo = OriginalControllerTooltipData.basicFamily(spec.machineName()) == Family.CRYO_DISTILLATION_TOWER;
        add(lines, "gt.lang.recipes", ChatFormatting.AQUA)
                .append(": ")
                .append(Component.translatable(
                                coke
                                        ? "gt.recipe.cokeoven"
                                        : cryo ? "gt.recipe.cryodistillationtower" : "gt.recipe.distillationtower")
                        .withStyle(ChatFormatting.WHITE))
                .append(TooltipHelper.parallel(spec.parallelLimit()).withStyle(ChatFormatting.WHITE));
        if (!coke) add(lines, "gt.lang.cheap.overclocking", ChatFormatting.YELLOW);
        if (coke) {
            io(lines, "gt.lang.item.input", List.of("gt.lang.face.top"), -1, ChatFormatting.GREEN);
            io(lines, "gt.lang.item.output", List.of("gt.lang.face.bottom"), -1, ChatFormatting.RED);
            io(
                    lines,
                    "gt.lang.fluid.output",
                    List.of(
                            "gt.lang.face.bottom",
                            "gt.lang.face.left",
                            "gt.lang.face.front",
                            "gt.lang.face.right",
                            "gt.lang.face.back"),
                    0,
                    ChatFormatting.RED);
            add(lines, "gt.lang.requirement.ignite.fire", ChatFormatting.GOLD);
        } else {
            var line = add(lines, "gt.lang.energy.input", ChatFormatting.GREEN)
                    .append(": ")
                    .append(Component.literal(spec.energyIn() + " ").withStyle(ChatFormatting.WHITE))
                    .append(Component.translatable(cryo ? "gt.td.short.energy.cryo" : "gt.td.short.energy.heat")
                            .withStyle(cryo ? ChatFormatting.AQUA : ChatFormatting.RED))
                    .append(Component.literal("/t (")
                            .append(TooltipHelper.range(0, spec.energyInMax()))
                            .append(", ")
                            .withStyle(ChatFormatting.WHITE));
            int index = 0;
            for (var key : List.of(
                    "gt.lang.face.bottom",
                    "gt.lang.face.top",
                    "gt.lang.face.left",
                    "gt.lang.face.front",
                    "gt.lang.face.right",
                    "gt.lang.face.back")) {
                if (index++ > 0) line.append(", ");
                line.append(Component.translatable(key).withStyle(ChatFormatting.WHITE));
            }
            line.append(")");
            // The tower overrides addToolTipsSided: its six structure rows describe all IO.
        }
        add(lines, "gt.lang.use.screwdriver.to.toggle", ChatFormatting.DARK_GRAY);
        var faces = spec.faceConfig();
        if (faces.fluidAutoInput() >= 0 || faces.itemAutoInput() >= 0)
            add(lines, "gt.lang.use.monkey.wrench.to.toggle.auto.inputs", ChatFormatting.DARK_GRAY);
        if (faces.fluidAutoOutput() >= 0 || faces.itemAutoOutput() >= 0)
            add(lines, "gt.lang.use.monkey.wrench.to.toggle.auto.outputs", ChatFormatting.DARK_GRAY);
        add(lines, "gt.lang.use.soft.hammer.to.reset", ChatFormatting.DARK_GRAY);
        inherited(lines);
    }

    public static void standalone(Family family, List<Component> lines) {
        if (family != Family.LOGISTICS_CORE && family != Family.BEDROCK_DRILL)
            throw new IllegalArgumentException("Standalone source controller: " + family);
        structure(family, lines);
        boolean drill = family == Family.BEDROCK_DRILL;
        var energy = gregtech.content.multiblock.OriginalControllerTooltipData.standaloneEnergy(family);
        add(lines, "gt.lang.energy.input", ChatFormatting.GREEN)
                .append(": ")
                .append(TooltipHelper.range(energy.minimum(), energy.maximum())
                        .append(" ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.translatable(energy.unitKey())
                        .withStyle(drill ? ChatFormatting.GREEN : ChatFormatting.BLUE))
                .append((drill
                                ? Component.literal("/t (")
                                        .append(TooltipHelper.range(0, energy.totalPerTick()))
                                        .append(" ")
                                : Component.literal("/t"))
                        .withStyle(ChatFormatting.WHITE));
        if (drill) {
            var line = (MutableComponent) lines.get(lines.size() - 1);
            line.append(Component.translatable("gt.td.short.energy.kinetic_rotation")
                            .withStyle(ChatFormatting.GREEN))
                    .append(Component.translatable("tooltip.gt6ce.ui.total_per_tick")
                            .withStyle(ChatFormatting.WHITE));
        }
        inherited(lines);
    }

    private static void inherited(List<Component> lines) {
        add(lines, "gt.lang.use.builder.wand.to.ease.building", ChatFormatting.DARK_GRAY);
        add(lines, "gt.lang.use.magnifyingglass.to.detail", ChatFormatting.DARK_GRAY);
        StorageBlockTooltips.facing(lines);
    }

    private static void io(List<Component> lines, String key, List<String> faces, int auto, ChatFormatting color) {
        var line = add(lines, key, color).append(": ");
        for (int i = 0; i < faces.size(); i++) {
            if (i > 0) line.append(Component.literal(", ").withStyle(ChatFormatting.WHITE));
            line.append(Component.translatable(faces.get(i)).withStyle(ChatFormatting.WHITE));
            if (i == auto)
                line.append(Component.translatable("tooltip.gt6ce.ui.auto").withStyle(ChatFormatting.WHITE));
        }
    }

    private static MutableComponent add(List<Component> lines, String key, ChatFormatting color) {
        var line = Component.translatable(key).withStyle(color);
        lines.add(line);
        return line;
    }
}
