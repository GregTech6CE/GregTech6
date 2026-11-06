package gregtech.platform.neoforge.machine;

import gregtech.content.block.machine.FuelGeneratorBlock;
import gregtech.content.blockentity.machine.FuelGeneratorBlockEntity;
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

/** Registers addon fuel generators in the addon's namespace, on GregTech's mod bus. */
public final class FuelGeneratorRegistration {
    private static final List<RegisteredBlock<FuelGeneratorBlock>> BLOCKS = new ArrayList<>();
    private static final List<DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelGeneratorBlockEntity>>> TYPES =
            new ArrayList<>();
    private static final List<DeferredRegister<BlockEntityType<?>>> ENTITIES = new ArrayList<>();

    private FuelGeneratorRegistration() {}

    public static List<RegisteredBlock<FuelGeneratorBlock>> all() {
        return List.copyOf(BLOCKS);
    }

    public static void register(ContentRegistrar registrar) {
        var byNamespace = new LinkedHashMap<String, List<AddonMachines.AcceptedFuel>>();
        for (AddonMachines.AcceptedFuel accepted : AddonMachines.fuelGenerators())
            byNamespace
                    .computeIfAbsent(accepted.addonId(), id -> new ArrayList<>())
                    .add(accepted);
        for (var entry : byNamespace.entrySet()) registerNamespace(registrar, entry.getKey(), entry.getValue());
    }

    public static void submit(IEventBus bus) {
        for (var entities : ENTITIES) entities.register(bus);
        ENTITIES.clear();
        bus.addListener(FuelGeneratorRegistration::capabilities);
    }

    private static void registerNamespace(
            ContentRegistrar registrar, String namespace, List<AddonMachines.AcceptedFuel> machines) {
        DeferredRegister<BlockEntityType<?>> entities =
                DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, namespace);
        ENTITIES.add(entities);
        for (AddonMachines.AcceptedFuel accepted : machines) {
            RegisteredBlock<FuelGeneratorBlock> block = gregtech.content.registry.MultiblockRegistration.register(
                    gregtech.content.multiblock.ControllerDeclarations.fuel(
                            accepted,
                            () -> new FuelGeneratorBlock(
                                    accepted,
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 5.0F)
                                            .requiresCorrectToolForDrops())),
                    (path, factory) -> registrar.block(
                            namespace, path, factory, bare -> new BlockItem(bare, new Item.Properties().stacksTo(16))));
            BLOCKS.add(block);
            String path = "be_" + accepted.declaration().casing().id().replace('/', '_');
            DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelGeneratorBlockEntity>> type =
                    entities.register(path, () -> {
                        AtomicReference<BlockEntityType<FuelGeneratorBlockEntity>> typeRef = new AtomicReference<>();
                        BlockEntityType<FuelGeneratorBlockEntity> built = BlockEntityType.Builder.of(
                                        (pos, state) -> (FuelGeneratorBlockEntity)
                                                block.get().create(typeRef.get(), pos, state),
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
                    Capabilities.FluidHandler.BLOCK, type.get(), FuelGeneratorBlockEntity::fluidCapability);
    }
}
