package com.gregtech.gregtech.block.wood;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTWoods;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6-style tinted log block with fireproof variant support. */
public class WoodLogBlock extends RotatedPillarBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    private final WoodSpecies species;

    public WoodLogBlock(WoodSpecies species, Properties properties) {
        super(properties);
        this.species = species;
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    public WoodSpecies species() { return species; }

    /** GT6 BlockTreeLogA/B/C: strip a living log into its corresponding beam. */
    public ItemStack toolProduct() {
        if (species == WoodSpecies.CINNAMON) {
            // GT6 BlockTreeLogB:99 prefers its own IL.Food_Cinnamon before cinnamon dust.
            Item bark = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("gregtech", "cinnamon_bark"));
            if (bark != null) return new ItemStack(bark);
            return GTItems.getStack(MaterialPrefix.dust, Materials.Cinnamon);
        }
        return GTItems.getStack(MaterialPrefix.dust, Materials.Bark);
    }

    /**
     * Axe/saw/knife produce a beam and bark. GT6's hand drill still makes maple/rainbowood sap
     * holes from a horizontal side, and other clicks leave the log untouched.
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        boolean axe = GTToolHelper.matchesTool(stack, GTToolType.AXE);
        if (axe || GTToolHelper.matchesTool(stack, GTToolType.SAW)
                || GTToolHelper.matchesTool(stack, GTToolType.KNIFE)) {
            ItemStack output = toolProduct();
            if (output.isEmpty()) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            BlockState beam = GTWoods.beam(species).defaultBlockState()
                    .setValue(WoodBeamBlock.AXIS, state.getValue(AXIS));
            if (!level.setBlock(pos, beam, 3)) return InteractionResult.FAIL;
            com.gregtech.gregtech.api.fluid.HandContainerTransfer.give(player, output);
            GTToolHelper.damageForToolClickReturn(stack, axe ? 500L : 1000L, player);
            return InteractionResult.CONSUME;
        }
        TreeHoleBlock hole = drilledHole();
        if (hole == null) return InteractionResult.PASS;
        Direction side = hit.getDirection();
        if (!side.getAxis().isHorizontal()) return InteractionResult.PASS;
        if (!GTToolHelper.matchesTool(stack, GTToolType.HAND_DRILL)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        level.setBlock(pos, hole.defaultBlockState().setValue(TreeHoleBlock.FACING, side), 3);
        if (!player.isCreative()) stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResult.CONSUME;
    }

    /** The sap hole GT6 drills into this species' log, or {@code null} when there is none. */
    private TreeHoleBlock drilledHole() {
        return switch (species) {
            case MAPLE -> com.gregtech.gregtech.registry.GTTreeHoles.hole(WoodSpecies.MAPLE);
            case RAINBOWOOD -> com.gregtech.gregtech.registry.GTTreeHoles.hole(WoodSpecies.RAINBOWOOD);
            default -> null;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AXIS); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(AXIS, ctx.getClickedFace().getAxis());
    }
}
