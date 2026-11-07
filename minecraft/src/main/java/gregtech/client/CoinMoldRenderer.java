package gregtech.client;
import net.minecraft.resources.ResourceLocation;
import gregtech.engine.mod.GregTechIdentity;

import gregtech.content.blockentity.tool.CoinMoldBlockEntity;
import gregtech.content.tool.CoinMoldGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlas;

/** GT6 coin mold material insert (pass 2): present for both the plate and the stamped coin. */
public final class CoinMoldRenderer implements BlockEntityRenderer<CoinMoldBlockEntity> {

    public CoinMoldRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CoinMoldBlockEntity mold, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        var material = mold.displayedMaterial();
        if (material == null || !material.isValid()) return;
//? if >=1.21.1 {
        /*var texture = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",
                "block/material_icons/"+MaterialIcons.resolveTextureSet(material).folder()+"/blocksolid");
*///?} else {
        var texture = ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, ("block/material_icons/"+MaterialIcons.resolveTextureSet(material).folder()+"/blocksolid").toLowerCase());
//?}
        var out = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        AnvilCuboidRenderer.draw(pose.last(), out, CoinMoldGeometry.CONTENT, CrucibleRenderSprites.resolve(texture), material.getColor(), light);
//? if >=1.21.1 {
        /*var detail = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", texture.getPath()+"_overlay");
*///?} else {
        var detail = ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, (texture.getPath()+"_overlay").toLowerCase());
//?}
        if (CrucibleRenderSprites.hasTexture(detail))
            AnvilCuboidRenderer.draw(pose.last(), out, CoinMoldGeometry.CONTENT, CrucibleRenderSprites.resolve(detail), 0xFFFFFF, light);
    }
}
