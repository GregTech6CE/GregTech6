package gregtech.compat.jade;

import gregtech.content.blockentity.sensor.SensorBlockEntity;
import gregtech.engine.sensor.SensorControl;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.Locale;

final class OmniSensorInfo {
    private OmniSensorInfo() {}

    static void append(JadeInspection info, BlockEntity entity) {
        if (!(entity instanceof SensorBlockEntity sensor)) return;
        var sample = sensor.inspection();
        info.line("sensor_mode", JadeInspection.label("sensor_mode_" + sample.mode().name().toLowerCase(Locale.ROOT)));
        Object unit = sample.addonUnit() != null ? sample.addonUnit()
                : JadeInspection.label("sensor_unit_" + sensor.kind().name().toLowerCase(Locale.ROOT));
        if (sample.measured()) {
            // TPS samples use hundredths of a tick/second; other meters already sample their display unit.
            String value = sensor.kind() == SensorBlockEntity.Kind.TPS
                    ? String.format(Locale.ROOT, "%.2f", sample.value() / 100.0) : Long.toString(sample.value());
            info.line("sensor_reading", value, unit);
            if (sample.maximum() > 0) info.line("sensor_maximum",
                    sensor.kind() == SensorBlockEntity.Kind.TPS ? String.format(Locale.ROOT, "%.2f", sample.maximum() / 100.0)
                            : Long.toString(sample.maximum()), unit);
        } else info.line("sensor_no_sample");
        info.line("sensor_display", sensor.displayText());
        if (SensorControl.hasThreshold(sample.mode())) info.line("sensor_threshold", sample.threshold());
        info.line("sensor_average", sample.samples());
        info.line("sensor_number_format", JadeInspection.label(sample.hexadecimal() ? "hexadecimal" : "decimal"));
        info.line("sensor_input", OmniTransportInfo.side(sensor.inputSide()));
        info.line("redstone_signal", sensor.signal());
    }
}
