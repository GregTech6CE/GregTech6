package gregtech.content.block.misc;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** A color belongs to the registered block identity; new variants have no color state or item tag. */
public interface FixedConstructionColor {
    DyeColor color();
    String family();

    static DyeColor colorOf(BlockState state) {
        return state.getBlock() instanceof FixedConstructionColor fixed ? fixed.color()
                : state.hasProperty(ConcreteBlock.COLOR) ? state.getValue(ConcreteBlock.COLOR) : DyeColor.LIGHT_GRAY;
    }

    static BlockState recolor(BlockState state, DyeColor color) {
        Block block = gregtech.content.registry.GTConstructionVariants.coloredBlock(state.getBlock(), color);
        if (block == null) return state.hasProperty(ConcreteBlock.COLOR) ? state.setValue(ConcreteBlock.COLOR, color) : state;
        BlockState result = block.defaultBlockState();
        for (var property : state.getProperties())
            result = copy(result, state, property);
        return result;
    }

    static boolean recolor(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, DyeColor color) {
        BlockState state = level.getBlockState(pos);
        BlockState next = recolor(state, color);
        if (state == next) return false;
        long age = level.getBlockEntity(pos) instanceof gregtech.content.blockentity.CFoamBlockEntity foam
                ? foam.timer() : 0;
        if (!level.setBlock(pos, next, 3)) return false;
        if (level.getBlockEntity(pos) instanceof gregtech.content.blockentity.CFoamBlockEntity foam)
            foam.restoreTimer(age);
        return true;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState target, BlockState source, Property<T> property) {
        return target.hasProperty(property) ? target.setValue(property, source.getValue(property)) : target;
    }
}
