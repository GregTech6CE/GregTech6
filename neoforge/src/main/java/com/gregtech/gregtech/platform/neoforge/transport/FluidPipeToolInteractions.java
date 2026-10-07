package com.gregtech.gregtech.platform.neoforge.transport;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.platform.neoforge.NeoToolBindings;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
/** Original 3x3 wrench face and reciprocal fluid-pipe connection mutation. Full electric tool module remains separate. */
public final class FluidPipeToolInteractions {
    private FluidPipeToolInteractions() {}
    public static Direction selectedFace(BlockHitResult hit){var p=hit.getBlockPos();return com.gregtech.gregtech.util.GTPlacementCode.getSideWrenching(hit.getDirection(),(float)(hit.getLocation().x-p.getX()),(float)(hit.getLocation().y-p.getY()),(float)(hit.getLocation().z-p.getZ()));}
    public static boolean isInteractionTool(ItemStack stack){
        if(NeoToolBindings.isMachineWrench(stack)||NeoToolBindings.isWireCutter(stack)||NeoToolBindings.isScrewdriver(stack))return true;
        for(String kind:new String[]{"crowbar","chisel","soft_hammer","hammer","monkey_wrench","scissors","knife","magnifying_glass","soldering_iron"})if(NeoToolBindings.matches(stack,kind))return true;
        return false;
    }
    public static boolean use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        ItemStack held=player.getItemInHand(hand);
        if(!NeoToolBindings.isMachineWrench(held))return false;
        Direction side=selectedFace(hit);BlockPos neighbor=pos.relative(side);
        if(!player.mayBuild()||!level.mayInteract(player,pos)||!level.hasChunkAt(neighbor)||!level.mayInteract(player,neighbor))return true;
        if(level.isClientSide)return true;
        boolean connected=!state.getValue(FluidPipeBlock.propFor(side));
        if(connected&&!com.gregtech.gregtech.content.cover.CoverConnections.canConnect(level,pos,side))return true;
        if(!level.setBlockAndUpdate(pos,state.setValue(FluidPipeBlock.propFor(side),connected)))return true;
        BlockState other=level.getBlockState(neighbor);
        if(other.getBlock() instanceof FluidPipeBlock)level.setBlockAndUpdate(neighbor,other.setValue(FluidPipeBlock.propFor(side.getOpposite()),connected));
        level.invalidateCapabilities(pos);level.invalidateCapabilities(neighbor);
        level.playSound(null,pos,FluidTransportRegistries.WRENCH.get(),net.minecraft.sounds.SoundSource.BLOCKS,1F,1F);
        NeoToolBindings.damageForUse(held,1,player);return true;
    }
}
