package com.gregtech.gregtech.api.tool;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
/** Hopper/queue-hopper delegate used when sneaking bypasses vanilla block item interaction. */
public interface ScrewdriverUseTarget { InteractionResult useScrewdriver(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit); }
