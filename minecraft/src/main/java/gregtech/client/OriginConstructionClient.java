package gregtech.client;
//? if >=1.21.1 {
/*import gregtech.content.registry.GTConstructionBlocks;
*///?} else {
import gregtech.content.registry.GTDecorBlocks;
//?}
import gregtech.content.block.misc.*;
//? if >=1.21.1 {
/*import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class OriginConstructionClient {
    private static net.minecraft.world.level.block.Block[] blocks() {
        return gregtech.content.registry.GTConstructionBlocks.all().stream()
                .map(gregtech.content.registry.RegisteredBlock::get)
                .toArray(net.minecraft.world.level.block.Block[]::new);
    }
//? if >=1.21.1 {
    /*@SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
*///?} else {
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
//?}
        event.enqueueWork(() -> {
            for (var block : blocks()) if (block instanceof ColoredGlassBlock || block instanceof ColoredGlassSlabBlock)
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(block, net.minecraft.client.renderer.RenderType.translucent());
        });
    }
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, index) -> index == 0
                ? ConcreteBlock.tint(FixedConstructionColor.colorOf(state)) : 0xFFFFFF, blocks());
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        var items = java.util.Arrays.stream(blocks()).map(net.minecraft.world.level.block.Block::asItem)
                .toArray(net.minecraft.world.item.Item[]::new);
//? if >=1.21.1 {
        /*event.register(ItemColorARGB.opaque((stack, index) -> index == 0
                ? ConcreteBlock.tint(ColoredConstructionBlock.itemColor(stack)) : 0xFFFFFF), items);
*///?} else {
        event.register((stack, index) -> index == 0
                ? ConcreteBlock.tint(ColoredConstructionBlock.itemColor(stack)) : 0xFFFFFF, items);
//?}
    }
}
