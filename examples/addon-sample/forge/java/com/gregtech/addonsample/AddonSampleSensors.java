package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.platform.AddonSensor;
import com.gregtech.gregtech.api.addon.platform.AddonSensorContributor;
import com.gregtech.gregtech.api.addon.platform.SensorContext;
import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.gregtech.gregtech.api.signal.GtSignalSource;

import java.util.List;

/** Reads the coarse signal strength of the block in front of the sensor. */
public final class AddonSampleSensors implements AddonSensorContributor {
    private static final AddonSensor SIGNAL = new AddonSensor() {
        @Override public String id() { return "signal"; }
        @Override public String titleKey() { return "block.addon_sample.sensor_signal"; }
        @Override public String unit() { return "scale"; }
        @Override public int color() { return 0xFF8000; }
        @Override public long maximum() { return 15; }

        @Override public long read(SensorContext context) {
            return context.target() instanceof GtSignalSource source
                    ? source.gtSignal(context.side().ordinal()) / GtSignalCarrier.UNIT : 0;
        }
    };

    @Override public String addonId() { return "addon_sample"; }

    @Override public List<AddonSensor> sensorKinds() { return List.of(SIGNAL); }
}
