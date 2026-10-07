/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Native adaptation of Behavior_Tool and ITileEntityMultiBlockController.Util. */
package com.gregtech.gregtech.api.multiblock;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;

public final class MultiblockTools {
    private MultiblockTools() {}

    public static InteractionResult use(UseOnContext context) {
        var stack = context.getItemInHand();
        boolean builder = GTToolHelper.matchesTool(stack, GTToolType.BUILDER_WAND);
        if (!builder && !GTToolHelper.matchesTool(stack, GTToolType.MAGNIFYING_GLASS)) return InteractionResult.PASS;
        var level = context.getLevel();
        var player = context.getPlayer();
        if (level.isClientSide || player == null || !level.hasChunkAt(context.getClickedPos())) return InteractionResult.PASS;
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof MultiblockToolTarget target)) return InteractionResult.PASS;
        var messages = new ArrayList<Component>();
        long cost = target.useMultiblockTool(context, messages);
        messages.forEach(message -> player.sendSystemMessage(message));
        if (cost <= 0) return InteractionResult.PASS;
        GTToolHelper.damageForToolClickReturn(stack, cost, player,
                context.getHand() == net.minecraft.world.InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
        level.playSound(null, context.getClickedPos(), builder ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_YES,
                SoundSource.PLAYERS, 1, 1);
        return InteractionResult.SUCCESS;
    }

    /** Original FluidTankGT.content; preserve full long amounts and translatable native names. */
    public static Component tankContent(com.gregtech.gregtech.api.fluid.FluidTankGT tank,String emptyMessage) {
        if(tank.isEmpty())return Component.literal(emptyMessage);
        var fluid=tank.getFluidLong();
        return Component.literal(MultiblockToolRules.amount(tank.getAmount())+" L of ").append(fluid.getDisplayName())
                .append(com.gregtech.gregtech.registry.GTFluids.isGas(fluid)?" (Gaseous)":" (Liquid)");
    }

    /** Exact structure cell placement; never the ordinary wand's copy-on-top operation. */
    public static boolean build(UseOnContext use, BlockPos pos, Block expected) {
        var level = use.getLevel();
        var player = use.getPlayer();
        var clicked = use.getClickedPos();
        if (level.isClientSide || player == null || !MultiblockToolRules.inBuilderReach(
                pos.getX() - clicked.getX(), pos.getY() - clicked.getY(), pos.getZ() - clicked.getZ())
                || !level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player, pos)) return false;
        var state = level.getBlockState(pos);
        if (!(state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES)
                || state.getBlock() instanceof BushBlock || state.getBlock() instanceof SnowLayerBlock
                || state.getBlock() instanceof BaseFireBlock)) return false;
        ItemStack source = ItemStack.EMPTY;
        if (player.getAbilities().instabuild) source = new ItemStack(expected);
        else for (int i = player.getInventory().getContainerSize() - 1; i >= 0; i--) {
            var candidate = player.getInventory().getItem(i);
            if (!candidate.isEmpty() && candidate.is(expected.asItem())) { source = candidate; break; }
        }
        if (source.isEmpty() || !(source.getItem() instanceof BlockItem item)
                || !player.mayUseItemAt(pos, Direction.UP, source)) return false;
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var context = new BlockPlaceContext(level, player, use.getHand(), source, hit) {
            @Override public BlockPos getClickedPos() { return pos; }
            @Override public boolean canPlace() { return true; }
            @Override public boolean replacingClickedOnBlock() { return true; }
        };
        // BlockItem preserves native item data, placement checks/updates and consumes only after success.
        if (!item.place(context).consumesAction() || !level.getBlockState(pos).is(expected)) return false;
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1, 1);
        return true;
    }
}
