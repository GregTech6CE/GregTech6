package gregtech.client;

import gregtech.content.block.machine.EngineBlock;
import gregtech.content.data.FuelRecipeMaps;
import gregtech.engine.energy.FaceConfig;
import gregtech.engine.fluid.FluidUnits;
import gregtech.engine.machine.DieselEngineSpec;
import gregtech.engine.machine.ElectricEngineSpec;
import gregtech.engine.machine.EngineType;
import gregtech.engine.machine.FluxEngineSpec;
import gregtech.engine.machine.RotationEngineSpec;
import gregtech.engine.machine.SteamEngineData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Engine parameters use the same labels, values and tool hints as other machine items. */
public final class EngineTooltips {
    private static final int FRONT = 1 << FaceConfig.FRONT;
    private static final int BACK = 1 << FaceConfig.BACK;
    private static final int SIDES =
            (1 << FaceConfig.LEFT) | (1 << FaceConfig.RIGHT) | (1 << FaceConfig.TOP) | (1 << FaceConfig.BOTTOM);

    private EngineTooltips() {}

    public static void appendEngine(EngineBlock block, List<Component> lines) {
        EngineType type = block.engineType();
        switch (type) {
            case ELECTRIC -> {
                ElectricEngineSpec spec = block.engineSpec(ElectricEngineSpec.class);
                appendPiston(
                        "electric", "EU", spec.inputRate(), block.machineSpec().outputRate(), 1, lines);
            }
            case FLUX -> {
                FluxEngineSpec spec = block.engineSpec(FluxEngineSpec.class);
                // GT6 EngineFlux expresses conversion efficiency in its native 8 RF energy ratio.
                appendPiston("flux", "RF", spec.inputRate(), block.machineSpec().outputRate(), 8, lines);
                lines.add(Component.translatable("gt.lang.accepts.redstoneflux.lossless")
                        .withStyle(ChatFormatting.GOLD));
            }
            case STEAM -> appendSteam(block, lines);
            case ROTATION -> appendRotation(block, lines);
            case DIESEL -> appendDiesel(block, lines);
        }
        if (type != EngineType.DIESEL) {
            lines.add(TooltipHelper.stat("tooltip.gt6ce.engine.emit_rf", ChatFormatting.GOLD, 50));
        }
        appendToolHints(type, lines);
        // CommonBlockTooltips and MaterialTooltipHandler supply harvest, resistance and composition once.
    }

    private static void appendPiston(
            String description, String unit, long input, long output, int energyRatio, List<Component> lines) {
        lines.add(EnergyText.format(Component.translatable("tooltip.gt6ce.engine.desc." + description)
                .withStyle(ChatFormatting.AQUA)));
        appendEfficiency((int) (output * energyRatio * 10000 / input), lines);
        lines.add(energyRate(false, input, 1, input * 2, unit, BACK));
        lines.add(capacity(input * 2, unit, ChatFormatting.GREEN));
        lines.add(energyRate(true, output, output, output, "KU", FRONT));
        lines.add(Component.translatable("tooltip.gt6ce.engine.piston.faces").withStyle(ChatFormatting.GOLD));
        lines.add(EnergyText.format(TooltipHelper.stat(
                "tooltip.gt6ce.engine.piston.modes",
                ChatFormatting.AQUA,
                (input + 15) / 16,
                input * 2,
                unit,
                output / 16,
                output * 2)));
    }

    private static void appendSteam(EngineBlock block, List<Component> lines) {
        SteamEngineData spec = block.engineSpec(SteamEngineData.class);
        long output = block.machineSpec().outputRate();
        int efficiency = spec.efficiency();
        lines.add(Component.translatable("gt.lang.energy.convert.from")
                .withStyle(ChatFormatting.AQUA)
                .append(" ")
                .append(white("200 " + FluidUnits.unit() + " "))
                .append(Component.translatable("fluid_type.gt6ce.steam").withStyle(EnergyText.color("SU")))
                .append(" ")
                .append(Component.translatable("gt.lang.energy.convert.to"))
                .append(" ")
                .append(white("100 "))
                .append(EnergyText.unit("KU")));
        appendEfficiency(efficiency, lines);
        lines.add(energyRate(
                false,
                output * 20000 / efficiency,
                output * 10000 / efficiency,
                output * 40000 / efficiency,
                "SU",
                BACK));
        lines.add(capacity(spec.steamCapacity(), "SU", ChatFormatting.GREEN));
        lines.add(energyRate(true, output, output / 2, output * 2, "KU", FRONT));
        lines.add(capacity(output * 2000, "KU", ChatFormatting.RED));
        lines.add(Component.translatable("tooltip.gt6ce.engine.steam.emits_water")
                .withStyle(ChatFormatting.GOLD)
                .append(white(" (" + TooltipHelper.dirMaskToString(SIDES) + ")")));
    }

