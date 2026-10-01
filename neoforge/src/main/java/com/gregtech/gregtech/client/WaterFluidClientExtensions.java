package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.fluid.GTWaterParity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * Client rendering of GT6's three world waters (sea, river and swamp water), copied value by value
 * from {@code ForgeMod.WATER_TYPE}'s client extension in Forge 1.20.1 so they are drawn exactly like
 * vanilla water instead of with GT6's darker dedicated sprites.
 *
 * <p>Textures are vanilla's own {@code block/water_still}, {@code block/water_flow} and
 * {@code block/water_overlay}; the tint is {@link GTWaterParity#VANILLA_WATER_TINT} for stacks and
 * items (via {@link #getTintColor()}) and vanilla's biome water colour
 * ({@code BiomeColors.getAverageWaterColor}) for blocks in the world, which is what
 * {@code LiquidBlockRenderer} asks for. The camera overlay is vanilla's
 * {@code textures/misc/underwater.png}.
 *
 * <p>This is the extension of the GT6 {@code FluidType} the port registers for those three fluids.
 * The <em>fluids</em> themselves report {@code ForgeMod.WATER_TYPE} (see
 * {@code GTWorldWaterFluid}), so the world renderer actually asks vanilla's own extension for them
 * ({@code ForgeHooksClient.getFluidSprites} and {@code IClientFluidTypeExtensions.of(FluidState)}
 * both go through {@code FluidState.getFluidType()}), which returns the same still/flow/texture
 * overlay sprites, the same tint and the same camera overlay this class returns; the values here
 * are the port's own copy of them and are what any code holding the GT6 type sees.
 *
 * <p>Client-only: it is instantiated from {@code FluidType.initializeClient}, which Forge only calls
 * on a physical client and never in datagen, so the client classes referenced here are never loaded
 * on a dedicated server. This mirrors the existing {@link FluidAppearance} reference in the common
 * fluid registry.
 */
public final class WaterFluidClientExtensions implements IClientFluidTypeExtensions {
    /** Stateless, so one shared instance serves all three fluids. */
    public static final WaterFluidClientExtensions INSTANCE = new WaterFluidClientExtensions();

    private WaterFluidClientExtensions() {}

    @Override
    public ResourceLocation getStillTexture() {
        return GTWaterParity.VANILLA_WATER_STILL_TEXTURE;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return GTWaterParity.VANILLA_WATER_FLOWING_TEXTURE;
    }

    @Override
    public ResourceLocation getOverlayTexture() {
        return GTWaterParity.VANILLA_WATER_OVERLAY_TEXTURE;
    }

    @Override
    public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
        return GTWaterParity.VANILLA_UNDERWATER_OVERLAY_TEXTURE;
    }

    @Override
    public int getTintColor() {
        return GTWaterParity.VANILLA_WATER_TINT;
    }

    @Override
    public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return BiomeColors.getAverageWaterColor(getter, pos) | 0xFF000000;
    }
}
