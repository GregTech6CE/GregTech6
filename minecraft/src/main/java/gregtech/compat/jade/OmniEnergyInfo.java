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
            boolean cooler = node.isOriginalThermalConverter() && OriginalThermalConverter.cooler(spec);
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
                        node.conversionMeter().averageInput(tick),
                        node.conversionMeter().averageOutput(tick));
                if (cooler)
                    info.energyRate(
                            "average_output",
                            node.conversionMeter().averageAuxiliaryOutput(tick),
                            spec.outputRate() / 2,
                            spec.outputRate() * 2,
                            "HU");
                EnergyRateText.efficiency(info, efficiency);
                long capacity = node.getEnergyCapacity(spec.inType(), null);
                if (capacity > 0 && !node.isSteamConverter())
                    info.detail("buffer", node.stored(), capacity, spec.inType().getShortName());
            } else {
                info.state(control.enabled(), control.running());
                info.energyRate(
                        "output",
                        node.getEnergySizeOutputRecommended(spec.outType(), null),
                        node.getEnergySizeOutputMin(spec.outType(), null),
                        node.getEnergySizeOutputMax(spec.outType(), null),
                        spec.outType().getShortName());
            }
            if (control.supportsMode()) {
                if (cooler) info.detail("mode", control.mode());
                else info.line("mode", control.mode());
            }
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
                if (!cooler
                        && node.getBlockState().getValue(gregtech.content.block.energy.RotaryConverterBlock.ACTIVITY)
                                > 0) info.line("contact_damage", OriginalThermalConverter.contactDamage(spec));
            }
        } else if (entity instanceof LaserConverterBlockEntity converter) {
            var spec = converter.spec();
            var control = converter.machineControl(null);
            info.state(control.enabled(), control.running());
            long tick = entity.getLevel().getGameTime();
            EnergyRateText.conversion(
                    info,
                    converter,
                    spec.inputType(),
                    spec.outputType(),
                    converter.conversionMeter().averageInput(tick),
                    converter.conversionMeter().averageOutput(tick));
            EnergyRateText.efficiency(
                    info, EnergyRateText.efficiency(spec.input(), spec.output(), spec.inputType(), spec.outputType()));
        } else if (entity instanceof PumpBlockEntity pump) {
            info.leftLine("stored", pump.storedPumpEnergy(), "RU");
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
            info.energyRate(
                    "average_input",
                    axial.conversionMeter().averageInput(tick),
                    grade.input() / 2,
                    grade.inputMaximum(),
                    grade.steam() ? "SU" : "RU");
            info.energyRate(
                    "average_output",
                    axial.conversionMeter().averageOutput(tick),
                    grade.output() / 2,
                    grade.outputMaximum(),
                    grade.steam() ? "RU" : "EU");
            EnergyRateText.efficiency(info, (int) (10000L * grade.output() * (grade.steam() ? 2 : 1) / grade.input()));
        } else if (entity instanceof LargeGasTurbineControllerBlockEntity gas) {
            var control = gas.machineControl(null);
            info.state(control.enabled(), control.running());
            var grade = gas.grade();
            info.energyRate(
                    "average_output",
                    gas.conversionMeter().averageOutput(entity.getLevel().getGameTime()),
                    grade.output() / 2,
                    grade.outputMaximum(),
                    "RU");
            EnergyRateText.efficiency(info, (int) (10000L * grade.output() / grade.input()));
        } else if (entity instanceof FuelGeneratorBlockEntity fuel) {
            var control = fuel.machineControl(null);
            info.state(control.enabled(), control.running());
            var declaration = fuel.declaration();
            long rated = declaration.output().perTick();
            info.energyRate(
                    "average_output",
                    fuel.conversionMeter().averageOutput(entity.getLevel().getGameTime()),
                    rated,
                    rated,
                    declaration.output().type().name());
            EnergyRateText.efficiency(info, declaration.efficiency());
            info.tanks("contents", fuel);
        }
    }
}
