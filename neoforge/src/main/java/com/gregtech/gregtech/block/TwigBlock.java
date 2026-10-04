package com.gregtech.gregtech.block;

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

    public TwigBlock(Properties properties) {
        super(properties);
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
        ItemStack tool = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
        int fortune = tool == null ? 0 : net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                builder.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), tool);
        var origin = builder.getParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
        return Collections.singletonList(stick(builder.getLevel(), BlockPos.containing(origin),
                1 + builder.getLevel().random.nextInt(1 + fortune)));
    }

    private static ItemStack stick(net.minecraft.world.level.Level level, BlockPos pos, int count) {
        String biome = level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("");
        String name = com.gregtech.gregtech.worldgen.SurfaceTwigRules.material(
                level.dimension().location().toString(), biome, level.random::nextInt);
        if (name == null) return new ItemStack(Items.STICK, count);
        var material = com.gregtech.gregtech.api.material.GTMaterialRegistry.get(name);
        // Old source WOODS field spelling vs canonical species name in the port.
        if (!material.isValid() && name.startsWith("Wood"))
            material = com.gregtech.gregtech.api.material.GTMaterialRegistry.get(name.substring(4));
        if (!material.isValid()) throw new IllegalStateException("Missing source twig material " + name);
        var result = com.gregtech.gregtech.registry.GTItems.getStack(
                com.gregtech.gregtech.data.MaterialPrefix.stick, material, count);
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
    public net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level,
            BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide) {
            ItemStack stack = stick(level, pos, 1);
            level.removeBlock(pos, false);
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
}
