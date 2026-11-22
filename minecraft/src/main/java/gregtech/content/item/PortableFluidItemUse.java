package gregtech.content.item;

import gregtech.content.block.energy.ReactorCoreBlock;
import gregtech.content.block.machine.AxialGeneratorBlock;
import gregtech.content.block.machine.BurningBoxBlock;
import gregtech.content.block.machine.CompressedGasTankBlock;
import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.block.machine.LargeGasTurbineControllerBlock;
import gregtech.content.block.machine.LargeHeatExchangerControllerBlock;
import gregtech.content.block.machine.OriginalBedrockDrillControllerBlock;
import gregtech.content.block.machine.OriginalLargeBoilerControllerBlock;
import gregtech.content.block.machine.TankBlock;
import gregtech.content.block.machine.TankControllerBlock;
import gregtech.content.block.tool.FluidAttachmentBlock;
import gregtech.content.block.tool.PortableContainerBlock;
import gregtech.content.block.tool.ProcessingToolBlock;
import gregtech.content.blockentity.machine.MoldBasinBlockEntity;
import gregtech.content.blockentity.machine.MoldBlockEntity;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity;
import gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity;
import gregtech.engine.fluid.FluidContainerInteraction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}
import org.jetbrains.annotations.Nullable;

/** Shared item-level pre-check so handheld containers yield to block handlers and avoid stray placements. */
final class PortableFluidItemUse {
    private PortableFluidItemUse() {}

    static boolean isMold(@Nullable BlockEntity entity) {
        return entity instanceof MoldBlockEntity || entity instanceof MoldBasinBlockEntity;
    }

    static boolean isBlockHandled(BlockState state, @Nullable BlockEntity entity) {
        Block block = state.getBlock();
        if (block instanceof FluidAttachmentBlock
                || block instanceof TankBlock
                || block instanceof CompressedGasTankBlock
                || block instanceof TankControllerBlock
                || block instanceof FluidPipeBlock
                || block instanceof PortableContainerBlock
                || block instanceof ProcessingToolBlock
                || block instanceof BurningBoxBlock
                || block instanceof ReactorCoreBlock
                || block instanceof OriginalBedrockDrillControllerBlock
                || block instanceof AxialGeneratorBlock
                || block instanceof OriginalLargeBoilerControllerBlock
                || block instanceof LargeHeatExchangerControllerBlock
                || block instanceof LargeGasTurbineControllerBlock) {
            return true;
        }
        return entity instanceof SmeltingCrucibleBlockEntity
                || (entity instanceof MultiblockPortBlockEntity port
                        && (port.crucibleController() != null
                                || port.sensorTarget() instanceof MultiblockTankControllerBlockEntity));
    }

    @Nullable
    static IFluidHandler fluidHandler(UseOnContext context) {
        return FluidUtil.getFluidHandler(context.getLevel(), context.getClickedPos(), context.getClickedFace())
                .orElse(null);
    }

    static InteractionResult onItemUseFirst(UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (player == null
                || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown() && context.getItemInHand().getItem() instanceof net.minecraft.world.item.BlockItem) {
            return InteractionResult.PASS;
        }
        var state = level.getBlockState(pos);
        var target = level.getBlockEntity(pos);
        if (isMold(target) || isBlockHandled(state, target)) return InteractionResult.PASS;
        var handler = fluidHandler(context);
        if (handler == null) return InteractionResult.PASS;
        if (level.isClientSide) return FluidContainerInteraction.clientResult(player, context.getHand());
        return FluidContainerInteraction.use(player, context.getHand(), handler)
                ? InteractionResult.CONSUME
                : InteractionResult.PASS;
    }

    static boolean blocksAccidentalPlacement(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return false;
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var target = level.getBlockEntity(pos);
        if (isMold(target)) return false;
        return isBlockHandled(state, target) || fluidHandler(context) != null;
    }
}
