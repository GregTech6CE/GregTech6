package gregtech.client;

import gregtech.content.item.WoodenBucketItem;
import gregtech.content.registry.GTMultiItems;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.tool.WoodenBucketVisuals;
import net.minecraft.world.item.Item;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
//?}

/** Dynamic band and liquid layer tinting for layered wooden bucket models. */
@EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WoodenBucketClientColors {
    private WoodenBucketClientColors() {}

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        Item[] buckets = GTMultiItems.all().stream()
                .map(reg -> reg.get())
                .filter(WoodenBucketItem.class::isInstance)
                .toArray(Item[]::new);
        if (buckets.length == 0) return;
        event.register(
                (stack, layer) -> 0xFF000000 | (stack.getItem() instanceof WoodenBucketItem bucket
                        ? WoodenBucketVisuals.tintRgb(bucket.band(), bucket.content(), layer)
                        : 0xFFFFFF),
                buckets);
    }
}
