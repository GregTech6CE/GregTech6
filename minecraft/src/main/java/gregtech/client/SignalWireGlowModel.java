package gregtech.client;

import gregtech.content.block.energy.SignalWireBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** GT6 draws powered redstone wires at full brightness without emitting world light. */
final class SignalWireGlowModel {
    private SignalWireGlowModel() {}

    static List<BakedQuad> apply(@Nullable BlockState state, List<BakedQuad> quads) {
        return lit(state) ? brighten(quads) : quads;
    }

    static boolean lit(@Nullable BlockState state) {
        return state != null
                && state.getBlock() instanceof SignalWireBlock
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
        return BakedQuadCompat.withAmbientOcclusion(
                vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), false, false);
    }
}
