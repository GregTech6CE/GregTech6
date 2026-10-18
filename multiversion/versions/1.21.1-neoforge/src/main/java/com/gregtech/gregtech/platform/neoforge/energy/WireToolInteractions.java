package com.gregtech.gregtech.platform.neoforge.energy;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity;
import com.gregtech.gregtech.platform.neoforge.NeoToolBindings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
/** Original cutter connection path; general tool actions and powered tools remain separate. */
public final class WireToolInteractions {
 private WireToolInteractions(){}
 public static boolean use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
  var held=player.getItemInHand(hand);if(!NeoToolBindings.isWireCutter(held))return false;
  var side=com.gregtech.gregtech.platform.neoforge.transport.FluidPipeToolInteractions.selectedFace(hit);var neighbor=pos.relative(side);
  if(!player.mayBuild()||!level.mayInteract(player,pos)||!level.hasChunkAt(neighbor)||!level.mayInteract(player,neighbor))return true;
  if(level.isClientSide)return true;
  boolean connect=!state.getValue(ElectricWireBlock.propFor(side));
  if(!level.setBlockAndUpdate(pos,state.setValue(ElectricWireBlock.propFor(side),connect)))return true;
  var other=level.getBlockState(neighbor);
  if(state.getBlock() instanceof ElectricWireBlock&&other.getBlock() instanceof ElectricWireBlock||state.getBlock() instanceof com.gregtech.gregtech.block.energy.SignalWireBlock&&other.getBlock() instanceof com.gregtech.gregtech.block.energy.SignalWireBlock){level.setBlockAndUpdate(neighbor,other.setValue(ElectricWireBlock.propFor(side.getOpposite()),connect));changed(level,neighbor);}
  changed(level,pos);level.playSound(null,pos,com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(),net.minecraft.sounds.SoundSource.BLOCKS,1F,1F);
  NeoToolBindings.damageForUse(held,1,player);return true;
 }
 private static void changed(Level level,BlockPos pos){if(level.getBlockEntity(pos) instanceof ElectricWireBlockEntity wire)wire.onConnectionChanged();}
}
