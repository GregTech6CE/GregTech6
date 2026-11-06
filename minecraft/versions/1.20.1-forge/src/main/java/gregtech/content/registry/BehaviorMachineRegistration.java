package gregtech.content.registry;

import gregtech.content.block.machine.BehaviorMachineBlock;
import gregtech.content.blockentity.machine.BehaviorMachineBlockEntity;
import gregtech.engine.addon.AddonMachines;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Registers addon behavior machines in the addon's namespace, on GregTech's mod bus. */
public final class BehaviorMachineRegistration {
    private static final List<RegisteredBlock<BehaviorMachineBlock>> BLOCKS = new ArrayList<>();
    private static final List<DeferredRegister<BlockEntityType<?>>> ENTITIES = new ArrayList<>();

    private BehaviorMachineRegistration() {}

    public static List<RegisteredBlock<BehaviorMachineBlock>> all() {
        return List.copyOf(BLOCKS);
    }

    public static void register(ContentRegistrar registrar) {
        var byNamespace = new LinkedHashMap<String, List<AddonMachines.AcceptedBehavior>>();
        for (AddonMachines.AcceptedBehavior accepted : AddonMachines.behaviorMachines())
            byNamespace
                    .computeIfAbsent(accepted.addonId(), id -> new ArrayList<>())
                    .add(accepted);
        for (var entry : byNamespace.entrySet()) registerNamespace(registrar, entry.getKey(), entry.getValue());
    }

    public static void submit(IEventBus bus) {
        for (var entities : ENTITIES) entities.register(bus);
        ENTITIES.clear();
    }

    private static void registerNamespace(
            ContentRegistrar registrar, String namespace, List<AddonMachines.AcceptedBehavior> machines) {
        DeferredRegister<BlockEntityType<?>> entities =
                DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, namespace);
        ENTITIES.add(entities);
        var holders = new ArrayList<RegisteredBlock<BehaviorMachineBlock>>();
        for (AddonMachines.AcceptedBehavior accepted : machines) {
            RegisteredBlock<BehaviorMachineBlock> block = gregtech.content.registry.MultiblockRegistration.register(
                    gregtech.content.multiblock.ControllerDeclarations.behavior(
                            accepted,
                            () -> new BehaviorMachineBlock(
                                    accepted,
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 5.0F)
                                            .requiresCorrectToolForDrops())),
                    (path, factory) -> registrar.block(
                            namespace, path, factory, bare -> new BlockItem(bare, new Item.Properties().stacksTo(16))));
            holders.add(block);
            BLOCKS.add(block);
        }
        var groups = new LinkedHashMap<String, List<RegisteredBlock<BehaviorMachineBlock>>>();
        for (int i = 0; i < machines.size(); i++)
            groups.computeIfAbsent(machines.get(i).blockEntityPath(), path -> new ArrayList<>())
                    .add(holders.get(i));
        for (var group : groups.entrySet()) {
            entities.register(group.getKey(), () -> {
                AtomicReference<BlockEntityType<BehaviorMachineBlockEntity>> typeRef = new AtomicReference<>();
                Block[] valid =
                        group.getValue().stream().map(RegisteredBlock::get).toArray(Block[]::new);
                BlockEntityType<BehaviorMachineBlockEntity> type = BlockEntityType.Builder.of(
                                (pos, state) -> new BehaviorMachineBlockEntity(typeRef.get(), pos, state), valid)
                        .build(null);
                typeRef.set(type);
                for (RegisteredBlock<BehaviorMachineBlock> block : group.getValue())
                    block.get().setBeType(() -> type);
                return type;
            });
        }
    }
}
