/* Gregorius Techneticies / GregTech-6 Team source data, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import java.util.List;

/** Loader_Recipes_Handlers external-prefix rows; RecipeMapHandlerPrefix:getCosts and Shredding targets. */
public final class ExternalOreProcessingRules {
    private ExternalOreProcessingRules() {}
    public record Route(String map, MaterialPrefix input, MaterialPrefix output, int count, long multiplier) {
        public long duration(int toolQuality) {
            long units = Math.max(input.getMaterialWeight(), output.getMaterialWeight() * count);
            long cost = Math.multiplyExact(Math.multiplyExact(units, multiplier), toolQuality + 1L);
            return Math.max(1, (cost + GTValues.U - 1) / GTValues.U);
        }
    }
    public static final List<Route> ROUTES = List.of(
            new Route("Crusher", MaterialPrefix.rawOreChunk, MaterialPrefix.crushedTiny, 3, 64),
            new Route("Crusher", MaterialPrefix.chunk, MaterialPrefix.rubble, 1, 128),
            new Route("Crusher", MaterialPrefix.rubble, MaterialPrefix.pebbles, 1, 128),
            new Route("Sifting", MaterialPrefix.pebbles, MaterialPrefix.dust, 3, 512));

    /** Source :117-119/:129-131 select two Shredder speeds; :158-160 require an empty Anvil workpiece. */
    public record GrindingRoute(String map, MaterialPrefix input, int dustCount) {
        public boolean requiresEmptySlot() { return map.equals("Anvil"); }
        public boolean allows(GTMaterial material) {
            return ExternalOreProcessingRules.allows(material)
                    && (!requiresEmptySlot() || MaterialWorkability.isMortarGrindable(material));
        }
        public GTMaterial outputMaterial(GTMaterial material) { return material.getTargetPulverMaterial().resolve(); }
        public long duration(int quality, boolean mortarGrindable) {
            long units = Math.max(input.getMaterialWeight(), dustCount * GTValues.U + GTValues.U9);
            long multiplier = requiresEmptySlot() || mortarGrindable ? 16 : 256;
            long cost = Math.multiplyExact(Math.multiplyExact(units, multiplier), quality + 1L);
            return Math.max(1, (cost + GTValues.U - 1) / GTValues.U);
        }
        public long duration(GTMaterial material) {
            return duration(material.getToolQuality(), MaterialWorkability.isMortarGrindable(material));
        }
    }
    public static final List<GrindingRoute> GRINDING = List.of(
            new GrindingRoute("Shredder", MaterialPrefix.chunk, 2),
            new GrindingRoute("Shredder", MaterialPrefix.rubble, 2),
            new GrindingRoute("Shredder", MaterialPrefix.pebbles, 3),
            new GrindingRoute("Anvil", MaterialPrefix.chunk, 2),
            new GrindingRoute("Anvil", MaterialPrefix.rubble, 2),
            new GrindingRoute("Anvil", MaterialPrefix.pebbles, 2));
    public static boolean allows(GTMaterial material) {
        return material.isValid() && !material.has(MaterialProperty.ANTIMATTER);
    }
}
