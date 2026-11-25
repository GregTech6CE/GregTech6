package gregtech.content.block;

import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Source MultiTileEntityStick: biome/dimension wood, including dead, mossy and rotten wood. */
public class TwigBlock extends GTBlock {
    /** Right-click picks the twigs up as sticks. */
    @Override
    protected InteractionResult useBlock(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            ItemStack stack = contents(level, pos, 1);
            level.removeBlock(pos, false);
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 2.0, 14.0);
    /** Source MultiTileEntityStickPlaced: hand-placed sticks always hand back one ordinary stick. */
    private final boolean placed;

    public TwigBlock(Properties properties) {
        this(properties, false);
    }

    public TwigBlock(Properties properties, boolean placed) {
        super(properties);
        this.placed = placed;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (placed) return Collections.singletonList(new ItemStack(Items.STICK));
        ItemStack tool =
                builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
        int fortune = tool == null ? 0 : gregtech.engine.stack.EnchantmentCompat.fortune(tool, builder.getLevel());
        var origin = builder.getParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
        return Collections.singletonList(contents(
                builder.getLevel(),
                BlockPos.containing(origin),
                1 + builder.getLevel().random.nextInt(1 + fortune)));
    }

    public ItemStack contents(net.minecraft.world.level.Level level, BlockPos pos, int count) {
        if (placed) return new ItemStack(Items.STICK, count);
        String biome = level.getBiome(pos)
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("");
        // A position has one stable content, shared by pickup, breaking and client inspection.
        var random = net.minecraft.util.RandomSource.create(
                pos.asLong() ^ level.dimension().location().hashCode());
        String name = gregtech.content.worldgen.SurfaceTwigRules.material(
                level.dimension().location().toString(), biome, random::nextInt);
        return stick(name, count);
    }

    /** Candidate pickup items for inspection, without consuming random numbers or changing the block. */
    public static List<ItemStack> possibleSticks(net.minecraft.world.level.Level level, BlockPos pos) {
        String biome = level.getBiome(pos)
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("");
        return gregtech.content.worldgen.SurfaceTwigRules.possibleMaterials(
                        level.dimension().location().toString(), biome)
                .stream()
                .map(name -> stick(name, 1))
                .toList();
    }

    private static ItemStack stick(String name, int count) {
        if (name == null) return new ItemStack(Items.STICK, count);
        var material = gregtech.engine.material.GTMaterialRegistry.get(name);
        // Old source WOODS field spelling vs canonical species name in the port.
        if (!material.isValid() && name.startsWith("Wood"))
            material = gregtech.engine.material.GTMaterialRegistry.get(name.substring(4));
        if (!material.isValid()) throw new IllegalStateException("Missing source twig material " + name);
        var result =
                gregtech.content.registry.GTItems.getStack(gregtech.content.data.MaterialPrefix.stick, material, count);
        if (result.isEmpty()) throw new IllegalStateException("Missing source twig item " + name);
        return result;
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbour,
            net.minecraft.world.level.LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos) {
        if (!canSurvive(state, level, pos)
                || side != Direction.DOWN && !neighbour.getFluidState().isEmpty())
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, side, neighbour, level, pos, neighbourPos);
    }
}
