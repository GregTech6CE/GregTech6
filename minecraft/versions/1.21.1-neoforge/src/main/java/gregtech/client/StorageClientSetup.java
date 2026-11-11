package gregtech.client;

import gregtech.content.block.inventory.MassStorageBlock;
import gregtech.content.registry.GTBlockEntities;
import gregtech.content.registry.GTMassStorages;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Original material casing tint, item display and six sprite cells for bulk storage. */
@EventBusSubscriber(
        modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class StorageClientSetup {
    private StorageClientSetup() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GTBlockEntities.MASS_STORAGE.get(), MassStorageRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.LOGISTICS_MASS_STORAGE.get(), MassStorageRenderer::new);
    }

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var holder : GTMassStorages.all()) {
            MassStorageBlock block = (MassStorageBlock) holder.get();
            event.register(
                    (state, level, pos, layer) -> layer == 0 && block.material() != null
                            ? block.material().getColor()
                            : 0xFFFFFF,
                    block);
        }
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        for (var holder : GTMassStorages.all()) {
            MassStorageBlock block = (MassStorageBlock) holder.get();
            event.register(
                    ItemColorARGB.opaque((stack, layer) -> layer == 0 && block.material() != null
                            ? block.material().getColor()
                            : 0xFFFFFF),
                    block.asItem());
        }
    }
}
