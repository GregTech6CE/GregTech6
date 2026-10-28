package gregtech.client;

import gregtech.api.addon.platform.BehaviorMachineClient;
import gregtech.api.addon.platform.BehaviorMachineClientView;
import gregtech.api.addon.platform.BehaviorMachineMenuView;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Client screens and renderers, loaded once from {@link BehaviorMachineClient}. */
public final class BehaviorMachineClients {
    private static final Map<String, BehaviorMachineClient> CLIENTS = new LinkedHashMap<>();
    private static final Map<String, BlockEntityRenderer<BlockEntity>> RENDERERS = new LinkedHashMap<>();
    private static boolean collected;

    private BehaviorMachineClients() {}

    /** Published only after every client is accepted, so a failure can be collected again. */
    public static synchronized void collect() {
        if (collected) return;
        var found = new LinkedHashMap<String, BehaviorMachineClient>();
        var renderers = new LinkedHashMap<String, BlockEntityRenderer<BlockEntity>>();
        ClassLoader loader = BehaviorMachineClient.class.getClassLoader();
        for (BehaviorMachineClient client : ServiceLoader.load(BehaviorMachineClient.class, loader)) {
            String id;
            try {
                id = client.addonId();
            } catch (RuntimeException | Error failure) {
                throw gregtech.api.addon.AddonFailures.of(
                        "Behavior machine client failed: " + client.getClass().getName(), failure);
            }
            if (!gregtech.api.addon.AddonNames.isAddonId(id))
                throw new IllegalStateException("Behavior machine client has no addon id: "
                        + client.getClass().getName());
            Set<String> types;
            try {
                types = client.machineTypes();
            } catch (RuntimeException | Error failure) {
                throw gregtech.api.addon.AddonFailures.of("Addon '" + id + "' behavior machine client failed", failure);
            }
            if (types == null) continue;
            for (String type : types) {
                if (type == null || type.isBlank())
                    throw new IllegalStateException("Addon '" + id + "' behavior machine client has an empty type");
                if (found.putIfAbsent(id + "\0" + type, client) != null)
                    throw new IllegalStateException(
                            "Addon '" + id + "' machine '" + type + "' registered two client hooks");
                BlockEntityRenderer<BlockEntity> renderer = client.renderer(type);
                if (renderer != null) renderers.put(id + "\0" + type, renderer);
            }
        }
        CLIENTS.putAll(found);
        RENDERERS.putAll(renderers);
        collected = true;
    }

    /** Null keeps the default screen. */
    @Nullable
    public static Screen screen(
            BehaviorMachineMenuView menu, String addonId, String machineType, Inventory inventory, Component title) {
        if (!collected) collect();
        BehaviorMachineClient client = CLIENTS.get(addonId + "\0" + machineType);
        if (client == null) return null;
        return client.screen(machineType, menu, inventory, title);
    }

    @Nullable
    public static BlockEntityRenderer<BlockEntity> renderer(BehaviorMachineClientView view) {
        if (!collected) collect();
        if (view == null) return null;
        return RENDERERS.get(view.addonId() + "\0" + view.machineType());
    }
}
