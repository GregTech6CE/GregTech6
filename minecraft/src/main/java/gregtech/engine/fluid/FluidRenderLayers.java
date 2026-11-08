package gregtech.engine.fluid;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which chunk render layer the port puts a fluid's world blocks in.
 *
 * <p><b>The vanilla mechanism.</b> The chunk mesher picks a fluid's layer with
 * {@code ItemBlockRenderTypes.getRenderLayer(FluidState)}
 * ({@code ChunkRenderDispatcher.java}:
 * {@code RenderType rendertype = ItemBlockRenderTypes.getRenderLayer(fluidstate);}), and that method
 * looks the fluid up in a map that vanilla fills with exactly two entries:
 * {@code Fluids.WATER} and {@code Fluids.FLOWING_WATER} mapped to
 * {@code RenderType.translucent()} ({@code ItemBlockRenderTypes.java}); every fluid that is
 * not in the map falls back to {@code RenderType.solid()}
 * ({@code ItemBlockRenderTypes.java}, {@code FLUID_RENDER_TYPES} default).
 * {@code RenderType.translucent()} is what makes vanilla water see-through. GT6's original
 * {@code BlockBaseFluid} used render pass 1 for each of its world fluids, including the oil and gas
 * springs, so those placed fluid families need that layer here.
 *
 * <p>The layer is named here as a string instead of a {@code RenderType} because
 * {@code net.minecraft.client.renderer.RenderType} is {@code @OnlyIn(Dist.CLIENT)} while this class
 * is common. The client maps the name back through {@link #isTranslucent} and calls
 * {@code ItemBlockRenderTypes.setRenderLayer(fluid, RenderType.translucent())} for all twelve
 * source/flowing world-fluid entries from {@code GregTechClient.clientSetup}.
 */
public final class FluidRenderLayers {
    /** {@code RenderType.translucent()} - vanilla water's layer. */
    public static final String TRANSLUCENT = "translucent";

    /** GT6's other world-placed {@code BlockBaseFluid}s also use render pass 1. */
    private static final List<String> WORLD_SPRINGS = List.of(
            "liquid_extra_heavy_oil",
            "liquid_heavy_oil",
            "liquid_medium_oil",
            "liquid_light_oil",
            "gas_natural_gas",
            "watergeothermal");

    private FluidRenderLayers() {}

    /** Source names used by world-block registration. */
    public static Set<String> worldFluidPaths() {
        return Set.copyOf(WORLD_SPRINGS);
    }

    /** Every GT6 fluid that can be placed in terrain uses its original translucent render pass. */
    public static Map<String, String> worldFluidLayers() {
        Map<String, String> layers = new LinkedHashMap<>();
        for (String spring : WORLD_SPRINGS) {
            layers.put(spring, TRANSLUCENT);
            layers.put(spring + "_flowing", TRANSLUCENT);
        }
        return layers;
    }

    /** True when the name is {@link #TRANSLUCENT}, i.e. when the client must register translucent. */
    public static boolean isTranslucent(String layer) {
        return TRANSLUCENT.equals(layer);
    }
}
