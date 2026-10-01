package com.gregtech.gregtech.api.energy;

import com.gregtech.gregtech.api.material.GTMaterial;

/** Defines one gearbox tier (GT6 {@code MultiTileEntityGearBox}). */
public record GearboxSpec(String id, GTMaterial material, long maxSpeed, long maxPower) {}
