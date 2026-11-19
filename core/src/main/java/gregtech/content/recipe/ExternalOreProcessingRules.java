/* Gregorius Techneticies / GregTech-6 Team source data, LGPL-3.0-or-later. */
package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.generated.MaterialWorkability;
import java.util.List;

/** Loader_Recipes_Handlers external-prefix rows; RecipeMapHandlerPrefix:getCosts and Shredding targets. */
public final class ExternalOreProcessingRules {
    private ExternalOreProcessingRules() {}
    /** RecipeMapCrucible:getNEIRecipes lists these eight external ore forms, not clump/crystal/rawOreChunk. */
    public static final List<MaterialPrefix> CRUCIBLE_FORMS = List.of(
            MaterialPrefix.chunk,
            MaterialPrefix.rubble,
            MaterialPrefix.pebbles,
            MaterialPrefix.cluster,
            MaterialPrefix.cleanGravel,
            MaterialPrefix.dirtyGravel,
            MaterialPrefix.crystalline,
            MaterialPrefix.reduced);

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

    /** Two Shredder speeds ({@code Loader_Recipes_Handlers}); the Anvil rows require an
     *  empty workpiece ({@code Loader_Recipes_Handlers}). */
    public record GrindingRoute(
            String map, MaterialPrefix input, int dustCount, int fineCount, boolean excludesBedrock) {
        public GrindingRoute(String map, MaterialPrefix input, int dustCount) {
            this(map, input, dustCount, 1, false);
        }

        public GrindingRoute(String map, MaterialPrefix input, int dustCount, boolean fines) {
            this(map, input, dustCount, fines ? 1 : 0, false);
        }

        public boolean fines() {
            return fineCount > 0;
        }

        public boolean requiresEmptySlot() {
            return map.equals("Anvil");
        }

        public boolean pulverizedRemains() {
            return map.equals("Mortar");
        }

        public boolean allows(GTMaterial material) {
            return ExternalOreProcessingRules.allows(material)
                    && (!excludesBedrock
                            || material.resolve() != gregtech.content.material.generated.CompoundMaterials.Bedrock)
                    && (!(requiresEmptySlot() || pulverizedRemains())
                            || MaterialWorkability.isMortarGrindable(material));
        }

        public GTMaterial outputMaterial(GTMaterial material) {
            return material.getTargetPulverMaterial().resolve();
        }

        public long duration(int quality, boolean mortarGrindable) {
            long units = Math.max(input.getMaterialWeight(), dustCount * GTValues.U + fineCount * GTValues.U9);
            long multiplier = requiresEmptySlot() || pulverizedRemains() || mortarGrindable ? 16 : 256;
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
            new GrindingRoute("Anvil", MaterialPrefix.pebbles, 2),
            // No fines at all ({@code Loader_Recipes_Handlers}).
            new GrindingRoute("Shredder", MaterialPrefix.clump, 1, false),
            new GrindingRoute("Shredder", MaterialPrefix.reduced, 1, false),
            new GrindingRoute("Shredder", MaterialPrefix.crystalline, 1, false),
            new GrindingRoute("Shredder", MaterialPrefix.cleanGravel, 1, false),
            new GrindingRoute("Shredder", MaterialPrefix.cluster, 3, false),
            new GrindingRoute("Anvil", MaterialPrefix.clump, 1, false),
            new GrindingRoute("Anvil", MaterialPrefix.reduced, 1, false),
            new GrindingRoute("Anvil", MaterialPrefix.crystalline, 1, false),
            new GrindingRoute("Anvil", MaterialPrefix.cleanGravel, 1, false),
            new GrindingRoute("Anvil", MaterialPrefix.cluster, 3, false),
            // use OM.pulverize(target amount), not a fixed dust count.
            new GrindingRoute("Mortar", MaterialPrefix.cleanGravel, 0, false),
            new GrindingRoute("Mortar", MaterialPrefix.crystalline, 0, false),
            new GrindingRoute("Mortar", MaterialPrefix.reduced, 0, false),
            new GrindingRoute("Mortar", MaterialPrefix.clump, 0, false),
            new GrindingRoute("Mortar", MaterialPrefix.dirtyGravel, 0, false),
            new GrindingRoute("Mortar", MaterialPrefix.crystal, 0, false));

    public static boolean allows(GTMaterial material) {
        return material.isValid() && !material.has(MaterialProperty.ANTIMATTER);
    }
}
