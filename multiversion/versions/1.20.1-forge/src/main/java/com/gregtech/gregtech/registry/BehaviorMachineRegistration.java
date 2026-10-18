package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.block.machine.BehaviorMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BehaviorMachineBlockEntity;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Registers addon behavior machines in the addon's namespace, on GregTech's mod bus. */
public final class BehaviorMachineRegistration {
    private static final List<RegistryObject<BehaviorMachineBlock>> BLOCKS = new ArrayList<>();

    private BehaviorMachineRegistration() {}

    public static List<RegistryObject<BehaviorMachineBlock>> all() { return List.copyOf(BLOCKS); }

    public static void register(IEventBus bus) {
        var byNamespace = new LinkedHashMap<String, List<AddonMachines.AcceptedBehavior>>();
        for (AddonMachines.AcceptedBehavior accepted : AddonMachines.behaviorMachines())
            byNamespace.computeIfAbsent(accepted.addonId(), id -> new ArrayList<>()).add(accepted);
        for (var entry : byNamespace.entrySet()) registerNamespace(entry.getKey(), entry.getValue(), bus);
    }

    private static void registerNamespace(String namespace, List<AddonMachines.AcceptedBehavior> machines, IEventBus bus) {
        DeferredRegister<Block> blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, namespace);
        DeferredRegister<Item> items = DeferredRegister.create(ForgeRegistries.ITEMS, namespace);
        DeferredRegister<BlockEntityType<?>> entities = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, namespace);
        var holders = new ArrayList<RegistryObject<BehaviorMachineBlock>>();
        for (AddonMachines.AcceptedBehavior accepted : machines) {
            RegistryObject<BehaviorMachineBlock> block = blocks.register(accepted.declaration().id(), () ->
                    new BehaviorMachineBlock(accepted, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 5.0F)
                            .requiresCorrectToolForDrops()));
            holders.add(block);
            BLOCKS.add(block);
            items.register(accepted.declaration().id(), () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
        }
        var groups = new LinkedHashMap<String, List<RegistryObject<BehaviorMachineBlock>>>();
        for (int i = 0; i < machines.size(); i++)
            groups.computeIfAbsent(machines.get(i).blockEntityPath(), path -> new ArrayList<>()).add(holders.get(i));
        for (var group : groups.entrySet()) {
            entities.register(group.getKey(), () -> {
                AtomicReference<BlockEntityType<BehaviorMachineBlockEntity>> typeRef = new AtomicReference<>();
                Block[] valid = group.getValue().stream().map(RegistryObject::get).toArray(Block[]::new);
                BlockEntityType<BehaviorMachineBlockEntity> type = BlockEntityType.Builder.of(
                        (pos, state) -> new BehaviorMachineBlockEntity(typeRef.get(), pos, state), valid).build(null);
                typeRef.set(type);
                for (RegistryObject<BehaviorMachineBlock> block : group.getValue()) block.get().setBeType(() -> type);
                return type;
            });
        }
        blocks.register(bus);
        items.register(bus);
        entities.register(bus);
    }
}
