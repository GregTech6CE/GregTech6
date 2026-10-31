package gregtech.event;

import gregtech.GregTechConfig;
import gregtech.content.blockentity.machine.CompressedGasTankBlockEntity;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity;
import gregtech.content.blockentity.machine.TankBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidUtil;
*///?} else {
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fluids.FluidUtil;
//?}

/** Gate the player click, leaving the tank capability available to taps, funnels and automation. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class TankContainerInteractionHandler {
    private TankContainerInteractionHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (!GregTechConfig.Rule.TANKS_REQUIRE_FLUID_TOOLS.enabled() || event.getEntity().isSpectator()) return;
        var held = event.getItemStack();
        // Fluid display items are a direct editing tool, not a survival bucket or vessel.
//? if >=1.21.1 {
        /*if (held.getItem() instanceof gregtech.platform.neoforge.fluid.FluidDisplayItem) return;
*///?} else {
        if (held.getItem() instanceof gregtech.content.item.FluidItem) return;
//?}
        if (held.isEmpty() || !tank(event.getLevel().getBlockEntity(event.getPos()))
                || !FluidUtil.getFluidHandler(held.copyWithCount(1)).isPresent()) return;
        if (event.getEntity().isShiftKeyDown() && held.getItem() instanceof BlockItem) {
            // Sneaking still places a portable container instead of pouring its contents.
//? if >=1.21.1 {
            /*event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
*///?} else {
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
//?}
            return;
        }
        // Consume the click as well, otherwise BucketItem may spill water next to the refused tank.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static boolean tank(BlockEntity entity) {
        if (entity instanceof MultiblockPortBlockEntity port) entity = port.sensorTarget();
        return entity instanceof TankBlockEntity || entity instanceof MultiblockTankControllerBlockEntity
                || entity instanceof CompressedGasTankBlockEntity;
    }
}