    private static void appendRotation(EngineBlock block, List<Component> lines) {
        RotationEngineSpec spec = block.engineSpec(RotationEngineSpec.class);
        long input = spec.inputRate();
        long output = block.machineSpec().outputRate();
        lines.add(EnergyText.format(
                Component.translatable("tooltip.gt6ce.engine.desc.rotation").withStyle(ChatFormatting.AQUA)));
        // Both shaft ends emit; the stored energy is RU, not a second KU buffer.
        appendEfficiency((int) (output * 2 * 10000 / input), lines);
        lines.add(energyRate(false, input, input <= 16 ? 1 : input / 2, input * 2, "RU", SIDES));
        lines.add(capacity(input * 2, "RU", ChatFormatting.GREEN));
        lines.add(energyRate(true, output, Math.max(1, output / 2), output * 2, "KU", FRONT | BACK));
        lines.add(Component.translatable("gt.lang.reminder.extenders").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void appendDiesel(EngineBlock block, List<Component> lines) {
        DieselEngineSpec spec = block.engineSpec(DieselEngineSpec.class);
        lines.add(label("gt.lang.recipes", ChatFormatting.AQUA)
                .append(Component.translatable(FuelRecipeMaps.Engine.mNameInternal)
                        .withStyle(ChatFormatting.WHITE)));
        appendEfficiency(10000, lines);
        lines.add(energyRate(true, spec.outputRate(), spec.outputRate(), spec.outputRate(), "RU", FRONT));
        lines.add(TooltipHelper.ioLine("fluids_in", 63, -1, ChatFormatting.GREEN));
        lines.add(TooltipHelper.ioLine("fluids_out", 63, FaceConfig.BACK, ChatFormatting.RED));
        lines.add(Component.translatable("tooltip.gt6ce.engine.diesel.faces").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("gt.lang.nogui.funnel.tap.tank").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component energyRate(boolean output, long nominal, long min, long max, String unit, int faces) {
        var line = label(
                        output ? "gt.lang.energy.output" : "gt.lang.energy.input",
                        output ? ChatFormatting.RED : ChatFormatting.GREEN)
                .append(white(TooltipHelper.formatLong(nominal) + " "))
                .append(EnergyText.unit(unit))
                .append(white("/t ("));
        if (min != nominal || max != nominal) {
            line.append(TooltipHelper.range(min, max).withStyle(ChatFormatting.WHITE))
                    .append(white(", "));
        }
        return line.append(white(TooltipHelper.dirMaskToString(faces) + ")"));
    }

    private static Component capacity(long value, String unit, ChatFormatting color) {
        return label("gt.lang.energy.capacity", color)
                .append(white(TooltipHelper.formatLong(value) + " "))
                .append(EnergyText.unit(unit));
    }

    private static void appendEfficiency(int efficiency, List<Component> lines) {
        lines.add(label("gt.lang.efficiency", ChatFormatting.YELLOW)
                .append(white(MachineTooltips.formatEfficiencyPercent(efficiency) + "%")));
    }

    private static MutableComponent label(String key, ChatFormatting color) {
        return Component.translatable(key).append(": ").withStyle(color);
    }

    private static MutableComponent white(String text) {
        return Component.literal(text).withStyle(ChatFormatting.WHITE);
    }

    private static void appendToolHints(EngineType type, List<Component> lines) {
        if (type == EngineType.ELECTRIC || type == EngineType.FLUX) {
            lines.add(
                    Component.translatable("gt.lang.use.screwdriver.to.toggle").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type == EngineType.ELECTRIC || type == EngineType.FLUX || type == EngineType.STEAM) {
            lines.add(
                    Component.translatable("gt.lang.use.soft.hammer.to.toggle").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type != EngineType.ROTATION) {
            lines.add(Component.translatable("gt.lang.use.magnifyingglass.to.detail")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        StorageBlockTooltips.facing(lines);
    }
}
