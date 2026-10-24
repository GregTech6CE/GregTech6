package gregtech.platform.neoforge.logistics;

import gregtech.content.block.inventory.MassStorageBlock;
import gregtech.content.blockentity.inventory.MassStorageBlockEntity;
import gregtech.internal.material.GTMaterial;
import gregtech.content.blockentity.inventory.LogisticsMassStorageBlockEntity;
import gregtech.content.registry.GTStorageMetals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.ArrayList;
import java.util.List;

/** Registers ordinary and logistics mass-storage blocks from the shared metal catalog. */
public final class StorageRegistries {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, "gt6ce");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, "gt6ce");
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "gt6ce");
    private static final List<java.util.function.Supplier<? extends Block>> STORAGES = new ArrayList<>();
    private static final List<java.util.function.Supplier<? extends Block>> LOGISTICS_STORAGES = new ArrayList<>();
    public static final java.util.function.Supplier<BlockEntityType<MassStorageBlockEntity>> MASS_STORAGE =
            TYPES.register("mass_storage",
            () -> BlockEntityType.Builder.of(MassStorageBlockEntity::new,
                    java.util.stream.Stream.concat(STORAGES.stream(), gregtech.content.registry.GTItemBarrels.ALL.stream()).map(java.util.function.Supplier::get).toArray(Block[]::new)).build(null));
    public static final java.util.function.Supplier<BlockEntityType<LogisticsMassStorageBlockEntity>> LOGISTICS_MASS_STORAGE =
            TYPES.register("logistics_mass_storage",
            () -> BlockEntityType.Builder.of(LogisticsMassStorageBlockEntity::new,
                    LOGISTICS_STORAGES.stream().map(java.util.function.Supplier::get).toArray(Block[]::new)).build(null));

    private StorageRegistries() {
    }

    public static List<java.util.function.Supplier<? extends Block>> all() {
        return java.util.stream.Stream.concat(STORAGES.stream(), LOGISTICS_STORAGES.stream()).toList();
    }

    public static void register(IEventBus eventBus) {
        // Register both block families before attaching their deferred registers.
        for (var materialSpec : GTStorageMetals.ALL)
            bind("mass_storage_" + materialSpec.suffix(), materialSpec.material(),
                    materialSpec.hardness(), materialSpec.resistance());
        for (var materialSpec : GTStorageMetals.ALL)
            bindLogistics("logistics_mass_storage_" + materialSpec.suffix(), materialSpec.material(),
                    materialSpec.hardness(), materialSpec.resistance());
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        TYPES.register(eventBus);
    }

    private static void bindLogistics(String registryId, GTMaterial material, float hardness, float resistance) {
        var registeredBlock = BLOCKS.register(registryId, () ->
                new gregtech.content.block.inventory.LogisticsMassStorageBlock(material,
                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                                .strength(hardness, resistance).requiresCorrectToolForDrops()));
        LOGISTICS_STORAGES.add(registeredBlock);
        ITEMS.register(registryId, () -> new BlockItem(registeredBlock.get(), new Item.Properties().stacksTo(16)));
    }

    private static void bind(String registryId, GTMaterial material, float hardness, float resistance) {
        var registeredBlock = BLOCKS.register(registryId, () -> new MassStorageBlock(material,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                        .strength(hardness, resistance).requiresCorrectToolForDrops()));
        STORAGES.add(registeredBlock);
        ITEMS.register(registryId, () -> new BlockItem(registeredBlock.get(), new Item.Properties().stacksTo(16)));
    }
}
