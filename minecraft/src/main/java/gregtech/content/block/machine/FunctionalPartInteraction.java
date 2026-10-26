package gregtech.content.block.machine;

import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import gregtech.content.blockentity.machine.FunctionalPartRuntime;
import gregtech.client.gui.PartMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
//?} else {
import net.minecraftforge.network.NetworkHooks;
//?}

/** Wrench, magnifying glass and part click. Energy-only parts do not call this. */
public final class FunctionalPartInteraction {
    private FunctionalPartInteraction() {}

    public static InteractionResult interact(Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FunctionalPartRuntime.Host host)) return InteractionResult.PASS;
        FunctionalPartRuntime runtime = host.function();
        if (runtime == null) return InteractionResult.PASS;
        var partHit = hitOf(pos, hit);
        if (GTToolHelper.isMachineWrench(player.getItemInHand(hand))) {
            if (runtime.onWrench(player, partHit)) return InteractionResult.sidedSuccess(level.isClientSide);
            boolean turned = MachineRotationType.handleWrench(level.getBlockState(pos), level, pos, player, hand, hit,
                    net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, MachineRotationType.HORIZONTAL);
            return turned ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
        }
        if (GTToolHelper.matchesTool(player.getItemInHand(hand), GTToolType.MAGNIFYING_GLASS)) {
            var lines = runtime.describe();
            if (lines.isEmpty()) return InteractionResult.PASS;
            if (!level.isClientSide) for (Component line : lines) player.displayClientMessage(line, false);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!runtime.handlesUse() && !runtime.opensMenu()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.sidedSuccess(true);
        boolean used = runtime.handlesUse() && runtime.onUse(player, hand, partHit);
        if (!used && runtime.opensMenu() && player instanceof ServerPlayer server) openMenu(server, host);
        return used || runtime.opensMenu() ? InteractionResult.sidedSuccess(false) : InteractionResult.PASS;
    }

    private static void openMenu(ServerPlayer server, FunctionalPartRuntime.Host host) {
        Component title = host.blockState().getBlock().getName();
        var provider = new SimpleMenuProvider((id, inventory, player) -> new PartMenu(id, inventory, host), title);
//? if >=1.21.1 {
/*        server.openMenu(provider, buf -> buf.writeBlockPos(host.pos()));
*///?} else {
        NetworkHooks.openScreen(server, provider, buf -> buf.writeBlockPos(host.pos()));
//?}
    }

    public static gregtech.api.addon.platform.PartHit hitOf(BlockPos pos, BlockHitResult hit) {
        Vec3 at = hit.getLocation();
        return gregtech.api.addon.platform.PartHit.at(hit.getDirection(),
                at.x - pos.getX(), at.y - pos.getY(), at.z - pos.getZ());
    }
}
