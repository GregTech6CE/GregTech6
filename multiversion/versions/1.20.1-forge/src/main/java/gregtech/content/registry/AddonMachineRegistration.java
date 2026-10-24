package gregtech.content.registry;

import gregtech.internal.machine.BasicMachineSpec;
import gregtech.internal.machine.MachineRegistry;
import gregtech.internal.machine.MachineSpec;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.GTMachineBlockItem;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Registers addon recipe machines in the addon's own namespace, on GregTech's mod bus. */
public final class AddonMachineRegistration {
    private static final Map<String, DeferredRegister<net.minecraft.world.level.block.Block>> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, DeferredRegister<Item>> ITEMS = new LinkedHashMap<>();
    private static final Map<String, DeferredRegister<BlockEntityType<?>>> ENTITIES = new LinkedHashMap<>();

    private AddonMachineRegistration() {}

    public static void register(BasicMachineSpec spec, IEventBus bus) {
        String namespace = spec.namespace();
        var blocks = BLOCKS.computeIfAbsent(namespace, ns -> {
            var created = DeferredRegister.create(ForgeRegistries.BLOCKS, ns);
            created.register(bus);
            return created;
        });
        var items = ITEMS.computeIfAbsent(namespace, ns -> {
            var created = DeferredRegister.create(ForgeRegistries.ITEMS, ns);
            created.register(bus);
            return created;
        });
        var entities = ENTITIES.computeIfAbsent(namespace, ns -> {
            var created = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ns);
            created.register(bus);
            return created;
        });
        RegistryObject<BasicMachineBlock> block = blocks.register(spec.id(), () ->
                new BasicMachineBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(spec.hardness(), spec.blastResistance())
                        .requiresCorrectToolForDrops()));
        MachineRegistry.trackBasicMachine(block);
        items.register(spec.id(), () -> new GTMachineBlockItem(block.get(), new Item.Properties().stacksTo(16),
                new MachineSpec(spec.id(), spec.material().getLocalName(), spec.material().getColor(),
                        0, 0, spec.textureFolder(), spec.hardness(), spec.blastResistance()),
                spec));
        entities.register("be_" + spec.id(), () -> {
            AtomicReference<BlockEntityType<BasicMachineBlockEntity>> typeRef = new AtomicReference<>();
            BlockEntityType<BasicMachineBlockEntity> type = BlockEntityType.Builder.of(
                    (pos, state) -> {
                        BasicMachineBlockEntity be = block.get().createBlockEntity(typeRef.get(), pos, state);
                        be.setSpec(spec);
                        return be;
                    },
                    block.get()
            ).build(null);
            typeRef.set(type);
            block.get().setBeTypeSupplier(() -> type);
            return type;
        });
    }
}
