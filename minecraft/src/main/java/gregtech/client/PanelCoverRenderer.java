package gregtech.client;

import gregtech.content.cover.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Original GT6 sprites, with the exact same face mapping used by the server's buttons. */
public final class PanelCoverRenderer {
    private static final String[] BUTTON_STYLES={"underlay","underlay_0_to_15","underlay_0_to_f","underlay_1_to_16","underlay_16_1_to_15","underlay_keypad_1_to_9","underlay_keypad_9_to_1","underlay_bits"};
    private PanelCoverRenderer(){}
    /** Layer identities only; original per-layer glow flags are not represented here. */
    public static List<String> layers(ItemStack stack) {
        var panel = PanelCover.of(stack);
        var component = ComponentCoverRuntime.kind(stack);
        if (component != null)
            return List.of(ComponentCoverRules.texture(component, ComponentCoverRuntime.visual(stack)));
        if (panel == null) {
            var id = CoverItems.behavior(stack);
            int count = CoverUtilityBehaviors.designCount(id);
            if (count > 0)
                return List.of((CoverUtilityBehaviors.BLANK_COVER.equals(id) ? "blank/" : "warning/")
                        + Math.floorMod(CoverUtilityBehaviors.design(stack), count));
            return List.of();
        }
        int value = PanelCoverRuntime.value(stack), style = PanelCoverRuntime.style(stack);
        return switch (panel) {
            case MANUAL -> List.of("manualselector/underlay", "manualselector/" + (value & 15));
            case REDSTONE -> List.of("redstoneselector/underlay", "redstoneselector/" + (value & 15));
            case BUTTONS -> List.of("buttonselector/" + BUTTON_STYLES[Math.floorMod(style, 8)],
                    "buttonselector/" + (value & 15));
            case EMITTER -> List.of("redstoneemitter/underlay", "redstoneemitter/" + (value & 15));
            case ENERGY_DISPLAY -> List.of("energydisplay/underlay", "energydisplay/" + Math.max(0, Math.min(10, value)));
            case STATUS -> {
                String folder = "statusdisplay/" + (Math.floorMod(style, 2) == 0 ? "bottom/" : "top/");
                var layers = new ArrayList<String>();
                layers.add(folder + "base");
                for (int i = 0; i < 4; i++)
                    if ((value & (32 << i)) != 0)
                        layers.add(folder + (i + 1) + ((value & (1 << i)) != 0 ? "_on" : "_off"));
                yield layers;
            }
            case PROGRESS -> List.of("progressredstone/circuit");
            case ENERGY -> List.of("energyredstone/circuit");
            case CONDUCTOR_IN -> List.of("redstoneconductor/in");
            case CONDUCTOR_OUT -> List.of("redstoneconductor/out");
            case CONTROLLER -> List.of("coverswitch/base", "coverswitch/circuit");
            case SHUTTER -> List.of("shutter/" + (MachineCoverSpec.inverted(stack) ? "inverted" : "normal"));
        };
    }

