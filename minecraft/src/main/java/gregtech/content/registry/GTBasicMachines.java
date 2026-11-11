package gregtech.content.registry;

import gregtech.api.machine.MachineControllerKind;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.BasicMachineItem;
import gregtech.content.block.machine.MultiblockControllerBlock;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.machine.BasicMachineDefinitions;
import gregtech.content.multiblock.ControllerDeclarations;
import gregtech.engine.machine.BasicMachineSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Shared registration for the original basic machines and their addon extensions. */
public final class GTBasicMachines {
    private GTBasicMachines() {}

    private static final List<RegisteredBlock<BasicMachineBlock>> ALL = new ArrayList<>();
    private static final List<RegisteredBlockEntity<BasicMachineBlockEntity>> TYPES = new ArrayList<>();

    public static List<RegisteredBlock<BasicMachineBlock>> all() {
        return List.copyOf(ALL);
    }

    public static List<RegisteredBlockEntity<BasicMachineBlockEntity>> types() {
        return List.copyOf(TYPES);
    }

    public static void registerAll(ContentRegistrar registrar) {
        for (BasicMachineSpec spec : BasicMachineDefinitions.specifications()) {
            var block = MultiblockRegistration.register(
                    ControllerDeclarations.recipe(
                            spec,
                            () -> spec.controllerKind() == MachineControllerKind.DISTILLATION
                                    ? new MultiblockControllerBlock(spec, properties(spec))
                                    : new BasicMachineBlock(spec, properties(spec))),
                    (path, factory) -> registrar.block(
                            spec.namespace(),
                            path,
                            factory,
                            bare -> new BasicMachineItem(bare, new Item.Properties().stacksTo(16))));
            ALL.add(block);
            var type = registrar.<BasicMachineBlockEntity>blockEntity(spec.namespace(), "be_" + spec.id(), () -> {
                AtomicReference<BlockEntityType<BasicMachineBlockEntity>> ref = new AtomicReference<>();
                var built = BlockEntityType.Builder.of(
                                (pos, state) -> {
                                    var be = block.get().createBlockEntity(ref.get(), pos, state);
                                    be.setSpec(spec);
                                    return be;
                                },
                                block.get())
                        .build(null);
                ref.set(built);
                block.get().setBeTypeSupplier(() -> built);
                return built;
            });
            TYPES.add(type);
        }
        BehaviorMachineRegistration.register(registrar);
        FuelGeneratorRegistration.register(registrar);
        CompressedGasTankRegistration.register(registrar);
    }

    private static BlockBehaviour.Properties properties(BasicMachineSpec spec) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(spec.hardness(), spec.blastResistance())
                .requiresCorrectToolForDrops();
    }
}
