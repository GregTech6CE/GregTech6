package gregtech.client;
import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.phys.AABB;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
*///?} else {
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
//?}
/** GT6 bounds: rods are 4x16x4 pixels, from y=1 to y=17; the four centres are (4,4), (4,12), (12,4), (12,12). */
public final class ReactorRodRenderer<T extends ReactorCoreBlockEntity> implements BlockEntityRenderer<T> {
    // Original 1x1/2x2 coolant pass 10 fills the interior whenever cold coolant is present.
    private static final AABB COOLANT_BOUNDS = new AABB(.125 + .005, .125, .125 + .005,
            .875 - .005, .875, .875 - .005);
    private final net.minecraft.client.renderer.entity.ItemRenderer items;
    public ReactorRodRenderer(BlockEntityRendererProvider.Context context){items=context.getItemRenderer();}
    @Override public void render(T core,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        for(int i=0;i<core.rods.length;i++)if(!core.rods[i].isEmpty()){
            pose.pushPose();pose.translate(core.rods.length==1?.5:.25+(i/2)*.5,.5625,core.rods.length==1?.5:.25+(i%2)*.5);
            items.renderStatic(core.rods[i],net.minecraft.world.item.ItemDisplayContext.NONE,light,overlay,pose,buffers,core.getLevel(),i);pose.popPose();
        }
        var coolant = core.input.getFluid();
        if (coolant.isEmpty()) return;
        var visual = IClientFluidTypeExtensions.of(coolant.getFluid());
        var texture = visual.getStillTexture(coolant);
        if (texture == null) return;
        int emission = Math.max(0, Math.min(15, coolant.getFluid().getFluidType().getLightLevel(coolant)));
        int fluidLight = LightTexture.pack(Math.max(LightTexture.block(light), emission), LightTexture.sky(light));
        // GT6 BlockTextureFluid uses an opaque tint by default; all six faces show through casing slots.
        AnvilCuboidRenderer.draw(pose.last(), buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)),
                COOLANT_BOUNDS, CrucibleRenderSprites.resolve(texture), visual.getTintColor(coolant), fluidLight);
    }
}
