package com.gregtech.gregtech.platform.neoforge.machine;

import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.block.machine.CompressedGasTankBlock;
import com.gregtech.gregtech.blockentity.machine.CompressedGasTankBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Registers addon compressed gas drums in the addon's namespace, on GregTech's mod bus. */
public final class CompressedGasTankRegistration {
    private static final List<DeferredHolder<Block, CompressedGasTankBlock>> BLOCKS = new ArrayList<>();
    private static final List<DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressedGasTankBlockEntity>>> TYPES = new ArrayList<>();

    private CompressedGasTankRegistration() {}

    public static List<DeferredHolder<Block, CompressedGasTankBlock>> all() { return List.copyOf(BLOCKS); }

    public static void register(IEventBus bus) {
        var byNamespace = new LinkedHashMap<String, List<AddonMachines.AcceptedCompressedGas>>();
        for (AddonMachines.AcceptedCompressedGas accepted : AddonMachines.compressedGasTanks())
            byNamespace.computeIfAbsent(accepted.addonId(), id -> new ArrayList<>()).add(accepted);
        for (var entry : byNamespace.entrySet()) registerNamespace(entry.getKey(), entry.getValue(), bus);
        bus.addListener(CompressedGasTankRegistration::capabilities);
    }

    private static void registerNamespace(String namespace, List<AddonMachines.AcceptedCompressedGas> tanks, IEventBus bus) {
        DeferredRegister<Block> blocks = DeferredRegister.create(Registries.BLOCK, namespace);
        DeferredRegister<Item> items = DeferredRegister.create(Registries.ITEM, namespace);
        DeferredRegister<BlockEntityType<?>> entities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, namespace);
        for (AddonMachines.AcceptedCompressedGas accepted : tanks) {
            DeferredHolder<Block, CompressedGasTankBlock> block = blocks.register(accepted.declaration().id(), () ->
                    new CompressedGasTankBlock(accepted, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.0F, 6.0F)
                            .requiresCorrectToolForDrops()));
            BLOCKS.add(block);
            items.register(accepted.declaration().id(), () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
            String path = "be_" + accepted.declaration().id().replace('/', '_');
            DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressedGasTankBlockEntity>> type = entities.register(path, () -> {
                AtomicReference<BlockEntityType<CompressedGasTankBlockEntity>> typeRef = new AtomicReference<>();
                BlockEntityType<CompressedGasTankBlockEntity> built = BlockEntityType.Builder.of(
                        (pos, state) -> new CompressedGasTankBlockEntity(typeRef.get(), pos, state),
                        block.get()).build(null);
                typeRef.set(built);
                block.get().setBeType(() -> built);
                return built;
            });
            TYPES.add(type);
        }
        blocks.register(bus);
        items.register(bus);
        entities.register(bus);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        for (var type : TYPES)
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type.get(), CompressedGasTankBlockEntity::fluidCapability);
    }
}
