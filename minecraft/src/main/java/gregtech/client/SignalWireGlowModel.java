package gregtech.client;

import gregtech.content.block.energy.SignalWireBlock;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** GT6 draws a powered redstone wire with a glowing texture, independent of its light value. */
//? if >=1.21.1 {
/*public final class SignalWireGlowModel extends net.neoforged.neoforge.client.model.BakedModelWrapper<BakedModel> {
*///?} else {
public final class SignalWireGlowModel extends net.minecraftforge.client.model.BakedModelWrapper<BakedModel> {
//?}
    public SignalWireGlowModel(BakedModel inner) {
        super(inner);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return lit(state) ? brighten(super.getQuads(state, side, random)) : super.getQuads(state, side, random);
    }

    private static boolean lit(@Nullable BlockState state) {
        return state != null && state.hasProperty(SignalWireBlock.POWER)
                && state.getValue(SignalWireBlock.POWER) > 0;
    }

    private static List<BakedQuad> brighten(List<BakedQuad> source) {
        if (source.isEmpty()) return source;
        List<BakedQuad> out = new ArrayList<>(source.size());
        for (BakedQuad quad : source) out.add(fullBright(quad));
        return out;
    }

    private static BakedQuad fullBright(BakedQuad quad) {
        int[] vertices = quad.getVertices().clone();
        for (int i = 0; i < 4; i++) vertices[i * 8 + 6] = LightTexture.FULL_BRIGHT;
//? if >=1.21.1 {
        /*return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
*///?} else {
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
//?}
    }
}
