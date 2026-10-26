package gregtech.engine.energy;

import gregtech.content.data.GregTechTags;

/** Metadata for energy conductors (GT6 {@code ITileEntityEnergyDataConductor}). */
public interface IEnergyConductor {

    boolean isEnergyConducting(GregTechTags.Tag energyType);

    long getEnergyMaxSize(GregTechTags.Tag energyType);

    long getEnergyMaxPackets(GregTechTags.Tag energyType);

    long getEnergyLossPerMeter(GregTechTags.Tag energyType);
}
