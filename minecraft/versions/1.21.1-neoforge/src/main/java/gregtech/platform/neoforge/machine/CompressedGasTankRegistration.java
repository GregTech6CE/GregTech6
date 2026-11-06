package gregtech.platform.neoforge.machine;

import gregtech.content.block.machine.CompressedGasTankBlock;
import gregtech.content.blockentity.machine.CompressedGasTankBlockEntity;
import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.RegisteredBlock;
import gregtech.engine.addon.AddonMachines;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers addon compressed gas drums in the addon's namespace, on GregTech's mod bus. */
public final class CompressedGasTankRegistration {
    private static final List<RegisteredBlock<CompressedGasTankBlock>> BLOCKS = new ArrayList<>();
    private static final List<DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressedGasTankBlockEntity>>> TYPES =
            new ArrayList<>();
    private static final List<DeferredRegister<BlockEntityType<?>>> ENTITIES = new ArrayList<>();

    private CompressedGasTankRegistration() {}

    public static List<RegisteredBlock<CompressedGasTankBlock>> all() {
        return List.copyOf(BLOCKS);
    }

    public static void register(ContentRegistrar registrar) {
        var byNamespace = new LinkedHashMap<String, List<AddonMachines.AcceptedCompressedGas>>();
        for (AddonMachines.AcceptedCompressedGas accepted : AddonMachines.compressedGasTanks())
            byNamespace
                    .computeIfAbsent(accepted.addonId(), id -> new ArrayList<>())
                    .add(accepted);
        for (var entry : byNamespace.entrySet()) registerNamespace(registrar, entry.getKey(), entry.getValue());
    }

    public static void submit(IEventBus bus) {
        for (var entities : ENTITIES) entities.register(bus);
        ENTITIES.clear();
        bus.addListener(CompressedGasTankRegistration::capabilities);
    }

    private static void registerNamespace(
            ContentRegistrar registrar, String namespace, List<AddonMachines.AcceptedCompressedGas> tanks) {
        DeferredRegister<BlockEntityType<?>> entities =
                DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, namespace);
        ENTITIES.add(entities);
        for (AddonMachines.AcceptedCompressedGas accepted : tanks) {
            RegisteredBlock<CompressedGasTankBlock> block = registrar.block(
                    namespace,
                    accepted.declaration().id(),
                    () -> new CompressedGasTankBlock(
                            accepted,
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(1.0F, 6.0F)
                                    .requiresCorrectToolForDrops()),
                    tank -> new BlockItem(tank, new Item.Properties().stacksTo(16)));
            BLOCKS.add(block);
            String path = "be_" + accepted.declaration().id().replace('/', '_');
            DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressedGasTankBlockEntity>> type =
                    entities.register(path, () -> {
                        AtomicReference<BlockEntityType<CompressedGasTankBlockEntity>> typeRef =
                                new AtomicReference<>();
                        BlockEntityType<CompressedGasTankBlockEntity> built = BlockEntityType.Builder.of(
                                        (pos, state) -> new CompressedGasTankBlockEntity(typeRef.get(), pos, state),
                                        block.get())
                                .build(null);
                        typeRef.set(built);
                        block.get().setBeType(() -> built);
                        return built;
                    });
            TYPES.add(type);
        }
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        for (var type : TYPES)
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK, type.get(), CompressedGasTankBlockEntity::fluidCapability);
    }
}
