package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.platform.BehaviorMachineClient;
import com.gregtech.gregtech.api.addon.platform.BehaviorMachineClientView;
import com.gregtech.gregtech.api.addon.platform.BehaviorMachineMenuView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Set;

/** Marks the armed sample switch with a red box. The default screen stays in use. */
public final class AddonSampleBehaviorClient implements BehaviorMachineClient {
    @Override public String addonId() { return "addon_sample"; }

    @Override public Set<String> machineTypes() { return Set.of("sample_switch"); }

    @Nullable
    @Override public Screen screen(String machineType, BehaviorMachineMenuView menu, Inventory inventory, Component title) {
        return null;
    }

    @Nullable
    @Override public BlockEntityRenderer<BlockEntity> renderer(String machineType) {
        return "sample_switch".equals(machineType) ? new ArmedRenderer() : null;
    }

    private static final class ArmedRenderer implements BlockEntityRenderer<BlockEntity> {
        @Override public void render(BlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffer,
                                     int light, int overlay) {
            if (!(entity instanceof BehaviorMachineClientView view) || !view.getBoolean("armed")) return;
            LevelRenderer.renderLineBox(pose, buffer.getBuffer(RenderType.lines()),
                    new AABB(0.35, 1.0, 0.35, 0.65, 1.3, 0.65), 1.0F, 0.15F, 0.15F, 1.0F);
        }

        @Override public boolean shouldRender(BlockEntity entity, Vec3 camera) {
            return entity instanceof BehaviorMachineClientView view && view.getBoolean("armed");
        }
    }
}
