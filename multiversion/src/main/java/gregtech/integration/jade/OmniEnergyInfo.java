package gregtech.integration.jade;

import gregtech.content.blockentity.energy.*;
import gregtech.content.blockentity.machine.*;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.OriginalThermalConverter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Energy observations use recorded transfers or explicitly labelled ratings, never simulated injection. */
final class OmniEnergyInfo {
    private OmniEnergyInfo() {}

    static void append(JadeInspection info, BlockEntity entity) {
        if (entity instanceof ElectricWireBlockEntity wire) {
            info.line("transferred", wire.lastWattage(), "EU");
            info.line("wire_rating", wire.voltage(), wire.amperage(), wire.lossPerBlock());
            if (wire.burnCounter() > 0) info.line("wire_overloads", wire.burnCounter());
        } else if (entity instanceof AxleBlockEntity axle) {
            info.line("transferred", axle.transferredLast(), "RU");
            info.line("axle_rating", axle.maxSpeed(), axle.maxPower());
        } else if (entity instanceof EnergyNodeBlockEntity node && node.spec() != null) {
            var spec = node.spec();
            var control = node.machineControl(null);
            if (!node.isSteamConverter()) {
                info.state(control.enabled(), control.running());
                info.flag("delivering", control.active());
                if (!node.isSolar()) {
                    info.line("rated_input", node.getEnergySizeInputRecommended(spec.inType(), null), spec.inType().getShortName());
                    info.line("packet_range", JadeInspection.label("input"), node.getEnergySizeInputMin(spec.inType(), null),
                            node.getEnergySizeInputMax(spec.inType(), null), spec.inType().getShortName());
                }
                info.line("rated_output", node.getEnergySizeOutputRecommended(spec.outType(), null), spec.outType().getShortName());
                info.line("packet_range", JadeInspection.label("output"), node.getEnergySizeOutputMin(spec.outType(), null),
                        node.getEnergySizeOutputMax(spec.outType(), null), spec.outType().getShortName());
                long capacity = node.isBatteryBox() ? node.batteryEnergy().bufferCapacity() : node.getEnergyCapacity(spec.inType(), null);
                if (capacity > 0) info.line("buffer", node.stored(), capacity, spec.inType().getShortName());
            }
            if (control.supportsMode()) info.line("mode", control.mode());
            if (node.isSolar()) {
                info.flag("sky", node.solarSkyVisible());
                info.line("solar_remaining", node.stored());
            }
            if (node.isElectricTransformer() || node.isRotationTransformer()) info.flag("reversed", node.transformerInverted());
            if (node.isBatteryBox()) {
                var battery = node.batteryEnergy();
                info.line("battery_total", battery.stored(), battery.capacity(), spec.outType().getShortName());
                info.line("battery_output", battery.providers(), battery.offered());
                var inventory = node.batteryInventory();
                int installed = 0;
                for (int i = 0; i < inventory.getSlots(); i++) if (!inventory.getStackInSlot(i).isEmpty()) installed++;
                info.line("battery_unavailable", Math.max(0, installed - battery.providers()));
                info.inventory("battery", inventory, 0, inventory.getSlots());
            }
            if (node.isOriginalMotor()) {
                info.line("rotation", JadeInspection.label(node.motorCounterClockwise() ? "counterclockwise" : "clockwise"));
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
        } else if (entity instanceof PumpBlockEntity pump) {
            info.line("stored", pump.storedPumpEnergy(), "RU");
            var tank = pump.tankForInspection();
            info.fluid("output", 0, tank.getFluid(), tank.getAmount(), tank.getCapacity());
            info.line("state", JadeInspection.label(!tank.isEmpty() ? "draining" : pump.storedPumpEnergy()
                    >= gregtech.content.energy.PumpWorkRules.START_ENERGY ? "searching" : "waiting"));
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
}
