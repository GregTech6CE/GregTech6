package com.gregtech.gregtech.api.fluid;

import com.gregtech.gregtech.data.RegisteredFluids;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/**
 * Vanilla-water parity facts for the three fluids GT6 puts into the world in place of vanilla water
 * ({@code Loader_Worldgen:576-578}: {@code WorldgenOcean("ocean.seawater")},
 * {@code WorldgenRiver("river.riverwater")}, {@code WorldgenSwamp("swamp.dirtywater")}).
 *
 * <p>The user report was that these three look darker than vanilla water. They are registered as
 * their own fluids (GT6 keeps three kinds, and the worldgen replaces vanilla water with them), so
 * nothing made them inherit vanilla water's rendering or attributes: their {@code FluidType} said
 * {@code canSwim(false)} / {@code canDrown(false)} / {@code canConvertToSource(false)} /
 * {@code supportsBoating(false)} / {@code canHydrate(false)} / {@code canExtinguish(false)}, their
 * client extension returned a GT sprite with the {@code 0xFFFFFFFF} "no tint" value instead of
 * water's tint, and they were not in {@code #minecraft:water}, so vanilla's own water fog, camera
 * overlay, swimming, boat and waterlogged-block checks all skipped them.
 *
 * <p>The constants here are the vanilla 1.20.1 values, so the registry, the loader, the client
 * visual policy and the GameTest all read the same numbers:
 * <ul>
 *   <li>{@code net.minecraft.world.level.block.Blocks.WATER} - the properties of the world water
 *       blocks,</li>
 *   <li>{@code net.minecraftforge.common.ForgeMod.WATER_TYPE} - the {@code FluidType} vanilla water
 *       gets from Forge; its {@code getTintColor()} is {@link #VANILLA_WATER_TINT} and its
 *       per-position tint is {@code BiomeColors.getAverageWaterColor},</li>
 *   <li>{@code net.minecraft.world.level.material.WaterFluid} - the flow numbers, the game rule
 *       gated source conversion and {@code canBeReplacedWith}.</li>
 * </ul>
 *
 * <p>Three of those checks are identity checks rather than {@code #minecraft:water} tests, so the
 * tag alone is not enough for them: Forge's entity water state ({@code Entity.isEyeInFluid},
 * {@code Entity.updateFluidHeightAndDoFluidPushing}, {@code IForgeEntity.isInFluidType}) compares the
 * {@code FluidType} instance against {@code ForgeMod.WATER_TYPE}, vanilla's waterlogged-block
 * contract compares the fluid instance against {@code Fluids.WATER}
 * ({@code SimpleWaterloggedBlock.java:17-22}), and the chunk mesher looks the fluid up in
 * {@code ItemBlockRenderTypes.FLUID_RENDER_TYPES}. See {@code GTWorldWaterFluid},
 * {@link VanillaWaterlogging} and {@link FluidRenderLayers} for how the three world waters satisfy
 * them while staying separate fluids.
 */
public final class GTWaterParity {
    /** The tint vanilla water's client extension reports: {@code ForgeMod.WATER_TYPE}. */
    public static final int VANILLA_WATER_TINT = 0xFF3F76E4;

    /** Vanilla water's sprites, exactly as {@code ForgeMod.WATER_TYPE} registers them. */
    public static final ResourceLocation VANILLA_WATER_STILL_TEXTURE =
            ResourceLocation.withDefaultNamespace("block/water_still");
    public static final ResourceLocation VANILLA_WATER_FLOWING_TEXTURE =
            ResourceLocation.withDefaultNamespace("block/water_flow");
    public static final ResourceLocation VANILLA_WATER_OVERLAY_TEXTURE =
            ResourceLocation.withDefaultNamespace("block/water_overlay");
    /** Vanilla water's camera overlay, shown while the eye is inside the fluid. */
    public static final ResourceLocation VANILLA_UNDERWATER_OVERLAY_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");

    /** Fluid registry path of GT6's sea water ({@code WorldgenOcean}, {@code FL.Ocean}). */
    public static final String SEAWATER = "seawater";
    /** Fluid registry path of GT6's river water ({@code WorldgenRiver}, {@code FL.River_Water}). */
    public static final String RIVERWATER = "riverwater";
    /** Fluid registry path of GT6's swamp water ({@code WorldgenSwamp}, {@code FL.Swampwater}). */
    public static final String SWAMPWATER = "swampwater";

    /**
     * The three world waters. The same six ids (each plus its {@code _flowing} variant) are the
     * contents written into {@code data/minecraft/tags/fluids/water.json}.
     */
    public static final Set<String> WORLD_WATER_PATHS = Set.of(SEAWATER, RIVERWATER, SWAMPWATER);

    /** GT6's registration order, still before flowing, of the six world-water registry paths. */
    public static final List<String> WORLD_WATER_REGISTRY_PATHS =
            List.of(SEAWATER, SEAWATER + "_flowing",
                    RIVERWATER, RIVERWATER + "_flowing",
                    SWAMPWATER, SWAMPWATER + "_flowing");

    /**
     * The same six paths as a set, so a lookup can ask "is this one of the world waters?" for the
     * flowing variant as well: a flowing world water is a registered fluid of its own
     * ({@code gregtech:seawater_flowing}) and it is what most positions of a GT6 water body actually
     * hold, so anything keyed by fluid name has to cover it.
     */
    private static final Set<String> ALL_WORLD_WATER_PATHS = Set.copyOf(WORLD_WATER_REGISTRY_PATHS);

    private GTWaterParity() {}

    /**
     * True for GT6's three world waters, keyed by fluid registry name: {@code "seawater"},
     * {@code "riverwater"}, {@code "swampwater"} <em>and</em> their {@code _flowing} variants. Uses
     * {@link RegisteredFluids#sanitizePath} so it accepts the raw GT6 name as well.
     *
     * <p>The flowing variants have to be accepted: {@link GTWaterParity#WORLD_WATER_PATHS} names the
     * three GT6 waters (which is what the fluid definitions and the {@code #minecraft:water} tag are
     * built from), but {@code gregtech:seawater_flowing} and friends are separate registered fluids
     * and every flowing block in a GT6 water body reports one of them, so a predicate that only knew
     * the three still names would answer "not a world water" for most of the water in the world.
     */
    public static boolean isWorldWater(String registryName) {
        return registryName != null && ALL_WORLD_WATER_PATHS.contains(RegisteredFluids.sanitizePath(registryName));
    }
}
