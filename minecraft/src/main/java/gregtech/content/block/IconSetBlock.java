package gregtech.content.block;

import gregtech.content.worldgen.OreHarvest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Simple decorative block that renders its texture from {@code gt6ce:block/iconsets/<iconName>}. */
public class IconSetBlock extends Block {
    private final String iconName;

    public IconSetBlock(BlockBehaviour.Properties properties, String iconName) {
        super(properties);
        this.iconName = iconName;
    }

    public String iconName() {
        return iconName;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        // GT6's ordinary treated planks are BlockBasePlanksFlammable (20/5).
        return iconName.equals("planks_treated") ? 20 : super.getFlammability(state, level, pos, face);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return iconName.equals("planks_treated") ? 5 : super.getFireSpreadSpeed(state, level, pos, face);
    }

//? if >=1.21.1 {
    /*@Override
    public int getExpDrop(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos,
            @Nullable net.minecraft.world.level.block.entity.BlockEntity blockEntity,
            @Nullable net.minecraft.world.entity.Entity breaker, ItemStack tool) {
        OreHarvest harvest = OreHarvest.from(level, tool);
        return onGetExpDrop(state, level, level.getRandom(), pos, harvest.fortune(), harvest.silkTouch() ? 1 : 0);
    }
*///?} else {
    @Override
    public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos,
            int fortuneLevel, int silkTouchLevel) {
        return onGetExpDrop(state, level, random, pos, fortuneLevel, silkTouchLevel);
    }
//?}

    /** Returns experience dropped when this block is mined across 1.20.1 / 1.21.1. */
    protected int onGetExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos,
            int fortuneLevel, int silkTouchLevel) {
        return 0;
    }
}
