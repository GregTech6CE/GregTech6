package gregtech.client;

import gregtech.content.block.machine.MoldItemData;
import gregtech.engine.machine.crucible.MoldShapes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?} else {
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
//?}
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import java.util.*;

/** The same 5x5 cavity grid as the world mold, selected from the native block-item shape data. */
public final class MoldItemBakedModel extends BakedModelWrapper<BakedModel> {
    private final BakedModel gridPass;
    private final ItemOverrides overrides;

    public MoldItemBakedModel(BakedModel hull, TextureAtlasSprite sprite) { this(hull,sprite,0,false); }

    private MoldItemBakedModel(BakedModel hull,TextureAtlasSprite sprite,int shape,boolean fixed) {
        super(hull);
        gridPass = gridPass(hull, Objects.requireNonNull(sprite,"Baked mold grid sprite"), shape);
        var variants=new LinkedHashMap<Integer,BakedModel>(16,0.75f,true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Integer,BakedModel> entry) {return size()>32;}
        };
        overrides=fixed ? ItemOverrides.EMPTY : new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel original,ItemStack stack,@Nullable ClientLevel level,
                                                @Nullable LivingEntity entity,int seed) {
                int shape=MoldItemData.shape(stack)&MoldShapes.SHAPE_MASK;
                if(shape==0)return original;
                return variants.computeIfAbsent(shape,key->new MoldItemBakedModel(hull,sprite,key,true));
            }
        };
    }

    // Borrow the sprite from this reload's baked template; ModelManager's atlas is not ready here.
    static TextureAtlasSprite gridSprite(BakedModel template) {
        var random=RandomSource.create(0);
        var quads=new ArrayList<>(template.getQuads(null,null,random));
        for(var side:Direction.values()) quads.addAll(template.getQuads(null,side,random));
        for(var quad:quads) if(quad.getTintIndex()==0) return quad.getSprite();
        return template.getParticleIcon();
    }

    @Nullable
    private static BakedModel gridPass(BakedModel hull,TextureAtlasSprite sprite,int shape) {
        if((shape&MoldShapes.SHAPE_MASK)==MoldGridGeometry.ALL_CAVITIES) return null;
        var bakery=new FaceBakery();
        var bySide=new EnumMap<Direction,List<BakedQuad>>(Direction.class);
        // Existing world mold geometry: 12px square, 2.4px cells raised from 1px to 3px above the plate.
        MoldGridGeometry.forEachVisibleFace(shape,(side,x0,z0,x1,z1,u0,v0,u1,v1)->{
            var from=new Vector3f(x0,MoldGridGeometry.BOTTOM,z0);
            var to=new Vector3f(x1,MoldGridGeometry.TOP,z1);
            var face=new BlockElementFace(null,0,"",new BlockFaceUV(new float[]{u0,v0,u1,v1},0));
//? if >=1.21.1 {
            /*var quad=bakery.bakeQuad(from,to,face,sprite,side,BlockModelRotation.X0_Y0,null,true);
*///?} else {
            var quad=bakery.bakeQuad(from,to,face,sprite,side,BlockModelRotation.X0_Y0,null,true,
                    ResourceLocation.fromNamespaceAndPath("gt6ce","mold_item_grid"));
//?}
            bySide.computeIfAbsent(side,key->new ArrayList<>()).add(quad);
        });
        if(bySide.isEmpty()) return null;
        return new GridPass(hull,bySide);
    }

    @Override public ItemOverrides getOverrides(){return overrides;}
    /** BakedModelWrapper otherwise returns the hull and drops the grid before item rendering. */
    @Override public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context,
                                               com.mojang.blaze3d.vertex.PoseStack pose, boolean leftHand) {
        originalModel.applyTransform(context,pose,leftHand);
        return this;
    }
    /** Keep the hull's own passes (layer render types and transforms) and append the shape grid as its own pass. */
    @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        var passes=new ArrayList<>(originalModel.getRenderPasses(stack,fabulous));
        if(gridPass!=null) passes.add(gridPass);
        return List.copyOf(passes);
    }

    /** The cavity grid holds no hull quads, so hull render types, lighting and transforms stay untouched. */
    private static final class GridPass extends BakedModelWrapper<BakedModel> {
        private final Map<Direction,List<BakedQuad>> quads;
        private GridPass(BakedModel hull,Map<Direction,List<BakedQuad>> quads) {
            super(hull);
            var copy=new EnumMap<Direction,List<BakedQuad>>(Direction.class);
            quads.forEach((side,list)->copy.put(side,List.copyOf(list)));
            this.quads=copy;
        }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state,@Nullable Direction side,RandomSource random) {
            return side==null ? List.of() : quads.getOrDefault(side,List.of());
        }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state,@Nullable Direction side,RandomSource random,
                                                 ModelData data,@Nullable net.minecraft.client.renderer.RenderType layer) {
            return side==null ? List.of() : quads.getOrDefault(side,List.of());
        }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
    }
}
