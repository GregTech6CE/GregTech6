package gregtech.compat.jade;

import gregtech.content.blockentity.energy.*;
import gregtech.content.blockentity.machine.*;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.LaserSpec;
import gregtech.content.energy.OriginalEnergyDeviceTooltipData;
import gregtech.content.energy.OriginalThermalConverter;
import gregtech.engine.energy.EnergyNodeSpec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Energy observations use recorded transfers or explicitly labelled ratings, never simulated injection. */
final class OmniEnergyInfo {
    private OmniEnergyInfo() {}

    static void append(JadeInspection info, BlockEntity entity) {
        if (entity instanceof AxleBlockEntity axle) {
            info.line("transferred", axle.transferredLast(), "RU");
            info.line("axle_rating", axle.maxSpeed(), axle.maxPower());
        } else if (entity instanceof EnergyNodeBlockEntity node && node.spec() != null && !node.isBatteryBox()) {
            var spec = node.spec();
            var control = node.machineControl(null);
            if (!node.isSteamConverter()) {
                info.state(control.enabled(), control.running());
                if (!node.isSolar()) {
                    appendConversion(info, node, spec);
                    info.line(
                            "energy_rate",
                            JadeInspection.label("input"),
                            EnergyRateText.rate(
                                    node.getEnergySizeInputRecommended(spec.inType(), null),
                                    node.getEnergySizeInputMin(spec.inType(), null),
                                    node.getEnergySizeInputMax(spec.inType(), null),
                                    spec.inType().getShortName()));
                }
                info.line(
                        "energy_rate",
                        JadeInspection.label("output"),
                        EnergyRateText.rate(
                                node.getEnergySizeOutputRecommended(spec.outType(), null),
                                node.getEnergySizeOutputMin(spec.outType(), null),
                                node.getEnergySizeOutputMax(spec.outType(), null),
                                spec.outType().getShortName()));
                long capacity = node.getEnergyCapacity(spec.inType(), null);
                if (capacity > 0)
                    info.line("buffer", node.stored(), capacity, spec.inType().getShortName());
            }
            if (control.supportsMode()) info.line("mode", control.mode());
            if (node.isSolar()) {
                info.flag("sky", node.solarSkyVisible());
                info.line("solar_remaining", node.stored());
            }
            if (node.isElectricTransformer() || node.isRotationTransformer())
                info.flag("reversed", node.transformerInverted());
            if (node.isOriginalMotor()) {
                info.line(
                        "rotation",
                        JadeInspection.label(node.motorCounterClockwise() ? "counterclockwise" : "clockwise"));
                // GT6's entity rotation is 5/10 degrees per tick, not the shaft's energy packet size.
                var state = node.getBlockState();
                if (state.hasProperty(gregtech.content.block.energy.RotaryConverterBlock.ACTIVITY)
                        && state.getValue(gregtech.content.block.energy.RotaryConverterBlock.ACTIVITY) > 0
                        && state.getValue(gregtech.content.block.energy.EnergyNodeBlock.FACING) == Direction.UP) {
                    info.line("entity_rotation", node.converterFast() ? 10 : 5);
                    info.line("rotation_warning");
                }
            }
            if (node.isOriginalThermalConverter()) {
                if (OriginalThermalConverter.cooler(spec)) info.line("rated_output", spec.outputRate(), "HU");
                else if (node.getBlockState().getValue(gregtech.content.block.energy.RotaryConverterBlock.ACTIVITY) > 0)
                    info.line("contact_damage", OriginalThermalConverter.contactDamage(spec));
            }
        } else if (entity instanceof LaserConverterBlockEntity converter) {
            var spec = converter.spec();
            var control = converter.machineControl(null);
            info.state(control.enabled(), control.running());
            info.line("converter_input", converter.lastInput(), spec.inputType().getShortName());
            info.line(
                    "converter_output",
                    converter.lastOutput(),
                    spec.outputType().getShortName());
            long normalized = spec.kind() == LaserSpec.Kind.FLUX ? spec.input() / 4 : spec.input();
            if (normalized > 0)
                info.line("efficiency", EnergyRateText.percent((int)
                        Math.min(10000, 10000L * spec.output() / normalized)));
            info.line(
                    "energy_rate",
                    JadeInspection.label("input"),
                    EnergyRateText.rate(
                            spec.input(),
                            spec.input() <= 16 ? 1 : spec.input() / 2,
                            spec.input() * 2,
                            spec.inputType().getShortName()));
            info.line(
                    "energy_rate",
                    JadeInspection.label("output"),
                    EnergyRateText.rate(
                            spec.output(),
                            spec.output() / 2,
                            spec.output() * 2,
                            spec.outputType().getShortName()));
        } else if (entity instanceof PumpBlockEntity pump) {
            info.line("stored", pump.storedPumpEnergy(), "RU");
            var tank = pump.tankForInspection();
            info.fluid("output", 0, tank.getFluid(), tank.getAmount(), tank.capacity());
            info.line(
                    "state",
                    JadeInspection.label(
                            !tank.isEmpty()
                                    ? "draining"
                                    : pump.storedPumpEnergy() >= gregtech.engine.energy.PumpWorkRules.START_ENERGY
                                            ? "searching"
                                            : "waiting"));
        } else if (entity instanceof AxialGeneratorBlockEntity axial) {
            var control = axial.machineControl(null);
            info.state(control.enabled(), control.running());
            info.flag("delivering", control.active());
        } else if (entity instanceof LargeGasTurbineControllerBlockEntity gas) {
            var control = gas.machineControl(null);
            info.state(control.enabled(), control.running());
            info.flag("delivering", control.active());
            info.line("rated_output", gas.getEnergySizeOutputRecommended(GregTechTags.Energy.RU, null), "RU");
        } else if (entity instanceof FuelGeneratorBlockEntity fuel) {
            var control = fuel.machineControl(null);
            info.state(control.enabled(), control.running());
            info.tanks("contents", fuel);
        } else if (entity instanceof PistonEngineBlockEntity piston) {
            var control = piston.machineControl(null);
            info.state(control.enabled(), control.running());
            info.flag("delivering", control.active());
        }
    }

    /** Shared last-tick power and rated efficiency for every pure energy converter. */
    private static void appendConversion(JadeInspection info, EnergyNodeBlockEntity node, EnergyNodeSpec spec) {
        if (!OriginalEnergyDeviceTooltipData.handles(spec)) return;
        boolean cooler = node.isOriginalThermalConverter() && OriginalThermalConverter.cooler(spec);
        info.line("converter_input", node.lastMeasuredInput(), spec.inType().getShortName());
        info.line("converter_output", node.lastMeasuredOutput(), spec.outType().getShortName());
        if (cooler) info.line("converter_output", node.lastMeasuredAuxOutput(), "HU");
        int efficiency = OriginalEnergyDeviceTooltipData.profile(spec, node.transformerInverted())
                .efficiency();
        if (efficiency >= 0) info.line("efficiency", EnergyRateText.percent(efficiency));
    }
}
