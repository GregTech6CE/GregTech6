package gregtech.client;

import gregtech.content.registry.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
        modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class StorageContainerClientSetup {
    @SubscribeEvent
    public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e) {
        e.register(GTMenuTypes.FILTER.get(), gregtech.client.gui.FilterScreen::new);
        e.register(GTMenuTypes.USB_SWITCH.get(), gregtech.client.gui.DataSwitchScreen::new);
        e.register(GTMenuTypes.HDD_SWITCH.get(), gregtech.client.gui.DataSwitchScreen::new);
        e.register(GTMenuTypes.GARBAGE_DUMP.get(), gregtech.client.gui.HopperScreen::new);
    }

    @SubscribeEvent
    public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(GTBlockEntities.COIN_MOLD.get(), CoinMoldRenderer::new);
        e.registerBlockEntityRenderer(GTBlockEntities.PROCESSING_TOOL.get(), ProcessingToolRenderer::new);
        e.registerBlockEntityRenderer(GTBlockEntities.MATERIAL_ANVIL.get(), MaterialAnvilRenderer::new);
        e.registerBlockEntityRenderer(GTBlockEntities.EXTENDER.get(), ExtenderCoverRenderer::new);
        e.registerBlockEntityRenderer(
                GTBlockEntities.USB_SWITCH.get(),
                (context) ->
                        new net.minecraft.client.renderer.blockentity.BlockEntityRenderer<
                                gregtech.content.blockentity.inventory.UsbSwitchBlockEntity>() {
                            public void render(
                                    gregtech.content.blockentity.inventory.UsbSwitchBlockEntity be,
                                    float partial,
                                    com.mojang.blaze3d.vertex.PoseStack pose,
                                    net.minecraft.client.renderer.MultiBufferSource buffers,
                                    int light,
                                    int overlay) {
                                for (var side : net.minecraft.core.Direction.values())
                                    PanelCoverRenderer.renderFace(be, side, pose, buffers, light);
                            }
                        });
        e.registerBlockEntityRenderer(GTBlockEntities.BOTTLE_CRATE.get(), BottleCrateRenderer::new);
    }

    @SubscribeEvent
    public static void blocks(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block e) {
        for (var b : GTStorageContainers.BOTTLE_CRATES)
            e.register((s, l, p, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF, b.get());
        for (var b : GTManualStations.ROPES)
            e.register((s, l, p, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF, b.get());
        for (var b : GTManualStations.SCAFFOLDS)
            e.register((s, l, p, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF, b.get());
        for (var b : GTManualStations.SHAPED)
            e.register((s, l, p, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF, b.get());
        for (var b : GTManualStations.VESSELS)
            e.register((s, l, p, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF, b.get());
        for (var b : GTManualStations.MANUAL) e.register((s, l, p, t) -> b.get().tint(t), b.get());
        for (var b : GTManualStations.ANVILS)
            e.register((s, l, p, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF, b.get());
        for (var b : GTStorageContainers.DRAWERS)
            e.register((s, l, p, t) -> t == 0 ? b.get().material().getColor() : 0xFFFFFF, b.get());
        for (var b : GTStorageContainers.LOCKERS)
            e.register((s, l, p, t) -> t == 0 ? b.get().material().getColor() : 0xFFFFFF, b.get());
    }

    @SubscribeEvent
    public static void items(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e) {
        for (var b : GTStorageContainers.BOTTLE_CRATES)
            e.register(
                    ItemColorARGB.opaque((s, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF),
                    b.get().asItem());
        for (var b : GTManualStations.ROPES)
            e.register(
                    ItemColorARGB.opaque((s, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF),
                    b.get().asItem());
        for (var b : GTManualStations.SCAFFOLDS)
            e.register(
                    ItemColorARGB.opaque((s, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF),
                    b.get().asItem());
        for (var b : GTManualStations.SHAPED)
            e.register(
                    ItemColorARGB.opaque((s, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF),
                    b.get().asItem());
        for (var b : GTManualStations.VESSELS)
            e.register(
                    ItemColorARGB.opaque((s, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF),
                    b.get().asItem());
        for (var b : GTManualStations.MANUAL)
            e.register(ItemColorARGB.opaque((s, t) -> b.get().tint(t)), b.get().asItem());
        for (var b : GTManualStations.ANVILS)
            e.register(
                    ItemColorARGB.opaque((s, t) -> t == 0 ? b.get().tintRgb() : 0xFFFFFF),
                    b.get().asItem());
        for (var b : GTStorageContainers.DRAWERS)
            e.register(
                    ItemColorARGB.opaque((s, t) -> t == 0 ? b.get().material().getColor() : 0xFFFFFF),
                    b.get().asItem());
        for (var b : GTStorageContainers.LOCKERS)
            e.register(
                    ItemColorARGB.opaque((s, t) -> t == 0 ? b.get().material().getColor() : 0xFFFFFF),
                    b.get().asItem());
    }
}
