package gregtech.client;

import gregtech.content.cover.MachineCoverSpec;
import java.util.List;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class FilterCoverClientSetup {
    private FilterCoverClientSetup() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        ResourceLocation property = ResourceLocation.fromNamespaceAndPath("gt6ce", "cover_inverted");
        event.enqueueWork(() -> {
            for (String id : List.of("item_filter", "fluid_filter", "item_retriever_cover")) {
                var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
                ItemProperties.register(item, property, (stack, level, entity, seed) ->
                        MachineCoverSpec.inverted(stack) ? 1.0F : 0.0F);
            }
        });
    }
}
