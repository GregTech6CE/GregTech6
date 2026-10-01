package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/** Defines one rotational pump tier (GT6 {@code MultiTileEntityPump}). */
public record PumpSpec(String id, GTMaterial material, long inputSpeed, int tier) {}