    public static boolean renderFace(PanelCoverHost host,Direction side,PoseStack pose,MultiBufferSource buffers,int fallbackLight){
        var stack=host.getCover(side);if(!CoverItems.isCover(stack))return false;
        var owner=host.coverOwner();var level=owner.getLevel();
        int light=CoverSurfaceRenderer.light(owner,side,fallbackLight);
        var behavior=CoverItems.behavior(stack);
        if((CoverItems.REDSTONE_TORCH.equals(behavior)||CoverItems.REDSTONE_REPEATER.equals(behavior))
                &&host.coverOwner() instanceof gregtech.content.blockentity.energy.SignalWireBlockEntity wire) {
            boolean torch=CoverItems.REDSTONE_TORCH.equals(behavior);
            // Network signal is server-only; POWER is the synchronized wire state.
            boolean powered=wire.getBlockState().getValue(gregtech.content.block.energy.SignalWireBlock.POWER)>0;
            boolean lit=torch?!powered:powered;
            String path="block/machines/covers/"+(torch?"redstonetorch/":"redstonerepeater/")+(lit?"on/":"off/");
            CoverSurfaceRenderer.torch(pose,buffers.getBuffer(RenderType.cutout()),side,
                    ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce",path+"side")),
                    ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce",path+"front")),light);
            return true;
        }
        if(CoverUtilityBehaviors.PRESSURE_VALVE.equals(behavior)){
            // Original CoverPressureValve renders a recessed knob, not the item icon as a face decal.
            String base="block/machines/covers/pressurevalve/";
            CoverSurfaceRenderer.valve(pose,buffers.getBuffer(RenderType.cutout()),side,
                    ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce",base+"front")),
                    ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce",base+"side")),light);
            return true;
        }
        if(CanvasData.isCanvas(stack)) {
            var canvas=(gregtech.content.item.CanvasItem)stack.getItem();
            CoverSurfaceRenderer.attachment(owner,side,pose,buffers.getBuffer(RenderType.cutout()),
                    ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce","block/machines/covers/canvas")),light,canvas.variant().rgb());
            CanvasCoverRenderer.render(stack,side,pose,buffers,light,level,owner.getBlockPos());return true;
        }
        if(stack.getItem() instanceof gregtech.content.item.PanelItemView panel) {
            var spec=panel.panelSpec();var vertices=buffers.getBuffer(RenderType.cutout());
            CoverSurfaceRenderer.attachment(host.coverOwner(),side,pose,vertices,ArmRenderHelper.getSprite(ResourceLocation.parse(spec.texture())),light,spec.tint());
            CoverSurfaceRenderer.draw(pose,vertices,side,ArmRenderHelper.getSprite(ResourceLocation.parse(spec.texture())),light,0,spec.tint());return true;
        }
        if(stack.getItem() instanceof gregtech.engine.material.MaterialFormItem material){
            var stone=MaterialCoverRules.stoneTextures(material.getPrefix().getName(),material.getMaterial().getName());
            if(!stone.isEmpty()){
                var vertices=buffers.getBuffer(RenderType.cutout());
                var sprite=ArmRenderHelper.getSprite(ResourceLocation.parse(stone.get(Math.floorMod(CoverUtilityBehaviors.design(stack),stone.size()))));
                CoverSurfaceRenderer.attachment(host.coverOwner(),side,pose,vertices,sprite,light,-1);
                CoverSurfaceRenderer.draw(pose,vertices,side,sprite,light,0);return true;
            }
            var textures=MaterialCoverRules.textures(material.getPrefix().getName());
            if(!textures.isEmpty()){
                String file=textures.get(Math.floorMod(CoverUtilityBehaviors.design(stack),textures.size()));
                var set=MaterialIcons.resolveTextureSet(material.getMaterial());
                String path="block/material_icons/"+set.folder()+"/"+file;
                var vertices=buffers.getBuffer(RenderType.cutout());
                CoverSurfaceRenderer.attachment(host.coverOwner(),side,pose,vertices,ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce",path)),light,material.getMaterial().getColor());
                CoverSurfaceRenderer.draw(pose,vertices,side,ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce",path)),light,0,material.getMaterial().getColor());
                CoverSurfaceRenderer.draw(pose,vertices,side,ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce",path+"_overlay")),light,1);
                if(CoverSurfaceRenderer.needsAttachment(host.coverOwner(),side))
                    CoverSurfaceRenderer.drawBack(pose,vertices,side,ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce",path+"_overlay")),light,1,-1);
                return true;
            }
        }
        if(PanelCover.of(stack)==null&&ComponentCoverRuntime.kind(stack)==null&&CoverUtilityBehaviors.designCount(CoverItems.behavior(stack))==0){
            var model=net.minecraft.client.Minecraft.getInstance().getItemRenderer().getModel(stack,host.coverOwner().getLevel(),null,0);
            var vertices=buffers.getBuffer(RenderType.cutout());
            CoverSurfaceRenderer.attachment(host.coverOwner(),side,pose,vertices,model.getParticleIcon(),light,net.minecraft.client.Minecraft.getInstance().getItemColors().getColor(stack,0));
            CoverSurfaceRenderer.draw(pose,vertices,side,model.getParticleIcon(),light,0,net.minecraft.client.Minecraft.getInstance().getItemColors().getColor(stack,0));
            return true;
        }
        var vc=buffers.getBuffer(RenderType.cutout());var matrix=pose.last().pose();
        var base=ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce","block/machines/covers/base"));
        // Source surface art is transparent on full machine faces. Only a freestanding
        // attachment (pipe/wire) needs the backing plate and holder.
        if (CoverSurfaceRenderer.needsAttachment(owner,side)) {
            CoverSurfaceRenderer.attachment(owner,side,pose,vc,base,light,-1);
            CoverSurfaceRenderer.draw(pose,vc,side,base,light,0);
        }
        var component=ComponentCoverRuntime.kind(stack);
        if(component!=null && CoverSurfaceRenderer.needsAttachment(owner,side)) {
            var inward=ComponentCoverRules.texture(component,ComponentCoverRuntime.visual(stack)==0?1:0);
            CoverSurfaceRenderer.drawBack(pose,vc,side,ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce","block/machines/covers/"+inward)),light,1,-1);
        }
        int layer=1;
        for(String texture:layers(stack)){
            var sprite=ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce","block/machines/covers/"+texture));
            CoverSurfaceRenderer.draw(pose,vc,side,sprite,light,layer++);
        }
        return true;
    }
}
