package com.gregtech.gregtech.internal.addon;

import com.gregtech.gregtech.api.addon.platform.EnergyBlock;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later. Presents an addon energy block to the existing energy network. */
public final class AddonEnergyAdapter implements IEnergyBlock {
    private final EnergyBlock addon;

    private AddonEnergyAdapter(EnergyBlock addon) {
        this.addon = addon;
    }

    public static IEnergyBlock of(EnergyBlock addon) {
        if (addon == null) throw new IllegalArgumentException("energy block");
        return new AddonEnergyAdapter(addon);
    }

    private static String name(GregTechTags.Tag energy) {
        return energy == null ? "" : energy.getShortName();
    }

    @Override public boolean isEnergyType(GregTechTags.Tag energyType, Direction side, boolean emitting) {
        return addon.isEnergyType(name(energyType), side, emitting);
    }

    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) {
        Collection<String> names = addon.getEnergyTypes(side);
        if (names == null || names.isEmpty()) return List.of();
        var tags = new ArrayList<GregTechTags.Tag>(names.size());
        for (String energy : names) tags.add(AddonPlatformServices.tag(energy));
        return List.copyOf(tags);
    }

    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, Direction side, boolean theoretical) {
        return addon.isEnergyAcceptingFrom(name(energyType), side, theoretical);
    }

    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, Direction side, boolean theoretical) {
        return addon.isEnergyEmittingTo(name(energyType), side, theoretical);
    }

    @Override public long doEnergyInjection(GregTechTags.Tag energyType, Direction side, long size, long amount, boolean doInject) {
        return addon.doEnergyInjection(name(energyType), side, size, amount, doInject);
    }

    @Override public long getEnergyDemanded(GregTechTags.Tag energyType, Direction side, long size) {
        return addon.getEnergyDemanded(name(energyType), side, size);
    }

    @Override public long doEnergyExtraction(GregTechTags.Tag energyType, Direction side, long size, long amount, boolean doExtract) {
        return addon.doEnergyExtraction(name(energyType), side, size, amount, doExtract);
    }

    @Override public long getEnergyOffered(GregTechTags.Tag energyType, Direction side, long size) {
        return addon.getEnergyOffered(name(energyType), side, size);
    }

    @Override public long getEnergySizeInputMin(GregTechTags.Tag energyType, Direction side) {
        return addon.getEnergySizeInputMin(name(energyType), side);
    }

    @Override public long getEnergySizeOutputMin(GregTechTags.Tag energyType, Direction side) {
        return addon.getEnergySizeOutputMin(name(energyType), side);
    }

    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, Direction side) {
        return addon.getEnergySizeInputRecommended(name(energyType), side);
    }

    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, Direction side) {
        return addon.getEnergySizeOutputRecommended(name(energyType), side);
    }

    @Override public long getEnergySizeInputMax(GregTechTags.Tag energyType, Direction side) {
        return addon.getEnergySizeInputMax(name(energyType), side);
    }

    @Override public long getEnergySizeOutputMax(GregTechTags.Tag energyType, Direction side) {
        return addon.getEnergySizeOutputMax(name(energyType), side);
    }

    @Override public boolean hasEnergySurface(Direction side) {
        return addon.hasEnergySurface(side);
    }

    @Override public boolean isEnergyCapacitorType(GregTechTags.Tag energyType, Direction side) {
        return addon.isEnergyCapacitorType(name(energyType), side);
    }

    @Override public long getEnergyStored(GregTechTags.Tag energyType, Direction side) {
        return addon.getEnergyStored(name(energyType), side);
    }

    @Override public long getEnergyCapacity(GregTechTags.Tag energyType, Direction side) {
        return addon.getEnergyCapacity(name(energyType), side);
    }

    @Override public Collection<GregTechTags.Tag> getEnergyCapacitorTypes(Direction side) {
        Collection<String> names = addon.getEnergyCapacitorTypes(side);
        if (names == null || names.isEmpty()) return List.of();
        var tags = new ArrayList<GregTechTags.Tag>(names.size());
        for (String energy : names) tags.add(AddonPlatformServices.tag(energy));
        return List.copyOf(tags);
    }

    @Override public long doInject(GregTechTags.Tag energyType, Direction side, long size, long amount, boolean doInject) {
        return addon.doInject(name(energyType), side, size, amount, doInject);
    }

    @Override public long doExtract(GregTechTags.Tag energyType, Direction side, long size, long amount, boolean doExtract) {
        return addon.doExtract(name(energyType), side, size, amount, doExtract);
    }
}
