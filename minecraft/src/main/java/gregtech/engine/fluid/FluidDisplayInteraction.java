package gregtech.engine.fluid;

import gregtech.content.block.tool.FluidAttachmentBlock;
import gregtech.content.cover.CoverItems;
import gregtech.content.cover.CoverUtilityBehaviors;
import gregtech.content.cover.PanelCoverHost;
import gregtech.content.logistics.LogisticsCoverHost;
import gregtech.content.logistics.LogisticsCoverInteraction;
import gregtech.content.logistics.LogisticsCoverType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** Original ItemFluidDisplay first-use: inexhaustible fluid source, server consumption even on no-op. */
public final class FluidDisplayInteraction {
    private FluidDisplayInteraction() {}

    /**
     * GT6 ItemFluidDisplay tries each face of the clicked target, stopping at the first
     * successful fill. Never drain or replace the display carrier; works in both Creative and
     * Survival once held. Filter covers on the clicked face take precedence so fluid filters can
     * still sample the display item and item filters can explicitly reject it.
     */
    public static InteractionResult useFirst(ItemStack stack, UseOnContext context) {
        var level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;
        var player = context.getPlayer();
        var pos = context.getClickedPos();
        if (player == null) return InteractionResult.PASS;
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack))
            return InteractionResult.CONSUME;
        if (passesToFilterCover(level, pos, context.getClickedFace())) return InteractionResult.PASS;
        var fluid = FluidDisplayBinding.resolve(level.registryAccess(), stack);
        if (fluid.isEmpty()) return InteractionResult.CONSUME;
        // Preserve the actual native identity and NBT/components, not merely the carrier's field.
        var offered = fluid.copy();
        offered.setAmount(Integer.MAX_VALUE);
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof FluidAttachmentBlock attachment) {
            // The attachment access wrapper decides whether this shape fills or only drains.
            var facing = state.getValue(FluidAttachmentBlock.FACING);
            var targetPos = pos.relative(facing);
            var raw = attachment.attachedHandler(level, targetPos, facing.getOpposite());
            if (raw != null) fillHandler(level.getBlockEntity(targetPos), attachment.access(raw), offered);
            return InteractionResult.CONSUME;
        }
        for (var side : Direction.values()) {
//? if >=1.21.1 {
            /*IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
*///?} else {
            var entity = level.getBlockEntity(pos);
            IFluidHandler handler = entity == null ? null
                    : entity.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
//?}
            // Native capability lookup retains sided cover/shutter/filter policy.
            if (handler != null && fillHandler(level.getBlockEntity(pos), handler, offered))
                return InteractionResult.CONSUME;
        }
        return InteractionResult.CONSUME;
    }

    /** A bounded series of native transfers fills large tanks without hanging on void sinks. */
    private static boolean fillHandler(BlockEntity target, IFluidHandler handler, FluidStack offered) {
        long capacity = FluidStorageInspection.read(target, handler).capacity();
        long remaining = Math.max(Integer.MAX_VALUE, capacity);
        boolean filled = false;
        for (int batch = 0; batch < 64 && remaining > 0; batch++) {
            var portion = offered.copy();
            portion.setAmount((int) Math.min(Integer.MAX_VALUE, remaining));
            int accepted = handler.fill(portion, IFluidHandler.FluidAction.EXECUTE);
            if (accepted <= 0) break;
            remaining -= Math.min(accepted, portion.getAmount());
            filled = true;
            if (accepted < portion.getAmount()) break;
        }
        return filled;
    }

    private static boolean passesToFilterCover(Level level, BlockPos pos, Direction face) {
        var entity = level.getBlockEntity(pos);
        if (entity instanceof PanelCoverHost host) {
            var installed = host.getCover(face);
            var id = CoverItems.behavior(installed);
            if (CoverUtilityBehaviors.FILTER_FLUID.equals(id)
                    && CoverUtilityBehaviors.fluidFilter(installed, level.registryAccess()).isEmpty()) return true;
            if (CoverUtilityBehaviors.FILTER_ITEM.equals(id) || CoverUtilityBehaviors.RETRIEVER_ITEM.equals(id))
                return true;
        }
        if (entity instanceof LogisticsCoverHost host) {
            var installed = host.logisticsCovers().get(face);
            var type = LogisticsCoverType.of(installed);
            if (type != null && type.filtered()) {
                if (type.channel() == LogisticsCoverType.Channel.FLUID
                        && LogisticsCoverInteraction.fluidFilter(installed, level.registryAccess()).isEmpty())
                    return true;
                if (type.channel() == LogisticsCoverType.Channel.ITEM) return true;
            }
        }
        return false;
    }
}
