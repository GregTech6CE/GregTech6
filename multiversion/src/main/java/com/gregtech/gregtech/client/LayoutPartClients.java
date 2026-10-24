package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.addon.platform.LayoutPartClient;
import com.gregtech.gregtech.api.addon.platform.PartClientView;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/** Client renderers, loaded once from {@link LayoutPartClient}. A part with no renderer still syncs its view. */
public final class LayoutPartClients {
    private static final Map<String, BlockEntityRenderer<BlockEntity>> RENDERERS = new LinkedHashMap<>();
    private static boolean collected;

    private LayoutPartClients() {}

    /** Published only after every client is accepted, so a failure can be collected again. */
    public static synchronized void collect() {
        if (collected) return;
        var renderers = new LinkedHashMap<String, BlockEntityRenderer<BlockEntity>>();
        ClassLoader loader = LayoutPartClient.class.getClassLoader();
        for (LayoutPartClient client : ServiceLoader.load(LayoutPartClient.class, loader)) {
            String id;
            try {
                id = client.addonId();
            } catch (RuntimeException | Error failure) {
                throw com.gregtech.gregtech.api.addon.AddonFailures.of(
                        "Part client failed: " + client.getClass().getName(), failure);
            }
            if (!com.gregtech.gregtech.api.addon.AddonNames.isAddonId(id))
                throw new IllegalStateException("Part client has no addon id: " + client.getClass().getName());
            Set<String> names;
            try {
                names = client.partNames();
            } catch (RuntimeException | Error failure) {
                throw com.gregtech.gregtech.api.addon.AddonFailures.of(
                        "Addon '" + id + "' part client failed", failure);
            }
            if (names == null) continue;
            for (String name : names) {
                BlockEntityRenderer<BlockEntity> renderer = client.renderer(name);
                if (renderer != null && renderers.putIfAbsent(id + "\0" + name, renderer) != null)
                    throw new IllegalStateException("Addon '" + id + "' part '" + name + "' registered two renderers");
            }
        }
        RENDERERS.putAll(renderers);
        collected = true;
    }

    public static BlockEntityRenderer<BlockEntity> renderer(PartClientView view) {
        if (!collected) collect();
        if (view == null) return null;
        return RENDERERS.get(view.addonId() + "\0" + view.name());
    }
}
