/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import java.util.Set;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Client renderers for one addon's functional parts. List this class in
 * {@code META-INF/services/gregtech.api.addon.platform.LayoutPartClient}.
 * GregTech loads it on the client only. The block entity implements {@link PartClientView}.
 * A part with no renderer still syncs that view.
 */
public interface LayoutPartClient {
    String addonId();

    Set<String> partNames();

    /** Null when this part has no extra drawing. GregTech registers the dispatcher. */
    @Nullable BlockEntityRenderer<BlockEntity> renderer(String partName);
}
