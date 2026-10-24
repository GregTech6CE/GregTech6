package gregtech.internal.machine;

import gregtech.internal.material.GTMaterial;

/** Defines one rotational pump tier (GT6 {@code MultiTileEntityPump}). */
public record PumpSpec(String id, GTMaterial material, long inputSpeed, int tier) {}
