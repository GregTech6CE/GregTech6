package gregtech.content.block.wood;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6's beam identity in modern Minecraft: a full stripped log block.
 *
 * <p>GT6 beams ({@code BlocksGT.BeamA/B/C}) were bark-free, full-block pillars; the port keeps that
 * role but uses the modern stripped-log representation so vanilla stripped logs and GT wood share one
 * item family. The old {@code gt6ce:beam_*} ids are aliased to these blocks.</p>
 */
public class WoodStrippedLogBlock extends RotatedPillarBlock {
//? if >=1.21.1 {
    /*@Override public com.mojang.serialization.MapCodec<WoodStrippedLogBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
*///?} else {
//?}
    public WoodStrippedLogBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(AXIS, ctx.getClickedFace().getAxis());
    }
}
