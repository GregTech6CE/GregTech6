package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.blockentity.machine.FunctionalPartRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
        if (!runtime.handlesUse()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.sidedSuccess(true);
        return runtime.onUse(player, hand, partHit)
                ? InteractionResult.sidedSuccess(false) : InteractionResult.PASS;
    }

    public static com.gregtech.gregtech.api.addon.platform.PartHit hitOf(BlockPos pos, BlockHitResult hit) {
        Vec3 at = hit.getLocation();
        return com.gregtech.gregtech.api.addon.platform.PartHit.at(hit.getDirection(),
                at.x - pos.getX(), at.y - pos.getY(), at.z - pos.getZ());
    }
}
