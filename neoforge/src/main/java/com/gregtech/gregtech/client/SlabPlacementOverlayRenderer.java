package com.gregtech.gregtech.client;


import com.gregtech.gregtech.block.stone.GTStoneSlabBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * GT6 block-highlight wrench grid for slab placement, burning-box facing, and pipe connections.
 */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SlabPlacementOverlayRenderer {
    private static long clientTime;

    private SlabPlacementOverlayRenderer() {}

    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        clientTime++;
    }

    /** Cancel the vanilla block highlight for pipes when holding a wrench — our overlay wireframe pulses instead. */
    @SubscribeEvent
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        BlockState state = mc.level.getBlockState(event.getTarget().getBlockPos());
        var spec = interaction(state, player);
        if (spec != null && spec.connection() != null) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult target = (BlockHitResult) hitResult;
        BlockState state = mc.level.getBlockState(target.getBlockPos());
        boolean holdingSlab = isHoldingSlab(player);
        var interaction = interaction(state, player);
        if (!holdingSlab && interaction == null) return;

        BlockPos blockPos = target.getBlockPos();
        float shade = GTRenderHelper.wrenchOverlayShade(clientTime);
        Vec3 cam = event.getCamera().getPosition();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(
                blockPos.getX() + 0.5D - cam.x,
                blockPos.getY() + 0.5D - cam.y,
                blockPos.getZ() + 0.5D - cam.z
        );

        GTRenderHelper.beginWrenchOverlayLines();
        var bufferSource = mc.renderBuffers().bufferSource();
        var consumer = bufferSource.getBuffer(RenderType.lines());
        if (holdingSlab) {
            GTRenderHelper.drawWrenchOverlay(poseStack, consumer, target.getDirection(), shade);
        } else {
            GTRenderHelper.drawPipeWrenchOverlay(poseStack, consumer, target.getDirection(), interaction.activeFaces(state), shade);
            ToolFaceOverlay.drawSelection(poseStack, consumer, target, interaction, state);
        }
        bufferSource.endBatch(RenderType.lines());
        GTRenderHelper.endWrenchOverlayLines();

        poseStack.popPose();
    }

    private static boolean isHoldingSlab(Player player) {
        return player.getMainHandItem().getItem() instanceof GTStoneSlabBlockItem
                || (player.getMainHandItem().isEmpty() && player.getOffhandItem().getItem() instanceof GTStoneSlabBlockItem);
    }

    private static com.gregtech.gregtech.api.tool.ToolInteractionSpec interaction(BlockState state, Player player) {
        var main = com.gregtech.gregtech.api.tool.ToolInteractions.describe(state, player.getMainHandItem());
        if (main != null || !player.getMainHandItem().isEmpty()) return main;
        var offhand = com.gregtech.gregtech.api.tool.ToolInteractions.describe(state, player.getOffhandItem());
        // Empty-hand machine interaction can open a GUI before the offhand is tried.
        return offhand != null && offhand.connection() != null ? offhand : null;
    }
}
