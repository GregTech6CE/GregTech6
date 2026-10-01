package com.gregtech.gregtech.platform.neoforge.energy;
import com.gregtech.gregtech.block.energy.AxleBlock;
import com.gregtech.gregtech.blockentity.energy.AxleBlockEntity;
import com.gregtech.gregtech.platform.neoforge.NeoToolBindings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
/** Original straight-axis tool validation and reciprocal connection mutation. */
public final class AxleToolInteractions {
 private AxleToolInteractions(){}
 public static boolean use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
  var tool=player.getItemInHand(hand);if(!NeoToolBindings.isMachineWrench(tool))return false;
  var side=com.gregtech.gregtech.platform.neoforge.transport.FluidPipeToolInteractions.selectedFace(hit);var neighbor=pos.relative(side);boolean connect=!state.getValue(AxleBlock.propFor(side));var next=state.setValue(AxleBlock.propFor(side),connect);
  if(!AxleBlock.isValidConnectionState(next)||!player.mayBuild()||!level.mayInteract(player,pos)||!level.hasChunkAt(neighbor)||!level.mayInteract(player,neighbor))return true;
  var other=level.getBlockState(neighbor);boolean matching=other.getBlock() instanceof AxleBlock;
  if(matching&&!AxleBlock.isValidConnectionState(other.setValue(AxleBlock.propFor(side.getOpposite()),connect)))return true;
  if(level.isClientSide)return true;if(!level.setBlockAndUpdate(pos,next))return true;
  if(matching){level.setBlockAndUpdate(neighbor,other.setValue(AxleBlock.propFor(side.getOpposite()),connect));changed(level,neighbor);}changed(level,pos);
  level.playSound(null,pos,com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(),net.minecraft.sounds.SoundSource.BLOCKS,1F,1F);NeoToolBindings.damageForUse(tool,1,player);return true;
 }
 private static void changed(Level level,BlockPos pos){if(level.getBlockEntity(pos) instanceof AxleBlockEntity axle)axle.onConnectionChanged();}
}
