package gregtech.content.block.misc;

import gregtech.content.block.GTBlock;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

/** GT6 BlockColored metadata palette; colour survives picking, drops and vanilla placement. */
public class ColoredConstructionBlock extends GTBlock {
    public static final EnumProperty<DyeColor> COLOR = ConcreteBlock.COLOR;

    public ColoredConstructionBlock(Properties properties, DyeColor color) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().hasProperty(COLOR)
                        ? stateDefinition.any().setValue(COLOR, color)
                        : stateDefinition.any());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (!(this instanceof FixedConstructionColor)) builder.add(COLOR);
    }

    public static DyeColor itemColor(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof FixedConstructionColor fixed)
            return fixed.color();
        DyeColor fallback = stack.getItem() instanceof BlockItem item
                        && item.getBlock().defaultBlockState().hasProperty(COLOR)
                ? item.getBlock().defaultBlockState().getValue(COLOR)
                : DyeColor.WHITE;
        return DyeColor.byName(gregtech.engine.stack.BlockStateData.getProperty(stack, "color"), fallback);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(ConcreteBlock.coloredItem(this, FixedConstructionColor.colorOf(state)));
    }

    @Override
    protected ItemStack onGetCloneItemStack(
            BlockState state, @Nullable HitResult target, BlockGetter level, BlockPos pos, @Nullable Player player) {
        return ConcreteBlock.coloredItem(this, FixedConstructionColor.colorOf(state));
    }
}
