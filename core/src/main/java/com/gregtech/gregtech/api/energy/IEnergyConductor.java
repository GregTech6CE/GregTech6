package com.gregtech.gregtech.api.energy;

import com.gregtech.gregtech.data.GregTechTags;

/** Metadata for energy conductors (GT6 {@code ITileEntityEnergyDataConductor}). */
public interface IEnergyConductor {

    boolean isEnergyConducting(GregTechTags.Tag energyType);

    long getEnergyMaxSize(GregTechTags.Tag energyType);

    long getEnergyMaxPackets(GregTechTags.Tag energyType);

    long getEnergyLossPerMeter(GregTechTags.Tag energyType);
}
