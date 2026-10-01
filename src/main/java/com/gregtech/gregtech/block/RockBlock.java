package com.gregtech.gregtech.block;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.blockentity.RockBlockEntity;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Ground pebble (GT6 surface rock) — drops/picks up the {@code rockGt} item of its
 * material. The {@code ground} state matches the supporting block's look (sand rocks
 * on sand, deepslate rocks on deepslate, ...); the blockstate file holds several
 * position-randomized shape variants per ground.
 */
public class RockBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = box(3.0, 0.0, 3.0, 13.0, 4.0, 13.0);

    /** Visual family of the block the rock is lying on. */
    public enum Ground implements StringRepresentable {
        STONE("stone"), SAND("sand"), RED_SAND("red_sand"), GRAVEL("gravel"),
        DEEPSLATE("deepslate"), GRANITE("granite"), DIORITE("diorite"), ANDESITE("andesite"),
        NETHERRACK("netherrack"), END_STONE("end_stone");

        private final String name;

        Ground(String name) { this.name = name; }

        @Override
        public String getSerializedName() { return name; }

        /** Visual family for an arbitrary supporting block state. */
        public static Ground of(BlockState below) {
            if (below.is(Blocks.SAND) || below.is(Blocks.SANDSTONE)) return SAND;
            if (below.is(Blocks.RED_SAND) || below.is(Blocks.RED_SANDSTONE)) return RED_SAND;
            if (below.is(Blocks.GRAVEL)) return GRAVEL;
            if (below.is(Blocks.DEEPSLATE) || below.is(Blocks.TUFF) || below.is(Blocks.COBBLED_DEEPSLATE)) return DEEPSLATE;
            if (below.is(Blocks.GRANITE)) return GRANITE;
            if (below.is(Blocks.DIORITE)) return DIORITE;
            if (below.is(Blocks.ANDESITE)) return ANDESITE;
            if (below.is(Blocks.NETHERRACK)) return NETHERRACK;
            if (below.is(Blocks.END_STONE)) return END_STONE;
            return STONE;
        }
    }

    public static final EnumProperty<Ground> GROUND = EnumProperty.create("ground", Ground.class);

    public RockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GROUND, Ground.STONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GROUND);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RockBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    /** Right-click picks the rock up (rockGt item + raw ore when marking a bedrock deposit). */
    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, net.minecraft.world.level.Level level,
                                                     BlockPos pos, net.minecraft.world.entity.player.Player player,
                                                     net.minecraft.world.InteractionHand hand,
                                                     net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide) {
            List<ItemStack> stacks = yields(level.getBlockEntity(pos));
            level.removeBlock(pos, false);
            for (ItemStack stack : stacks) {
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** rockGt of the material, plus a raw ore chunk when the rock marks a bedrock deposit. */
    private static List<ItemStack> yields(@Nullable BlockEntity be) {
        // GT6's item-on-the-ground mode: the rock carries the exact item it hands out.
        if (be instanceof RockBlockEntity rock && !rock.itemId().isEmpty()) {
            ItemStack explicit = rock.itemStack();
            java.util.ArrayList<ItemStack> out = new java.util.ArrayList<>(2);
            if (!explicit.isEmpty()) out.add(explicit);
            if (rock.hasRawOre()) {
                GTMaterial material = GTMaterialRegistry.get(rock.getMaterial());
                if (material != null && material.resolve().isValid()) {
                    ItemStack raw = MaterialStackItemHelper.mat(MaterialPrefix.oreRaw, material.resolve(), 1);
                    if (!raw.isEmpty()) out.add(raw);
                }
            }
            return out;
        }
        String materialName = be instanceof RockBlockEntity rock ? rock.getMaterial() : RockBlockEntity.DEFAULT_MATERIAL;
        GTMaterial material = GTMaterialRegistry.get(materialName);
        if (material == null || !material.resolve().isValid()) return Collections.emptyList();
        java.util.ArrayList<ItemStack> out = new java.util.ArrayList<>(2);
        int amount = be instanceof RockBlockEntity rock ? rock.count() : 1;
        ItemStack rockItem = MaterialStackItemHelper.mat(MaterialPrefix.rockGt, material.resolve(), amount);
        if (!rockItem.isEmpty()) out.add(rockItem);
        if (be instanceof RockBlockEntity rock && rock.hasRawOre()) {
            ItemStack raw = MaterialStackItemHelper.mat(MaterialPrefix.oreRaw, material.resolve(), 1);
            if (!raw.isEmpty()) out.add(raw);
        }
        return out;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return yields(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY));
    }
}
