/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gregtech.addoncontracts;

import com.gregtech.gregtech.api.addon.platform.BehaviorMachineClient;
import com.gregtech.gregtech.api.addon.platform.BehaviorMachineMenuView;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.Set;

/** Compile-only contract: the old screen overload remains implementable. */
@SuppressWarnings("deprecation")
public final class LegacyBehaviorClient implements BehaviorMachineClient {
    @Override public String addonId() { return "legacy_contract"; }
    @Override public Set<String> machineTypes() { return Set.of("legacy_contract"); }
    @Override public BlockEntityRenderer<BlockEntity> renderer(String machineType) { return null; }

    @Override
    public Screen screen(BehaviorMachineMenuView menu, Inventory inventory, Component title) {
        return null;
    }
}
