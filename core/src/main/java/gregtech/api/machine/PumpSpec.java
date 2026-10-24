package gregtech.api.machine;

import gregtech.api.material.GTMaterial;

/** Defines one rotational pump tier (GT6 {@code MultiTileEntityPump}). */
public record PumpSpec(String id, GTMaterial material, long inputSpeed, int tier) {}
