package gregtech.internal.energy;

import gregtech.internal.material.GTMaterial;

/** Defines one gearbox tier (GT6 {@code MultiTileEntityGearBox}). */
public record GearboxSpec(String id, GTMaterial material, long maxSpeed, long maxPower) {}
