package gregtech.platform.neoforge.machine;

import gregtech.api.addon.AddonMachines;
import gregtech.block.machine.FuelGeneratorBlock;
import gregtech.blockentity.machine.FuelGeneratorBlockEntity;
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

/** Registers addon fuel generators in the addon's namespace, on GregTech's mod bus. */
public final class FuelGeneratorRegistration {
    private static final List<DeferredHolder<Block, FuelGeneratorBlock>> BLOCKS = new ArrayList<>();
    private static final List<DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelGeneratorBlockEntity>>> TYPES = new ArrayList<>();

    private FuelGeneratorRegistration() {}

    public static List<DeferredHolder<Block, FuelGeneratorBlock>> all() { return List.copyOf(BLOCKS); }

    public static void register(IEventBus bus) {
        var byNamespace = new LinkedHashMap<String, List<AddonMachines.AcceptedFuel>>();
        for (AddonMachines.AcceptedFuel accepted : AddonMachines.fuelGenerators())
            byNamespace.computeIfAbsent(accepted.addonId(), id -> new ArrayList<>()).add(accepted);
        for (var entry : byNamespace.entrySet()) registerNamespace(entry.getKey(), entry.getValue(), bus);
        bus.addListener(FuelGeneratorRegistration::capabilities);
    }

    private static void registerNamespace(String namespace, List<AddonMachines.AcceptedFuel> machines, IEventBus bus) {
        DeferredRegister<Block> blocks = DeferredRegister.create(Registries.BLOCK, namespace);
        DeferredRegister<Item> items = DeferredRegister.create(Registries.ITEM, namespace);
        DeferredRegister<BlockEntityType<?>> entities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, namespace);
        for (AddonMachines.AcceptedFuel accepted : machines) {
            DeferredHolder<Block, FuelGeneratorBlock> block = blocks.register(accepted.declaration().casing().id(), () ->
                    new FuelGeneratorBlock(accepted, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 5.0F)
                            .requiresCorrectToolForDrops()));
            BLOCKS.add(block);
            items.register(accepted.declaration().casing().id(), () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
            String path = "be_" + accepted.declaration().casing().id().replace('/', '_');
            DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelGeneratorBlockEntity>> type = entities.register(path, () -> {
                AtomicReference<BlockEntityType<FuelGeneratorBlockEntity>> typeRef = new AtomicReference<>();
                BlockEntityType<FuelGeneratorBlockEntity> built = BlockEntityType.Builder.of(
                        (pos, state) -> (FuelGeneratorBlockEntity) block.get().create(typeRef.get(), pos, state),
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
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type.get(), FuelGeneratorBlockEntity::fluidCapability);
    }
}
