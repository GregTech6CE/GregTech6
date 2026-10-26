package gregtech.client;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.content.block.energy.AxleBlock;
import gregtech.content.blockentity.energy.AxleBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?} else {
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
//?}
import javax.annotation.Nullable;
import java.util.*;

/** Original Textures.BlockIcons.AXLES[axis][face][rotation], using original animated sprites. */
public final class AxleBakedModel extends BakedModelWrapper<PipeWireBakedModel> implements IDynamicBakedModel {
    private static final String[][][] TEXTURES={
        {{"horizontal","right","left"},{"horizontal","left","right"},{"","clockwise","counterclockwise"},{"","counterclockwise","clockwise"},{"vertical","down","up"},{"vertical","up","down"}},
        {{"","clockwise","counterclockwise"},{"","counterclockwise","clockwise"},{"horizontal","right","left"},{"horizontal","right","left"},{"horizontal","right","left"},{"horizontal","right","left"}},
        {{"vertical","down","up"},{"vertical","down","up"},{"vertical","up","down"},{"vertical","down","up"},{"","clockwise","counterclockwise"},{"","counterclockwise","clockwise"}}
    };
    private final PipeWireBakedModel[][] variants=new PipeWireBakedModel[3][3];
//? if >=1.21.1 {
    /*public AxleBakedModel(double half){this(half,net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS);}
    public AxleBakedModel(double half,net.minecraft.client.renderer.block.model.ItemTransforms transforms) {
        super(make(half,0,0).itemTransforms(transforms));
        for(int axis=0;axis<3;axis++)for(int rotation=0;rotation<3;rotation++)variants[axis][rotation]=make(half,axis,rotation).itemTransforms(transforms);
*///?} else {
    public AxleBakedModel(double half) {
        super(make(half,0,0));
        for(int axis=0;axis<3;axis++)for(int rotation=0;rotation<3;rotation++)variants[axis][rotation]=make(half,axis,rotation);
//?}
    }
    private static PipeWireBakedModel make(double half,int axis,int rotation) {
        var faces=new EnumMap<Direction,ResourceLocation>(Direction.class);
        for(var face:Direction.values()) {
            String suffix=TEXTURES[axis][face.ordinal()][rotation];
//? if >=1.21.1 {
            /*faces.put(face,ResourceLocation.fromNamespaceAndPath("gt6ce","block/iconsets/axle"+(suffix.isEmpty()?"":"_"+suffix)));
*///?} else {
            faces.put(face,GregTech.id("block/iconsets/axle"+(suffix.isEmpty()?"":"_"+suffix)));
//?}
        }
//? if >=1.21.1 {
        /*var base=ResourceLocation.fromNamespaceAndPath("gt6ce","block/iconsets/axle");
*///?} else {
        var base=GregTech.id("block/iconsets/axle");
//?}
        return new PipeWireBakedModel(half,base,0,base,0,0,null,faces);
    }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state,@Nullable Direction side,RandomSource random,ModelData data,@Nullable RenderType layer) {
        int axis=state==null||state.getValue(AxleBlock.NORTH)||state.getValue(AxleBlock.SOUTH)?0:
                state.getValue(AxleBlock.WEST)||state.getValue(AxleBlock.EAST)?2:1;
        Integer rotation=data.get(AxleBlockEntity.ROTATION);
        return variants[axis][state==null||rotation==null?0:Math.max(0,Math.min(2,rotation))].getQuads(state,side,random,data,layer);
    }
}
