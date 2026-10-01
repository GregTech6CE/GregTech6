package com.gregtech.gregtech.client;


import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.registry.GTBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Replaces the baked models of all {@link OreBlock}s (block states + item) with {@link OreBakedModel},
 * which renders the background stone from the {@link OreBlock#STONE} block-state property (item: the
 * {@code BlockStateTag}/{@code BlockEntityTag} on the stack) plus the material's icon-set ore overlay
 * (tint 0 → material color via the registered color handlers).
 *
 * <p>§103.B: because the host rock became a state property, the baking result holds one model location
 * per state ({@code gregtech:ore_iron#stone=granite}) instead of a single {@code #}; each gets the
 * stone-pinned {@link OreBakedModel#stoneModel} view. The blockstate JSON still only needs its one
 * {@code ""} variant.
 */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OreClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private OreClientModels() {}

    private static final ResourceLocation TEMPLATE = ResourceLocation.withDefaultNamespace("block/stone");

    @SubscribeEvent
    public static void registerTemplate(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(TEMPLATE));
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        BakedModel template = BakedModelLookup.find(models, TEMPLATE);
        if (template == null) {
            LOGGER.error("[{}] vanilla stone model missing — ore models not replaced", "gregtech");
            return;
        }

        Map<String, ResourceLocation> overlayCache = new HashMap<>();
        // Material RGB is supplied by color handlers, not stored in the geometry.
        Map<ResourceLocation, OreBakedModel> sharedModels = new HashMap<>();
        int mapped = 0;
        int states = 0;
        for (var entry : GTBlocks.allEntries()) {
            if (!entry.isBound() || !(entry.get() instanceof OreBlock ore)) continue;
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(ore);
            if (blockId == null) continue;

            ResourceLocation overlay = resolveOverlayTexture(ore, overlayCache);
            OreBakedModel model = sharedModels.computeIfAbsent(overlay, texture -> new OreBakedModel(template, texture));
            for (BlockState state : ore.getStateDefinition().getPossibleStates()) {
                models.put(BlockModelShaper.stateToModelLocation(blockId, state),
                        model.stoneModel(OreBlock.stoneOf(state), OreBlock.isBroken(state)));
                states++;
            }
            models.put(new ModelResourceLocation(blockId, "inventory"), model);
            mapped++;
        }
        LOGGER.info("[{}] Ore model replacement: {} ore blocks, {} state models from {} shared models",
                "gregtech", mapped, states, sharedModels.size());
    }

    /**
     * {@code material_icons/<set>/ore[small].png} for the material's texture set, falling back
     * to DULL then METALLIC when the set has no ore overlay texture.
     */
    private static ResourceLocation resolveOverlayTexture(OreBlock ore, Map<String, ResourceLocation> cache) {
        String file = ore.isSmall() ? "oresmall" : "ore";
        MaterialTextureSet primary = MaterialIcons.resolveTextureSet(ore.material());
        String cacheKey = primary.folder() + "/" + file;
        return cache.computeIfAbsent(cacheKey, key -> {
            for (MaterialTextureSet set : new MaterialTextureSet[]{primary, MaterialTextureSet.DULL, MaterialTextureSet.METALLIC}) {
                ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("gregtech",
                        "block/material_icons/" + set.folder() + "/" + file);
                ResourceLocation file_png = ResourceLocation.fromNamespaceAndPath("gregtech",
                        "textures/block/material_icons/" + set.folder() + "/" + file + ".png");
                if (Minecraft.getInstance().getResourceManager().getResource(file_png).isPresent()) {
                    return texture;
                }
            }
            return ResourceLocation.fromNamespaceAndPath("gregtech", "block/material_icons/metallic/" + file);
        });
    }
}
