package gregtech.engine.fluid;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** Original ItemFluidDisplay first-use: creative source, server consumption even on no-op. */
public final class FluidDisplayInteraction {
    private FluidDisplayInteraction() {}

    /**
     * GT6 ItemFluidDisplay:77-79 tries each face of the clicked target, stopping at the first
     * successful fill. Never drain or replace the display carrier. The server consumes even
     * survival/no-fill clicks so a block callback cannot turn it into a survival fuel source.
     */
    public static InteractionResult useFirst(ItemStack stack, UseOnContext context) {
        var level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;
        var player = context.getPlayer();
        var pos = context.getClickedPos();
        if (player == null || !player.getAbilities().instabuild || !level.hasChunkAt(pos)
                || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack))
            return InteractionResult.CONSUME;
//? if >=1.21.1 {
        /*var fluid = FluidDisplayBinding.resolve(level.registryAccess(), stack);
*///?} else {
        var fluid = FluidDisplayBinding.resolve(stack);
//?}
        if (fluid.isEmpty()) return InteractionResult.CONSUME;
        // Preserve the actual native identity and NBT/components, not merely the carrier's field.
        var offered = fluid.copy();
        offered.setAmount(Integer.MAX_VALUE);
        for (var side : Direction.values()) {
//? if >=1.21.1 {
            /*IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
*///?} else {
            var entity = level.getBlockEntity(pos);
            IFluidHandler handler = entity == null ? null
                    : entity.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
//?}
            // Native capability lookup retains sided cover/shutter/filter policy.
            if (handler != null) {
                var target = level.getBlockEntity(pos);
                long capacity = FluidStorageInspection.read(target, handler).capacity();
                // A bounded series of native transfers fills large tanks without hanging on void sinks.
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
                if (filled) return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.CONSUME;
    }
}
