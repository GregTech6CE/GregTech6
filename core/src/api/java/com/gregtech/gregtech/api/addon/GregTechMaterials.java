package com.gregtech.gregtech.api.addon;

import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/** Read-only material handles. The material table itself stays inside GregTech. */
public final class GregTechMaterials {
    private GregTechMaterials() {}

    public static Material find(String name) { return AddonRuntime.services().materials().find(name); }

    public static Material find(int id) { return AddonRuntime.services().materials().find(id); }

    /** The sentinel returned for an unknown name or id. {@link Material#valid()} is false. */
    public static Material invalid() { return AddonRuntime.services().materials().invalid(); }

    public static Iterable<Material> all() { return AddonRuntime.services().materials().all(); }

    public interface Catalog {
        Material find(String name);

        Material find(int id);

        Material invalid();

        Iterable<Material> all();
    }

    /** One material after aliases and links have resolved. */
    public interface Material {
        String name();

        int id();

        boolean valid();

        int color();

        int meltingPoint();

        int boilingPoint();

        float density();

        /** Tooltip formula, from the composition when the material has one. */
        String formula();

        String translationKey();

        List<Component> composition();

        Material resolve();
    }

    /** One entry of a material's composition. {@code amount} is in GregTech units ({@link com.gregtech.gregtech.api.material.GTValues#U}). */
    public record Component(String material, long amount) {}
}
