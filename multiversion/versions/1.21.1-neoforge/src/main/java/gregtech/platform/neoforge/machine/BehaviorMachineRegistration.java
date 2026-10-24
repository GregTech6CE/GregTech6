package gregtech.platform.neoforge.machine;

import gregtech.api.addon.AddonMachines;
import gregtech.block.machine.BehaviorMachineBlock;
import gregtech.blockentity.machine.BehaviorMachineBlockEntity;
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

/** Registers addon behavior machines in the addon's namespace, on GregTech's mod bus. */
public final class BehaviorMachineRegistration {
    private static final List<DeferredHolder<Block, BehaviorMachineBlock>> BLOCKS = new ArrayList<>();
    private static final List<DeferredHolder<BlockEntityType<?>, BlockEntityType<BehaviorMachineBlockEntity>>> TYPES = new ArrayList<>();

    private BehaviorMachineRegistration() {}

    public static List<DeferredHolder<Block, BehaviorMachineBlock>> all() { return List.copyOf(BLOCKS); }

    public static void register(IEventBus bus) {
        var byNamespace = new LinkedHashMap<String, List<AddonMachines.AcceptedBehavior>>();
        for (AddonMachines.AcceptedBehavior accepted : AddonMachines.behaviorMachines())
            byNamespace.computeIfAbsent(accepted.addonId(), id -> new ArrayList<>()).add(accepted);
        for (var entry : byNamespace.entrySet()) registerNamespace(entry.getKey(), entry.getValue(), bus);
        bus.addListener(BehaviorMachineRegistration::capabilities);
    }

    private static void registerNamespace(String namespace, List<AddonMachines.AcceptedBehavior> machines, IEventBus bus) {
        DeferredRegister<Block> blocks = DeferredRegister.create(Registries.BLOCK, namespace);
        DeferredRegister<Item> items = DeferredRegister.create(Registries.ITEM, namespace);
        DeferredRegister<BlockEntityType<?>> entities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, namespace);
        var holders = new ArrayList<DeferredHolder<Block, BehaviorMachineBlock>>();
        for (AddonMachines.AcceptedBehavior accepted : machines) {
            DeferredHolder<Block, BehaviorMachineBlock> block = blocks.register(accepted.declaration().casing().id(), () ->
                    new BehaviorMachineBlock(accepted, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 5.0F)
                            .requiresCorrectToolForDrops()));
            holders.add(block);
            BLOCKS.add(block);
            items.register(accepted.declaration().casing().id(), () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
        }
        var groups = new LinkedHashMap<String, List<DeferredHolder<Block, BehaviorMachineBlock>>>();
        for (int i = 0; i < machines.size(); i++)
            groups.computeIfAbsent(machines.get(i).blockEntityPath(), path -> new ArrayList<>()).add(holders.get(i));
        for (var group : groups.entrySet()) {
            DeferredHolder<BlockEntityType<?>, BlockEntityType<BehaviorMachineBlockEntity>> type = entities.register(group.getKey(), () -> {
                AtomicReference<BlockEntityType<BehaviorMachineBlockEntity>> typeRef = new AtomicReference<>();
                Block[] valid = group.getValue().stream().map(DeferredHolder::get).toArray(Block[]::new);
                BlockEntityType<BehaviorMachineBlockEntity> built = BlockEntityType.Builder.of(
                        (pos, state) -> new BehaviorMachineBlockEntity(typeRef.get(), pos, state), valid).build(null);
                typeRef.set(built);
                for (DeferredHolder<Block, BehaviorMachineBlock> block : group.getValue()) block.get().setBeType(() -> built);
                return built;
            });
            TYPES.add(type);
        }
        blocks.register(bus);
        items.register(bus);
        entities.register(bus);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        for (var type : TYPES) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type.get(), BehaviorMachineBlockEntity::itemCapability);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type.get(), BehaviorMachineBlockEntity::fluidCapability);
        }
    }
}
