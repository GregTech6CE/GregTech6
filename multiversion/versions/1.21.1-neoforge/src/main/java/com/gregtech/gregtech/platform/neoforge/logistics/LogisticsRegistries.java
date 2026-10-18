package com.gregtech.gregtech.platform.neoforge.logistics;
import com.gregtech.gregtech.block.machine.LogisticsWireBlock;
import com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity;
import com.gregtech.gregtech.content.logistics.LogisticsCoverDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
/** Actual original connector and all fourteen logistics covers in the existing GT namespace. */
public final class LogisticsRegistries {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(BuiltInRegistries.BLOCK,"gregtech");
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(BuiltInRegistries.ITEM,"gregtech");
    private static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE,"gregtech");
    public static final java.util.function.Supplier<LogisticsWireBlock> WIRE=BLOCKS.register("logistics_wire",()->new LogisticsWireBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(1,2).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.METAL)));
    public static final java.util.function.Supplier<BlockEntityType<LogisticsWireBlockEntity>> LOGISTICS_WIRE=TYPES.register("logistics_wire",()->BlockEntityType.Builder.of(LogisticsWireBlockEntity::new,WIRE.get()).build(null));
    public static java.util.Set<String> queuedBlockIds() { return BLOCKS.getEntries().stream().map(h -> h.getId().getPath()).collect(java.util.stream.Collectors.toUnmodifiableSet()); }
    private LogisticsRegistries() {}
    public static void register(IEventBus bus) {
        ITEMS.register("logistics_wire",()->new BlockItem(WIRE.get(),new Item.Properties()));
        BLOCKS.register(bus);ITEMS.register(bus);TYPES.register(bus);
    }
}
