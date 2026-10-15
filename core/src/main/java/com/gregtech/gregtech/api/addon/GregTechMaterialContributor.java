package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.material.MaterialDefinition;

import java.util.List;

/**
 * Early material entry discovered with {@link java.util.ServiceLoader}.
 * List this class in {@code META-INF/services/com.gregtech.gregtech.api.addon.GregTechMaterialContributor}.
 * GregTech loads it on the GregTech thread while materials are still open, so a mod constructor
 * that runs before or beside GregTech cannot be the material entry.
 * Return pure data. Do not touch {@code Materials} from the constructor or from these methods;
 * {@link #link()} runs later, after the built-in directory has linked.
 */
public interface GregTechMaterialContributor {
    /** The addon's own loader mod id, not {@code gregtech} or {@code gregtech6}. */
    String addonId();

    List<MaterialDefinition> materials();

    List<MaterialFormRequest> forms();

    /** Compositions and other cross-material links. New materials still belong in {@link #materials()}. */
    default void link() {}
}
