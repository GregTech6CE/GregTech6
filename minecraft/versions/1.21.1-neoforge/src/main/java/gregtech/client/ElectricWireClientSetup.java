package gregtech.client;

import gregtech.api.material.MaterialTextureSet;
import gregtech.content.registry.GTWires;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(
        modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ElectricWireClientSetup {
    private ElectricWireClientSetup() {}

    private static int color(gregtech.content.block.energy.ElectricWireBlock block, int layer) {
        return layer == 0
                ? block.spec().material().getColor()
                : block.spec().insulated() && layer == 1 ? 0x141414 : 0xFFFFFF;
    }

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var entry : GTWires.all()) {
            var block = entry.get();
            event.register((state, level, pos, layer) -> color(block, layer), block);
        }
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        for (var entry : GTWires.all()) {
            var block = entry.get();
            event.register(ItemColorARGB.opaque((stack, layer) -> color(block, layer)), block.asItem());
        }
    }

    public static void models(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        ItemTransforms transforms = BlockItemModelHelper.blockItemTransforms();
        var wire = BlockMaterialIcons.resolveTexture(MaterialTextureSet.METALLIC, "wire");
        var rubber = BlockMaterialIcons.resolveTexture(MaterialTextureSet.RUBBER, "pipeside");
        for (var entry : GTWires.all()) {
            var block = entry.get();
            var spec = block.spec();
            double half = spec.halfThickness();
            double ring = Math.max(1, Math.min(2, half / 4));
            var model = (spec.insulated()
                            ? new PipeWireBakedModel(half, rubber, 1, wire, 0, ring, null)
                            : new PipeWireBakedModel(half, wire, 0, wire, 0, 0, null))
                    .itemTransforms(transforms);
            BlockItemClientModels.alias(models, block, model, model);
        }
    }
}
