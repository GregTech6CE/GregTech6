package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.block.machine.CompressedGasTankBlock;
import com.gregtech.gregtech.blockentity.machine.CompressedGasTankBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Registers addon compressed gas drums in the addon's namespace, on GregTech's mod bus. */
public final class CompressedGasTankRegistration {
    private static final List<RegistryObject<CompressedGasTankBlock>> BLOCKS = new ArrayList<>();

    private CompressedGasTankRegistration() {}

    public static List<RegistryObject<CompressedGasTankBlock>> all() { return List.copyOf(BLOCKS); }

    public static void register(IEventBus bus) {
        var byNamespace = new LinkedHashMap<String, List<AddonMachines.AcceptedCompressedGas>>();
        for (AddonMachines.AcceptedCompressedGas accepted : AddonMachines.compressedGasTanks())
            byNamespace.computeIfAbsent(accepted.addonId(), id -> new ArrayList<>()).add(accepted);
        for (var entry : byNamespace.entrySet()) registerNamespace(entry.getKey(), entry.getValue(), bus);
    }

    private static void registerNamespace(String namespace, List<AddonMachines.AcceptedCompressedGas> tanks, IEventBus bus) {
        DeferredRegister<Block> blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, namespace);
        DeferredRegister<Item> items = DeferredRegister.create(ForgeRegistries.ITEMS, namespace);
        DeferredRegister<BlockEntityType<?>> entities = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, namespace);
        for (AddonMachines.AcceptedCompressedGas accepted : tanks) {
            RegistryObject<CompressedGasTankBlock> block = blocks.register(accepted.declaration().id(), () ->
                    new CompressedGasTankBlock(accepted, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.0F, 6.0F)
                            .requiresCorrectToolForDrops()));
            BLOCKS.add(block);
            items.register(accepted.declaration().id(), () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
            String path = "be_" + accepted.declaration().id().replace('/', '_');
            entities.register(path, () -> {
                AtomicReference<BlockEntityType<CompressedGasTankBlockEntity>> typeRef = new AtomicReference<>();
                BlockEntityType<CompressedGasTankBlockEntity> type = BlockEntityType.Builder.of(
                        (pos, state) -> new CompressedGasTankBlockEntity(typeRef.get(), pos, state),
                        block.get()).build(null);
                typeRef.set(type);
                block.get().setBeType(() -> type);
                return type;
            });
        }
        blocks.register(bus);
        items.register(bus);
        entities.register(bus);
    }
}
