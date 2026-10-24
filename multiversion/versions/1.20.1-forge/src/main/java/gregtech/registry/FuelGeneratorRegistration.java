package gregtech.registry;

import gregtech.api.addon.AddonMachines;
import gregtech.block.machine.FuelGeneratorBlock;
import gregtech.blockentity.machine.FuelGeneratorBlockEntity;
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

/** Registers addon fuel generators in the addon's namespace, on GregTech's mod bus. */
public final class FuelGeneratorRegistration {
    private static final List<RegistryObject<FuelGeneratorBlock>> BLOCKS = new ArrayList<>();

    private FuelGeneratorRegistration() {}

    public static List<RegistryObject<FuelGeneratorBlock>> all() { return List.copyOf(BLOCKS); }

    public static void register(IEventBus bus) {
        var byNamespace = new LinkedHashMap<String, List<AddonMachines.AcceptedFuel>>();
        for (AddonMachines.AcceptedFuel accepted : AddonMachines.fuelGenerators())
            byNamespace.computeIfAbsent(accepted.addonId(), id -> new ArrayList<>()).add(accepted);
        for (var entry : byNamespace.entrySet()) registerNamespace(entry.getKey(), entry.getValue(), bus);
    }

    private static void registerNamespace(String namespace, List<AddonMachines.AcceptedFuel> machines, IEventBus bus) {
        DeferredRegister<Block> blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, namespace);
        DeferredRegister<Item> items = DeferredRegister.create(ForgeRegistries.ITEMS, namespace);
        DeferredRegister<BlockEntityType<?>> entities = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, namespace);
        for (AddonMachines.AcceptedFuel accepted : machines) {
            RegistryObject<FuelGeneratorBlock> block = blocks.register(accepted.declaration().casing().id(), () ->
                    new FuelGeneratorBlock(accepted, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 5.0F)
                            .requiresCorrectToolForDrops()));
            BLOCKS.add(block);
            items.register(accepted.declaration().casing().id(), () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
            String path = "be_" + accepted.declaration().casing().id().replace('/', '_');
            entities.register(path, () -> {
                AtomicReference<BlockEntityType<FuelGeneratorBlockEntity>> typeRef = new AtomicReference<>();
                BlockEntityType<FuelGeneratorBlockEntity> type = BlockEntityType.Builder.of(
                        (pos, state) -> (FuelGeneratorBlockEntity) block.get().create(typeRef.get(), pos, state),
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
