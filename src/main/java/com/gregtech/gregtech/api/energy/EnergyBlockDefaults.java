package com.gregtech.gregtech.api.energy;

import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.util.GTEnergySides;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/** Default {@link IEnergyBlock} logic ported from GT6 {@code TileEntityBase01Root}. */
public final class EnergyBlockDefaults {
    private EnergyBlockDefaults() {}

    public static boolean isEnergyEmittingTo(IEnergyBlock self, GregTechTags.Tag energyType,
                                             @Nullable Direction side, boolean theoretical) {
        return self.isEnergyType(energyType, side, true) && self.hasEnergySurface(side);
    }

    public static boolean isEnergyAcceptingFrom(IEnergyBlock self, GregTechTags.Tag energyType,
                                                @Nullable Direction side, boolean theoretical) {
        return self.isEnergyType(energyType, side, false) && self.hasEnergySurface(side);
    }

    public static long doEnergyInjection(IEnergyBlock self, GregTechTags.Tag energyType, @Nullable Direction side,
                                         long size, long amount, boolean doInject) {
        if (size == 0 || size == Long.MIN_VALUE || amount <= 0 || !self.isEnergyAcceptingFrom(energyType, side, false)) {
            return 0;
        }
        return EnergyGate.gateInjection(energyType, true, size, self.getEnergySizeInputMin(energyType, side), amount,
                () -> self.doInject(energyType, side, size, amount, doInject));
    }

    public static long doEnergyExtraction(IEnergyBlock self, GregTechTags.Tag energyType, @Nullable Direction side,
                                          long size, long amount, boolean doExtract) {
        if (size == 0 || size == Long.MIN_VALUE || amount <= 0 || !self.isEnergyEmittingTo(energyType, side, false)) {
            return 0;
        }
        return EnergyGate.gateExtraction(energyType, true, size, self.getEnergySizeOutputMin(energyType, side), amount,
                () -> self.doExtract(energyType, side, size, amount, doExtract));
    }

    public static long getEnergySizeOutputMin(IEnergyBlock self, GregTechTags.Tag energyType, @Nullable Direction side) {
        long rec = self.getEnergySizeOutputRecommended(energyType, side);
        return rec <= 0 ? 0 : Math.max(1, rec / 2);
    }

    public static long getEnergySizeOutputMax(IEnergyBlock self, GregTechTags.Tag energyType, @Nullable Direction side) {
        long rec = self.getEnergySizeOutputRecommended(energyType, side);
        return rec <= 0 ? 0 : rec * 2;
    }

    public static long getEnergySizeInputMin(IEnergyBlock self, GregTechTags.Tag energyType, @Nullable Direction side) {
        long rec = self.getEnergySizeInputRecommended(energyType, side);
        return rec <= 0 ? 0 : Math.max(1, rec / 2);
    }

    public static long getEnergySizeInputMax(IEnergyBlock self, GregTechTags.Tag energyType, @Nullable Direction side) {
        long rec = self.getEnergySizeInputRecommended(energyType, side);
        return rec <= 0 ? 0 : rec * 2;
    }

    public static Collection<GregTechTags.Tag> emptyTypes() {
        return Collections.emptyList();
    }

    public static Iterable<Direction> sidesOrAll(@Nullable Direction side) {
        return side == null ? GTEnergySides.ALL : Collections.singleton(side);
    }
}
