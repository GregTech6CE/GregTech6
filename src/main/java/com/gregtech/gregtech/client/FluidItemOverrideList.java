package com.gregtech.gregtech.client;

import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.item.FluidItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves the correct fluid-texture {@link BakedModel} per {@link ItemStack} at render time. */
public final class FluidItemOverrideList extends ItemOverrides {
    public static final FluidItemOverrideList INSTANCE = new FluidItemOverrideList();
    private record ModelKey(BakedModel base, ResourceLocation texture) {}
    private static final BoundedCache<ModelKey, BakedModel> CACHE = new BoundedCache<>(256);

    public static void clearCache() { CACHE.clear(); }

    @Override
    public BakedModel resolve(@NotNull BakedModel original, @NotNull ItemStack stack,
                              @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        if (!(stack.getItem() instanceof FluidItem fluidItem)) {
            return original;
        }
        RegisteredFluids.FluidEntry flEntry = fluidItem.fluidEntry();
        ResourceLocation texKey;
        if(com.gregtech.gregtech.api.fluid.FluidDisplayBinding.hasPayload(stack)) {
            var actual=com.gregtech.gregtech.api.fluid.FluidDisplayBinding.resolve(stack);
            texKey=actual.isEmpty()?resolveTextureKey(flEntry):IClientFluidTypeExtensions.of(actual.getFluid()).getStillTexture(actual);
        } else texKey=resolveTextureKey(flEntry);
        if (texKey == null) {
            return original;
        }
        return CACHE.computeIfAbsent(new ModelKey(original, texKey), k -> {
            TextureAtlas atlas = Minecraft.getInstance().getModelManager()
                    .getAtlas(TextureAtlas.LOCATION_BLOCKS);
            TextureAtlasSprite sprite = atlas.getSprite(k.texture());
            return new FluidItemBakedModel(original, sprite);
        });
    }

    @Nullable
    private static ResourceLocation resolveTextureKey(RegisteredFluids.FluidEntry flEntry) {
        // sanitizePath also converts '.' -> '_' — molten fluids register as
        // "molten_copper", not "molten.copper"; the old lookup missed them all
        // (purple-black missing texture on every molten fluid item).
        String path = RegisteredFluids.sanitizePath(flEntry.registryName());
        ResourceLocation fluidId = ResourceLocation.fromNamespaceAndPath("gregtech", path);
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
        if (fluid != null) {
            FluidType type = fluid.getFluidType();
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(type);
            ResourceLocation stillTex = ext.getStillTexture();
            if (stillTex != null) {
                return stillTex;
            }
        }
        return FluidAppearance.appearance(flEntry).texture();
    }
}
