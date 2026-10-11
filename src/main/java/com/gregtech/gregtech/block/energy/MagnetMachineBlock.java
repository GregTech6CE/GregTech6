package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** GT6 bipolar magnet: four input faces, positive front and negative rear output. */
public final class MagnetMachineBlock extends EnergyNodeBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public MagnetMachineBlock(EnergyNodeSpec spec, Properties properties) {
        super(spec, properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        // Only an ordinary wrench rotates the source bipolar poles.
        return GTToolHelper.isMonkeyWrench(tool) ? null : super.toolInteraction(state, tool);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        ItemStack tool = player.getItemInHand(hand);
        if (level.getBlockEntity(pos) instanceof EnergyNodeBlockEntity node) {
            if (GTToolHelper.isSoftHammer(tool)) {
                if (!level.isClientSide) {
                    boolean on = node.toggleMagnetEnabled();
                    player.displayClientMessage(Component.translatable(on
                            ? "message.gregtech.magnet.enabled" : "message.gregtech.magnet.disabled"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }
}
