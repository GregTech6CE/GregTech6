package gregtech.content.block.wood;

import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import gregtech.engine.tool.ToolInteractions;
import gregtech.content.material.Materials;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTWoods;
import gregtech.platform.Mc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
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

//? if >=1.21.1 {
//?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}

/** GT6-style tinted log block with fireproof variant support. */
public class WoodLogBlock extends RotatedPillarBlock {
    /**
     * Axe/saw/knife produce a stripped log; GT tools also drop bark. A plain axe follows vanilla
     * stripping without the extra bark. GT6's hand drill still makes maple/rainbowood sap holes from
     * a horizontal side, and other clicks leave the log untouched.
     */
//? if >=1.21.1 {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return ToolInteractions.toItemResult(use(state, level, pos, player, hand, hit));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return use(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
*///?} else {
    @Override
//?}
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        boolean gtAxe = GTToolHelper.matchesTool(stack, GTToolType.AXE)
                || GTToolHelper.matchesTool(stack, GTToolType.DOUBLE_AXE);
        boolean gtSawOrKnife = GTToolHelper.matchesTool(stack, GTToolType.SAW)
                || GTToolHelper.matchesTool(stack, GTToolType.KNIFE);
        boolean plainAxe = !GTToolHelper.isTool(stack) && stack.is(ItemTags.AXES);
        if (gtAxe || gtSawOrKnife || plainAxe) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            BlockState stripped = GTWoods.strippedLog(species).defaultBlockState()
                    .setValue(WoodStrippedLogBlock.AXIS, state.getValue(AXIS));
            if (!level.setBlock(pos, stripped, 3)) return InteractionResult.FAIL;
            level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            if (plainAxe) {
//? if >=1.21.1 {
                /*stack.hurtAndBreak(1, player, slot);
*///?} else {
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
//?}
            } else {
                ItemStack output = toolProduct();
                if (!output.isEmpty()) gregtech.engine.fluid.HandContainerTransfer.give(player, output);
                GTToolHelper.damageForToolClickReturn(stack, gtAxe ? 500L : 1000L, player, slot);
            }
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
//? if >=1.21.1 {
        /*if (!player.isCreative()) GTToolHelper.damageForToolClickReturn(stack,1000L,player);
*///?} else {
        if (!player.isCreative()) stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
//?}
        return InteractionResult.CONSUME;
    }

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    private final WoodSpecies species;

    public WoodLogBlock(WoodSpecies species, Properties properties) {
        super(properties);
        this.species = species;
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    public WoodSpecies species() { return species; }

    /** GT6 BlockTreeLogA/B/C: strip a living log into its corresponding stripped log. */
    public ItemStack toolProduct() {
        if (species == WoodSpecies.CINNAMON) {
            // GT6 BlockTreeLogB prefers its own IL.Food_Cinnamon before cinnamon dust.
            Item bark = Mc.item(
                    ResourceLocation.fromNamespaceAndPath("gt6ce", "cinnamon_bark"));
//? if >=1.21.1 {
            /*if (bark != null && bark != net.minecraft.world.item.Items.AIR) return new ItemStack(bark);
*///?} else {
            if (bark != null) return new ItemStack(bark);
//?}
            return GTItems.getStack(MaterialPrefix.dust, Materials.Cinnamon);
        }
        return GTItems.getStack(MaterialPrefix.dust, Materials.Bark);
    }

    /** The sap hole GT6 drills into this species' log, or {@code null} when there is none. */
    private TreeHoleBlock drilledHole() {
        return switch (species) {
            case MAPLE -> gregtech.content.registry.GTTreeHoles.hole(WoodSpecies.MAPLE);
            case RAINBOWOOD -> gregtech.content.registry.GTTreeHoles.hole(WoodSpecies.RAINBOWOOD);
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
