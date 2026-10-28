package gregtech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gregtech.content.tool.PortableContainerShapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
*///?} else {
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
//?}
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import java.util.*;

/** Inventory counterpart of CapsuleCellRenderer; shell tint and fluid tint stay independent. */
public final class PortableFluidItemModel extends BakedModelWrapper<BakedModel> {
    private record Visual(ResourceLocation texture, int tint, int amount, int capacity) {}
    private final ItemOverrides overrides;
    private final BakedModel fluidPass;

    public PortableFluidItemModel(BakedModel shell, String shape) {
        super(shell);
        fluidPass = null;
        // Per baked shell/reload, bounded and containing no ItemStack, world or entity references.
        var variants = new LinkedHashMap<Visual, BakedModel>(16, .75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Visual, BakedModel> entry) { return size() > 64; }
        };
        overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel original, ItemStack stack, @Nullable ClientLevel level,
                                                @Nullable LivingEntity entity, int seed) {
                var fluid = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
                if (fluid.isEmpty()) return original;
                var visual = IClientFluidTypeExtensions.of(fluid.getFluid());
                var texture = visual.getStillTexture(fluid);
                if (texture == null) return original;
                int capacity = FluidUtil.getFluidHandler(stack).map(handler -> handler.getTankCapacity(0)).orElse(1);
                capacity = Math.max(1, capacity);
                var key = new Visual(texture, visual.getTintColor(fluid), Math.min(capacity, fluid.getAmount()), capacity);
                return variants.computeIfAbsent(key, value -> {
                    var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(value.texture());
                    return new PortableFluidItemModel(shell, surface(shape, sprite, value));
                });
            }
        };
    }

    private PortableFluidItemModel(BakedModel shell, List<BakedQuad> fluid) {
        super(shell);
        overrides = ItemOverrides.EMPTY;
        fluidPass = new FluidPass(shell, fluid);
    }

    static boolean supports(String shape) {
        return Set.of("cell", "cup", "measuring_pot", "jug").contains(shape);
    }

    private static List<BakedQuad> surface(String shape, TextureAtlasSprite sprite, Visual visual) {
        var result = new ArrayList<BakedQuad>();
        if (shape.equals("cell")) {
            for (int[] bounds : PortableContainerShapes.bounds("cell")) {
                var from = new Vector3f(bounds[0] + .008f, bounds[1], bounds[2] + .008f);
                var to = new Vector3f(bounds[3] - .008f, bounds[4], bounds[5] - .008f);
                for (var side : Direction.Plane.HORIZONTAL) result.add(quad(from, to, side, sprite, visual.tint()));
            }
        } else {
            float min = shape.equals("measuring_pot") ? 5 : 6;
            float bottom = shape.equals("jug") ? 10 : 1;
            float top = shape.equals("jug") ? 14 : shape.equals("cup") ? 5 : 8;
            float y = bottom + (top - bottom - .016f) * visual.amount() / visual.capacity();
            result.add(quad(new Vector3f(min, y, min), new Vector3f(16 - min, y, 16 - min),
                    Direction.UP, sprite, visual.tint()));
        }
        return List.copyOf(result);
    }

    private static BakedQuad quad(Vector3f from, Vector3f to, Direction side, TextureAtlasSprite sprite, int tint) {
        var face = new BlockElementFace(null, -1, "", new BlockFaceUV(new float[]{0, 0, 16, 16}, 0));
//? if >=1.21.1 {
        /*var quad = new FaceBakery().bakeQuad(from, to, face, sprite, side, BlockModelRotation.X0_Y0, null, true);
*///?} else {
        var quad = new FaceBakery().bakeQuad(from, to, face, sprite, side, BlockModelRotation.X0_Y0, null, true,
                ResourceLocation.fromNamespaceAndPath("gt6ce", "portable_fluid_surface"));
//?}
        int alpha = tint >>> 24;
        int abgr = (alpha == 0 ? 0xFF000000 : alpha << 24) | (tint & 255) << 16
                | (tint & 0xFF00) | (tint >>> 16 & 255);
        int[] vertices = quad.getVertices();
        for (int vertex = 0; vertex < 4; vertex++) vertices[vertex * 8 + 3] = abgr;
        return quad;
    }

    @Override public ItemOverrides getOverrides() { return overrides; }
    @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        originalModel.applyTransform(context, pose, leftHand);
        return this;
    }
    @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        var passes = new ArrayList<>(originalModel.getRenderPasses(stack, fabulous));
        if (fluidPass != null) passes.add(fluidPass);
        return List.copyOf(passes);
    }

    /** A separate pass avoids changing the casing's cutout/solid layer or tint-index handling. */
    private static final class FluidPass extends BakedModelWrapper<BakedModel> {
        private final List<BakedQuad> quads;
        private FluidPass(BakedModel shell, List<BakedQuad> quads) { super(shell); this.quads = quads; }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return side == null ? quads : List.of();
        }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                                 ModelData data, @Nullable RenderType layer) {
            return side == null ? quads : List.of();
        }
        @Override public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return List.of(gregtech.GregTechConfig.Rule.GT_ALPHA.enabled()
                    ? RenderType.itemEntityTranslucentCull(InventoryMenu.BLOCK_ATLAS)
                    : RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
    }
}
