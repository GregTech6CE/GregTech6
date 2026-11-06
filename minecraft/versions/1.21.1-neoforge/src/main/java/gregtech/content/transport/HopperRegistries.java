package gregtech.content.transport;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.*;
import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.GTBlockEntities;
import gregtech.content.registry.RegisteredBlock;
import gregtech.engine.machine.HopperSpec;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;

public final class HopperRegistries {
    private HopperRegistries() {}

    private static final java.util.List<RegisteredBlock<HopperBlock>> HOPPERS = new java.util.ArrayList<>();
    private static final java.util.List<RegisteredBlock<QueueHopperBlock>> QUEUES = new java.util.ArrayList<>();

    private static BlockBehaviour.Properties properties(HopperSpec spec) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(spec.hardness(), spec.blastResistance())
                .requiresCorrectToolForDrops();
    }

    public static java.util.List<RegisteredBlock<HopperBlock>> hoppers() {
        return java.util.Collections.unmodifiableList(HOPPERS);
    }

    public static java.util.List<RegisteredBlock<QueueHopperBlock>> queues() {
        return java.util.Collections.unmodifiableList(QUEUES);
    }

    public static void register(ContentRegistrar registrar) {
        if (!HOPPERS.isEmpty() || !QUEUES.isEmpty()) throw new IllegalStateException("Hoppers registered twice");
        for (var row : HopperCatalog.ALL) {
            var spec = row.spec();
            if (row.queue()) {
                var block = registrar.block(
                        REGISTRY_NAMESPACE,
                        spec.id(),
                        () -> new QueueHopperBlock(spec, properties(spec)),
                        hopper -> new QueueHopperBlockItem(hopper, new Item.Properties().stacksTo(16), spec));
                QUEUES.add(block);
            } else {
                var block = registrar.block(
                        REGISTRY_NAMESPACE,
                        spec.id(),
                        () -> new HopperBlock(spec, properties(spec)),
                        hopper -> new HopperBlockItem(hopper, new Item.Properties().stacksTo(16), spec));
                HOPPERS.add(block);
            }
        }
    }

    public static void submit(IEventBus bus) {
        bus.addListener(HopperRegistries::capabilities);
    }

    private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                GTBlockEntities.HOPPER.get(),
                (be, side) -> be.capabilityHandler(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                GTBlockEntities.QUEUE_HOPPER.get(),
                (be, side) -> be.capabilityHandler(side));
    }
}
