package gregtech.content.energy;

import gregtech.data.GregTechTags;

/** Shared passive battery contract, used by chemical cells and GT6 energy crystals. */
public interface PortableBatterySpec {
    String id();
    int tier();
    long voltage();
    long capacity();
    long minimumPacket();
    long maximumPacket();
    int inset();
    int height();
    int scale();
    String texture();
    int display(long energy);
    int color();
    GregTechTags.Tag energyType();
    default boolean barIndicator() { return true; }
    default int tint(long energy) { return color(); }
}
