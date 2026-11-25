package gregtech.content.block.plant;

import gregtech.content.block.IconSetPlantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

//? if >=1.21.1 {
//?} else {
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.common.PlantType;
//?}

import java.util.List;

/** Both families use ordinary flower soil. GT6's desert family also grows on sand. */
public final class BedrockFlowerBlock extends IconSetPlantBlock {
    private final boolean desert;

    public BedrockFlowerBlock(Properties properties, String iconName, boolean desert) {
        super(properties, iconName);
        this.desert = desert;
    }

    public boolean desert() { return desert; }

    @Override
//? if >=1.21.1 {
//?} else {
    public PlantType getPlantType(BlockGetter level, BlockPos pos) {
        return desert ? PlantType.DESERT : PlantType.PLAINS;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Forge's desert plant type alone rejects ordinary soil before the flower's soil check.
        BlockPos soil = pos.below();
        return (desert && mayPlaceOn(level.getBlockState(soil), level, soil)) || super.canSurvive(state, level, pos);
    }

    @Override
//?}
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return (desert && state.is(BlockTags.SAND)) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }
}
