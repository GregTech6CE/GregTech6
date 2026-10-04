/* Gregorius Techneticies / GregTech-6 Team source data, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;

/** Loader_Recipes_Handlers:62,65-67 and RecipeMapHandlerPrefix:getCosts, with OP:142,146-148. */
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
    public static boolean allows(GTMaterial material) {
        return material.isValid() && !material.has(MaterialProperty.ANTIMATTER);
    }
}
