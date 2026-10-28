package gregtech.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.List;

/** Source MultiTileEntityStick: biome/dimension wood, including dead, mossy and rotten wood. */
public class TwigBlock extends Block {
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
        ItemStack tool = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
        int fortune = tool == null ? 0 : net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
//? if >=1.21.1 {
                /*builder.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), tool);
*///?} else {
                net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, tool);
//?}
        var origin = builder.getParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
        return Collections.singletonList(stick(builder.getLevel(), BlockPos.containing(origin),
                1 + builder.getLevel().random.nextInt(1 + fortune)));
    }

    private static ItemStack stick(net.minecraft.world.level.Level level, BlockPos pos, int count) {
        String biome = level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("");
        String name = gregtech.content.worldgen.SurfaceTwigRules.material(
                level.dimension().location().toString(), biome, level.random::nextInt);
        return stick(name, count);
    }

    /** Candidate pickup items for inspection, without consuming random numbers or changing the block. */
    public static List<ItemStack> possibleSticks(net.minecraft.world.level.Level level, BlockPos pos) {
        String biome = level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("");
        return gregtech.content.worldgen.SurfaceTwigRules.possibleMaterials(
                level.dimension().location().toString(), biome).stream().map(name -> stick(name, 1)).toList();
    }

    private static ItemStack stick(String name, int count) {
        if (name == null) return new ItemStack(Items.STICK, count);
        var material = gregtech.engine.material.GTMaterialRegistry.get(name);
        // Old source WOODS field spelling vs canonical species name in the port.
        if (!material.isValid() && name.startsWith("Wood"))
            material = gregtech.engine.material.GTMaterialRegistry.get(name.substring(4));
        if (!material.isValid()) throw new IllegalStateException("Missing source twig material " + name);
        var result = gregtech.content.registry.GTItems.getStack(
                gregtech.content.data.MaterialPrefix.stick, material, count);
        if (result.isEmpty()) throw new IllegalStateException("Missing source twig item " + name);
        return result;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbour,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (!canSurvive(state,level,pos) || side != Direction.DOWN && !neighbour.getFluidState().isEmpty())
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state,side,neighbour,level,pos,neighbourPos);
    }

    /** Right-click picks the twigs up as sticks. */
    @Override
//? if >=1.21.1 {
    /*public net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level,
            BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.phys.BlockHitResult hit) {
*///?} else {
    public net.minecraft.world.InteractionResult use(BlockState state, net.minecraft.world.level.Level level,
                                                     BlockPos pos, net.minecraft.world.entity.player.Player player,
                                                     net.minecraft.world.InteractionHand hand,
                                                     net.minecraft.world.phys.BlockHitResult hit) {
//?}
        if (!level.isClientSide) {
            ItemStack stack = placed ? new ItemStack(Items.STICK) : stick(level, pos, 1);
            level.removeBlock(pos, false);
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
}
