package gregtech.content.tool;

import gregtech.content.block.OreBlock;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.engine.tool.GTToolHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

/** GT6 ToolCompat.prospectStone: inspect behind the clicked face and sample nearby ore. */
public final class HammerProspecting {
    private HammerProspecting() {}

    public static InteractionResult use(UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        if (level.isClientSide || player == null || !GTToolHelper.isUsable(context.getItemInHand()))
            return InteractionResult.PASS;
        var pos = context.getClickedPos();
        var clicked = level.getBlockState(pos);
        if (clicked.getBlock() instanceof OreBlock) {
            player.displayClientMessage(clicked.getBlock().getName().copy().append("!"), false);
            GTToolHelper.damageForToolClickReturn(
                    context.getItemInHand(),
                    100,
                    player,
                    context.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                            ? EquipmentSlot.OFFHAND
                            : EquipmentSlot.MAINHAND);
            return InteractionResult.CONSUME;
        }
        if (!(clicked.getBlock() instanceof GTStoneBlock)
                && !clicked.is(BlockTags.BASE_STONE_OVERWORLD)
                && !clicked.is(Blocks.NETHERRACK)
                && !clicked.is(Blocks.END_STONE)) return InteractionResult.PASS;
        int quality = Math.max(1, Math.min(20, GTToolHelper.getHarvestLevel(context.getItemInHand()) + 4));
        var cursor = pos.mutable();
        boolean notified = false;
        for (int i = 0; i < quality; i++) {
            cursor.move(context.getClickedFace().getOpposite());
            if (!level.hasChunkAt(cursor) || level.isOutsideBuildHeight(cursor)) break;
            var state = level.getBlockState(cursor);
            String message = null;
            if (state.getFluidState().is(Fluids.LAVA)) message = "There is Lava behind this Rock";
            else if (!state.getFluidState().isEmpty()) message = "There is a Fluid behind this Rock";
            else if (state.is(Blocks.INFESTED_STONE)
                    || state.getCollisionShape(level, cursor).isEmpty())
                message = "There is an Air Pocket behind this Rock";
            else if (i < 4 && !state.equals(clicked)) message = "Material is changing behind this Rock";
            if (message != null) {
                player.displayClientMessage(Component.literal(message), false);
                notified = true;
                break;
            }
        }
        var random = new java.util.Random(
                pos.getX() ^ pos.getY() ^ pos.getZ() ^ context.getClickedFace().ordinal());
        for (int i = 0; i < quality * quality; i++) {
            var sample = pos.offset(
                    random.nextInt(1 + 2 * quality) - quality,
                    random.nextInt(1 + 2 * quality) - quality,
                    random.nextInt(1 + 2 * quality) - quality);
            if (!level.hasChunkAt(sample) || level.isOutsideBuildHeight(sample)) continue;
            var block = level.getBlockState(sample).getBlock();
            if (block instanceof OreBlock) {
                player.displayClientMessage(
                        Component.literal("Found traces of ").append(block.getName()), false);
                notified = true;
                break;
            }
        }
        if (!notified) player.displayClientMessage(Component.literal("No traces of Ore found"), false);
        GTToolHelper.damageForToolClickReturn(
                context.getItemInHand(),
                10000,
                player,
                context.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                        ? EquipmentSlot.OFFHAND
                        : EquipmentSlot.MAINHAND);
        level.playSound(
                null,
                pos,
                net.minecraft.sounds.SoundEvents.ANVIL_USE,
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.5F,
                1.0F);
        return InteractionResult.CONSUME;
    }
}
