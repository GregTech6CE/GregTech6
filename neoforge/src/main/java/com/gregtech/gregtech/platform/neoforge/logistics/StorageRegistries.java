package com.gregtech.gregtech.platform.neoforge.logistics;

import com.gregtech.gregtech.block.inventory.MassStorageBlock;
import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import com.gregtech.gregtech.registry.GTStorageMetals;
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

/** Complete original metalset mass-storage variants; connected network variants follow separately. */
public final class StorageRegistries {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(BuiltInRegistries.BLOCK,"gregtech");
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(BuiltInRegistries.ITEM,"gregtech");
    public static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE,"gregtech");
    private static final List<java.util.function.Supplier<? extends Block>> STORAGES=new ArrayList<>();
    private static final List<java.util.function.Supplier<? extends Block>> LOGISTICS_STORAGES=new ArrayList<>();
    public static final java.util.function.Supplier<BlockEntityType<MassStorageBlockEntity>> MASS_STORAGE=TYPES.register("mass_storage",()->BlockEntityType.Builder.of(MassStorageBlockEntity::new,STORAGES.stream().map(java.util.function.Supplier::get).toArray(Block[]::new)).build(null));
    public static final java.util.function.Supplier<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.LogisticsMassStorageBlockEntity>> LOGISTICS_MASS_STORAGE=TYPES.register("logistics_mass_storage",()->BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.inventory.LogisticsMassStorageBlockEntity::new,LOGISTICS_STORAGES.stream().map(java.util.function.Supplier::get).toArray(Block[]::new)).build(null));
    private StorageRegistries() {}
    public static List<java.util.function.Supplier<? extends Block>> all() { return java.util.stream.Stream.concat(STORAGES.stream(),LOGISTICS_STORAGES.stream()).toList(); }
    public static void register(IEventBus bus) {
        bind("mass_storage",null,6,6);
        for(var spec:GTStorageMetals.ALL)bind("mass_storage_"+spec.suffix(),spec.material(),spec.hardness(),spec.resistance());
        for(var spec:GTStorageMetals.ALL)bindLogistics("logistics_mass_storage_"+spec.suffix(),spec.material(),spec.hardness(),spec.resistance());
        BLOCKS.register(bus);ITEMS.register(bus);TYPES.register(bus);
    }
    private static void bindLogistics(String id, com.gregtech.gregtech.api.material.GTMaterial material,float hardness,float resistance) {
        var block=BLOCKS.register(id,()->new com.gregtech.gregtech.block.inventory.LogisticsMassStorageBlock(material,BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(hardness,resistance).requiresCorrectToolForDrops()));
        LOGISTICS_STORAGES.add(block);ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties().stacksTo(16)));
    }
    private static void bind(String id, com.gregtech.gregtech.api.material.GTMaterial material,float hardness,float resistance) {
        var block=BLOCKS.register(id,()->new MassStorageBlock(material,BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(hardness,resistance).requiresCorrectToolForDrops()));
        STORAGES.add(block);ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties().stacksTo(16)));
    }
}
