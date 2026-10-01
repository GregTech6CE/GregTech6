package com.gregtech.gregtech.api.energy;

import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.Direction;
import java.util.Collection;
import java.util.List;

/** Bottom HU port for the Neo crucible, using the same GregTech network contract. */
public interface HeatEnergyBlock extends IEnergyBlock {
    long receiveHeat(Direction side, long size, long amount, boolean commit);
    default boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) { return !emitting && type == GregTechTags.Energy.HU && side == Direction.DOWN; }
    default Collection<GregTechTags.Tag> getEnergyTypes(Direction side) { return side == Direction.DOWN ? List.of(GregTechTags.Energy.HU) : List.of(); }
    default boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) { return isEnergyType(type, side, false); }
    default boolean isEnergyEmittingTo(GregTechTags.Tag type, Direction side, boolean theoretical) { return false; }
    default long doEnergyInjection(GregTechTags.Tag type, Direction side, long size, long amount, boolean commit) { return EnergyBlockDefaults.doEnergyInjection(this, type, side, size, amount, commit); }
    default long doInject(GregTechTags.Tag type, Direction side, long size, long amount, boolean commit) { return receiveHeat(side, size, amount, commit); }
    default long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) { return isEnergyType(type, side, false) ? Long.MAX_VALUE : 0; }
    default long doEnergyExtraction(GregTechTags.Tag type, Direction side, long size, long amount, boolean commit) { return 0; }
    default long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) { return 0; }
    default long getEnergySizeInputMin(GregTechTags.Tag type, Direction side) { return 1; }
    default long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) { return 1; }
    default long getEnergySizeInputMax(GregTechTags.Tag type, Direction side) { return Long.MAX_VALUE; }
    default long getEnergySizeOutputMin(GregTechTags.Tag type, Direction side) { return 0; }
    default long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) { return 0; }
    default long getEnergySizeOutputMax(GregTechTags.Tag type, Direction side) { return 0; }
}
