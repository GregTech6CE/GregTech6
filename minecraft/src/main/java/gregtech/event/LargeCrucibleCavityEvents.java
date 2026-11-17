package gregtech.event;

import gregtech.content.block.machine.VesselInnerFaces;
import gregtech.engine.mod.GregTechIdentity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
*///?} else {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.PistonEvent;
//?}

/**
 * Piston, placement and fluid events that would otherwise put a block, a fluid or a flame into the
 * hollow of a formed large crucible. The vessel's two cavity layers must stay air for the structure
 * to hold, so every path that bypasses the block's own use handlers is closed here instead.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?}
public final class LargeCrucibleCavityEvents {
    private LargeCrucibleCavityEvents() {}

    @SubscribeEvent
    public static void onPistonMovePre(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof Level level)) return;
        // The piston head itself, and a pushed block, may not enter a cavity layer.
        if (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND
                && VesselInnerFaces.isFormedLargeCrucibleCavity(level, event.getFaceOffsetPos())) {
            event.setCanceled(true);
            return;
        }
        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) return;
        Direction moveDir = event.getPistonMoveType().isExtend
                ? event.getDirection()
                : event.getDirection().getOpposite();
        for (BlockPos pos : resolver.getToPush())
            if (VesselInnerFaces.isFormedLargeCrucibleCavity(level, pos.relative(moveDir))) {
                event.setCanceled(true);
                return;
            }
    }

    @SubscribeEvent
    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        // The level of these events is already a BlockGetter, so the cavity lookup needs no cast.
        if (!event.getPlacedBlock().isAir()
                && VesselInnerFaces.isFormedLargeCrucibleCavity(event.getLevel(), event.getPos()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (!event.getNewState().isAir()
                && VesselInnerFaces.isFormedLargeCrucibleCavity(event.getLevel(), event.getPos()))
            event.setNewState(Blocks.AIR.defaultBlockState());
    }
}