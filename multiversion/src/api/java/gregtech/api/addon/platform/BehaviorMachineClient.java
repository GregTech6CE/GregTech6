/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Client screens and renderers for one addon's behavior machines. List this class in
 * {@code META-INF/services/gregtech.api.addon.platform.BehaviorMachineClient}.
 * GregTech loads it on the client only. A null screen keeps the default GUI.
 * A null renderer means the machine has no extra drawing.
 * The block entity implements {@link BehaviorMachineClientView} for renderers.
 */
public interface BehaviorMachineClient {
    String addonId();

    Set<String> machineTypes();

    /**
     * Null keeps GregTech's default screen. The menu view is read-only.
     * @deprecated Override {@link #screen(String, BehaviorMachineMenuView, Inventory, Component)}
     * to see which machine type is being opened.
     */
    @Deprecated(since = "0.0")
    @Nullable
    default Screen screen(BehaviorMachineMenuView menu, Inventory inventory, Component title) { return null; }

    /** Null keeps GregTech's default screen. The menu view is read-only. */
    @Nullable
    default Screen screen(String machineType, BehaviorMachineMenuView menu, Inventory inventory, Component title) {
        return screen(menu, inventory, title);
    }

    /** Null when this machine has no extra drawing. GregTech registers the dispatcher. */
    @Nullable BlockEntityRenderer<BlockEntity> renderer(String machineType);
}
