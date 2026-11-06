package gregtech.platform.neoforge.logistics;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.LogisticsWireBlock;
import gregtech.content.blockentity.machine.LogisticsWireBlockEntity;
import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.RegisteredBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NeoForge registration boundary for the logistics wire block, item and block entity. */
public final class LogisticsRegistries {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, REGISTRY_NAMESPACE);
    public static RegisteredBlock<LogisticsWireBlock> WIRE;
    public static final java.util.function.Supplier<BlockEntityType<LogisticsWireBlockEntity>> LOGISTICS_WIRE =
            TYPES.register(
                    "logistics_wire",
                    () -> BlockEntityType.Builder.of(LogisticsWireBlockEntity::new, WIRE.get())
                            .build(null));

    public static java.util.Set<String> queuedBlockIds() {
        return java.util.Set.of("logistics_wire");
    }

    private LogisticsRegistries() {}

    public static void register(ContentRegistrar registrar, IEventBus bus) {
        WIRE = registrar.block(
                REGISTRY_NAMESPACE,
                "logistics_wire",
                () -> new LogisticsWireBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                        .strength(1, 2)
                        .requiresCorrectToolForDrops()
                        .sound(net.minecraft.world.level.block.SoundType.METAL)),
                block -> new BlockItem(block, new Item.Properties()));
        TYPES.register(bus);
    }
}
