package gregtech.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;

/** GT6 BlockColored metadata palette; colour survives picking, drops and vanilla placement. */
public class ColoredConstructionBlock extends Block {
    public static final EnumProperty<DyeColor> COLOR = ConcreteBlock.COLOR;
    public ColoredConstructionBlock(Properties properties, DyeColor color) {
        super(properties);
        registerDefaultState(stateDefinition.any().hasProperty(COLOR)
                ? stateDefinition.any().setValue(COLOR, color) : stateDefinition.any());
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (!(this instanceof FixedConstructionColor)) builder.add(COLOR);
    }
    public static DyeColor itemColor(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof FixedConstructionColor fixed) return fixed.color();
        DyeColor fallback = stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().hasProperty(COLOR)
                ? item.getBlock().defaultBlockState().getValue(COLOR) : DyeColor.WHITE;
//? if >=1.21.1 {
        /*var data = stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        return data == null ? fallback : DyeColor.byName(data.properties().get("color"), fallback);
*///?} else {
        var data = stack.getTagElement("BlockStateTag");
        return data == null ? fallback : DyeColor.byName(data.getString("color"), fallback);
//?}
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(ConcreteBlock.coloredItem(this, FixedConstructionColor.colorOf(state)));
    }
//? if >=1.21.1 {
    /*@Override public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult hit, net.minecraft.world.level.LevelReader level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
*///?} else {
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
//?}
        return ConcreteBlock.coloredItem(this, FixedConstructionColor.colorOf(state));
    }
}
