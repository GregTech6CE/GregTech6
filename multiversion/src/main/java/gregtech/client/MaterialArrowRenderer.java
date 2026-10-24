package gregtech.client;

import gregtech.entity.MaterialArrowEntity;
import gregtech.registry.GTProjectiles;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/** GT6 uses the ordinary arrow mesh with its own arrow texture. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class MaterialArrowRenderer extends ArrowRenderer<MaterialArrowEntity> {
//? if >=1.21.1 {
    /*private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("gt6ce", "textures/entity/arrow.png");
*///?} else {
    private static final ResourceLocation TEXTURE = new ResourceLocation("gt6ce", "textures/entity/arrow.png");
//?}
    public MaterialArrowRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override
    public ResourceLocation getTextureLocation(MaterialArrowEntity arrow) { return TEXTURE; }
    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GTProjectiles.MATERIAL_ARROW.get(), MaterialArrowRenderer::new);
    }
}
