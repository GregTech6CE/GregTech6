/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import gregtech.api.addon.worldgen.*;

import gregtech.api.addon.recipe.*;








import java.util.List;

/**
 * Early material entry discovered with {@link java.util.ServiceLoader}.
 * List this class in {@code META-INF/services/gregtech.api.addon.material.AddonMaterialContributor}.
 * GregTech loads it while materials are still open. Return pure data.
 * {@link #link()} runs later, after the built-in directory has linked.
 */
public interface AddonMaterialContributor {
    /** The addon's own loader mod id, not {@code gt6ce}, {@code gregtech6} or {@code minecraft}. */
    String addonId();

    List<MaterialDeclaration> materials();

    List<MaterialFormRequest> forms();

    /**
     * Item prefixes this addon adds. GregTech registers them after the built-in prefixes.
     * A name that already exists fails startup. Return pure data.
     */
    default List<PrefixDeclaration> prefixes() { return List.of(); }

    /**
     * Compositions of this addon's own materials. Resolved after the built-in directory has linked.
     * A {@code crucible} composition also becomes one crucible reaction.
     */
    default List<MaterialComposition> compositions() { return List.of(); }

    /** Smelting results for this addon's own materials. Omitted materials still melt into themselves. */
    default List<SmeltingTargetDeclaration> smeltingTargets() { return List.of(); }

    /**
     * Crucible smelting targets to drop, including built-in ones. Applied after every addon's
     * smelting targets. Use this for ores that must not melt into a metal.
     */
    default List<SmeltingTargetRemoval> smeltingTargetRemovals() { return List.of(); }

    /** Explicit crucible alloy rows, in addition to composition-derived rows. */
    default List<CrucibleReactionDeclaration> crucibleReactions() { return List.of(); }

    /**
     * Crucible rows to drop, applied after every addon's additions.
     * An empty input list drops every row of that output. A miss warns and does not fail startup.
     */
    default List<CrucibleReactionRemoval> crucibleRemovals() { return List.of(); }

    /**
     * Large ore veins. GregTech appends them after the built-in veins of the same dimension.
     * A name that already exists in that dimension fails startup. Return pure data.
     */
    default List<OreVeinDeclaration> oreVeins() { return List.of(); }

    /**
     * Small ores. GregTech appends them after the built-in small ores of the same dimension.
     * A name that already exists in that dimension fails startup. Return pure data.
     */
    default List<SmallOreDeclaration> smallOres() { return List.of(); }

    /** Other cross-material links. New materials still belong in {@link #materials()}. */
    default void link() {}
}
