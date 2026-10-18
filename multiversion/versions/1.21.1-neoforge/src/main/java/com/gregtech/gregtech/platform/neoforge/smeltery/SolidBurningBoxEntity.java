package com.gregtech.gregtech.platform.neoforge.smeltery;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
/** Existing native ID/API delegates to the complete original executor used by every solid tier. */
public final class SolidBurningBoxEntity extends com.gregtech.gregtech.blockentity.machine.SolidBurningBoxBlockEntity {
 public SolidBurningBoxEntity(BlockPos pos,BlockState state){super(pos,state);}
 public void serverTick(){if(level!=null&&!level.isClientSide)tickServer();}
 public boolean use(Player player,InteractionHand hand,BlockHitResult hit){var result=handleToolUse(player,hand,hit);return result!=InteractionResult.PASS||handleUse(player,hand,hit)!=InteractionResult.PASS;}
}
