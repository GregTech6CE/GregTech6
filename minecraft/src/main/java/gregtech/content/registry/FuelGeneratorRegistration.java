package gregtech.content.registry;

import gregtech.content.block.machine.FuelGeneratorBlock;
import gregtech.content.blockentity.machine.FuelGeneratorBlockEntity;
import gregtech.content.multiblock.ControllerDeclarations;
import gregtech.engine.addon.AddonMachines;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Registers addon fuel generators in the addon's namespace. */
public final class FuelGeneratorRegistration {
    private static final List<RegisteredBlock<FuelGeneratorBlock>> BLOCKS = new ArrayList<>();
    private static final List<RegisteredBlockEntity<FuelGeneratorBlockEntity>> TYPES = new ArrayList<>();

    private FuelGeneratorRegistration() {}

    public static List<RegisteredBlock<FuelGeneratorBlock>> all() {
        return List.copyOf(BLOCKS);
    }

    public static List<RegisteredBlockEntity<FuelGeneratorBlockEntity>> types() {
        return List.copyOf(TYPES);
    }

    public static void register(ContentRegistrar registrar) {
        var byNamespace = new LinkedHashMap<String, List<AddonMachines.AcceptedFuel>>();
        for (AddonMachines.AcceptedFuel accepted : AddonMachines.fuelGenerators())
            byNamespace
                    .computeIfAbsent(accepted.addonId(), id -> new ArrayList<>())
                    .add(accepted);
        for (var entry : byNamespace.entrySet()) registerNamespace(registrar, entry.getKey(), entry.getValue());
    }

    private static void registerNamespace(
            ContentRegistrar registrar, String namespace, List<AddonMachines.AcceptedFuel> machines) {
        for (AddonMachines.AcceptedFuel accepted : machines) {
            RegisteredBlock<FuelGeneratorBlock> block = MultiblockRegistration.register(
                    ControllerDeclarations.fuel(
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
            var type = registrar.blockEntity(namespace, path, () -> {
                AtomicReference<BlockEntityType<FuelGeneratorBlockEntity>> typeRef = new AtomicReference<>();
                BlockEntityType<FuelGeneratorBlockEntity> built = BlockEntityType.Builder.of(
                                (pos, state) -> (FuelGeneratorBlockEntity) block.get().create(typeRef.get(), pos, state),
                                block.get())
                        .build(null);
                typeRef.set(built);
                block.get().setBeType(() -> built);
                return built;
            });
            TYPES.add(type);
        }
    }
}
