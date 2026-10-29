package gregtech.client;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.block.OreBlock;
import gregtech.content.registry.GTBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
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
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class OreClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private OreClientModels() {}

    private static final ResourceLocation TEMPLATE = ResourceLocation.withDefaultNamespace("block/stone");

    @SubscribeEvent
    public static void registerTemplate(ModelEvent.RegisterAdditional event) {
//? if >=1.21.1 {
        /*event.register(ModelResourceLocation.standalone(TEMPLATE));
*///?} else {
        event.register(TEMPLATE);
//?}
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
//? if >=1.21.1 {
        /*Map<ModelResourceLocation, BakedModel> models = event.getModels();
*///?} else {
        Map<ResourceLocation, BakedModel> models = event.getModels();
//?}
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        BakedModel template = BakedModelLookup.find(models, TEMPLATE);
        if (template == null) {
//? if >=1.21.1 {
            /*LOGGER.error("[{}] vanilla stone model missing — ore models not replaced", "gt6ce");
*///?} else {
            LOGGER.error("[{}] vanilla stone model missing — ore models not replaced", GregTech.NAMESPACE);
//?}
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
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(ore);
*///?} else {
            if (!entry.isPresent() || !(entry.get() instanceof OreBlock ore)) continue;
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(ore);
//?}
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
//? if >=1.21.1 {
                /*"gt6ce", mapped, states, sharedModels.size());
*///?} else {
                GregTech.NAMESPACE, mapped, states, sharedModels.size());
//?}
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
//? if >=1.21.1 {
                /*ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("gt6ce",
*///?} else {
                ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE,
//?}
                        "block/material_icons/" + set.folder() + "/" + file);
//? if >=1.21.1 {
                /*ResourceLocation file_png = ResourceLocation.fromNamespaceAndPath("gt6ce",
*///?} else {
                ResourceLocation file_png = ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE,
//?}
                        "textures/block/material_icons/" + set.folder() + "/" + file + ".png");
                if (Minecraft.getInstance().getResourceManager().getResource(file_png).isPresent()) {
                    return texture;
                }
            }
//? if >=1.21.1 {
            /*return ResourceLocation.fromNamespaceAndPath("gt6ce", "block/material_icons/metallic/" + file);
*///?} else {
            return ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, "block/material_icons/metallic/" + file);
//?}
        });
    }
}
