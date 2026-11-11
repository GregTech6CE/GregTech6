package gregtech.client;
import gregtech.engine.mod.GregTechIdentity;

import gregtech.api.material.MaterialTextureSet;
import gregtech.content.block.OreBlock;
import gregtech.content.registry.GTBlocks;
import gregtech.platform.Mc;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
//?}
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Replaces the baked models of all {@link OreBlock}s (block states + item) with {@link OreBakedModel},
 * which renders the background stone from the {@link OreBlock#STONE} block-state property (item: the
 * {@code BlockStateTag}/{@code BlockEntityTag} on the stack) plus the material's icon-set ore overlay
 * (tint 0 → material color via the registered color handlers).
 *
 * <p>Because the host rock is a state property, the baking result holds one model location
 * per state ({@code gt6ce:ore_iron#stone=granite}) instead of a single {@code #}; each gets the
 * stone-pinned {@link OreBakedModel#stoneModel} view. The blockstate JSON still only needs its one
 * {@code ""} variant.
 */
public final class OreClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private OreClientModels() {}

    private static final ResourceLocation TEMPLATE = ResourceLocation.withDefaultNamespace("block/stone");

    public static void registerTemplate(ModelEvent.RegisterAdditional event) {
        ClientModelEvents.registerAdditional(event, TEMPLATE);
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        BakedModel template = models.find(TEMPLATE);
        if (template == null) {
            LOGGER.error("[{}] vanilla stone model missing — ore models not replaced", GregTechIdentity.REGISTRY_NAMESPACE);
            return;
        }

        Map<String, ResourceLocation> overlayCache = new HashMap<>();
        // Material RGB is supplied by color handlers, not stored in the geometry.
        Map<ResourceLocation, OreBakedModel> sharedModels = new HashMap<>();
        int mapped = 0;
        int states = 0;
        for (var entry : GTBlocks.allEntries()) {
//? if >=1.21.1 {
            /*if (!entry.isBound() || !(entry.get() instanceof OreBlock ore)) continue;
*///?} else {
            if (!entry.isPresent() || !(entry.get() instanceof OreBlock ore)) continue;
//?}
            ResourceLocation blockId = Mc.blockId(ore);
            if (blockId == null) continue;

            ResourceLocation overlay = resolveOverlayTexture(ore, overlayCache);
            OreBakedModel model = sharedModels.computeIfAbsent(overlay, texture -> new OreBakedModel(template, texture));
            for (BlockState state : ore.getStateDefinition().getPossibleStates()) {
                models.putState(blockId, state, model.stoneModel(OreBlock.stoneOf(state), OreBlock.isBroken(state)));
                states++;
            }
            models.put(blockId, "inventory", model);
            mapped++;
        }
        LOGGER.info("[{}] Ore model replacement: {} ore blocks, {} state models from {} shared models",
                GregTechIdentity.REGISTRY_NAMESPACE, mapped, states, sharedModels.size());
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
                ResourceLocation texture = Mc.gregtechId("block/material_icons/" + set.folder() + "/" + file);
                ResourceLocation filePng = Mc.gregtechId("textures/block/material_icons/" + set.folder() + "/" + file + ".png");
                if (Minecraft.getInstance().getResourceManager().getResource(filePng).isPresent()) {
                    return texture;
                }
            }
            return Mc.gregtechId("block/material_icons/metallic/" + file);
        });
    }
}
