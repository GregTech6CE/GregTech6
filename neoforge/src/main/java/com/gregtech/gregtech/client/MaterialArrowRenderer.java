package com.gregtech.gregtech.client;

import com.gregtech.gregtech.entity.MaterialArrowEntity;
import com.gregtech.gregtech.registry.GTProjectiles;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** GT6 uses the ordinary arrow mesh with its own arrow texture. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MaterialArrowRenderer extends ArrowRenderer<MaterialArrowEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("gregtech", "textures/entity/arrow.png");
    public MaterialArrowRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override
    public ResourceLocation getTextureLocation(MaterialArrowEntity arrow) { return TEXTURE; }
    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GTProjectiles.MATERIAL_ARROW.get(), MaterialArrowRenderer::new);
    }
}
