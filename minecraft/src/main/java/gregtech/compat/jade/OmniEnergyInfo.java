package gregtech.compat.jade;

import gregtech.content.blockentity.energy.*;
import gregtech.content.blockentity.machine.*;
import gregtech.content.energy.OriginalEnergyDeviceTooltipData;
import gregtech.content.energy.OriginalThermalConverter;
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
            if (!node.isSolar()) {
                info.state(control.enabled(), control.running());
                long tick = entity.getLevel().getGameTime();
                int efficiency = OriginalEnergyDeviceTooltipData.handles(spec)
                        ? OriginalEnergyDeviceTooltipData.profile(spec, node.transformerInverted())
                                .efficiency()
                        : EnergyRateText.efficiency(spec.inputRate(), spec.outputRate(), spec.inType(), spec.outType());
                EnergyRateText.conversion(
                        info,
                        node,
                        spec.inType(),
                        spec.outType(),
                        node.conversionMeter().input(tick),
                        node.conversionMeter().output(tick),
                        efficiency);
                long capacity = node.getEnergyCapacity(spec.inType(), null);
                if (capacity > 0 && !node.isSteamConverter())
                    info.detail("buffer", node.stored(), capacity, spec.inType().getShortName());
            } else {
                info.state(control.enabled(), control.running());
                info.line(
                        "energy_rate",
                        JadeInspection.label("output"),
                        EnergyRateText.rate(
                                node.getEnergySizeOutputRecommended(spec.outType(), null),
                                node.getEnergySizeOutputMin(spec.outType(), null),
                                node.getEnergySizeOutputMax(spec.outType(), null),
                                spec.outType().getShortName()));
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
                if (OriginalThermalConverter.cooler(spec))
                    info.line(
                            "energy_rate",
                            JadeInspection.label("output"),
                            EnergyRateText.rate(
                                    node.lastMeasuredAuxOutput(), spec.outputRate() / 2, spec.outputRate() * 2, "HU"));
                else if (node.getBlockState().getValue(gregtech.content.block.energy.RotaryConverterBlock.ACTIVITY) > 0)
                    info.line("contact_damage", OriginalThermalConverter.contactDamage(spec));
            }
        } else if (entity instanceof LaserConverterBlockEntity converter) {
            var spec = converter.spec();
            var control = converter.machineControl(null);
            info.state(control.enabled(), control.running());
            EnergyRateText.conversion(
                    info,
                    converter,
                    spec.inputType(),
                    spec.outputType(),
                    converter.lastInput(),
                    converter.lastOutput(),
                    EnergyRateText.efficiency(spec.input(), spec.output(), spec.inputType(), spec.outputType()));
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
            var grade = axial.grade();
            long tick = entity.getLevel().getGameTime();
            info.line(
                    "energy_rate",
                    JadeInspection.label("input"),
                    EnergyRateText.rate(
                            axial.conversionMeter().input(tick),
                            grade.input() / 2,
                            grade.inputMaximum(),
                            grade.steam() ? "SU" : "RU"));
            info.line(
                    "energy_rate",
                    JadeInspection.label("output"),
                    EnergyRateText.rate(
                            axial.conversionMeter().output(tick),
                            grade.output() / 2,
                            grade.outputMaximum(),
                            grade.steam() ? "RU" : "EU"));
            EnergyRateText.efficiency(info, (int) (10000L * grade.output() * (grade.steam() ? 2 : 1) / grade.input()));
        } else if (entity instanceof LargeGasTurbineControllerBlockEntity gas) {
            var control = gas.machineControl(null);
            info.state(control.enabled(), control.running());
            var grade = gas.grade();
            info.line(
                    "energy_rate",
                    JadeInspection.label("output"),
                    EnergyRateText.rate(
                            gas.conversionMeter().output(entity.getLevel().getGameTime()),
                            grade.output() / 2,
                            grade.outputMaximum(),
                            "RU"));
            EnergyRateText.efficiency(info, (int) (10000L * grade.output() / grade.input()));
        } else if (entity instanceof FuelGeneratorBlockEntity fuel) {
            var control = fuel.machineControl(null);
            info.state(control.enabled(), control.running());
            var declaration = fuel.declaration();
            long rated = declaration.output().perTick();
            info.line(
                    "energy_rate",
                    JadeInspection.label("output"),
                    EnergyRateText.rate(
                            fuel.conversionMeter().output(entity.getLevel().getGameTime()),
                            rated,
                            rated,
                            declaration.output().type().name()));
            EnergyRateText.efficiency(info, declaration.efficiency());
            info.tanks("contents", fuel);
        }
    }
}
