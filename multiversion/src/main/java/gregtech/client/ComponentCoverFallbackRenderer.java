/**
 * Copyright (c) 2021 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech.client;

import gregtech.content.cover.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Direction;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
*///?} else {
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
//?}

/** Cover rendering for native GT hosts tracked by the fallback lifecycle. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
//?}
public final class ComponentCoverFallbackRenderer {

    private ComponentCoverFallbackRenderer() {
    }

    public static void receive(gregtech.network.PacketSyncComponentCovers packet) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.hasChunkAt(packet.pos())) return;
        var owner = level.getBlockEntity(packet.pos());
//? if >=1.21.1 {
        /*if (owner != null && packet.entityType().equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(
                owner.getType()))) ComponentCoverFallback.read(owner, packet.tag(), level.registryAccess());
*///?} else {
        if (owner != null && packet.type().equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(
                owner.getType()))) ComponentCoverFallback.read(owner, packet.tag());
//?}
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) return;
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        for (var owner : ComponentCoverFallback.active()) {
            if (owner.getLevel() != level || owner.isRemoved()
                    || !event.getFrustum().isVisible(new net.minecraft.world.phys.AABB(owner.getBlockPos()))) continue;
            var pos = owner.getBlockPos();
            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            int light = LevelRenderer.getLightColor(level, pos);
            var host = (PanelCoverHost) owner;
            for (var side : Direction.values())
                PanelCoverRenderer.renderFace(host, side, pose, buffers, light);
            if (owner instanceof gregtech.content.logistics.LogisticsCoverHost logistics)
                LogisticsCoverRenderer.renderFaces(owner, logistics, pose, buffers, light);
            pose.popPose();
        }
    }
}
