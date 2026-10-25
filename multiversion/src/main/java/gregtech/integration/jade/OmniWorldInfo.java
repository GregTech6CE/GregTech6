package gregtech.integration.jade;

import gregtech.content.blockentity.*;
import gregtech.content.block.plant.BushBlock;
import gregtech.content.block.wood.TreeHoleBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Reads only the inspected plant/source and its immediate growth environment. */
final class OmniWorldInfo {
    private OmniWorldInfo() {}

    static void append(JadeInspection info, BlockEntity entity) {
        if (entity instanceof BushBlockEntity bush) {
            double progress = bush.stage() >= 3 ? 100 : (bush.stage() * 256 + bush.growth()) * 100.0 / 768;
            info.line("bush_growth", String.format(java.util.Locale.ROOT, "%.1f", progress));
            if (bush.stage() >= 3) info.line("ripe");
            var berry = bush.berryStack(1);
            if (!berry.isEmpty()) info.line("harvest", berry, 1, 2);
            var level = bush.getLevel();
            var state = bush.getBlockState();
            var pos = bush.getBlockPos();
            Direction support = BushBlock.isCore(state) ? null : Direction.from3DDataValue(state.getValue(BushBlock.SUPPORT));
            var sample = pos.offset(support == null ? 0 : support.getStepX(),
                    support == Direction.UP ? 1 : 2, support == null ? 0 : support.getStepZ());
            // A tooltip must not load a neighbouring chunk or call grow/refreshSupport.
            if (level != null && level.hasChunkAt(sample)) {
                boolean sky = level.canSeeSky(pos.above());
                info.flag("growth_light", sky || level.getMaxLocalRawBrightness(sample) > BushBlockEntity.LIGHT_GATE);
                info.flag("growth_rain", sky && level.isRainingAt(sample));
            }
        } else if (entity instanceof TreeHoleBlockEntity tree && tree.getBlockState().hasProperty(TreeHoleBlock.RESIN)) {
            info.line("state", JadeInspection.label(tree.getBlockState().getValue(TreeHoleBlock.RESIN) ? "sap_ready" : "sap_waiting"));
        } else if (entity instanceof SapBagBlockEntity bag) {
            info.item("collected", 0, bag.stored());
            var tank = bag.tank();
            info.fluid("collected", 0, tank.getFluid(), tank.getAmount(), tank.getCapacity());
        } else if (entity instanceof FluidSpringBlockEntity spring) {
            var fluid = spring.springFluid();
            if (!fluid.isEmpty()) info.line("spring_fluid", fluid);
            info.flag("spring_active", spring.active());
            // This implementation rolls a source-block placement. It has no measurable L/t output.
            info.line("spring_interval", FluidSpringBlockEntity.expectedTicks(spring.amount()));
        }
    }
}
