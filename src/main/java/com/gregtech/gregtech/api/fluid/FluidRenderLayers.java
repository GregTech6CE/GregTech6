package com.gregtech.gregtech.api.fluid;

import com.gregtech.gregtech.data.RegisteredFluids;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which chunk render layer the port puts a fluid's world blocks in.
 *
 * <p><b>The vanilla mechanism.</b> The chunk mesher picks a fluid's layer with
 * {@code ItemBlockRenderTypes.getRenderLayer(FluidState)}
 * ({@code ChunkRenderDispatcher.java:620}:
 * {@code RenderType rendertype = ItemBlockRenderTypes.getRenderLayer(fluidstate);}), and that method
 * looks the fluid up in a map that vanilla fills with exactly two entries:
 * {@code Fluids.WATER} and {@code Fluids.FLOWING_WATER} mapped to
 * {@code RenderType.translucent()} ({@code ItemBlockRenderTypes.java:315-319}); every fluid that is
 * not in the map falls back to {@code RenderType.solid()}
 * ({@code ItemBlockRenderTypes.java:377-380}, {@code FLUID_RENDER_TYPES} default at line 400).
 * {@code RenderType.translucent()} is what makes vanilla water see-through. GT6's original
 * {@code BlockBaseFluid} used render pass 1 for each of its world fluids, including oil, gas and
 * geothermal springs, so all nine placed fluid families need that layer here.
 *
 * <p>The layer is named here as a string instead of a {@code RenderType} because
 * {@code net.minecraft.client.renderer.RenderType} is {@code @OnlyIn(Dist.CLIENT)} while this class
 * is common and is read by the server-side GameTest that asserts the parity. The client maps the
 * name back through {@link #isTranslucent} and calls
 * {@code ItemBlockRenderTypes.setRenderLayer(fluid, RenderType.translucent())} for all eighteen
 * source/flowing world-fluid entries from {@code GregTechClient.clientSetup}.
 */
public final class FluidRenderLayers {
    /** {@code RenderType.translucent()} - vanilla water's layer. */
    public static final String TRANSLUCENT = "translucent";
    /** {@code RenderType.solid()} - vanilla's fallback for a fluid that is not in its table. */
    public static final String SOLID = "solid";

    /** GT6's other world-placed {@code BlockBaseFluid}s also use render pass 1. */
    private static final List<String> WORLD_SPRINGS = List.of(
            "liquid_extra_heavy_oil", "liquid_heavy_oil", "liquid_medium_oil",
            "liquid_light_oil", "gas_natural_gas", "watergeothermal");

    private FluidRenderLayers() {}

    /**
     * The layer vanilla water is registered in: {@code ItemBlockRenderTypes.TYPE_BY_FLUID} maps
     * {@code Fluids.WATER} and {@code Fluids.FLOWING_WATER} to {@code RenderType.translucent()}.
     */
    public static String vanillaWater() {
        return TRANSLUCENT;
    }

    /**
     * The layer the port registers for a GT6 fluid, keyed by fluid registry path
     * ({@code "seawater"}, {@code "riverwater"}, {@code "swampwater"}, plus the {@code _flowing}
     * variants - all six are world waters, see {@link GTWaterParity#isWorldWater}). They and the six
     * spring families get GT6's translucent world layer; non-world fluids keep the default.
     */
    public static String select(String registryPath) {
        if (registryPath == null) return SOLID;
        String path = pathOf(registryPath);
        if (GTWaterParity.isWorldWater(path)) return vanillaWater();
        String still = path.endsWith("_flowing") ? path.substring(0, path.length() - 8) : path;
        return WORLD_SPRINGS.contains(still) ? TRANSLUCENT : SOLID;
    }

    /**
     * The exact registration the client performs: every world-water fluid id, still and flowing, with
     * the layer to register for it, in {@link GTWaterParity#WORLD_WATER_REGISTRY_PATHS} order.
     *
     * <p>{@code GregTechClient.clientSetup} iterates this map and calls
     * {@code ItemBlockRenderTypes.setRenderLayer(fluid, RenderType.translucent())} for each entry
     * whose layer {@link #isTranslucent}; this subset stays six world waters, while
     * {@link #worldFluidLayers()} adds the twelve spring entries for client registration.
     */
    public static Map<String, String> worldWaterLayers() {
        Map<String, String> layers = new LinkedHashMap<>();
        for (String registryPath : GTWaterParity.WORLD_WATER_REGISTRY_PATHS) {
            layers.put(registryPath, select(registryPath));
        }
        return layers;
    }

    /** Source names used by world-block registration, including the three water replacements. */
    public static Set<String> worldFluidPaths() {
        Set<String> paths = new LinkedHashSet<>(GTWaterParity.WORLD_WATER_PATHS);
        paths.addAll(WORLD_SPRINGS);
        return Set.copyOf(paths);
    }

    /** Every GT6 fluid that can be placed in terrain uses its original translucent render pass. */
    public static Map<String, String> worldFluidLayers() {
        Map<String, String> layers = worldWaterLayers();
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

    /** The registry path of a fluid id, for {@link #select}; also accepts a GT6 raw name. */
    public static String pathOf(String registryNameOrPath) {
        return RegisteredFluids.sanitizePath(registryNameOrPath);
    }
}
